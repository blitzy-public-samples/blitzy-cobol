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
 * JUnit 5 golden-output test for {@link NegatedConditionApplication}.
 *
 * <p>This is a black-box behavioral assertion against the COBOL &rarr; Java
 * translation of {@code OpenCobol/Conditions/NegatedCondition.cbl}
 * (PROGRAM-ID {@code NEGATED-CONDITION}). It captures {@code System.out} while
 * invoking {@code NegatedConditionApplication.run()} directly &mdash; with no
 * Spring {@code ApplicationContext}, per the AAP &sect;0.7.2 minimal-change
 * clause &mdash; normalizes line endings (CRLF&#x2192;LF) to make the assertion
 * platform-independent, and compares the captured output byte-for-byte against the
 * pre-recorded fixture at
 * {@code src/test/resources/expected/NegatedConditionApplication.txt}.
 *
 * <h2>Behavioral specification (from the COBOL source)</h2>
 * <p>{@code NegatedCondition.cbl} declares {@code 01 NUM01 PIC 9(2) VALUE 20}
 * and {@code 01 NUM02 PIC 9(9) VALUE 25}, then evaluates the negated relation
 * {@code IF NOT NUM01 IS LESS THAN NUM02}. The COBOL {@code NOT} negates the
 * <em>entire</em> relational expression, so it renders as
 * {@code if (!(NUM01 < NUM02))}, not {@code if (!NUM01 < NUM02)} (which would not
 * even be legal Java). Because {@code 20 < 25} is {@code TRUE},
 * {@code NOT (20 < 25)} is {@code FALSE} and the {@code ELSE} branch fires: the
 * program prints exactly one line, {@code "I AM HERE : ELSE"}. The THEN branch
 * ({@code DISPLAY "I AM HERE : IF "}) is dead code for these fixed {@code VALUE}
 * clauses and therefore never appears in the golden output.
 *
 * <h2>Trailing-whitespace asymmetry (the assertion's reason for existing)</h2>
 * <p>The two COBOL {@code DISPLAY} literals carry asymmetric trailing whitespace
 * and are reproduced byte-for-byte by the production translation:
 * <ul>
 *   <li>THEN branch ({@code NegatedCondition.cbl:L18}):
 *       {@code "I AM HERE : IF "} &mdash; HAS a single trailing space inside the
 *       literal (dead code here).</li>
 *   <li>ELSE branch ({@code NegatedCondition.cbl:L20}):
 *       {@code "I AM HERE : ELSE"} &mdash; has NO trailing space; it closes on
 *       {@code 'E'} (0x45).</li>
 * </ul>
 * Since the {@code ELSE} branch is the one that fires, the golden fixture is
 * exactly 17 bytes: the 16 characters {@code "I AM HERE : ELSE"} plus a single
 * {@code LF} (0x0A). This is deliberately the <strong>opposite</strong> of
 * {@code CombinedConditionsApplicationTest}, whose fired branch literal <em>does</em>
 * end with a trailing space. To keep both byte-exact behaviors honest, this test
 * performs a literal {@code assertEquals} with <strong>no</strong>
 * {@code trim()}/{@code strip()} of either operand &mdash; the only normalization
 * applied is CRLF&#x2192;LF, which never touches the trailing-space distinction.
 * Any drift toward emitting {@code "I AM HERE : ELSE \n"} (18 bytes, with a
 * spurious trailing space) makes this assertion fail.
 *
 * <p>Per the AAP &sect;0.7.2 minimal-change clause, this class is fully
 * self-contained: no shared base classes, no helper utilities beyond the private
 * fixture loader below, and no shared fixture loaders across sub-modules. The
 * output-capture boilerplate is duplicated verbatim across the sibling
 * {@code cobol-conditions} test classes; this duplication is intentional and
 * required to keep each translation's verification independent.
 */
@DisplayName("NegatedConditionApplication golden-output verification")
class NegatedConditionApplicationTest {

    /**
     * Classpath location of the golden-output fixture. Resolved relative to the
     * test classpath root (Maven copies {@code src/test/resources/} onto the test
     * classpath), so {@code "expected/NegatedConditionApplication.txt"} maps to
     * {@code src/test/resources/expected/NegatedConditionApplication.txt}.
     */
    private static final String EXPECTED_FIXTURE = "expected/NegatedConditionApplication.txt";

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
     * Verifies that {@code NegatedConditionApplication.run()} prints exactly
     * {@code "I AM HERE : ELSE"} (followed by a single line separator) &mdash; the
     * ELSE branch of {@code IF NOT (NUM01 < NUM02)} &mdash; matching the golden
     * fixture byte-for-byte.
     *
     * <p>The production class is exercised as a plain POJO
     * ({@code new NegatedConditionApplication().run()}); this deliberately bypasses
     * the Spring container because the translation's observable behavior is pure
     * console output with no bean wiring. The captured output is normalized
     * CRLF&#x2192;LF so the assertion holds on every platform regardless of the
     * line separator emitted by {@code System.out.println}. Crucially, neither
     * operand is trimmed or stripped, so the absence of a trailing space in the
     * ELSE literal is enforced at the byte level (17 bytes total).
     *
     * @throws Exception propagated from {@code run(String...)} (declared by the
     *                   {@code CommandLineRunner} contract; this translation throws
     *                   nothing) and from fixture loading
     */
    @Test
    @DisplayName("run() prints 'I AM HERE : ELSE' from ELSE branch of NOT-relational")
    void runProducesExpectedOutput() throws Exception {
        new NegatedConditionApplication().run();

        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadExpected(EXPECTED_FIXTURE);

        assertEquals(expected, actual,
                "Captured stdout must reflect ELSE branch of IF NOT (NUM01 < NUM02)");
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
