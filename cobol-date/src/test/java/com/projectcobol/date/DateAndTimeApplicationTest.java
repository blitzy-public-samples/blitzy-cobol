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

package com.projectcobol.date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden-output test for {@link DateAndTimeApplication}.
 *
 * <p>The COBOL source {@code OpenCobol/Date/DateAndTime.cbl} emits the runtime
 * clock and calendar via {@code ACCEPT FROM TIME} and {@code ACCEPT FROM DATE},
 * which makes its output environment-dependent. This test therefore loads a
 * fixture of <strong>regex patterns</strong> from the classpath resource
 * {@code /expected/DateAndTimeApplication.txt} and asserts each captured stdout
 * line matches its corresponding pattern.</p>
 *
 * <p>The five expected lines, in order, are:</p>
 * <ol>
 *   <li>{@code ^W-TIME: \d{8}$} &mdash; no space before colon, 8 digits HHmmssNN</li>
 *   <li>{@code ^W-DATE: \d{8}$} &mdash; no space before colon, 8 digits YYYYMMDD</li>
 *   <li>{@code ^W-BATCH: \d{16}$} &mdash; no space before colon, 16 digits</li>
 *   <li>{@code ^COMPLET : \d{8} \d{8}$} &mdash; <strong>space before colon</strong>, 8 + space + 8 digits</li>
 *   <li>{@code ^TEST : \d{16}$} &mdash; <strong>space before colon</strong>, 16 digits</li>
 * </ol>
 */
class DateAndTimeApplicationTest {

    private static final String EXPECTED_FIXTURE_RESOURCE = "/expected/DateAndTimeApplication.txt";

    private PrintStream originalSystemOut;
    private ByteArrayOutputStream capturedOutput;

    @BeforeEach
    void redirectStandardOutput() {
        this.originalSystemOut = System.out;
        this.capturedOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(this.capturedOutput, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStandardOutput() {
        System.setOut(this.originalSystemOut);
    }

    @Test
    void producesExpectedFiveLines() throws Exception {
        // Arrange: nothing extra — @BeforeEach already redirected stdout.

        // Act: invoke the application's CommandLineRunner.run() directly.
        // Direct instantiation is intentional per AAP §0.7.2 minimal-change clause:
        // avoiding @SpringBootTest keeps tests fast and isolated from Spring context startup.
        new DateAndTimeApplication().run(new String[0]);

        // Capture stdout text using explicit UTF-8 to avoid platform-charset warnings.
        String capturedText = this.capturedOutput.toString(StandardCharsets.UTF_8);
        String[] capturedLines = capturedText.split("\\R", -1);

        // Drop the trailing empty element introduced by a final line terminator
        // (System.out.println appends a line separator after the last DISPLAY line).
        List<String> nonEmptyCapturedLines = new ArrayList<>();
        for (String line : capturedLines) {
            if (!line.isEmpty()) {
                nonEmptyCapturedLines.add(line);
            }
        }

        // Load the regex fixture from the classpath.
        List<String> expectedRegexLines = loadExpectedRegexFixture();

        assertEquals(
            expectedRegexLines.size(),
            nonEmptyCapturedLines.size(),
            "Expected " + expectedRegexLines.size()
                + " stdout lines from DateAndTimeApplication, but captured "
                + nonEmptyCapturedLines.size() + ":\n" + capturedText
        );

        for (int i = 0; i < expectedRegexLines.size(); i++) {
            String regex = expectedRegexLines.get(i);
            String captured = nonEmptyCapturedLines.get(i);
            assertTrue(
                captured.matches(regex),
                "Line " + (i + 1) + " did not match expected regex.\n"
                    + "  Regex:    " + regex + "\n"
                    + "  Captured: " + captured
            );
        }
    }

    private List<String> loadExpectedRegexFixture() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(EXPECTED_FIXTURE_RESOURCE)) {
            assertNotNull(
                stream,
                "Classpath resource not found: " + EXPECTED_FIXTURE_RESOURCE
                    + ". Ensure cobol-date/src/test/resources/expected/DateAndTimeApplication.txt exists."
            );
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.strip();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        lines.add(trimmed);
                    }
                }
                return lines;
            }
        }
    }
}
