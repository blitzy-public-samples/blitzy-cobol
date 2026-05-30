# OpenCobol Games — raylib Bindings

*This folder preserves the original COBOL raylib binding examples; it is intentionally NOT part of the Java 21 / Spring Boot translation that produces the `cobol-<topic>/` sub-modules at the repository root.*

The two COBOL programs `OpenCobol/Games/raylib/core_basic_window.cbl` and `OpenCobol/Games/raylib/core_random_values.cbl` are intentionally excluded from the Java 21 / Spring Boot 3.x translation effort. That effort converts the rest of the `OpenCobol/` branch into per-topic Maven sub-modules named `cobol-<topic>/` at the repository root, but it deliberately leaves these two programs alone. Both `.cbl` files remain in place under `OpenCobol/Games/raylib/` and are NOT modified or deleted; they continue to serve as the authoritative raylib reference for Project COBOL.

## Why raylib Is Not Translated

raylib is a C graphics library ([raylib](https://www.raylib.com)). The two COBOL programs bind to it through the C application binary interface by issuing COBOL `CALL` statements directly to raylib functions, passing parameters either `BY VALUE` (window dimensions, the 60 FPS target, key flags, and the random-range bounds) or `BY REFERENCE` (the window-title string and the RGBA color structures). Between them, the two examples exercise nine raylib functions: `InitWindow` initializes the application window with a width, height, and title; `SetTargetFPS` locks the render loop to a fixed frame rate (60 FPS in both examples); `WindowShouldClose` polls the operating-system event queue for an exit signal such as the ESC key or the window-close button; `BeginDrawing` opens a per-frame drawing scope; `ClearBackground` paints the canvas with a single RGBA color; `DrawText` renders a text string at fixed pixel coordinates with a chosen font size and color; `EndDrawing` closes the per-frame drawing scope and swaps the display buffers; `CloseWindow` tears down the window and frees the native resources; and `GetRandomValue` returns a pseudo-random integer in the inclusive range `[min, max]` (used only by `core_random_values.cbl`). Each program follows the same lifecycle: an `INIT-WINDOW` section, then a `MAIN-LOOP` section that repeats until `WindowShouldClose` reports an exit, then a `CLOSE-WINDOW` section, with the initial `InitWindow` call guarded by an `ON EXCEPTION` handler that reports a missing raylib library.

The Java ecosystem has no first-class, COBOL-pedagogically-aligned binding for raylib. These two programs depend on direct C-API calls that have no equivalent anywhere in the Java standard library. Translating them to Java would require first selecting a Java graphics stack — for example `java.awt`, `javafx`, `LWJGL`, or a third-party raylib JNI binding — and then re-implementing the 60 FPS render loop, the RGBA color model, and the exception-guarded window initialization from scratch. None of those pieces exists in the source COBOL in a form that a Java equivalent could mirror line-for-line; each would have to be designed anew.

That kind of work is new feature work, not a refactor. It would introduce a new graphics dependency, a new event loop, a new threading model, and behavioral semantics that the original `.cbl` programs never expressed. The refactor that produces the `cobol-<topic>/` sub-modules is governed by a minimal-change principle: make only the changes absolutely necessary for the COBOL-to-Java migration, preserve existing functionality exactly, and forbid speculative enhancements. Translating the raylib examples would violate that principle, so the two raylib `.cbl` files are intentionally left out. By contrast, every other topic folder under `OpenCobol/` — HelloWorld, Conditions, Database, Date, Loops, Memory, Random, SQLite, Sort, String, and Struct — is translated, because each of those programs is a single-process console application whose COBOL semantics map cleanly onto the Java standard library and Spring Boot 3.x.

## Authoritative raylib Reference

The two preserved programs, `OpenCobol/Games/raylib/core_basic_window.cbl` and `OpenCobol/Games/raylib/core_random_values.cbl`, remain in place, unmodified, as the authoritative raylib reference within Project COBOL. Readers interested in COBOL-to-raylib integration should consult these original `.cbl` sources directly rather than looking for a Java counterpart, because none exists. The programs are built with the GnuCOBOL compiler against the system raylib library using the command recorded in their file headers, `cobc -xjd core_basic_window.cbl -lraylib`, which is a useful starting point for anyone who wants to compile and run the originals.

## Dual-Platform Educational Mission

Project COBOL deliberately preserves two complete sample sets side by side for teaching purposes: the `OpenCobol/` GNU COBOL examples and the `AS400/` IBM i examples. The Java translation is added alongside these branches in new `cobol-<topic>/` sub-modules — it does not replace them, and neither original branch is altered by the migration. Keeping the raylib COBOL sources in this folder, even though they have no Java counterpart, preserves the educational value of the COBOL-to-raylib binding demonstration for learners who want to study direct C-API integration patterns from COBOL.

## Translated Topics

The following eleven topics under `OpenCobol/` are translated into Java sub-modules at the repository root (the order matches the `<modules>` block of the root `pom.xml`):

1. `cobol-helloworld` — minimal `Hello world!` smoke test
2. `cobol-conditions` — predicate-pattern demonstrations (eight programs)
3. `cobol-database` — sequential file I/O and the `FileStatus` enum (four programs)
4. `cobol-date` — date and time formatting
5. `cobol-loops` — `while` and `for` translations of `PERFORM UNTIL` / `PERFORM VARYING`
6. `cobol-memory` — `POINTER` / `LINKAGE SECTION` modeled with `String.substring` views (two programs)
7. `cobol-random` — `java.util.Random` translation of `FUNCTION RANDOM` (two programs)
8. `cobol-sqlite` — `sqlite-jdbc` translation of the `ocsqlite` binding; includes the SQL-injection security fix via `PreparedStatement`
9. `cobol-sort` — bubble, insertion, and selection sort (three programs)
10. `cobol-string` — `String.substring` translation of COBOL reference modification
11. `cobol-struct` — grouped `OCCURS` arrays via Java arrays and records

For build, run, and test instructions, see the root `README.md`; it is the canonical source for the Maven commands and is not duplicated here.

## License

This README and the entire Project COBOL repository are governed by the GNU General Public License v3.0; the `LICENSE` file at the repository root is the authoritative license text. No separate license header is required for Markdown documentation files — only Java source files (`.java`) carry per-file GPLv3 header comments — so this document intentionally omits any license-header block. The `LICENSE` file itself is preserved verbatim by this refactor.
