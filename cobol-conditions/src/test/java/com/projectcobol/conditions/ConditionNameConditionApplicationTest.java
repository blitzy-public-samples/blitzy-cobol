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
 * JUnit 5 golden-output test for {@link ConditionNameConditionApplication}.
 *
 * <p>This is a black-box behavioral assertion against the COBOL &rarr; Java
 * translation of {@code OpenCobol/Conditions/ConditionNameCondition.cbl}. It
 * captures {@code System.out} while invoking
 * {@code ConditionNameConditionApplication.run()} directly &mdash; with no Spring
 * {@code ApplicationContext}, per the AAP &sect;0.7.2 minimal-change clause &mdash;
 * normalizes line endings (CRLF&#x2192;LF) to make the assertion
 * platform-independent, and compares the captured output byte-for-byte against the
 * pre-recorded fixture at
 * {@code src/test/resources/expected/ConditionNameConditionApplication.txt}.
 *
 * <h2>Behavioral specification (from the COBOL source)</h2>
 * <p>{@code ConditionNameCondition.cbl} declares a single unsigned 3-digit numeric
 * and attaches two COBOL {@code 88-level} <strong>condition names</strong> to it:
 * <pre>
 *   01 M_NUMBER PIC 9(3).
 *       88 M_TRUE  VALUES ARE 30 THRU 100.
 *       88 M_FALSE VALUES ARE 000 THRU 40.
 * </pre>
 * A {@code 88-level} item is a named boolean predicate over its parent's value:
 * {@code M_TRUE} holds exactly when {@code M_NUMBER} is in the inclusive range
 * {@code [30, 100]}, and {@code M_FALSE} holds exactly when {@code M_NUMBER} is in
 * the inclusive range {@code [0, 40]} (COBOL {@code THRU} is inclusive on both ends,
 * and the literal {@code 000} is the integer {@code 0}). The {@code MAIN-PROCEDURE}
 * then assigns {@code MOVE 50 TO M_NUMBER} and guards two {@code DISPLAY} statements:
 * <pre>
 *   MOVE 50 TO M_NUMBER.
 *   IF M_TRUE  DISPLAY 'Passed with ' M_NUMBER ' marks'.
 *   IF M_FALSE DISPLAY 'FAILED with ' M_NUMBER ' marks'.
 *   GOBACK.
 * </pre>
 * With {@code M_NUMBER = 50}, only {@code M_TRUE} (30..100) matches; {@code M_FALSE}
 * (0..40) does not. The program therefore emits exactly <em>one</em> line. The
 * {@code "FAILED with ..."} {@code DISPLAY} is a dead branch for the hardcoded
 * {@code MOVE 50} and never appears in the golden output; this test's single-line
 * fixture implicitly enforces that the dead branch stays dead (if both branches
 * fired, the captured output would roughly double in size).
 *
 * <h2>The {@code "050"} zero-padding (this assertion's reason for existing)</h2>
 * <p>COBOL renders a {@code PIC 9(3)} item in a {@code DISPLAY} statement as an
 * unsigned numeric right-aligned and zero-padded to width 3, so {@code 50} prints
 * as {@code "050"} &mdash; three characters {@code '0'}, {@code '5'}, {@code '0'},
 * <strong>not</strong> {@code "50"} and <strong>not</strong> {@code " 50"}. The
 * golden fixture is exactly 22 bytes: the 21 characters {@code "Passed with 050
 * marks"} plus a single {@code LF} (0x0A). Because the comparison is byte-exact and
 * neither operand is trimmed or stripped, any regression that dropped the
 * zero-padding (emitting {@code "Passed with 50 marks\n"}, 20 bytes) or widened it
 * (emitting {@code "Passed with  50 marks\n"}, 22 bytes but with a space instead of
 * a leading zero) makes this assertion fail with a clear mismatch.
 *
 * <h2>Filename traceability vs PROGRAM-ID (AAP &sect;0.7.3)</h2>
 * <p>The COBOL source carries {@code PROGRAM-ID. CONDITION-STATEMENT.} &mdash; the
 * same identifier used by the sibling {@code ConditionStatement.cbl} &mdash; which
 * appears to be an authoring error in the original COBOL. Per the AAP
 * filename-traceability rule, the authoritative filesystem name drives the Java
 * naming, so the class under test is {@link ConditionNameConditionApplication}
 * (from {@code ConditionNameCondition.cbl}), <strong>not</strong>
 * {@code ConditionStatementApplication}. This test class name
 * ({@code ConditionNameConditionApplicationTest}) follows the same rule and must not
 * propagate the PROGRAM-ID misnaming.
 *
 * <p>Per the AAP &sect;0.7.2 minimal-change clause, this class is fully
 * self-contained: no shared base classes, no helper utilities beyond the private
 * fixture loader below, and no shared fixture loaders across sub-modules. The
 * output-capture boilerplate is duplicated verbatim across the sibling
 * {@code cobol-conditions} test classes; this duplication is intentional and
 * required to keep each translation's verification independent.
 */
@DisplayName("ConditionNameConditionApplication golden-output verification")
class ConditionNameConditionApplicationTest {

    /**
     * Classpath location of the golden-output fixture. Resolved relative to the
     * test classpath root (Maven copies {@code src/test/resources/} onto the test
     * classpath), so {@code "expected/ConditionNameConditionApplication.txt"} maps
     * to {@code src/test/resources/expected/ConditionNameConditionApplication.txt}.
     */
    private static final String EXPECTED_FIXTURE = "expected/ConditionNameConditionApplication.txt";

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
     * Verifies that {@code ConditionNameConditionApplication.run()} prints exactly
     * {@code "Passed with 050 marks"} (followed by a single line separator) &mdash;
     * the {@code M_TRUE} branch of {@code IF M_TRUE} with the {@code PIC 9(3)}
     * zero-padded rendering of {@code M_NUMBER = 50} &mdash; matching the golden
     * fixture byte-for-byte.
     *
     * <p>The production class is exercised as a plain POJO
     * ({@code new ConditionNameConditionApplication().run()}); this deliberately
     * bypasses the Spring container because the translation's observable behavior is
     * pure console output with no bean wiring. The captured output is normalized
     * CRLF&#x2192;LF so the assertion holds on every platform regardless of the line
     * separator emitted by {@code System.out.println}. Crucially, neither operand is
     * trimmed or stripped, so the zero-padded {@code "050"} is enforced at the byte
     * level (22 bytes total).
     *
     * @throws Exception propagated from {@code run(String...)} (declared by the
     *                   {@code CommandLineRunner} contract; this translation throws
     *                   nothing) and from fixture loading
     */
    @Test
    @DisplayName("run() prints 'Passed with 050 marks' (zero-padded M_NUMBER)")
    void runProducesExpectedOutput() throws Exception {
        new ConditionNameConditionApplication().run();

        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadExpected(EXPECTED_FIXTURE);

        assertEquals(expected, actual,
                "Captured stdout must include 88-level M_TRUE branch with zero-padded display");
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
