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

package com.projectcobol.sort;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

class SelectSortApplicationTest {

    private static final long FIXED_SEED = 42L;

    @Test
    void outputMatchesGoldenFixture() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            SelectSortApplication app = new SelectSortApplication(FIXED_SEED);
            app.run();
        } finally {
            System.setOut(originalOut);
        }
        String actual = buf.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        String expected;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("expected/SelectSortApplication.txt")) {
            assertNotNull(in, "Missing fixture expected/SelectSortApplication.txt");
            expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
        assertEquals(expected, actual);
    }
}
