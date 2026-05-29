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
 * {@code OpenCobol/Conditions/ConditionStatement.cbl}
 * (PROGRAM-ID: {@code CONDITION-STATEMENT}).
 *
 * <p>Demonstrates a <strong>nested {@code IF}/{@code ELSE}</strong> structure built
 * from two relational conditions ({@code <=} / {@code IS LESS THAN OR EQUAL TO} and
 * {@code >=} / {@code IS GREATER THAN OR EQUAL TO}). The COBOL working storage
 * declares four unsigned 9-digit numerics with fixed {@code VALUE} clauses
 * (NUM01=5, NUM02=6, NUM03=7, NUM04=8) and the {@code MAIN-PROCEDURE} nests an inner
 * {@code IF}/{@code ELSE} <em>inside</em> the outer {@code THEN} branch:
 * <ul>
 *   <li>Outer test: {@code 5 <= 6} is {@code TRUE} &rarr; enters the outer
 *       {@code THEN}, prints {@code "IS NOT LESS"}.</li>
 *   <li>Inner test (only reached on the outer {@code THEN} path): {@code 7 >= 8} is
 *       {@code FALSE} &rarr; enters the inner {@code ELSE}, prints
 *       {@code "IS NOT GREATER"}.</li>
 * </ul>
 * For these fixed values the program therefore emits exactly two lines, matching the
 * golden fixture {@code expected/ConditionStatementApplication.txt} byte-for-byte:
 * <pre>
 *     IS NOT LESS
 *     IS NOT GREATER
 * </pre>
 *
 * <p><strong>Minimal-change / preserve-behavior note (AAP &sect;0.7.2):</strong> the
 * inner {@code THEN} literal {@code "IS GREATER"} and the outer {@code ELSE} literal
 * {@code "IS LESS"} are dead branches for the hardcoded {@code VALUE} clauses, yet
 * they are retained verbatim in the Java {@code run(...)} body so the translation
 * mirrors the original COBOL control-flow structure exactly. The dead branches are
 * deliberately <em>not</em> optimized away.
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the source
 * filename ({@code ConditionStatement.cbl} &rarr; {@code ConditionStatementApplication}),
 * not the COBOL {@code PROGRAM-ID} ({@code CONDITION-STATEMENT}).
 *
 * <p>This entry class is one of eight {@code @SpringBootApplication} classes that
 * share the package {@code com.projectcobol.conditions} and the single executable
 * {@code cobol-conditions-1.0.0.jar}. It can be selected at run time from that
 * shared jar via the Spring Boot {@code PropertiesLauncher}:
 * <pre>
 *     java -Dloader.main=com.projectcobol.conditions.ConditionStatementApplication \
 *          -jar cobol-conditions/target/cobol-conditions-1.0.0.jar
 * </pre>
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 NUM01 PIC 9(9) VALUE 5} &rarr;
 *       {@code private static final int NUM01 = 5} (unsigned 9-digit numeric with an
 *       immutable {@code VALUE} clause &rarr; {@code static final} constant). Likewise
 *       for NUM02=6, NUM03=7, NUM04=8.</li>
 *   <li>{@code IF NUM01 <= NUM02 THEN ... ELSE ... END-IF} &rarr;
 *       {@code if (NUM01 <= NUM02) { ... } else { ... }} (inclusive
 *       {@code IS LESS THAN OR EQUAL TO}).</li>
 *   <li>{@code IF NUM03 >= NUM04 THEN ... ELSE ... END-IF} (nested inside the outer
 *       {@code THEN}) &rarr; nested {@code if (NUM03 >= NUM04) { ... } else { ... }}
 *       (inclusive {@code IS GREATER THAN OR EQUAL TO}).</li>
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
public class ConditionStatementApplication implements CommandLineRunner {

    // COBOL: 01 NUM01 PIC 9(9) VALUE 5.  (ConditionStatement.cbl L10)
    // PIC 9(9) is an unsigned 9-digit numeric; the immutable VALUE clause maps to a
    // compile-time constant, hence static final.
    private static final int NUM01 = 5;

    // COBOL: 01 NUM02 PIC 9(9) VALUE 6.  (ConditionStatement.cbl L11)
    private static final int NUM02 = 6;

    // COBOL: 01 NUM03 PIC 9(9) VALUE 7.  (ConditionStatement.cbl L12)
    private static final int NUM03 = 7;

    // COBOL: 01 NUM04 PIC 9(9) VALUE 8.  (ConditionStatement.cbl L13)
    private static final int NUM04 = 8;

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
        SpringApplication.run(ConditionStatementApplication.class, args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code ConditionStatement.cbl} lines 16-31:
     * <pre>
     *     IF NUM01 <= NUM02 THEN
     *       DISPLAY "IS NOT LESS"
     *
     *       IF NUM03 >= NUM04 THEN
     *         DISPLAY "IS GREATER"
     *       ELSE
     *         DISPLAY "IS NOT GREATER"
     *       END-IF
     *
     *     ELSE
     *       DISPLAY "IS LESS"
     *     END-IF.
     *     GOBACK.
     * </pre>
     * With {@code NUM01 = 5}, {@code NUM02 = 6}, {@code NUM03 = 7}, {@code NUM04 = 8}:
     * the outer test {@code 5 <= 6} is {@code TRUE} so the outer {@code THEN} fires
     * ({@code "IS NOT LESS"}); the nested inner test {@code 7 >= 8} is {@code FALSE}
     * so the inner {@code ELSE} fires ({@code "IS NOT GREATER"}). The COBOL
     * {@code GOBACK} maps to the implicit Java method return.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner} contract;
     *                   this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: IF NUM01 <= NUM02 THEN ... ELSE ... END-IF.
        // The COBOL "IS LESS THAN OR EQUAL TO" (symbolic <=) inclusive relational
        // condition maps to the standard Java <= operator (NOT <), preserving the
        // boundary semantics where NUM01 == NUM02 also evaluates TRUE.
        if (NUM01 <= NUM02) {
            // COBOL: DISPLAY "IS NOT LESS".  Outer THEN branch -- FIRES for 5 <= 6.
            System.out.println("IS NOT LESS");

            // COBOL: IF NUM03 >= NUM04 THEN ... ELSE ... END-IF.  This inner IF/ELSE
            // is nested INSIDE the outer THEN branch (it is reached only when the
            // outer condition is TRUE), faithfully mirroring the COBOL nesting. The
            // "IS GREATER THAN OR EQUAL TO" (symbolic >=) inclusive condition maps to
            // the standard Java >= operator.
            if (NUM03 >= NUM04) {
                // COBOL: DISPLAY "IS GREATER".  Inner THEN branch -- dead branch for
                // the fixed VALUE clauses (7 >= 8 is FALSE), retained verbatim per
                // the minimal-change clause to mirror the COBOL structure exactly.
                System.out.println("IS GREATER");
            } else {
                // COBOL: DISPLAY "IS NOT GREATER".  Inner ELSE branch -- FIRES for
                // 7 >= 8 being FALSE.
                System.out.println("IS NOT GREATER");
            }
        } else {
            // COBOL: DISPLAY "IS LESS".  Outer ELSE branch -- dead branch for the
            // fixed VALUE clauses (5 <= 6 is TRUE), retained verbatim per the
            // minimal-change clause to mirror the COBOL structure exactly.
            System.out.println("IS LESS");
        }
    }
}
