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

package com.projectcobol.database;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java 21 / Spring Boot 3.x translation of
 * {@code OpenCobol/Database/StatusCode.cbl} (PROGRAM-ID {@code STATUS-CODE}).
 *
 * <p>This is the minimal member of the {@code cobol-database} sub-module: the
 * COBOL {@code MAIN-PROCEDURE} ({@code StatusCode.cbl:L41-44}) consists solely
 * of {@code DISPLAY "Hello world"} followed by {@code GOBACK}, so the Java
 * {@link #run(String...)} body is the single statement
 * {@code System.out.println("Hello world")}.
 *
 * <p>The 30 {@code 88-level} {@code FILE-STATUS} condition declarations in the
 * COBOL {@code WORKING-STORAGE SECTION} ({@code StatusCode.cbl:L10-39}) are NOT
 * reproduced in this class. Per AAP &sect;0.6.3 they are extracted into the
 * dedicated {@link FileStatus} enum (same package), which is consumed by the
 * file-reading translations elsewhere in this module rather than by this
 * trivial program. Consequently this class needs no I/O, no JDBC and no
 * reference to {@link FileStatus} beyond this documentation cross-link.
 *
 * <p>Per the AAP &sect;0.7.3 filename-traceability rule, the Java class name
 * follows the source {@code .cbl} filename ({@code StatusCode.cbl} &rarr;
 * {@code StatusCodeApplication}) rather than the COBOL {@code PROGRAM-ID}
 * ({@code STATUS-CODE}).
 */
@SpringBootApplication
public class StatusCodeApplication implements CommandLineRunner {

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
        SpringApplication.run(StatusCodeApplication.class, args);
    }

    /**
     * Spring Boot {@link CommandLineRunner} entry point. Translates the COBOL
     * {@code MAIN-PROCEDURE} from {@code StatusCode.cbl} lines 41-44:
     * <pre>
     *     PROCEDURE DIVISION.
     *     MAIN-PROCEDURE.
     *         DISPLAY "Hello world"
     *         GOBACK.
     * </pre>
     * <p>The COBOL {@code DISPLAY "Hello world"} maps to
     * {@code System.out.println("Hello world")}, which emits the line followed
     * by the platform line separator; on the build/runtime platform this yields
     * the 12-byte sequence {@code Hello world\n} expected by the golden-output
     * fixture. The COBOL {@code GOBACK} maps to the implicit method return after
     * the single statement completes.
     *
     * @param args command-line arguments (unused; required by the
     *             {@link CommandLineRunner} contract)
     * @throws Exception declared to satisfy the {@link CommandLineRunner}
     *                   contract; this translation never actually throws
     */
    @Override
    public void run(String... args) throws Exception {
        // COBOL: DISPLAY "Hello world".  (StatusCode.cbl L43)
        System.out.println("Hello world");
        // COBOL: GOBACK. (StatusCode.cbl L44) -> implicit Java method return
    }
}
