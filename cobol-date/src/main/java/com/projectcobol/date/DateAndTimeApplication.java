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

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/Date/DateAndTime.cbl}.
 *
 * <p>The original COBOL program {@code DATE-TIME} demonstrates runtime clock and
 * calendar access via the {@code ACCEPT FROM TIME} and {@code ACCEPT FROM DATE}
 * verbs, renders the captured values with {@code DISPLAY}, and reshapes them
 * across grouped working-storage buffers with {@code STRING} and {@code MOVE}.
 * The program emits five fixed-format lines to stdout.</p>
 *
 * <p>This Java translation preserves the COBOL behavior exactly (byte-for-byte,
 * per AAP &sect;0.7.1). In particular the COBOL
 * {@code STRING W-DATE DELIMITED BY SPACE W-TIME DELIMITED BY SPACE INTO
 * W-BATCH-COMPLET} concatenates the two operands with NO embedded separator:
 * {@code DELIMITED BY SPACE} controls where copying STOPS within each source
 * operand, not what is inserted into the destination, and since both operands are
 * eight non-space digits the result is the plain concatenation
 * {@code W-DATE || W-TIME} (16 digits). The COBOL {@code ACCEPT}-from-clock verbs
 * map to the idiomatic {@code java.time} API: {@link LocalTime#now()} formatted
 * with {@code HHmmssSS} and {@link LocalDate#now()} formatted with
 * {@code yyyyMMdd}, each yielding the 8-character output the COBOL runtime
 * produces.</p>
 *
 * <p>COBOL &rarr; Java translation rules applied:</p>
 * <ul>
 *   <li>{@code 01 W-BATCH-COMPLET PIC X(16)} &rarr; local {@code String wBatchComplet}
 *       populated via direct concatenation {@code wDate + wTime} (the
 *       {@code STRING ... DELIMITED BY SPACE} inserts no separator).</li>
 *   <li>{@code 01 W-BATCH-TEST PIC X(16)} &rarr; local {@code String wBatchTest}
 *       populated via direct concatenation {@code wDate + wTime} (group-level MOVE).</li>
 *   <li>{@code 01 W-BATCH. 03 W-DATE PIC X(8). 03 W-TIME PIC X(8).} &rarr; two local
 *       {@code String} variables {@code wDate} and {@code wTime}; the group is modeled
 *       as the concatenation {@code wDate + wTime} when displayed as {@code W-BATCH}.</li>
 *   <li>{@code ACCEPT W-TIME FROM TIME} &rarr;
 *       {@code LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmssSS"))}.</li>
 *   <li>{@code ACCEPT W-DATE FROM DATE YYYYMMDD} &rarr;
 *       {@code LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.</li>
 *   <li>{@code DISPLAY} &rarr; {@link java.io.PrintStream#println(String)} on
 *       {@code System.out}.</li>
 *   <li>{@code GOBACK} &rarr; the implicit return at the end of {@link #run(String...)}.</li>
 * </ul>
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the
 * source filename ({@code DateAndTime.cbl} &rarr; {@code DateAndTimeApplication})
 * rather than the COBOL {@code PROGRAM-ID} ({@code DATE-TIME}, which would otherwise
 * suggest {@code DateTimeApplication}).</p>
 *
 * <p>See {@code OpenCobol/Date/DateAndTime.cbl} for the authoritative behavioral
 * reference; the original {@code .cbl} file remains in place and untouched per the
 * Project COBOL minimal-change clause.</p>
 */
@SpringBootApplication
public class DateAndTimeApplication implements CommandLineRunner {

    /**
     * Spring Boot bootstrap entry point. Builds a {@link SpringApplication} for
     * this class with the Spring Boot banner and startup/profile logging disabled
     * (via {@link Banner.Mode#OFF} and
     * {@link SpringApplication#setLogStartupInfo(boolean)}), then runs it. This
     * suppresses the ASCII-art banner and the {@code Starting…}/
     * {@code No active profile set}/{@code Started…} INFO lines so the executable
     * jar emits ONLY the COBOL-translated stdout, keeping the documented
     * {@code java -jar} run golden-output-clean. The {@link CommandLineRunner}
     * lifecycle (which invokes this class's {@link #run(String...)} method) is
     * preserved unchanged.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by
     *             this translation, but required by the Spring Boot contract)
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DateAndTimeApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code DateAndTime.cbl} lines 18-37.
     *
     * <p>Captures the current time and date, then emits the five DISPLAY-equivalent
     * lines in the original order: {@code W-TIME:}, {@code W-DATE:}, {@code W-BATCH:},
     * {@code COMPLET :}, and {@code TEST :}. The leading-literal spacing is preserved
     * verbatim from the COBOL source: the first three labels have no space before the
     * colon, while {@code COMPLET :} and {@code TEST :} include a space before the colon.</p>
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner} contract;
     *                   this translation never actually throws
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: ACCEPT W-TIME OF W-BATCH FROM TIME.                       (DateAndTime.cbl L20)
        // COBOL ACCEPT ... FROM TIME yields HHMMSSNN (8 chars; NN = hundredths of a second).
        // DateTimeFormatter pattern: HH = 24-hour, mm = minute, ss = second,
        // SS = 2-digit fraction-of-second (hundredths of a second) => exactly 8 digits.
        String wTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmssSS"));

        // COBOL: ACCEPT W-DATE OF W-BATCH FROM DATE YYYYMMDD.              (DateAndTime.cbl L21)
        // COBOL ACCEPT ... FROM DATE YYYYMMDD yields YYYYMMDD (8 chars).
        String wDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // COBOL: DISPLAY "W-TIME: " W-TIME OF W-BATCH.                     (DateAndTime.cbl L23)
        System.out.println("W-TIME: " + wTime);

        // COBOL: DISPLAY "W-DATE: " W-DATE OF W-BATCH.                     (DateAndTime.cbl L24)
        System.out.println("W-DATE: " + wDate);

        // COBOL: DISPLAY "W-BATCH: " W-BATCH.                              (DateAndTime.cbl L25)
        // W-BATCH is the 16-char group containing W-DATE concatenated with W-TIME.
        // A COBOL group DISPLAY emits the concatenated raw bytes with no separator.
        System.out.println("W-BATCH: " + wDate + wTime);

        // COBOL: STRING W-DATE OF W-BATCH DELIMITED BY SPACE
        //               W-TIME OF W-BATCH DELIMITED BY SPACE
        //          INTO W-BATCH-COMPLET
        //        END-STRING.                                              (DateAndTime.cbl L27-30)
        // COBOL STRING ... DELIMITED BY SPACE does NOT insert a separator into the
        // destination: "DELIMITED BY <delimiter>" specifies where to STOP copying
        // from each SOURCE operand (i.e. copy up to, but not including, the first
        // space). Because both W-DATE (YYYYMMDD) and W-TIME (HHMMSSNN) are eight
        // non-space digits, each is copied in full and the result is the plain
        // concatenation W-DATE || W-TIME with no embedded space. The byte-exact
        // behavior-preservation rule (AAP §0.7.1) takes precedence over the
        // String.join(" ", ...) example sketched in AAP §0.4.1, which would
        // incorrectly inject a space and produce 17 chars instead of 16.
        String wBatchComplet = wDate + wTime;

        // COBOL: MOVE W-BATCH TO W-BATCH-TEST.                            (DateAndTime.cbl L32)
        // A group-level MOVE is a byte-for-byte copy of the 16-char W-BATCH structure
        // (W-DATE concatenated with W-TIME, no separator). This yields the same 16-digit
        // value as W-BATCH-COMPLET above, since the STRING ... DELIMITED BY SPACE that
        // produced W-BATCH-COMPLET likewise inserts no separator.
        String wBatchTest = wDate + wTime;

        // COBOL: DISPLAY "COMPLET : " W-BATCH-COMPLET.                    (DateAndTime.cbl L34)
        // Note: the COBOL literal has a SPACE before the colon ("COMPLET : ").
        System.out.println("COMPLET : " + wBatchComplet);

        // COBOL: DISPLAY "TEST : " W-BATCH-TEST                          (DateAndTime.cbl L35)
        // Note: the COBOL literal has a SPACE before the colon ("TEST : ").
        System.out.println("TEST : " + wBatchTest);

        // COBOL: GOBACK.                                                  (DateAndTime.cbl L37)
        // The run() method returns naturally; Spring Boot exits when the
        // CommandLineRunner completes.
    }
}
