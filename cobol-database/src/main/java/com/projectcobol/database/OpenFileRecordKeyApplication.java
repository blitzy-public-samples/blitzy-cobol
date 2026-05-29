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
 * {@code OpenCobol/Database/OpenFileRecordKey.cbl} (PROGRAM-ID {@code OPEN-FILE}).
 *
 * <p>Reads the classpath-loaded {@code data.txt} fixture and emits one
 * 25-character record per line, matching the COBOL {@code DISPLAY MY-DATA-STRUCT}
 * behavior on {@code OpenFileRecordKey.cbl:L58}. Each line in {@code data.txt} is
 * tokenized by whitespace into the {@code DATA-ID}, {@code DATA-NAME}, and
 * {@code DATA-TIME} fields of the COBOL {@code MY-DATA-STRUCT} group
 * ({@code OpenFileRecordKey.cbl:L28-31}) and formatted with
 * {@code %-5s%-10s%-10s} to enforce the COBOL {@code PIC X} widths
 * (5 + 10 + 10 = 25).
 *
 * <p>The COBOL original opens the fixed-width file {@code ../data.dat} via
 * {@code OPEN I-O DATA-FILE} ({@code OpenFileRecordKey.cbl:L12, L41}); this Java
 * translation instead reads the byte-identical text fixture {@code data.txt}
 * from the classpath per AAP &sect;0.4.1 row 10, replacing the COBOL
 * {@code PERFORM UNTIL EOF = 'Y' / READ ... AT END} loop with a
 * {@link BufferedReader#readLine()} iteration. The COBOL {@code OPEN I-O}
 * (read/write) mode is mapped to read-only classpath access because the Java
 * translation is strictly demonstrative and never writes to the fixture.
 *
 * <p>The COBOL {@code FILE STATUS} check
 * ({@code OpenFileRecordKey.cbl:L45-50}) that aborts with an error message when
 * the file cannot be opened is mirrored here using the same-package
 * {@link FileStatus} enum: a missing classpath resource is treated as the COBOL
 * {@code FILE-STATUS} "file not found" code ({@link FileStatus#NOT_EXISTS}) and
 * triggers the same {@code ERROR-MESSAGE} / {@code END-PROGRAM} sequence.
 *
 * <p>Per the AAP &sect;0.7.3 filename-traceability rule, the Java class name
 * follows the source {@code .cbl} filename ({@code OpenFileRecordKey.cbl}
 * &rarr; {@code OpenFileRecordKeyApplication}) rather than the COBOL
 * {@code PROGRAM-ID} ({@code OPEN-FILE}).
 */
@SpringBootApplication
public class OpenFileRecordKeyApplication implements CommandLineRunner {

    /**
     * Name of the classpath-loaded fixture resource. This Java translation reads
     * {@code data.txt} from the classpath (via
     * {@link ClassLoader#getResourceAsStream(String)}) rather than the COBOL
     * original relative path {@code ../data.dat}
     * ({@code OpenFileRecordKey.cbl:L12}), per AAP &sect;0.4.1 row 10.
     */
    private static final String DATA_RESOURCE = "data.txt";

    /**
     * Synthetic constant mirroring the COBOL literal
     * {@code MOVE "ERROR OPENING FILE : " TO ERROR-MSG} on
     * {@code OpenFileRecordKey.cbl:L47}.
     */
    private static final String ERROR_OPENING_FILE_PREFIX = "ERROR OPENING FILE : ";

    /**
     * COBOL {@code 05 DATA-ID PIC X(5)} ({@code OpenFileRecordKey.cbl:L29}) -
     * the 5-character identifier field of the {@code MY-DATA-STRUCT} group.
     * Initialized to the empty string to mirror the COBOL spaces initial state
     * before the first {@code READ}.
     */
    private String dataId = "";

    /**
     * COBOL {@code 05 DATA-NAME PIC X(10)} ({@code OpenFileRecordKey.cbl:L30}) -
     * the 10-character name field of the {@code MY-DATA-STRUCT} group.
     */
    private String dataName = "";

    /**
     * COBOL {@code 05 DATA-TIME PIC X(10)} ({@code OpenFileRecordKey.cbl:L31}) -
     * the 10-character time field of the {@code MY-DATA-STRUCT} group.
     */
    private String dataTime = "";

    /**
     * Models the COBOL {@code 01 EOF PIC A(1)} flag
     * ({@code OpenFileRecordKey.cbl:L34}). {@code true} means the COBOL
     * {@code EOF = 'Y'} end-of-file state (set by {@code AT END MOVE 'Y' TO EOF}
     * on {@code OpenFileRecordKey.cbl:L57}); {@code false} means more records
     * remain to be read.
     */
    private boolean eof;

    /**
     * COBOL {@code 05 ERROR-LEVEL PIC XX} ({@code OpenFileRecordKey.cbl:L36}) -
     * captures the COBOL {@code FILE-STATUS} code on an open failure
     * ({@code MOVE FILE-STATUS TO ERROR-LEVEL} on {@code OpenFileRecordKey.cbl:L46}).
     */
    private String errorLevel = "";

    /**
     * COBOL {@code 05 ERROR-MSG PIC X(50)} ({@code OpenFileRecordKey.cbl:L37}) -
     * holds the error message text emitted by {@link #errorMessage()}.
     */
    private String errorMsg = "";

    /**
     * Spring Boot bootstrap entry point. Delegates to
     * {@link SpringApplication#run(Class, String[])}, which builds the
     * application context and invokes every {@link CommandLineRunner} bean,
     * including this class's {@link #run(String...)} method (the Java analogue
     * of the COBOL {@code PROCEDURE DIVISION} lifecycle).
     *
     * <p>The JUnit 5 golden-output test invokes {@link #run(String...)} directly
     * (rather than through {@code SpringApplication}) so that Spring's startup
     * logging does not pollute the captured {@code System.out}.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by
     *             this translation, but required by the Spring Boot contract)
     */
    public static void main(String[] args) {
        SpringApplication.run(OpenFileRecordKeyApplication.class, args);
    }

    /**
     * Translation of the COBOL {@code PROCEDURE DIVISION}
     * ({@code OpenFileRecordKey.cbl:L39-63}).
     *
     * <p>Opens the classpath resource {@value #DATA_RESOURCE}, mirroring
     * {@code OPEN I-O DATA-FILE} on {@code OpenFileRecordKey.cbl:L41}. If the
     * resource cannot be located, records a synthetic FILE-STATUS code via
     * {@link FileStatus#NOT_EXISTS} and invokes the error / end-program sequence
     * equivalent to the COBOL fallback path on
     * {@code OpenFileRecordKey.cbl:L45-50}
     * ({@code MOVE FILE-STATUS TO ERROR-LEVEL}, {@code MOVE "ERROR OPENING FILE : "},
     * {@code PERFORM ERROR-MESSAGE}, {@code PERFORM END-PROGRAM}).
     *
     * <p>Otherwise it performs the COBOL {@code READ-FILE SECTION} sequential
     * read loop ({@code OpenFileRecordKey.cbl:L52-62}) via {@link #readFile}
     * and finally closes the stream (the COBOL {@code CLOSE DATA-FILE} on
     * {@code OpenFileRecordKey.cbl:L63}) declaratively through try-with-resources.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract; thrown if the underlying stream raises an
     *                   {@link java.io.IOException} while reading
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: OPEN I-O DATA-FILE. -> open the classpath resource instead of
        // the relative path "../data.dat". The COBOL read/write I-O mode is
        // mapped to read-only access because this translation only DISPLAYs.
        InputStream in = getClass().getClassLoader().getResourceAsStream(DATA_RESOURCE);

        // COBOL: IF FILE-STATUS NOT = '00' (the open failed). A missing classpath
        // resource is the Java equivalent of the COBOL "file not found"
        // FILE-STATUS; FileStatus.fromCode confirms it is not the success code
        // "00" before taking the error path (PERFORM ERROR-MESSAGE / END-PROGRAM).
        if (in == null) {
            String statusCode = FileStatus.NOT_EXISTS.getCode();
            if (FileStatus.fromCode(statusCode) != FileStatus.SUCCESS) {
                errorLevel = statusCode;                 // MOVE FILE-STATUS TO ERROR-LEVEL
                errorMsg = ERROR_OPENING_FILE_PREFIX;    // MOVE "ERROR OPENING FILE : " TO ERROR-MSG
                errorMessage();                          // PERFORM ERROR-MESSAGE
                endProgram();                            // PERFORM END-PROGRAM
            }
            return;
        }

        // COBOL: READ-FILE SECTION (sequential read loop) followed by
        // CLOSE DATA-FILE. try-with-resources guarantees the stream is closed
        // (the COBOL CLOSE) on every exit path. The InputStream is listed as its
        // own resource so it is closed even if BufferedReader construction fails.
        try (in;
             BufferedReader br = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            readFile(br);
        }
    }

    /**
     * Translation of the COBOL {@code READ-FILE SECTION}
     * ({@code OpenFileRecordKey.cbl:L52-62}). Iterates
     * {@link BufferedReader#readLine()} until end-of-file, tokenizing each line
     * into the {@code MY-DATA-STRUCT} fields and emitting one 25-character
     * output line via {@link System#out}.
     *
     * <p>Each output line mirrors {@code DISPLAY MY-DATA-STRUCT} on
     * {@code OpenFileRecordKey.cbl:L58}, formatted as {@code %-5s%-10s%-10s} to
     * enforce the COBOL {@code PIC X} widths (5 + 10 + 10 = 25). Java's
     * {@code %-Ns} format left-justifies and right-pads with spaces but does NOT
     * truncate; every token in the {@code data.txt} fixture fits within its
     * COBOL width (the widest {@code DATA-NAME} / {@code DATA-TIME} token is six
     * characters), so the output is exactly 25 characters per record.
     *
     * @param br the buffered reader over the classpath {@value #DATA_RESOURCE}
     *           fixture
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract chain; thrown if {@code br} raises an
     *                   {@link java.io.IOException} while reading
     */
    private void readFile(BufferedReader br) throws Exception {
        eof = false; // COBOL: EOF starts un-set (more records expected)
        String line;
        // COBOL: PERFORM UNTIL EOF = 'Y' / READ DATA-FILE INTO MY-DATA-STRUCT.
        while (!eof && (line = br.readLine()) != null) {
            // The COBOL record is a fixed 25-char layout (DATA-ID 5 + DATA-NAME 10
            // + DATA-TIME 10). The data.txt fixture is whitespace-delimited, so
            // each line is tokenized into the three MY-DATA-STRUCT fields; absent
            // tokens default to the empty string so a short line never throws
            // ArrayIndexOutOfBoundsException.
            String[] tokens = line.trim().split("\\s+");
            dataId   = tokens.length > 0 ? tokens[0] : "";
            dataName = tokens.length > 1 ? tokens[1] : "";
            dataTime = tokens.length > 2 ? tokens[2] : "";
            // COBOL: NOT AT END -> DISPLAY MY-DATA-STRUCT (25-char fixed width).
            System.out.println(String.format("%-5s%-10s%-10s", dataId, dataName, dataTime));
        }
        eof = true; // COBOL: AT END -> MOVE 'Y' TO EOF
    }

    /**
     * Translation of the COBOL {@code ERROR-MESSAGE SECTION}
     * ({@code OpenFileRecordKey.cbl:L65-66}):
     * {@code DISPLAY ERROR-MSG " " ERROR-LEVEL.}.
     */
    private void errorMessage() {
        System.out.println(errorMsg + " " + errorLevel);
    }

    /**
     * Translation of the COBOL {@code END-PROGRAM SECTION}
     * ({@code OpenFileRecordKey.cbl:L68-69}): {@code CLOSE DATA-FILE.}.
     *
     * <p>Java's try-with-resources auto-closes the underlying reader in
     * {@link #run(String...)}, so this helper is retained for COBOL traceability
     * only - invoking it is a no-op because the resource cleanup happens
     * declaratively. In the COBOL source it is reached only on the open-failure
     * fallback path ({@code PERFORM END-PROGRAM} on
     * {@code OpenFileRecordKey.cbl:L49}), where no stream was successfully opened
     * and therefore nothing remains to close.
     */
    private void endProgram() {
        // try-with-resources closes the reader; no explicit action needed.
    }
}
