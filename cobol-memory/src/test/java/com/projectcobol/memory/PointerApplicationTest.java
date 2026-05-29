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
 * Golden-output JUnit 5 test for {@link PointerApplication}.
 *
 * <p>Captures {@code System.out} produced by invoking {@code PointerApplication.run(...)}
 * and asserts that the output matches the pre-recorded fixture at
 * {@code src/test/resources/expected/PointerApplication.txt}.
 *
 * <p>Per AAP &sect;0.6.1, the two {@code W-POINTER} sentinel lines (lines 1 and 3) use
 * a regex format-match against {@code ^W-POINTER: 0x[0-9A-Fa-f]{8}$} because Java has
 * no equivalent runtime memory address; the deterministic data lines (2, 4, 5) are
 * compared with literal {@code assertEquals} equality against the fixture.
 *
 * <p>Per AAP &sect;0.7.2 minimal-change clause, line 5 ({@code NEXT-WORK-DATA : BBBB})
 * preserves the INCONSISTENT space-colon-space formatting from
 * {@code OpenCobol/Memory/Pointer.cbl} line 34 verbatim, while lines 1-4 use the
 * colon-space convention from Pointer.cbl lines 26, 31-33. The fixture and test
 * MUST NOT normalize this authentic COBOL formatting quirk.
 *
 * <p>Mirrors the behavior of {@code OpenCobol/Memory/Pointer.cbl}
 * (PROGRAM-ID {@code WORK-WITH-POINTER}).
 */
class PointerApplicationTest {

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
        PointerApplication app = new PointerApplication();
        app.run(new String[0]);

        String actual = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected = loadFixture("/expected/PointerApplication.txt");

        String[] actualLines = actual.split("\\r?\\n", -1);
        String[] expectedLines = expected.split("\\r?\\n", -1);

        assertEquals(expectedLines.length, actualLines.length,
            "Line count mismatch between fixture and PointerApplication output. "
                + "Expected " + expectedLines.length + " lines, got " + actualLines.length + ".");

        String wPointerPattern = "^W-POINTER: 0x[0-9A-Fa-f]{8}$";

        // Line 1 (0-indexed 0): W-POINTER sentinel line — regex match per AAP §0.6.1
        assertTrue(actualLines[0].matches(wPointerPattern),
            "Line 1 should match W-POINTER pointer sentinel format '"
                + wPointerPattern + "', got: '" + actualLines[0] + "'");

        // Line 2 (0-indexed 1): WORK-DATA — literal equality (colon-space convention)
        assertEquals(expectedLines[1], actualLines[1],
            "Line 2 (WORK-DATA) mismatch");

        // Line 3 (0-indexed 2): W-POINTER sentinel line — regex match per AAP §0.6.1
        assertTrue(actualLines[2].matches(wPointerPattern),
            "Line 3 should match W-POINTER pointer sentinel format '"
                + wPointerPattern + "', got: '" + actualLines[2] + "'");

        // Line 4 (0-indexed 3): WORK-A — literal equality (colon-space convention)
        assertEquals(expectedLines[3], actualLines[3],
            "Line 4 (WORK-A) mismatch");

        // Line 5 (0-indexed 4): NEXT-WORK-DATA — literal equality preserving the
        // INCONSISTENT space-colon-space spacing from Pointer.cbl line 34 verbatim
        // per AAP §0.7.2 minimal-change clause. Future maintainers MUST NOT
        // "normalize" this spacing to match lines 2 and 4.
        assertEquals(expectedLines[4], actualLines[4],
            "Line 5 (NEXT-WORK-DATA) mismatch — note the intentional space-colon-space "
                + "spacing preserved verbatim from Pointer.cbl line 34");
    }

    private String loadFixture(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in,
                "Golden fixture " + path + " must be present on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
