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

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/Memory/Address.cbl}
 * (PROGRAM-ID {@code WORK-OFFSET}).
 *
 * <p>Demonstrates COBOL {@code POINTER}, {@code ADDRESS OF}, and {@code LINKAGE SECTION}
 * overlay semantics by modeling them with a 16-character backing {@link String} buffer
 * and {@link String#substring(int, int)} views. Structurally identical to
 * {@link PointerApplication} but uses different field names ({@code WK-A..WK-D}
 * instead of {@code AREA-A..AREA-D}) and a different pointer name ({@code WK-PTR}
 * instead of {@code W-POINTER}).
 *
 * <p>The original COBOL program:
 * <ol>
 *   <li>Declares a 16-byte {@code WORK-AREA} group containing four {@code PIC X(4)}
 *       elementaries ({@code WK-A='AAAA'}, {@code WK-B='BBBB'}, {@code WK-C='CCCC'},
 *       {@code WK-D='DDDF'}) and a {@code WK-PTR POINTER}.</li>
 *   <li>Declares a {@code LINKAGE SECTION} group {@code WORK-DATA} with two
 *       {@code PIC X(4)} sub-fields {@code WORK-A} and {@code NEXT-WORK-DATA}.</li>
 *   <li>Captures the address of {@code WORK-AREA} into {@code WK-PTR} via
 *       {@code SET WK-PTR TO ADDRESS OF WORK-AREA}.</li>
 *   <li>Aliases {@code WORK-DATA} at that address via
 *       {@code SET ADDRESS OF WORK-DATA TO WK-PTR}.</li>
 *   <li>Displays {@code WORK-DATA}, {@code WK-PTR}, {@code WORK-A}, and
 *       {@code NEXT-WORK-DATA} to verify the overlay.</li>
 * </ol>
 *
 * <p><b>Java translation strategy</b> (per AAP &sect;0.6.1): Java is memory-safe and
 * exposes no raw pointer or {@code union}-style reinterpretation. The COBOL semantics
 * are modeled with a backing {@link String} buffer plus substring projections. The
 * {@code WK-PTR} variable is simulated with an {@code int} offset into the buffer;
 * pointer-display lines emit a deterministic sentinel ({@code 0x00000000} pre-SET,
 * {@code 0x00000010} post-SET) because Java has no equivalent runtime memory address
 * that can be reliably displayed. The {@code AddressApplicationTest} golden-output
 * test matches these sentinel lines with a regex format-match rather than literal
 * equality.
 *
 * <p>Per AAP &sect;0.7.3 filename traceability rule, this class is named
 * {@code AddressApplication} (from the source filename {@code Address.cbl}), NOT
 * {@code WorkOffsetApplication} (which would be derived from the COBOL
 * {@code PROGRAM-ID. WORK-OFFSET.}). The filesystem name is the authoritative
 * identifier.
 */
// One-program-one-class fidelity: each OpenCobol/Memory/*.cbl program is
// translated to its own @SpringBootApplication entry class. The cobol-memory
// module hosts two entry classes in package com.projectcobol.memory
// (AddressApplication and PointerApplication; AddressApplication is the pinned
// default mainClass). @SpringBootApplication's default @ComponentScan would
// discover the sibling memory application (a @Configuration + CommandLineRunner)
// and run it together in the same JVM, corrupting this program's stdout.
// Disabling the default component-scan filters means this application registers
// ONLY itself (the primary source handed to SpringApplication.run), so exactly
// one COBOL memory translation executes per invocation - preserving the
// one-.cbl-one-class invariant alongside PointerApplication.
@SpringBootApplication
@ComponentScan(useDefaultFilters = false)
public class AddressApplication implements CommandLineRunner {

    /**
     * COBOL: {@code 01 WORK-AREA.} containing four {@code 03 PIC X(4)} elementaries.
     *
     * <p>The COBOL group is laid out contiguously in memory as 16 bytes:
     * {@code WK-A='AAAA'} (bytes 0-3), {@code WK-B='BBBB'} (bytes 4-7),
     * {@code WK-C='CCCC'} (bytes 8-11), {@code WK-D='DDDF'} (bytes 12-15).
     * Note that {@code WK-D} is {@code 'DDDF'} (D-D-D-F), preserved verbatim from
     * {@code OpenCobol/Memory/Address.cbl} line 13.
     *
     * <p>In Java, the contiguous COBOL group is modeled with a single {@link String}
     * backing buffer that concatenates the four 4-character literals in declaration
     * order, yielding a 16-character buffer that can be sliced via
     * {@link String#substring(int, int)} to simulate {@code LINKAGE SECTION} overlay
     * aliasing.
     */
    private static final String WORK_AREA = "AAAA" + "BBBB" + "CCCC" + "DDDF";

    /**
     * COBOL: {@code 01 WK-PTR POINTER.} A simulated logical byte-offset into
     * {@link #WORK_AREA}. Java does not expose raw memory addresses, so the pointer
     * is modeled as an {@code int} offset (range 0..15) and the pointer-display
     * statements emit a deterministic sentinel hexadecimal string instead of an
     * actual address value.
     *
     * <p>{@code -1} indicates the uninitialized/null pointer state (pre-SET), which
     * the pointer-display logic renders as {@code 0x00000000}.
     */
    private int wkPtr = -1;

    /**
     * COBOL: {@code 03 WORK-A PIC X(4).} inside the {@code LINKAGE SECTION} group
     * {@code 01 WORK-DATA.} Aliases the first 4 bytes of the {@code WORK-DATA}
     * overlay (which itself aliases bytes at {@code WK-PTR} into {@link #WORK_AREA}).
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
     * <p>The Spring Boot banner and startup/profile logging are disabled via
     * {@link Banner.Mode#OFF} and
     * {@link SpringApplication#setLogStartupInfo(boolean)} so the executable jar
     * emits ONLY the COBOL-translated stdout (no banner, no {@code Starting…}/
     * {@code No active profile set}/{@code Started…} INFO lines), keeping the
     * documented {@code java -jar} run golden-output-clean for this educational
     * sample.
     *
     * @param args command-line arguments (unused in this educational sample)
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AddressApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * {@link CommandLineRunner} entry point &mdash; the translated COBOL
     * {@code PROCEDURE DIVISION MAIN-PROCEDURE} body.
     *
     * <p>Mirrors {@code OpenCobol/Memory/Address.cbl} lines 22-35, including the
     * five {@code DISPLAY} statements that produce the deterministic 5-line output
     * verified by {@code AddressApplicationTest}.
     *
     * @param args command-line arguments (unused in this educational sample;
     *             present to satisfy the {@link CommandLineRunner} contract)
     * @throws Exception declared per {@link CommandLineRunner}; never actually thrown
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL line 25: DISPLAY "WK-PTR :   " WK-PTR
        //
        // The uninitialized COBOL POINTER displays as an implementation-defined
        // value (typically a hex address or "NULL"). Java has no raw memory address,
        // so we emit the deterministic sentinel 0x00000000 for the pre-SET state.
        // This sentinel is matched by the AddressApplicationTest regex
        // ^WK-PTR :   0x[0-9A-Fa-f]{8}$ rather than literal equality.
        System.out.println("WK-PTR :   " + formatPointer(wkPtr));

        // COBOL line 27: SET WK-PTR TO ADDRESS OF WORK-AREA.
        //
        // In COBOL, this captures the runtime address of WORK-AREA into WK-PTR.
        // Java does NOT expose raw memory addresses; the address is modeled as a
        // logical byte offset into the backing String. SET TO ADDRESS OF WORK-AREA
        // is therefore modeled as setting wkPtr to offset 0 (the start of the
        // backing buffer). This is a faithful SEMANTIC translation, not a literal
        // memory operation.
        wkPtr = 0;

        // COBOL line 28: SET ADDRESS OF WORK-DATA TO WK-PTR.
        //
        // In COBOL, this aliases the LINKAGE SECTION group WORK-DATA at the address
        // currently held in WK-PTR; subsequent reads/writes to WORK-A and
        // NEXT-WORK-DATA operate on the same bytes as the WORKING-STORAGE
        // WORK-AREA group. Java models the overlay by projecting substring views
        // from the backing buffer at the offset stored in wkPtr.
        //
        // WORK-A (PIC X(4)) views bytes [wkPtr, wkPtr+4) of WORK_AREA.
        // NEXT-WORK-DATA (PIC X(4)) views bytes [wkPtr+4, wkPtr+8) of WORK_AREA.
        workA = WORK_AREA.substring(wkPtr, wkPtr + 4);
        nextWorkData = WORK_AREA.substring(wkPtr + 4, wkPtr + 8);

        // COBOL line 30: DISPLAY "WORK-DATA : " WORK-DATA.
        //
        // The WORK-DATA LINKAGE group is the concatenation of its two sub-fields
        // WORK-A and NEXT-WORK-DATA -- the first 8 bytes of the WORK-AREA backing
        // buffer ("AAAABBBB").
        System.out.println("WORK-DATA : " + workA + nextWorkData);

        // COBOL line 31: DISPLAY "WK-PTR :   " WK-PTR.
        //
        // Post-SET sentinel. After SET WK-PTR TO ADDRESS OF WORK-AREA, the COBOL
        // runtime displays an actual memory address. Java emits the sentinel
        // 0x00000010 to deterministically differ from the pre-SET 0x00000000,
        // matched by the same AddressApplicationTest regex used for line 1.
        System.out.println("WK-PTR :   " + formatPointer(wkPtr));

        // COBOL line 32: DISPLAY "WORK-A : " WORK-A.
        System.out.println("WORK-A : " + workA);

        // COBOL line 33: DISPLAY "NEXT-WORK-DATA : " NEXT-WORK-DATA.
        System.out.println("NEXT-WORK-DATA : " + nextWorkData);

        // COBOL line 35: GOBACK. -- implicit in Java; CommandLineRunner.run()
        // returns, Spring Boot tears down the context, and the JVM exits.
    }

    /**
     * Formats the simulated COBOL POINTER value as an 8-hex-digit sentinel string
     * (e.g., {@code 0x00000000} for the uninitialized pre-SET state,
     * {@code 0x00000010} for the post-SET state).
     *
     * <p>Per AAP &sect;0.6.1, the COBOL {@code DISPLAY WK-PTR} statement emits an
     * implementation-defined runtime memory address. Java has no equivalent runtime
     * memory address that can be reliably displayed, so this method returns a
     * deterministic sentinel that matches the test fixture and the regex
     * {@code ^WK-PTR :   0x[0-9A-Fa-f]{8}$}.
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
