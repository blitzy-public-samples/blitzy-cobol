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
 * JUnit 5 golden-output test for {@link EvaluteVerbApplication}.
 *
 * <p>This is a black-box behavioral assertion against the COBOL &rarr; Java
 * translation of {@code OpenCobol/Conditions/EvaluteVerb.cbl}
 * (PROGRAM-ID {@code EVALUATE-VERB}). It captures {@code System.out} while
 * invoking {@code EvaluteVerbApplication.run()} directly &mdash; with no Spring
 * {@code ApplicationContext}, per the AAP &sect;0.7.2 minimal-change clause
 * &mdash; normalizes line endings (CRLF&#x2192;LF) to make the assertion
 * platform-independent, and compares the captured output byte-for-byte against
 * the pre-recorded fixture at
 * {@code src/test/resources/expected/EvaluteVerbApplication.txt}.
 *
 * <h2>Naming (filename traceability &mdash; the misspelling is intentional)</h2>
 * <p>The COBOL source <em>filename</em> is {@code EvaluteVerb.cbl} &mdash; a typo,
 * "Evalute" with the second "a" of "Evaluate" missing. The COBOL
 * {@code PROGRAM-ID} itself is spelled correctly ({@code EVALUATE-VERB}), but per
 * the AAP filename-traceability rule (&sect;0.7.3, "Preserve the source filename's
 * exact spelling, including non-standard cases") the Java type names follow the
 * <em>filename</em>, not the {@code PROGRAM-ID}. This test class is therefore
 * deliberately named {@code EvaluteVerbApplicationTest} (not
 * {@code EvaluateVerbApplicationTest}), exercises a production class named
 * {@code EvaluteVerbApplication}, and loads a fixture named
 * {@code EvaluteVerbApplication.txt}. The misspelling is preserved
 * character-for-character across the production class, this test, and the fixture;
 * "correcting" it on any one of the three would break compilation
 * ({@code EvaluteVerbApplication} would not resolve) or classpath fixture
 * resolution, so this test is itself the strongest enforcement of &sect;0.7.3.
 *
 * <h2>Behavioral specification (from the COBOL source)</h2>
 * <p>{@code EvaluteVerb.cbl} declares a single unsigned 3-digit numeric
 * ({@code 01 NUM01 PIC 9(3) VALUE ZERO}), assigns it via {@code MOVE 3 TO NUM01},
 * then runs a COBOL {@code EVALUATE TRUE} switch over three heterogeneous boolean
 * predicates (first-match-wins, no fall-through):
 * <pre>
 *     MOVE 3 TO NUM01.
 *     EVALUATE TRUE
 *       WHEN NUM01 &gt; 2   -&gt; (3 &gt; 2) TRUE  -&gt; DISPLAY "NUMBER01 GREATER THAN 2"
 *       WHEN NUM01 &lt; 0   -&gt; (3 &lt; 0) FALSE -&gt; (skipped)
 *       WHEN OTHER       -&gt; default fallback -&gt; (skipped)
 *     END-EVALUATE.
 * </pre>
 * With {@code NUM01 == 3} the first predicate ({@code NUM01 > 2}) is TRUE, so the
 * {@code EVALUATE} fires its first branch and the program prints exactly one line
 * that matches the 24-byte golden fixture byte-for-byte:
 * <pre>
 *     NUMBER01 GREATER THAN 2
 * </pre>
 * The second {@code WHEN} ({@code NUM01 < 0}) and the {@code WHEN OTHER} default
 * are never evaluated because {@code EVALUATE TRUE} stops at the first matching
 * predicate.
 *
 * <h2>EVALUATE TRUE first-match semantics &mdash; the assertion's reason for being</h2>
 * <p>COBOL's {@code EVALUATE TRUE} evaluates a sequence of independent boolean
 * predicates top-to-bottom and executes <strong>only</strong> the first whose
 * predicate is TRUE; the remaining {@code WHEN} clauses are skipped (there is no
 * C-style fall-through). The faithful Java equivalent in the production class is a
 * sequential {@code if / else if / else} cascade, whose semantics are identical. A
 * Java {@code switch} expression would be semantically incorrect here, because
 * {@code switch} discriminates on a single value whereas {@code EVALUATE TRUE}
 * discriminates on independent boolean conditions. This test verifies the
 * <em>observable output</em>, not the implementation strategy: the tightly-bounded
 * 24-byte single-line fixture fails the byte-exact comparison if more than one
 * branch fires (e.g. a buggy fall-through that also printed
 * {@code "NUMBER01 LESS THAN 0"} or {@code "INVALID VALUE OF NUMBER01"}), if the
 * wrong branch fires, or if any stray character (such as a trailing space after
 * {@code "2"}) is emitted.
 *
 * <p>Per the AAP &sect;0.7.2 minimal-change clause, this class is fully
 * self-contained: no shared base classes, no helper utilities beyond the private
 * fixture loader below, and no shared fixture loaders across sub-modules. The
 * output-capture boilerplate is duplicated verbatim across the sibling
 * {@code cobol-conditions} test classes; this duplication is intentional and
 * required to keep each translation's verification independent.
 */
@DisplayName("EvaluteVerbApplication golden-output verification (filename misspelling preserved)")
class EvaluteVerbApplicationTest {

    /**
     * Classpath location of the golden-output fixture. Resolved relative to the
     * test classpath root (Maven copies {@code src/test/resources/} onto the test
     * classpath), so {@code "expected/EvaluteVerbApplication.txt"} maps to
     * {@code src/test/resources/expected/EvaluteVerbApplication.txt}. The fixture
     * filename preserves the {@code EvaluteVerb.cbl} "Evalute" misspelling, matching
     * the production class and this test class (AAP &sect;0.7.3).
     */
    private static final String EXPECTED_FIXTURE = "expected/EvaluteVerbApplication.txt";

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
     * Verifies that {@code EvaluteVerbApplication.run()} prints exactly one line
     * &mdash; {@code "NUMBER01 GREATER THAN 2"} followed by a single line separator
     * &mdash; matching the 24-byte golden fixture byte-for-byte.
     *
     * <p>The production class is exercised as a plain POJO
     * ({@code new EvaluteVerbApplication().run()}); this deliberately bypasses the
     * Spring container because the translation's observable behavior is pure console
     * output with no bean wiring. The captured output is normalized CRLF&#x2192;LF so
     * the assertion holds on every platform regardless of the line separator emitted
     * by {@code System.out.println}.
     *
     * <p>The assertion enforces COBOL's {@code EVALUATE TRUE} first-match-wins
     * semantics: with {@code NUM01 == 3} only the first {@code WHEN} ({@code NUM01 > 2})
     * fires, so a second or third line (from the dead {@code WHEN NUM01 < 0} or
     * {@code WHEN OTHER} branches) would fail this byte-exact comparison, as would
     * the wrong branch firing or any stray trailing character.
     *
     * @throws Exception propagated from {@code run(String...)} (declared by the
     *                   {@code CommandLineRunner} contract; this translation throws
     *                   nothing) and from fixture loading
     */
    @Test
    @DisplayName("run() prints 'NUMBER01 GREATER THAN 2' from EVALUATE TRUE first match")
    void runProducesExpectedOutput() throws Exception {
        new EvaluteVerbApplication().run();

        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadExpected(EXPECTED_FIXTURE);

        assertEquals(expected, actual,
                "Captured stdout must reflect EVALUATE TRUE WHEN NUM01 > 2 branch");
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
