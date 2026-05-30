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

package com.projectcobol.struct;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * JUnit 5 golden-output test for {@link StructApplication}, the Java 21 / Spring
 * Boot translation of {@code OpenCobol/Struct/Struct.cbl}.
 *
 * <p>This test captures everything {@link StructApplication#run(String...)}
 * writes to {@code System.out}, normalizes line endings to LF, and asserts
 * byte-for-byte equality against the pre-recorded golden fixture at
 * {@code src/test/resources/expected/StructApplication.txt}. The fixture mirrors
 * the {@code cobc}-compiled output of the original COBOL program: five
 * {@code "Array1 contains number: NN"} lines, a 25-hyphen separator, and ten
 * {@code "Array2 contains number: NN"} lines (16 lines total).
 *
 * <p>The test exercises the {@link org.springframework.boot.CommandLineRunner}
 * method directly via {@code new StructApplication().run()} rather than booting
 * a Spring context; the translation is a self-contained console program, so a
 * full application context would add no coverage and only slow the test.
 */
class StructApplicationTest {

    /** The real {@code System.out}, captured so {@link #tearDown()} can restore it. */
    private final PrintStream originalOut = System.out;

    /** In-memory buffer that receives redirected {@code System.out} during each test. */
    private ByteArrayOutputStream captured;

    /**
     * Redirects {@code System.out} into a fresh in-memory buffer before every
     * test so each test observes only its own output. The replacement
     * {@link PrintStream} uses {@code autoFlush=true} and UTF-8 so writes are
     * visible immediately and decode identically to the golden fixture.
     */
    @BeforeEach
    void setUp() {
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    /**
     * Restores the original {@code System.out} after every test so the
     * redirection never leaks into other test classes in the module run.
     */
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    /**
     * Asserts that the full stdout produced by {@link StructApplication#run(String...)}
     * matches the golden fixture exactly (after CRLF&rarr;LF normalization).
     *
     * @throws Exception propagated from {@link StructApplication#run(String...)}
     *                   (declared by the {@code CommandLineRunner} contract) and
     *                   from {@link #readExpectedFixture()}
     */
    @Test
    void runProducesExpectedGoldenOutput() throws Exception {
        StructApplication app = new StructApplication();
        app.run();

        String actual = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = readExpectedFixture();

        assertEquals(expected, actual,
            "StructApplication console output should match the golden fixture at "
                + "src/test/resources/expected/StructApplication.txt");
    }

    /**
     * Line-count sanity check guarding against off-by-one regressions in either
     * the fill/display loops or the fixture. The program emits exactly 16 lines:
     * five for {@code ARRAY-ONE}, one hyphen separator, and ten for
     * {@code ARRAY-TWO}; therefore the captured output contains exactly 16
     * newline characters.
     *
     * @throws Exception propagated from {@link StructApplication#run(String...)}
     */
    @Test
    void arrayOneAndArrayTwoBothEmitSixteenLines() throws Exception {
        StructApplication app = new StructApplication();
        app.run();

        String actual = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        long newlineCount = actual.chars().filter(ch -> ch == '\n').count();
        assertEquals(16L, newlineCount,
            "StructApplication should emit exactly 16 lines "
                + "(5 Array1 + 1 hyphen separator + 10 Array2)");
    }

    /**
     * Loads the golden-output fixture from the test classpath. Maven Surefire
     * copies {@code src/test/resources/expected/StructApplication.txt} to
     * {@code target/test-classes/expected/StructApplication.txt}, so the
     * relative resource path {@code expected/StructApplication.txt} (no leading
     * slash) resolves through the context class loader.
     *
     * @return the fixture content decoded as UTF-8 with line endings normalized to LF
     * @throws Exception if reading the fixture stream fails
     */
    private String readExpectedFixture() throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("expected/StructApplication.txt")) {
            assertNotNull(in,
                "Golden fixture expected/StructApplication.txt must be present on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
