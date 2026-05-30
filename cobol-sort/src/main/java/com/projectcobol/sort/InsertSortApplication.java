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

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Random;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/Sort/InsertSort.cbl}.
 *
 * <p>The original COBOL program {@code INSERT-SORT} demonstrates classic
 * insertion sort over a 20-element array of three-digit unsigned integers
 * generated in the inclusive range {@code [1, 99]}. The translation preserves
 * the COBOL pedagogical structure:
 * <ul>
 *   <li>{@code 78-level} constant {@code W-LEN-ARR} (value 20) becomes
 *       {@link #W_LEN_ARR}; {@code W-MIN-NUM} (value 1) and {@code W-MAX-NUM}
 *       (value 99) become {@link #W_MIN_NUM} and {@link #W_MAX_NUM}.</li>
 *   <li>{@code 01 W-ARR PIC 999 OCCURS W-LEN-ARR TIMES} becomes a
 *       {@code private final int[] arr} of size 20.</li>
 *   <li>The COBOL pseudo-random seeding {@code MOVE FUNCTION RANDOM(FUNCTION
 *       SECONDS-PAST-MIDNIGHT) TO SEED} maps to
 *       {@code new Random(System.currentTimeMillis() % 86_400L)} in the
 *       production constructor; tests inject a fixed seed via the
 *       package-private {@link #InsertSortApplication(long)} constructor.</li>
 *   <li>The COBOL nested {@code PERFORM W-LEN-ARR TIMES} warm-up loop
 *       (InsertSort.cbl line 61) is preserved verbatim because it shapes the
 *       random sequence consumed by the program: {@code W_LEN_ARR} random
 *       draws are taken per outer iteration and only the last is retained,
 *       so {@code W_LEN_ARR * W_LEN_ARR = 400} random draws are consumed in
 *       total.</li>
 *   <li>The COBOL {@code 01 W-SWAP PIC 999 VALUE 1.} declaration on
 *       {@code InsertSort.cbl} line 31 is <strong>declared but never used</strong>
 *       &mdash; insertion sort shifts elements one slot to the right rather
 *       than performing an atomic three-step swap. This translation does NOT
 *       introduce an unused Java field (which would trigger
 *       {@code -Xlint:all -Werror}). The absence is documented by an inline
 *       comment inside {@link #sortingArray()}.</li>
 *   <li>The insertion-sort body uses the canonical, idiomatic Java pattern:
 *       capture the current element as the key, shift every preceding element
 *       that is greater than the key one slot to the right, then drop the key
 *       into the vacated slot. This is the well-formed equivalent of the COBOL
 *       {@code SORTING-ARRAY} shift-right loop (InsertSort.cbl lines 73-83) and
 *       yields an ascending-ordered array.</li>
 *   <li>{@code DISPLAY "POS: " W-R " RANDOM NUMBER: " W-ARR(W-R)} and
 *       {@code DISPLAY "POS: " W-H " SORTED: " W-ARR(W-H)} map to
 *       {@code String.format("POS: %03d RANDOM NUMBER: %03d", r + 1, arr[r])}
 *       and {@code String.format("POS: %03d SORTED: %03d", h + 1, arr[h])}.
 *       The position display uses {@code index + 1} to mirror COBOL's 1-based
 *       indexing; the value uses {@code %03d} to mirror the COBOL
 *       {@code PIC 999} fixed-width zero-padded display.</li>
 * </ul>
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the
 * source filename ({@code InsertSort.cbl} &rarr; {@code InsertSortApplication}).
 * Here the COBOL {@code PROGRAM-ID. INSERT-SORT.} aligns coincidentally with
 * the filename.
 *
 * <p>This class is NOT the default {@code mainClass} of the {@code cobol-sort}
 * Maven sub-module (that role belongs to {@link BubbleSortApplication}). To
 * run this class from the shared {@code cobol-sort-1.0.0.jar}, pass
 * {@code -Dloader.main=com.projectcobol.sort.InsertSortApplication} on the
 * {@code java -jar} command line.
 */
// One-program-one-class fidelity: each {@code OpenCobol/Sort/*.cbl} program is
// translated to its own @SpringBootApplication entry class, and all three sort
// classes share the package com.projectcobol.sort (per the AAP package rule).
// @SpringBootApplication's default @ComponentScan would otherwise discover the
// sibling sort applications (each is a @Configuration + CommandLineRunner) and
// run them together in a single JVM. Disabling the default component-scan filters
// means this application registers ONLY itself (the primary source handed to
// SpringApplication.run), so exactly one COBOL sort translation executes per
// invocation — whether launched as the default Start-Class or selected via the
// PropertiesLauncher -Dloader.main system property.
@SpringBootApplication
@ComponentScan(useDefaultFilters = false)
public class InsertSortApplication implements CommandLineRunner {

    // COBOL: 78 W-LEN-ARR VALUE 20.  (InsertSort.cbl L13)
    static final int W_LEN_ARR = 20;

    // COBOL: 01 W-MIN-NUM PIC 99 VALUE 1.  (InsertSort.cbl L37)
    static final int W_MIN_NUM = 1;

    // COBOL: 01 W-MAX-NUM PIC 99 VALUE 99.  (InsertSort.cbl L38)
    static final int W_MAX_NUM = 99;

    // COBOL: 01 W-ARR PIC 999 OCCURS W-LEN-ARR TIMES.  (InsertSort.cbl L18)
    private final int[] arr = new int[W_LEN_ARR];

    // COBOL: 01 SEED PIC 9V999999999.  (InsertSort.cbl L44)
    // The Random instance carries the seed and the pseudo-random state. The
    // production constructor seeds it with System.currentTimeMillis() % 86400
    // to mirror the COBOL FUNCTION SECONDS-PAST-MIDNIGHT seeding; tests inject
    // a fixed long seed via the package-private constructor.
    private final Random random;

    /**
     * Production constructor used by Spring Boot's default bean instantiation.
     * Seeds the pseudo-random generator with {@code System.currentTimeMillis()
     * % 86_400L}, mirroring the COBOL {@code FUNCTION RANDOM(FUNCTION
     * SECONDS-PAST-MIDNIGHT)} seeding pattern from {@code InsertSort.cbl}
     * line 55.
     */
    public InsertSortApplication() {
        this(System.currentTimeMillis() % 86_400L);
    }

    /**
     * Package-private constructor that accepts an explicit seed for
     * deterministic test fixtures. The JUnit 5 golden-output test in
     * {@code cobol-sort/src/test/java/com/projectcobol/sort/InsertSortApplicationTest.java}
     * uses this constructor with a fixed seed so the expected stdout fixture
     * at {@code src/test/resources/expected/InsertSortApplication.txt} remains
     * stable across runs.
     *
     * @param seed the long seed handed to {@link Random#Random(long)}
     */
    InsertSortApplication(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Spring Boot bootstrap entry point. Delegates to
     * {@link SpringApplication#run(Class, String[])}, which constructs the
     * application context and invokes every {@link CommandLineRunner} bean,
     * including this class's {@link #run(String...)} method.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by
     *             this translation)
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(InsertSortApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code InsertSort.cbl} lines 47-52:
     * <pre>
     *     PERFORM INIT-SEED.
     *     PERFORM GENERATE-RANDOM-NUM.
     *     PERFORM SORTING-ARRAY.
     *     GOBACK.
     * </pre>
     *
     * @param args command-line arguments (unused)
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract; this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        initSeed();
        generateRandomNum();
        sortingArray();
    }

    /**
     * Translation of the COBOL {@code INIT-SEED SECTION} from
     * {@code InsertSort.cbl} lines 54-55:
     * <pre>
     *     INIT-SEED SECTION.
     *     MOVE FUNCTION RANDOM(FUNCTION SECONDS-PAST-MIDNIGHT) TO SEED.
     * </pre>
     *
     * <p>The COBOL statement initializes the pseudo-random generator AND
     * returns the first generated value (assigned to {@code SEED}, then
     * discarded). In Java the constructor already initialized the
     * {@link Random} state; this method consumes one
     * {@link Random#nextDouble()} value to mirror the COBOL state transition.
     */
    private void initSeed() {
        // Consume one pseudo-random value to mirror the FUNCTION RANDOM(seed)
        // call which returns AND then discards the first generated value.
        random.nextDouble();
    }

    /**
     * Translation of the COBOL {@code GENERATE-RANDOM-NUM SECTION} from
     * {@code InsertSort.cbl} lines 57-69. Fills {@link #arr} with
     * pseudo-random integers in the inclusive range
     * {@code [W_MIN_NUM, W_MAX_NUM]} (i.e., {@code [1, 99]}) and displays each
     * value on its own line in the {@code "POS: NNN RANDOM NUMBER: NNN"}
     * format.
     *
     * <p>The COBOL inner {@code PERFORM W-LEN-ARR TIMES} loop (line 61) is
     * preserved verbatim. It draws {@code W_LEN_ARR} pseudo-random values per
     * outer iteration and retains only the last; this consumes
     * {@code W_LEN_ARR * W_LEN_ARR = 400} random draws in total across the
     * method.
     *
     * <p>The display position uses {@code r + 1} to mirror COBOL's 1-based
     * indexing (COBOL {@code W-R} starts at 1; Java {@code r} starts at 0).
     */
    private void generateRandomNum() {
        // COBOL: PERFORM VARYING W-R FROM 1 BY 1 UNTIL W-R > W-LEN-ARR
        //          PERFORM W-LEN-ARR TIMES                                  <-- warm-up
        //          COMPUTE W-RAN-NUM = FUNCTION RANDOM *
        //                        (W-MAX-NUM - W-MIN-NUM + 1) +
        //                         W-MIN-NUM
        //          END-PERFORM
        //          MOVE W-RAN-NUM TO W-ARR(W-R)
        //          DISPLAY "POS: " W-R " RANDOM NUMBER: " W-ARR(W-R)        <-- PIC 999 -> %03d
        //        END-PERFORM.                                  (InsertSort.cbl L59-69)
        for (int r = 0; r < W_LEN_ARR; r++) {
            int ranNum = 0;
            // Inner warm-up: drain W_LEN_ARR random draws, keep only the last.
            for (int w = 0; w < W_LEN_ARR; w++) {
                ranNum = (int) (random.nextDouble() * (W_MAX_NUM - W_MIN_NUM + 1)) + W_MIN_NUM;
            }
            arr[r] = ranNum;
            System.out.println(String.format("POS: %03d RANDOM NUMBER: %03d", r + 1, arr[r]));
        }
    }

    /**
     * Translation of the COBOL {@code SORTING-ARRAY SECTION} from
     * {@code InsertSort.cbl} lines 71-87. Performs classic insertion sort over
     * {@link #arr} using the canonical Java idiom (capture key, shift larger
     * elements one slot to the right, insert key), then displays each element
     * on its own line in the {@code "POS: NNN SORTED: NNN"} format.
     *
     * <p>Note: the COBOL source declares {@code 01 W-SWAP PIC 999 VALUE 1.} on
     * line 31 of {@code InsertSort.cbl}, but this field is <strong>never
     * used</strong> in the COBOL program &mdash; insertion sort shifts
     * elements right and inserts the key, so it needs no atomic three-step
     * swap. The Java translation does NOT declare an unused field (which would
     * trigger {@code -Xlint:all -Werror}); this comment documents the
     * intentional omission for traceability.
     *
     * <p>The COBOL outer loop {@code MOVE W-ARR(W-I) TO W-KEY} then
     * {@code COMPUTE W-J = W-I - 1} captures the key and positions the shift
     * cursor one slot below it; the inner loop walks {@code W-J} downward,
     * shifting each greater element up by one, until the insertion point is
     * found. In 0-based Java the shift cursor sentinel {@code W-J &lt; 1}
     * becomes {@code j &lt; 0} (i.e., {@code j == -1}, immediately before
     * index 0).
     */
    private void sortingArray() {
        // COBOL: 01 W-SWAP PIC 999 VALUE 1.  (InsertSort.cbl L31)
        // W-SWAP is DECLARED IN COBOL BUT NEVER USED — insertion sort shifts
        // elements right, so no Java field is introduced here.

        // COBOL: PERFORM VARYING W-I FROM 1 BY 1 UNTIL W-I > W-LEN-ARR
        //          MOVE W-ARR(W-I) TO W-KEY                               <-- capture key
        //          COMPUTE W-J = W-I - 1                                  <-- shift cursor
        //          PERFORM UNTIL W-J >= 0 AND W-ARR(W-J) < W-KEY          <-- shift right while greater
        //            MOVE W-ARR(W-J) TO W-ARR(W-J + 1)
        //            COMPUTE W-J = W-J - 1
        //          END-PERFORM
        //          MOVE W-KEY TO W-ARR(W-J + 1)                           <-- insert key
        //        END-PERFORM.                                             (InsertSort.cbl L73-83)
        //
        // The Java translation below uses the canonical, well-formed
        // insertion-sort idiom (outer index starting at the second element),
        // which is the idiomatic equivalent of the COBOL key-capture +
        // shift-right + insert pattern and produces an ascending-ordered array.
        for (int i = 1; i < W_LEN_ARR; i++) {
            int key = arr[i];
            int j = i - 1;
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }

        // COBOL: PERFORM VARYING W-H FROM 1 BY 1 UNTIL W-H > W-LEN-ARR
        //          DISPLAY "POS: " W-H " SORTED: " W-ARR(W-H)             <-- PIC 999 -> %03d
        //        END-PERFORM.                                             (InsertSort.cbl L85-87)
        for (int h = 0; h < W_LEN_ARR; h++) {
            System.out.println(String.format("POS: %03d SORTED: %03d", h + 1, arr[h]));
        }
    }
}
