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

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Java 21 / Spring Boot 3.x translation of
 * {@code OpenCobol/Database/OpenFileSequential.cbl} (PROGRAM-ID
 * {@code OPEN-FILE-SEQUENTIAL}).
 *
 * <p>Reads the classpath-loaded {@code data.txt} fixture and emits one labeled
 * record per line, matching the COBOL
 * {@code DISPLAY "ID: " DET-ID " STR: " DET-TIME " DET-NUM: " DET-NUM} statement
 * on {@code OpenFileSequential.cbl:L67}. Each line in {@code data.txt} is
 * normalized to the fixed COBOL record width (right-padded with spaces, or
 * truncated, to 16 characters), then the {@code DET-ID}, {@code DET-TIME} and
 * {@code DET-NUM} fields of the COBOL {@code DETAILS} record
 * ({@code OpenFileSequential.cbl:L21-26}) are extracted by their fixed COBOL byte
 * offsets ({@code [0,5)}, {@code [5,10)}, {@code [10,16)}) via
 * {@link String#substring(int, int)}, preserving the COBOL {@code PIC X} widths
 * (5 / 5 / 6 = 16). Fixed-offset extraction is required by AAP &sect;0.4.1
 * (&quot;record fields &rarr; fixed-offset {@code substring} extractions&quot;) and
 * keeps the field boundaries on COBOL byte positions rather than re-flowing
 * whitespace-delimited tokens.
 *
 * <p>The COBOL original opens the fixed-width file {@code ../data.dat} via
 * {@code OPEN INPUT} ({@code OpenFileSequential.cbl:L12, L36}); this Java
 * translation instead reads the text fixture {@code data.txt} from the
 * classpath per AAP &sect;0.4.1 row 11, replacing the COBOL sequential
 * {@code READ ... AT END SET EOF-T TO TRUE} loop with a
 * {@link BufferedReader#readLine()} iteration.
 *
 * <p>The COBOL {@code FILE STATUS} check
 * ({@code OpenFileSequential.cbl:L38-41}) that aborts with an error message
 * when the file cannot be opened is mirrored here using the same-package
 * {@link FileStatus} enum: a missing classpath resource is treated as the COBOL
 * {@code FILE-STATUS} "file not found" code and produces the same early-exit
 * behaviour ({@code DISPLAY "Error opening the DB file, program will exit." GOBACK}).
 *
 * <p>Per the AAP &sect;0.7.3 filename-traceability rule, the Java class name
 * follows the source {@code .cbl} filename ({@code OpenFileSequential.cbl}
 * &rarr; {@code OpenFileSequentialApplication}) rather than the COBOL
 * {@code PROGRAM-ID} ({@code OPEN-FILE-SEQUENTIAL}).
 */
// Multi-application isolation: cobol-database hosts four @SpringBootApplication
// entry classes in the same package com.projectcobol.database (one per COBOL
// program in OpenCobol/Database/), all sharing a single fat-jar. A bare
// @SpringBootApplication enables a default @ComponentScan that would discover the
// other three sibling applications (each is itself a @Configuration via
// @SpringBootApplication and a CommandLineRunner) and run ALL of their run(...)
// methods in the same JVM, polluting stdout and breaking the byte-for-byte
// golden-output contract. Disabling the default component-scan filters with
// @ComponentScan(useDefaultFilters = false) means booting this class registers and
// runs ONLY itself (the primary source handed to SpringApplication.run), so exactly
// one COBOL Database translation executes per invocation. This mirrors the
// established convention used by the other multi-application sub-modules
// (cobol-conditions, cobol-loops, cobol-memory, cobol-random, cobol-sort).
@SpringBootApplication
@ComponentScan(useDefaultFilters = false)
public class OpenFileSequentialApplication implements CommandLineRunner {

    /**
     * Name of the classpath-loaded fixture resource. This Java translation
     * reads {@code data.txt} from the classpath (via
     * {@link ClassLoader#getResourceAsStream(String)}) rather than the COBOL
     * original relative path {@code ../data.dat}
     * ({@code OpenFileSequential.cbl:L12}), per AAP &sect;0.4.1 row 11.
     */
    private static final String DATA_RESOURCE = "data.txt";

    /**
     * Fixed width, in characters, of the COBOL {@code DETAILS} record:
     * {@code DET-ID PIC X(5)} + {@code DET-TIME PIC X(5)} + {@code DET-NUM PIC X(6)}
     * = 16 ({@code OpenFileSequential.cbl:L21-26}). Each fixture line is normalized
     * to this width before fixed-offset field extraction, reproducing how a COBOL
     * line-sequential {@code READ} fills a fixed-length record.
     */
    private static final int RECORD_LENGTH = 16;

    /**
     * Error message mirroring the COBOL literal on
     * {@code OpenFileSequential.cbl:L39}:
     * {@code DISPLAY "Error opening the DB file, program will exit."}.
     */
    private static final String ERROR_OPEN_MESSAGE =
            "Error opening the DB file, program will exit.";

    /**
     * COBOL {@code 02 DET-ID PIC X(5)} ({@code OpenFileSequential.cbl:L22}) -
     * the 5-character ID field of the {@code DETAILS} record. Initialized to
     * the empty string to mirror the COBOL low-values / spaces initial state
     * before the first {@code READ}.
     */
    private String detId = "";

    /**
     * COBOL {@code 03 DET-TIME PIC X(5)} ({@code OpenFileSequential.cbl:L24}) -
     * the 5-character time / string field nested inside {@code DET-STR}.
     */
    private String detTime = "";

    /**
     * COBOL {@code 03 DET-NUM PIC X(6)} ({@code OpenFileSequential.cbl:L26}) -
     * the 6-character number field nested inside {@code DET-STR}.
     */
    private String detNum = "";

    /**
     * Models the COBOL {@code 77 EOF PIC X.} flag
     * ({@code OpenFileSequential.cbl:L31}) with its {@code 88 EOF-T value "Y"}
     * / {@code 88 EOF-F value "N"} condition names
     * ({@code OpenFileSequential.cbl:L32-33}). {@code true} means end-of-file
     * has been reached (COBOL {@code EOF-T}); {@code false} means more records
     * remain (COBOL {@code EOF-F}).
     */
    private boolean eof;

    /**
     * Predicate for the COBOL {@code 88 EOF-F value "N"} condition - returns
     * {@code true} while more records remain to be read. Used as the
     * {@code PERFORM UNTIL EOF-T} loop guard in {@link #run(String...)}
     * ({@code OpenFileSequential.cbl:L52}).
     *
     * <p>The complementary {@code 88 EOF-T} predicate is intentionally not
     * defined as a separate method: it would never be referenced (the loop
     * ends when {@link BufferedReader#readLine()} returns {@code null}), and an
     * unused private method would break the build under
     * {@code -Xlint:all -Werror}.
     *
     * @return {@code true} if end-of-file has not yet been reached
     */
    private boolean isEofFalse() {
        return !eof;
    }

    /**
     * Spring Boot bootstrap entry point. Delegates to
     * {@link SpringApplication#run(Class, String[])}, which builds the
     * application context and invokes every {@link CommandLineRunner} bean,
     * including this class's {@link #run(String...)} method (the Java analogue
     * of the COBOL {@code PROCEDURE DIVISION} lifecycle).
     *
     * <p>The JUnit 5 golden-output test invokes {@link #run(String...)}
     * directly (rather than through {@code SpringApplication}) so that Spring's
     * startup logging does not pollute the captured {@code System.out}.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by
     *             this translation, but required by the Spring Boot contract)
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OpenFileSequentialApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Translation of the COBOL {@code PROCEDURE DIVISION}
     * ({@code OpenFileSequential.cbl:L35-62}).
     *
     * <p>Opens the classpath resource {@value #DATA_RESOURCE} (the Java
     * equivalent of the COBOL {@code OPEN INPUT DATA-FILE} on
     * {@code OpenFileSequential.cbl:L36}). If the resource is absent this
     * mirrors the COBOL early-exit path
     * ({@code IF FILE-STATUS NOT = "00" ... DISPLAY ... GOBACK} on
     * {@code OpenFileSequential.cbl:L38-41}): it classifies the synthetic
     * status via the same-package {@link FileStatus} enum, prints
     * {@value #ERROR_OPEN_MESSAGE} and returns without reading (the Java
     * analogue of {@code GOBACK}).
     *
     * <p>Otherwise it performs the COBOL priming-read plus
     * {@code PERFORM UNTIL EOF-T} sequential read loop
     * ({@code OpenFileSequential.cbl:L44-59}), invoking {@link #displayDetS()}
     * for every record, and finally closes the stream (the COBOL
     * {@code CLOSE DATA-FILE} / {@code GOBACK} on
     * {@code OpenFileSequential.cbl:L61-62}) via try-with-resources.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract; thrown if the underlying stream raises an
     *                   {@link java.io.IOException} while reading
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: OPEN INPUT DATA-FILE. -> open the classpath resource instead
        // of the relative path "../data.dat".
        InputStream in = getClass().getClassLoader().getResourceAsStream(DATA_RESOURCE);

        // COBOL: IF FILE-STATUS NOT = "00" DISPLAY "Error ..." GOBACK END-IF.
        // A missing classpath resource is the Java equivalent of the COBOL
        // "file not found" FILE-STATUS; FileStatus.fromCode confirms it is not
        // the success code "00" before taking the early-exit path (GOBACK).
        if (in == null) {
            String statusCode = FileStatus.NOT_EXISTS.getCode();
            if (FileStatus.fromCode(statusCode) != FileStatus.SUCCESS) {
                System.out.println(ERROR_OPEN_MESSAGE);
                return;
            }
        }

        // COBOL: priming READ + PERFORM UNTIL EOF-T { READ DATA-FILE NEXT } +
        // CLOSE DATA-FILE + GOBACK. try-with-resources guarantees the stream is
        // closed (the COBOL CLOSE) on every exit path. The InputStream is listed
        // as its own resource so it is closed even if BufferedReader construction
        // were to fail.
        try (in;
             BufferedReader br = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            eof = false; // SET EOF-F TO TRUE (more records expected)
            String line;
            while (isEofFalse() && (line = br.readLine()) != null) {
                // COBOL reads each record into the fixed 16-char DETAILS layout
                // (DET-ID 5 + DET-TIME 5 + DET-NUM 6). A line-sequential READ
                // right-pads a short line with spaces and truncates a long line to
                // the record width, so normalize the source line to exactly
                // RECORD_LENGTH characters before fixed-offset extraction.
                String record = padOrTruncate(line, RECORD_LENGTH);
                // Fixed-offset field extraction on the COBOL byte positions of the
                // DETAILS record. COBOL columns are 1-based; Java substring uses
                // 0-based, end-exclusive bounds:
                //   DET-ID   cols 1-5   -> [0, 5)
                //   DET-TIME cols 6-10  -> [5, 10)
                //   DET-NUM  cols 11-16 -> [10, 16)
                detId   = record.substring(0, 5);
                detTime = record.substring(5, 10);
                detNum  = record.substring(10, 16);
                // COBOL: NOT AT END -> PERFORM DISPLAY-DET-S THROUGH DISPLAY-DET-E
                displayDetS();
            }
            eof = true; // COBOL: AT END -> SET EOF-T TO TRUE
        }
    }

    /**
     * Normalizes a source line to a fixed COBOL record width, reproducing how a
     * COBOL line-sequential {@code READ} populates a fixed-length record: a shorter
     * line is right-padded with spaces, a longer line is truncated to the record
     * width.
     *
     * @param line   the raw line read from the fixture (never {@code null})
     * @param length the COBOL record width in characters
     * @return the line normalized to exactly {@code length} characters
     */
    private static String padOrTruncate(String line, int length) {
        if (line.length() >= length) {
            return line.substring(0, length);
        }
        StringBuilder padded = new StringBuilder(length);
        padded.append(line);
        while (padded.length() < length) {
            padded.append(' ');
        }
        return padded.toString();
    }

    /**
     * Translation of the COBOL {@code DISPLAY-DET-S} paragraph in the
     * {@code DISPAY-DET SECTION} (the {@code DISPAY} typo is preserved verbatim
     * from the source) on {@code OpenFileSequential.cbl:L64-67}:
     * {@code DISPLAY "ID: " DET-ID " STR: " DET-TIME " DET-NUM: " DET-NUM.}.
     *
     * <p>The {@code DET-ID}, {@code DET-TIME} and {@code DET-NUM} values are
     * already exactly 5, 5 and 6 characters wide because they are extracted by
     * fixed COBOL byte offsets from the 16-character normalized record in
     * {@link #run(String...)}; the COBOL group {@code DISPLAY} concatenates the
     * intervening string literals with these fixed-width fields, so this method
     * emits the fields verbatim with no additional formatting.
     */
    private void displayDetS() {
        System.out.println("ID: " + detId + " STR: " + detTime + " DET-NUM: " + detNum);
    }
}
