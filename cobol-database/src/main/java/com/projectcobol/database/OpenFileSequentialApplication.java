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

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
 * tokenized by whitespace into {@code DET-ID}, {@code DET-TIME} and
 * {@code DET-NUM} fields and formatted with {@code %-5s} / {@code %-6s} to
 * enforce the COBOL {@code PIC X} widths (5 / 5 / 6) declared in the
 * {@code DETAILS} record ({@code OpenFileSequential.cbl:L21-26}).
 *
 * <p>The COBOL original opens the fixed-width file {@code ../data.dat} via
 * {@code OPEN INPUT} ({@code OpenFileSequential.cbl:L12, L36}); this Java
 * translation instead reads the text fixture {@code data.txt} from the
 * classpath per AAP &sect;0.4.1 row 11, replacing the COBOL sequential
 * {@code READ ... AT END SET EOF-T TO TRUE} loop with a
 * {@link BufferedReader#readLine()} iteration.
 *
 * <p>Java's {@code %-Ns} format left-justifies and right-pads with spaces but
 * does NOT truncate. For the 36 records in the fixture this faithfully
 * preserves the COBOL data: the two records whose {@code DET-TIME} token is six
 * characters ({@code DATa45}, {@code DATA50}) emit a 37-character line instead
 * of 36, and the single record whose {@code DET-NUM} token is five characters
 * ({@code 02061}) is right-padded to the COBOL six-character width
 * ({@code "02061 "}).
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
@SpringBootApplication
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
        SpringApplication.run(OpenFileSequentialApplication.class, args);
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
                // COBOL: the DETAILS record is a fixed 16-char layout
                // (DET-ID 5 + DET-TIME 5 + DET-NUM 6). The data.txt fixture is
                // whitespace-delimited, so each line is tokenized into the three
                // record fields; absent tokens default to the empty string so a
                // short line never throws ArrayIndexOutOfBoundsException.
                String[] tokens = line.trim().split("\\s+");
                detId   = tokens.length > 0 ? tokens[0] : "";
                detTime = tokens.length > 1 ? tokens[1] : "";
                detNum  = tokens.length > 2 ? tokens[2] : "";
                // COBOL: NOT AT END -> PERFORM DISPLAY-DET-S THROUGH DISPLAY-DET-E
                displayDetS();
            }
            eof = true; // COBOL: AT END -> SET EOF-T TO TRUE
        }
    }

    /**
     * Translation of the COBOL {@code DISPLAY-DET-S} paragraph in the
     * {@code DISPAY-DET SECTION} (the {@code DISPAY} typo is preserved verbatim
     * from the source) on {@code OpenFileSequential.cbl:L64-67}:
     * {@code DISPLAY "ID: " DET-ID " STR: " DET-TIME " DET-NUM: " DET-NUM.}.
     *
     * <p>The COBOL {@code PIC X} field widths are enforced via {@code %-5s}
     * ({@code DET-ID}, {@code DET-TIME}) and {@code %-6s} ({@code DET-NUM}).
     * Java's {@code %-Ns} format left-aligns and right-pads with spaces but
     * does NOT truncate, so a token longer than its declared width is emitted
     * in full (faithfully preserving the COBOL data) while shorter tokens are
     * space-padded to the COBOL width.
     */
    private void displayDetS() {
        String detIdPad   = String.format("%-5s", detId);
        String detTimePad = String.format("%-5s", detTime);
        String detNumPad  = String.format("%-6s", detNum);
        System.out.println("ID: " + detIdPad + " STR: " + detTimePad + " DET-NUM: " + detNumPad);
    }
}
