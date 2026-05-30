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

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot translation of {@code OpenCobol/Random/RandomNumbers.cbl}
 * (COBOL {@code PROGRAM-ID. RANDOM-NUMBERS}).
 *
 * <p>The source COBOL program is a minimal demonstration of pseudo-random
 * integer generation. It seeds the PRNG with
 * {@code FUNCTION SECONDS-PAST-MIDNIGHT} and then prints ten random integers
 * in {@code [1, 100]}, each labeled {@code "Random number: NNN"} with a
 * three-digit zero-padded value (COBOL {@code PIC 9(3)}).
 *
 * <p>Deterministic seeding: the public no-argument constructor used by
 * Spring Boot mirrors the COBOL {@code FUNCTION SECONDS-PAST-MIDNIGHT}
 * seeding via {@code System.currentTimeMillis() % 86400L}. A package-private
 * constructor accepting a fixed {@code long} seed enables byte-exact
 * golden-output testing without exposing the {@link Random} field.
 */
// One-program-one-class fidelity: each OpenCobol/Random/*.cbl program is
// translated to its own @SpringBootApplication entry class, and both random
// classes (RandomBingoApplication, RandomNumbersApplication) share the package
// com.projectcobol.random (per the AAP package rule). @SpringBootApplication's
// default @ComponentScan would otherwise discover the sibling random application
// (a @Configuration + CommandLineRunner) and run it together in the same JVM,
// corrupting this program's stdout with the sibling's output. Disabling the
// default component-scan filters means this application registers ONLY itself
// (the primary source handed to SpringApplication.run), so exactly one COBOL
// random translation executes per invocation.
@SpringBootApplication
@ComponentScan(useDefaultFilters = false)
public class RandomNumbersApplication implements CommandLineRunner {

    /** PRNG instance replacing COBOL's implicit {@code FUNCTION RANDOM} state. */
    private final Random random;

    /**
     * Production constructor used by Spring Boot. Mirrors the COBOL seeding
     * strategy {@code MOVE FUNCTION RANDOM(FUNCTION SECONDS-PAST-MIDNIGHT) TO SEED}
     * by using the current second-of-day as the seed.
     */
    public RandomNumbersApplication() {
        this(System.currentTimeMillis() % 86400L);
    }

    /**
     * Test-friendly constructor with a deterministic seed. Package-private so
     * unit tests in the same package may inject a fixed seed (typically
     * {@code 42L}) for byte-exact golden-output verification.
     *
     * @param seed the value passed to {@link Random#Random(long)}
     */
    RandomNumbersApplication(long seed) {
        this.random = new Random(seed);
    }

    /**
     * JVM entry point. Builds a {@link SpringApplication} with the Spring Boot
     * banner and startup/profile logging disabled, then runs it. Disabling
     * {@link Banner.Mode#OFF the banner} and
     * {@link SpringApplication#setLogStartupInfo(boolean) startup info} suppresses
     * the ASCII-art banner and the {@code Starting…}/{@code No active profile set}/
     * {@code Started…} INFO lines, so the executable jar emits ONLY the
     * COBOL-translated stdout. This keeps the documented {@code java -jar} run
     * golden-output-clean for this educational sample while preserving the
     * {@link CommandLineRunner} lifecycle.
     *
     * @param args command-line arguments forwarded to the Spring application context
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RandomNumbersApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry. Mirrors the COBOL
     * {@code MAIN-PROCEDURE}: performs {@code GET-SEED} and then
     * {@code GENERATE-NUMBER} before {@code GOBACK}.
     *
     * @param args application arguments (unused; preserved to satisfy the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to match {@link CommandLineRunner#run(String...)}
     */
    @Override
    public void run(String... args) throws Exception {
        getSeed();
        generateNumber();
    }

    /**
     * Mirrors the COBOL {@code GET-SEED SECTION}:
     * <pre>{@code MOVE FUNCTION RANDOM(FUNCTION SECONDS-PAST-MIDNIGHT) TO SEED.}</pre>
     *
     * <p>The COBOL argument-form {@code FUNCTION RANDOM(seed)} both seeds
     * the PRNG and returns the first random value. Java's {@code new Random(seed)}
     * only seeds and produces no value, so one {@code nextDouble()} call is
     * issued and its result discarded to advance the internal state by exactly
     * one increment - keeping subsequent state transitions identical to COBOL.
     */
    private void getSeed() {
        random.nextDouble();
    }

    /**
     * Mirrors the COBOL {@code GENERATE-NUMBER SECTION}:
     * <pre>{@code PERFORM 10 TIMES
     *     COMPUTE W-RESULT = (FUNCTION RANDOM * 100) + 1
     *     DISPLAY "Random number: " W-RESULT
     * END-PERFORM.}</pre>
     *
     * <p>Prints ten lines of the form {@code "Random number: NNN"} where
     * {@code NNN} is a three-digit zero-padded integer in {@code [1, 100]}
     * (COBOL {@code PIC 9(3)} display semantics, modeled with
     * {@link String#format(String, Object...)} pattern {@code "%03d"}).
     */
    private void generateNumber() {
        // COBOL: PERFORM 10 TIMES.
        for (int i = 0; i < 10; i++) {
            // COBOL: COMPUTE W-RESULT = (FUNCTION RANDOM * 100) + 1.
            int wResult = (int) (random.nextDouble() * 100) + 1;
            // COBOL: DISPLAY "Random number: " W-RESULT.  (PIC 9(3) -> %03d zero-padded.)
            System.out.println("Random number: " + String.format("%03d", wResult));
        }
    }
}
