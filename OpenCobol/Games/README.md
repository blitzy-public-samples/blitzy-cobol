Games (raylib) — Excluded from Java Translation
===============================================

This folder contains two GnuCOBOL programs that drive the
[raylib](https://www.raylib.com/) C graphics library:

 * `raylib/core_basic_window.cbl`
 * `raylib/core_random_values.cbl`

Why these programs have no Java / Spring Boot translation
--------------------------------------------------------

The Java 21 / Spring Boot 3.x effort in this repository is a **direct, minimal
translation** of the OpenCobol console samples. Every other `OpenCobol/` topic is
a single-process program that reads from `stdin` and writes to `stdout`, which
maps cleanly onto a Spring Boot `CommandLineRunner`.

The `raylib` samples are different: they are bindings to a native C graphics
library and open an interactive GPU-accelerated window. raylib has **no idiomatic
Java equivalent**, so reproducing these programs in Java would require choosing
and integrating an unrelated Java graphics/game framework (for example, a
JNI/Panama raylib binding, LWJGL, JavaFX, or libGDX) and re-implementing the
window, render loop, and input handling from scratch.

That would constitute **new feature work**, not a translation, and would violate
the project's minimal-change refactoring principle (translate existing behavior
faithfully; do not add capabilities). For that reason the `raylib` programs are
intentionally left out of the Java build.

Authoritative reference
-----------------------

The original `.cbl` files in `raylib/` remain in place and are the authoritative
raylib reference for this repository. They continue to be compiled with the
GnuCOBOL raylib build path documented for the OpenCobol examples; nothing here is
deleted or modified.

Dual-platform educational mission
---------------------------------

Project COBOL deliberately preserves both its OpenCobol/GnuCOBOL and AS400 (IBM i)
material side by side for teaching purposes. Keeping the raylib COBOL sources in
this folder — even without a Java counterpart — maintains that complete,
dual-platform educational catalog.
