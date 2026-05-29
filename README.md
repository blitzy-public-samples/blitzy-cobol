Project COBOL
=============
- Examples for OpenCobol (Gnu Cobol)
- Examples for AS400, PF, LF, DSPF
- Examples for ILE Cobol, ILE CL

### AS400 ILE COBOL
   * ILE COBOL 
   * ILE CL 
   * Physical files
   * Logical files
   * Display files
   
### OpenCOBOL/GNUCobol
   * Hello Cobol
   * Conditions	
   * Database	
   * Date
   * Games
   * Loops	
   * Memory	
   * Random	
   * SQLite	
   * Sort	
   * String
   * Struct 
   
#### Compilation with GnuCobol compiler

```sh
cobc -x -W -Wall -O2 HelloWorld.cbl
```

### For contributions 
 * Contribution are welcome !

-----------------------------------------------------------

### IDE for OpenCobol
 * https://launchpad.net/cobcide/+download

Support SQL Database
============

### SQLITE 
 * https://github.com/Martinfx/SQLiteCobol

### DBPRE
 * https://github.com/Martinfx/DBPRE

License
==========
>You can check out the full license [here](https://github.com/Martinfx/Cobol/blob/master/LICENSE)

This project is licensed under the terms of the **GNU General Public License v3.0** license.

Java 21 / Spring Boot Build
===========================

This repository now provides Java 21 / Spring Boot 3.x translations of every translatable OpenCobol sample under `OpenCobol/`. The original `.cbl` files remain in place as authoritative references. The Java translations live in eleven per-topic Maven sub-modules at the repository root.

### Prerequisites

 * Java 21 JDK (for example, Eclipse Temurin 21 — https://adoptium.net/)
 * Apache Maven 3.9 or newer (https://maven.apache.org/download.cgi)

### Build all sub-modules

```sh
git clone https://github.com/Martinfx/Cobol.git
cd Cobol
mvn clean package -DskipTests
```

This produces an executable Spring Boot JAR per sub-module under `cobol-<topic>/target/cobol-<topic>-1.0.0.jar`.

### Run a sample

```sh
java -jar cobol-helloworld/target/cobol-helloworld-1.0.0.jar
```

Some sub-modules (for example `cobol-conditions`, `cobol-database`, `cobol-sort`) contain multiple Spring Boot entry classes that share a single JAR. To run a non-default entry class, pass `-Dloader.main=<fully.qualified.ClassName>` or rebuild that sub-module with `-Dstart-class=<FQCN>`.

### Run the JUnit 5 test suite

```sh
mvn test
```

The test suite uses JUnit Jupiter 5 golden-output assertions: each entry class is invoked and its captured `System.out` is compared against `src/test/resources/expected/<ClassName>.txt`.

### Sub-module catalog

| Sub-module | Source COBOL | Notes |
|---|---|---|
| `cobol-helloworld` | `OpenCobol/HelloWorld.cbl` | The minimal "Hello world!" smoke test |
| `cobol-conditions` | `OpenCobol/Conditions/*.cbl` (8 files) | Predicate-pattern demonstrations |
| `cobol-database` | `OpenCobol/Database/*.cbl` (4 files) | Sequential file I/O with `BufferedReader`; ships a `FileStatus` enum mapping COBOL `FILE-STATUS` codes |
| `cobol-date` | `OpenCobol/Date/DateAndTime.cbl` | `LocalDate` / `LocalTime` translation of COBOL `ACCEPT FROM DATE/TIME` |
| `cobol-loops` | `OpenCobol/Loops/*.cbl` (2 files) | `PERFORM UNTIL` → `while`, `PERFORM VARYING` → `for` |
| `cobol-memory` | `OpenCobol/Memory/*.cbl` (2 files) | COBOL `POINTER` / `LINKAGE SECTION` modeled with `String.substring` views |
| `cobol-random` | `OpenCobol/Random/*.cbl` (2 files) | `java.util.Random` seeded from `System.currentTimeMillis() % 86400` |
| `cobol-sqlite` | `OpenCobol/SQLite/Hello_SQLITE.cbl` | `org.xerial:sqlite-jdbc` replaces `ocsqlite`; uses `PreparedStatement` parameter binding for SQL safety |
| `cobol-sort` | `OpenCobol/Sort/*.cbl` (3 files) | Bubble, insertion, and selection sort over `int[]` |
| `cobol-string` | `OpenCobol/String/String.cbl` | `String.substring` translation of COBOL reference modification |
| `cobol-struct` | `OpenCobol/Struct/Struct.cbl` | Grouped `OCCURS` arrays via Java arrays / records |

The `OpenCobol/Games/raylib/*.cbl` programs are intentionally excluded from Java translation. See `OpenCobol/Games/README.md` for the rationale.

### Java code license

Every new Java source file inherits the project's GNU General Public License v3.0. The `LICENSE` file at the repository root is the authoritative license text.
