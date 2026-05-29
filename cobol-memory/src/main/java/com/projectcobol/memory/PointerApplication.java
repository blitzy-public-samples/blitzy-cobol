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

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/Memory/Pointer.cbl}
 * (PROGRAM-ID {@code WORK-WITH-POINTER}).
 *
 * <p>Demonstrates COBOL {@code POINTER}, {@code ADDRESS OF}, and {@code LINKAGE SECTION}
 * overlay semantics by modeling them with a 16-character backing {@link String} buffer
 * and {@link String#substring(int, int)} views. Structurally identical to
 * {@link AddressApplication} but uses different field names ({@code AREA-A..AREA-D}
 * instead of {@code WK-A..WK-D}) and a different pointer name ({@code W-POINTER}
 * instead of {@code WK-PTR}).
 *
 * <p>The original COBOL program:
 * <ol>
 *   <li>Declares a 16-byte {@code WORK-AREA} group containing four {@code PIC X(4)}
 *       elementaries ({@code AREA-A='AAAA'}, {@code AREA-B='BBBB'}, {@code AREA-C='CCCC'},
 *       {@code AREA-D='DDDF'}) and a {@code W-POINTER POINTER}.</li>
 *   <li>Declares a {@code LINKAGE SECTION} group {@code WORK-DATA} with two
 *       {@code PIC X(4)} sub-fields {@code WORK-A} and {@code NEXT-WORK-DATA}.</li>
 *   <li>Captures the address of {@code WORK-AREA} into {@code W-POINTER} via
 *       {@code SET W-POINTER TO ADDRESS OF WORK-AREA}.</li>
 *   <li>Aliases {@code WORK-DATA} at that address via
 *       {@code SET ADDRESS OF WORK-DATA TO W-POINTER}.</li>
 *   <li>Displays {@code WORK-DATA}, {@code W-POINTER}, {@code WORK-A}, and
 *       {@code NEXT-WORK-DATA} to verify the overlay.</li>
 * </ol>
 *
 * <p><b>Java translation strategy</b> (per AAP &sect;0.6.1): Java is memory-safe and
 * exposes no raw pointer or {@code union}-style reinterpretation. The COBOL semantics
 * are modeled with a backing {@link String} buffer plus substring projections. The
 * {@code W-POINTER} variable is simulated with an {@code int} offset into the buffer;
 * pointer-display lines emit a deterministic sentinel ({@code 0x00000000} pre-SET,
 * {@code 0x00000010} post-SET) because Java has no equivalent runtime memory address
 * that can be reliably displayed. The {@code PointerApplicationTest} golden-output
 * test matches these sentinel lines with a regex format-match rather than literal
 * equality.
 *
 * <p><b>Authentic INCONSISTENT formatting preserved</b> (per AAP &sect;0.7.2
 * minimal-change clause): {@code Pointer.cbl} lines 26 and 31-33 use {@code "LABEL: "}
 * (colon-space, NO leading space), but line 34 uses {@code "NEXT-WORK-DATA : "}
 * (space-colon-space). This inconsistency is <b>NOT a bug</b> &mdash; it is an authentic
 * quirk in the COBOL source. The Java translation preserves the inconsistency
 * verbatim: lines 1-4 of the output use colon-space, line 5 uses space-colon-space.
 * The {@code PointerApplicationTest} golden-output fixture and assertions explicitly
 * encode this inconsistency.
 *
 * <p>Per AAP &sect;0.7.3 filename traceability rule, this class is named
 * {@code PointerApplication} (from the source filename {@code Pointer.cbl}), NOT
 * {@code WorkWithPointerApplication} (which would be derived from the COBOL
 * {@code PROGRAM-ID. WORK-WITH-POINTER.}). The filesystem name is the authoritative
 * identifier.
 */
@SpringBootApplication
public class PointerApplication implements CommandLineRunner {

    /**
     * COBOL: {@code 01 WORK-AREA.} containing four {@code 03 PIC X(4)} elementaries.
     *
     * <p>The COBOL group is laid out contiguously in memory as 16 bytes:
     * {@code AREA-A='AAAA'} (bytes 0-3), {@code AREA-B='BBBB'} (bytes 4-7),
     * {@code AREA-C='CCCC'} (bytes 8-11), {@code AREA-D='DDDF'} (bytes 12-15).
     * Note that {@code AREA-D} is {@code 'DDDF'} (D-D-D-F), preserved verbatim from
     * {@code OpenCobol/Memory/Pointer.cbl} line 14.
     *
     * <p>In Java, the contiguous COBOL group is modeled with a single {@link String}
     * backing buffer that concatenates the four 4-character literals in declaration
     * order, yielding a 16-character buffer that can be sliced via
     * {@link String#substring(int, int)} to simulate {@code LINKAGE SECTION} overlay
     * aliasing.
     */
    private static final String WORK_AREA = "AAAA" + "BBBB" + "CCCC" + "DDDF";

    /**
     * COBOL: {@code 01 W-POINTER POINTER.} A simulated logical byte-offset into
     * {@link #WORK_AREA}. Java does not expose raw memory addresses, so the pointer
     * is modeled as an {@code int} offset (range 0..15) and the pointer-display
     * statements emit a deterministic sentinel hexadecimal string instead of an
     * actual address value.
     *
     * <p>{@code -1} indicates the uninitialized/null pointer state (pre-SET), which
     * the pointer-display logic renders as {@code 0x00000000}.
     */
    private int wPointer = -1;

    /**
     * COBOL: {@code 03 WORK-A PIC X(4).} inside the {@code LINKAGE SECTION} group
     * {@code 01 WORK-DATA.} Aliases the first 4 bytes of the {@code WORK-DATA}
     * overlay (which itself aliases bytes at {@code W-POINTER} into
     * {@link #WORK_AREA}).
     */
    private String workA = "";

    /**
     * COBOL: {@code 03 NEXT-WORK-DATA PIC X(4).} inside the {@code LINKAGE SECTION}
     * group {@code 01 WORK-DATA.} Aliases bytes 4-7 of the {@code WORK-DATA} overlay.
     */
    private String nextWorkData = "";

    /**
     * Standard Spring Boot entry point. Bootstraps the Spring application context
     * and invokes {@link #run(String...)} via the {@link CommandLineRunner} contract.
     *
     * @param args command-line arguments (unused in this educational sample)
     */
    public static void main(String[] args) {
        SpringApplication.run(PointerApplication.class, args);
    }

    /**
     * {@link CommandLineRunner} entry point &mdash; the translated COBOL
     * {@code PROCEDURE DIVISION MAIN-PROCEDURE} body.
     *
     * <p>Mirrors {@code OpenCobol/Memory/Pointer.cbl} lines 23-36, including the
     * five {@code DISPLAY} statements that produce the deterministic 5-line output
     * verified by {@code PointerApplicationTest}.
     *
     * <p>The output formatting preserves the INTENTIONAL INCONSISTENCY from
     * {@code Pointer.cbl}: lines 1-4 use {@code "LABEL: "} (colon-space) per
     * {@code Pointer.cbl} lines 26, 31-33, while line 5 uses
     * {@code "NEXT-WORK-DATA : "} (space-colon-space) per {@code Pointer.cbl}
     * line 34. This authentic COBOL quirk is preserved verbatim per AAP
     * &sect;0.7.2 minimal-change clause.
     *
     * @param args command-line arguments (unused in this educational sample;
     *             present to satisfy the {@link CommandLineRunner} contract)
     * @throws Exception declared per {@link CommandLineRunner}; never actually thrown
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL line 26: DISPLAY "W-POINTER: " W-POINTER
        //
        // The uninitialized COBOL POINTER displays as an implementation-defined
        // value (typically a hex address or "NULL"). Java has no raw memory address,
        // so we emit the deterministic sentinel 0x00000000 for the pre-SET state.
        // This sentinel is matched by the PointerApplicationTest regex
        // ^W-POINTER: 0x[0-9A-Fa-f]{8}$ rather than literal equality.
        // Note: "W-POINTER: " uses colon-space (NO leading space before colon),
        // per Pointer.cbl line 26 -- distinct from Address.cbl line 25's
        // "WK-PTR :   " (space-colon-3spaces).
        System.out.println("W-POINTER: " + formatPointer(wPointer));

        // COBOL line 28: SET W-POINTER TO ADDRESS OF WORK-AREA.
        //
        // In COBOL, this captures the runtime address of WORK-AREA into W-POINTER.
        // Java does NOT expose raw memory addresses; the address is modeled as a
        // logical byte offset into the backing String. SET TO ADDRESS OF WORK-AREA
        // is therefore modeled as setting wPointer to offset 0 (the start of the
        // backing buffer). This is a faithful SEMANTIC translation, not a literal
        // memory operation.
        wPointer = 0;

        // COBOL line 29: SET ADDRESS OF WORK-DATA TO W-POINTER.
        //
        // In COBOL, this aliases the LINKAGE SECTION group WORK-DATA at the address
        // currently held in W-POINTER; subsequent reads/writes to WORK-A and
        // NEXT-WORK-DATA operate on the same bytes as the WORKING-STORAGE
        // WORK-AREA group. Java models the overlay by projecting substring views
        // from the backing buffer at the offset stored in wPointer.
        //
        // WORK-A (PIC X(4)) views bytes [wPointer, wPointer+4) of WORK_AREA.
        // NEXT-WORK-DATA (PIC X(4)) views bytes [wPointer+4, wPointer+8) of
        // WORK_AREA.
        workA = WORK_AREA.substring(wPointer, wPointer + 4);
        nextWorkData = WORK_AREA.substring(wPointer + 4, wPointer + 8);

        // COBOL line 31: DISPLAY "WORK-DATA: " WORK-DATA.
        //
        // The WORK-DATA LINKAGE group is the concatenation of its two sub-fields
        // WORK-A and NEXT-WORK-DATA -- the first 8 bytes of the WORK-AREA backing
        // buffer ("AAAABBBB").
        System.out.println("WORK-DATA: " + workA + nextWorkData);

        // COBOL line 32: DISPLAY "W-POINTER: " W-POINTER.
        //
        // Post-SET sentinel. After SET W-POINTER TO ADDRESS OF WORK-AREA, the COBOL
        // runtime displays an actual memory address. Java emits the sentinel
        // 0x00000010 to deterministically differ from the pre-SET 0x00000000,
        // matched by the same PointerApplicationTest regex used for line 1.
        System.out.println("W-POINTER: " + formatPointer(wPointer));

        // COBOL line 33: DISPLAY "WORK-A: " WORK-A.
        System.out.println("WORK-A: " + workA);

        // COBOL line 34: DISPLAY "NEXT-WORK-DATA : " NEXT-WORK-DATA.
        //
        // **INCONSISTENT FORMATTING PRESERVED**: This line uses "NEXT-WORK-DATA : "
        // (space-colon-space), distinct from the colon-space convention on lines
        // 26, 31, 32, 33. This is an authentic quirk in Pointer.cbl line 34, NOT
        // a bug. Per AAP section 0.7.2 minimal-change clause, the Java translation
        // preserves this inconsistency VERBATIM. Future maintainers MUST NOT
        // "normalize" the spacing -- the PointerApplicationTest fixture and
        // assertions explicitly encode this inconsistency.
        System.out.println("NEXT-WORK-DATA : " + nextWorkData);

        // COBOL line 36: GOBACK. -- implicit in Java; CommandLineRunner.run()
        // returns, Spring Boot tears down the context, and the JVM exits.
    }

    /**
     * Formats the simulated COBOL POINTER value as an 8-hex-digit sentinel string
     * (e.g., {@code 0x00000000} for the uninitialized pre-SET state,
     * {@code 0x00000010} for the post-SET state).
     *
     * <p>Per AAP &sect;0.6.1, the COBOL {@code DISPLAY W-POINTER} statement emits an
     * implementation-defined runtime memory address. Java has no equivalent runtime
     * memory address that can be reliably displayed, so this method returns a
     * deterministic sentinel that matches the test fixture and the regex
     * {@code ^W-POINTER: 0x[0-9A-Fa-f]{8}$}.
     *
     * <p>The mapping convention is:
     * <ul>
     *   <li>{@code offset == -1} (uninitialized) &rarr; {@code 0x00000000}</li>
     *   <li>{@code offset >= 0}   (post-SET)     &rarr; {@code 0x00000010}</li>
     * </ul>
     *
     * @param offset the logical byte offset into {@link #WORK_AREA}, or {@code -1}
     *               if the pointer is uninitialized
     * @return an 8-hex-digit sentinel string prefixed with {@code 0x}
     */
    private static String formatPointer(int offset) {
        // Pre-SET pointer is uninitialized (null/zero in COBOL); emit 0x00000000.
        // Post-SET pointer holds the address of WORK-AREA; emit the deterministic
        // sentinel 0x00000010 (an arbitrary non-zero offset, NOT a real memory
        // address -- Java does not expose raw memory addresses).
        return offset < 0 ? "0x00000000" : "0x00000010";
    }
}
