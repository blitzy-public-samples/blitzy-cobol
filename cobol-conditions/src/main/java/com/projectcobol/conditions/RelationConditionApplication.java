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
 * {@code OpenCobol/Conditions/RelationCondition.cbl}
 * (PROGRAM-ID: {@code RELATION-CONDITION}).
 *
 * <p>Demonstrates COBOL's relational condition {@code IS GREATER THAN OR EQUAL TO}.
 * The COBOL working storage declares {@code 01 NUM01 PIC 9(9) VALUE 50} and
 * {@code 01 NUM02 PIC 9(9) VALUE 6}; the {@code MAIN-PROCEDURE} evaluates
 * {@code IF NUM01 IS GREATER THAN OR EQUAL TO NUM02}. With {@code NUM01 = 50} and
 * {@code NUM02 = 6}, the test {@code 50 >= 6} is {@code TRUE}, so the THEN branch
 * fires, printing {@code "NUMBER01 IS GREATER OR EQUAL THAN NUMBER02"}.
 *
 * <p><strong>Translation note (non-standard English preserved verbatim):</strong>
 * The COBOL source uses the non-standard English phrasing
 * {@code "GREATER OR EQUAL THAN"} in the display literal on
 * {@code RelationCondition.cbl:L18} (standard English would be
 * {@code "GREATER THAN OR EQUAL TO"}). This original-author quirk is preserved
 * character-for-character in the Java string literal per the AAP's
 * preserve-existing-behavior / minimal-change rule (&sect;0.7.3). The grammar is
 * deliberately <em>not</em> corrected. The Java operator used in the condition
 * test is the standard {@code >=}, which faithfully mirrors the COBOL
 * {@code IS GREATER THAN OR EQUAL TO} inclusive comparison (so the boundary case
 * {@code NUM01 == NUM02} would also evaluate {@code TRUE}, even though it does not
 * arise for these specific values).
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the
 * source filename ({@code RelationCondition.cbl} &rarr;
 * {@code RelationConditionApplication}).
 *
 * <p>This entry class is one of eight {@code @SpringBootApplication} classes that
 * share the package {@code com.projectcobol.conditions} and the single executable
 * {@code cobol-conditions-1.0.0.jar}. It can be selected at run time from that
 * shared jar via the Spring Boot {@code PropertiesLauncher}:
 * <pre>
 *     java -Dloader.main=com.projectcobol.conditions.RelationConditionApplication \
 *          -jar cobol-conditions/target/cobol-conditions-1.0.0.jar
 * </pre>
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 NUM01 PIC 9(9) VALUE 50} &rarr;
 *       {@code private static final int NUM01 = 50} (unsigned 9-digit numeric,
 *       immutable {@code VALUE} clause &rarr; {@code static final}).</li>
 *   <li>{@code 01 NUM02 PIC 9(9) VALUE 6} &rarr;
 *       {@code private static final int NUM02 = 6}.</li>
 *   <li>{@code IF NUM01 IS GREATER THAN OR EQUAL TO NUM02} &rarr;
 *       {@code if (NUM01 >= NUM02)}.</li>
 *   <li>{@code DISPLAY '...'} &rarr; {@code System.out.println("...")}.</li>
 *   <li>{@code GOBACK} &rarr; implicit return at the end of {@link #run(String...)}.</li>
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
// (cobol-loops, cobol-memory, cobol-random, cobol-sort).
@SpringBootApplication
@ComponentScan(useDefaultFilters = false)
public class RelationConditionApplication implements CommandLineRunner {

    // COBOL: 01 NUM01 PIC 9(9) VALUE 50.  (RelationCondition.cbl L11)
    // PIC 9(9) is an unsigned 9-digit numeric; the immutable VALUE clause maps to
    // a compile-time constant, hence static final.
    private static final int NUM01 = 50;

    // COBOL: 01 NUM02 PIC 9(9) VALUE 6.   (RelationCondition.cbl L12)
    private static final int NUM02 = 6;

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
        SpringApplication app = new SpringApplication(RelationConditionApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code RelationCondition.cbl} lines 15-23:
     * <pre>
     *     IF NUM01 IS GREATER THAN OR EQUAL TO NUM02 THEN
     *       DISPLAY 'NUMBER01 IS GREATER OR EQUAL THAN NUMBER02'
     *     ELSE
     *       DISPLAY 'NUMBER01 IS LESS THAN NUMBER02'
     *     END-IF.
     *     GOBACK.
     * </pre>
     * With {@code NUM01 = 50} and {@code NUM02 = 6}, {@code 50 >= 6} is {@code TRUE},
     * so the THEN branch fires and exactly one line is printed:
     * {@code "NUMBER01 IS GREATER OR EQUAL THAN NUMBER02"}. The COBOL {@code GOBACK}
     * maps to the implicit Java method return.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner} contract;
     *                   this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: IF NUM01 IS GREATER THAN OR EQUAL TO NUM02 THEN ... ELSE ... END-IF.
        // The COBOL "IS GREATER THAN OR EQUAL TO" inclusive relational condition
        // maps to the standard Java >= operator (NOT >), preserving semantic
        // faithfulness for the boundary case NUM01 == NUM02.
        if (NUM01 >= NUM02) {
            // COBOL: DISPLAY 'NUMBER01 IS GREATER OR EQUAL THAN NUMBER02'.
            // NOTE: "GREATER OR EQUAL THAN" is non-standard English (standard would
            // be "GREATER THAN OR EQUAL TO"). This original-author wording is
            // preserved verbatim per the minimal-change rule -- do NOT correct it.
            System.out.println("NUMBER01 IS GREATER OR EQUAL THAN NUMBER02");
        } else {
            // COBOL: DISPLAY 'NUMBER01 IS LESS THAN NUMBER02'.
            // Dead branch for the fixed VALUE clauses (50 >= 6 is always TRUE), but
            // retained to faithfully mirror the COBOL ELSE per the minimal-change
            // clause.
            System.out.println("NUMBER01 IS LESS THAN NUMBER02");
        }
    }
}
