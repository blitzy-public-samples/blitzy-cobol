# Technical Specification

# 0. Agent Action Plan

## 0.1 Intent Clarification

### 0.1.1 Core Refactoring Objective

Based on the prompt, the Blitzy platform understands that the refactoring objective is to **translate every runnable COBOL sample program in the `OpenCobol/` branch of Project COBOL into idiomatic, well-structured Java 21 / Spring Boot 3.x implementations** while preserving the original pedagogical character of the repository and its dual-platform educational mission [`README.md:L1-56`, tech-spec §1.2.1].

- **Refactoring Type:** Tech stack migration (PRIMARY), with concurrent code-structure, design-pattern, and modularity refactoring. There are no performance objectives — the source is an educational sample set [tech-spec §2.4.1].
- **Target Repository:** Same repository at `https://github.com/Martinfx/Cobol`. The translated Java sub-modules live alongside the untouched `OpenCobol/` COBOL sources and the entirely untouched `AS400/` branch. No new repository is created.
- **Refactoring Goals (enhanced for clarity):**
    - Convert every translatable `.cbl` file under `OpenCobol/` into a discrete Java class within a per-topic Maven sub-module, preserving filename traceability (one `.cbl` produces one `*Application.java`).
    - Bootstrap each Java entry class with the Spring Boot `CommandLineRunner` pattern (`@SpringBootApplication` annotation + `implements CommandLineRunner` + `run(String... args)` body) so that the procedural COBOL `PROCEDURE DIVISION` → `MAIN-PROCEDURE` → `GOBACK` lifecycle maps cleanly to a Spring Boot lifecycle entry point.
    - Establish a Maven multi-module project structure with a root parent POM aggregating eleven topic sub-modules: `cobol-helloworld`, `cobol-conditions`, `cobol-database`, `cobol-date`, `cobol-loops`, `cobol-memory`, `cobol-random`, `cobol-sqlite`, `cobol-sort`, `cobol-string`, and `cobol-struct`.
    - Replace the `ocsqlite` C-binding used by `OpenCobol/SQLite/Hello_SQLITE.cbl` [tech-spec §3.4.4] with `org.xerial:sqlite-jdbc` accessed through the standard `java.sql` API.
    - Remediate the SQL injection vulnerability in `OpenCobol/SQLite/Hello_SQLITE.cbl` by replacing text-substitution SQL execution with `java.sql.PreparedStatement` parameter binding [tech-spec §2.4.4, §5.3.5, §6.2.3.1]. This is the **one and only** intentional behavioral improvement permitted by the prompt's minimal-change clause.
    - Introduce JUnit 5 golden-output testing per sub-module, with expected stdout fixtures stored at `src/test/resources/expected/<ClassName>.txt`.
    - Compile every Java source under Java 21 with `-Xlint:all` enabled and zero warnings.
    - Apply a GPLv3 copyright header to every new Java source file to align with the project's existing `LICENSE` posture [tech-spec §3.7].

- **Implicit Requirements Surfaced (not stated outright but mandatory for the build to succeed):**
    - A root `pom.xml` (parent Maven POM) must be created that inherits from `spring-boot-starter-parent`, declares `<java.version>21</java.version>`, lists every sub-module, and pins the `maven-compiler-plugin` argument list to `-Xlint:all -Werror`.
    - A `.gitignore` must be created (or extended) at the repository root to exclude Java/Maven build artifacts (`target/`, `*.class`, `*.jar`, IDE metadata).
    - Each Maven sub-module needs its own `pom.xml` inheriting from the root parent.
    - Each sub-module needs the canonical Maven layout: `src/main/java/`, `src/main/resources/` (where applicable for fixtures), `src/test/java/`, and `src/test/resources/expected/`.
    - The original `.cbl` files in `OpenCobol/` are **NOT** deleted or modified — they remain as authoritative references against which golden-output tests are calibrated.
    - A new `OpenCobol/Games/README.md` documents why the `OpenCobol/Games/raylib/` subfolder is excluded from Java translation (the raylib C-library binding has no idiomatic Java equivalent and translating it would constitute new feature work rather than refactoring).
    - The existing root `README.md` is **appended** with a Java 21 / Spring Boot build/run section; existing COBOL build content is preserved verbatim.

### 0.1.2 Technical Interpretation

This refactoring translates to the following technical transformation strategy:

The current Project COBOL repository is a flat collection of standalone `.cbl` files organized by topic under `OpenCobol/`, with no build system, no automated testing, no managed dependencies, and platform-specific compilation paths via `cobc` (standard, raylib, and SQLite variants) [tech-spec §2.4.1, §5.3.4]. The target architecture is a **Maven multi-module Spring Boot 3.x project** in which each topic folder is mirrored by a Java sub-module, every `.cbl` file is reproduced as a Java entry class implementing `CommandLineRunner`, and runtime concerns (file I/O, SQL access, randomization, console output) are routed through the Java standard library and standard Spring Boot dependencies.

**Current Architecture → Target Architecture Mapping:**

| Current (COBOL) | Target (Java 21 / Spring Boot 3.x) |
|-----------------|------------------------------------|
| `.cbl` files under `OpenCobol/<topic>/` | `*Application.java` files under `cobol-<topic>/src/main/java/com/projectcobol/<topic>/` |
| `PROGRAM-ID` + `PROCEDURE DIVISION` + `MAIN-PROCEDURE` + `GOBACK` | `@SpringBootApplication` class + `implements CommandLineRunner` + `run(String... args)` method |
| `WORKING-STORAGE SECTION` declarations | Java class-level fields with equivalent initialization |
| `PERFORM <section-name>` and labelled sections | `private void` methods invoked from `run(...)` |
| `DISPLAY` | `System.out.println(...)` |
| `ACCEPT` from terminal | `java.util.Scanner` reading `System.in` |
| `IF / ELSE` | Java `if / else` |
| `EVALUATE TRUE WHEN ... WHEN ...` | Java `switch` expression (Java 21 enhanced switch) |
| `88-level` condition names | `static final boolean` constants, private predicate methods, or Java `enum` (used in `cobol-database`) |
| `FILE-CONTROL / FILE-STATUS / READ / OPEN / CLOSE` | `java.io.BufferedReader` with try-with-resources; `java.nio.file.Files`; a `FileStatus` enum encodes COBOL status codes |
| `PERFORM UNTIL <condition>` | Java `while (!condition) { ... }` |
| `PERFORM VARYING X FROM A BY B UNTIL ...` | Java `for (int x = a; ! condition; x += b)` |
| `POINTER` / `ADDRESS OF` / `LINKAGE SECTION` overlay | Java object references with `String.substring` views over a backing buffer; explanatory comments document the COBOL→Java semantic mapping |
| `FUNCTION RANDOM` | `java.util.Random.nextDouble()` or `Random.nextInt(...)` |
| `FUNCTION SECONDS-PAST-MIDNIGHT` seeding | `System.currentTimeMillis() % 86400` |
| `SCREEN SECTION ACCEPT` feeding raw SQL | `java.sql.PreparedStatement` parameter binding (**SECURITY FIX**) |
| `ocsqlite_init` / `ocsqlite` (exec) / `ocsqlite_close` C-API | `java.sql.DriverManager` / `Connection.prepareStatement` / try-with-resources auto-close |
| `callback-proc` PROCEDURE-POINTER + 20-slot `sql-table` row buffer | `java.sql.ResultSet` iteration with `while (rs.next())` |
| `OCCURS n TIMES` arrays | Java arrays (`int[]`, `String[]`) |
| `78-level` constants | `static final` fields |
| Grouped data items (`01` groups with `03` elementaries) | Inner classes or Java `record` types where semantically appropriate |
| Substring reference syntax `name(pos:len)` | `String.substring(pos - 1, pos - 1 + len)` (with 1-based → 0-based offset comment) |

**Transformation Rules and Patterns:**

- **Filename Traceability Rule:** Every `*Application.java` is named by Pascal-casing the basename of its source `.cbl` file (e.g., `OpenCobol/Loops/While.cbl` → `WhileApplication.java`, `OpenCobol/Memory/Address.cbl` → `AddressApplication.java`). This rule overrides the COBOL `PROGRAM-ID` when the two diverge (e.g., `PROGRAM-ID. WORK-OFFSET.` [`OpenCobol/Memory/Address.cbl:L6`] becomes `AddressApplication`, not `WorkOffsetApplication`, because the filesystem name is the authoritative identifier developers reference).
- **One-`.cbl`-One-Class Rule:** No COBOL program is merged with another; each `.cbl` produces exactly one Java class. This preserves the pedagogical isolation that gives the repository its educational value.
- **No-Cross-Submodule-Dependency Rule:** Each `cobol-<topic>` sub-module is self-contained. There is no shared "common" module; even the `FileStatus` enum used across the four database programs lives inside `cobol-database` because no other sub-module references COBOL file-status semantics.
- **Verbatim SQL Preservation Rule:** SQL DDL/DML strings in `Hello_SQLITE.cbl` (the `CREATE TABLE trial`, `INSERT INTO trial VALUES (NULL, lower(hex(randomblob(20))), datetime())`, etc.) are preserved character-for-character in the Java translation — only the **execution mechanism** changes (text-substitution → `PreparedStatement`). The SQLite-specific functions `randomblob`, `hex`, `lower`, `datetime`, `julianday` remain in the SQL strings [`OpenCobol/SQLite/Hello_SQLITE.cbl:L204-208`].
- **Untouched-Source Rule:** The original `.cbl` files in `OpenCobol/` are **never** modified or deleted. They remain in place as the authoritative reference against which JUnit 5 golden-output fixtures are calibrated, and the dual-platform educational mission [tech-spec §1.2.1] requires their continued presence.
- **AS400 Isolation Rule:** The `AS400/` branch is read-only. Not a single file in `AS400/` — not `AS400/README.md`, not `AS400/QCLSSRC/DAOP01CL.cl`, not any of the `AS400/COBOL_examples/*` or `AS400/CL_examples/*` directories — may be modified, deleted, or referenced from the Java code. This preserves the IBM i operational manual and the dual-platform separation [tech-spec §1.2.1].
- **GPLv3 Header Rule:** Every new Java source file (`.java`) begins with a GPLv3 license header comment matching the existing repository `LICENSE`.


## 0.2 Scope Boundaries

### 0.2.1 Exhaustively In Scope

The following file paths and path patterns are in scope for creation or modification during this refactor. Every wildcard listed is **trailing** (no leading wildcards used); enumerated files take precedence where stated.

**Source transformations — new Java production classes (CREATE):**
- `cobol-helloworld/src/main/java/com/projectcobol/helloworld/HelloWorldApplication.java`
- `cobol-conditions/src/main/java/com/projectcobol/conditions/*Application.java` — eight classes (`ClassConditionApplication`, `CombinedConditionsApplication`, `ConditionNameConditionApplication`, `ConditionStatementApplication`, `EvaluteVerbApplication`, `NegatedConditionApplication`, `RelationConditionApplication`, `SignConditionApplication`)
- `cobol-database/src/main/java/com/projectcobol/database/*Application.java` — four classes (`OpenFileRecordKeyApplication`, `OpenFileSequentialApplication`, `SequentialReadApplication`, `StatusCodeApplication`)
- `cobol-database/src/main/java/com/projectcobol/database/FileStatus.java` — enum encoding the COBOL `FILE-STATUS` codes [`OpenCobol/Database/StatusCode.cbl:L8-39`]
- `cobol-date/src/main/java/com/projectcobol/date/DateAndTimeApplication.java`
- `cobol-loops/src/main/java/com/projectcobol/loops/*Application.java` — two classes (`ForLoopApplication`, `WhileApplication`)
- `cobol-memory/src/main/java/com/projectcobol/memory/*Application.java` — two classes (`AddressApplication`, `PointerApplication`)
- `cobol-random/src/main/java/com/projectcobol/random/*Application.java` — two classes (`RandomBingoApplication`, `RandomNumbersApplication`)
- `cobol-sqlite/src/main/java/com/projectcobol/sqlite/SqliteApplication.java` — security-fix target
- `cobol-sort/src/main/java/com/projectcobol/sort/*Application.java` — three classes (`BubbleSortApplication`, `InsertSortApplication`, `SelectSortApplication`)
- `cobol-string/src/main/java/com/projectcobol/string/StringApplication.java`
- `cobol-struct/src/main/java/com/projectcobol/struct/StructApplication.java`

**Test updates — new JUnit 5 golden-output tests (CREATE):**
- `cobol-helloworld/src/test/java/com/projectcobol/helloworld/*ApplicationTest.java` — one test class
- `cobol-conditions/src/test/java/com/projectcobol/conditions/*ApplicationTest.java` — eight test classes
- `cobol-database/src/test/java/com/projectcobol/database/*ApplicationTest.java` — four test classes
- `cobol-date/src/test/java/com/projectcobol/date/*ApplicationTest.java` — one test class
- `cobol-loops/src/test/java/com/projectcobol/loops/*ApplicationTest.java` — two test classes
- `cobol-memory/src/test/java/com/projectcobol/memory/*ApplicationTest.java` — two test classes
- `cobol-random/src/test/java/com/projectcobol/random/*ApplicationTest.java` — two test classes (must use seed-injected `java.util.Random` for determinism)
- `cobol-sqlite/src/test/java/com/projectcobol/sqlite/*ApplicationTest.java` — one test class (must use `jdbc:sqlite::memory:` connection; must include a SQL-injection regression assertion that confirms `PreparedStatement` parameter binding neutralizes malicious input)
- `cobol-sort/src/test/java/com/projectcobol/sort/*ApplicationTest.java` — three test classes (seed-injected)
- `cobol-string/src/test/java/com/projectcobol/string/*ApplicationTest.java` — one test class
- `cobol-struct/src/test/java/com/projectcobol/struct/*ApplicationTest.java` — one test class

**Test resources — golden-output expected stdout fixtures (CREATE):**
- `cobol-helloworld/src/test/resources/expected/HelloWorldApplication.txt`
- `cobol-conditions/src/test/resources/expected/*Application.txt` — eight files
- `cobol-database/src/test/resources/expected/*Application.txt` — four files
- `cobol-date/src/test/resources/expected/DateAndTimeApplication.txt`
- `cobol-loops/src/test/resources/expected/*Application.txt` — two files
- `cobol-memory/src/test/resources/expected/*Application.txt` — two files
- `cobol-random/src/test/resources/expected/*Application.txt` — two files
- `cobol-sqlite/src/test/resources/expected/SqliteApplication.txt`
- `cobol-sort/src/test/resources/expected/*Application.txt` — three files
- `cobol-string/src/test/resources/expected/StringApplication.txt`
- `cobol-struct/src/test/resources/expected/StructApplication.txt`

**Main resources — fixture copies (CREATE):**
- `cobol-database/src/main/resources/data.txt` — copy of `OpenCobol/Database/data.txt` (34 fixed-width records) to be loaded via `ClassLoader.getResourceAsStream("data.txt")`

**Configuration updates — Maven build files (CREATE):**
- `pom.xml` (repository root) — parent Maven POM: `<packaging>pom</packaging>`, inherits `spring-boot-starter-parent:3.5.x`, declares `<java.version>21</java.version>`, lists all 11 sub-modules in `<modules>`, configures `maven-compiler-plugin` with `<compilerArgs><arg>-Xlint:all</arg><arg>-Werror</arg></compilerArgs>`
- `cobol-helloworld/pom.xml` — inherits root parent; depends on `spring-boot-starter`, `spring-boot-starter-test` (test scope)
- `cobol-conditions/pom.xml` — same dependency profile
- `cobol-database/pom.xml` — same dependency profile
- `cobol-date/pom.xml` — same dependency profile
- `cobol-loops/pom.xml` — same dependency profile
- `cobol-memory/pom.xml` — same dependency profile
- `cobol-random/pom.xml` — same dependency profile
- `cobol-sqlite/pom.xml` — same dependency profile **plus** `org.xerial:sqlite-jdbc:3.53.1.0`
- `cobol-sort/pom.xml` — same dependency profile
- `cobol-string/pom.xml` — same dependency profile
- `cobol-struct/pom.xml` — same dependency profile
- `.gitignore` (repository root) — CREATE if absent or UPDATE if present; entries for `target/`, `*.class`, `*.jar`, `.idea/`, `.vscode/`, `*.iml`

**Documentation updates:**
- `README.md` (repository root) — UPDATE in append-only mode: new `## Java 21 / Spring Boot Build` section after existing COBOL content [`README.md:L1-56`]. Existing COBOL build instructions and topic catalog remain verbatim.
- `OpenCobol/Games/README.md` — CREATE: explains that `OpenCobol/Games/raylib/*.cbl` is intentionally excluded from Java translation because the raylib C-library binding has no idiomatic Java equivalent and translating it would constitute new feature work rather than refactoring. Points readers to the original `.cbl` files as the authoritative raylib reference and reinforces the dual-platform educational mission [tech-spec §1.2.1].

**Import corrections:**
- Every new Java source file declares `package com.projectcobol.<submodule>;` and imports `org.springframework.boot.SpringApplication`, `org.springframework.boot.CommandLineRunner`, `org.springframework.boot.autoconfigure.SpringBootApplication`. Module-specific imports follow the cross-file dependency rules in section 0.4.2.
- Because all production Java files are CREATE (not UPDATE) and there are no pre-existing Java imports to update, there is no import-rewriting work on existing files.

**Rule-mandated files:**
- None. The user supplied zero implementation rules (`review_rules` returned `[]`). All in-scope files are derived from the prompt itself.

### 0.2.2 Explicitly Out of Scope

The following files and directories **must not be modified, deleted, moved, or renamed** under any circumstance during this refactor:

**Original COBOL sources (preserved as authoritative references):**
- `OpenCobol/HelloWorld.cbl`
- `OpenCobol/Conditions/*.cbl` (all 8 files — `ClassCondition.cbl`, `CombinedConditions.cbl`, `ConditionNameCondition.cbl`, `ConditionStatement.cbl`, `EvaluteVerb.cbl`, `NegatedCondition.cbl`, `RelationCondition.cbl`, `SignCondition.cbl`)
- `OpenCobol/Database/*.cbl` (all 4 files — `OpenFileRecordKey.cbl`, `OpenFileSequential.cbl`, `SequentialRead.cbl`, `StatusCode.cbl`)
- `OpenCobol/Database/data.txt` (original fixture remains at its original location)
- `OpenCobol/Date/DateAndTime.cbl`
- `OpenCobol/Loops/*.cbl` (`ForLoop.cbl`, `While.cbl`)
- `OpenCobol/Memory/*.cbl` (`Address.cbl`, `Pointer.cbl`)
- `OpenCobol/Random/*.cbl` (`RandomBingo.cbl`, `RandomNumbers.cbl`)
- `OpenCobol/SQLite/Hello_SQLITE.cbl` (the **vulnerable** COBOL source remains in place; the security fix lives only in the new Java translation)
- `OpenCobol/Sort/*.cbl` (`BubbleSort.cbl`, `InsertSort.cbl`, `SelectSort.cbl`)
- `OpenCobol/String/String.cbl`
- `OpenCobol/Struct/Struct.cbl`

**Excluded-from-translation COBOL sources (read-only references; no Java equivalent produced):**
- `OpenCobol/Games/raylib/core_basic_window.cbl`
- `OpenCobol/Games/raylib/core_random_values.cbl`

The raylib bindings are excluded because raylib is a C graphics library with no idiomatic Java equivalent; translating it would constitute new feature work, violating the minimal-change clause. The new `OpenCobol/Games/README.md` documents this rationale.

**AS400/ branch — entirely out of scope:**
- `AS400/README.md` (operational manual for IBM i source migration — `CRTSRCPF`, `STRSEU`, FTP workflow)
- `AS400/CL_examples/Loops/**` (F-013 Proposed)
- `AS400/CL_examples/Messages/**` (F-014 Proposed)
- `AS400/COBOL_examples/CallingExample/**` (F-015 Proposed — references `NESTEDCALL.CBBLE`, `CALLER.CBLLE`, `HERON.CBLLE`)
- `AS400/COBOL_examples/Logging/**` (F-016 Completed — `LOG0010CB` COBOL logger + `GETJOBA1CL.cl` helper)
- `AS400/COBOL_examples/MoveClause/**` (F-017 Proposed)
- `AS400/COBOL_examples/PictureClause/**` (F-018 Proposed)
- `AS400/QCLSSRC/**` (F-019 Completed — `DAOP01CL.cl` 4-gate batch DAO gatekeeper, 58 lines)

The AS400/ branch is preserved verbatim to maintain the dual-platform educational mission and the IBM i migration workflow [tech-spec §1.2.1].

**Licensing artifact:**
- `LICENSE` (GPLv3 verbatim at the repository root) — must not be touched. The new Java code inherits this license via GPLv3 header comments on every `.java` file.

**Behavioral changes not permitted:**
- The COBOL programs themselves are not modified — neither to fix the SQL injection in `Hello_SQLITE.cbl`, nor to add error handling, nor to alter output formatting. All behavioral preservation is achieved by the Java translation matching the COBOL output exactly.
- No new business logic, no performance optimization, no API surface beyond what each `.cbl` defines.
- No CI/CD configuration (`.github/workflows/*.yml`, `.gitlab-ci.yml`, etc.) — the prompt does not mandate CI/CD, and the minimal-change clause forbids speculative additions.
- No Dockerfile, no Kubernetes manifests, no deployment descriptors — the prompt scopes the build to `mvn clean package` and run-via-`java -jar`.

**Design system / UI library:**
- Not applicable. The prompt does not specify a UI component library or design system; all Java output is text via `System.out.println` mirroring COBOL `DISPLAY` behavior. The DESIGN SYSTEM ALIGNMENT PROTOCOL is therefore skipped.


## 0.3 Target Design

### 0.3.1 Refactored Structure Planning

The target repository layout after refactor is a Maven multi-module project rooted at the existing repository top-level. The original `OpenCobol/`, `AS400/`, `LICENSE`, and `README.md` artifacts remain in place — the new Java sub-modules are added alongside them, not in place of them.

```
Cobol/
├── LICENSE                                              # UNCHANGED - GPLv3 verbatim
├── README.md                                            # APPENDED with Java 21 / Spring Boot build section
├── pom.xml                                              # NEW - parent Maven POM
├── .gitignore                                           # NEW (or UPDATED if pre-existing)
│
├── AS400/                                               # UNTOUCHED in its entirety
│   ├── README.md
│   ├── CL_examples/
│   ├── COBOL_examples/
│   └── QCLSSRC/
│
├── OpenCobol/                                           # UNTOUCHED - originals remain as reference
│   ├── HelloWorld.cbl
│   ├── Conditions/             ClassCondition.cbl, CombinedConditions.cbl,
│   │                           ConditionNameCondition.cbl, ConditionStatement.cbl,
│   │                           EvaluteVerb.cbl, NegatedCondition.cbl,
│   │                           RelationCondition.cbl, SignCondition.cbl
│   ├── Database/               OpenFileRecordKey.cbl, OpenFileSequential.cbl,
│   │                           SequentialRead.cbl, StatusCode.cbl, data.txt
│   ├── Date/                   DateAndTime.cbl
│   ├── Games/
│   │   ├── README.md           # NEW - documents raylib exclusion rationale
│   │   └── raylib/             core_basic_window.cbl, core_random_values.cbl
│   │                                                   # EXCLUDED FROM JAVA TRANSLATION
│   ├── Loops/                  ForLoop.cbl, While.cbl
│   ├── Memory/                 Address.cbl, Pointer.cbl
│   ├── Random/                 RandomBingo.cbl, RandomNumbers.cbl
│   ├── SQLite/                 Hello_SQLITE.cbl
│   ├── Sort/                   BubbleSort.cbl, InsertSort.cbl, SelectSort.cbl
│   ├── String/                 String.cbl
│   └── Struct/                 Struct.cbl
│
├── cobol-helloworld/                                    # NEW Maven sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/projectcobol/helloworld/
│       │   └── HelloWorldApplication.java
│       └── test/
│           ├── java/com/projectcobol/helloworld/
│           │   └── HelloWorldApplicationTest.java
│           └── resources/expected/
│               └── HelloWorldApplication.txt
│
├── cobol-conditions/                                    # NEW Maven sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/projectcobol/conditions/
│       │   ├── ClassConditionApplication.java
│       │   ├── CombinedConditionsApplication.java
│       │   ├── ConditionNameConditionApplication.java
│       │   ├── ConditionStatementApplication.java
│       │   ├── EvaluteVerbApplication.java
│       │   ├── NegatedConditionApplication.java
│       │   ├── RelationConditionApplication.java
│       │   └── SignConditionApplication.java
│       └── test/
│           ├── java/com/projectcobol/conditions/        # 8 *Test.java classes
│           └── resources/expected/                      # 8 *.txt golden files
│
├── cobol-database/                                      # NEW Maven sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/projectcobol/database/
│       │   │   ├── OpenFileRecordKeyApplication.java
│       │   │   ├── OpenFileSequentialApplication.java
│       │   │   ├── SequentialReadApplication.java
│       │   │   ├── StatusCodeApplication.java
│       │   │   └── FileStatus.java                      # Enum encoding COBOL FILE-STATUS codes
│       │   └── resources/
│       │       └── data.txt                             # Fixture copy for classpath loading
│       └── test/
│           ├── java/com/projectcobol/database/          # 4 *Test.java classes
│           └── resources/expected/                      # 4 *.txt golden files
│
├── cobol-date/                                          # NEW Maven sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/projectcobol/date/
│       │   └── DateAndTimeApplication.java
│       └── test/
│           ├── java/com/projectcobol/date/
│           │   └── DateAndTimeApplicationTest.java
│           └── resources/expected/
│               └── DateAndTimeApplication.txt
│
├── cobol-loops/                                         # NEW Maven sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/projectcobol/loops/
│       │   ├── ForLoopApplication.java
│       │   └── WhileApplication.java
│       └── test/
│           ├── java/com/projectcobol/loops/             # 2 *Test.java
│           └── resources/expected/                      # 2 *.txt
│
├── cobol-memory/                                        # NEW Maven sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/projectcobol/memory/
│       │   ├── AddressApplication.java
│       │   └── PointerApplication.java
│       └── test/
│           ├── java/com/projectcobol/memory/            # 2 *Test.java
│           └── resources/expected/                      # 2 *.txt
│
├── cobol-random/                                        # NEW Maven sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/projectcobol/random/
│       │   ├── RandomBingoApplication.java
│       │   └── RandomNumbersApplication.java
│       └── test/
│           ├── java/com/projectcobol/random/            # 2 *Test.java (seed-injected)
│           └── resources/expected/                      # 2 *.txt
│
├── cobol-sqlite/                                        # NEW Maven sub-module - SECURITY FIX target
│   ├── pom.xml                                          # depends on sqlite-jdbc:3.53.1.0
│   └── src/
│       ├── main/java/com/projectcobol/sqlite/
│       │   └── SqliteApplication.java                   # uses PreparedStatement (SQL injection FIX)
│       └── test/
│           ├── java/com/projectcobol/sqlite/
│           │   └── SqliteApplicationTest.java           # SQL injection regression test
│           └── resources/expected/
│               └── SqliteApplication.txt
│
├── cobol-sort/                                          # NEW Maven sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/projectcobol/sort/
│       │   ├── BubbleSortApplication.java
│       │   ├── InsertSortApplication.java
│       │   └── SelectSortApplication.java
│       └── test/
│           ├── java/com/projectcobol/sort/              # 3 *Test.java (seed-injected)
│           └── resources/expected/                      # 3 *.txt
│
├── cobol-string/                                        # NEW Maven sub-module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/projectcobol/string/
│       │   └── StringApplication.java
│       └── test/
│           ├── java/com/projectcobol/string/
│           │   └── StringApplicationTest.java
│           └── resources/expected/
│               └── StringApplication.txt
│
└── cobol-struct/                                        # NEW Maven sub-module
    ├── pom.xml
    └── src/
        ├── main/java/com/projectcobol/struct/
        │   └── StructApplication.java
        └── test/
            ├── java/com/projectcobol/struct/
            │   └── StructApplicationTest.java
            └── resources/expected/
                └── StructApplication.txt
```

**Totals:** 11 new Maven sub-modules, 26 new main Java sources (25 entry classes + 1 `FileStatus` enum), 25 new test classes, 25 new golden-output text fixtures, 1 fixture resource copy (`data.txt`), 12 new POM files (1 parent + 11 children), 1 new `.gitignore`, 1 new `OpenCobol/Games/README.md`, and 1 appended `README.md` — **92 file operations** in total.

### 0.3.2 Web Search Research Conducted

The following research was executed to pin precise, current dependency versions before defining the build configuration:

- **Best practices for COBOL → Java tech-stack migration** — confirmed the Spring Boot `CommandLineRunner` pattern as the idiomatic equivalent of COBOL's `PROCEDURE DIVISION` → `MAIN-PROCEDURE` → `GOBACK` lifecycle for batch-style procedural programs.
- **Spring Boot 3.x release-train selection** — verified that Spring Boot 3.5.x is the recommended branch for Java 17+ projects that need Spring Framework 6, supports Java 21 as a first-class runtime, and is the final 3.x minor line before Spring Boot 4.0 (sources: `https://github.com/spring-projects/spring-boot/releases`, `https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes`).
- **`sqlite-jdbc` version selection** — verified the latest stable release as `org.xerial:sqlite-jdbc:3.53.1.0`, including Apache 2.0 licensing (GPLv3-compatible), bundled native libraries for Windows, macOS, Linux, and FreeBSD (no platform-specific configuration needed), and JDBC URL form `jdbc:sqlite:<path>` (sources: `https://github.com/xerial/sqlite-jdbc`, `https://central.sonatype.com/artifact/org.xerial/sqlite-jdbc`).
- **JUnit 5 release-train selection** — confirmed JUnit Jupiter 5.12.x is BOM-managed by `spring-boot-starter-parent:3.5.x` (sources: Spring Boot 3.5 release notes; JUnit 5 release notes at `https://docs.junit.org/`).
- **Migration strategies for `ocsqlite` text-substitution → JDBC `PreparedStatement`** — confirmed parameter binding as the canonical OWASP-recommended remediation for SQL injection in the pattern documented at [tech-spec §2.4.4, §5.3.5, §6.2.3.1].
- **Tools and techniques for safe refactoring** — adopted golden-output testing (capture `System.out`, compare to pre-recorded expected fixture) as the lowest-risk behavioral-preservation verification strategy for COBOL → Java translation of single-process console programs.

### 0.3.3 Design Pattern Applications

The following patterns are applied across the Java sub-modules. Patterns appear only where they earn their weight; the prompt's minimal-change clause forbids speculative pattern adoption.

- **Spring Boot `CommandLineRunner` pattern** — applied to **all 25 entry classes**. Each entry class is annotated `@SpringBootApplication`, implements `CommandLineRunner`, exposes a `public static void main(String[] args)` that calls `SpringApplication.run(<Class>.class, args)`, and overrides `public void run(String... args) throws Exception { /* translated COBOL PROCEDURE DIVISION */ }`. The original COBOL `PERFORM <section-name>` calls become `private void` methods invoked from `run(...)`.

- **JDBC `PreparedStatement` pattern** — applied to `cobol-sqlite/.../SqliteApplication.java` **exclusively**. This is the structural security fix that replaces `ocsqlite`'s text-substitution `CALL "ocsqlite" using by value db callback-proc by reference zquery ...` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L296-304`] with parameterized `Connection.prepareStatement(sql)` followed by `ps.setString(1, userInput)` and `ps.executeQuery()`.

- **JDBC `ResultSet` iteration pattern** — applied to `cobol-sqlite/.../SqliteApplication.java` **exclusively**. Replaces the `ocsqlite` callback architecture (procedure-pointer `callback-proc` bound via `set callback-proc to entry "callback"` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L152`] feeding a 20-slot `sql-table external` row buffer [`OpenCobol/SQLite/Hello_SQLITE.cbl:L102-104`]) with idiomatic `while (rs.next()) { ... }` iteration.

- **`BufferedReader` + try-with-resources pattern** — applied to all four `cobol-database/.../*Application.java` classes. Replaces COBOL `FILE-CONTROL`/`SELECT`/`OPEN INPUT`/`READ`/`CLOSE` with `try (BufferedReader br = Files.newBufferedReader(path)) { ... }`. Fixed-width record extraction uses `String.substring(start, end)` to mirror COBOL field offsets.

- **`enum` for COBOL 88-level condition tables** — applied to `cobol-database/.../FileStatus.java`. Encodes all 30 COBOL `FILE-STATUS` 88-level entries from `OpenCobol/Database/StatusCode.cbl` [`OpenCobol/Database/StatusCode.cbl:L10-39`] as a Java enum with String code, `fromCode(String)` factory, and Javadoc per constant.

- **Deterministic seeding pattern** — applied to `cobol-random/.../*Application.java` and `cobol-sort/.../*Application.java`. Production runs use `java.util.Random` seeded with `System.currentTimeMillis() % 86400` to mirror COBOL `FUNCTION SECONDS-PAST-MIDNIGHT` seeding. Tests inject a fixed `long` seed (via package-private constructor or Spring `@Value` property) so golden-output fixtures remain stable.

- **String-substring view pattern (Memory sub-module)** — applied to `cobol-memory/.../AddressApplication.java` and `PointerApplication.java`. Models the COBOL `LINKAGE SECTION` overlay (`SET ADDRESS OF WORK-DATA TO WK-PTR.` [`OpenCobol/Memory/Address.cbl:L28`]) as `String.substring` views over a 16-character backing buffer. Java's lack of raw pointers is bridged with explanatory inline comments mapping each COBOL pointer manipulation to its Java equivalent.

**Patterns intentionally NOT adopted:**

- **No `@Autowired` services or repositories.** Each sub-module's main class is self-contained; introducing service beans purely for testability would constitute speculative complexity and violate the minimal-change clause.
- **No Repository / DAO layer for `cobol-database`.** The four database classes are intentionally low-level demonstrations of file I/O; abstracting them behind a repository interface would obscure the pedagogical mapping.
- **No Factory pattern.** No object-construction polymorphism is needed; the COBOL programs are flat single-entry programs.
- **No Spring `@Profile`-based dependency injection (except in `cobol-sqlite` for in-memory vs file-backed SQLite if needed for test isolation).** All other sub-modules run identically in production and test.

### 0.3.4 User Interface Design

Not applicable. The original COBOL programs are console applications that emit text via `DISPLAY` and read terminal input via `ACCEPT`. The Java translations preserve this exact interaction model: output goes to `System.out.println`, interactive input (where present) reads from `java.util.Scanner` over `System.in`, and no GUI, web, or terminal-graphics layer is introduced. The `OpenCobol/Games/raylib/*.cbl` programs — the only COBOL sources that drive a graphical interface — are explicitly excluded from translation per section 0.2.2, so no Java graphics framework is required by this refactor.


## 0.4 Transformation Mapping

### 0.4.1 File-by-File Transformation Plan

Every target file is mapped to its source file (or marked source-less when none exists). Mode legend: **CREATE** = a brand-new file is added; **UPDATE** = an existing file is modified in place; **REFERENCE** = the file is read as a behavioral specification but never modified.

**Repository root (3 files):**

| Target File | Transformation | Source File | Key Changes |
|------------|---------------|-------------|-------------|
| `pom.xml` | CREATE | (none) | Parent Maven POM. `<packaging>pom</packaging>`; parent = `org.springframework.boot:spring-boot-starter-parent:3.5.9` (or latest 3.5.x); `<java.version>21</java.version>`; `<modules>` lists all 11 sub-modules; `maven-compiler-plugin` configured with `<compilerArgs><arg>-Xlint:all</arg><arg>-Werror</arg></compilerArgs>`. |
| `README.md` | UPDATE | `README.md` | **APPEND-ONLY.** Existing COBOL build instructions and topic catalog [`README.md:L1-56`] are preserved verbatim. A new `## Java 21 / Spring Boot Build` section is appended documenting `mvn clean package -DskipTests`, `java -jar cobol-helloworld/target/cobol-helloworld-1.0.0.jar`, and `mvn test`. |
| `.gitignore` | CREATE | (none) | New file at repository root. Entries: `target/`, `*.class`, `*.jar`, `.idea/`, `.vscode/`, `*.iml`. If a pre-existing `.gitignore` is discovered at refactor time, this becomes UPDATE in append-only mode. |

**Sub-module POM files (11 files, all CREATE):**

| Target File | Transformation | Source File | Key Changes |
|------------|---------------|-------------|-------------|
| `cobol-helloworld/pom.xml` | CREATE | (none) | `<parent>` references root `cobol-parent`; depends on `spring-boot-starter` and `spring-boot-starter-test` (test scope); `spring-boot-maven-plugin` repackages a runnable JAR. |
| `cobol-conditions/pom.xml` | CREATE | (none) | Same profile as helloworld; eight main classes share the same JAR but require explicit `-Dstart-class=<FQCN>` at run time (documented in this sub-module's POM via a placeholder). |
| `cobol-database/pom.xml` | CREATE | (none) | Same profile as helloworld; four main classes; `src/main/resources` packaged into the JAR for `data.txt` classpath access. |
| `cobol-date/pom.xml` | CREATE | (none) | Same profile as helloworld. |
| `cobol-loops/pom.xml` | CREATE | (none) | Same profile as helloworld. |
| `cobol-memory/pom.xml` | CREATE | (none) | Same profile as helloworld. |
| `cobol-random/pom.xml` | CREATE | (none) | Same profile as helloworld. |
| `cobol-sqlite/pom.xml` | CREATE | (none) | Same profile as helloworld **plus** `<dependency><groupId>org.xerial</groupId><artifactId>sqlite-jdbc</artifactId><version>3.53.1.0</version></dependency>`. |
| `cobol-sort/pom.xml` | CREATE | (none) | Same profile as helloworld; three main classes share the JAR. |
| `cobol-string/pom.xml` | CREATE | (none) | Same profile as helloworld. |
| `cobol-struct/pom.xml` | CREATE | (none) | Same profile as helloworld. |

**Production Java entry classes (25 files, all CREATE) + 1 FileStatus enum:**

| Target File | Transformation | Source File | Key Changes |
|------------|---------------|-------------|-------------|
| `cobol-helloworld/src/main/java/com/projectcobol/helloworld/HelloWorldApplication.java` | CREATE | `OpenCobol/HelloWorld.cbl` | `@SpringBootApplication` + `CommandLineRunner`; `run()` invokes `System.out.println("Hello world!")` matching the single `DISPLAY "Hello world!"` and `STOP RUN.` lifecycle [`OpenCobol/HelloWorld.cbl:L1-21`]. |
| `cobol-conditions/src/main/java/com/projectcobol/conditions/ClassConditionApplication.java` | CREATE | `OpenCobol/Conditions/ClassCondition.cbl` | Translate `PIC S9(9) NUM01 VALUE -5000` → `int num01 = -5000`; `PIC X(9) STR01 VALUE "ABCDF"` → `String str01 = "ABCDF "` (right-padded); `ALPHABETIC` / `NUMERIC` tests → `str.chars().allMatch(Character::isAlphabetic)` / `Character::isDigit`. |
| `cobol-conditions/src/main/java/com/projectcobol/conditions/CombinedConditionsApplication.java` | CREATE | `OpenCobol/Conditions/CombinedConditions.cbl` | Translate `IF NUM01 < NUM02 AND NUM01 = NUM03` → Java `if (num01 < num02 && num01 == num03)`. |
| `cobol-conditions/src/main/java/com/projectcobol/conditions/ConditionNameConditionApplication.java` | CREATE | `OpenCobol/Conditions/ConditionNameCondition.cbl` | Translate `88 M_TRUE VALUE 30 THRU 100` / `88 M_FALSE VALUE 000 THRU 40` → private `boolean isMTrue(int n)` / `boolean isMFalse(int n)` predicate methods. |
| `cobol-conditions/src/main/java/com/projectcobol/conditions/ConditionStatementApplication.java` | CREATE | `OpenCobol/Conditions/ConditionStatement.cbl` | Translate nested `IF/ELSE` on `NUM01..NUM04` = 5, 6, 7, 8 to Java nested `if/else`. |
| `cobol-conditions/src/main/java/com/projectcobol/conditions/EvaluteVerbApplication.java` | CREATE | `OpenCobol/Conditions/EvaluteVerb.cbl` | Translate `EVALUATE TRUE WHEN ... WHEN OTHER` → Java 21 switch expression with pattern matching where natural; spelling of `Evalute` preserved to match the source filename. |
| `cobol-conditions/src/main/java/com/projectcobol/conditions/NegatedConditionApplication.java` | CREATE | `OpenCobol/Conditions/NegatedCondition.cbl` | Translate `IF NOT NUM01 IS LESS THAN NUM02` → Java `if (!(num01 < num02))`. |
| `cobol-conditions/src/main/java/com/projectcobol/conditions/RelationConditionApplication.java` | CREATE | `OpenCobol/Conditions/RelationCondition.cbl` | Translate `IF NUM01 IS GREATER THAN OR EQUAL TO NUM02` → Java `if (num01 >= num02)`. |
| `cobol-conditions/src/main/java/com/projectcobol/conditions/SignConditionApplication.java` | CREATE | `OpenCobol/Conditions/SignCondition.cbl` | Translate `IS POSITIVE` / `IS NEGATIVE` / `IS ZERO` → direct numeric comparison (`n > 0`, `n < 0`, `n == 0`). |
| `cobol-database/src/main/java/com/projectcobol/database/OpenFileRecordKeyApplication.java` | CREATE | `OpenCobol/Database/OpenFileRecordKey.cbl` | Translate `FILE-CONTROL SELECT ... ASSIGN TO "../data.dat" ORGANIZATION IS INDEXED` → `BufferedReader` over classpath-loaded `data.txt`; `MY-DATA-ID/NAME/TIME` record fields → `String.substring` extraction; `ERROR-RESULT` group → instance fields; `FILE-STATUS` checks → `FileStatus.fromCode(status)`. |
| `cobol-database/src/main/java/com/projectcobol/database/OpenFileSequentialApplication.java` | CREATE | `OpenCobol/Database/OpenFileSequential.cbl` | Translate sequential `READ ... AT END SET EOF-T TO TRUE` → `String line; while ((line = br.readLine()) != null) { ... }`; `DET-ID/TIME/NUM` record fields → `String.substring`. |
| `cobol-database/src/main/java/com/projectcobol/database/SequentialReadApplication.java` | CREATE | `OpenCobol/Database/SequentialRead.cbl` | Translate `OPEN INPUT` of `../database.dat` → `BufferedReader`; `DETAILS-ID/SURNAME/INITIALS/BIRTHDAY/SOME-CODE` → fixed-offset `substring` extractions. |
| `cobol-database/src/main/java/com/projectcobol/database/StatusCodeApplication.java` | CREATE | `OpenCobol/Database/StatusCode.cbl` | Translate `MAIN-PROCEDURE DISPLAY "Hello world" GOBACK` → `run()` calls `System.out.println("Hello world")` [`OpenCobol/Database/StatusCode.cbl:L41-44`]. The 30 `88-level` declarations [`OpenCobol/Database/StatusCode.cbl:L10-39`] are extracted into the separate `FileStatus.java` enum. |
| `cobol-database/src/main/java/com/projectcobol/database/FileStatus.java` | CREATE | `OpenCobol/Database/StatusCode.cbl` | New enum with one constant per `88-level` declaration: `SUCCESS("00")`, `SUCCESS_DUPLICATE("02")`, `SUCCESS_INCOMPLETE("04")`, `SUCCESS_OPTIONAL("05")`, `SUCCESS_NO_UNIT("07")`, `END_OF_FILE("10")`, `OUT_OF_KEY_RANGE("14")`, `KEY_INVALID("21")`, `KEY_EXISTS("22")`, `KEY_NOT_EXISTS("23")`, `PERMANENT_ERROR("30")`, `INCONSISTENT_FILENAME("31")`, `BOUNDARY_VIOLATION("34")`, `NOT_EXISTS("35")`, `PERMISSION_DENIED("37")`, `CLOSED_WITH_LOCK("38")`, `CONFLICT_ATTRIBUTE("39")`, `ALREADY_OPEN("41")`, `NOT_OPEN("42")`, `READ_NOT_DONE("43")`, `RECORD_OVERFLOW("44")`, `READ_ERROR("46")`, `INPUT_DENIED("47")`, `OUTPUT_DENIED("48")`, `I_O_DENIED("49")`, `RECORD_LOCKED("51")`, `END_OF_PAGE("52")`, `I_O_LINAGE("57")`, `FILE_SHARING("61")`, `NOT_AVAILABLE("91")`, `UNKNOWN("")`. Static `FileStatus fromCode(String)` factory method [`OpenCobol/Database/StatusCode.cbl:L10-39`, tech-spec §6.2.3.2]. |
| `cobol-database/src/main/resources/data.txt` | CREATE | `OpenCobol/Database/data.txt` | Byte-for-byte copy of the 34-record fixture so the Java programs load it from the classpath via `ClassLoader.getResourceAsStream("data.txt")` rather than the COBOL relative path `../data.dat`. |
| `cobol-date/src/main/java/com/projectcobol/date/DateAndTimeApplication.java` | CREATE | `OpenCobol/Date/DateAndTime.cbl` | Translate `ACCEPT W-TIME FROM TIME` → `LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmssSS"))`; `ACCEPT W-DATE FROM DATE` → `LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))`; `STRING W-DATE W-TIME DELIMITED BY " " INTO W-BATCH-COMPLET` → `String.join(" ", date, time)`. |
| `cobol-loops/src/main/java/com/projectcobol/loops/ForLoopApplication.java` | CREATE | `OpenCobol/Loops/ForLoop.cbl` | Translate `PERFORM UNTIL W-I = 11` and nested counters → nested `for (int i = 1; i <= 10; i++)` loops; `COMPUTE W-RESULT = W-I * W-J` → `result = i * j`. |
| `cobol-loops/src/main/java/com/projectcobol/loops/WhileApplication.java` | CREATE | `OpenCobol/Loops/While.cbl` | Translate `W-I PIC 99 VALUE 20` / `PERFORM UNTIL W-I <= 0 ... COMPUTE W-I = W-I - 1` → `int i = 20; while (i > 0) { ... i--; }`. |
| `cobol-memory/src/main/java/com/projectcobol/memory/AddressApplication.java` | CREATE | `OpenCobol/Memory/Address.cbl` | Translate `01 WORK-AREA` group with four `PIC X(4)` elementaries [`OpenCobol/Memory/Address.cbl:L9-13`] → backing `String workArea = "AAAA" + "BBBB" + "CCCC" + "DDDF"`; `01 WK-PTR POINTER` [`OpenCobol/Memory/Address.cbl:L15`] → simulated `int wkPtr` offset; `LINKAGE SECTION WORK-DATA` [`OpenCobol/Memory/Address.cbl:L17-20`] → `String workA`, `nextWorkData` view fields; `SET WK-PTR TO ADDRESS OF WORK-AREA` [`OpenCobol/Memory/Address.cbl:L27`] → `wkPtr = 0`; `SET ADDRESS OF WORK-DATA TO WK-PTR` [`OpenCobol/Memory/Address.cbl:L28`] → `workA = workArea.substring(wkPtr, wkPtr + 4); nextWorkData = workArea.substring(wkPtr + 4, wkPtr + 8)`. Inline comments document the COBOL→Java pointer semantic mapping. |
| `cobol-memory/src/main/java/com/projectcobol/memory/PointerApplication.java` | CREATE | `OpenCobol/Memory/Pointer.cbl` | Same pattern as `AddressApplication` with field names `AREA-A..AREA-D` and `W-POINTER` preserved from the source [`OpenCobol/Memory/Pointer.cbl:L10-29`]. |
| `cobol-random/src/main/java/com/projectcobol/random/RandomBingoApplication.java` | CREATE | `OpenCobol/Random/RandomBingo.cbl` | Translate `01 W-LEN-ARR VALUE 100` → `static final int W_LEN_ARR = 100`; `01 W-ARR PIC 999 OCCURS 100 TIMES` → `int[] arr = new int[100]`; `COMPUTE SEED = FUNCTION SECONDS-PAST-MIDNIGHT` → `Random random = new Random(System.currentTimeMillis() % 86400)`; `INIT-SEED` / `GENERATE-NUMBERS` / `PRINT-NUMBER` sections → private methods (200-iteration warm-up loop preserved). |
| `cobol-random/src/main/java/com/projectcobol/random/RandomNumbersApplication.java` | CREATE | `OpenCobol/Random/RandomNumbers.cbl` | Translate `PERFORM 10 TIMES COMPUTE W-RESULT = (FUNCTION RANDOM * 100) + 1` → `for (int i = 0; i < 10; i++) System.out.println(random.nextInt(100) + 1)`. |
| `cobol-sqlite/src/main/java/com/projectcobol/sqlite/SqliteApplication.java` | CREATE | `OpenCobol/SQLite/Hello_SQLITE.cbl` | **THE SECURITY FIX.** Replace `call "ocsqlite_init" using db, database, errstr` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L137-143`] with `DriverManager.getConnection("jdbc:sqlite:test.db")`. Replace `call "ocsqlite" using by value db callback-proc by reference zquery ...` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L296-304`] with `PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()`. Replace the `callback` sub-program [`OpenCobol/SQLite/Hello_SQLITE.cbl:L333-396`] with `while (rs.next()) { ... }`. Preserve the schema `CREATE TABLE trial (first INTEGER PRIMARY KEY, second char(20), third date)` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L191-193`] and the INSERT with `lower(hex(randomblob(20)))`, `datetime()`, `julianday()` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L204-208`] verbatim as `PreparedStatement` SQL. Bind `key-field` user input via `ps.setInt(1, keyField)` instead of `SCREEN SECTION ACCEPT` text substitution [tech-spec §2.4.4, §5.3.5, §6.2.3.1]. |
| `cobol-sort/src/main/java/com/projectcobol/sort/BubbleSortApplication.java` | CREATE | `OpenCobol/Sort/BubbleSort.cbl` | Translate `W-LEN-ARR=10`, `PIC 99` array, `INIT-SEED` → `GENERATE-RANDOM-NUM` → `SORTING-ARRAY` (nested compare/swap via `W-TEMP`) → Java bubble sort on `int[10]`. |
| `cobol-sort/src/main/java/com/projectcobol/sort/InsertSortApplication.java` | CREATE | `OpenCobol/Sort/InsertSort.cbl` | Translate `W-LEN-ARR=20`, `PIC 999` array, `W-KEY`-based insertion logic → Java insertion sort on `int[20]`. |
| `cobol-sort/src/main/java/com/projectcobol/sort/SelectSortApplication.java` | CREATE | `OpenCobol/Sort/SelectSort.cbl` | Translate `W-LEN-ARR=30`, `W-MIN-NUMBER=1`, `W-MAX-NUMBER=200`, `W-SWAP` selection logic → Java selection sort on `int[30]`. |
| `cobol-string/src/main/java/com/projectcobol/string/StringApplication.java` | CREATE | `OpenCobol/String/String.cbl` | Translate `W-COUNT PIC 999`, `W-STRING PIC X(10) VALUE "HOHOHOHOHO"`, `PERFORM VARYING W-COUNT FROM 1 BY 1 UNTIL W-COUNT > 10 DISPLAY W-STRING(W-COUNT:1)` → `for (int i = 0; i < 10; i++) System.out.println(wString.substring(i, i + 1))`. Note the 1-based-to-0-based offset translation. |
| `cobol-struct/src/main/java/com/projectcobol/struct/StructApplication.java` | CREATE | `OpenCobol/Struct/Struct.cbl` | Translate `78-level W-LEN-ARR1 VALUE 5` / `W-LEN-ARR2 VALUE 10` → `static final int` constants; `W-STRUCT` group containing `W-ARRAY OCCURS 5 PIC S99` / `W-ARRAY2 OCCURS 10 PIC S99` → static nested class or `record` with `int[] arr1` / `int[] arr2`; `ARRAY-ONE` / `ARRAY-TWO` sections → private methods. |

**Test classes (25 files, all CREATE):**

Each `<EntryClass>Test.java` follows the same JUnit 5 golden-output pattern: capture `System.out`, invoke the entry class's `run(...)` method (or `SpringApplication.run(...)` with output capture for full Spring context tests), load `src/test/resources/expected/<EntryClass>.txt`, and assert equality.

| Target File | Transformation | Source File | Key Changes |
|------------|---------------|-------------|-------------|
| `cobol-helloworld/src/test/java/com/projectcobol/helloworld/HelloWorldApplicationTest.java` | CREATE | (none) | Golden-output test for `HelloWorldApplication`. |
| `cobol-conditions/src/test/java/com/projectcobol/conditions/*ApplicationTest.java` | CREATE | (none) | 8 golden-output tests (one per Conditions entry class). |
| `cobol-database/src/test/java/com/projectcobol/database/*ApplicationTest.java` | CREATE | (none) | 4 golden-output tests; each loads `data.txt` from classpath via `getResourceAsStream`. |
| `cobol-date/src/test/java/com/projectcobol/date/DateAndTimeApplicationTest.java` | CREATE | (none) | Uses regex assertion (`yyyyMMdd HHmmssSS`) rather than literal equality because date/time output is environment-dependent. |
| `cobol-loops/src/test/java/com/projectcobol/loops/*ApplicationTest.java` | CREATE | (none) | 2 golden-output tests (deterministic loop output). |
| `cobol-memory/src/test/java/com/projectcobol/memory/*ApplicationTest.java` | CREATE | (none) | 2 tests; pointer-display lines use placeholder format match rather than literal equality. |
| `cobol-random/src/test/java/com/projectcobol/random/*ApplicationTest.java` | CREATE | (none) | 2 tests; seed-injected `Random` produces deterministic golden output. |
| `cobol-sqlite/src/test/java/com/projectcobol/sqlite/SqliteApplicationTest.java` | CREATE | (none) | Uses `jdbc:sqlite::memory:` connection. Includes a dedicated `@Test injectionAttemptIsNeutralized()` that passes `'; DROP TABLE trial; --` as the `keyField` input and asserts (a) no exception, (b) `trial` table still exists in the in-memory DB, (c) the literal string is bound as a parameter rather than executed. |
| `cobol-sort/src/test/java/com/projectcobol/sort/*ApplicationTest.java` | CREATE | (none) | 3 tests; seed-injected `Random` produces deterministic golden output. |
| `cobol-string/src/test/java/com/projectcobol/string/StringApplicationTest.java` | CREATE | (none) | Golden-output test asserting ten lines of alternating `H`, `O`. |
| `cobol-struct/src/test/java/com/projectcobol/struct/StructApplicationTest.java` | CREATE | (none) | Golden-output test asserting `Array1 contains number:` and `Array2 contains number:` lines. |

**Golden-output text fixtures (25 files, all CREATE):**

Each file is a hand-curated capture of the original COBOL program's `cobc`-compiled stdout (or a deterministic equivalent for seeded random programs / mocked clock programs). Path pattern: `cobol-<topic>/src/test/resources/expected/<EntryClass>.txt`.

| Target File | Source File (used as behavioral reference) |
|------------|--------------------------------------------|
| `cobol-helloworld/src/test/resources/expected/HelloWorldApplication.txt` | `OpenCobol/HelloWorld.cbl` (literal: `Hello world!`) |
| `cobol-conditions/src/test/resources/expected/*Application.txt` (8 files) | `OpenCobol/Conditions/*.cbl` |
| `cobol-database/src/test/resources/expected/*Application.txt` (4 files) | `OpenCobol/Database/*.cbl` + `OpenCobol/Database/data.txt` |
| `cobol-date/src/test/resources/expected/DateAndTimeApplication.txt` | `OpenCobol/Date/DateAndTime.cbl` (regex pattern, not literal) |
| `cobol-loops/src/test/resources/expected/*Application.txt` (2 files) | `OpenCobol/Loops/*.cbl` |
| `cobol-memory/src/test/resources/expected/*Application.txt` (2 files) | `OpenCobol/Memory/*.cbl` (pointer lines use format match) |
| `cobol-random/src/test/resources/expected/*Application.txt` (2 files) | `OpenCobol/Random/*.cbl` (run with fixed seed) |
| `cobol-sqlite/src/test/resources/expected/SqliteApplication.txt` | `OpenCobol/SQLite/Hello_SQLITE.cbl` (banner + ResultSet output) |
| `cobol-sort/src/test/resources/expected/*Application.txt` (3 files) | `OpenCobol/Sort/*.cbl` (run with fixed seed) |
| `cobol-string/src/test/resources/expected/StringApplication.txt` | `OpenCobol/String/String.cbl` |
| `cobol-struct/src/test/resources/expected/StructApplication.txt` | `OpenCobol/Struct/Struct.cbl` |

**Documentation additions (1 file, CREATE):**

| Target File | Transformation | Source File | Key Changes |
|------------|---------------|-------------|-------------|
| `OpenCobol/Games/README.md` | CREATE | (none — explanatory rather than translated) | NEW documentation file explaining that `OpenCobol/Games/raylib/core_basic_window.cbl` and `core_random_values.cbl` are intentionally excluded from Java translation because raylib is a C graphics library with no idiomatic Java equivalent and translating it would constitute new feature work. Cross-references the dual-platform educational mission [tech-spec §1.2.1] and notes that the original `.cbl` files remain in place as the authoritative raylib reference. |

**REFERENCE files (read but never modified):**

The following files are read by the implementation agent to understand existing semantics but are **never** written to:

- `OpenCobol/HelloWorld.cbl` — Hello-world reference [`OpenCobol/HelloWorld.cbl:L1-21`]
- `OpenCobol/Conditions/*.cbl` — eight predicate-pattern references
- `OpenCobol/Database/*.cbl` — four sequential-file pattern references; `OpenCobol/Database/data.txt` is the fixture format reference
- `OpenCobol/Date/DateAndTime.cbl` — date/time `ACCEPT` reference
- `OpenCobol/Games/raylib/*.cbl` — referenced ONLY to confirm exclusion rationale
- `OpenCobol/Loops/*.cbl` — `PERFORM UNTIL` / `PERFORM VARYING` references
- `OpenCobol/Memory/*.cbl` — `POINTER` / `LINKAGE SECTION` references [`OpenCobol/Memory/Address.cbl:L1-36`, `OpenCobol/Memory/Pointer.cbl:L1-37`]
- `OpenCobol/Random/*.cbl` — `FUNCTION RANDOM` / seeding references
- `OpenCobol/SQLite/Hello_SQLITE.cbl` — `ocsqlite` integration and SQL-injection vector reference [`OpenCobol/SQLite/Hello_SQLITE.cbl:L1-396`]
- `OpenCobol/Sort/*.cbl` — sort algorithm references
- `OpenCobol/String/String.cbl` — substring iteration reference
- `OpenCobol/Struct/Struct.cbl` — `OCCURS` arrays in grouped structure reference
- `LICENSE` — GPLv3 text reference (used to extract the license header for new `.java` files)
- `README.md` — existing root README to preserve verbatim during append

### 0.4.2 Cross-File Dependencies

**Package declarations (one per Java source file, no exceptions):**
- Each entry class and its test declare `package com.projectcobol.<submodule>;` where `<submodule>` is the sub-module name with the `cobol-` prefix stripped. Example: `cobol-helloworld` → `com.projectcobol.helloworld`.
- `cobol-sqlite` → `com.projectcobol.sqlite`; `cobol-conditions` → `com.projectcobol.conditions`; and so on for every sub-module.

**Import statement additions (all NEW; no pre-existing Java imports to refactor):**

All 25 entry classes import:
```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
```

Module-specific additions:
- `cobol-database/*Application.java`: `java.io.BufferedReader`, `java.io.InputStream`, `java.io.InputStreamReader`, `java.nio.charset.StandardCharsets`, `com.projectcobol.database.FileStatus`
- `cobol-date/DateAndTimeApplication.java`: `java.time.LocalDate`, `java.time.LocalTime`, `java.time.format.DateTimeFormatter`
- `cobol-random/*Application.java`, `cobol-sort/*Application.java`: `java.util.Random`
- `cobol-sqlite/SqliteApplication.java`: `java.sql.Connection`, `java.sql.DriverManager`, `java.sql.PreparedStatement`, `java.sql.ResultSet`, `java.sql.SQLException`, `java.util.Scanner` (only if interactive paging mode preserved)
- All 25 `*Test.java` classes import: `org.junit.jupiter.api.Test`, `static org.junit.jupiter.api.Assertions.assertEquals`, `java.io.ByteArrayOutputStream`, `java.io.PrintStream`, `java.nio.charset.StandardCharsets`

**No cross-submodule Java dependencies:**

- Each `cobol-<topic>` sub-module is self-contained. There is no `cobol-common` module.
- `FileStatus` lives inside `cobol-database` and is not exported to any other sub-module — no other sub-module references COBOL FILE-STATUS semantics.

**Maven parent-child wiring:**

- Each sub-module `pom.xml` declares:
  ```xml
  <parent>
      <groupId>com.projectcobol</groupId>
      <artifactId>cobol-parent</artifactId>
      <version>1.0.0</version>
  </parent>
  ```
- The root `pom.xml` declares:
  ```xml
  <modules>
      <module>cobol-helloworld</module>
      <module>cobol-conditions</module>
      <module>cobol-database</module>
      <module>cobol-date</module>
      <module>cobol-loops</module>
      <module>cobol-memory</module>
      <module>cobol-random</module>
      <module>cobol-sqlite</module>
      <module>cobol-sort</module>
      <module>cobol-string</module>
      <module>cobol-struct</module>
  </modules>
  ```

**Configuration updates for new structure:**

- `.gitignore` MUST include `target/` to prevent committing build artifacts.
- The existing `README.md` requires no edits beyond the appended Java build section.

**Test file import corrections:**

- Not applicable in the traditional refactor sense — every test file is CREATE (no pre-existing imports to update).

### 0.4.3 Wildcard Patterns

All wildcard patterns used in this AAP are **trailing** (e.g., `cobol-conditions/src/main/java/com/projectcobol/conditions/*Application.java`). No leading wildcards (such as `**/conditions/*.java`) appear in any scope or transformation statement, in accordance with the AAP's wildcard discipline. Where individual files are enumerable, this AAP enumerates them by name rather than relying on a wildcard — for example, the 8 Conditions classes, 4 Database classes, 3 Sort classes, and 2 each for Loops / Memory / Random are listed by full name in section 0.4.1 to remove ambiguity.

### 0.4.4 One-Phase Execution

The entire refactor will be executed by Blitzy in **ONE phase**. The 92 file operations listed in section 0.4.1 (3 root + 11 sub-module POMs + 26 main Java + 25 test Java + 25 golden-output fixtures + 1 data.txt resource copy + 1 Games README) execute as a single coherent commit. There is no multi-phase split; the build is structurally self-consistent only after all 92 artifacts exist, so partial execution would leave the repository in a non-buildable state.

Within the single phase, there is no enforced ordering constraint between files — Maven resolves the parent-child relationship at build time, so the root `pom.xml`, sub-module POMs, and Java sources may be authored in any sequence.


## 0.5 Dependency Inventory

### 0.5.1 Key Private and Public Packages

Project COBOL today has zero managed package dependencies — there is no Maven, npm, Gradle, or any other dependency manifest in the repository, and the existing COBOL build relies only on the host `cobc` compiler and (for `Hello_SQLITE.cbl`) the `libsqlite3-dev` system library [tech-spec §2.4.1, §3.4.4]. Every dependency in the table below is therefore **new**, added as part of this refactor.

| Registry | Group:Artifact | Version | Scope | Purpose |
|----------|----------------|---------|-------|---------|
| JDK distribution | Eclipse Temurin OpenJDK | 21 (LTS) | host runtime | Java 21 LTS — language and standard library for every sub-module |
| Build tool | Apache Maven | 3.9.x or newer | host build | Multi-module build orchestration; `mvn clean package -DskipTests` and `mvn test` |
| Maven Central | `org.springframework.boot:spring-boot-starter-parent` | 3.5.9 (or latest 3.5.x at translation time) | parent POM | Root POM inheritance; manages the Spring Boot 3.5 BOM, sets `java.version=21`, pins plugin versions |
| Maven Central | `org.springframework.boot:spring-boot-starter` | (BOM-managed → 3.5.x) | `compile` | Core Spring Boot bootstrap: `SpringApplication`, `CommandLineRunner`, autoconfiguration; transitively brings Spring Framework 6.2.x |
| Maven Central | `org.springframework.boot:spring-boot-starter-test` | (BOM-managed → 3.5.x) | `test` | Test scaffolding: JUnit Jupiter 5.12.x, AssertJ, Mockito, spring-test, spring-boot-test |
| Maven Central | `org.xerial:sqlite-jdbc` | 3.53.1.0 | `compile` (cobol-sqlite ONLY) | JDBC driver for SQLite; replaces the `ocsqlite` C binding [tech-spec §3.4.4]. Bundled native libraries for Windows, macOS, Linux, FreeBSD. Apache 2.0 licensed (GPLv3-compatible). |

**No private packages.** All dependencies are publicly available on Maven Central; no internal artifact registries, no proprietary libraries, no GitHub Packages, no Sonatype-private repositories are involved.

**Transitive test framework versions (BOM-managed via `spring-boot-starter-test`):**

| Group:Artifact | Version (Spring Boot 3.5 BOM) | Purpose |
|----------------|--------------------------------|---------|
| `org.junit.jupiter:junit-jupiter-api` | 5.12.x | JUnit 5 API for `@Test`, `Assertions` |
| `org.junit.jupiter:junit-jupiter-engine` | 5.12.x | JUnit 5 execution engine |
| `org.junit.jupiter:junit-jupiter-params` | 5.12.x | `@ParameterizedTest` support |
| `org.junit.platform:junit-platform-launcher` | 1.12.x | Platform launcher |
| `org.assertj:assertj-core` | 3.27.x | Optional fluent assertions |
| `org.mockito:mockito-core` | 5.17.x | Mocking (not exercised by golden-output tests, but available) |

**Maven plugin inventory (BOM-managed by `spring-boot-starter-parent` unless overridden in the root `pom.xml`):**

| Plugin | Version | Configuration |
|--------|---------|---------------|
| `org.apache.maven.plugins:maven-compiler-plugin` | BOM-managed | `source=21`, `target=21`, `<compilerArgs><arg>-Xlint:all</arg><arg>-Werror</arg></compilerArgs>` (the `-Xlint:all` is mandated by the prompt's zero-warning compile requirement) |
| `org.apache.maven.plugins:maven-surefire-plugin` | 3.5.5 (BOM-managed) | JUnit Jupiter test discovery; respects `-DskipTests` |
| `org.springframework.boot:spring-boot-maven-plugin` | BOM-managed (Spring Boot 3.5.x) | `<goals><goal>repackage</goal></goals>` — produces an executable fat-jar per sub-module |

### 0.5.2 Dependency Updates

**Direct dependency delta:**

- **Added:** 3 direct compile/test dependencies (`spring-boot-starter`, `spring-boot-starter-test`, `sqlite-jdbc:3.53.1.0`) plus 1 parent (`spring-boot-starter-parent:3.5.x`).
- **Updated:** None — no pre-existing Java manifest exists to update.
- **Removed:** None — the `ocsqlite` C library is **not** removed from the system. The original `OpenCobol/SQLite/Hello_SQLITE.cbl` remains intact and continues to depend on `ocsqlite` at the COBOL level; the new `cobol-sqlite` Java sub-module simply does not consume `ocsqlite`. The COBOL build path documented in tech-spec §3.4.4 / §5.3.4 (the two-step `cobc -c ocsqlite.c` / `cobc -x -lsqlite3 sqlscreen.cob ocsqlite.o`) is preserved for the COBOL build, untouched.

### 0.5.3 Import Refactoring

There is no traditional "import refactoring" work in this project because **all** Java source files are newly created — there are no pre-existing Java imports to rewrite. The new import statements introduced are documented exhaustively in section 0.4.2 (Cross-File Dependencies). For convenience the standard import set is restated here:

**Files requiring new imports (all CREATE-mode, no pattern needed but listed by sub-module trailing wildcard):**
- `cobol-helloworld/src/main/java/com/projectcobol/helloworld/*.java`
- `cobol-conditions/src/main/java/com/projectcobol/conditions/*.java`
- `cobol-database/src/main/java/com/projectcobol/database/*.java`
- `cobol-date/src/main/java/com/projectcobol/date/*.java`
- `cobol-loops/src/main/java/com/projectcobol/loops/*.java`
- `cobol-memory/src/main/java/com/projectcobol/memory/*.java`
- `cobol-random/src/main/java/com/projectcobol/random/*.java`
- `cobol-sqlite/src/main/java/com/projectcobol/sqlite/*.java`
- `cobol-sort/src/main/java/com/projectcobol/sort/*.java`
- `cobol-string/src/main/java/com/projectcobol/string/*.java`
- `cobol-struct/src/main/java/com/projectcobol/struct/*.java`
- All `*Test.java` paths under `cobol-*/src/test/java/com/projectcobol/*/`

**Import transformation rules** (these are additive insertions into newly created files, not edits to existing files):

- **Spring Boot bootstrap triplet** (applied to every entry class):
    - `import org.springframework.boot.SpringApplication;`
    - `import org.springframework.boot.CommandLineRunner;`
    - `import org.springframework.boot.autoconfigure.SpringBootApplication;`
- **JUnit 5 triplet** (applied to every test class):
    - `import org.junit.jupiter.api.Test;`
    - `import static org.junit.jupiter.api.Assertions.assertEquals;`
    - `import org.junit.jupiter.api.AfterEach;` / `BeforeEach` where output redirection setup is needed
- **Output capture pair** (applied to every test class):
    - `import java.io.ByteArrayOutputStream;`
    - `import java.io.PrintStream;`
- **Module-specific additions** are listed in section 0.4.2; the most important are `java.sql.*` for `cobol-sqlite`, `java.io.BufferedReader` / `java.nio.file.*` for `cobol-database`, and `java.util.Random` for `cobol-random` / `cobol-sort`.

### 0.5.4 External Reference Updates

The following non-source artifacts are touched:

- **`README.md` (repository root)** — APPEND-ONLY update. A new `## Java 21 / Spring Boot Build` section is appended documenting:
    - Prerequisites: Java 21 JDK (e.g., Eclipse Temurin 21), Maven 3.9+
    - Build: `git clone https://github.com/Martinfx/Cobol.git && cd Cobol; mvn clean package -DskipTests`
    - Run: `java -jar cobol-helloworld/target/cobol-helloworld-1.0.0.jar`
    - Test: `mvn test`
    - The new section sits **after** the existing COBOL build instructions; the existing topic catalog [`README.md:L1-56`] is preserved verbatim.

- **`OpenCobol/Games/README.md`** — CREATE. New file documenting the rationale for excluding `OpenCobol/Games/raylib/*.cbl` from Java translation (raylib is a C-only graphics library; no idiomatic Java equivalent; translating it would violate the minimal-change clause).

The following categories of external reference files are **not** updated because they do not exist in the repository today and are not mandated by the prompt:

- **CI/CD configuration files** — no `.github/workflows/*.yml`, `.gitlab-ci.yml`, `.circleci/config.yml`, or equivalent are created. The prompt does not require automated CI, and the minimal-change clause forbids speculative additions [tech-spec §2.4.1 confirms the absence of any pre-existing CI/CD configuration].
- **Container or deployment descriptors** — no `Dockerfile`, no `docker-compose.yml`, no Kubernetes manifests are created.
- **Build configuration files** beyond Maven — no Gradle, no SBT, no Bazel files are introduced.

### 0.5.5 Licensing Compatibility

Every new dependency is license-compatible with the project's existing GPLv3 license posture [tech-spec §3.7]:

| Component | License | GPLv3 Compatibility |
|-----------|---------|---------------------|
| Spring Boot (Apache 2.0) | Apache License 2.0 | ✅ Compatible (one-way; can be combined into GPLv3 work) |
| sqlite-jdbc (Apache 2.0) | Apache License 2.0 | ✅ Compatible |
| JUnit Jupiter (EPL 2.0) | Eclipse Public License v2.0 | ✅ Compatible (in test scope) |
| AssertJ (Apache 2.0) | Apache License 2.0 | ✅ Compatible (test scope) |
| Mockito (MIT) | MIT License | ✅ Compatible (test scope) |

No dependency change weakens the project's GPLv3 obligations, and the existing `LICENSE` file is preserved verbatim.


## 0.6 Special Analysis

Three transformation areas warrant deeper, file-level analysis because their COBOL semantics map non-trivially to Java idiom. Each analysis below cites the original `.cbl` source by line range and the tech-spec section that authoritatively documents the existing behavior.

### 0.6.1 COBOL `POINTER` / `LINKAGE SECTION` → Java Reference Semantics (`cobol-memory`)

**Existing system evidence:**

`OpenCobol/Memory/Address.cbl` and `OpenCobol/Memory/Pointer.cbl` are structurally identical twin demonstrations of COBOL pointer aliasing. Both programs declare a 16-byte `WORKING-STORAGE` group split into four `PIC X(4)` elementaries [`OpenCobol/Memory/Address.cbl:L9-13`, `OpenCobol/Memory/Pointer.cbl:L10-14`], a single `POINTER` variable [`OpenCobol/Memory/Address.cbl:L15`, `OpenCobol/Memory/Pointer.cbl:L16`], and a `LINKAGE SECTION` group of type `01 WORK-DATA` with two `PIC X(4)` sub-fields `WORK-A` and `NEXT-WORK-DATA` [`OpenCobol/Memory/Address.cbl:L17-20`, `OpenCobol/Memory/Pointer.cbl:L18-21`]. The `PROCEDURE DIVISION` performs the two critical pointer manipulations:

```
SET WK-PTR TO ADDRESS OF WORK-AREA.        *> capture the address of the WORKING-STORAGE group
SET ADDRESS OF WORK-DATA TO WK-PTR.        *> alias the LINKAGE group at that address
```

These statements [`OpenCobol/Memory/Address.cbl:L27-28`, `OpenCobol/Memory/Pointer.cbl:L28-29`] establish a typed VIEW: after execution, references to `WORK-DATA` (and its sub-fields `WORK-A`, `NEXT-WORK-DATA`) read and write the same bytes as the `WORK-AREA` group. The DISPLAY statements that follow [`OpenCobol/Memory/Address.cbl:L30-33`, `OpenCobol/Memory/Pointer.cbl:L31-34`] verify that `WORK-A` returns `"AAAA"` (the first four bytes of `WORK-AREA`) and `NEXT-WORK-DATA` returns `"BBBB"` (the next four bytes).

**Java translation strategy:**

Java is memory-safe and exposes no raw pointer or `union`-style reinterpretation. The Java translation models the COBOL semantics with a **backing buffer + substring views** approach:

```java
// COBOL: 01 WORK-AREA. 03 WK-A PIC X(4) VALUE 'AAAA'. ... 03 WK-D PIC X(4) VALUE 'DDDF'.
private String workArea = "AAAA" + "BBBB" + "CCCC" + "DDDF";  // 16-char backing store

// COBOL: 01 WK-PTR POINTER.
private int wkPtr = -1;  // simulated address — logical offset into workArea

// COBOL: 01 WORK-DATA. 03 WORK-A PIC X(4). 03 NEXT-WORK-DATA PIC X(4).
private String workA;
private String nextWorkData;
```

The two COBOL `SET` statements become deterministic offset assignments and substring projections:

```java
// COBOL: SET WK-PTR TO ADDRESS OF WORK-AREA.
wkPtr = 0;

// COBOL: SET ADDRESS OF WORK-DATA TO WK-PTR.
workA = workArea.substring(wkPtr, wkPtr + 4);
nextWorkData = workArea.substring(wkPtr + 4, wkPtr + 8);
```

**Required inline documentation comments:**

Every pointer-manipulation block in `AddressApplication.java` and `PointerApplication.java` MUST carry an explanatory comment block explaining the COBOL→Java mapping, satisfying the prompt's "explanatory comments" mandate for the memory sub-module. Example:

```java
// COBOL POINTER → Java does NOT expose raw memory addresses; the address is
// modeled as a logical byte offset into a backing String. SET ADDRESS OF
// WORK-DATA TO WK-PTR maps to projecting substring views starting at that offset.
// This is a faithful semantic translation, not a literal memory operation.
```

**Golden-output implications:**

The original COBOL `DISPLAY "WK-PTR :   " WK-PTR` line emits an implementation-defined pointer value (typically a hex or decimal address). Java has no equivalent runtime memory address that can be reliably displayed. The Java translation MUST emit a deterministic placeholder — for example:

- Before `SET`: `WK-PTR :   0x00000000`
- After `SET`: `WK-PTR :   0x00000010` (a sentinel offset, not a real address)

The `*ApplicationTest.java` golden-output fixtures for `cobol-memory` use a format-match assertion (regex) for the two `WK-PTR :` lines instead of literal equality. All other DISPLAY lines (`WORK-DATA :`, `WORK-A :`, `NEXT-WORK-DATA :`) are deterministic and compared exactly.

**Citation anchors:**

- `OpenCobol/Memory/Address.cbl:L1-36` — full source for `WORK-OFFSET` program
- `OpenCobol/Memory/Pointer.cbl:L1-37` — full source for `WORK-WITH-POINTER` program
- Tech-spec §6.2.2 documents the in-memory data structures preserved by this translation

### 0.6.2 `ocsqlite` Callback Architecture → JDBC `ResultSet` Iteration (`cobol-sqlite`)

This is simultaneously the **most architecturally significant** translation and the **only intentionally behavior-modifying** translation in the entire refactor — it is where the SQL injection vulnerability documented in tech-spec §2.4.4, §5.3.5, and §6.2.3.1 is remediated.

**Existing system evidence:**

`OpenCobol/SQLite/Hello_SQLITE.cbl` is a 396-line composite program comprising the main `sqlscreen` program (lines 76–310) and a child `callback` sub-program (lines 333–396). It interacts with SQLite via the `ocsqlite` C binding through three external CALLs:

- `ocsqlite_init` — initializes the connection with a zero-terminated database path string `pic x(8) value 'test.db' & x'00'` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L87`, L137-143]
- `ocsqlite` — executes an arbitrary SQL string, with a procedure-pointer (`callback-proc`) bound to receive each result row [`OpenCobol/SQLite/Hello_SQLITE.cbl:L296-304`]
- `ocsqlite_close` — terminates the connection [`OpenCobol/SQLite/Hello_SQLITE.cbl:L276-281`]

The callback architecture is the central design constraint. The `callback` sub-program [`OpenCobol/SQLite/Hello_SQLITE.cbl:L333-396`] is invoked once per result row and receives four parameters via `linkage section`: a void pointer (unused), the field count, the row data as a `pic x(132)` alphanumeric, and the row length [`OpenCobol/SQLite/Hello_SQLITE.cbl:L361-362`]. Each row is moved into a shared `external` data structure — a 20-slot ring buffer declared `01 sql-table external. 03 sql-records pic x(50) occurs 20 times.` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L102-104`, `:L351-352`] — and the shared `row-counter binary-long external` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L105`, `:L349`] is incremented. After `ocsqlite` returns, the main program iterates the buffered rows in reverse (`perform varying row-counter from row-max by -1 until row-counter < 1` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L250-256`]) and then again forward through a `screen section` interactive paging loop [`OpenCobol/SQLite/Hello_SQLITE.cbl:L262-266`].

The `ocsql-exec` paragraph [`OpenCobol/SQLite/Hello_SQLITE.cbl:L288-308`] is the textual SQL execution mechanism. It builds a zero-terminated C-string `zquery` via `STRING ... DELIMITED BY SIZE x"00" DELIMITED BY SIZE INTO zquery` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L289-294`] and passes it to `ocsqlite` for execution. **There is no parameter binding, no escaping, no separation of code from data.** Any user input that flows into the `query` field is interpolated literally into the SQL stream — this is the SQL injection vector documented in [tech-spec §2.4.4, §5.3.5, §6.2.3.1]. The user input source is the `SCREEN SECTION` declaration at lines 108–126, specifically the `using key-field` field on line 116 [`OpenCobol/SQLite/Hello_SQLITE.cbl:L116`].

**Java translation strategy — structural replacement:**

| COBOL Construct | JDBC Replacement |
|-----------------|------------------|
| `call "ocsqlite_init" using db, database, errstr, length(errstr) returning result` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L137-143`] | `Connection conn = DriverManager.getConnection("jdbc:sqlite:test.db");` (try-with-resources auto-closes) |
| `01 db usage pointer` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L89`] | `java.sql.Connection conn` |
| `01 callback-proc usage procedure-pointer` + `program-id. callback.` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L90, L334`] | (eliminated — replaced by inline `while (rs.next())` loop) |
| `01 sql-table external. 03 sql-records pic x(50) occurs 20 times` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L102-104`] | `java.sql.ResultSet rs` (streaming, unlimited rows) |
| `01 row-counter usage binary-long external` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L105`] | (eliminated — `rs.next()` advances internally) |
| `ocsql-exec` paragraph with `STRING ... x"00" ... INTO zquery` + `call "ocsqlite" using db, callback-proc, zquery, length(zquery), errstr, length(errstr)` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L288-308`] | `PreparedStatement ps = conn.prepareStatement(sql); ps.setInt(1, keyField); ResultSet rs = ps.executeQuery();` |
| `screen section entry-screen. ... pic x(10) using key-field` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L108-116`] | `java.util.Scanner scanner; int keyField = scanner.nextInt();` — interactive input source, value bound via `ps.setInt(1, keyField)` |
| `call "ocsqlite_close" using by value db returning result` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L276-281`] | try-with-resources auto-closes the `Connection`; no explicit close call |
| `perform varying row-counter from row-max by -1 ...` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L250-256`] | `List<Row> rows = new ArrayList<>(); while (rs.next()) rows.add(...); Collections.reverse(rows); rows.forEach(...);` (only if reverse-iteration output is part of the golden fixture) |

**The security fix:**

The behavioral change permitted by the prompt's minimal-change-clause exception is that the SQL execution mechanism switches from text substitution to parameter binding. The CREATE/DROP/INSERT statements that contain only SQL literals (no user input) remain text-substituted via `Statement.execute(...)` — they cannot be injected because no user data is concatenated into them. The `SELECT` statement that consumes `key-field` user input is converted to a `PreparedStatement` with `ps.setInt(1, keyField)`:

```java
// COBOL: SCREEN SECTION pic x(10) using key-field → ACCEPT entry-screen
int keyField = scanner.nextInt();

// COBOL: move "select * from trial where first = " ... key-field ...  → ocsql-exec
PreparedStatement ps = conn.prepareStatement("SELECT * FROM trial WHERE first = ?");
ps.setInt(1, keyField);  // parameter binding — SQL injection is structurally impossible
ResultSet rs = ps.executeQuery();
while (rs.next()) {
    System.out.println(rs.getInt("first") + " " + rs.getString("second") + " " + rs.getString("third"));
}
```

**Preserved verbatim SQL:**

The SQLite-specific functions used in the COBOL source are preserved character-for-character in the Java translation. The `CREATE TABLE` schema [`OpenCobol/SQLite/Hello_SQLITE.cbl:L191-193`] becomes:

```java
stmt.execute("create table trial (first integer primary key, second char(20), third date)");
```

The INSERT that exercises SQLite's `randomblob`, `hex`, `lower`, `datetime`, and `julianday` functions [`OpenCobol/SQLite/Hello_SQLITE.cbl:L204-208`, `:L220-224`] is preserved as:

```java
stmt.execute("insert into trial (first, second, third) values "
           + "(null, lower(hex(randomblob(20))), datetime()); "
           + "insert into trial values (null, 'something', julianday())");
```

These statements contain no user input and require no parameter binding — they are pure DDL/DML literals.

**Regression test for the security fix:**

`SqliteApplicationTest.java` MUST contain a dedicated `@Test injectionAttemptIsNeutralized()` method that passes the literal string `'; DROP TABLE trial; --` as the `keyField` value through the `PreparedStatement`. The test asserts (a) no `SQLException` thrown, (b) the `trial` table still exists in the in-memory database after the query runs (verified via `SELECT name FROM sqlite_master WHERE type='table' AND name='trial'`), (c) the result set is empty (the literal is bound as a value, never executed as code).

**Test connection scheme:**

Production code uses `jdbc:sqlite:test.db` (file-backed, mirrors COBOL's `'test.db' & x'00'` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L87`]). Tests use `jdbc:sqlite::memory:` (in-memory, reset per `@BeforeEach`, no filesystem footprint) so test runs are deterministic and parallel-safe.

**Citation anchors:**

- `OpenCobol/SQLite/Hello_SQLITE.cbl:L1-396` — full source for `sqlscreen` + `callback` programs
- Tech-spec §3.4.4 — `ocsqlite` v0.90 by Brian Tiffin (09-Oct-2008) and the two-step `cobc` compilation
- Tech-spec §3.5.2 — F-009 callback-based row buffering
- Tech-spec §2.4.4 — SQL injection vulnerability documentation
- Tech-spec §5.3.5 — SQL injection acceptable for educational use only in COBOL form
- Tech-spec §6.2.3.1 — `trial` table schema and the SCREEN SECTION ACCEPT as injection point

### 0.6.3 COBOL `FILE-STATUS` 88-Level Codes → Java `enum FileStatus` (`cobol-database`)

**Existing system evidence:**

`OpenCobol/Database/StatusCode.cbl` is a 45-line reference declaration of the COBOL `FILE-STATUS` code vocabulary. Its `WORKING-STORAGE SECTION` [`OpenCobol/Database/StatusCode.cbl:L8-39`] declares `01 STATUS-CODE pic x(2) value spaces.` followed by 30 `88-level` condition names mapping descriptive identifiers to two-character status codes. The complete inventory captured from the source:

| 88-Level Name | Code | Semantics |
|---------------|------|-----------|
| `SUCCESS` | `'00'` | Operation completed successfully |
| `SUCCESS_DUPLICATE` | `'02'` | Success with duplicate key encountered |
| `SUCCESS_INCOMPLETE` | `'04'` | Success but length mismatch |
| `SUCCESS_OPTIONAL` | `'05'` | Success on optional file open |
| `SUCCESS_NO_UNIT` | `'07'` | Success with no unit info |
| `END_OF_FILE` | `'10'` | End-of-file reached during sequential read |
| `OUT_OF_KEY_RANGE` | `'14'` | Key beyond file boundary |
| `KEY_INVALID` | `'21'` | Invalid key sequence on indexed file |
| `KEY_EXISTS` | `'22'` | Duplicate primary key on WRITE |
| `KEY_NOT_EXISTS` | `'23'` | Key not found on READ/REWRITE/DELETE |
| `PERMANENT_ERROR` | `'30'` | Hardware/system error |
| `INCONSISTENT_FILENAME` | `'31'` | File name attribute mismatch |
| `BOUNDARY_VIOLATION` | `'34'` | Boundary violation on sequential file |
| `NOT_EXISTS` | `'35'` | File not found on OPEN |
| `PERMISSION_DENIED` | `'37'` | OPEN denied due to permissions |
| `CLOSED_WITH_LOCK` | `'38'` | File previously CLOSED WITH LOCK |
| `CONFLICT_ATTRIBUTE` | `'39'` | OPEN attribute mismatch with file |
| `ALREADY_OPEN` | `'41'` | OPEN attempted on already-open file |
| `NOT_OPEN` | `'42'` | I/O attempted on closed file |
| `READ_NOT_DONE` | `'43'` | REWRITE/DELETE without prior READ |
| `RECORD_OVERFLOW` | `'44'` | Record length boundary exceeded |
| `READ_ERROR` | `'46'` | Sequential READ failure |
| `INPUT_DENIED` | `'47'` | Input operation denied |
| `OUTPUT_DENIED` | `'48'` | Output operation denied |
| `I_O_DENIED` | `'49'` | I/O operation denied |
| `RECORD_LOCKED` | `'51'` | Record locked by another process |
| `END_OF_PAGE` | `'52'` | End-of-page reached on REPORT |
| `I_O_LINAGE` | `'57'` | LINAGE violation |
| `FILE_SHARING` | `'61'` | File-sharing conflict |
| `NOT_AVAILABLE` | `'91'` | Implementation-defined unavailability |

The `PROCEDURE DIVISION` itself is minimal — `MAIN-PROCEDURE` displays `"Hello world"` and issues `GOBACK` [`OpenCobol/Database/StatusCode.cbl:L41-44`]. The program's actual value is the declarative table of file-status codes, which is **referenced implicitly** by the other three Database programs (`OpenFileRecordKey.cbl`, `OpenFileSequential.cbl`, `SequentialRead.cbl`) when they check `FILE STATUS IS STATUS-CODE` after each file operation.

**Java translation strategy:**

The 30 `88-level` conditions form a fixed, externally-known enumeration. The natural Java equivalent is a `public enum`:

```java
public enum FileStatus {
    SUCCESS("00"), SUCCESS_DUPLICATE("02"), SUCCESS_INCOMPLETE("04"),
    SUCCESS_OPTIONAL("05"), SUCCESS_NO_UNIT("07"), END_OF_FILE("10"),
    OUT_OF_KEY_RANGE("14"), KEY_INVALID("21"), KEY_EXISTS("22"),
    KEY_NOT_EXISTS("23"), PERMANENT_ERROR("30"), INCONSISTENT_FILENAME("31"),
    BOUNDARY_VIOLATION("34"), NOT_EXISTS("35"), PERMISSION_DENIED("37"),
    CLOSED_WITH_LOCK("38"), CONFLICT_ATTRIBUTE("39"), ALREADY_OPEN("41"),
    NOT_OPEN("42"), READ_NOT_DONE("43"), RECORD_OVERFLOW("44"),
    READ_ERROR("46"), INPUT_DENIED("47"), OUTPUT_DENIED("48"),
    I_O_DENIED("49"), RECORD_LOCKED("51"), END_OF_PAGE("52"),
    I_O_LINAGE("57"), FILE_SHARING("61"), NOT_AVAILABLE("91"),
    UNKNOWN("");

    private final String code;
    FileStatus(String code) { this.code = code; }
    public String getCode() { return code; }

    public static FileStatus fromCode(String code) {
        for (FileStatus fs : values()) if (fs.code.equals(code)) return fs;
        return UNKNOWN;
    }
}
```

**Usage across `cobol-database`:**

- `OpenFileRecordKeyApplication.java` translates `IF SUCCESS DISPLAY "OK" ELSE DISPLAY "Error: " STATUS-CODE` to `if (FileStatus.fromCode(statusCode) == FileStatus.SUCCESS) { ... } else { ... }`. The `ERROR-RESULT` group in the COBOL source becomes an instance field plus the enum lookup.
- `OpenFileSequentialApplication.java` translates the EOF check `88 EOF-T VALUE 'T'` (loop-state EOF flag, distinct from FILE-STATUS) to a `boolean eof` field, but also uses `FileStatus.END_OF_FILE` for documentation parity when reporting EOF to the user.
- `SequentialReadApplication.java` is the simplest consumer — it just translates `OPEN INPUT` / `READ NEXT ... AT END` to `BufferedReader.readLine() == null`, with `FileStatus.SUCCESS` / `END_OF_FILE` used in display messages.
- `StatusCodeApplication.java` itself is a one-liner: `run()` calls `System.out.println("Hello world")` [`OpenCobol/Database/StatusCode.cbl:L43`]. The enum exists independently and the application's behavior matches the original COBOL exactly.

**Placement decision:**

`FileStatus.java` lives at `cobol-database/src/main/java/com/projectcobol/database/FileStatus.java`. It is **not** promoted to a shared `cobol-common` module because no other sub-module references COBOL `FILE-STATUS` codes — keeping it inside `cobol-database` preserves the per-sub-module isolation principle from section 0.3.3.

**Golden-output fidelity:**

`StatusCodeApplication.txt` contains a single line: `Hello world`. The 30 enum entries are structural translations of declarations, not behavior — they produce no console output in the `StatusCodeApplication` itself.

**Citation anchors:**

- `OpenCobol/Database/StatusCode.cbl:L1-45` — full source
- `OpenCobol/Database/StatusCode.cbl:L10-39` — the 30 `88-level` condition declarations
- `OpenCobol/Database/StatusCode.cbl:L41-44` — the minimal `MAIN-PROCEDURE` body
- Tech-spec §6.2.3.2 — canonical FILE-STATUS code reference and its role across the OpenCobol/Database/ topic


## 0.7 Refactoring Rules

### 0.7.1 User-Emphasized Refactoring Rules

The prompt explicitly mandates the following rules. Each is restated in technical language and bound to enforceable acceptance criteria.

- **Maintain dual-platform educational mission.** The `AS400/` branch and the original `OpenCobol/` `.cbl` sources must remain untouched [tech-spec §1.2.1]. Acceptance: a `git diff` against the pre-refactor `main` branch shows zero modifications to any file under `AS400/` or to any `OpenCobol/**/*.cbl` file.
- **Preserve existing COBOL behavior exactly in the Java translation.** Every Java `*Application.java` must produce output that matches the COBOL original byte-for-byte (with documented exceptions for non-deterministic clock and pointer-address output, which use format-match assertions). Acceptance: the JUnit 5 golden-output test suite passes (`mvn test` returns exit code 0).
- **Apply the SQL injection fix as the only intentional behavioral change.** `cobol-sqlite/.../SqliteApplication.java` MUST use `PreparedStatement` parameter binding for any SQL statement that consumes user input. Acceptance: (a) no string concatenation appears in `SqliteApplication.java` between a user-input field and a SQL keyword; (b) `SqliteApplicationTest.injectionAttemptIsNeutralized()` passes; (c) the COBOL source `OpenCobol/SQLite/Hello_SQLITE.cbl` remains unmodified — the fix lives ONLY in the Java translation.
- **Maintain GPLv3 license posture.** Every new `.java` file begins with a GPLv3 license header comment. The existing `LICENSE` file is preserved verbatim. Acceptance: a recursive `grep "GNU General Public License" cobol-*/src/main/java cobol-*/src/test/java` matches every Java source file in the new sub-modules.
- **Compile cleanly under Java 21 with `-Xlint:all`.** Zero compilation errors, zero compiler warnings. Acceptance: `mvn clean compile` exits with code 0 and no `[WARNING]` lines from `maven-compiler-plugin`. The root `pom.xml` configures `<compilerArgs><arg>-Xlint:all</arg><arg>-Werror</arg></compilerArgs>` to enforce this at the build level.
- **Use Maven multi-module structure with per-topic sub-modules.** Eleven sub-modules listed in section 0.3.1. Acceptance: `mvn -pl cobol-helloworld package` (or any single `-pl`) builds successfully in isolation; `mvn clean package` from the root builds all sub-modules.
- **Use package prefix `com.projectcobol.<submodule>` for all Java code.** Acceptance: every `.java` file's first non-comment line is `package com.projectcobol.<submodule>;` matching its directory location.
- **Implement each entry class as `@SpringBootApplication` + `CommandLineRunner`.** Acceptance: every `*Application.java` carries the `@SpringBootApplication` annotation, implements `CommandLineRunner`, defines `public static void main(String[] args)` calling `SpringApplication.run(...)`, and overrides `public void run(String... args) throws Exception`.
- **One `.cbl` → one Java class.** Acceptance: the file count under `cobol-*/src/main/java/com/projectcobol/*/*Application.java` exactly matches the count of `.cbl` files in scope (25 production entry classes for 24 in-scope `.cbl` files + 1 explicit standalone `FileStatus.java` enum that does not extend `*Application`).
- **JUnit 5 golden-output testing with fixture path `src/test/resources/expected/<ClassName>.txt`.** Acceptance: every entry class has a matching `*Test.java` that loads its expected fixture from the documented path and asserts equality (or regex match for non-deterministic output).

### 0.7.2 Minimal Change Clause / Refactor Discipline

The prompt's minimal-change clause is reproduced here as the central governing principle for this refactor:

- Make **only** changes absolutely necessary for the COBOL → Java migration.
- Preserve existing functionality exactly. The Java output must match the COBOL output (modulo the documented SQL injection fix).
- Do not modify code beyond what is directly required for the refactor.
- Do not enhance, optimize, generalize, or "improve" beyond migration requirements. Every refactor temptation (extracting a `JdbcTemplate` abstraction, adding a `FileRepository` interface, introducing a logging framework) is **explicitly forbidden** unless the prompt mandates it.
- Isolate new implementations in dedicated files and modules. The Java code lives in new `cobol-*` sub-modules; the original COBOL sources stay in `OpenCobol/`.
- Document all technology-specific changes with clear inline comments — particularly in `cobol-memory` (pointer semantics), `cobol-sqlite` (callback → ResultSet structural rewrite + security fix), and `cobol-database` (FILE-STATUS enum extraction).
- **EXCEPTION (the only one):** The SQL injection vulnerability fix in `OpenCobol/SQLite/Hello_SQLITE.cbl` [tech-spec §2.4.4, §5.3.5, §6.2.3.1] is the **single** intentional behavioral improvement permitted. The Java `SqliteApplication.java` uses `PreparedStatement`, while the COBOL source file itself remains unchanged.

### 0.7.3 Special Instructions and Constraints

- **Filename traceability is authoritative.** When the COBOL `PROGRAM-ID` and the source filename diverge (e.g., `PROGRAM-ID. WORK-OFFSET.` [`OpenCobol/Memory/Address.cbl:L6`] versus the filename `Address.cbl`), the Java class name follows the **filename** (`AddressApplication`), not the `PROGRAM-ID` (`WorkOffsetApplication`). This applies to `Address.cbl` → `AddressApplication`, `Pointer.cbl` → `PointerApplication`, `String.cbl` → `StringApplication` (not `WorkWithStringApplication`), `Struct.cbl` → `StructApplication` (not `StructExampleApplication`).
- **Preserve the source filename's exact spelling, including non-standard cases.** `OpenCobol/Conditions/EvaluteVerb.cbl` (note: `Evalute`, not `Evaluate`) maps to `EvaluteVerbApplication.java` — the misspelling is preserved verbatim for traceability.
- **Preserve verbatim SQL literals.** All SQL strings inside `OpenCobol/SQLite/Hello_SQLITE.cbl` (`CREATE TABLE trial (first integer primary key, second char(20), third date)`, `INSERT INTO trial ... lower(hex(randomblob(20))), datetime() ...`, `select * from trial`) are preserved character-for-character in `SqliteApplication.java`. Only the execution mechanism changes.
- **Preserve the 20-row `sql-table` design intent in documentation, but not in implementation.** The Java translation removes the 20-row buffer because JDBC's `ResultSet` is unlimited and streaming. A comment in `SqliteApplication.java` documents that the COBOL original was bounded by a 20-row `external sql-table` [`OpenCobol/SQLite/Hello_SQLITE.cbl:L102-104`] and that the Java implementation lifts this constraint by using `ResultSet` iteration.
- **Run instructions must be reproducible.** The appended README section must produce a buildable, runnable, testable project starting from a fresh `git clone` with only Java 21 and Maven 3.9+ pre-installed. No additional system packages or configuration steps may be required.
- **No CI/CD setup.** The prompt does not request `.github/workflows/*` or any other CI/CD configuration; the minimal-change clause forbids speculative additions.

**User-provided build and run examples (preserved verbatim from the prompt):**

> **User Example:**
> ```bash
> # Prerequisites: Java 21 JDK installed (e.g., Eclipse Temurin 21), Maven 3.9+ installed
> git clone https://github.com/Martinfx/Cobol.git && cd Cobol
> mvn clean package -DskipTests        # build all sub-modules
> java -jar cobol-helloworld/target/cobol-helloworld-1.0.0.jar  # run a sample
> mvn test                             # run JUnit 5 tests
> ```

**User-provided COBOL-to-Java conceptual mappings (preserved verbatim as the authoritative translation rules):**

> **User Example — Translation Rules:**
> - COBOL `WORKING-STORAGE` → Java class-level fields with equivalent initialization
> - COBOL `PERFORM` sections → private Java methods
> - COBOL `DISPLAY` → `System.out.println()`
> - COBOL `ACCEPT` → `java.util.Scanner` reads from `System.in`
> - COBOL `IF/ELSE` → Java `if/else`
> - COBOL `EVALUATE TRUE` → Java `switch` expression
> - COBOL `88-level` conditions → static final boolean constants or enums
> - COBOL `FILE-CONTROL` / `FILE-STATUS` / `READ` / `OPEN` / `CLOSE` → `java.io.BufferedReader` / `java.nio.file.Files`
> - COBOL `PERFORM UNTIL` → `while` loop
> - COBOL `PERFORM VARYING` → `for` loop
> - COBOL `POINTER` / `ADDRESS OF` / `LINKAGE SECTION` → Java object references and array indexing (with explanatory comments)
> - COBOL `FUNCTION RANDOM` → `java.util.Random`
> - COBOL `SECONDS-PAST-MIDNIGHT` seeding → `System.currentTimeMillis() % 86400`
> - COBOL `SCREEN SECTION ACCEPT` with raw SQL → JDBC `PreparedStatement` (security fix)
> - COBOL `OCCURS` arrays → Java arrays
> - COBOL `78-level` constants → static final fields
> - COBOL grouped data items → inner classes or records
> - COBOL substring reference syntax → `String.substring()`
> - COBOL `ocsqlite` callback architecture → JDBC `ResultSet` iteration

**User-emphasized module mapping (preserved verbatim from the prompt):**

> **User Example — Module Mapping:**
>
> | Source | Sub-module | Entry Class |
> |---|---|---|
> | `OpenCobol/HelloWorld.cbl` | `cobol-helloworld` | `HelloWorldApplication.java` |
> | `OpenCobol/Conditions/*.cbl` (8 files) | `cobol-conditions` | One class per `.cbl` file |
> | `OpenCobol/Database/*.cbl` (4 files) | `cobol-database` | One class per `.cbl` file |
> | `OpenCobol/Date/Date.cbl` | `cobol-date` | `DateApplication.java` |
> | `OpenCobol/Loops/*.cbl` (2 files) | `cobol-loops` | One class per `.cbl` file |
> | `OpenCobol/Memory/*.cbl` (2 files) | `cobol-memory` | One class per `.cbl` file |
> | `OpenCobol/Random/*.cbl` (2 files) | `cobol-random` | `RandomApplication.java` |
> | `OpenCobol/SQLite/Hello_SQLITE.cbl` | `cobol-sqlite` | `SqliteApplication.java` |
> | `OpenCobol/Sort/*.cbl` (3 files) | `cobol-sort` | One class per sort algorithm |
> | `OpenCobol/String/*.cbl` | `cobol-string` | `StringApplication.java` |
> | `OpenCobol/Struct/*.cbl` | `cobol-struct` | `StructApplication.java` |

**Discrepancy resolution note:** The user-provided mapping references `OpenCobol/Date/Date.cbl`, but the actual file in the repository is `OpenCobol/Date/DateAndTime.cbl` (confirmed via repository inspection). The implementation follows the **actual filename** per the "filename traceability is authoritative" rule above: the entry class is `DateAndTimeApplication.java`, not `DateApplication.java`. This decision preserves traceability between the source `.cbl` and the translated Java class.

### 0.7.4 Web Search Requirements

The prompt indirectly requires research into current dependency versions for Java 21, Spring Boot 3.x, `sqlite-jdbc`, and JUnit 5. This research was conducted during Phase R1 (Intent Clarification) and the findings are captured in section 0.5.1. Sources consulted:

- `https://github.com/spring-projects/spring-boot/releases` — Spring Boot 3.5.x release notes
- `https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes` — Spring Boot 3.5 BOM details
- `https://github.com/xerial/sqlite-jdbc` — `sqlite-jdbc` latest stable version (3.53.1.0)
- `https://central.sonatype.com/artifact/org.xerial/sqlite-jdbc` — Maven Central artifact confirmation
- `https://docs.junit.org/` — JUnit 5 release notes for Jupiter 5.12.x

No best-practice research was conducted on speculative patterns (e.g., "Spring Boot best practices for batch programs", "JDBC connection pooling patterns") because the minimal-change clause restricts the refactor to direct translation; patterns earn their place by being needed, not by being canonical.

### 0.7.5 Tech Spec Reference Citations

The following technical specification sections are the authoritative anchors for claims made throughout this Agent Action Plan:

- **§1.2.1** — Dual-platform educational mission; AS400/ preservation requirement
- **§1.2.2** — OpenCobol/ inventory count (24 .cbl + 1 fixture)
- **§2.1** — Feature catalog F-001 through F-022
- **§2.4.1** — Confirmation of no existing build system, no automated testing
- **§2.4.4** — SQL injection vulnerability documentation
- **§3.4.4** — `ocsqlite` v0.90 attribution and two-step compilation
- **§3.5.2** — F-009 callback-based row buffering description
- **§3.7** — Licensing and compatibility (GPLv3)
- **§5.3.4** — Three compilation paths (standard, raylib, SQLite)
- **§5.3.5** — SQL injection acceptable for educational use only in COBOL form
- **§6.2.3.1** — `trial` table schema and SCREEN SECTION ACCEPT injection point
- **§6.2.3.2** — FILE-STATUS code reference and StatusCode.cbl role


## 0.8 Attachments

No attachments were supplied with the prompt. The `review_attachments` tool returned an empty attachment set (zero PDFs, zero images, zero Figma frames). The `review_rules` tool returned an empty rules array (`[]`), indicating no user-specified implementation rules accompany the prompt.

Consequently:

- **PDF / image attachments:** None.
- **Figma frames / URLs:** None. No design system or visual specification was provided. The DESIGN SYSTEM ALIGNMENT PROTOCOL is not applicable to this refactor (also noted in section 0.2.2).
- **External documents referenced by the prompt:** The prompt itself references the public GitHub repository `https://github.com/Martinfx/Cobol` as the refactor target, and tech-spec sections §1.2.1, §2.4.4, §3.4.4, §3.5, §3.5.2, §5.3, §5.3.4, §5.3.5, §6.2.3.1, and §6.2.3.2 as anchoring context for the existing system. These references are not "attachments" in the project-file sense — they are inline citations within the prompt and the technical specification document being authored.

All in-scope source material for this refactor was discovered through repository inspection of the public GitHub clone of Project COBOL and through `get_tech_spec_section` retrievals of the existing technical specification. No supplementary user-provided files are required to execute the refactor described in sections 0.1 through 0.7.


