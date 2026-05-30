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
 * JUnit 5 golden-output test for {@link ClassConditionApplication}.
 *
 * <p>This is a black-box behavioral assertion against the COBOL &rarr; Java
 * translation of {@code OpenCobol/Conditions/ClassCondition.cbl}
 * (PROGRAM-ID {@code CLASS-CONDITION}). It captures {@code System.out} while
 * invoking {@code ClassConditionApplication.run()} directly &mdash; with no Spring
 * {@code ApplicationContext}, per the AAP &sect;0.7.2 minimal-change clause &mdash;
 * normalizes line endings (CRLF&#x2192;LF) to make the assertion platform-independent,
 * and compares the captured output byte-for-byte against the pre-recorded fixture at
 * {@code src/test/resources/expected/ClassConditionApplication.txt}.
 *
 * <h2>Behavioral specification (from the COBOL source)</h2>
 * <p>{@code ClassCondition.cbl} declares one signed 9-digit numeric and one 9-byte
 * alphanumeric field &mdash; {@code 01 NUM01 PIC S9(9) VALUE -5000} and
 * {@code 01 STR01 PIC X(9) VALUE 'ABCDF'} (right-space-padded to nine characters)
 * &mdash; then evaluates three sequential {@code IF} class-condition tests:
 * <ol>
 *   <li>{@code IF STR01 IS ALPHABETIC} &mdash; the padded {@code "ABCDF    "} is all
 *       letters and spaces, which COBOL treats as alphabetic, so this is
 *       {@code true} and the first {@code DISPLAY} fires
 *       ({@code "STR01 IS ALPHABETIC"}).</li>
 *   <li>{@code IF NUM01 IS NUMERIC} &mdash; {@code IS NUMERIC} on a signed
 *       {@code PIC S9(9)} item is structurally always {@code true}, so the second
 *       {@code DISPLAY} fires ({@code "NUM01 IS NUMERIC"}).</li>
 *   <li>{@code IF STR01 IS NUMERIC ... ELSE ...} &mdash; {@code STR01} contains
 *       letters, so {@code IS NUMERIC} is {@code false} and the {@code ELSE} branch
 *       fires ({@code "STR01 ISNT NUMERIC IS ALPHABETIC"}).</li>
 * </ol>
 * The program therefore prints exactly three lines, each terminated by a single
 * line separator.
 *
 * <h2>Preserved-spelling significance (the assertion's reason for existing)</h2>
 * <p>The {@code ELSE} branch's DISPLAY literal uses the source's non-standard
 * spelling {@code "ISNT"} (no apostrophe), reproduced byte-for-byte by the
 * production translation from {@code ClassCondition.cbl} line 28 per the AAP
 * minimal-change rule. The golden fixture is therefore exactly 70 bytes: the three
 * lines {@code "STR01 IS ALPHABETIC"} (19 chars), {@code "NUM01 IS NUMERIC"}
 * (16 chars) and {@code "STR01 ISNT NUMERIC IS ALPHABETIC"} (32 chars), each
 * followed by a single {@code LF} ({@code 0x0A}). To keep this byte-exact behavior
 * honest, this test performs a literal {@code assertEquals} with <strong>no</strong>
 * {@code trim()}/{@code strip()} of either operand &mdash; the only normalization
 * applied is CRLF&#x2192;LF, which never touches the {@code "ISNT"} spelling or the
 * line content. Were the production class to "correct" the spelling to
 * {@code "ISN'T"}, this assertion would fail with a clear byte-for-byte mismatch.
 *
 * <p>Per the AAP &sect;0.7.2 minimal-change clause, this class is fully
 * self-contained: no shared base classes, no helper utilities beyond the private
 * fixture loader below, and no shared fixture loaders across sub-modules. The
 * output-capture boilerplate is duplicated verbatim across the sibling
 * {@code cobol-conditions} test classes; this duplication is intentional and
 * required to keep each translation's verification independent.
 */
@DisplayName("ClassConditionApplication golden-output verification")
class ClassConditionApplicationTest {

    /**
     * Classpath location of the golden-output fixture. Resolved relative to the
     * test classpath root (Maven copies {@code src/test/resources/} onto the test
     * classpath), so {@code "expected/ClassConditionApplication.txt"} maps to
     * {@code src/test/resources/expected/ClassConditionApplication.txt}.
     */
    private static final String EXPECTED_FIXTURE = "expected/ClassConditionApplication.txt";

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
     * platform-independent capture (and avoids the platform-default-charset warning
     * that {@code -Xlint:all -Werror} would otherwise raise on the
     * {@code PrintStream} constructor).
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
     * Verifies that {@code ClassConditionApplication.run()} prints exactly the three
     * lines {@code "STR01 IS ALPHABETIC"}, {@code "NUM01 IS NUMERIC"} and
     * {@code "STR01 ISNT NUMERIC IS ALPHABETIC"} (the two fired {@code IF} branches
     * plus the {@code ELSE} branch of the third class condition), each terminated by
     * a single line separator, matching the golden fixture byte-for-byte.
     *
     * <p>The production class is exercised as a plain POJO
     * ({@code new ClassConditionApplication().run()}); this deliberately bypasses the
     * Spring container because the translation's observable behavior is pure console
     * output with no bean wiring. The captured output is normalized CRLF&#x2192;LF so
     * the assertion holds on every platform regardless of the line separator emitted
     * by {@code System.out.println}. Crucially, neither operand is trimmed or
     * stripped, so the source's preserved {@code "ISNT"} spelling is enforced at the
     * byte level (70 bytes total).
     *
     * @throws Exception propagated from {@code run(String...)} (declared by the
     *                   {@code CommandLineRunner} contract; this translation throws
     *                   nothing) and from fixture loading
     */
    @Test
    @DisplayName("run() produces the three lines captured in expected fixture")
    void runProducesExpectedOutput() throws Exception {
        new ClassConditionApplication().run();

        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadExpected(EXPECTED_FIXTURE);

        assertEquals(expected, actual,
                "Captured stdout must match golden-output fixture byte-for-byte");
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
