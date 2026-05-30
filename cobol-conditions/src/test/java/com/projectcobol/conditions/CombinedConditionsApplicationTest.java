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
 * JUnit 5 golden-output test for {@link CombinedConditionsApplication}.
 *
 * <p>This is a black-box behavioral assertion against the COBOL &rarr; Java
 * translation of {@code OpenCobol/Conditions/CombinedConditions.cbl}
 * (PROGRAM-ID {@code COMBINED-CONDITIONS}). It captures {@code System.out} while
 * invoking {@code CombinedConditionsApplication.run()} directly &mdash; with no
 * Spring {@code ApplicationContext}, per the AAP &sect;0.7.2 minimal-change
 * clause &mdash; normalizes line endings (CRLF&#x2192;LF) to make the assertion
 * platform-independent, and compares the captured output byte-for-byte against the
 * pre-recorded fixture at
 * {@code src/test/resources/expected/CombinedConditionsApplication.txt}.
 *
 * <h2>Behavioral specification (from the COBOL source)</h2>
 * <p>{@code CombinedConditions.cbl} declares three unsigned 3-digit numerics with
 * immutable {@code VALUE} clauses &mdash; {@code 01 NUM01 PIC 9(3) VALUE 50},
 * {@code 01 NUM02 PIC 9(3) VALUE 20}, {@code 01 NUM03 PIC 9(3) VALUE 30} &mdash;
 * then evaluates the compound condition
 * {@code IF NUM01 IS LESS THAN NUM02 AND NUM01 = NUM03}. With {@code NUM01 = 50}
 * and {@code NUM02 = 20}, the first conjunct {@code 50 < 20} is {@code false}.
 * COBOL's {@code AND} and Java's {@code &&} both short-circuit, so the second
 * conjunct {@code NUM01 = NUM03} ({@code 50 = 30}) is never evaluated; the overall
 * condition is {@code false} and the {@code ELSE} branch fires. The program prints
 * exactly one line, {@code "I AM HERE :( "}. The THEN branch
 * ({@code DISPLAY "I AM HERE!"}) is dead code for these fixed {@code VALUE} clauses
 * and therefore never appears in the golden output.
 *
 * <h2>Trailing-space significance (the assertion's reason for existing)</h2>
 * <p>The {@code ELSE} branch's DISPLAY literal carries a single trailing space
 * <em>inside</em> the quotes ({@code CombinedConditions.cbl:L21}:
 * {@code DISPLAY "I AM HERE :( "}), reproduced byte-for-byte by the production
 * translation. The golden fixture is therefore exactly 14 bytes: the 13 characters
 * {@code "I AM HERE :( "} (the last of which is a space, {@code 0x20}) plus a single
 * {@code LF} ({@code 0x0A}) &mdash; the byte sequence
 * {@code 49 20 41 4D 20 48 45 52 45 20 3A 28 20 0A}. To keep this byte-exact
 * behavior honest, this test performs a literal {@code assertEquals} with
 * <strong>no</strong> {@code trim()}/{@code strip()} of either operand &mdash; the
 * only normalization applied is CRLF&#x2192;LF, which never touches the
 * trailing-space distinction. Were the production class to strip the trailing space,
 * its captured output would be 13 bytes ({@code ...0x28 0x0A}) and this assertion
 * would fail with a clear byte-for-byte mismatch.
 *
 * <p>Per the AAP &sect;0.7.2 minimal-change clause, this class is fully
 * self-contained: no shared base classes, no helper utilities beyond the private
 * fixture loader below, and no shared fixture loaders across sub-modules. The
 * output-capture boilerplate is duplicated verbatim across the sibling
 * {@code cobol-conditions} test classes; this duplication is intentional and
 * required to keep each translation's verification independent.
 */
@DisplayName("CombinedConditionsApplication golden-output verification")
class CombinedConditionsApplicationTest {

    /**
     * Classpath location of the golden-output fixture. Resolved relative to the
     * test classpath root (Maven copies {@code src/test/resources/} onto the test
     * classpath), so {@code "expected/CombinedConditionsApplication.txt"} maps to
     * {@code src/test/resources/expected/CombinedConditionsApplication.txt}.
     */
    private static final String EXPECTED_FIXTURE = "expected/CombinedConditionsApplication.txt";

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
     * Verifies that {@code CombinedConditionsApplication.run()} prints exactly
     * {@code "I AM HERE :( "} (note the byte-significant trailing space, followed by
     * a single line separator) &mdash; the {@code ELSE} branch of the short-circuit
     * compound condition {@code IF NUM01 < NUM02 AND NUM01 == NUM03} &mdash; matching
     * the golden fixture byte-for-byte.
     *
     * <p>The production class is exercised as a plain POJO
     * ({@code new CombinedConditionsApplication().run()}); this deliberately bypasses
     * the Spring container because the translation's observable behavior is pure
     * console output with no bean wiring. The captured output is normalized
     * CRLF&#x2192;LF so the assertion holds on every platform regardless of the line
     * separator emitted by {@code System.out.println}. Crucially, neither operand is
     * trimmed or stripped, so the presence of the trailing space in the {@code ELSE}
     * literal is enforced at the byte level (14 bytes total).
     *
     * @throws Exception propagated from {@code run(String...)} (declared by the
     *                   {@code CommandLineRunner} contract; this translation throws
     *                   nothing) and from fixture loading
     */
    @Test
    @DisplayName("run() produces 'I AM HERE :( ' (with trailing space) on ELSE branch")
    void runProducesExpectedOutput() throws Exception {
        new CombinedConditionsApplication().run();

        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadExpected(EXPECTED_FIXTURE);

        assertEquals(expected, actual,
                "Captured stdout must include the trailing space inside the COBOL literal");
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
