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

package com.projectcobol.memory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden-output JUnit 5 test for {@link AddressApplication}.
 *
 * <p>Captures {@code System.out} produced by invoking {@code AddressApplication.run(...)}
 * and asserts that the output matches the pre-recorded fixture at
 * {@code src/test/resources/expected/AddressApplication.txt}.
 *
 * <p>Per AAP &sect;0.6.1, the two {@code WK-PTR} sentinel lines (lines 1 and 3) use a
 * regex format-match against {@code ^WK-PTR :   0x[0-9A-Fa-f]{8}$} because Java has
 * no equivalent runtime memory address; the deterministic data lines (2, 4, 5) are
 * compared with literal {@code assertEquals} equality against the fixture.
 *
 * <p>Mirrors the behavior of {@code OpenCobol/Memory/Address.cbl}
 * (PROGRAM-ID {@code WORK-OFFSET}).
 */
class AddressApplicationTest {

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
    public void runsAndMatchesGoldenOutput() throws Exception {
        AddressApplication app = new AddressApplication();
        app.run(new String[0]);

        String actual = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadFixture("/expected/AddressApplication.txt");

        String[] actualLines = actual.split("\\r?\\n", -1);
        String[] expectedLines = expected.split("\\r?\\n", -1);

        assertEquals(expectedLines.length, actualLines.length,
            "Line count mismatch between fixture and AddressApplication output. "
                + "Expected " + expectedLines.length + " lines, got " + actualLines.length + ".");

        String wkPtrPattern = "^WK-PTR :   0x[0-9A-Fa-f]{8}$";

        // Line 1 (0-indexed 0): WK-PTR sentinel line — regex match per AAP §0.6.1
        assertTrue(actualLines[0].matches(wkPtrPattern),
            "Line 1 should match WK-PTR pointer sentinel format '"
                + wkPtrPattern + "', got: '" + actualLines[0] + "'");

        // Line 2 (0-indexed 1): WORK-DATA — literal equality
        assertEquals(expectedLines[1], actualLines[1],
            "Line 2 (WORK-DATA) mismatch");

        // Line 3 (0-indexed 2): WK-PTR sentinel line — regex match per AAP §0.6.1
        assertTrue(actualLines[2].matches(wkPtrPattern),
            "Line 3 should match WK-PTR pointer sentinel format '"
                + wkPtrPattern + "', got: '" + actualLines[2] + "'");

        // Line 4 (0-indexed 3): WORK-A — literal equality
        assertEquals(expectedLines[3], actualLines[3],
            "Line 4 (WORK-A) mismatch");

        // Line 5 (0-indexed 4): NEXT-WORK-DATA — literal equality
        assertEquals(expectedLines[4], actualLines[4],
            "Line 5 (NEXT-WORK-DATA) mismatch");
    }

    private String loadFixture(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in,
                "Golden fixture " + path + " must be present on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
