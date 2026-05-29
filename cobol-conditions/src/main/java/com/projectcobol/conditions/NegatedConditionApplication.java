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
 * {@code OpenCobol/Conditions/NegatedCondition.cbl}
 * (PROGRAM-ID: {@code NEGATED-CONDITION}).
 *
 * <p>Demonstrates COBOL's <strong>negated condition</strong>
 * {@code IF NOT NUM01 IS LESS THAN NUM02}, with {@code NUM01 = 20} and
 * {@code NUM02 = 25}:
 * <ul>
 *   <li>Inner relation: {@code 20 < 25} is {@code TRUE}.</li>
 *   <li>Negation: {@code NOT TRUE} is {@code FALSE}.</li>
 *   <li>Result: the {@code ELSE} branch fires, printing
 *       {@code "I AM HERE : ELSE"}.</li>
 * </ul>
 *
 * <p><strong>Negation rendering (AAP &sect;0.4.1):</strong> COBOL
 * {@code IF NOT <relation>} negates the entire relational expression, so it is
 * rendered as {@code if (!(NUM01 < NUM02))} with explicit parentheses that make the
 * negation scope unambiguous and mirror the COBOL phrase structure. The
 * semantically equivalent {@code NUM01 >= NUM02} is deliberately NOT used because it
 * would obscure the literal COBOL {@code NOT (...)} construct.
 *
 * <p><strong>Trailing-whitespace asymmetry (AAP &sect;0.7.3, preserve behavior
 * verbatim):</strong> the two COBOL {@code DISPLAY} literals have asymmetric trailing
 * whitespace and are reproduced byte-for-byte:
 * <ul>
 *   <li>THEN branch (source line 18): {@code "I AM HERE : IF "} &mdash; HAS a single
 *       trailing space inside the literal.</li>
 *   <li>ELSE branch (source line 20): {@code "I AM HERE : ELSE"} &mdash; has NO
 *       trailing space (closes on {@code 'E'}).</li>
 * </ul>
 * Because {@code NUM01 = 20} and {@code NUM02 = 25} are fixed {@code VALUE} clauses,
 * the {@code ELSE} branch fires deterministically and the program emits exactly
 * {@code "I AM HERE : ELSE\n"} (17 bytes including the line feed), matching the golden
 * fixture {@code expected/NegatedConditionApplication.txt} byte-for-byte. The THEN
 * branch never fires for these hardcoded values but is retained verbatim (including
 * its trailing space) per the minimal-change clause &mdash; it is NOT optimized away.
 *
 * <p>Per the AAP filename-traceability rule, the Java class name follows the source
 * filename ({@code NegatedCondition.cbl} &rarr; {@code NegatedConditionApplication}),
 * not the COBOL {@code PROGRAM-ID} ({@code NEGATED-CONDITION}).
 *
 * <p>This entry class is one of eight {@code @SpringBootApplication} classes that
 * share the package {@code com.projectcobol.conditions} and the single executable
 * {@code cobol-conditions-1.0.0.jar}. It can be selected at run time from that shared
 * jar via the Spring Boot {@code PropertiesLauncher}:
 * <pre>
 *     java -Dloader.main=com.projectcobol.conditions.NegatedConditionApplication \
 *          -jar cobol-conditions/target/cobol-conditions-1.0.0.jar
 * </pre>
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 NUM01 PIC 9(2) VALUE 20} &rarr;
 *       {@code private static final int NUM01 = 20} (unsigned 2-digit numeric with an
 *       immutable {@code VALUE} clause &rarr; {@code static final} constant).</li>
 *   <li>{@code 01 NUM02 PIC 9(9) VALUE 25} &rarr;
 *       {@code private static final int NUM02 = 25}.</li>
 *   <li>{@code IF NOT NUM01 IS LESS THAN NUM02} &rarr;
 *       {@code if (!(NUM01 < NUM02))}.</li>
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
public class NegatedConditionApplication implements CommandLineRunner {

    // COBOL: 01 NUM01 PIC 9(2) VALUE 20.  (NegatedCondition.cbl L11)
    // Unsigned 2-digit numeric; the immutable VALUE clause maps to a compile-time
    // constant, hence static final.
    private static final int NUM01 = 20;

    // COBOL: 01 NUM02 PIC 9(9) VALUE 25.  (NegatedCondition.cbl L12)
    // Unsigned 9-digit numeric holding the comparison bound.
    private static final int NUM02 = 25;

    /**
     * Spring Boot bootstrap entry point. Delegates to
     * {@link SpringApplication#run(Class, String...)}, which initializes the
     * {@code ApplicationContext} and then invokes this class's
     * {@link #run(String...)} callback (the {@link CommandLineRunner} contract). This
     * wires the COBOL {@code PROCEDURE DIVISION} / {@code MAIN-PROCEDURE} /
     * {@code GOBACK} lifecycle to a Spring Boot batch-style entry class.
     *
     * @param args command-line arguments forwarded to Spring Boot (unused by this
     *             translation, but required by the Spring Boot contract)
     */
    public static void main(String[] args) {
        SpringApplication.run(NegatedConditionApplication.class, args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code NegatedCondition.cbl} lines 17-23:
     * <pre>
     *     IF NOT NUM01 IS LESS THAN NUM02 THEN
     *       DISPLAY "I AM HERE : IF "
     *     ELSE
     *       DISPLAY "I AM HERE : ELSE"
     *     END-IF.
     *     GOBACK.
     * </pre>
     * With {@code NUM01 = 20} and {@code NUM02 = 25}, {@code 20 < 25} is {@code TRUE},
     * so {@code NOT (20 < 25)} is {@code FALSE} and the {@code ELSE} branch fires,
     * printing {@code "I AM HERE : ELSE"}. The COBOL {@code GOBACK} maps to the
     * implicit Java method return.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner} contract;
     *                   this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: IF NOT NUM01 IS LESS THAN NUM02 THEN ... ELSE ... END-IF.
        // NOT negates the whole relation; explicit parentheses around (NUM01 < NUM02)
        // preserve the COBOL phrase structure (see class Javadoc, AAP §0.4.1).
        if (!(NUM01 < NUM02)) {
            // THEN branch (NegatedCondition.cbl L18). Dead for NUM01=20, NUM02=25 but
            // retained verbatim per the minimal-change clause. NOTE: the literal ends
            // with a single trailing space INSIDE the quotes ("...IF "), exactly as in
            // the COBOL source -- do not strip it.
            System.out.println("I AM HERE : IF ");
        } else {
            // ELSE branch (NegatedCondition.cbl L20). This is the branch that fires.
            // The literal ends on 'E' with NO trailing space, producing the 17-byte
            // golden output "I AM HERE : ELSE\n".
            System.out.println("I AM HERE : ELSE");
        }
    }
}
