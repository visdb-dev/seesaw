# IDE setup for SeeSaw contributors

SeeSaw uses **Maven** as its source of truth for project structure, dependencies,
compiler settings, and builds. Avoid committing IDE-generated project metadata.
The repository-root `pom.xml` is the parent/aggregator for `seesaw`,
`seesaw-demo`, and `contrib` (which has its own child modules).
`seesaw-demo-runner` has a standalone POM and is not in the root reactor.

## Prerequisites

- Install a JDK and a Maven version compatible with the current root `pom.xml`.
  The POM currently sets a **Maven 3.9.6 minimum** and uses the **Frgaal 25**
  compiler with Java 24 source / Java 17 target settings. **JDK 25 is a
  practical choice** for development; check the POM if these settings change.
  Maven's enforcer Java minimum (17) is not the same as the preferred
  development JDK.
- Verify that Maven is using the intended JDK by running `mvn -version`.
  If necessary, configure `JAVA_HOME` before starting Maven or the IDE.

From the repository root, run:

```sh
mvn -version
mvn -DskipTests package
```

Build and test the Maven modules as appropriate for your environment; some
demo/UI tests may need a graphical display.

## Eclipse (m2e)

1. Install Eclipse with Java support and **Maven Integration for Eclipse**
   (m2e).
2. Use **File > Import > Maven > Existing Maven Projects**, selecting the
   **repository root**, and import the detected projects/modules.
3. Set the project's JDK / installed JRE and Maven runtime as needed for the
   current POM. Use **Maven > Update Project** after import.
4. If workspace projects already exist from an earlier checkout, remove the
   affected projects **from the workspace only** (do **not** select the
   option to delete project contents from disk) and import them again.
   Removing tracked `.project`, `.classpath`, or `.settings/` files can
   leave old Eclipse workspace references stale. A clean re-import has
   sometimes been necessary.

Do not commit Eclipse `.project`, `.classpath`, or `.settings/` files:
m2e recreates local configuration from Maven.

## NetBeans

1. Use **File > Open Project** on the repository's Maven root; allow NetBeans
   to discover its Maven modules. If needed, open an individual module's
   `pom.xml` as a Maven project.
2. Configure the project Java platform and Maven installation for the POM.
3. If an existing NetBeans project becomes confused after the IDE files are
   removed, **close and re-open / re-import the Maven project**.
   Back up any personal run actions or other local customizations first.

The removed `nbproject/project.properties` files were **empty** and were
not used as Maven build inputs. NetBeans may regenerate local `nbproject/`
or `nbactions.xml` files; these are ignored by default. This does **not**
mean that Java dependencies with `org.netbeans` in their Maven coordinates
should be removed; they are actual project dependencies.

## IntelliJ IDEA / VS Code

- **IntelliJ IDEA:** open the repository-root `pom.xml` or open the
  repository as a Maven project. Let the IDE import the reactor modules.
- **VS Code:** open the repository folder with Java and Maven extensions
  installed; use the Maven project import/refresh functionality.

IDE-managed `.idea/`, `*.iml`, and `.vscode/` entries are ignored.
If a team deliberately wants to share an IDE configuration (such as
formatting, launch actions, or editor recommendations), discuss and
explicitly whitelist that file rather than committing local workspace state.

## When cleaning up an existing checkout

The Git changes remove **tracked** IDE metadata; they do not necessarily
remove unrelated **untracked** local IDE data or cached workspace state.
Before deleting anything locally, save non-generated customization you need.
If imports are stale, close the project, remove only generated IDE metadata
and/or remove its workspace registration, then import the Maven project anew.
Do **not** delete source directories, `pom.xml`, `.mvn/`, or build-tool
configuration while cleaning IDE files.

Use `git status --short` before committing, and `git check-ignore -v PATH`
to understand why a generated file is ignored.
