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

package com.projectcobol.conditions;

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Java 21 / Spring Boot 3.x translation of
 * {@code OpenCobol/Conditions/EvaluteVerb.cbl}
 * (PROGRAM-ID: {@code EVALUATE-VERB}).
 *
 * <p><strong>Note on naming (filename traceability):</strong> the source filename
 * is {@code EvaluteVerb.cbl} &mdash; a typo, "Evalute" with the second "a" of
 * "Evaluate" missing. The COBOL {@code PROGRAM-ID} itself is spelled correctly
 * ({@code EVALUATE-VERB}), but per the AAP filename-traceability rule
 * (&sect;0.7.3) the Java class name follows the <em>filename</em>, not the
 * {@code PROGRAM-ID}. This class is therefore deliberately named
 * {@code EvaluteVerbApplication} (not {@code EvaluateVerbApplication}); the
 * misspelling is preserved verbatim and must not be silently "corrected".
 *
 * <p>Demonstrates COBOL's {@code EVALUATE TRUE WHEN ... WHEN OTHER} first-match-wins
 * switching construct. The COBOL working storage declares a single unsigned
 * 3-digit numeric ({@code 01 NUM01 PIC 9(3) VALUE ZERO}) which is set to 3 by
 * {@code MOVE 3 TO NUM01} before the {@code EVALUATE}. With {@code NUM01 = 3} the
 * first WHEN predicate ({@code NUM01 &gt; 2}) is TRUE, so the EVALUATE fires its
 * first branch and prints exactly one line that matches the golden fixture
 * {@code expected/EvaluteVerbApplication.txt} byte-for-byte:
 * <pre>
 *     NUMBER01 GREATER THAN 2
 * </pre>
 * The second WHEN ({@code NUM01 &lt; 0}) and the {@code WHEN OTHER} default are
 * never evaluated, because {@code EVALUATE TRUE} stops at the first matching
 * predicate.
 *
 * <p><strong>Translation strategy &mdash; EVALUATE TRUE &rarr;
 * {@code if / else if / else} cascade (NOT a {@code switch}):</strong> COBOL's
 * {@code EVALUATE TRUE WHEN <pred1> ... WHEN <pred2> ... WHEN OTHER} evaluates a
 * sequence of <em>heterogeneous boolean predicates</em> top-to-bottom and executes
 * the first whose predicate is TRUE; subsequent WHEN clauses are skipped (there is
 * no C-style fall-through). The faithful Java equivalent is a sequential
 * {@code if / else if / else} cascade, whose semantics are identical
 * (first-match-wins, no fall-through, {@code WHEN OTHER} &rarr; the trailing
 * {@code else}). A Java {@code switch} expression would be <em>semantically
 * incorrect</em> here, because {@code switch} discriminates on a single value
 * whereas {@code EVALUATE TRUE} discriminates on independent boolean conditions.
 *
 * <p><strong>Minimal-change / preserve-behavior note (AAP &sect;0.7.2):</strong> all
 * three branches of the COBOL {@code EVALUATE} are retained in the Java
 * {@code run(...)} body, including the two branches ({@code "NUMBER01 LESS THAN 0"}
 * and {@code "INVALID VALUE OF NUMBER01"}) that never fire for the hardcoded
 * {@code MOVE 3 TO NUM01}. The dead branches faithfully mirror the original COBOL
 * control-flow structure and are deliberately <em>not</em> optimized away.
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the source
 * filename ({@code EvaluteVerb.cbl} &rarr; {@code EvaluteVerbApplication}), not the
 * COBOL {@code PROGRAM-ID} ({@code EVALUATE-VERB}).
 *
 * <p>This entry class is one of eight {@code @SpringBootApplication} classes that
 * share the package {@code com.projectcobol.conditions} and the single executable
 * {@code cobol-conditions-1.0.0.jar}. It can be selected at run time from that
 * shared jar via the Spring Boot {@code PropertiesLauncher}:
 * <pre>
 *     java -Dloader.main=com.projectcobol.conditions.EvaluteVerbApplication \
 *          -jar cobol-conditions/target/cobol-conditions-1.0.0.jar
 * </pre>
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 NUM01 PIC 9(3) VALUE ZERO} &rarr; {@code private int num01 = 0}
 *       (unsigned 3-digit numeric; the {@code ZERO} figurative constant maps to
 *       {@code 0}; modeled as a <em>mutable instance field</em> because the
 *       PROCEDURE DIVISION reassigns it &mdash; not a {@code static final}
 *       constant).</li>
 *   <li>{@code MOVE 3 TO NUM01} &rarr; {@code num01 = 3;}.</li>
 *   <li>{@code EVALUATE TRUE} &rarr; an {@code if / else if / else} cascade.</li>
 *   <li>{@code WHEN NUM01 > 2} &rarr; {@code if (num01 > 2)}.</li>
 *   <li>{@code WHEN NUM01 < 0} &rarr; {@code else if (num01 < 0)}.</li>
 *   <li>{@code WHEN OTHER} &rarr; the trailing {@code else}.</li>
 *   <li>{@code DISPLAY '...'} &rarr; {@code System.out.println("...")}.</li>
 *   <li>{@code GOBACK} &rarr; the implicit return at the end of
 *       {@link #run(String...)}.</li>
 * </ul>
 */
// Multi-application isolation: cobol-conditions hosts eight @SpringBootApplication
// entry classes in the same package com.projectcobol.conditions (one per COBOL
// program in OpenCobol/Conditions/), all sharing a single fat-jar. A bare
// @SpringBootApplication enables a default @ComponentScan that would discover the
// other seven sibling applications (each is itself a @Configuration via
// @SpringBootApplication and a CommandLineRunner) and run ALL of their run(...)
// methods in the same JVM, polluting stdout and breaking the byte-for-byte
// golden-output contract. Disabling the default component-scan filters with
// @ComponentScan(useDefaultFilters = false) means booting this class registers and
// runs ONLY itself (the primary source handed to SpringApplication.run), so exactly
// one COBOL Conditions translation executes per invocation. This mirrors the
// established convention used by the sibling Conditions entry classes and the other
// multi-application sub-modules (cobol-loops, cobol-memory, cobol-random, cobol-sort).
@SpringBootApplication
@ComponentScan(useDefaultFilters = false)
public class EvaluteVerbApplication implements CommandLineRunner {

    /**
     * COBOL field {@code 01 NUM01 PIC 9(3) VALUE ZERO} (EvaluteVerb.cbl L12).
     *
     * <p>An unsigned 3-digit numeric initialized to {@code 0} via the {@code ZERO}
     * figurative constant, then reassigned to {@code 3} by {@code MOVE 3 TO NUM01}
     * in the PROCEDURE DIVISION. Because the value is mutated at run time it is a
     * mutable instance field, not a {@code static final} constant.
     */
    private int num01 = 0;

    /**
     * Spring Boot bootstrap entry point. Delegates to
     * {@link SpringApplication#run(Class, String...)}, which initializes the
     * {@code ApplicationContext} and then invokes this class's
     * {@link #run(String...)} callback (the {@link CommandLineRunner} contract).
     * This wires the COBOL {@code PROCEDURE DIVISION} / {@code MAIN-PROCEDURE} /
     * {@code GOBACK} lifecycle to a Spring Boot batch-style entry class.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by this
     *             translation, but required by the Spring Boot contract)
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EvaluteVerbApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code EvaluteVerb.cbl} lines 15-28:
     * <pre>
     *     MOVE 3 TO NUM01.
     *     EVALUATE TRUE
     *       WHEN NUM01 &gt; 2
     *         DISPLAY "NUMBER01 GREATER THAN 2"
     *       WHEN NUM01 &lt; 0
     *         DISPLAY "NUMBER01 LESS THAN 0"
     *       WHEN OTHER
     *         DISPLAY "INVALID VALUE OF NUMBER01"
     *     END-EVALUATE.
     *     GOBACK.
     * </pre>
     * With {@code num01 == 3}, the first predicate ({@code num01 > 2}) is TRUE, so
     * only {@code "NUMBER01 GREATER THAN 2"} is printed; the remaining branches are
     * skipped (first-match-wins). The COBOL {@code GOBACK} maps to the implicit Java
     * method return.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner} contract;
     *                   this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: MOVE 3 TO NUM01.  (EvaluteVerb.cbl L17)
        num01 = 3;

        // COBOL: EVALUATE TRUE / WHEN ... / WHEN OTHER / END-EVALUATE.
        //        (EvaluteVerb.cbl L19-26)
        // EVALUATE TRUE is first-match-wins over heterogeneous boolean predicates,
        // which maps to a Java if / else if / else cascade (NOT a switch). The
        // branches are evaluated top-to-bottom and the first TRUE predicate wins;
        // there is no fall-through, exactly mirroring COBOL EVALUATE WHEN semantics.
        if (num01 > 2) {
            // COBOL: WHEN NUM01 > 2 -> DISPLAY "NUMBER01 GREATER THAN 2".
            // With NUM01 = 3 this predicate is TRUE, so this branch fires and is the
            // sole line of output (matches expected/EvaluteVerbApplication.txt).
            System.out.println("NUMBER01 GREATER THAN 2");
        } else if (num01 < 0) {
            // COBOL: WHEN NUM01 < 0 -> DISPLAY "NUMBER01 LESS THAN 0".
            // Dead branch for NUM01 = 3; retained verbatim per the minimal-change clause.
            System.out.println("NUMBER01 LESS THAN 0");
        } else {
            // COBOL: WHEN OTHER -> DISPLAY "INVALID VALUE OF NUMBER01".
            // Dead branch for NUM01 = 3; retained verbatim per the minimal-change clause.
            System.out.println("INVALID VALUE OF NUMBER01");
        }
    }
}
