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

package com.projectcobol.conditions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * JUnit 5 golden-output test for {@link ConditionStatementApplication}.
 *
 * <p>This is a black-box behavioral assertion against the COBOL &rarr; Java
 * translation of {@code OpenCobol/Conditions/ConditionStatement.cbl}
 * (PROGRAM-ID {@code CONDITION-STATEMENT}). It captures {@code System.out} while
 * invoking {@code ConditionStatementApplication.run()} directly &mdash; with no
 * Spring {@code ApplicationContext}, per the AAP &sect;0.7.2 minimal-change
 * clause &mdash; normalizes line endings (CRLF&#x2192;LF) to make the assertion
 * platform-independent, and compares the captured output byte-for-byte against the
 * pre-recorded fixture at
 * {@code src/test/resources/expected/ConditionStatementApplication.txt}.
 *
 * <h2>Behavioral specification (from the COBOL source)</h2>
 * <p>{@code ConditionStatement.cbl} declares four unsigned 9-digit numerics with
 * fixed {@code VALUE} clauses: {@code 01 NUM01 PIC 9(9) VALUE 5},
 * {@code 01 NUM02 PIC 9(9) VALUE 6}, {@code 01 NUM03 PIC 9(9) VALUE 7} and
 * {@code 01 NUM04 PIC 9(9) VALUE 8}. The {@code MAIN-PROCEDURE} nests an inner
 * {@code IF}/{@code ELSE} <em>inside</em> the outer {@code THEN} branch:
 * <pre>
 *     IF NUM01 &lt;= NUM02 THEN
 *       DISPLAY "IS NOT LESS"
 *       IF NUM03 &gt;= NUM04 THEN
 *         DISPLAY "IS GREATER"
 *       ELSE
 *         DISPLAY "IS NOT GREATER"
 *       END-IF
 *     ELSE
 *       DISPLAY "IS LESS"
 *     END-IF.
 * </pre>
 * For the hardcoded values the only live path is outer-{@code THEN} &rarr;
 * inner-{@code ELSE}: the outer test {@code 5 <= 6} is {@code TRUE} (so the outer
 * {@code THEN} fires {@code "IS NOT LESS"}) and the nested inner test {@code 7 >= 8}
 * is {@code FALSE} (so the inner {@code ELSE} fires {@code "IS NOT GREATER"}). The
 * program therefore emits exactly two lines.
 *
 * <h2>Branch-sequence and dead-code preservation (the assertion's reason for existing)</h2>
 * <p>This test pins two distinct invariants of the translation. First, the
 * <strong>source-statement ordering</strong>: {@code "IS NOT LESS"} must be printed
 * <em>before</em> {@code "IS NOT GREATER"}, because the outer {@code THEN} body
 * executes its {@code DISPLAY} prior to evaluating the nested {@code IF}. Second,
 * the <strong>dead-branch discipline</strong>: the inner {@code THEN} literal
 * {@code "IS GREATER"} and the outer {@code ELSE} literal {@code "IS LESS"} are dead
 * code for these fixed {@code VALUE} clauses and, per the AAP &sect;0.7.2
 * minimal-change clause, are retained verbatim in the production {@code run(...)}
 * body but must <em>never</em> execute. The 27-byte fixture
 * ({@code "IS NOT LESS"} + {@code LF} + {@code "IS NOT GREATER"} + {@code LF}) makes
 * the assertion fail if either dead branch ever fires, if the two live lines are
 * reordered, or if the nested control flow is otherwise flattened or simplified.
 *
 * <p>Per the AAP &sect;0.7.2 minimal-change clause, this class is fully
 * self-contained: no shared base classes, no helper utilities beyond the private
 * fixture loader below, and no shared fixture loaders across sub-modules. The
 * output-capture boilerplate is duplicated verbatim across the sibling
 * {@code cobol-conditions} test classes; this duplication is intentional and
 * required to keep each translation's verification independent.
 */
@DisplayName("ConditionStatementApplication golden-output verification")
class ConditionStatementApplicationTest {

    /**
     * Classpath location of the golden-output fixture. Resolved relative to the
     * test classpath root (Maven copies {@code src/test/resources/} onto the test
     * classpath), so {@code "expected/ConditionStatementApplication.txt"} maps to
     * {@code src/test/resources/expected/ConditionStatementApplication.txt}.
     */
    private static final String EXPECTED_FIXTURE = "expected/ConditionStatementApplication.txt";

    /**
     * The real {@code System.out}, captured at test-instance construction time
     * (before {@link #redirectStdout()} runs) so it can be restored afterward.
     */
    private final PrintStream originalOut = System.out;

    /** Buffer that receives everything written to {@code System.out} during a test. */
    private ByteArrayOutputStream capturedOut;

    /**
     * Redirects {@code System.out} to an in-memory buffer before each test so the
     * console output produced by {@code run(...)} can be inspected. A
     * UTF-8-encoded, auto-flushing {@link PrintStream} guarantees byte-exact,
     * platform-independent capture.
     */
    @BeforeEach
    void redirectStdout() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    /** Restores the original {@code System.out} after each test. */
    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    /**
     * Verifies that {@code ConditionStatementApplication.run()} prints exactly two
     * lines &mdash; {@code "IS NOT LESS"} followed by {@code "IS NOT GREATER"} &mdash;
     * matching the golden fixture byte-for-byte and confirming the nested
     * {@code IF}/{@code ELSE} control flow takes the outer-{@code THEN} &rarr;
     * inner-{@code ELSE} path.
     *
     * <p>The production class is exercised as a plain POJO
     * ({@code new ConditionStatementApplication().run()}); this deliberately bypasses
     * the Spring container because the translation's observable behavior is pure
     * console output with no bean wiring. The captured output is normalized
     * CRLF&#x2192;LF so the assertion holds on every platform regardless of the
     * line separator emitted by {@code System.out.println}.
     *
     * @throws Exception propagated from {@code run(String...)} (declared by the
     *                   {@code CommandLineRunner} contract; this translation throws
     *                   nothing) and from fixture loading
     */
    @Test
    @DisplayName("run() prints 'IS NOT LESS' then 'IS NOT GREATER' from nested IF/ELSE")
    void runProducesExpectedOutput() throws Exception {
        new ConditionStatementApplication().run();

        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadExpected(EXPECTED_FIXTURE);

        assertEquals(expected, actual,
                "Captured stdout must reflect outer THEN -> inner ELSE branch sequence");
    }

    /**
     * Loads a golden-output fixture from the test classpath as a UTF-8 string.
     *
     * @param resourcePath classpath-relative path to the fixture
     * @return the fixture contents decoded as UTF-8
     * @throws Exception if the fixture stream cannot be read
     */
    private String loadExpected(String resourcePath) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(in, "Missing classpath fixture: " + resourcePath);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
