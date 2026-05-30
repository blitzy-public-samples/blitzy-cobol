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
 * JUnit 5 golden-output test for {@link SignConditionApplication}.
 *
 * <p>This is a black-box behavioral assertion against the COBOL &rarr; Java
 * translation of {@code OpenCobol/Conditions/SignCondition.cbl}
 * (PROGRAM-ID {@code SIGN-CONDITION}). It captures {@code System.out} while
 * invoking {@code SignConditionApplication.run()} directly &mdash; with no
 * Spring {@code ApplicationContext}, per the AAP &sect;0.7.2 minimal-change
 * clause &mdash; normalizes line endings (CRLF&#x2192;LF) to make the assertion
 * platform-independent, and compares the captured output byte-for-byte against the
 * pre-recorded fixture at
 * {@code src/test/resources/expected/SignConditionApplication.txt}.
 *
 * <h2>Behavioral specification (from the COBOL source)</h2>
 * <p>{@code SignCondition.cbl} declares three working-storage numerics with fixed
 * {@code VALUE} clauses &mdash; {@code 01 NUM01 PIC S9(9) VALUE -5000} (signed,
 * negative), {@code 01 NUM02 PIC S9(9) VALUE 6} (signed, positive) and
 * {@code 01 NUM03 PIC 9(9) VALUE ZERO} (unsigned, zero) &mdash; then runs
 * <strong>six independent {@code IF ... END-IF} sign-condition tests</strong> in
 * source order (there is no {@code ELSE} chaining between them, so every test is
 * evaluated unconditionally):
 * <pre>
 *     1. IF NUM01 IS POSITIVE  -&gt; (-5000 &gt; 0) FALSE -&gt; no output
 *     2. IF NUM01 IS NEGATIVE  -&gt; (-5000 &lt; 0) TRUE  -&gt; "NUM01 IS NEGATIVE"
 *     3. IF NUM02 IS ZERO      -&gt; (6 == 0)    FALSE -&gt; no output
 *     4. IF NUM02 IS POSITIVE  -&gt; (6 &gt; 0)     TRUE  -&gt; "NUM02 IS POSITIVE"
 *     5. IF NUM03 IS ZERO      -&gt; (0 == 0)    TRUE  -&gt; "NUM03 IS ZERO"
 *     6. IF NUM03 IS POSITIVE  -&gt; (0 &gt; 0)     FALSE -&gt; no output
 * </pre>
 * Exactly three of the six tests fire, producing three lines (in that source order)
 * that match the 50-byte golden fixture byte-for-byte:
 * <pre>
 *     NUM01 IS NEGATIVE
 *     NUM02 IS POSITIVE
 *     NUM03 IS ZERO
 * </pre>
 *
 * <h2>COBOL sign semantics &mdash; the assertion's reason for existing</h2>
 * <p>COBOL's sign conditions are mathematically rigorous: {@code IS POSITIVE}
 * means <em>strictly</em> {@code &gt; 0}, {@code IS NEGATIVE} means
 * <em>strictly</em> {@code &lt; 0}, and <strong>zero is NEITHER positive nor
 * negative</strong>. The sixth test, {@code IF NUM03 IS POSITIVE} with
 * {@code NUM03 == 0}, must therefore evaluate {@code FALSE} and emit nothing. A
 * naive Java translation that used {@code &gt;= 0} for positivity would fire that
 * sixth test and append a fourth line, {@code "NUM03 IS POSITIVE"}, breaking the
 * byte-exact comparison. This test is the regression sentinel against that specific
 * mistake: the tightly-bounded 50-byte fixture fails the assertion if a fourth line
 * appears, if any of the three expected lines is missing, or if the lines are
 * emitted out of source order.
 *
 * <p>Per the AAP &sect;0.7.2 minimal-change clause, this class is fully
 * self-contained: no shared base classes, no helper utilities beyond the private
 * fixture loader below, and no shared fixture loaders across sub-modules. The
 * output-capture boilerplate is duplicated verbatim across the sibling
 * {@code cobol-conditions} test classes; this duplication is intentional and
 * required to keep each translation's verification independent.
 */
@DisplayName("SignConditionApplication golden-output verification")
class SignConditionApplicationTest {

    /**
     * Classpath location of the golden-output fixture. Resolved relative to the
     * test classpath root (Maven copies {@code src/test/resources/} onto the test
     * classpath), so {@code "expected/SignConditionApplication.txt"} maps to
     * {@code src/test/resources/expected/SignConditionApplication.txt}.
     */
    private static final String EXPECTED_FIXTURE = "expected/SignConditionApplication.txt";

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
     * Verifies that {@code SignConditionApplication.run()} prints exactly three
     * lines &mdash; {@code "NUM01 IS NEGATIVE"}, {@code "NUM02 IS POSITIVE"} and
     * {@code "NUM03 IS ZERO"}, in that source order, each followed by a single line
     * separator &mdash; matching the 50-byte golden fixture byte-for-byte.
     *
     * <p>The production class is exercised as a plain POJO
     * ({@code new SignConditionApplication().run()}); this deliberately bypasses the
     * Spring container because the translation's observable behavior is pure console
     * output with no bean wiring. The captured output is normalized CRLF&#x2192;LF so
     * the assertion holds on every platform regardless of the line separator emitted
     * by {@code System.out.println}.
     *
     * <p>The assertion enforces COBOL's strict sign semantics: with {@code NUM03 == 0}
     * the sixth {@code IF NUM03 IS POSITIVE} test must produce no output, so a fourth
     * {@code "NUM03 IS POSITIVE"} line would fail this test. It likewise fails if the
     * three lines are reordered, guarding the source-order-preservation contract.
     *
     * @throws Exception propagated from {@code run(String...)} (declared by the
     *                   {@code CommandLineRunner} contract; this translation throws
     *                   nothing) and from fixture loading
     */
    @Test
    @DisplayName("run() prints NUM01 IS NEGATIVE, NUM02 IS POSITIVE, NUM03 IS ZERO (3 of 6 IFs)")
    void runProducesExpectedOutput() throws Exception {
        new SignConditionApplication().run();

        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadExpected(EXPECTED_FIXTURE);

        assertEquals(expected, actual,
                "Captured stdout must reflect 3 of 6 IF predicates (zero is NEITHER positive nor negative)");
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
