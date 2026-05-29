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
 * Golden-output JUnit 5 test for {@link OpenFileRecordKeyApplication}.
 *
 * <p>Exercises the Java translation of the COBOL {@code READ-FILE SECTION} from
 * {@code OpenCobol/Database/OpenFileRecordKey.cbl} (lines 52-62), which performs a
 * {@code PERFORM UNTIL EOF = 'Y'} loop and emits one {@code DISPLAY MY-DATA-STRUCT}
 * line per record ({@code OpenFileRecordKey.cbl:L58}). The COBOL
 * {@code MY-DATA-STRUCT} group ({@code OpenFileRecordKey.cbl:L28-31}) is a fixed
 * 25-character layout composed of {@code DATA-ID PIC X(5)},
 * {@code DATA-NAME PIC X(10)} and {@code DATA-TIME PIC X(10)}. The production class
 * reads the 36-record classpath fixture {@code data.txt}, tokenizes each line by
 * whitespace and re-emits it through {@code String.format("%-5s%-10s%-10s", ...)},
 * so the captured output is exactly {@code 36 records × 1 line = 36 lines}
 * ({@code 936} bytes: 36 × (25 chars + LF)).
 *
 * <p>The production {@link OpenFileRecordKeyApplication#run(String...)} method is
 * invoked directly via {@code new OpenFileRecordKeyApplication().run(new String[]{})}
 * rather than through {@link org.springframework.boot.SpringApplication},
 * deliberately avoiding the Spring Boot banner and INFO-level startup logging that
 * would otherwise pollute the captured {@code System.out} stream and break the
 * byte-for-byte golden-output contract.
 *
 * <p>Captured output is normalized from CRLF to LF so the assertion is independent
 * of the host platform's {@link System#lineSeparator()}, then compared byte-exactly
 * against the pre-recorded fixture at
 * {@code src/test/resources/expected/OpenFileRecordKeyApplication.txt}.
 */
class OpenFileRecordKeyApplicationTest {

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
     * Verifies that {@link OpenFileRecordKeyApplication} prints exactly the 36 lines of
     * the golden fixture (one 25-character {@code MY-DATA-STRUCT} line per record),
     * matching the behavior of the original COBOL {@code DISPLAY MY-DATA-STRUCT}
     * statement in the {@code READ-FILE SECTION}
     * ({@code OpenCobol/Database/OpenFileRecordKey.cbl:L54-58}).
     *
     * @throws Exception propagated from {@link OpenFileRecordKeyApplication#run(String...)}
     *                   (declared on the {@code CommandLineRunner} contract) and from
     *                   the fixture-loading helper
     */
    @Test
    void producesExpectedGoldenOutput() throws Exception {
        // Invoke run() directly to avoid Spring Boot banner / INFO logging pollution.
        new OpenFileRecordKeyApplication().run(new String[]{});

        // Load the pre-recorded expected fixture from the test classpath.
        String expected = loadExpectedFixture("expected/OpenFileRecordKeyApplication.txt");

        // Normalize captured output for cross-platform determinism (Windows -> Unix LF).
        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");

        assertEquals(expected, actual);
    }

    /**
     * Loads a UTF-8 text fixture from the test classpath.
     *
     * <p>Maven places {@code src/test/resources/} on the test classpath, so the
     * relative resource path {@code expected/OpenFileRecordKeyApplication.txt} resolves
     * to
     * {@code cobol-database/src/test/resources/expected/OpenFileRecordKeyApplication.txt}.
     * The {@link InputStream} is closed via try-with-resources, and a missing fixture
     * fails the test loudly with a clear {@link IllegalStateException} rather than an
     * obscure {@link NullPointerException}.
     *
     * @param resourcePath classpath-relative path of the fixture, e.g.
     *                     {@code expected/OpenFileRecordKeyApplication.txt}
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
