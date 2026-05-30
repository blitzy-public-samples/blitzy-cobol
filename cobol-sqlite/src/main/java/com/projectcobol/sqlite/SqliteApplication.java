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

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/SQLite/Hello_SQLITE.cbl}.
 *
 * <p>This class is the security-fix target of the COBOL &rarr; Java refactor: the only
 * sub-module whose Java translation contains an intentional behavioral change
 * relative to the COBOL original. The change replaces the {@code ocsqlite}
 * C-binding text-substitution SQL execution
 * (cf. {@code OpenCobol/SQLite/Hello_SQLITE.cbl} lines 288-307, the
 * {@code ocsql-exec} paragraph) with {@link java.sql.PreparedStatement} parameter
 * binding, structurally neutralizing the SQL injection vector that the SCREEN
 * SECTION {@code key-field} input creates in the COBOL source.
 *
 * <p>Structural translation summary (per AAP &sect;0.6.2):
 * <ul>
 *   <li>{@code call "ocsqlite_init"} &rarr; {@link DriverManager#getConnection(String)}</li>
 *   <li>{@code ocsql-exec} paragraph (lines 288-308, text-substitution execution +
 *       {@code if result not equal 0 display "Err:    " errstr}) &rarr;
 *       {@link #ocsqlExec(Connection, String)}, which executes the verbatim SQL and
 *       reports failures COBOL-style ({@code Err:    <message>}) then continues</li>
 *   <li>{@code call "ocsqlite"} user-input text-substitution &rarr;
 *       parameterized {@link PreparedStatement#executeQuery()} (THE SECURITY FIX)</li>
 *   <li>{@code callback} sub-program (lines 333-395) &rarr; inline {@code while (rs.next())} loop</li>
 *   <li>20-row {@code sql-table external} buffer &rarr; streaming {@link ResultSet}</li>
 *   <li>SCREEN SECTION {@code accept entry-screen} &rarr; {@link Scanner#nextLine()}</li>
 *   <li>{@code call "ocsqlite_close"} &rarr; try-with-resources auto-close</li>
 * </ul>
 *
 * <p>SQL DDL/DML strings ({@code drop table trial;}, {@code create table trial (...);},
 * the compound {@code insert}s with {@code randomblob}, {@code hex}, {@code lower},
 * {@code datetime}, {@code julianday} SQLite functions, and {@code select * from trial;})
 * are preserved character-for-character from the COBOL source in the
 * {@link #QUERY_DROP_TABLE}, {@link #QUERY_CREATE_TABLE}, {@link #QUERY_INSERT}, and
 * {@link #QUERY_SELECT_ALL} constants &mdash; including the trailing semicolons and the
 * double-quoted {@code "something"} literal. Only the <em>execution mechanism</em>
 * changes, in two explicitly documented ways: (1) {@link #ocsqlExec(Connection, String)}
 * splits a verbatim compound statement on {@code ;} because JDBC executes one
 * statement per call; and (2) the single SELECT that consumes user input is
 * parameterized via {@link PreparedStatement} (the permitted SQL-injection fix).
 * The SQL <em>content</em> is otherwise identical to the COBOL literals.
 *
 * <p>Per the AAP filename-traceability rule (&sect;0.7.3), the Java class name follows
 * the source filename ({@code Hello_SQLITE.cbl} &rarr; {@code SqliteApplication})
 * rather than the COBOL {@code PROGRAM-ID} ({@code sqlscreen}).
 */
@SpringBootApplication
public class SqliteApplication implements CommandLineRunner {

    /**
     * Default JDBC URL &mdash; mirrors the COBOL
     * {@code 01 database pic x(8) value 'test.db' & x'00'.} declaration at
     * {@code OpenCobol/SQLite/Hello_SQLITE.cbl:L87}. JDBC drivers do not require
     * the trailing {@code x'00'} null terminator that the COBOL C-string needed.
     */
    static final String DEFAULT_JDBC_URL = "jdbc:sqlite:test.db";

    /**
     * Verbatim COBOL SQL string, preserved character-for-character per the AAP
     * &sect;0.6.2 "Verbatim SQL Preservation Rule" &mdash; including the trailing
     * semicolon present in the COBOL source. Translates
     * {@code move "drop table trial;" to query}
     * ({@code OpenCobol/SQLite/Hello_SQLITE.cbl:L188}). Only the execution
     * mechanism changes (text-substitution {@code ocsqlite} call &rarr;
     * {@link #ocsqlExec(Connection, String)} / {@link Statement#execute(String)});
     * the SQL text itself is identical to the COBOL literal.
     */
    static final String QUERY_DROP_TABLE = "drop table trial;";

    /**
     * Verbatim COBOL SQL string, preserved character-for-character per AAP
     * &sect;0.6.2 &mdash; including the trailing semicolon. Translates
     * {@code move "create table trial (first integer primary key, " &
     * "second char(20), third date);" to query}
     * ({@code OpenCobol/SQLite/Hello_SQLITE.cbl:L191-192}). The literal spelling
     * (lower-case keywords, column list, {@code char(20)}) is identical to the
     * COBOL source; only the execution mechanism changes.
     */
    static final String QUERY_CREATE_TABLE =
            "create table trial (first integer primary key, "
          + "second char(20), third date);";

    /**
     * Verbatim COBOL SQL string, preserved character-for-character per AAP
     * &sect;0.6.2 &mdash; a compound statement containing TWO {@code insert}s
     * separated by a semicolon, and ending with a trailing semicolon. Translates
     * <pre>
     *     move 'insert into trial (first, second, third) values ' &amp;
     *         '(null, lower(hex(randomblob(20))), datetime()); ' &amp;
     *         'insert into trial values (null, "something",' &amp;
     *         ' julianday());' to query
     * </pre>
     * ({@code OpenCobol/SQLite/Hello_SQLITE.cbl:L204-208}, executed twice; see
     * {@link #populateData(Connection)}). The COBOL double-quoted string literal
     * {@code "something"} is preserved <em>verbatim</em> (double quotes retained);
     * SQLite's compatibility quirk interprets a double-quoted token with no
     * matching identifier as the string value {@code something}, so this is
     * functionally identical to the COBOL behavior. The SQLite functions
     * {@code lower}, {@code hex}, {@code randomblob}, {@code datetime}, and
     * {@code julianday} are preserved character-for-character.
     */
    static final String QUERY_INSERT =
            "insert into trial (first, second, third) values "
          + "(null, lower(hex(randomblob(20))), datetime()); "
          + "insert into trial values (null, \"something\","
          + " julianday());";

    /**
     * Verbatim COBOL SQL string, preserved character-for-character per AAP
     * &sect;0.6.2 &mdash; including the trailing semicolon. Translates
     * {@code move "select * from trial;" to query}
     * ({@code OpenCobol/SQLite/Hello_SQLITE.cbl:L244}). Executed via
     * {@link Statement#executeQuery(String)} (which accepts the trailing
     * semicolon) in {@link #displayAllRowsReverse(Connection)} because this
     * SELECT feeds a {@link ResultSet}; it carries no user input and therefore
     * needs no {@link PreparedStatement}.
     */
    static final String QUERY_SELECT_ALL = "select * from trial;";

    /**
     * JDBC connection URL. Production defaults to {@link #DEFAULT_JDBC_URL}.
     * Tests inject {@code jdbc:sqlite::memory:} via the package-private constructor
     * for in-memory, deterministic, parallel-safe execution (per AAP &sect;0.6.2).
     */
    private final String jdbcUrl;

    /**
     * Input stream for the interactive {@code key-field} prompt. Production uses
     * {@link System#in}; tests inject a {@link java.io.ByteArrayInputStream}.
     */
    private final InputStream input;

    /**
     * Output stream for {@code DISPLAY} statements. Production uses
     * {@link System#out}; tests inject a {@link java.io.ByteArrayOutputStream}
     * wrapper for golden-output assertions.
     */
    private final PrintStream output;

    /**
     * Default constructor used by Spring Boot at startup
     * (see {@link #main(String[])}). Initializes the application with production
     * defaults: file-backed SQLite database at {@code jdbc:sqlite:test.db}, and
     * the JVM's standard {@code System.in} / {@code System.out} streams.
     */
    public SqliteApplication() {
        this(DEFAULT_JDBC_URL, System.in, System.out);
    }

    /**
     * Package-private constructor for test injection. Allows JUnit 5 tests
     * (in {@code cobol-sqlite/src/test/java/com/projectcobol/sqlite/}) to
     * substitute an in-memory SQLite URL ({@code jdbc:sqlite::memory:}) and
     * deterministic input/output streams without going through Spring's bean
     * lifecycle.
     *
     * @param jdbcUrl JDBC connection URL (e.g., {@code jdbc:sqlite::memory:} for tests)
     * @param input   input stream for the key-field prompt (e.g., a
     *                {@link java.io.ByteArrayInputStream} populated with test
     *                inputs, including the SQL injection regression payload
     *                {@code '; DROP TABLE trial; --})
     * @param output  output stream for {@code DISPLAY}-equivalent output
     */
    SqliteApplication(String jdbcUrl, InputStream input, PrintStream output) {
        this.jdbcUrl = jdbcUrl;
        this.input = input;
        this.output = output;
    }

    /**
     * Spring Boot entry point. Bootstraps the Spring application context, which
     * detects this class as a {@link CommandLineRunner} bean and invokes
     * {@link #run(String...)} after the context refreshes.
     *
     * @param args command-line arguments forwarded to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SqliteApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Translated PROCEDURE DIVISION of {@code sqlscreen} (the main COBOL program
     * in {@code OpenCobol/SQLite/Hello_SQLITE.cbl} lines 129-268).
     *
     * <p>Execution flow (matching the non-debug COBOL path; lines marked with
     * {@code >>D} in the source are conditional on the {@code -fdebugging-line}
     * compiler flag and are NOT part of the default behavior):
     * <ol>
     *   <li>Open SQLite connection (replaces {@code call "ocsqlite_init"} at
     *       lines 137-143).</li>
     *   <li>{@link #initializeSchema(Connection)} &mdash; DROP + CREATE TABLE trial
     *       (lines 188-193).</li>
     *   <li>{@link #populateData(Connection)} &mdash; two INSERTs with SQLite
     *       {@code randomblob}/{@code hex}/{@code lower}/{@code datetime}/{@code julianday}
     *       functions (lines 204-208, 220-224).</li>
     *   <li>{@link #displayAllRowsReverse(Connection)} &mdash; SELECT and display
     *       rows in reverse order (lines 244-256, mirroring the COBOL
     *       {@code perform varying row-counter from row-max by -1} loop, preceded
     *       by the {@code display function trim(sql-table trailing)} summary at
     *       line 247).</li>
     *   <li>{@link #acceptKeyFieldAndQuery(Connection)} &mdash; accept interactive
     *       {@code key-field} input and execute a parameterized SELECT &mdash; THE
     *       SECURITY FIX (replaces the SCREEN SECTION {@code accept entry-screen}
     *       at lines 262-266 and the text-substitution
     *       {@code ocsql-exec} at lines 288-307).</li>
     * </ol>
     *
     * <p>The {@code Connection} is acquired via try-with-resources, replacing the
     * separate {@code call "ocsqlite_close"} at lines 276-281 (which in the
     * COBOL source is unreachable after {@code goback} at line 268 anyway).
     *
     * @param args command-line arguments (forwarded by Spring Boot; unused here)
     * @throws SQLException if any JDBC operation fails
     */
    @Override
    public void run(String... args) throws SQLException {
        // COBOL: call "ocsqlite_init" using db, database, errstr [L137-143]
        //   -> Java: DriverManager.getConnection(...)
        // The 'test.db' & x'00' null-terminated C string is replaced by a clean
        // JDBC URL; JDBC drivers do not require null termination.
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            initializeSchema(conn);
            populateData(conn);
            displayAllRowsReverse(conn);
            acceptKeyFieldAndQuery(conn);
        }
        // The Connection is auto-closed here, replacing the explicit
        // call "ocsqlite_close" using by value db returning result [L276-281].
    }

    /**
     * Initializes the {@code trial} table schema. Translates COBOL
     * {@code OpenCobol/SQLite/Hello_SQLITE.cbl} lines 188-193:
     * <pre>
     *     move "drop table trial;" to query
     *     perform ocsql-exec
     *
     *     move "create table trial (first integer primary key, " &amp;
     *         "second char(20), third date);" to query
     *     perform ocsql-exec
     * </pre>
     *
     * <p>The SQL strings are preserved character-for-character per AAP &sect;0.6.2
     * "Verbatim SQL Preservation Rule" &mdash; they are the {@link #QUERY_DROP_TABLE}
     * and {@link #QUERY_CREATE_TABLE} constants, which retain the COBOL trailing
     * semicolons and literal spelling. The DROP statement is the verbatim COBOL SQL
     * {@code drop table trial;}; no {@code IF EXISTS} clause is added, because doing
     * so would be a behavioral change beyond the single permitted SQL-injection fix
     * and would violate the AAP &sect;0.7.2 minimal-change clause.
     *
     * <p>Both statements are routed through {@link #ocsqlExec(Connection, String)},
     * the faithful translation of the COBOL {@code ocsql-exec} paragraph
     * ({@code OpenCobol/SQLite/Hello_SQLITE.cbl:L306-308}), which on a nonzero
     * result {@code display "Err:    " errstr} and then <em>falls through</em> to
     * the next statement rather than aborting. On a fresh database (the in-memory
     * test database, and the first run of a file-backed {@code test.db}) the
     * verbatim {@code drop table trial;} fails with "no such table: trial"; this
     * translation reports that error COBOL-style ({@code Err:    <message>}) and
     * continues to the CREATE TABLE, exactly mirroring the COBOL behavior. The
     * golden-output fixture therefore begins with that {@code Err:} line.
     *
     * @param conn open JDBC connection
     */
    void initializeSchema(Connection conn) {
        // COBOL: move "drop table trial;" to query; perform ocsql-exec [L188-189]
        //   -> Java: ocsqlExec(conn, QUERY_DROP_TABLE) (verbatim SQL incl. semicolon).
        // On a fresh database the DROP fails; ocsqlExec reports "Err:    <message>"
        // and continues, exactly like the COBOL ocsql-exec paragraph [L306-308].
        ocsqlExec(conn, QUERY_DROP_TABLE);

        // COBOL: move "create table trial (...);" to query; perform ocsql-exec [L191-193]
        //   -> Java: ocsqlExec(conn, QUERY_CREATE_TABLE) (verbatim SQL incl. semicolon).
        ocsqlExec(conn, QUERY_CREATE_TABLE);
    }

    /**
     * Populates the {@code trial} table with rows using SQLite-specific
     * functions. Translates COBOL
     * {@code OpenCobol/SQLite/Hello_SQLITE.cbl} lines 204-208 and 220-224:
     * <pre>
     *     move 'insert into trial (first, second, third) values ' &amp;
     *         '(null, lower(hex(randomblob(20))), datetime()); ' &amp;
     *         'insert into trial values (null, "something",' &amp;
     *         ' julianday());' to query
     *     perform ocsql-exec
     * </pre>
     *
     * <p>The COBOL source executes this same compound INSERT TWICE (once at
     * lines 204-208 and once at lines 220-224). The Java translation preserves
     * the duplicate execution behavior for behavioral fidelity. Each invocation
     * inserts two rows, so the {@code trial} table ends up with 4 rows total
     * (matching the COBOL behavior).
     *
     * <p>SQL preserved character-for-character per AAP &sect;0.6.2 &mdash; the
     * {@link #QUERY_INSERT} constant retains the COBOL compound statement
     * exactly, including the embedded and trailing semicolons and the
     * <em>double-quoted</em> string literal {@code "something"}. Only the
     * execution mechanism changes: {@link #ocsqlExec(Connection, String)} splits
     * the verbatim compound statement on {@code ;} (a documented
     * execution-mechanism exception, because JDBC's
     * {@link Statement#execute(String)} processes one statement at a time) and
     * executes each fragment. SQLite's compatibility quirk interprets the
     * double-quoted {@code "something"} as the string value {@code something},
     * so the verbatim double quotes are functionally identical to the COBOL
     * behavior.
     *
     * @param conn open JDBC connection
     */
    void populateData(Connection conn) {
        // COBOL: move 'insert into trial (first, second, third) values '
        //              '(null, lower(hex(randomblob(20))), datetime()); '
        //              'insert into trial values (null, "something", julianday());'
        //         to query; perform ocsql-exec  [L204-208, L220-224 - executed TWICE]
        //   -> Java: ocsqlExec(conn, QUERY_INSERT) for each of the two COBOL
        //           invocations. QUERY_INSERT is the verbatim compound SQL; ocsqlExec
        //           splits it on ';' (execution-mechanism exception) and runs each
        //           statement, reporting any failure COBOL-style and continuing.
        for (int i = 0; i < 2; i++) {
            ocsqlExec(conn, QUERY_INSERT);
        }
    }

    /**
     * Faithful Java translation of the COBOL {@code ocsql-exec} paragraph
     * ({@code OpenCobol/SQLite/Hello_SQLITE.cbl:L288-308}). In the COBOL source
     * this paragraph builds a null-terminated C string from the {@code query}
     * buffer, passes it to the {@code ocsqlite} C binding for execution, and
     * &mdash; critically &mdash; on a nonzero result reports the error and
     * <em>falls through</em> rather than aborting:
     * <pre>
     *     call "ocsqlite" using by value db callback-proc by reference zquery ...
     *     if result not equal 0
     *         display "Err:    " errstr end-display
     *     end-if
     * </pre>
     *
     * <p>This method preserves that error-reporting/continuation behavior exactly
     * (per finding F5 / AAP &sect;0.6.2): on a {@link SQLException} it writes
     * {@code "Err:    " + message} to the {@link #output} sink (the COBOL literal
     * has four spaces after the colon) and continues to the next statement. The
     * error is NOT suppressed &mdash; suppressing it would be a behavioral change
     * beyond the single permitted SQL-injection remediation.
     *
     * <p><strong>Execution-mechanism exception (documented per AAP &sect;0.6.2):</strong>
     * the COBOL {@code ocsqlite}/{@code sqlite3_exec} binding executes an entire
     * multi-statement SQL string in one call, but JDBC's
     * {@link Statement#execute(String)} processes only the first statement of a
     * compound string. To reproduce the COBOL multi-statement behavior while
     * keeping the SQL <em>content</em> verbatim, this method splits the verbatim
     * {@code query} on the statement separator {@code ;} and executes each
     * non-empty fragment in turn. None of the preserved SQL literals contain a
     * semicolon inside a string literal, so this split is unambiguous.
     *
     * @param conn  open JDBC connection
     * @param query the verbatim COBOL SQL string (may contain multiple {@code ;}
     *              separated statements and a trailing {@code ;})
     */
    void ocsqlExec(Connection conn, String query) {
        // Execution-mechanism exception: split the verbatim multi-statement query
        // on ';' so every COBOL statement runs (JDBC executes one at a time). The
        // SQL CONTENT in the QUERY_* constants stays verbatim; only HOW it is
        // dispatched to the driver changes.
        for (String fragment : query.split(";")) {
            String sql = fragment.trim();
            if (sql.isEmpty()) {
                // Skip the empty trailing fragment produced by the trailing ';'.
                continue;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException ex) {
                // COBOL ocsql-exec [L306-308]: if result not equal 0,
                //   display "Err:    " errstr  (four spaces after the colon),
                // then fall through to the next statement. We report and continue.
                output.println("Err:    " + ex.getMessage());
            }
        }
    }

    /**
     * Selects all rows from {@code trial} and displays them in reverse order,
     * mirroring the COBOL {@code sqlscreen} reverse-iteration loop at
     * {@code OpenCobol/SQLite/Hello_SQLITE.cbl} lines 244-256:
     * <pre>
     *     move 1 to row-counter
     *     move "select * from trial;" to query
     *     perform ocsql-exec
     *     display function trim(sql-table trailing) end-display
     *
     *     subtract 1 from row-counter giving row-max end-subtract
     *     perform varying row-counter from row-max by -1
     *         until row-counter &lt; 1
     *             move sql-records(row-counter) to main-record
     *             display "|" key-field "|" end-display
     *             display "|" str-field "|" end-display
     *             display "|" date-field "|" end-display
     *     end-perform
     * </pre>
     *
     * <p>Two COBOL displays are reproduced here:
     * <ol>
     *   <li>The {@code display function trim(sql-table trailing)} at line 247
     *       emits a single summary line listing every row's three fields in
     *       forward order. Per AAP &sect;0.6.2 the 20-row {@code sql-table}
     *       buffer is eliminated, so the summary is assembled by space-joining
     *       the streamed {@link ResultSet} rows ({@code first second third} per
     *       row, in forward order).</li>
     *   <li>The {@code perform varying row-counter from row-max by -1} loop at
     *       lines 250-256 emits each row's three fields, one per line, wrapped in
     *       {@code |...|}, iterating the rows in REVERSE order. Per AAP &sect;0.6.2
     *       this becomes
     *       "{@code List<String[]> rows = new ArrayList<>(); while (rs.next())
     *        rows.add(...); Collections.reverse(rows); rows.forEach(...);}".</li>
     * </ol>
     *
     * <p>The 20-row {@code sql-table external} buffer (lines 102-104, 351-352)
     * is eliminated; the {@link ResultSet} is streaming and unbounded. The
     * inline NOTE below records that the COBOL original was bounded by 20 rows.
     *
     * <p>This SELECT statement has no user input and therefore no SQL injection
     * vector; it is executed via {@link Statement#executeQuery(String)} rather
     * than {@link PreparedStatement} to keep the no-input-no-prepared-statement
     * pattern explicit. The SECURITY FIX (parameterized SELECT) is applied
     * separately in {@link #queryByKey(Connection, String)}.
     *
     * @param conn open JDBC connection
     * @throws SQLException if the SELECT fails
     */
    void displayAllRowsReverse(Connection conn) throws SQLException {
        // NOTE: The COBOL original buffered query results into a 20-row
        // external sql-table (sql-records pic x(50) occurs 20 times)
        // [OpenCobol/SQLite/Hello_SQLITE.cbl:L102-104, L351-352] and reverse-
        // iterated that buffer. The Java translation streams the ResultSet
        // into a List and uses Collections.reverse(...), lifting the 20-row
        // constraint (AAP §0.6.2 design note: "The Java implementation removes
        // the 20-row buffer because JDBC's ResultSet is unlimited and streaming").
        List<String[]> rows = new ArrayList<>();

        // COBOL: move "select * from trial;" to query; perform ocsql-exec [L244-246]
        //   -> Java: Statement.executeQuery(QUERY_SELECT_ALL)
        // QUERY_SELECT_ALL is the verbatim COBOL SQL "select * from trial;" (incl.
        // the trailing semicolon, which executeQuery accepts) per AAP §0.6.2.
        // No user input -> no PreparedStatement needed -> no injection risk.
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY_SELECT_ALL)) {
            // COBOL callback (lines 333-395) populated sql-records via row-counter;
            // here we iterate the ResultSet directly, eliminating the callback
            // sub-program and the procedure-pointer (callback-proc) [L90, L152].
            while (rs.next()) {
                rows.add(new String[] {
                        String.valueOf(rs.getInt("first")),
                        rs.getString("second"),
                        rs.getString("third")
                });
            }
        }

        // COBOL: display function trim(sql-table trailing) end-display [L247]
        //   -> Java: space-join every row's three fields in FORWARD order into a
        //            single summary line. The COBOL sql-table is a flat buffer of
        //            all rows concatenated; trimming trailing spaces and printing
        //            it produces one line containing every field. Streaming the
        //            ResultSet and joining with single spaces reproduces that
        //            single-line summary without the fixed 20-row buffer.
        StringBuilder summary = new StringBuilder();
        for (String[] row : rows) {
            if (summary.length() > 0) {
                summary.append(' ');
            }
            summary.append(row[0]).append(' ')
                   .append(row[1]).append(' ')
                   .append(row[2]);
        }
        output.println(summary.toString());

        // COBOL: subtract 1 from row-counter giving row-max;
        //        perform varying row-counter from row-max by -1
        //        until row-counter < 1 [L249-256]
        //   -> Java: Collections.reverse(rows); then forward-iterate the reversed
        //            list to emit rows highest-first.
        Collections.reverse(rows);

        for (String[] row : rows) {
            // COBOL: display "|" key-field "|" end-display [L253]
            output.println("|" + row[0] + "|");
            // COBOL: display "|" str-field "|" end-display [L254]
            output.println("|" + row[1] + "|");
            // COBOL: display "|" date-field "|" end-display [L255]
            output.println("|" + row[2] + "|");
        }
    }

    /**
     * Accepts the interactive {@code key-field} input and executes the
     * parameterized SELECT. THIS IS WHERE THE SECURITY FIX IS WIRED IN.
     *
     * <p>Translates COBOL {@code OpenCobol/SQLite/Hello_SQLITE.cbl} SCREEN SECTION
     * input loop at lines 262-266:
     * <pre>
     *     perform varying row-counter from 1 by 1
     *         until row-counter &gt; row-max
     *         move sql-records(row-counter) to main-record
     *         accept entry-screen end-accept
     *     end-perform
     * </pre>
     *
     * <p>In the COBOL source, the SCREEN SECTION at lines 108-127 defines the
     * {@code entry-screen} layout including
     * {@code line 2 col 14 pic x(10) using key-field} at line 116 &mdash; the user
     * input field. In the COBOL implementation, this user input would be
     * concatenated into SQL strings via the {@code ocsql-exec} paragraph
     * (lines 288-307), creating the SQL injection vector that
     * AAP &sect;0.6.2 documents.
     *
     * <p>The Java translation breaks this vulnerability by:
     * <ol>
     *   <li>Reading the input via {@link Scanner#nextLine()} from the injectable
     *       {@link #input} stream (not directly from {@code System.in}, enabling
     *       JUnit 5 tests to feed deterministic inputs including the regression
     *       payload {@code '; DROP TABLE trial; --}).</li>
     *   <li>Delegating to {@link #queryByKey(Connection, String)} which uses
     *       {@link PreparedStatement} parameter binding &mdash; making SQL injection
     *       structurally impossible.</li>
     * </ol>
     *
     * <p>If no input is available (e.g., a production run with no connected
     * stdin), this method returns silently without executing the parameterized
     * query &mdash; matching the COBOL behavior when the SCREEN SECTION accept
     * receives no input.
     *
     * @param conn open JDBC connection
     * @throws SQLException if the parameterized query fails
     */
    void acceptKeyFieldAndQuery(Connection conn) throws SQLException {
        // COBOL: SCREEN SECTION key-field pic x(10) [L116]
        //   -> Java: Scanner.nextLine() from injectable InputStream.
        // The Scanner is NOT closed via try-with-resources because closing the
        // underlying System.in in production would prevent any subsequent code
        // from reading stdin. The Scanner is garbage-collected when this method
        // returns; the wrapped InputStream is the caller's responsibility.
        // StandardCharsets.UTF_8 is specified explicitly to silence the
        // -Xlint:all default-charset warning that -Werror would fail the build on.
        Scanner scanner = new Scanner(input, StandardCharsets.UTF_8);

        if (!scanner.hasNextLine()) {
            // No input available (e.g., closed stdin in batch mode) -> mirror
            // the COBOL behavior of silently skipping the interactive prompt.
            return;
        }

        // COBOL: accept entry-screen -> key-field receives 10-char user input
        // (the SQL INJECTION VECTOR in the COBOL source per AAP §0.6.2)
        String keyFieldInput = scanner.nextLine();

        // SECURITY FIX: bind via PreparedStatement (delegated to queryByKey).
        queryByKey(conn, keyFieldInput);
    }

    /**
     * Executes a parameterized SELECT against the {@code trial} table &mdash; THE
     * CORE SECURITY FIX that replaces the COBOL {@code ocsqlite} text-substitution
     * SQL execution (AAP &sect;0.6.2).
     *
     * <p>In the COBOL source, the {@code ocsql-exec} paragraph
     * (lines 288-307) builds {@code zquery} as a null-terminated C string from
     * the {@code query} buffer and passes it verbatim to {@code ocsqlite}; user
     * input from the SCREEN SECTION {@code key-field} flows through the
     * {@code query} buffer into the SQL stream WITHOUT any escaping or
     * parameterization. This is the textbook SQL injection vulnerability
     * documented in AAP &sect;0.6.2.
     *
     * <p>The Java translation uses {@link PreparedStatement#setInt(int, int)}
     * (matching the COBOL {@code pic 9(10)} numeric semantics of
     * {@code key-field}) or, when the input cannot be parsed as an integer
     * (e.g., a SQL injection payload like {@code '; DROP TABLE trial; --}),
     * {@link PreparedStatement#setString(int, String)} &mdash; both of which
     * structurally neutralize injection by binding the value as a parameter
     * rather than concatenating it into the SQL text.
     *
     * <p>Regression test contract: {@code SqliteApplicationTest.injectionAttemptIsNeutralized()}
     * (created by the sibling sub-agent for
     * {@code cobol-sqlite/src/test/java/com/projectcobol/sqlite/}) invokes
     * this method with the literal string {@code '; DROP TABLE trial; --}
     * and asserts that (a) no {@link SQLException} is thrown, (b) the
     * {@code trial} table still exists in the in-memory database after the
     * query runs, and (c) the returned ResultSet is empty (the literal is
     * bound as a value, never executed as SQL).
     *
     * @param conn          open JDBC connection
     * @param keyFieldInput the raw user input (potentially malicious; safely
     *                      parameter-bound by this method)
     * @throws SQLException if the parameterized query fails (NOT thrown for
     *                      injection attempts &mdash; those are bound as inert values)
     */
    void queryByKey(Connection conn, String keyFieldInput) throws SQLException {
        // COBOL: ocsql-exec used text-substitution [L288-307]:
        //   string function trim(query trailing) delimited by size
        //          x"00" delimited by size into zquery end-string
        //   call "ocsqlite" using by value db callback-proc by reference zquery
        //   This concatenated user input directly into SQL text -> SQL injection.
        //
        //   -> Java: PreparedStatement with ? placeholder + setInt/setString.
        //   The user input is bound as a parameter; the SQL parser never sees
        //   the user input as syntax, making injection structurally impossible.
        String sql = "select * from trial where first = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // The COBOL key-field is pic 9(10) (10-digit numeric). Attempt to
            // parse as int to match the COBOL data type. If the input is not
            // numeric (including SQL injection attempts like
            // "'; DROP TABLE trial; --"), bind it as a String -- PreparedStatement
            // neutralizes injection regardless of the binding type used.
            String trimmed = keyFieldInput.trim();
            try {
                int key = Integer.parseInt(trimmed);
                ps.setInt(1, key);
            } catch (NumberFormatException nfe) {
                // Non-numeric input (including injection attempts) is safely
                // bound as a String value. SQLite's dynamic typing allows the
                // comparison to proceed; PreparedStatement guarantees the
                // input is never executed as SQL syntax.
                ps.setString(1, keyFieldInput);
            }

            try (ResultSet rs = ps.executeQuery()) {
                // COBOL callback (lines 333-395) populated sql-records;
                // Java iterates the ResultSet directly via while (rs.next()).
                while (rs.next()) {
                    output.println("|" + rs.getInt("first") + "|");
                    output.println("|" + rs.getString("second") + "|");
                    output.println("|" + rs.getString("third") + "|");
                }
            }
        }
    }
}
