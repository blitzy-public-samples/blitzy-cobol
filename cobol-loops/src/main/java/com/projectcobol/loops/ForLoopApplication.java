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

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Java 21 / Spring Boot 3.x translation of {@code OpenCobol/Loops/ForLoop.cbl}.
 *
 * <p>The original COBOL program {@code FOR-LOOP} demonstrates the contrast
 * between {@code PERFORM UNTIL} and {@code PERFORM VARYING} loop idioms. It
 * declares three module-scoped {@code PIC 999 VALUE 0} working-storage
 * counters ({@code W-I}, {@code W-J}, {@code W-K}) shared between two
 * pedagogical sections:
 * <ul>
 *   <li>{@code FOR-UNTIL-LOOP SECTION} &mdash; a nested
 *       {@code PERFORM UNTIL W-I &gt; 20} loop with explicit counter
 *       bookkeeping ({@code MOVE W-I TO W-J}, {@code ADD 1 TO W-I},
 *       {@code ADD 1 TO W-J}). The outer iterations run for
 *       {@code W-I = 0..20} (21 outer iterations); each outer iteration runs
 *       its inner loop for {@code W-J} values starting at the (pre-increment)
 *       value of {@code W-I} up through 20 inclusive. The inner loop emits
 *       {@code "UNTIL: nnn W-K: nnn = nnn * nnn"} for each iteration, where
 *       the displayed {@code W-I} is the post-increment value. The section
 *       emits {@code 21 + 20 + 19 + ... + 1 = 231} lines.</li>
 *   <li>{@code FOR-VAIRING-LOOP SECTION} &mdash; note the deliberate
 *       misspelling {@code VAIRING} (instead of {@code VARYING}) in the COBOL
 *       section name. This Java translation preserves the misspelling verbatim
 *       as method name {@link #forVairingLoop()} per the AAP &sect;0.7.3
 *       source-spelling preservation rule. The section uses
 *       {@code PERFORM VARYING X FROM 1 BY 1 UNTIL X &gt; 20} for both outer
 *       and inner loops, but the body also explicitly increments the loop
 *       variable via {@code ADD 1 TO X}. Combined with the {@code BY 1}
 *       auto-increment of {@code PERFORM VARYING}, this makes each counter
 *       advance by 2 per iteration. Both outer and inner loops therefore run
 *       for 10 iterations each, producing {@code 10 * 10 = 100} lines of
 *       {@code "VARYING: nnn W-K: nnn = nnn * nnn"} output.</li>
 * </ul>
 *
 * <p>The total stdout footprint is {@code 231 + 100 = 331} lines. The
 * deterministic loop bounds make this program fully suitable for byte-for-byte
 * golden-output testing.
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the
 * source filename ({@code ForLoop.cbl} &rarr; {@code ForLoopApplication}). The
 * COBOL {@code PROGRAM-ID. FOR-LOOP.} happens to align with the filename here,
 * but the rule applies uniformly across the codebase.
 *
 * <p>This is the default {@code mainClass} of the {@code cobol-loops} Maven
 * sub-module (pinned in {@code cobol-loops/pom.xml} because
 * {@code ForLoopApplication} sorts alphabetically first among the two loop
 * entry classes). To run the sibling {@link WhileApplication} from the shared
 * {@code cobol-loops-1.0.0.jar}, pass
 * {@code -Dloader.main=com.projectcobol.loops.WhileApplication} on the
 * {@code java -jar} command line:
 * <pre>
 *     java -Dloader.main=com.projectcobol.loops.WhileApplication \
 *          -jar cobol-loops/target/cobol-loops-1.0.0.jar
 * </pre>
 *
 * <p>Because this sub-module hosts two {@code @SpringBootApplication} entry
 * classes in the same package, the default component scan is disabled via
 * {@code @ComponentScan(useDefaultFilters = false)} so that booting this class
 * registers and runs ONLY itself; see the implementation comment on the class
 * declaration for the full rationale. This guarantees the byte-for-byte
 * {@code 331}-line golden output without the sibling
 * {@link WhileApplication}'s countdown bleeding into stdout.
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 W-I PIC 999 VALUE 0} &rarr; {@code private int wI = 0}
 *       (instance field; preserves COBOL WORKING-STORAGE module scope so both
 *       private methods share state)</li>
 *   <li>{@code PERFORM UNTIL <cond>} &rarr; {@code while (!<cond>)} (TEST
 *       BEFORE semantics)</li>
 *   <li>{@code PERFORM VARYING X FROM A BY B UNTIL C} &rarr;
 *       {@code for (X = A; !C; X += B)} (TEST BEFORE, post-step
 *       increment)</li>
 *   <li>{@code DISPLAY "lit " V " lit " V} &rarr;
 *       {@code System.out.println(String.format("lit %03d lit %03d", v, v))}
 *       ({@code %03d} mirrors {@code PIC 999} zero-padded 3-digit
 *       display)</li>
 *   <li>{@code ADD 0 TO X} &rarr; documented as a no-op via inline comment; no
 *       Java statement emitted (a literal {@code wI += 0;} would compile under
 *       {@code -Xlint:all} but the explicit no-op is omitted as semantically
 *       irrelevant)</li>
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
public class ForLoopApplication implements CommandLineRunner {

    // COBOL: 01 W-I PIC 999 VALUE 0.  (ForLoop.cbl L12)
    private int wI = 0;

    // COBOL: 01 W-J PIC 999 VALUE 0.  (ForLoop.cbl L13)
    private int wJ = 0;

    // COBOL: 01 W-K PIC 999 VALUE 0.  (ForLoop.cbl L14)
    private int wK = 0;

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
        SpringApplication.run(ForLoopApplication.class, args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code ForLoop.cbl} lines 17-21:
     * <pre>
     *     PERFORM FOR-UNTIL-LOOP.
     *     PERFORM FOR-VAIRING-LOOP.
     *     GOBACK.
     * </pre>
     * The COBOL {@code GOBACK} maps to the implicit Java method return after
     * the two private method calls.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract; this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        forUntilLoop();
        forVairingLoop();
    }

    /**
     * Translation of the COBOL {@code FOR-UNTIL-LOOP SECTION} from
     * {@code ForLoop.cbl} lines 23-35. Demonstrates classic
     * {@code PERFORM UNTIL} nested loops with explicit counter bookkeeping.
     *
     * <p>The outer loop runs while {@code wI &le; 20}; the inner loop runs
     * while {@code wJ &le; 20}. Both loops increment their counter manually via
     * {@code ADD 1} statements (there is no {@code PERFORM VARYING}
     * auto-increment here). The outer loop body re-initializes {@code wJ} to
     * the pre-increment {@code wI} value, then increments {@code wI}, then runs
     * the inner loop from the new {@code wJ} up through 20 inclusive.
     *
     * <p>The total output is {@code 21 + 20 + 19 + ... + 1 = 231} lines of
     * {@code "UNTIL: nnn W-K: nnn = nnn * nnn"}.
     */
    private void forUntilLoop() {
        // COBOL: PERFORM UNTIL W-I > 20
        //          MOVE W-I TO W-J
        //          ADD 1 TO W-I
        //          PERFORM UNTIL W-J > 20
        //            COMPUTE W-K = W-J * W-I
        //            DISPLAY "UNTIL: " W-I  " W-K: " W-K " = " W-J " * " W-I
        //            ADD 1 TO W-J
        //          END-PERFORM
        //        END-PERFORM.                                    (ForLoop.cbl L25-35)
        //
        // PERFORM UNTIL is TEST BEFORE: the condition is evaluated first; if it
        // is already true the body is skipped. The body therefore runs while
        // !(wI > 20), preserved literally below rather than simplified to
        // (wI <= 20) so the COBOL UNTIL condition stays visually traceable.
        while (!(wI > 20)) {
            // COBOL: MOVE W-I TO W-J
            wJ = wI;
            // COBOL: ADD 1 TO W-I
            wI = wI + 1;

            // COBOL: PERFORM UNTIL W-J > 20
            while (!(wJ > 20)) {
                // COBOL: COMPUTE W-K = W-J * W-I
                wK = wJ * wI;
                // COBOL: DISPLAY "UNTIL: " W-I  " W-K: " W-K " = " W-J " * " W-I
                // The double space between W-I and " W-K: " in the COBOL source
                // is inter-token source whitespace, not output whitespace; the
                // emitted line is single-spaced. PIC 999 -> %03d zero-padded.
                System.out.println(String.format("UNTIL: %03d W-K: %03d = %03d * %03d", wI, wK, wJ, wI));
                // COBOL: ADD 1 TO W-J
                wJ = wJ + 1;
            }
        }
    }

    /**
     * Translation of the COBOL {@code FOR-VAIRING-LOOP SECTION} from
     * {@code ForLoop.cbl} lines 38-54. <strong>Note the misspelling
     * {@code VAIRING}</strong> (instead of {@code VARYING}) in the COBOL
     * section name; this Java method preserves the misspelling verbatim per the
     * AAP &sect;0.7.3 source-spelling preservation rule.
     *
     * <p>Demonstrates {@code PERFORM VARYING} nested loops, but with a
     * pedagogical twist: each loop body explicitly increments its loop variable
     * via {@code ADD 1}, in addition to the {@code BY 1} auto-increment of
     * {@code PERFORM VARYING}. The net effect is that both counters advance by 2
     * per iteration, producing 10 outer iterations and 10 inner iterations per
     * outer iteration &mdash; a total of {@code 10 * 10 = 100} lines of
     * {@code "VARYING: nnn W-K: nnn = nnn * nnn"}.
     *
     * <p>The three {@code ADD 0 TO W-I/W-J/W-K} statements on
     * {@code ForLoop.cbl} lines 40-42 are no-ops (adding zero has no effect on
     * the operand); they are documented via inline comment but generate no Java
     * statements because the subsequent {@code PERFORM VARYING W-I FROM 1}
     * reinitializes {@code wI} to 1 anyway. The {@code MOVE W-I TO W-J}
     * statement on line 45 is effectively overwritten by the inner
     * {@code PERFORM VARYING W-J FROM 1} on line 48, but is preserved here for
     * behavioral fidelity (it has no observable side effect because no DISPLAY
     * statement reads {@code wJ} between the two assignments).
     *
     * <p>The deliberate misspelling is part of the pedagogical artifact &mdash;
     * COBOL programmers occasionally encountered such typos in historical code
     * bases, and the educational mission of this repository preserves them
     * rather than silently correcting them. Note that the COBOL {@code DISPLAY}
     * string literal on line 50 is the correctly-spelled {@code "VARYING: "};
     * only the section name (and hence this Java method name) carries the
     * {@code VAIRING} misspelling.
     */
    private void forVairingLoop() {
        // COBOL: ADD 0 TO W-I.  (ForLoop.cbl L40 - no-op)
        // COBOL: ADD 0 TO W-J.  (ForLoop.cbl L41 - no-op)
        // COBOL: ADD 0 TO W-K.  (ForLoop.cbl L42 - no-op)
        // These three statements have no observable effect in COBOL. The
        // subsequent PERFORM VARYING W-I FROM 1 reinitializes wI to 1
        // unconditionally. No Java code is emitted for these no-ops (an explicit
        // wI += 0; would be dead code).

        // COBOL: PERFORM VARYING W-I FROM 1 BY 1 UNTIL W-I > 20
        //          MOVE W-I TO W-J
        //          ADD 1 TO W-I
        //          PERFORM VARYING W-J FROM 1 BY 1 UNTIL W-J > 20
        //            COMPUTE W-K = W-J * W-I
        //            DISPLAY "VARYING: " W-I  " W-K: " W-K " = " W-J " * " W-I
        //            ADD 1 TO W-J
        //          END-PERFORM
        //        END-PERFORM.                                    (ForLoop.cbl L44-54)
        //
        // PERFORM VARYING semantics (TEST BEFORE):
        //   1. Initialize var
        //   2. Test condition; if true, exit
        //   3. Execute body
        //   4. var += step
        //   5. Goto 2
        //
        // The body's explicit ADD 1 combines with the BY 1 auto-increment to
        // make each counter advance by 2 per iteration. The for(...) header
        // below encodes only the init + condition + auto-increment; the body's
        // manual increment is a separate statement inside.
        for (wI = 1; !(wI > 20); wI = wI + 1) {
            // COBOL: MOVE W-I TO W-J
            // (This assignment is harmlessly overwritten by the inner
            // PERFORM VARYING W-J FROM 1 below; preserved for fidelity.)
            wJ = wI;
            // COBOL: ADD 1 TO W-I (manual increment in addition to BY 1)
            wI = wI + 1;

            // COBOL: PERFORM VARYING W-J FROM 1 BY 1 UNTIL W-J > 20
            for (wJ = 1; !(wJ > 20); wJ = wJ + 1) {
                // COBOL: COMPUTE W-K = W-J * W-I
                wK = wJ * wI;
                // COBOL: DISPLAY "VARYING: " W-I  " W-K: " W-K " = " W-J " * " W-I
                // The DISPLAY literal is the correctly-spelled "VARYING: " (only
                // the COBOL section name has the VAIRING misspelling). PIC 999
                // -> %03d zero-padded 3-digit display.
                System.out.println(String.format("VARYING: %03d W-K: %03d = %03d * %03d", wI, wK, wJ, wI));
                // COBOL: ADD 1 TO W-J (manual increment in addition to BY 1)
                wJ = wJ + 1;
            }
        }
    }
}
