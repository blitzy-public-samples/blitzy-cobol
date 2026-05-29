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
 * {@code OpenCobol/Conditions/CombinedConditions.cbl}
 * (PROGRAM-ID: {@code COMBINED-CONDITIONS}).
 *
 * <p>Demonstrates COBOL's compound logical condition (the {@code AND} conjunction):
 * {@code IF NUM01 IS LESS THAN NUM02 AND NUM01 = NUM03}. The COBOL working storage
 * declares three unsigned 3-digit numerics with immutable {@code VALUE} clauses
 * ({@code NUM01 = 50}, {@code NUM02 = 20}, {@code NUM03 = 30}) and no subsequent
 * {@code MOVE}, so each maps to a {@code private static final int} constant.
 *
 * <p><strong>Deterministic outcome (short-circuit AND):</strong> with
 * {@code NUM01 = 50} and {@code NUM02 = 20}, the first conjunct {@code 50 < 20} is
 * {@code false}. COBOL's {@code AND} and Java's {@code &&} both short-circuit, so the
 * second conjunct {@code NUM01 == NUM03} is never evaluated; the condition is
 * {@code false} and the {@code ELSE} branch fires. The program therefore prints
 * exactly one line that matches the golden fixture
 * {@code expected/CombinedConditionsApplication.txt} byte-for-byte.
 *
 * <p><strong>Translation note &mdash; trailing space is byte-significant
 * (AAP &sect;0.7.3):</strong> the {@code ELSE} branch's DISPLAY literal
 * {@code "I AM HERE :( "} includes a single trailing space character <em>inside</em>
 * the string literal (preserved verbatim from {@code CombinedConditions.cbl} line 21,
 * where the closing quote is preceded by one space). The Java translation MUST
 * preserve this trailing space; the 14-byte golden fixture ends in the byte sequence
 * {@code 0x20 0x0A} (space then LF). The literal must never be trimmed, stripped, or
 * normalized &mdash; doing so would emit 13 bytes and fail the golden-output test.
 *
 * <p><strong>Minimal-change / preserve-behavior note (AAP &sect;0.7.2):</strong> both
 * the {@code THEN} branch ({@code "I AM HERE!"}) and the {@code ELSE} branch
 * ({@code "I AM HERE :( "}) are retained in the Java {@code run(...)} body, even
 * though the {@code THEN} branch never fires for the hardcoded values. The dead
 * branch faithfully mirrors the original COBOL control-flow structure and is
 * deliberately <em>not</em> optimized away.
 *
 * <p>Per the AAP filename-traceability rule (&sect;0.7.3), the Java class name follows
 * the source filename ({@code CombinedConditions.cbl} &rarr;
 * {@code CombinedConditionsApplication}); here the {@code PROGRAM-ID}
 * ({@code COMBINED-CONDITIONS}) and the filename agree.
 *
 * <p>This entry class is one of eight {@code @SpringBootApplication} classes that
 * share the package {@code com.projectcobol.conditions} and the single executable
 * {@code cobol-conditions-1.0.0.jar}. It can be selected at run time from that shared
 * jar via the Spring Boot {@code PropertiesLauncher}:
 * <pre>
 *     java -Dloader.main=com.projectcobol.conditions.CombinedConditionsApplication \
 *          -jar cobol-conditions/target/cobol-conditions-1.0.0.jar
 * </pre>
 *
 * <h2>COBOL &rarr; Java translation rules applied</h2>
 * <ul>
 *   <li>{@code 01 NUM01 PIC 9(3) VALUE 50} &rarr;
 *       {@code private static final int NUM01 = 50} (immutable {@code VALUE} clause,
 *       no {@code MOVE}, hence a compile-time constant).</li>
 *   <li>{@code 01 NUM02 PIC 9(3) VALUE 20} &rarr;
 *       {@code private static final int NUM02 = 20}.</li>
 *   <li>{@code 01 NUM03 PIC 9(3) VALUE 30} &rarr;
 *       {@code private static final int NUM03 = 30}.</li>
 *   <li>{@code IF NUM01 IS LESS THAN NUM02 AND NUM01 = NUM03} &rarr;
 *       {@code if (NUM01 < NUM02 && NUM01 == NUM03)} (COBOL {@code AND} &rarr; Java
 *       {@code &&}, both short-circuit).</li>
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
public class CombinedConditionsApplication implements CommandLineRunner {

    /**
     * COBOL field {@code 01 NUM01 PIC 9(3) VALUE 50} (CombinedConditions.cbl L11).
     *
     * <p>An unsigned 3-digit numeric whose {@code VALUE} clause is never reassigned
     * by a {@code MOVE} in the PROCEDURE DIVISION, so it is modeled as an immutable
     * {@code static final} constant rather than a mutable instance field.
     */
    private static final int NUM01 = 50;

    /**
     * COBOL field {@code 01 NUM02 PIC 9(3) VALUE 20} (CombinedConditions.cbl L12).
     *
     * <p>Unsigned 3-digit numeric; immutable {@code VALUE} clause, hence
     * {@code static final}.
     */
    private static final int NUM02 = 20;

    /**
     * COBOL field {@code 01 NUM03 PIC 9(3) VALUE 30} (CombinedConditions.cbl L13).
     *
     * <p>Unsigned 3-digit numeric; immutable {@code VALUE} clause, hence
     * {@code static final}.
     */
    private static final int NUM03 = 30;

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
        SpringApplication app = new SpringApplication(CombinedConditionsApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code CombinedConditions.cbl} lines 16-24:
     * <pre>
     *     IF NUM01 IS LESS THAN NUM02 AND NUM01 = NUM03 THEN
     *       DISPLAY "I AM HERE!"
     *     ELSE
     *       DISPLAY "I AM HERE :( "
     *     END-IF.
     *     GOBACK.
     * </pre>
     * With {@code NUM01 == 50} and {@code NUM02 == 20}, the first conjunct
     * {@code 50 < 20} is {@code false}; the {@code &&} short-circuits (mirroring
     * COBOL {@code AND}) so {@code NUM01 == NUM03} is never evaluated, and the
     * {@code ELSE} branch prints {@code "I AM HERE :( "} (note the byte-significant
     * trailing space). The COBOL {@code GOBACK} maps to the implicit Java method
     * return.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner} contract;
     *                   this implementation throws nothing
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: IF NUM01 IS LESS THAN NUM02 AND NUM01 = NUM03 THEN ... ELSE ... END-IF.
        //        (CombinedConditions.cbl L18-22)
        // COBOL AND maps to Java &&; both short-circuit. With NUM01 = 50 and
        // NUM02 = 20 the first conjunct (50 < 20) is FALSE, so NUM01 == NUM03 is
        // never evaluated and the ELSE branch fires deterministically.
        if (NUM01 < NUM02 && NUM01 == NUM03) {
            // COBOL: DISPLAY "I AM HERE!"  (CombinedConditions.cbl L19)
            // Dead branch for the hardcoded values; retained verbatim per the
            // minimal-change clause to mirror the original COBOL control flow.
            System.out.println("I AM HERE!");
        } else {
            // COBOL: DISPLAY "I AM HERE :( "  (CombinedConditions.cbl L21)
            // The literal ends with a single trailing space INSIDE the quotes,
            // preserved byte-for-byte from the COBOL source. Combined with the LF
            // emitted by println this yields the 14-byte golden fixture
            // (... 0x28 0x20 0x0A). Do NOT trim/strip the trailing space.
            System.out.println("I AM HERE :( ");
        }
    }
}
