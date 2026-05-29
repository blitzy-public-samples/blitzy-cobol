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

/**
 * Java enumeration of COBOL {@code FILE-STATUS} two-character codes.
 *
 * <p>This enum is the Java translation of the 30 {@code 88-level} condition
 * declarations in {@code OpenCobol/Database/StatusCode.cbl:L10-39}, plus one
 * synthetic {@link #UNKNOWN} sentinel for unmatched or null inputs.
 *
 * <p>The constant names preserve the COBOL {@code 88-level} identifiers
 * verbatim (e.g., {@code SUCCESS}, {@code END_OF_FILE}, {@code KEY_NOT_EXISTS},
 * {@code I_O_DENIED}). The two-character {@link #getCode() code} field
 * preserves the COBOL {@code PIC X(2)} status code (e.g., {@code "00"} for
 * {@code SUCCESS}, {@code "10"} for {@code END_OF_FILE}).
 *
 * <p>Per AAP &sect;0.3.3 "No-Cross-Submodule-Dependency Rule" this enum lives
 * inside {@code cobol-database} and is NOT promoted to a shared
 * {@code cobol-common} module because no other sub-module references
 * COBOL FILE-STATUS semantics.
 *
 * <p>Usage from same-package callers (no {@code import} needed):
 * <pre>{@code
 * if (FileStatus.fromCode(statusCode) == FileStatus.SUCCESS) {
 *     // happy path
 * } else {
 *     // error path
 * }
 * }</pre>
 *
 * @see <a href="OpenCobol/Database/StatusCode.cbl">StatusCode.cbl source</a>
 */
public enum FileStatus {

    /** {@code 00} - Operation completed successfully. */
    SUCCESS("00"),

    /** {@code 02} - Success with duplicate key encountered. */
    SUCCESS_DUPLICATE("02"),

    /** {@code 04} - Success but length mismatch (record shorter or longer than declared). */
    SUCCESS_INCOMPLETE("04"),

    /** {@code 05} - Success on optional file open (file did not exist but was created). */
    SUCCESS_OPTIONAL("05"),

    /** {@code 07} - Success with no unit info. */
    SUCCESS_NO_UNIT("07"),

    /** {@code 10} - End-of-file reached during sequential read. */
    END_OF_FILE("10"),

    /** {@code 14} - Key beyond file boundary on relative I/O. */
    OUT_OF_KEY_RANGE("14"),

    /** {@code 21} - Invalid key sequence on indexed file. */
    KEY_INVALID("21"),

    /** {@code 22} - Duplicate primary key on WRITE. */
    KEY_EXISTS("22"),

    /** {@code 23} - Key not found on READ/REWRITE/DELETE. */
    KEY_NOT_EXISTS("23"),

    /** {@code 30} - Permanent error (hardware or system error). */
    PERMANENT_ERROR("30"),

    /** {@code 31} - File name attribute mismatch. */
    INCONSISTENT_FILENAME("31"),

    /** {@code 34} - Boundary violation on sequential file. */
    BOUNDARY_VIOLATION("34"),

    /** {@code 35} - File not found on OPEN. */
    NOT_EXISTS("35"),

    /** {@code 37} - OPEN denied due to permissions. */
    PERMISSION_DENIED("37"),

    /** {@code 38} - File previously CLOSED WITH LOCK. */
    CLOSED_WITH_LOCK("38"),

    /** {@code 39} - OPEN attribute mismatch with file. */
    CONFLICT_ATTRIBUTE("39"),

    /** {@code 41} - OPEN attempted on already-open file. */
    ALREADY_OPEN("41"),

    /** {@code 42} - I/O attempted on closed file. */
    NOT_OPEN("42"),

    /** {@code 43} - REWRITE/DELETE without prior READ. */
    READ_NOT_DONE("43"),

    /** {@code 44} - Record length boundary exceeded. */
    RECORD_OVERFLOW("44"),

    /** {@code 46} - Sequential READ failure. */
    READ_ERROR("46"),

    /** {@code 47} - Input operation denied. */
    INPUT_DENIED("47"),

    /** {@code 48} - Output operation denied. */
    OUTPUT_DENIED("48"),

    /** {@code 49} - I/O operation denied. */
    I_O_DENIED("49"),

    /** {@code 51} - Record locked by another process. */
    RECORD_LOCKED("51"),

    /** {@code 52} - End-of-page reached on REPORT. */
    END_OF_PAGE("52"),

    /** {@code 57} - LINAGE violation. */
    I_O_LINAGE("57"),

    /** {@code 61} - File-sharing conflict. */
    FILE_SHARING("61"),

    /** {@code 91} - Implementation-defined unavailability. */
    NOT_AVAILABLE("91"),

    /**
     * Synthetic sentinel for codes outside the COBOL 88-level vocabulary
     * declared in {@code OpenCobol/Database/StatusCode.cbl}, including
     * {@code null} input. Returned by {@link #fromCode(String)} when no
     * match is found. Has an empty {@code code} value.
     */
    UNKNOWN("");

    /** Two-character COBOL {@code FILE-STATUS} code (or empty string for {@link #UNKNOWN}). */
    private final String code;

    /**
     * Constructs an enum constant with its associated COBOL FILE-STATUS code.
     *
     * @param code the two-character status code (or empty for {@link #UNKNOWN})
     */
    FileStatus(String code) {
        this.code = code;
    }

    /**
     * Returns the two-character COBOL FILE-STATUS code (or empty for {@link #UNKNOWN}).
     *
     * @return the status code
     */
    public String getCode() {
        return code;
    }

    /**
     * Looks up the {@link FileStatus} constant whose {@link #getCode() code}
     * matches the supplied two-character string. Null-safe - returns
     * {@link #UNKNOWN} when {@code code} is {@code null} or does not match
     * any of the 30 declared COBOL FILE-STATUS codes.
     *
     * <p>This method preserves the COBOL semantic of testing
     * {@code IF SUCCESS} (the {@code 88-level} condition implicitly compares
     * the {@code STATUS-CODE} field to {@code '00'}).
     *
     * @param code the two-character status code to look up, may be {@code null}
     * @return the matching {@link FileStatus}, or {@link #UNKNOWN} if no match
     */
    public static FileStatus fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (FileStatus fs : values()) {
            if (fs.code.equals(code)) {
                return fs;
            }
        }
        return UNKNOWN;
    }
}
