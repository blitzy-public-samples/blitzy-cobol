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

package com.projectcobol.string;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Golden-output JUnit 5 test for {@link StringApplication}.
 *
 * <p>Captures {@code System.out} produced by invoking {@code StringApplication.run()},
 * normalizes line endings (CRLF&#x2192;LF) to make the assertion platform-independent,
 * and compares the captured output byte-for-byte against the pre-recorded fixture at
 * {@code src/test/resources/expected/StringApplication.txt}.
 *
 * <p>The fixture contains 10 lines, alternating between {@code H} and {@code O},
 * mirroring the COBOL substring iteration over {@code W-STRING = "HOHOHOHOHO"} per
 * {@code OpenCobol/String/String.cbl} (PROGRAM-ID {@code WORK-WITH-STRING}).
 */
class StringApplicationTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void redirectStdout() {
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void runProducesGoldenOutput() throws Exception {
        new StringApplication().run();

        String actual = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadFixture("expected/StringApplication.txt");

        assertEquals(expected, actual,
            "StringApplication console output should match the golden fixture at "
                + "src/test/resources/expected/StringApplication.txt");
    }

    private String loadFixture(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in,
                "Golden fixture " + path + " must be present on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
