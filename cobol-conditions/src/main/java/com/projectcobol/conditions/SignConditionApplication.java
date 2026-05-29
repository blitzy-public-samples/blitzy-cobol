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
 * {@code OpenCobol/Conditions/SignCondition.cbl}
 * (PROGRAM-ID: {@code SIGN-CONDITION}).
 *
 * <p>Demonstrates COBOL's <strong>sign conditions</strong> {@code IS POSITIVE},
 * {@code IS NEGATIVE} and {@code IS ZERO}, exercised across <strong>six separate
 * {@code IF} blocks</strong>. The COBOL source uses six independent
 * {@code IF ... END-IF} statements (there is no {@code ELSE} chaining between
 * them), so each test is evaluated unconditionally regardless of the outcome of
 * the preceding tests. This maps to six standalone {@code if (...) { ... }}
 * statements in Java &mdash; <em>not</em> an {@code if / else if} cascade.
 *
 * <p><strong>COBOL sign-condition semantics (critical):</strong>
 * <ul>
 *   <li>{@code IS POSITIVE} &equiv; {@code n > 0} &mdash; <em>strictly</em> greater
 *       than zero. <strong>Zero is NOT positive in COBOL.</strong></li>
 *   <li>{@code IS NEGATIVE} &equiv; {@code n < 0} &mdash; <em>strictly</em> less than
 *       zero. Zero is NOT negative.</li>
 *   <li>{@code IS ZERO} &equiv; {@code n == 0}.</li>
 * </ul>
 * Using {@code >= 0} for {@code IS POSITIVE} would be a faithfulness bug: with
 * {@code NUM03 == 0}, the sixth test ({@code NUM03 IS POSITIVE}) must evaluate
 * {@code FALSE}, so {@code "NUM03 IS POSITIVE"} must NOT be printed.
 *
 * <p>The COBOL working storage declares three numerics with fixed {@code VALUE}
 * clauses: {@code 01 NUM01 PIC S9(9) VALUE -5000} (signed, negative),
 * {@code 01 NUM02 PIC S9(9) VALUE 6} (signed, positive) and
 * {@code 01 NUM03 PIC 9(9) VALUE ZERO} (unsigned, zero). With these values exactly
 * three of the six tests fire, producing three lines that match the golden fixture
 * {@code expected/SignConditionApplication.txt} byte-for-byte:
 * <pre>
 *     NUM01 IS NEGATIVE
 *     NUM02 IS POSITIVE
 *     NUM03 IS ZERO
 * </pre>
 *
 * <p><strong>Minimal-change / preserve-behavior note (AAP &sect;0.7.2):</strong> all
 * six {@code IF} blocks are retained in the Java {@code run(...)} body, including the
 * three dead branches ({@code "NUM01 IS POSITIVE"}, {@code "NUM02 IS ZERO"},
 * {@code "NUM03 IS POSITIVE"}) that never fire for the hardcoded {@code VALUE}
 * clauses. The dead branches faithfully mirror the original COBOL control-flow
 * structure and are deliberately <em>not</em> optimized away.
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the source
 * filename ({@code SignCondition.cbl} &rarr; {@code SignConditionApplication}), not
 * the COBOL {@code PROGRAM-ID} ({@code SIGN-CONDITION}).
 *
 * <p>This entry class is one of eight {@code @SpringBootApplication} classes that
 * share the package {@code com.projectcobol.conditions} and the single executable
 * {@code cobol-conditions-1.0.0.jar}. It can be selected at run time from that
 * shared jar via the Spring Boot {@code PropertiesLauncher}:
 * <pre>
 *     java -Dloader.main=com.projectcobol.conditions.SignConditionApplication \
 *          -jar cobol-conditions/target/cobol-conditions-1.0.0.jar
 * </pre>
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 NUM01 PIC S9(9) VALUE -5000} &rarr;
 *       {@code private static final int NUM01 = -5000} (signed 9-digit numeric with
 *       an immutable {@code VALUE} clause &rarr; {@code static final} constant).</li>
 *   <li>{@code 01 NUM02 PIC S9(9) VALUE 6} &rarr;
 *       {@code private static final int NUM02 = 6}.</li>
 *   <li>{@code 01 NUM03 PIC 9(9) VALUE ZERO} &rarr;
 *       {@code private static final int NUM03 = 0} (unsigned; {@code ZERO} figurative
 *       constant &rarr; {@code 0}).</li>
 *   <li>{@code IF n IS POSITIVE} &rarr; {@code if (n > 0)} (strictly &gt; 0).</li>
 *   <li>{@code IF n IS NEGATIVE} &rarr; {@code if (n < 0)} (strictly &lt; 0).</li>
 *   <li>{@code IF n IS ZERO} &rarr; {@code if (n == 0)}.</li>
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
// established convention used by the other multi-application sub-modules
// (cobol-loops, cobol-memory, cobol-random, cobol-sort) and the sibling Conditions
// entry classes.
@SpringBootApplication
@ComponentScan(useDefaultFilters = false)
public class SignConditionApplication implements CommandLineRunner {

    // COBOL: 01 NUM01 PIC S9(9) VALUE -5000.  (SignCondition.cbl L13)
    // The leading "S" in the PICTURE makes this a signed 9-digit numeric; the
    // immutable VALUE clause maps to a compile-time constant, hence static final.
    private static final int NUM01 = -5000;

    // COBOL: 01 NUM02 PIC S9(9) VALUE 6.      (SignCondition.cbl L14)
    // Signed 9-digit numeric holding a positive value.
    private static final int NUM02 = 6;

    // COBOL: 01 NUM03 PIC 9(9) VALUE ZERO.    (SignCondition.cbl L15)
    // Unsigned 9-digit numeric; the ZERO figurative constant maps to 0.
    private static final int NUM03 = 0;

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
        SpringApplication app = new SpringApplication(SignConditionApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code SignCondition.cbl} lines 20-44, which is a
     * sequence of six independent {@code IF ... END-IF} sign-condition tests:
     * <pre>
     *     IF NUM01 IS POSITIVE THEN DISPLAY 'NUM01 IS POSITIVE' END-IF
     *     IF NUM01 IS NEGATIVE THEN DISPLAY 'NUM01 IS NEGATIVE' END-IF
     *     IF NUM02 IS ZERO     THEN DISPLAY 'NUM02 IS ZERO'     END-IF
     *     IF NUM02 IS POSITIVE THEN DISPLAY 'NUM02 IS POSITIVE' END-IF
     *     IF NUM03 IS ZERO     THEN DISPLAY 'NUM03 IS ZERO'     END-IF
     *     IF NUM03 IS POSITIVE THEN DISPLAY 'NUM03 IS POSITIVE' END-IF.
     *     GOBACK.
     * </pre>
     * With {@code NUM01 = -5000}, {@code NUM02 = 6} and {@code NUM03 = 0}, tests
     * 2, 4 and 5 fire (in that source order), printing exactly three lines:
     * {@code "NUM01 IS NEGATIVE"}, {@code "NUM02 IS POSITIVE"},
     * {@code "NUM03 IS ZERO"}. The COBOL {@code GOBACK} maps to the implicit Java
     * method return.
     *
     * <p>Each {@code if} below is a standalone statement (no {@code else if}
     * chaining), preserving the independent evaluation of the six COBOL tests.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner} contract;
     *                   this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: IF NUM01 IS POSITIVE THEN DISPLAY 'NUM01 IS POSITIVE' END-IF.
        // IS POSITIVE is strictly > 0. With NUM01 = -5000 this is FALSE -> no output.
        // Dead branch retained verbatim per the minimal-change clause.
        if (NUM01 > 0) {
            System.out.println("NUM01 IS POSITIVE");
        }

        // COBOL: IF NUM01 IS NEGATIVE THEN DISPLAY 'NUM01 IS NEGATIVE' END-IF.
        // IS NEGATIVE is strictly < 0. With NUM01 = -5000 this is TRUE -> fires.
        if (NUM01 < 0) {
            System.out.println("NUM01 IS NEGATIVE");
        }

        // COBOL: IF NUM02 IS ZERO THEN DISPLAY 'NUM02 IS ZERO' END-IF.
        // IS ZERO is == 0. With NUM02 = 6 this is FALSE -> no output.
        // Dead branch retained verbatim per the minimal-change clause.
        if (NUM02 == 0) {
            System.out.println("NUM02 IS ZERO");
        }

        // COBOL: IF NUM02 IS POSITIVE THEN DISPLAY 'NUM02 IS POSITIVE' END-IF.
        // IS POSITIVE is strictly > 0. With NUM02 = 6 this is TRUE -> fires.
        if (NUM02 > 0) {
            System.out.println("NUM02 IS POSITIVE");
        }

        // COBOL: IF NUM03 IS ZERO THEN DISPLAY 'NUM03 IS ZERO' END-IF.
        // IS ZERO is == 0. With NUM03 = 0 this is TRUE -> fires.
        if (NUM03 == 0) {
            System.out.println("NUM03 IS ZERO");
        }

        // COBOL: IF NUM03 IS POSITIVE THEN DISPLAY 'NUM03 IS POSITIVE' END-IF.
        // IS POSITIVE is strictly > 0 (zero is NOT positive in COBOL). With
        // NUM03 = 0 this is FALSE -> no output. Using >= 0 here would be a
        // faithfulness bug. Dead branch retained verbatim per the minimal-change
        // clause.
        if (NUM03 > 0) {
            System.out.println("NUM03 IS POSITIVE");
        }
    }
}
