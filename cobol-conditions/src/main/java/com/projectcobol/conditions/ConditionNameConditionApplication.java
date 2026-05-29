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

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Java 21 / Spring Boot 3.x translation of
 * {@code OpenCobol/Conditions/ConditionNameCondition.cbl}.
 *
 * <p>Demonstrates COBOL {@code 88-level} <strong>condition-name</strong> declarations
 * with {@code VALUES ARE n THRU m} ranges. The COBOL working storage declares a single
 * unsigned 3-digit numeric and attaches two condition names to it:
 * <pre>
 *   01 M_NUMBER PIC 9(3).
 *       88 M_TRUE  VALUES ARE 30 THRU 100.
 *       88 M_FALSE VALUES ARE 000 THRU 40.
 * </pre>
 * A COBOL {@code 88-level} item is a named boolean predicate over the value of its parent
 * data item: {@code M_TRUE} is "true" exactly when {@code M_NUMBER} lies in the inclusive
 * range {@code [30, 100]}, and {@code M_FALSE} is "true" exactly when {@code M_NUMBER} lies
 * in the inclusive range {@code [0, 40]}. Per the AAP, these are translated into two private
 * predicate methods, {@link #mTrue()} and {@link #mFalse()}, that test the current value of
 * the {@code mNumber} field against the range.
 *
 * <p>The {@code MAIN-PROCEDURE} assigns {@code MOVE 50 TO M_NUMBER} and then guards two
 * {@code DISPLAY} statements:
 * <pre>
 *   MOVE 50 TO M_NUMBER.
 *   IF M_TRUE  DISPLAY 'Passed with ' M_NUMBER ' marks'.
 *   IF M_FALSE DISPLAY 'FAILED with ' M_NUMBER ' marks'.
 *   GOBACK.
 * </pre>
 * With {@code M_NUMBER = 50}, only {@code M_TRUE} (30..100) matches; {@code M_FALSE} (0..40)
 * does not, so the program emits exactly one line, matching the golden fixture
 * {@code expected/ConditionNameConditionApplication.txt} byte-for-byte:
 * <pre>
 *     Passed with 050 marks
 * </pre>
 *
 * <p><strong>PIC 9(3) display semantics:</strong> COBOL {@code PIC 9(3)} is an unsigned
 * 3-digit numeric whose {@code DISPLAY} representation is right-aligned and zero-padded to
 * width 3. The value {@code 50} therefore renders as {@code "050"} (not {@code "50"} and not
 * {@code " 50"}). This is reproduced exactly by {@link #formatPic9(int)} via
 * {@code String.format("%03d", value)}.
 *
 * <p><strong>Minimal-change / preserve-behavior note (AAP &sect;0.7.2):</strong> the second
 * {@code IF M_FALSE} guard and its {@code "FAILED with ..."} {@code DISPLAY} are a dead branch
 * for the hardcoded {@code MOVE 50} (50 is not in {@code [0, 40]}), yet the guard is retained
 * verbatim in the Java {@link #run(String...)} body so the translation mirrors the original
 * COBOL control flow exactly. The dead branch is deliberately <em>not</em> optimized away.
 *
 * <p>Per the AAP filename-traceability rule (AAP &sect;0.7.3), the Java class name follows the
 * source <em>filename</em> ({@code ConditionNameCondition.cbl} &rarr;
 * {@code ConditionNameConditionApplication}), <strong>not</strong> the COBOL
 * {@code PROGRAM-ID}. The source {@code PROGRAM-ID} is mislabeled {@code CONDITION-STATEMENT}
 * (the same identifier used by the sibling {@code ConditionStatement.cbl}); this appears to be
 * an authoring error in the original COBOL, and the authoritative filesystem name takes
 * precedence over the {@code PROGRAM-ID}.
 *
 * <p>This entry class is one of eight {@code @SpringBootApplication} classes that share the
 * package {@code com.projectcobol.conditions} and the single executable
 * {@code cobol-conditions-1.0.0.jar}. It can be selected at run time from that shared jar via
 * the Spring Boot {@code PropertiesLauncher}:
 * <pre>
 *     java -Dloader.main=com.projectcobol.conditions.ConditionNameConditionApplication \
 *          -jar cobol-conditions/target/cobol-conditions-1.0.0.jar
 * </pre>
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 M_NUMBER PIC 9(3)} (no {@code VALUE} clause, default 0, mutated by
 *       {@code MOVE}) &rarr; {@code private int mNumber} (a mutable instance field, hence
 *       <em>not</em> {@code static final}).</li>
 *   <li>{@code 88 M_TRUE VALUES ARE 30 THRU 100} &rarr; {@link #mTrue()} returning
 *       {@code mNumber >= 30 && mNumber <= 100} (COBOL {@code THRU} is inclusive on both
 *       ends).</li>
 *   <li>{@code 88 M_FALSE VALUES ARE 000 THRU 40} &rarr; {@link #mFalse()} returning
 *       {@code mNumber >= 0 && mNumber <= 40} (COBOL literal {@code 000} is the integer
 *       {@code 0}).</li>
 *   <li>{@code MOVE 50 TO M_NUMBER} &rarr; {@code mNumber = 50}.</li>
 *   <li>{@code IF M_TRUE ... } / {@code IF M_FALSE ...} &rarr; Java {@code if (mTrue())} /
 *       {@code if (mFalse())}.</li>
 *   <li>{@code DISPLAY '...' M_NUMBER '...'} &rarr; {@code System.out.println("..." +
 *       formatPic9(mNumber) + "...")} with PIC 9(3) zero-padding.</li>
 *   <li>{@code GOBACK} &rarr; the implicit return at the end of {@link #run(String...)}.</li>
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
// established convention used by the other multi-application sub-modules
// (cobol-loops, cobol-memory, cobol-random, cobol-sort) and the sibling Conditions
// entry classes.
@SpringBootApplication
@ComponentScan(useDefaultFilters = false)
public class ConditionNameConditionApplication implements CommandLineRunner {

    // COBOL: 01 M_NUMBER PIC 9(3).  (ConditionNameCondition.cbl L15)
    // PIC 9(3) is an unsigned 3-digit numeric declared WITHOUT a VALUE clause, so it
    // defaults to 0 and is later reassigned by MOVE 50 TO M_NUMBER in the PROCEDURE
    // DIVISION. Because the value is mutated at run time, this maps to a mutable
    // instance field (NOT a static final constant).
    private int mNumber;

    /**
     * Spring Boot bootstrap entry point. Delegates to
     * {@link SpringApplication#run(Class, String...)}, which initializes the
     * {@code ApplicationContext} and then invokes this class's {@link #run(String...)}
     * callback (the {@link CommandLineRunner} contract). This wires the COBOL
     * {@code PROCEDURE DIVISION} / {@code MAIN-PROCEDURE} / {@code GOBACK} lifecycle to a
     * Spring Boot batch-style entry class.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by this
     *             translation, but required by the Spring Boot contract)
     */
    public static void main(String[] args) {
        SpringApplication.run(ConditionNameConditionApplication.class, args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code ConditionNameCondition.cbl} lines 20-29:
     * <pre>
     *     MOVE 50 TO M_NUMBER.
     *
     *     IF M_TRUE
     *     DISPLAY 'Passed with ' M_NUMBER ' marks'.
     *
     *     IF M_FALSE
     *     DISPLAY 'FAILED with ' M_NUMBER ' marks'.
     *
     *     GOBACK.
     * </pre>
     * With {@code mNumber = 50}: {@link #mTrue()} is {@code true} (50 is in {@code [30, 100]})
     * so the first {@code DISPLAY} fires, printing {@code "Passed with 050 marks"};
     * {@link #mFalse()} is {@code false} (50 is not in {@code [0, 40]}) so the second
     * {@code DISPLAY} is skipped. The COBOL {@code GOBACK} maps to the implicit Java method
     * return.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner} contract;
     *                   this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: MOVE 50 TO M_NUMBER.
        mNumber = 50;

        // COBOL: IF M_TRUE DISPLAY 'Passed with ' M_NUMBER ' marks'.
        // FIRES for mNumber == 50 because 50 is within the inclusive range [30, 100].
        // M_NUMBER is rendered with PIC 9(3) zero-padding, so 50 prints as "050".
        if (mTrue()) {
            System.out.println("Passed with " + formatPic9(mNumber) + " marks");
        }

        // COBOL: IF M_FALSE DISPLAY 'FAILED with ' M_NUMBER ' marks'.
        // Dead branch for mNumber == 50 (50 is outside the inclusive range [0, 40]), but
        // retained verbatim per the minimal-change clause to mirror the COBOL control flow.
        if (mFalse()) {
            System.out.println("FAILED with " + formatPic9(mNumber) + " marks");
        }
    }

    /**
     * COBOL {@code 88 M_TRUE VALUES ARE 30 THRU 100}.
     *
     * <p>A COBOL {@code THRU} range is inclusive on both ends, so the condition is satisfied
     * for any {@code mNumber} in {@code [30, 100]}.
     *
     * @return {@code true} iff {@code mNumber} is in the inclusive range {@code [30, 100]}
     */
    private boolean mTrue() {
        return mNumber >= 30 && mNumber <= 100;
    }

    /**
     * COBOL {@code 88 M_FALSE VALUES ARE 000 THRU 40}.
     *
     * <p>A COBOL {@code THRU} range is inclusive on both ends, and the COBOL literal
     * {@code 000} is simply the integer {@code 0}, so the condition is satisfied for any
     * {@code mNumber} in {@code [0, 40]}.
     *
     * @return {@code true} iff {@code mNumber} is in the inclusive range {@code [0, 40]}
     */
    private boolean mFalse() {
        return mNumber >= 0 && mNumber <= 40;
    }

    /**
     * Renders an integer using COBOL {@code PIC 9(3)} display format: an unsigned numeric
     * right-aligned and zero-padded to a fixed width of three digits. For example, {@code 50}
     * becomes {@code "050"} and {@code 7} becomes {@code "007"}. This reproduces the exact
     * byte sequence COBOL emits when a {@code PIC 9(3)} item is named in a {@code DISPLAY}
     * statement.
     *
     * @param value the numeric value to render (expected to be in the {@code [0, 999]} domain
     *              of a {@code PIC 9(3)} item)
     * @return the value formatted as a zero-padded width-3 string
     */
    private String formatPic9(int value) {
        return String.format("%03d", value);
    }
}
