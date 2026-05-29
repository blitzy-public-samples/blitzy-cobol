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

This repository is being extended with Java 21 / Spring Boot 3.x translations of the OpenCobol sample programs. The original `.cbl` files under `OpenCobol/` remain in place as the authoritative references; the Java translations live in per-topic Maven sub-modules at the repository root and are added incrementally, one topic at a time.

### Prerequisites

 * Java 21 JDK (for example, Eclipse Temurin 21 — https://adoptium.net/)
 * Apache Maven 3.9 or newer (https://maven.apache.org/download.cgi)

### Currently available sub-modules

The Maven reactor currently builds the following sub-modules:

| Sub-module | Source COBOL | Notes |
|---|---|---|
| `cobol-sort` | `OpenCobol/Sort/*.cbl` (3 files) | Bubble, insertion, and selection sort over `int[]`; three entry classes share one JAR |
| `cobol-string` | `OpenCobol/String/String.cbl` | `String.substring` translation of COBOL reference modification |
| `cobol-struct` | `OpenCobol/Struct/Struct.cbl` | Grouped `OCCURS` arrays via Java arrays / records |

The remaining topics (`cobol-helloworld`, `cobol-conditions`, `cobol-database`, `cobol-date`, `cobol-loops`, `cobol-memory`, `cobol-random`, `cobol-sqlite`) are translated and added in subsequent stages; each is appended to the root `pom.xml` `<modules>` list in the same change that introduces it. The full target catalog is listed at the end of this section.

### Build all available sub-modules

```sh
git clone https://github.com/Martinfx/Cobol.git
cd Cobol
mvn clean package -DskipTests
```

This produces an executable Spring Boot JAR per sub-module under `cobol-<topic>/target/cobol-<topic>-1.0.0.jar`.

### Run a sample

```sh
java -jar cobol-string/target/cobol-string-1.0.0.jar
```

### Running a multi-entry sub-module (`cobol-sort`)

`cobol-sort` contains three Spring Boot entry classes that share a single JAR. Running the JAR with no extra options runs the default entry class (`BubbleSortApplication`):

```sh
java -jar cobol-sort/target/cobol-sort-1.0.0.jar
```

The JAR is packaged with Spring Boot's `PropertiesLauncher`, so a non-default entry class can be selected at run time with `-Dloader.main=<fully.qualified.ClassName>`:

```sh
java -Dloader.main=com.projectcobol.sort.InsertSortApplication -jar cobol-sort/target/cobol-sort-1.0.0.jar
java -Dloader.main=com.projectcobol.sort.SelectSortApplication -jar cobol-sort/target/cobol-sort-1.0.0.jar
```

Each entry class disables sibling component scanning, so exactly one COBOL sort translation runs per invocation.

### Run the test suite

```sh
mvn test
```

Each sub-module ships golden-output fixtures under `src/test/resources/expected/<ClassName>.txt`. JUnit 5 golden-output test classes — which capture `System.out` and compare it against the matching fixture — are added alongside each module; `mvn test` runs whatever tests are present on the branch.

### Target sub-module catalog

The complete set of planned Java sub-modules (delivered incrementally) is:

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
