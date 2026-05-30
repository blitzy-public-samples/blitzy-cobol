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
package com.projectcobol.helloworld;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorldApplicationTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void runProducesExpectedOutput() throws Exception {
        new HelloWorldApplication().run();

        String actual = outputStream.toString(StandardCharsets.UTF_8);

        String expected;
        try (InputStream in = getClass().getResourceAsStream("/expected/HelloWorldApplication.txt")) {
            if (in == null) {
                throw new IllegalStateException(
                        "Missing classpath fixture: /expected/HelloWorldApplication.txt");
            }
            expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Byte-exact golden-output comparison (AAP §0.7.1): normalize only line
        // endings (CRLF -> LF) for cross-platform stability -- unlike trim(), this
        // does NOT mask missing/extra line terminators or surrounding whitespace,
        // preserving the byte-faithful golden-output guarantee.
        assertEquals(expected.replace("\r\n", "\n"), actual.replace("\r\n", "\n"));
    }
}
