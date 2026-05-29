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

package com.projectcobol.sort;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Random;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/Sort/SelectSort.cbl}.
 *
 * <p>The original COBOL program {@code SELECT-SORT} demonstrates classic
 * selection sort over a 30-element array of three-digit unsigned integers
 * generated in the inclusive range {@code [1, 200]}. The translation
 * preserves the COBOL pedagogical structure:
 * <ul>
 *   <li>{@code 78-level} constant {@code W-LEN-ARR} (value 30) becomes
 *       {@link #W_LEN_ARR}; {@code W-MIN-NUMBER} (value 1) and
 *       {@code W-MAX-NUMBER} (value 200) become {@link #W_MIN_NUMBER} and
 *       {@link #W_MAX_NUMBER}. The COBOL name suffix {@code NUMBER} (rather
 *       than {@code NUM} used in {@code BubbleSort.cbl} and
 *       {@code InsertSort.cbl}) is preserved verbatim for traceability.</li>
 *   <li>{@code 01 W-ARR PIC 999 OCCURS W-LEN-ARR TIMES} becomes a
 *       {@code private final int[] arr} of size 30.</li>
 *   <li><strong>The COBOL {@code SELECT-SORT} program does NOT call
 *       {@code PERFORM INIT-SEED}</strong> and does not declare an
 *       {@code INIT-SEED SECTION} (compare with {@code BubbleSort.cbl} and
 *       {@code InsertSort.cbl}, which both do). The Java translation mirrors
 *       this exactly: {@link #run(String...)} calls only
 *       {@link #generateRandomNum()} followed by {@link #sortingArray()};
 *       there is <strong>no {@code initSeed()} method</strong> in this
 *       class. The pseudo-random generator is still seeded in the
 *       constructor with {@code System.currentTimeMillis() % 86_400L} to
 *       mirror the COBOL {@code FUNCTION SECONDS-PAST-MIDNIGHT} seeding
 *       idiom, but the COBOL state-transition trick (FUNCTION RANDOM
 *       returning AND discarding the first generated value) is absent
 *       here.</li>
 *   <li>The COBOL nested {@code PERFORM W-LEN-ARR TIMES} warm-up loop
 *       (SelectSort.cbl line 54) is preserved verbatim because it shapes the
 *       random sequence consumed by the program: {@code W_LEN_ARR} random
 *       draws are taken per outer iteration and only the last is retained,
 *       so {@code W_LEN_ARR * W_LEN_ARR = 900} random draws are consumed in
 *       total.</li>
 *   <li>The COBOL selection-sort body initializes {@code W-MIN} to
 *       {@code W-LEN-ARR} ({@code MOVE W-LEN-ARR TO W-MIN}, SelectSort.cbl
 *       line 67), which in 1-based COBOL refers to the LAST element
 *       (index 30). In 0-based Java this translates to
 *       {@code int min = W_LEN_ARR - 1} (index 29). The inner scan runs
 *       {@code W-J} from {@code W-I} to {@code W-LEN-ARR} (in 0-based Java:
 *       {@code j = i .. W_LEN_ARR - 1}), updating {@code min} whenever
 *       {@code arr[min] > arr[j]}.</li>
 *   <li>The COBOL 3-step swap via {@code W-SWAP} (SelectSort.cbl lines
 *       78-80) <strong>always executes</strong> &mdash; the COBOL code does
 *       NOT guard the swap with {@code IF W-MIN NOT = W-I}. The Java
 *       translation honors this exactly: the swap is unconditional. Where
 *       {@code min == i} (the current element is already the smallest) the
 *       swap is a no-op three-step copy that leaves the array unchanged.
 *       (For uniformly random input this produces identical output to a
 *       guarded swap; the unguarded version is preserved purely for
 *       behavioral fidelity to the COBOL source.)</li>
 *   <li>{@code DISPLAY "POS: " W-R " RANDOM NUMBER: " W-ARR(W-R)} and
 *       {@code DISPLAY "POS: " W-H " SORTED: " W-ARR(W-H)} map to
 *       {@code String.format("POS: %03d RANDOM NUMBER: %03d", r + 1,
 *       arr[r])} and {@code String.format("POS: %03d SORTED: %03d", h + 1,
 *       arr[h])}. The position display uses {@code index + 1} to mirror
 *       COBOL's 1-based indexing; the value uses {@code %03d} to mirror the
 *       COBOL {@code PIC 999} fixed-width zero-padded display.</li>
 * </ul>
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the
 * source filename ({@code SelectSort.cbl} &rarr; {@code SelectSortApplication}).
 * Here the COBOL {@code PROGRAM-ID. SELECT-SORT.} aligns coincidentally with
 * the filename.
 *
 * <p>This class is NOT the default {@code mainClass} of the {@code cobol-sort}
 * Maven sub-module (that role belongs to {@link BubbleSortApplication}). To
 * run this class from the shared {@code cobol-sort-1.0.0.jar}, pass
 * {@code -Dloader.main=com.projectcobol.sort.SelectSortApplication} on the
 * {@code java -jar} command line.
 */
@SpringBootApplication
public class SelectSortApplication implements CommandLineRunner {

    // COBOL: 78 W-LEN-ARR VALUE 30.  (SelectSort.cbl L15)
    static final int W_LEN_ARR = 30;

    // COBOL: 01 W-MIN-NUMBER PIC 999 VALUE 1.  (SelectSort.cbl L39)
    // Note: SelectSort.cbl uses the suffix "-NUMBER" rather than "-NUM" used
    // by BubbleSort.cbl and InsertSort.cbl. The Java field name mirrors the
    // COBOL identifier verbatim for traceability.
    static final int W_MIN_NUMBER = 1;

    // COBOL: 01 W-MAX-NUMBER PIC 999 VALUE 200.  (SelectSort.cbl L40)
    static final int W_MAX_NUMBER = 200;

    // COBOL: 01 W-ARR PIC 999 OCCURS W-LEN-ARR TIMES.  (SelectSort.cbl L20)
    private final int[] arr = new int[W_LEN_ARR];

    // The COBOL program does NOT declare a SEED variable and does NOT call
    // PERFORM INIT-SEED. The Java translation still seeds the Random instance
    // in the constructor (to satisfy the deterministic-seeding pattern from
    // AAP §0.7.3), but does NOT consume a "warm-up" pseudo-random value in
    // run() — there is no initSeed() method.
    private final Random random;

    /**
     * Production constructor used by Spring Boot's default bean instantiation.
     * Seeds the pseudo-random generator with {@code System.currentTimeMillis()
     * % 86_400L}. Unlike the sibling {@link BubbleSortApplication} and
     * {@link InsertSortApplication} classes, this class does NOT mirror a
     * COBOL {@code FUNCTION RANDOM(FUNCTION SECONDS-PAST-MIDNIGHT)} call in
     * {@code run(...)} because the COBOL {@code SELECT-SORT} program omits the
     * {@code INIT-SEED SECTION} entirely.
     */
    public SelectSortApplication() {
        this(System.currentTimeMillis() % 86_400L);
    }

    /**
     * Package-private constructor that accepts an explicit seed for
     * deterministic test fixtures. The JUnit 5 golden-output test in
     * {@code cobol-sort/src/test/java/com/projectcobol/sort/SelectSortApplicationTest.java}
     * uses this constructor with a fixed seed so the expected stdout fixture
     * at {@code src/test/resources/expected/SelectSortApplication.txt} remains
     * stable across runs.
     *
     * @param seed the long seed handed to {@link Random#Random(long)}
     */
    SelectSortApplication(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Spring Boot bootstrap entry point. Delegates to
     * {@link SpringApplication#run(Class, String[])}, which constructs the
     * application context and invokes every {@link CommandLineRunner} bean,
     * including this class's {@link #run(String...)} method.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused)
     */
    public static void main(String[] args) {
        SpringApplication.run(SelectSortApplication.class, args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code SelectSort.cbl} lines 44-48:
     * <pre>
     *     PERFORM GENERATE-RANDOM-NUM.
     *     PERFORM SORTING-ARRAY.
     *     GOBACK.
     * </pre>
     *
     * <p><strong>Note:</strong> Unlike {@link BubbleSortApplication#run(String...)}
     * and {@link InsertSortApplication#run(String...)}, this method does NOT
     * call an {@code initSeed()} helper because the COBOL source omits
     * {@code PERFORM INIT-SEED} from {@code MAIN-PROCEDURE} entirely. The
     * pseudo-random generator was already seeded in the constructor.
     *
     * @param args command-line arguments (unused)
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract; this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        // NOTE: no initSeed() call here — COBOL SELECT-SORT MAIN-PROCEDURE
        // does NOT contain PERFORM INIT-SEED (compare with BubbleSort.cbl and
        // InsertSort.cbl, both of which DO call INIT-SEED from MAIN-PROCEDURE).
        generateRandomNum();
        sortingArray();
    }

    /**
     * Translation of the COBOL {@code GENERATE-RANDOM-NUM SECTION} from
     * {@code SelectSort.cbl} lines 50-62. Fills {@link #arr} with
     * pseudo-random integers in the inclusive range
     * {@code [W_MIN_NUMBER, W_MAX_NUMBER]} (i.e., {@code [1, 200]}) and
     * displays each value on its own line in the
     * {@code "POS: NNN RANDOM NUMBER: NNN"} format.
     *
     * <p>The COBOL inner {@code PERFORM W-LEN-ARR TIMES} loop (line 54) is
     * preserved verbatim. It draws {@code W_LEN_ARR} pseudo-random values per
     * outer iteration and retains only the last; this consumes
     * {@code W_LEN_ARR * W_LEN_ARR = 900} random draws in total across the
     * method.
     *
     * <p>The COBOL outer loop is a {@code PERFORM VARYING W-R FROM 1 BY 1
     * UNTIL W-R > W-LEN-ARR} (line 52); the {@code PERFORM VARYING} handles
     * the {@code W-R} increment intrinsically, so the Java {@code for} loop
     * counter {@code r} is the faithful equivalent and no separate manual
     * counter is required.
     */
    private void generateRandomNum() {
        // COBOL: PERFORM VARYING W-R FROM 1 BY 1 UNTIL W-R > W-LEN-ARR
        //          PERFORM W-LEN-ARR TIMES                                  <-- warm-up
        //          COMPUTE W-RAN-NUMBER = FUNCTION RANDOM *
        //                        (W-MAX-NUMBER - W-MIN-NUMBER + 1) +
        //                         W-MIN-NUMBER
        //          END-PERFORM
        //          MOVE W-RAN-NUMBER TO W-ARR(W-R)
        //          DISPLAY "POS: " W-R " RANDOM NUMBER: " W-ARR(W-R)        <-- PIC 999 -> %03d
        //        END-PERFORM.                                  (SelectSort.cbl L52-62)
        for (int r = 0; r < W_LEN_ARR; r++) {
            int ranNumber = 0;
            // Inner warm-up: drain W_LEN_ARR random draws, keep only the last.
            for (int w = 0; w < W_LEN_ARR; w++) {
                ranNumber = (int) (random.nextDouble() * (W_MAX_NUMBER - W_MIN_NUMBER + 1)) + W_MIN_NUMBER;
            }
            arr[r] = ranNumber;
            System.out.println(String.format("POS: %03d RANDOM NUMBER: %03d", r + 1, arr[r]));
        }
    }

    /**
     * Translation of the COBOL {@code SORTING-ARRAY SECTION} from
     * {@code SelectSort.cbl} lines 64-87. Performs classic selection sort over
     * {@link #arr} using the COBOL {@code W-MIN} cursor + {@code W-SWAP}
     * three-step swap pattern, then displays each element on its own line in
     * the {@code "POS: NNN SORTED: NNN"} format.
     *
     * <p>The COBOL line {@code MOVE W-LEN-ARR TO W-MIN} (line 67) initializes
     * the running-minimum index to {@code W-LEN-ARR} &mdash; in 1-based COBOL
     * this is the position of the LAST element (index 30). Translated to
     * 0-based Java this is {@code int min = W_LEN_ARR - 1} (index 29). The
     * inner loop scans {@code W-J} from {@code W-I} to {@code W-LEN-ARR} (in
     * 0-based Java: {@code j = i .. W_LEN_ARR - 1}), updating {@code min}
     * whenever {@code arr[min] > arr[j]}.
     *
     * <p>The COBOL 3-step swap via {@code W-SWAP} (lines 78-80) always
     * executes &mdash; the COBOL code does NOT guard the swap with
     * {@code IF W-MIN NOT = W-I}. The Java translation honors this exactly:
     * the swap is unconditional. Where {@code min == i} (the current element
     * is already the smallest), the swap is a no-op three-step copy that
     * leaves the array unchanged.
     */
    private void sortingArray() {
        // COBOL: PERFORM UNTIL W-I > W-LEN-ARR
        //          MOVE W-LEN-ARR TO W-MIN                                  <-- W-MIN = last element (1-based)
        //          MOVE W-I TO W-J
        //          PERFORM UNTIL W-J > W-LEN-ARR
        //            IF W-ARR(W-MIN) > W-ARR(W-J)
        //              MOVE W-J TO W-MIN
        //            END-IF
        //            ADD 1 TO W-J
        //          END-PERFORM
        //          MOVE W-ARR(W-MIN) TO W-SWAP                              <-- 3-step swap (unconditional)
        //          MOVE W-ARR(W-I) TO W-ARR(W-MIN)
        //          MOVE W-SWAP TO W-ARR(W-I)
        //          ADD 1 TO W-I
        //        END-PERFORM.                                               (SelectSort.cbl L66-83)
        for (int i = 0; i < W_LEN_ARR; i++) {
            // COBOL: MOVE W-LEN-ARR TO W-MIN — initial min = last element.
            // 1-based COBOL index W-LEN-ARR (= 30) translates to 0-based
            // Java index W_LEN_ARR - 1 (= 29).
            int min = W_LEN_ARR - 1;
            for (int j = i; j < W_LEN_ARR; j++) {
                if (arr[min] > arr[j]) {
                    min = j;
                }
            }
            // Unconditional 3-step swap via local int swap (mirrors W-SWAP);
            // executed even when min == i, matching the COBOL exactly.
            int swap = arr[min];
            arr[min] = arr[i];
            arr[i] = swap;
        }

        // COBOL: PERFORM VARYING W-H FROM 1 BY 1 UNTIL W-H > W-LEN-ARR
        //          DISPLAY "POS: " W-H " SORTED: " W-ARR(W-H)               <-- PIC 999 -> %03d
        //        END-PERFORM.                                               (SelectSort.cbl L85-87)
        for (int h = 0; h < W_LEN_ARR; h++) {
            System.out.println(String.format("POS: %03d SORTED: %03d", h + 1, arr[h]));
        }
    }
}
