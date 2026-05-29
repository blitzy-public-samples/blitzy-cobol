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
 * Golden-output JUnit 5 test for {@link StatusCodeApplication}.
 *
 * <p>This is the simplest of the four {@code cobol-database} test classes: it has
 * no {@code data.txt} dependency and asserts a single 12-byte fixture. The only
 * behavior under test is the Java translation of the COBOL {@code MAIN-PROCEDURE}
 * {@code DISPLAY "Hello world"} from {@code OpenCobol/Database/StatusCode.cbl}
 * (lines 41-44), which maps to {@code System.out.println("Hello world")}.
 *
 * <p>The production {@link StatusCodeApplication#run(String...)} method is invoked
 * directly via {@code new StatusCodeApplication().run(new String[]{})} rather than
 * through {@link org.springframework.boot.SpringApplication}, deliberately avoiding
 * the Spring Boot banner and INFO-level startup logging that would otherwise pollute
 * the captured {@code System.out} stream and break the byte-for-byte golden-output
 * contract.
 *
 * <p>Captured output is normalized from CRLF to LF so the assertion is independent
 * of the host platform's {@link System#lineSeparator()}, then compared byte-exactly
 * against the pre-recorded fixture at
 * {@code src/test/resources/expected/StatusCodeApplication.txt}.
 */
class StatusCodeApplicationTest {

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
     * Verifies that {@link StatusCodeApplication} prints exactly {@code Hello world}
     * followed by a single newline, matching the golden fixture and thereby the
     * behavior of the original COBOL {@code DISPLAY "Hello world"} statement.
     *
     * @throws Exception propagated from {@link StatusCodeApplication#run(String...)}
     *                   (declared on the {@code CommandLineRunner} contract) and from
     *                   the fixture-loading helper
     */
    @Test
    void producesExpectedGoldenOutput() throws Exception {
        // Invoke run() directly to avoid Spring Boot banner / INFO logging pollution.
        new StatusCodeApplication().run(new String[]{});

        // Load the pre-recorded expected fixture from the test classpath.
        String expected = loadExpectedFixture("expected/StatusCodeApplication.txt");

        // Normalize captured output for cross-platform determinism (Windows -> Unix LF).
        String actual = capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");

        assertEquals(expected, actual);
    }

    /**
     * Loads a UTF-8 text fixture from the test classpath.
     *
     * <p>Maven places {@code src/test/resources/} on the test classpath, so the
     * relative resource path {@code expected/StatusCodeApplication.txt} resolves to
     * {@code cobol-database/src/test/resources/expected/StatusCodeApplication.txt}.
     * The {@link InputStream} is closed via try-with-resources, and a missing fixture
     * fails the test loudly with a clear {@link IllegalStateException} rather than an
     * obscure {@link NullPointerException}.
     *
     * @param resourcePath classpath-relative path of the fixture, e.g.
     *                     {@code expected/StatusCodeApplication.txt}
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
