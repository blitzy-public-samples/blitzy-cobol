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

package com.projectcobol.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Golden-output JUnit 5 test for {@link OpenFileSequentialApplication}.
 *
 * <p>It exercises the Java translation of the COBOL {@code DISPLAY-DET-S}
 * paragraph from {@code OpenCobol/Database/OpenFileSequential.cbl} (lines
 * 64-67), which emits a single labeled {@code DISPLAY} line per record:
 * {@code DISPLAY "ID: " DET-ID " STR: " DET-TIME " DET-NUM: " DET-NUM}. The
 * production class reads the 36-record classpath fixture {@code data.txt},
 * tokenizes each line by whitespace into the {@code DET-ID}, {@code DET-TIME}
 * and {@code DET-NUM} fields and formats them with {@code %-5s} / {@code %-5s}
 * / {@code %-6s} to enforce the COBOL {@code PIC X} widths (5 / 5 / 6) declared
 * in the {@code DETAILS} record, so the captured output is exactly
 * {@code 36 records = 36 lines}.
 *
 * <p>Java's {@code %-Ns} format left-justifies and right-pads with spaces but
 * does NOT truncate, so the fixture is a faithful, mixed-width capture of the
 * COBOL data rather than a uniform 36-character block. Thirty-four records emit
 * the standard 36-character line; the two records whose {@code DET-TIME} token
 * is six characters ({@code DATa45}, {@code DATA50}) emit a 37-character line;
 * and the single record whose {@code DET-NUM} token is five characters
 * ({@code 02061}) is right-padded to the COBOL six-character width
 * ({@code "02061 "}). The resulting fixture is therefore
 * {@code 34 * 36 + 2 * 37 + 36 line feeds = 1334} bytes.
 *
 * <p>The production {@link OpenFileSequentialApplication#run(String...)} method
 * is invoked directly via {@code new OpenFileSequentialApplication().run(new
 * String[]{})} rather than through {@link org.springframework.boot.SpringApplication},
 * deliberately avoiding the Spring Boot banner and INFO-level startup logging
 * that would otherwise pollute the captured {@code System.out} stream and break
 * the byte-for-byte golden-output contract.
 *
 * <p>Captured output is normalized from CRLF to LF so the assertion is
 * independent of the host platform's {@link System#lineSeparator()}, then
 * compared byte-exactly against the pre-recorded fixture at
 * {@code src/test/resources/expected/OpenFileSequentialApplication.txt}.
 */
class OpenFileSequentialApplicationTest {

    /** In-memory buffer that captures everything written to {@code System.out} during a test. */
    private ByteArrayOutputStream capturedOut;

    /** The real {@code System.out}, saved in {@link #redirectStdout()} and restored in {@link #restoreStdout()}. */
    private PrintStream originalOut;

    /**
     * Redirects {@code System.out} into an in-memory buffer before each test so the
     * application's console output can be captured and asserted. A fresh buffer is
     * allocated per test to guarantee state isolation between test methods. The
     * replacement {@link PrintStream} uses the three-argument constructor with
     * {@code autoFlush=true} and an explicit {@link StandardCharsets#UTF_8} charset so
     * writes are visible immediately and decoded deterministically.
     */
    @BeforeEach
    void redirectStdout() {
        capturedOut = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    /**
     * Restores the original {@code System.out} after each test so subsequent tests
     * (and JUnit's own reporting) emit to the real console.
     */
    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    /**
     * Verifies that {@link OpenFileSequentialApplication} prints exactly the 36 lines
     * of the golden fixture (one labeled line per record), matching the behavior of
     * the original COBOL {@code DISPLAY-DET-S} paragraph
     * ({@code OpenCobol/Database/OpenFileSequential.cbl:L64-67}).
     *
     * @throws Exception propagated from {@link OpenFileSequentialApplication#run(String...)}
     *                   (declared on the {@code CommandLineRunner} contract) and from
     *                   the fixture-loading helper
     */
    @Test
    void producesExpectedGoldenOutput() throws Exception {
        // Invoke run() directly to avoid Spring Boot banner / INFO logging pollution.
        new OpenFileSequentialApplication().run(new String[]{});

        // Load the pre-recorded expected fixture from the test classpath.
        String expected = loadExpectedFixture("expected/OpenFileSequentialApplication.txt");

        // Normalize captured output for cross-platform determinism (Windows -> Unix LF).
        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");

        assertEquals(expected, actual);
    }

    /**
     * Loads a UTF-8 text fixture from the test classpath.
     *
     * <p>Maven places {@code src/test/resources/} on the test classpath, so the
     * relative resource path {@code expected/OpenFileSequentialApplication.txt} resolves
     * to {@code cobol-database/src/test/resources/expected/OpenFileSequentialApplication.txt}.
     * The {@link InputStream} is closed via try-with-resources, and a missing fixture
     * fails the test loudly with a clear {@link IllegalStateException} rather than an
     * obscure {@link NullPointerException}.
     *
     * @param resourcePath classpath-relative path of the fixture, e.g.
     *                     {@code expected/OpenFileSequentialApplication.txt}
     * @return the decoded fixture content
     * @throws Exception if the resource bytes cannot be read (for example, an
     *                   {@code IOException} from {@link InputStream#readAllBytes()})
     * @throws IllegalStateException if the fixture is not present on the test classpath
     */
    private String loadExpectedFixture(String resourcePath) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Expected fixture not found on test classpath: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
