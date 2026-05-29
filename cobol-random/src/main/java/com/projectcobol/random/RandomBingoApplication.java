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
package com.projectcobol.random;

import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot translation of {@code OpenCobol/Random/RandomBingo.cbl}
 * (COBOL {@code PROGRAM-ID. GAME-LOTTERY}).
 *
 * <p>The source COBOL program is a tip-lottery generator that:
 * <ol>
 *   <li>Prints a four-line welcome banner.</li>
 *   <li>Seeds the PRNG with {@code FUNCTION SECONDS-PAST-MIDNIGHT}.</li>
 *   <li>Fills a 100-slot array with random integers in {@code [1, 100]}.</li>
 *   <li>Drifts the PRNG state via a 200-iteration warm-up loop, recording
 *       the final value as {@code W-RANDOM-TIP}.</li>
 *   <li>Increments {@code W-J} from {@code 1} until {@code W-J > W-RANDOM-TIP},
 *       then reads {@code W-ARR(W-J)} and prints it as the "winning number"
 *       inside a three-line closing banner.</li>
 * </ol>
 *
 * <p>The class name follows the AAP filename-traceability rule: the source
 * filename {@code RandomBingo.cbl} overrides the COBOL {@code PROGRAM-ID}
 * {@code GAME-LOTTERY}, yielding {@code RandomBingoApplication}.
 *
 * <p>Deterministic seeding: the public no-argument constructor used by
 * Spring Boot mirrors the COBOL {@code FUNCTION SECONDS-PAST-MIDNIGHT}
 * seeding via {@code System.currentTimeMillis() % 86400L}. A package-private
 * constructor accepting a fixed {@code long} seed enables byte-exact
 * golden-output testing without exposing the {@link Random} field.
 */
@SpringBootApplication
public class RandomBingoApplication implements CommandLineRunner {

    /** COBOL: {@code 78 W-LEN-ARR VALUE 100.} */
    private static final int W_LEN_ARR = 100;

    /** COBOL: {@code 01 W-ARR PIC 999 OCCURS W-LEN-ARR TIMES.} */
    private final int[] arr = new int[W_LEN_ARR];

    /** PRNG instance replacing COBOL's implicit {@code FUNCTION RANDOM} state. */
    private final Random random;

    /**
     * Production constructor used by Spring Boot. Mirrors the COBOL seeding
     * strategy {@code MOVE FUNCTION RANDOM(FUNCTION SECONDS-PAST-MIDNIGHT) TO SEED}
     * by using the current second-of-day as the seed.
     */
    public RandomBingoApplication() {
        this(System.currentTimeMillis() % 86400L);
    }

    /**
     * Test-friendly constructor with a deterministic seed. Package-private so
     * unit tests in the same package may inject a fixed seed (typically
     * {@code 42L}) for byte-exact golden-output verification.
     *
     * @param seed the value passed to {@link Random#Random(long)}
     */
    RandomBingoApplication(long seed) {
        this.random = new Random(seed);
    }

    /**
     * JVM entry point delegating to {@link SpringApplication#run(Class, String...)}.
     *
     * @param args command-line arguments forwarded to the Spring application context
     */
    public static void main(String[] args) {
        SpringApplication.run(RandomBingoApplication.class, args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry. Mirrors the COBOL
     * {@code MAIN-PROCEDURE}: prints the welcome banner, then performs
     * {@code INIT-SEED}, {@code GENERATE-NUMBERS}, and {@code PRINT-NUMBER}
     * in sequence before {@code GOBACK}.
     *
     * @param args application arguments (unused; preserved to satisfy the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to match {@link CommandLineRunner#run(String...)}
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL lines 46-49: four-line welcome banner (each line exactly 49 characters wide).
        System.out.println("-------------------------------------------------");
        System.out.println("- Welcome in the game tip lottery !             -");
        System.out.println("- You choose one number from 1 to 100!          -");
        System.out.println("-------------------------------------------------");

        initSeed();
        generateNumbers();
        printNumber();
    }

    /**
     * Mirrors the COBOL {@code INIT-SEED SECTION}:
     * <pre>{@code MOVE FUNCTION RANDOM(FUNCTION SECONDS-PAST-MIDNIGHT) TO SEED.}</pre>
     *
     * <p>The COBOL argument-form {@code FUNCTION RANDOM(seed)} both seeds
     * the PRNG and returns the first random value. Java's {@code new Random(seed)}
     * only seeds and produces no value, so one {@code nextDouble()} call is
     * issued and its result discarded to advance the internal state by exactly
     * one increment - keeping subsequent state transitions identical to COBOL.
     */
    private void initSeed() {
        random.nextDouble();
    }

    /**
     * Mirrors the COBOL {@code GENERATE-NUMBERS SECTION}: prints the
     * "Generating numbers" banner, then fills {@link #arr} with 100 random
     * integers in {@code [1, 100]} using the formula
     * {@code (int) (random.nextDouble() * 100) + 1} (COBOL:
     * {@code COMPUTE W-NUM = (FUNCTION RANDOM * 100) + 1}).
     */
    private void generateNumbers() {
        // COBOL lines 61-63: three-line "Generating numbers" banner (49 chars each).
        System.out.println("-------------------------------------------------");
        System.out.println("- Generating numbers .......                    -");
        System.out.println("-------------------------------------------------");

        // COBOL: PERFORM VARYING W-I FROM 1 BY 1 UNTIL W-I > W-LEN-ARR.
        for (int wI = 1; wI <= W_LEN_ARR; wI++) {
            // COBOL: COMPUTE W-NUM = (FUNCTION RANDOM * 100) + 1.
            int wNum = (int) (random.nextDouble() * 100) + 1;
            // COBOL: MOVE W-NUM TO W-ARR(W-I).  [COBOL 1-based -> Java 0-based]
            arr[wI - 1] = wNum;
        }
    }

    /**
     * Mirrors the COBOL {@code PRINT-NUMBER SECTION}.
     *
     * <p>Step 1: A 200-iteration warm-up loop drifts the PRNG state and stores
     * the final value in {@code wRandomTip}. The COBOL comment reads "Prevent
     * 'shake' numbers", indicating the loop's purpose is to randomize the
     * eventual array index beyond the trivially low-entropy first few PRNG
     * outputs.
     *
     * <p>Step 2: A J-incrementing loop runs until {@code wJ > wRandomTip}
     * (COBOL: {@code PERFORM UNTIL W-J > W-RANDOM-TIP}). Because COBOL
     * {@code PERFORM UNTIL} uses {@code WITH TEST BEFORE} semantics, the
     * counter terminates one past the threshold, leaving
     * {@code wJ = wRandomTip + 1}. The subsequent
     * {@code MOVE W-ARR(W-J) TO W-TIP} therefore reads
     * {@code W-ARR(W-RANDOM-TIP + 1)} in COBOL 1-based indexing, which maps
     * to {@code arr[wRandomTip]} in Java 0-based indexing.
     *
     * <p>Off-by-one preservation: the Java translation reads
     * {@code arr[wJ - 1]} verbatim, faithfully reproducing the COBOL
     * indexing scheme. With seed {@code 42L} this yields {@code arr[39] = 47}
     * and {@code wTip = "047"}.
     *
     * <p>Step 3: Prints the three-line closing "Winning number" banner.
     */
    private void printNumber() {
        // COBOL: PERFORM UNTIL W-K > 200.  W-K initial value is 1, so 200 iterations execute.
        int wRandomTip = 0;
        for (int wK = 1; wK <= 200; wK++) {
            // COBOL: COMPUTE W-RANDOM-TIP = (FUNCTION RANDOM * 100) + 1.
            wRandomTip = (int) (random.nextDouble() * 100) + 1;
            // (COBOL: ADD 1 TO W-K. - handled by the for-loop step.)
        }

        // COBOL: PERFORM UNTIL W-J > W-RANDOM-TIP.  W-J initial value is 1.
        int wJ = 1;
        while (wJ <= wRandomTip) {
            wJ++;
        }

        // COBOL: MOVE W-ARR(W-J) TO W-TIP.  [1-based -> 0-based via (wJ - 1)]
        int wTip = arr[wJ - 1];

        // COBOL lines 84-86: three-line "Winning number" banner.
        // Line 85 has the W-TIP value appended directly (no trailing dash on that line).
        System.out.println("-------------------------------------------------");
        System.out.println("- Winning number is : " + String.format("%03d", wTip));
        System.out.println("-------------------------------------------------");
    }
}
