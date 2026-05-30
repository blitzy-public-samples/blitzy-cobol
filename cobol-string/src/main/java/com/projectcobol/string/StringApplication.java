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

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/String/String.cbl}.
 *
 * <p>The original COBOL program {@code WORK-WITH-STRING} demonstrates substring
 * reference modification by iterating over each character of the 10-character
 * literal {@code "HOHOHOHOHO"} and displaying it on its own line. The Java
 * translation preserves the COBOL pedagogical pattern with the following
 * mappings:
 * <ul>
 *   <li>{@code 01 W-STRING PIC X(10) VALUE "HOHOHOHOHO"} becomes a class-level
 *       {@code private final String wString = "HOHOHOHOHO"}.</li>
 *   <li>{@code 01 W-COUNT PIC 999} (used only as a loop counter) becomes the
 *       loop-local variable {@code i} declared inside the {@code for}
 *       statement.</li>
 *   <li>{@code PERFORM VARYING W-COUNT FROM 1 BY 1 UNTIL W-COUNT > 10} becomes
 *       {@code for (int i = 0; i < 10; i++)}. Note the 1-based-to-0-based
 *       offset translation: COBOL iterates {@code W-COUNT} from 1 to 10
 *       inclusive; Java iterates {@code i} from 0 to 9 inclusive.</li>
 *   <li>{@code DISPLAY W-STRING(W-COUNT:1)} becomes
 *       {@code System.out.println(wString.substring(i, i + 1))}. COBOL's
 *       {@code (position:length)} reference modification is 1-based; Java's
 *       {@code substring(beginIndex, endIndex)} is 0-based with an exclusive
 *       end index, hence the {@code (i, i + 1)} bounds.</li>
 *   <li>{@code GOBACK} becomes the implicit method return at the end of
 *       {@link #run(String...)}.</li>
 * </ul>
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the
 * source filename ({@code String.cbl} &rarr; {@code StringApplication}) rather
 * than the COBOL {@code PROGRAM-ID} ({@code WORK-WITH-STRING}).
 */
@SpringBootApplication
public class StringApplication implements CommandLineRunner {

    // COBOL: 01 W-STRING PIC X(10) VALUE "HOHOHOHOHO".  (String.cbl L12)
    // Java: class-level field with equivalent initialization. The COBOL PIC X(10)
    // fixed-width type is naturally satisfied by the literal's 10 characters.
    private final String wString = "HOHOHOHOHO";

    /**
     * Spring Boot bootstrap entry point. Delegates to
     * {@link SpringApplication#run(Class, String[])}, which constructs the
     * application context and invokes every {@link CommandLineRunner} bean,
     * including this class's {@link #run(String...)} method.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by
     *             this translation, but required by the Spring Boot contract)
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(StringApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code String.cbl} lines 15-21:
     * <pre>
     *     PERFORM VARYING W-COUNT FROM 1 BY 1 UNTIL W-COUNT &gt; 10
     *       DISPLAY W-STRING(W-COUNT:1)
     *     END-PERFORM
     *     GOBACK.
     * </pre>
     * <p>The COBOL {@code GOBACK} maps to the implicit method return after the
     * loop. The 1-based COBOL reference modification {@code W-STRING(W-COUNT:1)}
     * (position {@code W-COUNT}, length {@code 1}) maps to the 0-based Java
     * {@code wString.substring(i, i + 1)} (begin index {@code i}, end index
     * {@code i + 1} exclusive).
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract; this translation never actually throws
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: PERFORM VARYING W-COUNT FROM 1 BY 1 UNTIL W-COUNT > 10
        //          DISPLAY W-STRING(W-COUNT:1)
        //        END-PERFORM.    (String.cbl L17-19)
        //
        // The COBOL loop runs W-COUNT = 1, 2, ..., 10 (inclusive). At each
        // iteration, W-STRING(W-COUNT:1) extracts one character at the 1-based
        // position W-COUNT. The Java loop runs i = 0, 1, ..., 9 (inclusive),
        // and wString.substring(i, i + 1) extracts the same character using
        // 0-based indexing.
        for (int i = 0; i < 10; i++) {
            System.out.println(wString.substring(i, i + 1));
        }
        // COBOL: GOBACK. (String.cbl L21) -> implicit Java method return
    }
}
