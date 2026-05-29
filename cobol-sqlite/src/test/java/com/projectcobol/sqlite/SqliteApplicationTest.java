/*
 * Copyright (C) 2017-present Maxfx and Project COBOL contributors.
 *
 * This file is part of Project COBOL.
 *
 * Project COBOL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Project COBOL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.projectcobol.sqlite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * JUnit 5 test suite for {@link SqliteApplication}. Validates the Java translation
 * of {@code OpenCobol/SQLite/Hello_SQLITE.cbl} per AAP &sect;0.2.1 and &sect;0.6.2.
 *
 * <p>Contains two MANDATORY test methods plus one optional regression test:
 * <ol>
 *   <li>{@link #producesGoldenOutputMatchingFixture()} &mdash; golden-output regex
 *       match against {@code expected/SqliteApplication.txt} (which uses
 *       {@code <RANDOMBLOB>}, {@code <DATETIME>}, and {@code <JULIANDAY>}
 *       placeholders to handle non-deterministic SQLite function output).</li>
 *   <li>{@link #injectionAttemptIsNeutralized()} &mdash; SQL injection regression test
 *       (THE MANDATORY security-fix validation per AAP &sect;0.6.2). Passes the
 *       literal payload {@code '; DROP TABLE trial; --} as a parameter-bound
 *       value and asserts (a) no {@code SQLException}, (b) {@code trial} table
 *       still exists, (c) {@code ResultSet} is empty.</li>
 *   <li>{@link #queryByKeyWithInjectionPayloadIsNeutralized()} &mdash; additional
 *       regression test invoking the PRODUCTION {@link SqliteApplication#queryByKey}
 *       method directly with the malicious payload (exercises the actual
 *       security-fix code path, not just an isolated JDBC test).</li>
 * </ol>
 *
 * <p>All tests use {@code jdbc:sqlite::memory:} (MANDATORY per AAP &sect;0.2.1) for
 * in-memory, deterministic, parallel-safe execution with no filesystem footprint.
 * Production code uses {@link SqliteApplication#DEFAULT_JDBC_URL}
 * ({@code jdbc:sqlite:test.db}); tests inject the in-memory URL via the
 * package-private 3-arg constructor.
 */
class SqliteApplicationTest {

    /**
     * AAP &sect;0.2.1 mandate: tests MUST use in-memory SQLite for determinism,
     * parallel-safety, and zero filesystem pollution. Production code uses
     * {@link SqliteApplication#DEFAULT_JDBC_URL} ({@code jdbc:sqlite:test.db});
     * tests inject this URL via the package-private 3-arg constructor instead.
     */
    static final String TEST_JDBC_URL = "jdbc:sqlite::memory:";

    /**
     * The literal SQL injection payload that the regression tests bind as a
     * parameter to demonstrate the security fix. Under COBOL's text-substitution
     * {@code ocsql-exec} (lines 288-307 of Hello_SQLITE.cbl), this string would
     * be concatenated into the SQL stream and the {@code DROP TABLE} statement
     * would execute. Under JDBC {@link PreparedStatement} parameter binding,
     * this string is bound as a VALUE and never executed as SQL syntax.
     */
    private static final String MALICIOUS_PAYLOAD = "'; DROP TABLE trial; --";

    /**
     * Golden-output assertion: runs {@link SqliteApplication#run(String...)}
     * against an in-memory SQLite database and verifies the captured stdout
     * matches the expected fixture pattern (with placeholder regex substitution
     * for non-deterministic SQLite function output).
     *
     * <p>Per AAP &sect;0.7.1, this validates the JUnit 5 golden-output testing
     * pattern with fixture path {@code src/test/resources/expected/SqliteApplication.txt}.
     *
     * <p>Empty stdin is injected so {@link SqliteApplication#acceptKeyFieldAndQuery(Connection)}
     * returns early via {@code if (!scanner.hasNextLine()) return;}, producing
     * no additional output beyond the lines from
     * {@link SqliteApplication#displayAllRowsReverse(Connection)} (one summary
     * line followed by the reverse-ordered, pipe-delimited per-field detail lines).
     *
     * @throws Exception if fixture loading or run() execution fails
     */
    @Test
    void producesGoldenOutputMatchingFixture() throws Exception {
        // Capture stdout via an injected ByteArrayOutputStream-wrapped PrintStream.
        // Empty stdin -> acceptKeyFieldAndQuery returns early, leaving only the
        // displayAllRowsReverse output to compare against the fixture.
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream capturedOut = new PrintStream(captured, true, StandardCharsets.UTF_8);
        InputStream emptyInput = new ByteArrayInputStream(new byte[0]);

        SqliteApplication app = new SqliteApplication(TEST_JDBC_URL, emptyInput, capturedOut);
        app.run();

        // Load fixture from classpath. The expected/ subdirectory is rooted at
        // src/test/resources/ which Maven adds to the test classpath.
        String fixtureContent;
        try (InputStream fixture = getClass().getClassLoader()
                .getResourceAsStream("expected/SqliteApplication.txt")) {
            assertNotNull(fixture, "Golden-output fixture must exist on classpath at "
                    + "expected/SqliteApplication.txt");
            fixtureContent = new String(fixture.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Build a regex from the fixture: Pattern.quote(...) escapes regex
        // metacharacters in literal segments (especially the | pipe character,
        // which is a regex alternation metacharacter), then substitute each
        // placeholder with its regex pattern. The \E...\Q sequence closes the
        // current \Q...\E literal block, inserts the regex, and reopens a new
        // literal block. This is the canonical pattern for embedding regex
        // patterns inside Pattern.quote-escaped strings.
        String regex = Pattern.quote(fixtureContent)
                .replace("<JULIANDAY>", "\\E[0-9.]+\\Q")
                .replace("<RANDOMBLOB>", "\\E[0-9a-f]+\\Q")
                .replace("<DATETIME>", "\\E[0-9-]+ [0-9:]+\\Q");

        // Normalize line endings to LF for cross-platform stability. The fixture
        // is committed with LF-only line endings, but on Windows JDK
        // System.lineSeparator() is "\r\n" -- so we normalize the captured side.
        String actualOutput = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");

        // Use Pattern.DOTALL so '.' matches '\n' if needed (defensive -- the
        // current regex does not use a bare '.', but DOTALL keeps the matcher
        // robust against future fixture additions that might span lines).
        assertTrue(Pattern.compile(regex, Pattern.DOTALL).matcher(actualOutput).matches(),
                "Captured stdout must match golden fixture pattern. Actual was:\n"
                        + actualOutput);
    }

    /**
     * MANDATORY SQL injection regression test per AAP &sect;0.2.1 and &sect;0.6.2.
     *
     * <p>Confirms that {@link PreparedStatement} parameter binding structurally
     * prevents SQL injection. Passes the literal string {@link #MALICIOUS_PAYLOAD}
     * as a parameter-bound value to a SELECT query and asserts:
     * <ol type="a">
     *   <li>No {@link java.sql.SQLException} is thrown (covered by
     *       {@link org.junit.jupiter.api.Assertions#assertDoesNotThrow}).</li>
     *   <li>The {@code trial} table still exists in the in-memory database
     *       after the query runs (verified via {@code SELECT name FROM
     *       sqlite_master WHERE type='table' AND name='trial'}).</li>
     *   <li>The returned {@link ResultSet} is empty &mdash; the literal is bound as
     *       a value, never executed as SQL syntax (verified via
     *       {@code assertFalse(rs.next(), ...)}).</li>
     * </ol>
     *
     * <p>This test exercises the SECURITY MECHANISM directly via a clean JDBC
     * connection, isolating the validation from the broader SqliteApplication
     * flow. The companion test
     * {@link #queryByKeyWithInjectionPayloadIsNeutralized()} validates the
     * actual production code path through {@link SqliteApplication#queryByKey}.
     *
     * <p>Under COBOL's text-substitution {@code ocsql-exec} paragraph
     * (Hello_SQLITE.cbl lines 288-307), the payload would close the string
     * literal with a single quote, append a {@code DROP TABLE trial} statement,
     * and comment out the remainder &mdash; destroying the table. Under JDBC
     * {@link PreparedStatement}, the payload is bound as an inert value.
     */
    @Test
    void injectionAttemptIsNeutralized() {
        assertDoesNotThrow(() -> {
            try (Connection conn = DriverManager.getConnection(TEST_JDBC_URL)) {
                // Setup: create trial table mirroring production schema
                // (AAP §0.6.2 verbatim preservation rule: CREATE TABLE schema
                // matches Hello_SQLITE.cbl:L191-193 character-for-character).
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("create table trial (first integer primary key, "
                            + "second char(20), third date)");
                    stmt.execute("insert into trial values (1, 'safe', '2024-01-01')");
                }

                // Attack: bind MALICIOUS_PAYLOAD via PreparedStatement parameter
                // binding. Under COBOL text-substitution this payload would
                // execute DROP TABLE; under JDBC PreparedStatement it is bound
                // as a VALUE and never parsed as SQL syntax.
                //
                // The query targets the 'second' column (a CHAR column) so the
                // payload (a String) can be bound via setString without type
                // coercion concerns.
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM trial WHERE second = ?")) {
                    ps.setString(1, MALICIOUS_PAYLOAD);
                    try (ResultSet rs = ps.executeQuery()) {
                        // ASSERTION (c): ResultSet is empty -- the literal is
                        // bound as a value, never executed as SQL. No row in
                        // trial has 'second' equal to the malicious payload
                        // string, so the result set must be empty.
                        assertFalse(rs.next(),
                                "Malicious payload bound as parameter must not "
                                        + "match any row (proves the literal is "
                                        + "a value, not executed SQL)");
                    }
                }

                // ASSERTION (b): trial table still exists -- DROP TABLE was
                // structurally prevented by PreparedStatement parameter binding.
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT name FROM sqlite_master "
                                + "WHERE type='table' AND name='trial'");
                     ResultSet rs = check.executeQuery()) {
                    assertTrue(rs.next(),
                            "trial table must still exist -- DROP TABLE was "
                                    + "structurally prevented by "
                                    + "PreparedStatement parameter binding");
                    assertEquals("trial", rs.getString("name"));
                }

                // ASSERTION (a): no SQLException thrown -- covered by the
                // surrounding assertDoesNotThrow lambda. If any of the JDBC
                // operations above had thrown SQLException, the assertion
                // would fail before reaching this point.
            }
        });
    }

    /**
     * Additional regression test invoking the PRODUCTION
     * {@link SqliteApplication#queryByKey(Connection, String)} method directly
     * with the malicious payload. This complements
     * {@link #injectionAttemptIsNeutralized()} (which is an isolated JDBC test)
     * by exercising the EXACT security-fix code path in the production class.
     *
     * <p>If a future regression accidentally introduces string concatenation
     * into {@code queryByKey}, this test will fail because the malicious payload
     * would trigger a SQLException (closing the prepared statement's quoted
     * string improperly) or destroy the trial table.
     *
     * <p>The method is invoked with the malicious payload as the
     * {@code keyFieldInput} parameter; production code attempts to parse it as
     * an integer first (which fails because of the embedded apostrophes and
     * semicolons), then falls back to {@code setString(1, payload)} &mdash; which
     * neutralizes the injection regardless of binding type.
     */
    @Test
    void queryByKeyWithInjectionPayloadIsNeutralized() {
        assertDoesNotThrow(() -> {
            try (Connection conn = DriverManager.getConnection(TEST_JDBC_URL)) {
                // Initialize the schema and populate data using the production
                // helper methods (package-private -- directly invokable from this
                // same-package test class). This mirrors what run() does, but
                // gives us explicit control over the connection lifecycle.
                ByteArrayOutputStream captured = new ByteArrayOutputStream();
                PrintStream capturedOut = new PrintStream(
                        captured, true, StandardCharsets.UTF_8);
                InputStream emptyInput = new ByteArrayInputStream(new byte[0]);

                SqliteApplication app = new SqliteApplication(
                        TEST_JDBC_URL, emptyInput, capturedOut);
                app.initializeSchema(conn);
                app.populateData(conn);

                // Invoke queryByKey with the malicious payload directly.
                // This exercises the EXACT security-fix code path in
                // production. If queryByKey were to use string concatenation
                // instead of PreparedStatement, this call would either:
                //   - throw SQLException (failing assertDoesNotThrow), OR
                //   - drop the trial table (failing the next assertion).
                app.queryByKey(conn, MALICIOUS_PAYLOAD);

                // Verify trial table still exists after the malicious query.
                // This is the structural proof that PreparedStatement.setString
                // neutralized the injection attempt.
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT name FROM sqlite_master "
                                + "WHERE type='table' AND name='trial'");
                     ResultSet rs = check.executeQuery()) {
                    assertTrue(rs.next(),
                            "trial table must still exist after queryByKey "
                                    + "invocation with injection payload -- "
                                    + "PreparedStatement.setString neutralized "
                                    + "the attack");
                }
            }
        });
    }
}
