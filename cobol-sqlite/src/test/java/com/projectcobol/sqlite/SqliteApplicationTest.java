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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * JUnit 5 test suite for {@link SqliteApplication}. Validates the Java translation
 * of {@code OpenCobol/SQLite/Hello_SQLITE.cbl} per AAP &sect;0.2.1 and &sect;0.6.2.
 *
 * <p>Contains two MANDATORY test methods:
 * <ol>
 *   <li>{@link #producesGoldenOutputMatchingFixture()} &mdash; golden-output regex
 *       match against {@code expected/SqliteApplication.txt} (which uses
 *       {@code <RANDOMBLOB>}, {@code <DATETIME>}, and {@code <JULIANDAY>}
 *       placeholders to handle non-deterministic SQLite function output).</li>
 *   <li>{@link #injectionAttemptIsNeutralized()} &mdash; SQL injection regression test
 *       (THE MANDATORY security-fix validation per AAP &sect;0.6.2). This is the
 *       designated security gate: it drives the malicious payload
 *       {@code '; DROP TABLE trial; --} through the PRODUCTION
 *       {@link SqliteApplication#queryByKey(Connection, String)} method &mdash; the
 *       {@code keyField} / {@code first = ?} code path that the COBOL
 *       {@code key-field} input feeds (AAP &sect;0.4.1) &mdash; and asserts
 *       (a) no {@code SQLException}, (b) the {@code trial} table still exists,
 *       (c) no result/output is produced for the malicious key, and
 *       (d) the seeded row count is unchanged. Because it exercises production
 *       logic, this gate fails if {@code queryByKey} were ever regressed back to
 *       {@link Statement}-plus-string-concatenation SQL.</li>
 * </ol>
 *
 * <p>A shared in-memory database is provisioned by {@link #setUp()} (annotated
 * {@code @BeforeEach}) and released by {@link #tearDown()} (annotated
 * {@code @AfterEach}). {@code setUp()} opens a single {@code jdbc:sqlite::memory:}
 * {@link Connection}, creates the {@code trial} table with the verbatim COBOL
 * schema, and seeds one deterministic row so the injection gate can assert the
 * row count is unchanged after the attack. Because each {@code jdbc:sqlite::memory:}
 * connection is an isolated database, the security gate operates on this same
 * shared connection.
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
     * Shared in-memory SQLite connection used by the SQL-injection security gate.
     *
     * <p>Provisioned by {@link #setUp()} ({@code @BeforeEach}) and released by
     * {@link #tearDown()} ({@code @AfterEach}). Because every
     * {@code jdbc:sqlite::memory:} connection is an isolated database, the
     * {@code trial} table seeded in {@link #setUp()} is only visible through THIS
     * connection &mdash; so {@link #injectionAttemptIsNeutralized()} must drive the
     * production {@link SqliteApplication#queryByKey(Connection, String)} call
     * against this same shared connection.
     */
    private Connection connection;

    /**
     * Lifecycle setup ({@code @BeforeEach}). Opens the shared in-memory SQLite
     * connection, creates the {@code trial} table using the schema preserved
     * character-for-character from {@code OpenCobol/SQLite/Hello_SQLITE.cbl:L191-193}
     * (AAP &sect;0.6.2 verbatim SQL-preservation rule), and seeds one deterministic
     * row.
     *
     * <p>The single seeded row {@code (1, 'safe', '2024-01-01')} gives the
     * injection gate a known baseline: a legitimate integer key (1) that the
     * production {@code first = ?} query can match, and a fixed row count (1) that
     * must remain unchanged after the malicious payload is bound as a parameter.
     *
     * @throws SQLException if opening the connection or running the DDL/seed fails
     */
    @BeforeEach
    void setUp() throws SQLException {
        // AAP §0.2.1: tests MUST use in-memory SQLite. A fresh database is created
        // for this connection; it is the same connection the security gate queries.
        connection = DriverManager.getConnection(TEST_JDBC_URL);
        try (Statement stmt = connection.createStatement()) {
            // Schema matches Hello_SQLITE.cbl:L191-193 character-for-character
            // (AAP §0.6.2 verbatim preservation rule).
            stmt.execute("create table trial (first integer primary key, "
                    + "second char(20), third date)");
            // Deterministic seed row: known integer key (1) and known row count (1)
            // so the injection gate can assert the row count is unchanged.
            stmt.execute("insert into trial values (1, 'safe', '2024-01-01')");
        }
    }

    /**
     * Lifecycle teardown ({@code @AfterEach}). Closes the shared in-memory SQLite
     * connection opened in {@link #setUp()}, releasing the in-memory database so
     * each test runs against a fresh, isolated {@code trial} table with no
     * cross-test contamination.
     *
     * @throws SQLException if closing the connection fails
     */
    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Golden-output assertion: runs {@link SqliteApplication#run(String...)}
     * against an in-memory SQLite database and verifies the captured stdout
     * matches the expected fixture pattern (with placeholder regex substitution
     * for non-deterministic SQLite function output).
     *
     * <p>Per AAP &sect;0.7.1, this validates the JUnit 5 golden-output testing
     * pattern with fixture path {@code src/test/resources/expected/SqliteApplication.txt}.
     *
     * <p>The fixture's first line is the COBOL-faithful {@code Err:    <message>}
     * report (finding F5 / AAP &sect;0.6.2): the in-memory test database is fresh,
     * so the verbatim {@code drop table trial;} in
     * {@link SqliteApplication#initializeSchema(Connection)} fails with
     * "no such table: trial", and {@link SqliteApplication#ocsqlExec(Connection, String)}
     * reports it COBOL-style and continues (the error is reported, not suppressed).
     * Because the test pins {@code sqlite-jdbc:3.53.1.0}, that message is
     * deterministic, so the fixture matches it as a literal line.
     *
     * <p>Empty stdin is injected so {@link SqliteApplication#acceptKeyFieldAndQuery(Connection)}
     * returns early via {@code if (!scanner.hasNextLine()) return;}, producing
     * no additional output beyond the leading {@code Err:} line and the lines from
     * {@link SqliteApplication#displayAllRowsReverse(Connection)} (one summary
     * line followed by the reverse-ordered, pipe-delimited per-field detail lines).
     *
     * @throws Exception if fixture loading or run() execution fails
     */
    @Test
    void producesGoldenOutputMatchingFixture() throws Exception {
        // Capture stdout via an injected ByteArrayOutputStream-wrapped PrintStream.
        // Empty stdin -> acceptKeyFieldAndQuery returns early, leaving the leading
        // Err: line (fresh-DB DROP failure, reported by ocsqlExec) plus the
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
     * MANDATORY SQL injection regression test per AAP &sect;0.2.1 and &sect;0.6.2 &mdash;
     * THE designated security gate.
     *
     * <p>Drives the literal payload {@link #MALICIOUS_PAYLOAD} through the
     * PRODUCTION {@link SqliteApplication#queryByKey(Connection, String)} method,
     * i.e. the {@code keyField} / {@code first = ?} code path that the COBOL
     * SCREEN SECTION {@code key-field} input feeds (AAP &sect;0.4.1). Exercising the
     * real production query &mdash; rather than an isolated, test-local
     * {@link PreparedStatement} &mdash; is what makes this gate meaningful: it would
     * FAIL if {@code queryByKey} were ever regressed back to {@link Statement}
     * plus string concatenation, because the payload would then either throw a
     * {@link SQLException} or drop the {@code trial} table.
     *
     * <p>The shared {@code trial} table (created and seeded with one row in
     * {@link #setUp()}) is queried through the same shared {@link #connection}
     * &mdash; mandatory because each {@code jdbc:sqlite::memory:} connection is an
     * isolated database. The production method's output is captured via an
     * injected {@link PrintStream} so the "no result" condition can be asserted.
     *
     * <p>Assertions:
     * <ol type="a">
     *   <li>No {@link SQLException} is thrown by the production {@code queryByKey}
     *       call (wrapped in
     *       {@link org.junit.jupiter.api.Assertions#assertDoesNotThrow}).</li>
     *   <li>The {@code trial} table still exists after the query (verified via
     *       {@code SELECT name FROM sqlite_master WHERE type='table' AND name='trial'})
     *       &mdash; the {@code DROP TABLE} embedded in the payload never executed.</li>
     *   <li>No result/output is produced for the malicious key: the captured
     *       output is empty because {@code first = '<payload>'} matches no row
     *       (the literal is bound as a value, never executed as SQL).</li>
     *   <li>The seeded row count is unchanged &mdash; the attack neither inserted
     *       nor deleted any row.</li>
     * </ol>
     *
     * <p>Under COBOL's text-substitution {@code ocsql-exec} paragraph
     * (Hello_SQLITE.cbl lines 288-307), the payload would close the string
     * literal with a single quote, append a {@code DROP TABLE trial} statement,
     * and comment out the remainder &mdash; destroying the table. Under JDBC
     * {@link PreparedStatement} parameter binding, the payload is bound as an
     * inert value.
     *
     * @throws SQLException if a verification query (row count / table existence)
     *                      fails; the production {@code queryByKey} call itself is
     *                      asserted not to throw via {@code assertDoesNotThrow}
     */
    @Test
    void injectionAttemptIsNeutralized() throws SQLException {
        // Capture the production output sink so assertion (c) can verify that the
        // malicious key produces NO result rows. queryByKey writes matched rows to
        // the injected PrintStream; an empty capture proves nothing matched.
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream capturedOut = new PrintStream(captured, true, StandardCharsets.UTF_8);
        InputStream emptyInput = new ByteArrayInputStream(new byte[0]);
        SqliteApplication app = new SqliteApplication(TEST_JDBC_URL, emptyInput, capturedOut);

        // Baseline row count from the row seeded in setUp() (assertion (d) compares
        // against this after the attack).
        int rowsBefore = countTrialRows();

        // ASSERTION (a): the PRODUCTION security-fix path must not throw. This is
        // the heart of the gate -- it runs queryByKey ("select * from trial where
        // first = ?") against the shared connection, binding MALICIOUS_PAYLOAD as a
        // parameter. If queryByKey were regressed to Statement+concatenation, the
        // payload would throw here (or drop the table, caught by assertion (b)).
        assertDoesNotThrow(() -> app.queryByKey(connection, MALICIOUS_PAYLOAD),
                "Production queryByKey must bind the payload as a parameter and "
                        + "must NOT throw -- a throw would indicate unsafe "
                        + "Statement/string-concatenation SQL");

        // ASSERTION (c): no result/output for the malicious key. "first = '<payload>'"
        // matches no row because the payload is bound as a value (never executed),
        // so queryByKey prints nothing.
        String producedOutput = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals("", producedOutput,
                "Malicious key must yield NO result rows / NO output (payload bound "
                        + "as a value, never executed as SQL). Actual output was:\n"
                        + producedOutput);

        // ASSERTION (b): trial table still exists -- the DROP TABLE embedded in the
        // payload was structurally prevented by PreparedStatement parameter binding.
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT name FROM sqlite_master "
                        + "WHERE type='table' AND name='trial'");
             ResultSet rs = check.executeQuery()) {
            assertTrue(rs.next(),
                    "trial table must still exist -- the DROP TABLE in the payload "
                            + "was structurally prevented by PreparedStatement "
                            + "parameter binding in production queryByKey");
            assertEquals("trial", rs.getString("name"));
        }

        // ASSERTION (d): seeded row count unchanged -- the attack neither inserted
        // nor deleted any row.
        assertEquals(rowsBefore, countTrialRows(),
                "Seeded row count must be unchanged after the injection attempt "
                        + "(the payload executed no INSERT/DROP)");
    }

    /**
     * Counts the rows currently in the shared {@code trial} table. Used by
     * {@link #injectionAttemptIsNeutralized()} to capture the seeded baseline
     * before the attack and to confirm the count is unchanged afterwards.
     *
     * @return the number of rows in {@code trial} on the shared {@link #connection}
     * @throws SQLException if the {@code COUNT(*)} query fails
     */
    private int countTrialRows() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("select count(*) from trial")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
}
