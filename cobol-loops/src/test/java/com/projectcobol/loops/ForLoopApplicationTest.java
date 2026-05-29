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

package com.projectcobol.loops;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * JUnit 5 golden-output test for {@link ForLoopApplication}.
 *
 * <p>Captures {@code System.out} while invoking {@code ForLoopApplication.run()}
 * directly (no Spring application context, per AAP &sect;0.7.2 minimal-change
 * clause) and asserts byte-identical equality with the classpath fixture
 * {@code expected/ForLoopApplication.txt}.
 *
 * <p>The fixture contains 331 lines (231 {@code UNTIL:} lines from the nested
 * {@code PERFORM UNTIL} idiom + 100 {@code VARYING:} lines from the nested
 * {@code PERFORM VARYING} idiom in {@code OpenCobol/Loops/ForLoop.cbl}).
 *
 * <p>Per AAP &sect;0.7.2 (minimal-change clause), this class is fully
 * self-contained &mdash; no shared base classes, no helper utilities, no shared
 * fixture loaders. Output capture uses the inline redirection pattern (a
 * {@code try}/{@code finally} inside the test method, matching the cobol-sort
 * tests) rather than shared {@code @BeforeEach}/{@code @AfterEach} hooks. The
 * output-capture boilerplate is duplicated verbatim in {@link WhileApplicationTest};
 * this duplication is intentional and required.
 */
class ForLoopApplicationTest {

    @Test
    void outputMatchesGoldenFixture() throws Exception {
        // Inline output-redirection pattern (matching the cobol-sort tests):
        // redirect System.out only for the duration of run(), and always restore
        // the original stream in finally so a failure cannot leak the redirected
        // stream into sibling tests.
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            new ForLoopApplication().run();
        } finally {
            System.setOut(originalOut);
        }

        // Normalize CRLF -> LF on both sides so the 331-line fixture comparison is
        // platform-independent.
        String actual = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("expected/ForLoopApplication.txt")) {
            assertNotNull(in, "Missing fixture expected/ForLoopApplication.txt");
            expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
        assertEquals(expected, actual);
    }
}
