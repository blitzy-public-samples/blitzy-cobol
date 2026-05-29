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
 * {@code OpenCobol/Database/SequentialRead.cbl} (PROGRAM-ID
 * {@code SEQUENTIAL-READ}).
 *
 * <p>Reads the classpath-loaded {@code data.txt} fixture and emits four lines
 * per record, faithfully reproducing the COBOL {@code DISPLAY-DET-S} paragraph
 * ({@code SequentialRead.cbl:L63-67}). Each {@code data.txt} line is padded to
 * {@value #DETAILS_WIDTH} characters (mirroring the width of the COBOL
 * {@code DETAILS} structure) and fixed-offset substring views are extracted:
 * {@code DETAILS-ID} at {@code [0,7)}, {@code DETAILS-SURNAME} at
 * {@code [7,15)} and {@code DETAILS-BIRTHDAY} at {@code [17,25)}. The COBOL
 * {@code INITIALS} ({@code [15,17)}) and {@code SOME-CODE} ({@code [25,30)})
 * fields are part of the record layout but are NOT displayed by
 * {@code DISPLAY-DET-S}, so this translation does not print them either.
 *
 * <p>The COBOL original opens the binary, fixed-width file
 * {@code ../database.dat} via {@code OPEN INPUT}; this Java translation instead
 * reads the text fixture {@code data.txt} from the classpath per AAP
 * &sect;0.4.1 row 12. Because {@code data.txt} records are variable width
 * (17-19 characters) whereas the COBOL {@code DETAILS} record is a fixed 30
 * characters, every line is right-padded to {@value #DETAILS_WIDTH} characters
 * before substring extraction so the field offsets line up exactly as they did
 * against the original fixed-width file. Records shorter than the offsets being
 * read therefore yield trailing-space-padded field values rather than throwing
 * {@link StringIndexOutOfBoundsException}.
 *
 * <p>The COBOL {@code FILE STATUS} check ({@code SequentialRead.cbl:L37-40})
 * that aborts with an error message when the file cannot be opened is mirrored
 * here using the same-package {@link FileStatus} enum: a missing classpath
 * resource is treated as the COBOL {@code FILE-STATUS} "file not found" code
 * and produces the same early-exit behaviour.
 *
 * <p>Per the AAP &sect;0.7.3 filename-traceability rule, the Java class name
 * follows the source {@code .cbl} filename ({@code SequentialRead.cbl} &rarr;
 * {@code SequentialReadApplication}) rather than the COBOL {@code PROGRAM-ID}
 * ({@code SEQUENTIAL-READ}).
 */
@SpringBootApplication
public class SequentialReadApplication implements CommandLineRunner {

    /**
     * Name of the classpath-loaded fixture resource. This Java translation
     * reads {@code data.txt} from the classpath rather than the COBOL original
     * relative path {@code ../database.dat}, per AAP &sect;0.4.1 row 12.
     */
    private static final String DATA_RESOURCE = "data.txt";

    /**
     * Width of the COBOL {@code DETAILS} structure in characters:
     * {@code DETAILS-ID PIC 9(7)} (7) + {@code DETAILS-SURNAME PIC X(8)} (8)
     * + {@code INITIALS PIC XX} (2) + {@code DETAILS-BIRTHDAY PIC X(8)} (8)
     * + {@code SOME-CODE PIC X(5)} (5) = 30.
     */
    private static final int DETAILS_WIDTH = 30;

    /**
     * Error message mirroring the COBOL literal on
     * {@code SequentialRead.cbl:L38}:
     * {@code DISPLAY "Error opening the DB file, program will exit."}.
     */
    private static final String ERROR_OPEN_MESSAGE =
            "Error opening the DB file, program will exit.";

    /**
     * Models the COBOL {@code 77 EOF PIC X.} flag with its {@code 88 EOF-T}
     * ("Y") / {@code 88 EOF-F} ("N") condition names. {@code true} means
     * end-of-file has been reached (COBOL {@code EOF-T}); {@code false} means
     * more records remain (COBOL {@code EOF-F}).
     */
    private boolean eof;

    /**
     * Predicate for the COBOL {@code 88 EOF-F value "N"} condition - returns
     * {@code true} while more records remain to be read. Used as the
     * {@code PERFORM UNTIL EOF-T} loop guard in {@link #run(String...)}.
     *
     * <p>The complementary {@code 88 EOF-T} predicate is intentionally not
     * defined as a separate method: it is never referenced (the loop ends when
     * {@link BufferedReader#readLine()} returns {@code null}), and an unused
     * private method would break the build under {@code -Xlint:all -Werror}.
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
     * including this class's {@link #run(String...)} method.
     *
     * <p>The JUnit 5 golden-output test invokes {@link #run(String...)}
     * directly (rather than through {@code SpringApplication}) so that Spring's
     * startup logging does not pollute the captured {@code System.out}.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by
     *             this translation, but required by the Spring Boot contract)
     */
    public static void main(String[] args) {
        SpringApplication.run(SequentialReadApplication.class, args);
    }

    /**
     * Translation of the COBOL {@code PROCEDURE DIVISION / MAIN-PROCEDURE}
     * ({@code SequentialRead.cbl:L33-60}).
     *
     * <p>Opens the classpath resource {@value #DATA_RESOURCE} (the Java
     * equivalent of the COBOL {@code OPEN INPUT DATA-FILE}). If the resource is
     * absent this mirrors the COBOL early-exit path
     * ({@code IF FILE-STATUS not = "00" ... DISPLAY ... GOBACK}): it reports the
     * status via the {@link FileStatus} enum, prints
     * {@value #ERROR_OPEN_MESSAGE} and returns without reading.
     *
     * <p>Otherwise it performs the COBOL priming-read plus
     * {@code PERFORM UNTIL EOF-T} sequential read loop, invoking
     * {@link #displayDetS(String)} for every record, and finally closes the
     * stream (the COBOL {@code CLOSE DATA-FILE} / {@code GOBACK}) via
     * try-with-resources.
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
        // of the relative path "../database.dat".
        InputStream in = getClass().getClassLoader().getResourceAsStream(DATA_RESOURCE);

        // COBOL: IF FILE-STATUS not = "00" DISPLAY "Error ..." GOBACK END-IF.
        // A missing classpath resource is the Java equivalent of the COBOL
        // "file not found" FILE-STATUS; FileStatus.fromCode confirms it is not
        // the success code "00" before taking the early-exit path.
        if (in == null) {
            String statusCode = FileStatus.NOT_EXISTS.getCode();
            if (FileStatus.fromCode(statusCode) != FileStatus.SUCCESS) {
                System.out.println(ERROR_OPEN_MESSAGE);
                return;
            }
        }

        // COBOL: priming READ + PERFORM UNTIL EOF-T { READ DATA-FILE NEXT } +
        // CLOSE DATA-FILE + GOBACK. try-with-resources guarantees the stream is
        // closed (the COBOL CLOSE) on every exit path.
        try (in;
             BufferedReader br = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            eof = false; // SET EOF-F TO TRUE (more records expected)
            String line;
            while (isEofFalse() && (line = br.readLine()) != null) {
                // COBOL: NOT AT END -> PERFORM DISPLAY-DET-S THROUGH DISPLAY-DET-E
                displayDetS(line);
            }
            eof = true; // COBOL: AT END -> SET EOF-T TO TRUE
        }
    }

    /**
     * Translation of the COBOL {@code DISPLAY-DET-S} paragraph
     * ({@code SequentialRead.cbl:L63-67}). Emits exactly four lines per record:
     * <ol>
     *   <li>{@code DISPLAY DETAILS.} - the full {@value #DETAILS_WIDTH}-character
     *       padded structure</li>
     *   <li>{@code DISPLAY "DETAILS-ID: " DETAILS-ID} - 7-char id at {@code [0,7)}</li>
     *   <li>{@code DISPLAY "DETAILS-NAME: " DETAILS-SURNAME.} - 8-char surname at
     *       {@code [7,15)}</li>
     *   <li>{@code DISPLAY "DETAILS-BIRTHDAY: " DETAILS-BIRTHDAY.} - 8-char
     *       birthday at {@code [17,25)}</li>
     * </ol>
     *
     * <p>The COBOL {@code INITIALS PIC XX} field at {@code [15,17)} and
     * {@code SOME-CODE PIC X(5)} field at {@code [25,30)} are part of the record
     * layout but are NOT displayed by {@code DISPLAY-DET-S}; they are documented
     * below as commented offsets rather than extracted into unused locals (which
     * would break the {@code -Xlint:all -Werror} build).
     *
     * <p>Lines shorter than {@value #DETAILS_WIDTH} characters (typical
     * {@code data.txt} lines are 17-19 characters) are right-padded with spaces
     * via {@code %-30s} so substring extraction yields trailing-space-padded
     * values instead of throwing {@link StringIndexOutOfBoundsException}. The
     * defensive truncation guard caps any (hypothetical) over-length line at
     * {@value #DETAILS_WIDTH} characters so the displayed structure never
     * exceeds the COBOL record width.
     *
     * @param line a single raw record read from {@value #DATA_RESOURCE}
     */
    private void displayDetS(String line) {
        // Right-pad to the COBOL DETAILS width; %-30s is left-justified padding.
        String padded = String.format("%-" + DETAILS_WIDTH + "s", line);
        if (padded.length() > DETAILS_WIDTH) {
            padded = padded.substring(0, DETAILS_WIDTH);
        }

        // Fixed COBOL field offsets within the 30-char DETAILS structure
        // (substring end indices are exclusive, mirroring the COBOL PIC widths):
        String detailsId  = padded.substring(0, 7);    // DETAILS-ID PIC 9(7)
        String surname    = padded.substring(7, 15);   // DETAILS-SURNAME PIC X(8)
        // INITIALS         = padded.substring(15, 17); // PIC XX (declared, NOT displayed)
        String birthday   = padded.substring(17, 25);  // DETAILS-BIRTHDAY PIC X(8)
        // SOME-CODE        = padded.substring(25, 30); // PIC X(5) (declared, NOT displayed)

        System.out.println(padded);                          // DISPLAY DETAILS.
        System.out.println("DETAILS-ID: " + detailsId);      // DISPLAY "DETAILS-ID: " DETAILS-ID
        System.out.println("DETAILS-NAME: " + surname);      // DISPLAY "DETAILS-NAME: " DETAILS-SURNAME
        System.out.println("DETAILS-BIRTHDAY: " + birthday); // DISPLAY "DETAILS-BIRTHDAY: " DETAILS-BIRTHDAY
    }
}
