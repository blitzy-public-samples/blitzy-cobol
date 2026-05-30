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
 * {@code OpenCobol/Conditions/ClassCondition.cbl}
 * (PROGRAM-ID: {@code CLASS-CONDITION}).
 *
 * <p>Demonstrates COBOL <strong>class conditions</strong> ({@code IS ALPHABETIC} /
 * {@code IS NUMERIC}) on numeric and alphanumeric ({@code PIC X}) fields. The three
 * sequential {@code IF} blocks in the COBOL {@code MAIN-PROCEDURE} each invoke
 * {@code DISPLAY} for the matching branch, producing exactly three lines of output
 * that match the golden fixture {@code expected/ClassConditionApplication.txt}
 * byte-for-byte:
 * <pre>
 *     STR01 IS ALPHABETIC
 *     NUM01 IS NUMERIC
 *     STR01 ISNT NUMERIC IS ALPHABETIC
 * </pre>
 *
 * <p><strong>Translation notes (COBOL class-condition semantics, critical):</strong>
 * <ul>
 *   <li>{@code 01 STR01 PIC X(9) VALUE 'ABCDF'} becomes the right-padded String
 *       {@code "ABCDF    "} (5 letters + 4 trailing spaces = 9 characters) to mirror
 *       COBOL {@code PIC X(9)} fixed-width padding semantics. COBOL space-pads
 *       alphanumeric literals shorter than the PICTURE on the right.</li>
 *   <li>COBOL {@code IS ALPHABETIC} accepts letters <em>and</em> spaces, so the
 *       padded {@code "ABCDF    "} satisfies the test and the first {@code DISPLAY}
 *       fires.</li>
 *   <li>COBOL {@code IS NUMERIC} on a signed numeric ({@code PIC S9}) item is
 *       structurally always {@code true} (the sign and digits are part of the
 *       numeric representation regardless of the held value), so the second
 *       {@code DISPLAY} fires unconditionally.</li>
 *   <li>COBOL {@code IS NUMERIC} on the alphanumeric {@code STR01} is {@code false}
 *       because the field contains letters, so the third {@code IF} takes its
 *       {@code ELSE} branch.</li>
 *   <li>The literal {@code "STR01 ISNT NUMERIC IS ALPHABETIC"} preserves the
 *       source's non-standard spelling ("ISNT" without an apostrophe) verbatim per
 *       the AAP's preserve-existing-behavior / minimal-change rule
 *       ({@code ClassCondition.cbl} line 28).</li>
 * </ul>
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the source
 * filename ({@code ClassCondition.cbl} &rarr; {@code ClassConditionApplication}), not
 * the COBOL {@code PROGRAM-ID} ({@code CLASS-CONDITION}).
 *
 * <p>This entry class is one of eight {@code @SpringBootApplication} classes that
 * share the package {@code com.projectcobol.conditions} and the single executable
 * {@code cobol-conditions-1.0.0.jar}. It is the default {@code Start-Class} of that
 * jar (pinned via the {@code start-class} property in {@code cobol-conditions/pom.xml})
 * and can also be selected explicitly at run time through the Spring Boot
 * {@code PropertiesLauncher}:
 * <pre>
 *     java -Dloader.main=com.projectcobol.conditions.ClassConditionApplication \
 *          -jar cobol-conditions/target/cobol-conditions-1.0.0.jar
 * </pre>
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 NUM01 PIC S9(9) VALUE -5000} &rarr;
 *       {@code private static final int NUM01 = -5000} (signed 9-digit numeric with
 *       an immutable {@code VALUE} clause &rarr; {@code static final} constant).</li>
 *   <li>{@code 01 STR01 PIC X(9) VALUE 'ABCDF'} &rarr;
 *       {@code private static final String STR01 = "ABCDF    "} (9-char fixed-width
 *       field, right-space-padded).</li>
 *   <li>{@code IF STR01 IS ALPHABETIC} &rarr; {@code if (isAlphabetic(STR01))}
 *       (letters and/or spaces).</li>
 *   <li>{@code IF NUM01 IS NUMERIC} &rarr; {@code if (isNumericInt(NUM01))}
 *       (always true on a {@code PIC S9} item).</li>
 *   <li>{@code IF STR01 IS NUMERIC ... ELSE ...} &rarr;
 *       {@code if (isNumeric(STR01)) { ... } else { ... }} (all-digits test).</li>
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
public class ClassConditionApplication implements CommandLineRunner {

    // COBOL: 01 NUM01 PIC S9(9) VALUE -5000.  (ClassCondition.cbl L11)
    // The leading "S" in the PICTURE makes this a signed 9-digit numeric; the
    // immutable VALUE clause maps to a compile-time constant, hence static final.
    private static final int NUM01 = -5000;

    // COBOL: 01 STR01 PIC X(9) VALUE 'ABCDF'.  (ClassCondition.cbl L12)
    // A 9-character alphanumeric field initialized to "ABCDF" and right-padded with
    // 4 trailing spaces to fill PIC X(9). The trailing spaces are significant: COBOL
    // IS ALPHABETIC treats space as alphabetic, so the padded value still passes.
    private static final String STR01 = "ABCDF    ";

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
        SpringApplication app = new SpringApplication(ClassConditionApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code ClassCondition.cbl} lines 17-29, which is a
     * sequence of three {@code IF} class-condition tests:
     * <pre>
     *     IF STR01 IS ALPHABETIC THEN DISPLAY "STR01 IS ALPHABETIC" END-IF
     *     IF NUM01 IS NUMERIC    THEN DISPLAY "NUM01 IS NUMERIC"    END-IF
     *     IF STR01 IS NUMERIC    THEN DISPLAY "STR01 IS NUMERIC"
     *                            ELSE DISPLAY "STR01 ISNT NUMERIC IS ALPHABETIC"
     *     END-IF.
     *     GOBACK.
     * </pre>
     * With {@code STR01 = "ABCDF    "} and {@code NUM01 = -5000}, the first two tests
     * fire and the third takes its {@code ELSE} branch, printing exactly three lines:
     * {@code "STR01 IS ALPHABETIC"}, {@code "NUM01 IS NUMERIC"} and
     * {@code "STR01 ISNT NUMERIC IS ALPHABETIC"}. The COBOL {@code GOBACK} maps to
     * the implicit Java method return.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner} contract;
     *                   this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: IF STR01 IS ALPHABETIC THEN DISPLAY "STR01 IS ALPHABETIC" END-IF.
        // "ABCDF    " is all letters and spaces -> IS ALPHABETIC is TRUE -> fires.
        if (isAlphabetic(STR01)) {
            System.out.println("STR01 IS ALPHABETIC");
        }

        // COBOL: IF NUM01 IS NUMERIC THEN DISPLAY "NUM01 IS NUMERIC" END-IF.
        // IS NUMERIC on a signed PIC S9(9) item is structurally always TRUE -> fires.
        if (isNumericInt(NUM01)) {
            System.out.println("NUM01 IS NUMERIC");
        }

        // COBOL: IF STR01 IS NUMERIC THEN DISPLAY "STR01 IS NUMERIC"
        //        ELSE DISPLAY "STR01 ISNT NUMERIC IS ALPHABETIC" END-IF.
        // "ABCDF    " contains letters -> IS NUMERIC is FALSE -> the ELSE branch
        // fires. The "ISNT" spelling (no apostrophe) is preserved verbatim from the
        // COBOL source (ClassCondition.cbl L28) per the minimal-change clause.
        if (isNumeric(STR01)) {
            System.out.println("STR01 IS NUMERIC");
        } else {
            System.out.println("STR01 ISNT NUMERIC IS ALPHABETIC");
        }
    }

    /**
     * COBOL {@code IS ALPHABETIC} predicate for an alphanumeric ({@code PIC X})
     * field: matches a non-empty string consisting entirely of letters and/or space
     * characters. This mirrors COBOL's class condition, where the space character is
     * considered alphabetic &mdash; without space acceptance the four trailing spaces
     * in {@code "ABCDF    "} would incorrectly fail the test.
     *
     * @param s the field value to test (the right-space-padded {@code STR01})
     * @return {@code true} if {@code s} is non-empty and every character is a letter
     *         or a space; {@code false} otherwise
     */
    private boolean isAlphabetic(String s) {
        return !s.isEmpty() && s.chars().allMatch(c -> Character.isLetter(c) || c == ' ');
    }

    /**
     * COBOL {@code IS NUMERIC} predicate for an alphanumeric ({@code PIC X}) field:
     * matches a non-empty string consisting entirely of digit characters. This
     * mirrors COBOL's class condition on {@code PIC X} items, where only an
     * all-digit content satisfies {@code IS NUMERIC}.
     *
     * @param s the field value to test (the alphanumeric {@code STR01})
     * @return {@code true} if {@code s} is non-empty and every character is a digit;
     *         {@code false} otherwise (e.g. for {@code "ABCDF    "}, which contains
     *         letters)
     */
    private boolean isNumeric(String s) {
        return !s.isEmpty() && s.chars().allMatch(Character::isDigit);
    }

    /**
     * COBOL {@code IS NUMERIC} predicate for a signed numeric ({@code PIC S9}) field.
     * Mirrors COBOL semantics where any value held in a numeric {@code PIC} always
     * satisfies {@code IS NUMERIC} (the digits and the operational sign are intrinsic
     * to the numeric representation), so the predicate is structurally {@code true}
     * for every value of an {@code S9(9)} item.
     *
     * <p>The {@code value} parameter is intentionally unused: it documents that the
     * test is performed against {@code NUM01} at the call site while the result is
     * independent of the held value. {@code @SuppressWarnings("unused")} records that
     * this is a deliberate, faithfulness-preserving design rather than an oversight.
     *
     * @param value the signed numeric value the COBOL {@code IS NUMERIC} test is
     *              applied to (intentionally not inspected; see above)
     * @return {@code true} unconditionally, matching COBOL's {@code IS NUMERIC}
     *         behavior on a {@code PIC S9(9)} item
     */
    @SuppressWarnings("unused")
    private boolean isNumericInt(int value) {
        return true;
    }
}
