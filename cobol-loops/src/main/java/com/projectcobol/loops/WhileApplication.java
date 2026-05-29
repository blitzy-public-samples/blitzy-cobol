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

package com.projectcobol.loops;

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/Loops/While.cbl}.
 *
 * <p>The original COBOL program {@code WHILE} demonstrates the
 * {@code PERFORM UNTIL} countdown idiom. A single {@code PIC 99 VALUE 20}
 * working-storage counter ({@code W-I}) is decremented from 20 down to 1,
 * displaying each value as a zero-padded 2-digit number, for a total of
 * 20 lines of stdout.
 *
 * <p>Behavioral trace:
 * <pre>
 *     20
 *     19
 *     18
 *     ...
 *     02
 *     01
 * </pre>
 *
 * <p>The deterministic loop bounds make this program fully suitable for
 * byte-for-byte golden-output testing.
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the
 * source filename ({@code While.cbl} &rarr; {@code WhileApplication}). The
 * COBOL {@code PROGRAM-ID. WHILE.} happens to align with the filename here,
 * but the rule applies uniformly across the codebase.
 *
 * <p>This is the <strong>alternate</strong> entry class of the
 * {@code cobol-loops} Maven sub-module. The default {@code mainClass} is
 * the sibling {@link ForLoopApplication} (pinned in
 * {@code cobol-loops/pom.xml} because it sorts alphabetically first). To
 * run this class explicitly from the shared {@code cobol-loops-1.0.0.jar},
 * pass {@code -Dloader.main=com.projectcobol.loops.WhileApplication} on the
 * {@code java -jar} command line:
 * <pre>
 *     java -Dloader.main=com.projectcobol.loops.WhileApplication \
 *          -jar cobol-loops/target/cobol-loops-1.0.0.jar
 * </pre>
 *
 * <p>Because this sub-module hosts two {@code @SpringBootApplication}
 * entry classes in the same package, the default component scan is disabled
 * via {@code @ComponentScan(useDefaultFilters = false)} so that booting this
 * class registers and runs ONLY itself; see the implementation comment on
 * the class declaration for the full rationale.
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 W-I PIC 99 VALUE 20} &rarr; {@code private int wI = 20}
 *       (instance field; preserves COBOL WORKING-STORAGE module scope)</li>
 *   <li>{@code PERFORM UNTIL W-I &le; 0} &rarr; {@code while (!(wI &le; 0))}
 *       i.e. {@code while (wI &gt; 0)} (TEST BEFORE semantics)</li>
 *   <li>{@code DISPLAY W-I} &rarr;
 *       {@code System.out.println(String.format("%02d", wI))} ({@code %02d}
 *       mirrors {@code PIC 99} zero-padded 2-digit display)</li>
 *   <li>{@code COMPUTE W-I = W-I - 1} &rarr; {@code wI = wI - 1}</li>
 * </ul>
 */
// One-program-one-class fidelity: each OpenCobol/Loops/*.cbl program is
// translated to its own @SpringBootApplication entry class, and both loop
// classes (ForLoopApplication, WhileApplication) share the package
// com.projectcobol.loops (per the AAP package rule). @SpringBootApplication's
// default @ComponentScan would otherwise discover the sibling loop application
// (a @Configuration + CommandLineRunner) and run it together in the same JVM.
// Disabling the default component-scan filters means this application registers
// ONLY itself (the primary source handed to SpringApplication.run), so exactly
// one COBOL loop translation executes per invocation - whether launched as the
// default Start-Class or selected via the PropertiesLauncher -Dloader.main
// system property documented above.
@SpringBootApplication
@ComponentScan(useDefaultFilters = false)
public class WhileApplication implements CommandLineRunner {

    // COBOL: 01 W-I PIC 99 VALUE 20.  (While.cbl L12)
    private int wI = 20;

    /**
     * Spring Boot bootstrap entry point. Builds a {@link SpringApplication} for
     * this class with the Spring Boot banner and startup/profile logging disabled
     * (via {@link Banner.Mode#OFF} and
     * {@link SpringApplication#setLogStartupInfo(boolean)}), then runs it. This
     * suppresses the ASCII-art banner and the {@code Starting…}/
     * {@code No active profile set}/{@code Started…} INFO lines so the executable
     * jar emits ONLY the COBOL-translated stdout, keeping the documented
     * {@code java -jar} run golden-output-clean. The {@link CommandLineRunner}
     * lifecycle (which invokes this class's {@link #run(String...)} method) is
     * preserved unchanged.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by
     *             this translation, but required by the Spring Boot contract)
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(WhileApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code While.cbl} lines 15-18:
     * <pre>
     *     PERFORM WHILE-LOOP.
     *     GOBACK.
     * </pre>
     * The COBOL {@code GOBACK} maps to the implicit Java method return after
     * the {@link #whileLoop()} call.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract; this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        whileLoop();
    }

    /**
     * Translation of the COBOL {@code WHILE-LOOP SECTION} from
     * {@code While.cbl} lines 20-24. Demonstrates the {@code PERFORM UNTIL}
     * countdown idiom.
     *
     * <p>The loop runs while {@code wI &gt; 0} (the negation of the COBOL
     * {@code UNTIL W-I &le; 0} condition, per the TEST BEFORE semantics of
     * {@code PERFORM UNTIL}). Each iteration displays the current value of
     * {@code wI} zero-padded to 2 digits and decrements {@code wI} by 1.
     *
     * <p>The total output is 20 lines counting down from {@code "20"} to
     * {@code "01"}.
     */
    private void whileLoop() {
        // COBOL: PERFORM UNTIL W-I <= 0
        //          DISPLAY W-I
        //          COMPUTE W-I = W-I - 1
        //        END-PERFORM.                                    (While.cbl L21-24)
        //
        // PERFORM UNTIL is TEST BEFORE: condition is evaluated first; if true,
        // the loop exits without executing the body. The body therefore runs
        // while !(wI <= 0), i.e. while (wI > 0).
        while (!(wI <= 0)) {
            // COBOL: DISPLAY W-I
            // PIC 99 -> %02d zero-padded 2-digit display
            System.out.println(String.format("%02d", wI));
            // COBOL: COMPUTE W-I = W-I - 1
            wI = wI - 1;
        }
    }
}
