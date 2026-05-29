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
 * JUnit 5 golden-output test for {@link RelationConditionApplication}.
 *
 * <p>This is a black-box behavioral assertion against the COBOL &rarr; Java
 * translation of {@code OpenCobol/Conditions/RelationCondition.cbl}
 * (PROGRAM-ID {@code RELATION-CONDITION}). It captures {@code System.out} while
 * invoking {@code RelationConditionApplication.run()} directly &mdash; with no
 * Spring {@code ApplicationContext}, per the AAP &sect;0.7.2 minimal-change
 * clause &mdash; normalizes line endings (CRLF&#x2192;LF) to make the assertion
 * platform-independent, and compares the captured output byte-for-byte against the
 * pre-recorded fixture at
 * {@code src/test/resources/expected/RelationConditionApplication.txt}.
 *
 * <h2>Behavioral specification (from the COBOL source)</h2>
 * <p>{@code RelationCondition.cbl} declares {@code 01 NUM01 PIC 9(9) VALUE 50}
 * and {@code 01 NUM02 PIC 9(9) VALUE 6}, then evaluates
 * {@code IF NUM01 IS GREATER THAN OR EQUAL TO NUM02}. Because {@code 50 >= 6} is
 * {@code TRUE}, the THEN branch fires and the program prints exactly one line:
 * {@code "NUMBER01 IS GREATER OR EQUAL THAN NUMBER02"}. The ELSE branch
 * ({@code DISPLAY 'NUMBER01 IS LESS THAN NUMBER02'}) is dead code for these fixed
 * {@code VALUE} clauses and therefore never appears in the golden output.
 *
 * <h2>Verbatim-literal preservation (the assertion's reason for existing)</h2>
 * <p>The COBOL display literal on {@code RelationCondition.cbl:L18} uses the
 * non-standard English phrasing {@code "GREATER OR EQUAL THAN"} (idiomatic English
 * would be {@code "GREATER THAN OR EQUAL TO"}). Per the AAP &sect;0.7.3
 * verbatim-preservation rule and the minimal-change clause, this original-author
 * quirk is preserved character-for-character in the production translation and is
 * <em>not</em> "corrected". This test enforces that discipline at the byte level:
 * the 43-byte fixture
 * ({@code "NUMBER01 IS GREATER OR EQUAL THAN NUMBER02"} + a single {@code LF})
 * makes the assertion fail if the production code ever drifts toward the more
 * natural &mdash; but, for this educational repository, incorrect &mdash; wording.
 *
 * <p>Per the AAP &sect;0.7.2 minimal-change clause, this class is fully
 * self-contained: no shared base classes, no helper utilities beyond the private
 * fixture loader below, and no shared fixture loaders across sub-modules. The
 * output-capture boilerplate is duplicated verbatim across the sibling
 * {@code cobol-conditions} test classes; this duplication is intentional and
 * required to keep each translation's verification independent.
 */
@DisplayName("RelationConditionApplication golden-output verification")
class RelationConditionApplicationTest {

    /**
     * Classpath location of the golden-output fixture. Resolved relative to the
     * test classpath root (Maven copies {@code src/test/resources/} onto the test
     * classpath), so {@code "expected/RelationConditionApplication.txt"} maps to
     * {@code src/test/resources/expected/RelationConditionApplication.txt}.
     */
    private static final String EXPECTED_FIXTURE = "expected/RelationConditionApplication.txt";

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
     * Verifies that {@code RelationConditionApplication.run()} prints exactly the
     * verbatim COBOL literal {@code "NUMBER01 IS GREATER OR EQUAL THAN NUMBER02"}
     * (followed by a single line separator), matching the golden fixture
     * byte-for-byte.
     *
     * <p>The production class is exercised as a plain POJO
     * ({@code new RelationConditionApplication().run()}); this deliberately bypasses
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
    @DisplayName("run() prints 'NUMBER01 IS GREATER OR EQUAL THAN NUMBER02' (verbatim COBOL literal)")
    void runProducesExpectedOutput() throws Exception {
        new RelationConditionApplication().run();

        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadExpected(EXPECTED_FIXTURE);

        assertEquals(expected, actual,
                "Captured stdout must preserve 'OR EQUAL THAN' (non-standard COBOL phrasing)");
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
