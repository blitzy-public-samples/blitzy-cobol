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
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/Sort/BubbleSort.cbl}.
 *
 * <p>The original COBOL program {@code BUBBLE-SORT} demonstrates classic
 * bubble sort over a 10-element array of two-digit unsigned integers. The
 * translation preserves the COBOL pedagogical structure:
 * <ul>
 *   <li>{@code 78-level} constants ({@code W-LEN-ARR}, {@code W-MIN-NUM},
 *       {@code W-MAX-NUM}) become {@code static final int} fields.</li>
 *   <li>{@code 01 W-ARR PIC 99 OCCURS W-LEN-ARR TIMES} becomes a
 *       {@code private final int[] arr} initialized to size
 *       {@link #W_LEN_ARR}.</li>
 *   <li>The COBOL pseudo-random seeding {@code MOVE FUNCTION RANDOM(FUNCTION
 *       SECONDS-PAST-MIDNIGHT) TO SEED} maps to
 *       {@code new Random(System.currentTimeMillis() % 86_400L)} in the
 *       production constructor; tests inject a fixed seed via the
 *       package-private {@link #BubbleSortApplication(long)} constructor for
 *       deterministic golden-output fixtures.</li>
 *   <li>The COBOL nested {@code PERFORM W-LEN-ARR TIMES} warm-up loop is
 *       preserved verbatim because it shapes the random sequence consumed by
 *       the program (10 random draws per outer iteration, only the last is
 *       retained).</li>
 *   <li>{@code COMPUTE W-RAN-NUM = FUNCTION RANDOM * (W-MAX-NUM - W-MIN-NUM
 *       + 1) + W-MIN-NUM} becomes the standard {@code (int)(random.nextDouble()
 *       * (max - min + 1)) + min} idiom yielding integers in {@code [min, max]}
 *       inclusive.</li>
 *   <li>{@code DISPLAY 'Unsorted: ' W-ARR(W-I)} and {@code DISPLAY 'Sorted: '
 *       W-ARR(W-H)} map to {@code System.out.println(String.format("Unsorted: %02d", ...))}
 *       and {@code String.format("Sorted: %02d", ...)} respectively &mdash; the
 *       {@code %02d} format mirrors the COBOL {@code PIC 99} fixed-width
 *       zero-padded display.</li>
 *   <li>The bubble sort {@code PERFORM VARYING W-J ... PERFORM VARYING W-K ...
 *       IF W-ARR(W-K) > W-ARR(W-K + 1)} becomes a nested Java {@code for}
 *       loop with the canonical three-step swap via a local {@code int temp}.
 *       The COBOL {@code UNTIL W-K >= W-LEN-ARR} condition is strict (the
 *       inner loop runs {@code W-K = 1..9} inclusive), translated to
 *       {@code k < W_LEN_ARR - 1} in 0-based Java so the safe maximum index
 *       accessed via {@code arr[k + 1]} remains {@code W_LEN_ARR - 1}.</li>
 * </ul>
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the
 * source filename ({@code BubbleSort.cbl} &rarr; {@code BubbleSortApplication}).
 * The COBOL {@code PROGRAM-ID. BUBBLE-SORT.} happens to align with the
 * filename here, but the rule applies uniformly across the codebase.
 *
 * <p>This is the default {@code mainClass} of the {@code cobol-sort} Maven
 * sub-module (pinned in {@code cobol-sort/pom.xml}). To run the sibling
 * {@code InsertSortApplication} or {@code SelectSortApplication} from the
 * shared {@code cobol-sort-1.0.0.jar}, pass
 * {@code -Dloader.main=<fully.qualified.ClassName>} on the {@code java -jar}
 * command line.
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
public class BubbleSortApplication implements CommandLineRunner {

    // COBOL: 78 W-LEN-ARR VALUE 10.  (BubbleSort.cbl L16)
    static final int W_LEN_ARR = 10;

    // COBOL: 01 W-MIN-NUM PIC 99 VALUE 1.  (BubbleSort.cbl L39)
    static final int W_MIN_NUM = 1;

    // COBOL: 01 W-MAX-NUM PIC 99 VALUE 99.  (BubbleSort.cbl L40)
    static final int W_MAX_NUM = 99;

    // COBOL: 01 W-ARR PIC 99 OCCURS W-LEN-ARR TIMES.  (BubbleSort.cbl L21)
    private final int[] arr = new int[W_LEN_ARR];

    // COBOL: 01 SEED PIC 9V999999999.  (BubbleSort.cbl L46)
    // The Random instance carries the seed and the pseudo-random state. The
    // production constructor seeds it with System.currentTimeMillis() % 86400
    // to mirror the COBOL FUNCTION SECONDS-PAST-MIDNIGHT seeding; tests inject
    // a fixed long seed via the package-private constructor.
    private final Random random;

    /**
     * Production constructor used by Spring Boot's default bean instantiation.
     * Seeds the pseudo-random generator with {@code System.currentTimeMillis()
     * % 86_400L}, mirroring the COBOL {@code FUNCTION RANDOM(FUNCTION
     * SECONDS-PAST-MIDNIGHT)} seeding pattern.
     */
    public BubbleSortApplication() {
        this(System.currentTimeMillis() % 86_400L);
    }

    /**
     * Package-private constructor that accepts an explicit seed for
     * deterministic test fixtures. The JUnit 5 golden-output test in
     * {@code cobol-sort/src/test/java/com/projectcobol/sort/BubbleSortApplicationTest.java}
     * uses this constructor with a fixed seed so the expected stdout fixture
     * at {@code src/test/resources/expected/BubbleSortApplication.txt} remains
     * stable across runs.
     *
     * @param seed the long seed handed to {@link Random#Random(long)}
     */
    BubbleSortApplication(long seed) {
        this.random = new Random(seed);
    }

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
        SpringApplication app = new SpringApplication(BubbleSortApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code BubbleSort.cbl} lines 49-54:
     * <pre>
     *     PERFORM INIT-SEED.
     *     PERFORM GENERATE-RANDOM-NUM.
     *     PERFORM SORTING-ARRAY.
     *     GOBACK.
     * </pre>
     * The COBOL {@code GOBACK} maps to the implicit Java method return after
     * the three private method calls.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
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
     * {@code BubbleSort.cbl} lines 56-57:
     * <pre>
     *     INIT-SEED SECTION.
     *     MOVE FUNCTION RANDOM(FUNCTION SECONDS-PAST-MIDNIGHT) TO SEED.
     * </pre>
     *
     * <p>The COBOL statement {@code FUNCTION RANDOM(arg)} initializes the
     * pseudo-random generator with the supplied seed AND returns the first
     * generated value (assigned to {@code SEED}, then discarded). In Java the
     * constructor already initialized the {@link Random} state; this method
     * consumes one {@link Random#nextDouble()} value to mirror the COBOL
     * state transition where the first random number was captured and then
     * unused. This keeps the subsequent {@link #generateRandomNum()} sequence
     * aligned with the COBOL original.
     */
    private void initSeed() {
        // Consume one pseudo-random value to mirror the FUNCTION RANDOM(seed)
        // call which returns AND then discards the first generated value.
        random.nextDouble();
    }

    /**
     * Translation of the COBOL {@code GENERATE-RANDOM-NUM SECTION} from
     * {@code BubbleSort.cbl} lines 59-69. Fills {@link #arr} with
     * pseudo-random integers in the inclusive range
     * {@code [W_MIN_NUM, W_MAX_NUM]} and displays each value on its own line
     * with the {@code "Unsorted: "} prefix.
     *
     * <p>The COBOL inner {@code PERFORM W-LEN-ARR TIMES} loop (line 62) is
     * preserved verbatim. It draws {@code W_LEN_ARR} pseudo-random values per
     * outer iteration and retains only the last; this consumes
     * {@code W_LEN_ARR * W_LEN_ARR} random draws total across the method. The
     * folder-level prompt mandates preservation of this loop because it shapes
     * the pseudo-random sequence used by the golden-output fixtures.
     */
    private void generateRandomNum() {
        // COBOL: PERFORM VARYING W-I FROM 1 BY 1 UNTIL W-I > W-LEN-ARR
        //          PERFORM W-LEN-ARR TIMES                                  <-- warm-up: W_LEN_ARR random draws per outer
        //            COMPUTE W-RAN-NUM = FUNCTION RANDOM
        //                              * (W-MAX-NUM - W-MIN-NUM + 1)
        //                              + W-MIN-NUM
        //          END-PERFORM
        //          MOVE W-RAN-NUM TO W-ARR(W-I)
        //          DISPLAY 'Unsorted: ' W-ARR(W-I)                          <-- PIC 99 -> zero-padded 2-digit
        //        END-PERFORM.                                  (BubbleSort.cbl L61-69)
        for (int i = 0; i < W_LEN_ARR; i++) {
            int ranNum = 0;
            // Inner warm-up: drain W_LEN_ARR random draws, keep only the last.
            for (int w = 0; w < W_LEN_ARR; w++) {
                ranNum = (int) (random.nextDouble() * (W_MAX_NUM - W_MIN_NUM + 1)) + W_MIN_NUM;
            }
            arr[i] = ranNum;
            System.out.println(String.format("Unsorted: %02d", arr[i]));
        }
    }

    /**
     * Translation of the COBOL {@code SORTING-ARRAY SECTION} from
     * {@code BubbleSort.cbl} lines 71-85. Performs classic bubble sort over
     * {@link #arr} with the COBOL {@code W-TEMP}-based three-step swap, then
     * displays each element on its own line with the {@code "Sorted: "}
     * prefix.
     *
     * <p>The COBOL inner loop bound {@code UNTIL W-K >= W-LEN-ARR} is strict
     * &mdash; {@code W-K} iterates from 1 to {@code W-LEN-ARR - 1} inclusive
     * (i.e., 1..9), so the maximum index accessed via {@code W-ARR(W-K + 1)}
     * is {@code W-LEN-ARR} (i.e., 10). Translated to 0-based Java the inner
     * bound is {@code k < W_LEN_ARR - 1}, keeping the maximum index accessed
     * via {@code arr[k + 1]} at {@code W_LEN_ARR - 1}.
     */
    private void sortingArray() {
        // COBOL: PERFORM VARYING W-J FROM 1 BY 1 UNTIL W-J > W-LEN-ARR
        //          PERFORM VARYING W-K FROM 1 BY 1 UNTIL W-K >= W-LEN-ARR   <-- inner W-K runs 1..9
        //            IF (W-ARR(W-K) > W-ARR(W-K + 1))
        //              MOVE W-ARR(W-K) TO W-TEMP                            <-- 3-step swap via W-TEMP
        //              MOVE W-ARR(W-K + 1) TO W-ARR(W-K)
        //              MOVE W-TEMP TO W-ARR(W-K + 1)
        //            END-IF
        //          END-PERFORM
        //        END-PERFORM                                                (BubbleSort.cbl L73-81)
        for (int j = 0; j < W_LEN_ARR; j++) {
            for (int k = 0; k < W_LEN_ARR - 1; k++) {
                if (arr[k] > arr[k + 1]) {
                    int temp = arr[k];
                    arr[k] = arr[k + 1];
                    arr[k + 1] = temp;
                }
            }
        }

        // COBOL: PERFORM VARYING W-H FROM 1 BY 1 UNTIL W-H > W-LEN-ARR
        //          DISPLAY 'Sorted: ' W-ARR(W-H)                            <-- PIC 99 -> zero-padded 2-digit
        //        END-PERFORM.                                               (BubbleSort.cbl L83-85)
        for (int h = 0; h < W_LEN_ARR; h++) {
            System.out.println(String.format("Sorted: %02d", arr[h]));
        }
    }
}
