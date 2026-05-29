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

package com.projectcobol.struct;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/Struct/Struct.cbl}.
 *
 * <p>The original COBOL program {@code STRUCT-EXAMPLE} demonstrates grouped
 * {@code OCCURS} arrays nested inside a parent group structure. The translation
 * preserves the COBOL pedagogical pattern:
 * <ul>
 *   <li>{@code 78-level} constants become {@code static final int} fields.</li>
 *   <li>The {@code W-STRUCT} group with two {@code OCCURS} arrays becomes a
 *       Java record holding two {@code int[]} arrays.</li>
 *   <li>The {@code ARRAY-ONE} and {@code ARRAY-TWO} sections become private
 *       methods invoked from {@link #run(String...)}.</li>
 *   <li>COBOL 1-based array indexing maps to Java 0-based via {@code [w - 1]}.</li>
 *   <li>{@code DISPLAY} with numeric reference modification renders zero-padded
 *       values via {@code String.format("%02d", value)}.</li>
 * </ul>
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the
 * source filename ({@code Struct.cbl} &rarr; {@code StructApplication}) rather
 * than the COBOL {@code PROGRAM-ID} ({@code STRUCT-EXAMPLE}).
 */
@SpringBootApplication
public class StructApplication implements CommandLineRunner {

    // COBOL: 78 W-LEN-ARR1 VALUE 5.  (Struct.cbl L19)
    static final int W_LEN_ARR1 = 5;

    // COBOL: 78 W-LEN-ARR2 VALUE 10. (Struct.cbl L20)
    static final int W_LEN_ARR2 = 10;

    /**
     * Java equivalent of the COBOL {@code 01 W-STRUCT} group from
     * {@code Struct.cbl} lines 22-24. The original COBOL group bundles two
     * {@code OCCURS} arrays under a single parent identifier; in Java we
     * model this as a record holding two primitive {@code int[]} arrays.
     *
     * <p>A record with {@code int[]} components inherits reference-equality
     * {@code equals()} and identity-based {@code hashCode()} for the array
     * fields. That is acceptable here because the {@code WStruct} instance is
     * never compared for value equality nor used as a map key; it merely groups
     * the two backing arrays exactly as the COBOL {@code 01 W-STRUCT} group does.
     *
     * @param arr1 the 5-element array corresponding to {@code W-ARRAY  PIC S99 OCCURS W-LEN-ARR1 TIMES}
     * @param arr2 the 10-element array corresponding to {@code W-ARRAY2 PIC S99 OCCURS W-LEN-ARR2 TIMES}
     */
    private record WStruct(int[] arr1, int[] arr2) { }

    // Single instance allocated up front, mirroring COBOL WORKING-STORAGE allocation.
    private final WStruct wStruct = new WStruct(new int[W_LEN_ARR1], new int[W_LEN_ARR2]);

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
        SpringApplication.run(StructApplication.class, args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code Struct.cbl} lines 27-31:
     * <pre>
     *     PERFORM ARRAY-ONE.
     *     PERFORM ARRAY-TWO.
     *     GOBACK.
     * </pre>
     * The COBOL {@code GOBACK} maps to the implicit method return.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract; this translation never actually throws
     */
    @Override
    public void run(String... args) throws Exception {
        arrayOne();
        arrayTwo();
    }

    /**
     * Translation of the COBOL {@code ARRAY-ONE SECTION} from {@code Struct.cbl}
     * lines 33-45. Two sequential {@code PERFORM UNTIL} loops run over
     * {@code W-ARRAY}:
     * <ol>
     *   <li>Fill loop: writes {@code W-I} into {@code W-ARRAY(W-I)} for
     *       {@code W-I = 1..5}, mapped to {@code arr1[w - 1] = w}.</li>
     *   <li>Display loop: emits {@code "Array1 contains number: "} followed by
     *       the zero-padded element value, one line per element.</li>
     * </ol>
     *
     * <p>The COBOL {@code MOVE 1 TO W-I} reset between the two loops is
     * implicit in Java because each {@code for} loop has its own initializer.
     */
    private void arrayOne() {
        // COBOL: PERFORM UNTIL W-I > W-LEN-ARR1
        //          MOVE W-I TO W-ARRAY(W-I)
        //          ADD 1 TO W-I
        //        END-PERFORM.    (Struct.cbl L35-38)
        for (int w = 1; w <= W_LEN_ARR1; w++) {
            wStruct.arr1()[w - 1] = w;
        }

        // COBOL: PERFORM UNTIL W-I > W-LEN-ARR1
        //          DISPLAY "Array1 contains number: " W-ARRAY(W-I)
        //          ADD 1 TO W-I
        //        END-PERFORM.    (Struct.cbl L42-45)
        for (int w = 1; w <= W_LEN_ARR1; w++) {
            System.out.println("Array1 contains number: "
                    + String.format("%02d", wStruct.arr1()[w - 1]));
        }
    }

    /**
     * Translation of the COBOL {@code ARRAY-TWO SECTION} from {@code Struct.cbl}
     * lines 47-61. Mirrors {@link #arrayOne()} over {@code W-ARRAY2}, but with
     * a 25-hyphen separator between the fill loop and the display loop:
     * <ol>
     *   <li>Fill loop: writes {@code W-J} into {@code W-ARRAY2(W-J)} for
     *       {@code W-J = 1..10}, mapped to {@code arr2[w - 1] = w}.</li>
     *   <li>Separator: emits {@code "-------------------------"} (exactly 25
     *       hyphens) on its own line, matching the COBOL
     *       {@code DISPLAY "-------------------------"} on line 54.</li>
     *   <li>Display loop: emits {@code "Array2 contains number: "} followed by
     *       the zero-padded element value, one line per element.</li>
     * </ol>
     */
    private void arrayTwo() {
        // COBOL: PERFORM UNTIL W-J > W-LEN-ARR2
        //          MOVE W-J TO W-ARRAY2(W-J)
        //          ADD 1 TO W-J
        //        END-PERFORM.    (Struct.cbl L49-52)
        for (int w = 1; w <= W_LEN_ARR2; w++) {
            wStruct.arr2()[w - 1] = w;
        }

        // COBOL: DISPLAY "-------------------------"   (Struct.cbl L54)
        // The literal contains EXACTLY 25 hyphens.
        System.out.println("-------------------------");

        // COBOL: PERFORM UNTIL W-J > W-LEN-ARR2
        //          DISPLAY "Array2 contains number: " W-ARRAY2(W-J)
        //          ADD 1 TO W-J
        //        END-PERFORM.    (Struct.cbl L58-61)
        for (int w = 1; w <= W_LEN_ARR2; w++) {
            System.out.println("Array2 contains number: "
                    + String.format("%02d", wStruct.arr2()[w - 1]));
        }
    }
}
