# seesaw-demo-datasets-plan

Nov 19, 2026 · @Ernie Rael

> This is the plan as agreed in review, kept for its reasoning. It was implemented in
> PR #56; the code and seesaw-demo/README.md are authoritative. Where they differ:
> - html-restricted has 11 ss_label records; the two that break Swing under
>   `--rules=PASS` are records 10 and 11 (frameset, and 600 nested tables), not 8 and 9.
> - `DemoDataset` has no `open()`; it has `resolve(script, server)` and
>   `fallbacks(scripts, server)`, and the setup classes open the resolved paths.
> - `DemoOptions` holds the database options and the dataset; `--rules` is applied in
>   `main()` with `Globals.setOption`, so rules and style are not in the record.

## Summary

Plan, updated after the review of 2026-11-19. A `db script set` is called a **dataset**. Each named dataset is a resource directory, `datasets/<name>/`. You select one with `--dataset=<name>`; the default, `""`, uses today's files where they are. Every script and load-list lookup goes through one small `DemoDataset` class. There are three html datasets, `html-inline`, `html-multiline` and `html-restricted`, and a `--rules=<name>` option picks the SsHtml the demo puts in `Globals`. Both options have static defaults in `main()`, and the demo never relies on a library default. All changes stay in seesaw-demo, and no code has been written yet.

Decided:

1. Name: `dataset`.
2. Layout: one directory per dataset.
3. A script missing from a named dataset falls back to the unnamed file. The fallback is logged and also shown in a modal dialog that lists the files. `--dataset-check` reports the fallbacks of every dataset.
4. The html datasets are full copies of the base scripts.
5. `--rules=NAME` picks the SsHtml from a `DemoRules` table, where adding a value is one line. `PASS` and `PLAIN` count as rules values.
6. MainClass: reorganize the database part, with parsed options collected in an immutable record. MySQL and external databases keep today's behavior; H2 is the main path.

## How the demo loads its database today

There are two load paths with different code, and the script names live in three places. From `MainClass.java` at commit 24b6195:

| Path | When | Scripts | How found | Runner |
| --- | --- | --- | --- | --- |
| `getDatabase()` | default, `dbname == "h2"` | `suppliers_and_parts.sql`, `tests.sql`, `debug.sql` (off), chosen by `RUN_*_SQL_SCRIPTS` booleans | `getClassLoader().getResourceAsStream(name)` | H2 `RunScript.execute` |
| `MysqlSetup.run()` | positional `mysql` | `/mysql.demo-app.sql`, `/mysql.demo-components.sql` | `MainClass.class.getResourceAsStream("/" + name)` | `DemoUtil.runSqlStatements` (splits on `;`) |
| `ExternalSetup.run()` | `-p props -s file.sql` | user files | `FileReader` | `DemoUtil.runSqlStatements` |
| `H2Setup` | only for `-d` dump | `suppliers_and_parts.sql`, `tests.sql`, `tests_load_blobs.sql` | as MySQL | none, `run()` is empty |

After the scripts, both real paths load images with `DemoUtil.loadBinaries(conn, "/demo-images.txt", ...)` unless `-i`.

Things worth fixing on the way:

- `H2Setup` lists `tests_load_blobs.sql` for dump, but the H2 load never runs it, because images come from `demo-images.txt`. That is on purpose and stays: the script is for someone who runs the dumped scripts themselves, for example against a persistent H2 database.
- The H2 list is written twice (the `RUN_*` blocks and `H2Setup.getScripts()`), and the special case `if ("h2".equals(dbname)) getDatabase() else databaseSetup.run()` exists only because `H2Setup.run()` is a stub.
- Options are mutable statics (`verbose`, `dump`, `no_load_images`, ...) read from inside the setup classes.

No example loads data of its own. `H2Demo` runs SQL from a string, and the examples only `SELECT` and call `nextval`. So the scripts and `demo-images.txt` named above are the whole surface a dataset has to cover.

## Part 1: selectable datasets

### Name

**Dataset** (agreed): "the demo's `html-inline` dataset", `--dataset=html-inline`, class `DemoDataset`. Each dataset has a description, shown by `--dataset-list`; the programmer writes it with care, since it is how a user learns what a dataset is for.

### Prefix or directory

A directory per dataset (agreed), with the unnamed dataset staying where the files are now.

|  | Directory `datasets/html-inline/suppliers_and_parts.sql` | Prefix `html-inline-suppliers_and_parts.sql` |
| --- | --- | --- |
| Resource folder | one folder per set; the root stays as it is | every set adds files to one flat folder |
| Comparing sets | `diff -r` of two folders; same file names | file names differ per set |
| Set-specific extras | a set can hold its own `demo-images.txt`, images, a `README.txt` | names get long |
| Separator | none needed | `-` is ambiguous: `html-inline-tests.sql` |
| Knowing a set exists | look up `datasets/<name>/dataset.properties` | must probe for some known file |
| Moving current files | not needed: `""` maps to the root | not needed |

The base files stay at the resource root, so nothing that names them today (README, `-d` dump, the MySQL path) changes.

Resolution of a logical name such as `suppliers_and_parts.sql`:

1. Dataset `""`: `/suppliers_and_parts.sql`, exactly as today.
2. Dataset `html-inline`: `/datasets/html-inline/suppliers_and_parts.sql` if present, else fall back to `/suppliers_and_parts.sql`. So a dataset holds only the files it changes; `debug.sql` and `demo-images.txt` come from the root unless a set overrides them.
3. A name that is not in the dataset list is an error at startup, which prints the list.

A fallback is reported two ways, because a log line is easy to miss. Each file is logged at INFO. And before anything is loaded, if any file of a named dataset would fall back, a modal dialog lists those files with OK and Cancel; Cancel exits the demo. This works because resolving is only a lookup, so every fallback is known before the connection is opened. With \`-d\`, which is not interactive, the list goes to stderr instead of a dialog. `--dataset-check` prints, for every dataset in the list, which of its files are its own and which fall back, then exits.

### The dataset list

One resource, `/datasets/datasets.txt`, names the datasets and gives each a description. It stays inside the jar and is read with `getResourceAsStream`; nothing is extracted. It is needed because code cannot list the entries of a resource directory inside a jar. There is no per-directory `dataset.properties`, and a dataset carries no hint for `--rules`.

```text
# name: description          (the unnamed dataset "" is always present)
html-inline: Inline html (Rules.INLINE) in names, cities and labels. Accepted html renders on one line; multiline html is refused.
html-multiline: html-inline plus multiline and block html (Rules.MULTILINE): br, p, lists, tables, headings, pre.
html-restricted: html-multiline plus html no built-in Rules allows: img, style, form, frameset. Records 8 and 9 break Swing with --rules=PASS.
```

`--dataset-list` prints the names and descriptions and exits.

### Selecting a dataset

- In code: `private static String dataset = "";` near the other defaults in `MainClass`, so `main()` can change it, as it can the decorator style.
- Command line: long option `--dataset=NAME`, next to `--style`, with no short option (`-d` is dump). Also `--dataset-list` and `--dataset-check`, which print and exit.
- Run with maven: `mvn exec:exec -Dargs="--dataset=html-inline"`.
- `-s` (external SQL files) together with `--dataset` is an error, as `-s` with a server is today.
- `-d` dumps the selected dataset, named `dump.<dataset>.suppliers_and_parts.sql`, or `dump.suppliers_and_parts.sql` for `""`.
- MySQL uses the same lookup. The html datasets have no MySQL files, so `mysql --dataset=html-inline` falls back to the plain MySQL data, with the log line and the dialog.
- The main window title shows a named dataset: `SeeSaw Demo [html-inline]`.

### Loader abstraction

Only `DemoDataset` knows the layout. Everything else asks for a logical name.

```java
/** The scripts and load lists that fill the demo database. */
final class DemoDataset {
  static final String UNNAMED = "";

  /** @throws IllegalArgumentException if the name is not in datasets.txt */
  static DemoDataset of(String name);
  static List<DemoDataset> available();       // from datasets/datasets.txt

  String name();
  String description();

  /** Resource path used for a script on a server, after fallback. */
  String resolve(DemoScript script, DbServer server);
  /** Open it; the log says which file was used, and a fallback is remembered. */
  Reader open(DemoScript script, DbServer server) throws IOException;
  /** The fallbacks so far, for the dialog. */
  List<String> fallbacks();
}
```

Logical names are constants, so typos fail at compile time. Each one knows its file name per database server, which keeps today's file names out of the rest of the demo. Renaming the files later for consistency (for example `mysql.demo-components.sql` to `demo_components.sql` in a `mysql/` directory) then touches only this enum.

```java
enum DemoScript {
  //                   H2                          MySQL
  SUPPLIERS_AND_PARTS("suppliers_and_parts.sql", "mysql.demo-app.sql"),
  COMPONENTS(         "tests.sql",               "mysql.demo-components.sql"),
  COMPONENTS_BLOBS(   "tests_load_blobs.sql",    null),   // H2 dump only
  DEBUG(              "debug.sql",               null),
  DEMO_IMAGES(        "demo-images.txt",         "demo-images.txt");
  ...
  String fileName(DbServer server);
}
```

`DbServer` is a small new enum, `H2` and `MYSQL`, for the database server the demo was started with: the positional argument, or `h2` when none is given. It replaces the string constants `DBMS_H2` and `DBMS_MYSQL`, and it is the column `DemoScript` picks a file name from. External (`-s`) is not a `DbServer`, because its files come from the command line and not from resources. Adding a server later means a new constant and a new column.

`DemoUtil.loadBinaries` takes a `Reader` (or the resolved path) instead of the hard-coded `"/demo-images.txt"`. The image files named inside it stay absolute resource paths, so a dataset may point at the root images or its own.

### MainClass reorganization

The database part only (agreed). The H2 path is the one used almost all the time and is the one reworked; MySQL and external databases keep doing what they do today. Each step builds and runs the demo with the same data as before.

1. Move `DatabaseSetup`, `ExternalSetup`, `MysqlSetup` and `H2Setup` out of `MainClass` into a new file `DemoDatabase.java` (package private). Give the H2 class a real `run()` that holds what `getDatabase()` does now, so `MainClass()` calls `databaseSetup.run()` for every server and the `"h2".equals(dbname)` branch goes away.
2. Each setup returns its `List<DemoScript>` to load and its list to dump. H2's load list replaces the `RUN_DEMO/TEST/DEBUG_SQL_SCRIPTS` booleans; its dump list still includes `COMPONENTS_BLOBS`. One open path through `DemoDataset` replaces the two `getResourceAsStream` styles.
3. The statics stay as the editable defaults, and getopt still sets them. After parsing, `main()` copies them into an immutable `DemoOptions` record (`verbose`, `dump`, `noLoadImages`, `noInitializeDb`, `propertyFile`, `userSqlFiles`, `dataset`, `rules`, `style`) and passes it to the setup class, which no longer reads `MainClass` statics.
4. Not now: the example buttons are each touched in 5 places (field, listener, size, layout, an if/else branch) and could become one list of `(label, Example::new)`.

## Part 2: html datasets

Three datasets, each a copy of `suppliers_and_parts.sql` and `tests.sql` with html added, and each a superset of the one before: `html-inline` (Rules.INLINE only), `html-multiline` (adds multiline, Rules.MULTILINE), `html-restricted` (adds what no built-in Rules allows). In each, only some rows get html, so plain, accepted and refused text alternate in the same column.

### Where html is actually rendered

Swing renders html only in labels and renderers. In the demo that is SsLabel, the SsDbComboBox2 list, and SsTable cells, including its ComboRenderer. Text fields and text areas always show the markup literally, which is useful as a contrast and for typing new html in.

| Column | Rendered in | Literal in |
| --- | --- | --- |
| `part_data.part_name` | Example3 and TestBaseComponents part combo, Example4 part selector combo, Example5/6 grid cells, Example7 grid ComboRenderer | Example4 text field |
| `supplier_data.supplier_name` | Example3 supplier combo, Example7 grid ComboRenderer | Example1/2 text fields |
| `part_data.city` | Example5/6 grid cells | Example4 text field |
| `base_test_data.ss_label` | TestBaseComponents SsLabel; the only label, so the main place for multiline and restricted html |  |
| `supplier_data.city`, `base_test_data.ss_text_area`, `ss_text_field` | nowhere | Example1/2, TestBaseComponents |

For the SsDbComboBox2 combos, "rendered" means the popup list only. With ModelType.GLAZED, GlazedLists makes them editable, so the closed combo shows the selected item in a text field, literally (see Set 2). With ModelType.SWING the combo is not editable and the closed combo goes through the renderer too. The model type is a compile-time choice today, so a run of the html datasets covers one of the two.

The combo and list items in TestBaseComponents (`comboItems`, `listItems`) are hard-coded in Java, not read from the database, so the datasets cannot reach them. The navigator combos show primary keys.

### Common to all three

- Widen the html columns: `supplier_name`, `part_name`, `city` and `ss_label` go from `VARCHAR(50)` to `VARCHAR(2000)`; the 50 limit would reject most samples.
- Write attribute values in double quotes so the SQL literal needs no `''` escaping: `'<html><font color="blue">Athens</font>'`.
- Ordering: Example4/5/6/7 `ORDER BY part_name` or `supplier_name`, so rows starting with `<html>` sort together. That is expected; leaving some rows plain keeps the order readable.
- Entities such as `&amp;` contain `;`. That is fine for H2's `RunScript` but would be split by `DemoUtil.runSqlStatements`; another reason the html sets are H2 only for now.

### Set 1: `html-inline`

Only `b i u s strike em strong small big sub sup tt code font span` with `color size face`, so accepted html always renders on one line. Everything renders with `--rules=INLINE`, `MULTILINE` or `BROAD`. Includes three edge cases that are not html at all, so they show literally whatever `--rules` is.

```sql
-- supplier_data: Example3 combo, Example7 grid
MERGE INTO supplier_data VALUES (2,'<html><b>Jones</b>', 10, 'Paris');
MERGE INTO supplier_data VALUES (5,'<html><i>Adams</i> &amp; Sons', 30, '<html><i>Athens</i>');
MERGE INTO supplier_data VALUES (4,'Clark', 20, 'London');
MERGE INTO supplier_data VALUES (6,'<html>Anurak<sup>TM</sup>', 73, 'Bankok');
MERGE INTO supplier_data VALUES (3,'<html><font color="#c00000" face="Serif">Blake</font>', 30, 'Paris');
MERGE INTO supplier_data VALUES (1,'<HTML><u>Smith</u>', 20, 'London');     -- upper case is html
MERGE INTO supplier_data VALUES (8,' <html><b>Gupta</b>', 71, 'Mumbai');    -- leading space: not html
MERGE INTO supplier_data VALUES (7,'<html lang=uk><b>Kozar</b>', 79, 'Kyiv'); -- attribute: not html

-- part_data: combos and grids
MERGE INTO part_data VALUES (6,'<html><b>Cog</b>', 0, 19, 'London');
MERGE INTO part_data VALUES (5,'<html><font color="green">Cam</font>', 2, 12, '<html><i>Paris</i>');
MERGE INTO part_data VALUES (4,'<html>Screw <small>M4</small>', 0, 14, 'London');
MERGE INTO part_data VALUES (3,'<html>Screw <tt>M6</tt> <strike>old</strike>', 2, 17, '<html><span color="blue">Rome</span>');
MERGE INTO part_data VALUES (2,'<html><big>B</big>olt<sub>2</sub>', 1, 17, 'Paris');
MERGE INTO part_data VALUES (1,'Nut', 0, 12, 'London');

-- base_test_data.ss_label (SsLabel); ss_text_area stays literal
--   1: '<html><b>Label</b> <i>1</i>'
--   3: '<html><font size="+1" color="purple">Label 3</font>'
--   5: '<html><code>Label</code> <em>5</em> <strong>!</strong>'
--   ss_text_area 1: '<html><b>Text Area</b> markup is shown literally here'
```

### Set 2: `html-multiline`

Set 1 plus multiline and block html from Rules.MULTILINE (`br p div center pre blockquote hr h1-h6 ul ol li dl dt dd table tr td th caption`, `align valign width height border bgcolor colspan rowspan cellpadding cellspacing nowrap`). It renders with `--rules=MULTILINE` or `BROAD`. With `--rules=INLINE` the multiline rows show literally and one warning per column is logged; that is the main INLINE versus MULTILINE comparison.

"Multiline" covers both kinds: html that renders on several lines, and SQL literals that contain newlines.

```sql
-- ss_label (SsLabel grows to fit)
--   1: '<html>Line one<br>Line two<br><i>Line three</i>'
--   2: '<html><h2>Heading</h2>followed by text<hr>'
--   3: '<html><ul><li>alpha<li>beta<li>gamma</ul>'
--   4: '<html><table border="1" cellpadding="2"><tr><th>a</th><td bgcolor="yellow">b</td></tr></table>'
--   6: a literal written over several lines in the .sql file:
'<html>
<p>First paragraph,
newlines here are just spaces.</p>
<pre>  pre keeps
    the layout</pre>'
--   7: nesting just under the limit (32), built with H2 REPEAT:
'<html>' || REPEAT('<div>', 30) || 'deep' || REPEAT('</div>', 30)

-- part_data.part_name: two lines
MERGE INTO part_data VALUES (6,'<html>Cog<br><small>sprocket</small>', 0, 19, 'London');
-- supplier_name
MERGE INTO supplier_data VALUES (3,'<html><div align="right">Blake</div>', 30, 'Paris');
```

The two-line part name is there to compare components: a combo popup cell should grow to two lines, while an SsTable row keeps its height and clips the second line. SsDbComboBox2 installs GlazedLists autocompletion, which makes the combo editable. So the closed combo shows the selected item in its editor, a JTextField: markup shows literally, and `filterNewlines` turns a newline into a space (you fixed GlazedLists' DocumentFilter to honor it). Only the popup list renders html. The `pre` row with real newlines shows both effects. Typing in the editor matches against the stored text, markup included.

### Set 3: `html-restricted`

Set 2 plus html that none of Rules.INLINE, MULTILINE or BROAD allows. With any of those, each refused row shows literally and logs one WARNING per column with what was refused. Refused rows sit between accepted ones, which exercises the presenter turning `html.disable` on and off again on a shared renderer and on the SsLabel as you navigate. The last column says what Swing does with the row when nothing checks it, `--rules=PASS`.

| Row (ss\_label unless noted) | Html | Refused as | Under `--rules=PASS` |
| --- | --- | --- | --- |
| part\_name 5 | `<html><img src="https://raw.githubusercontent.com/visdb-dev/seesaw/main/seesaw-demo/src/main/resources/sample_image_1.png" width="16" height="16"> Cam` | `img`, `img@src` | fetches the project's own image; in a grid, watch whether repaints fetch again |
| part\_name 3 | `<html><img src="http://host.invalid/beacon.png"> Screw` | `img`, `img@src` | a host that never resolves (RFC 6761); shows the stall on the event thread |
| 1 | `<html><body background="https://raw.githubusercontent.com/.../sample_image_2.png">Background</body>` | `body@background` | background fetch |
| 2 | `<html><span style="color:red">styled</span>` | `span@style` | red text |
| 3 | `<html><head><style>@import url(https://raw.githubusercontent.com/.../x.css);</style></head>import` | `style` | stylesheet fetch |
| 4 | `<html><a href="https://example.com">a link</a>` | `a`, `a@href` | underlined, not clickable |
| 5 | `<html><form><input type="text" value="user"><input type="password"><input type="submit"></form>` | `form`, `input` | real fields and a button inside the label |
| 6 | `<html><title>T</title>body text` | `title` | a component in the label |
| 7 | `'<html>' \|\| REPEAT('<div>', 40) \|\| 'too deep' \|\| REPEAT('</div>', 40)` | `(depth)` | renders |
| supplier\_name 6 | `<html><object classid="javax.swing.JButton"></object>Anurak` | `object` | nothing, unless `-Dswing.html.object=true` |

#### What can throw or crash, and in which components

Nothing that Rules.INLINE, MULTILINE or BROAD allows throws or crashes. `RulesRenderTest` renders everything each constant allows, alone and nested in each other, and fails on an exception, a fetch or a component in the label. Swing's two crash hazards are outside those lists, or are stopped by the checker's limits:

| Hazard | Cause | Allowed by a Rules constant | Stopped by |
| --- | --- | --- | --- |
| `setText` throws a RuntimeException | `frame`, `frameset`, `noframes` | no | the element list |
| StackOverflowError while rendering | nesting depth: fine at 256, failed at 512 nested tables | yes, the elements (`table`, `ul`, `blockquote`, ...) are in MULTILINE | `maxDepth` 32 and `maxLength` 32768 |

So a crash needs `--rules=PASS`, or a `DemoRules` entry a programmer adds that allows `frameset` or raises the limits.

Every component that renders html is equally sensitive, because each is a JLabel underneath. SsLabel is a JLabel. SsTable's TextRenderer and ComboRenderer, and the combo and list renderers, extend Swing's JLabel-based renderers. They all use the same `BasicHTML` view. SsLabel has no extra protection: `updateSsComponent()` calls `setText(presenter.prepare(...))` with no catch, so behind PASS it throws like any JLabel. It is safe with anything only because the checker is in front of it. What differs between components is when the html is parsed:

| Component | When html is parsed | A throwing or too-deep row under PASS |
| --- | --- | --- |
| SsLabel | at `setText`, when the record changes | one exception on the event thread each time you navigate to the row |
| SsTable cell, ComboRenderer | on every paint of the cell | repeats on every repaint; the grid may stop painting |
| SsDbComboBox2, SsList1 items | on every paint of the item, and when the popup is sized | repeats while the popup is open |
| Text fields, text areas | never | nothing; shown literally |

The SsLabel row was measured: the analysis and `RulesRenderTest` use a JLabel. The renderer rows are inferred from how renderers work, not measured, and the restricted dataset will show them. Fetches follow the same pattern: once per `setText` for a label, and possibly on every paint for a cell, unless Swing's image cache answers.

The two crashing rows go only in `ss_label`, as records 8 and 9 at the end. Under PASS you reach them only by navigating there in TestBaseComponents, and no grid or combo shows them:

- 8: `<html><frameset><frame></frameset>`: refused as `frameset`; under PASS `setText` throws.
- 9: `'<html>' || REPEAT('<table><tr><td>', 600)`: refused as `(depth)`; under PASS a StackOverflowError. 600 levels, about 9 KB, is past the measured 512 and under the length limit, so this row tests depth alone.

All network targets are the project's own GitHub files or `.invalid`, so nothing reaches a third party. No `file:` or UNC paths.

### The `--rules` option

`--rules=NAME` picks the SsHtml that `main()` puts in `Globals`. Like `--dataset`, it has a static default in `MainClass`. The demo never relies on the library's default SsHtml, which may change, perhaps per component type: `main()` always calls `Globals.setOption(SsHtml.class, ...)` before `SeeSaw.init()`.

_Changed 2026-11-20:_ the `SsHtml` in `Globals` is now an override of the library's per-type defaults (PR 59). Without `--rules`, the demo sets nothing and the library's defaults apply; `--rules=NAME` sets the override for every component.

```java
private static String dataset = "";
private static String rules = "MULTILINE";
```

The values live in one table, `DemoRules`. `PASS` and `PLAIN` are SsHtml kinds rather than Rules, but they are rules values too, so one option covers every choice. Adding a value is one line:

```java
final class DemoRules {
  private static final Map<String, Supplier<SsHtml>> CHOICES = new LinkedHashMap<>();
  static {
    add("PASS",        SsHtml::pass);
    add("PLAIN",       SsHtml::plain);
    add("INLINE",      () -> SsHtmlChecker.get(Rules.INLINE));
    add("MULTILINE",   () -> SsHtmlChecker.get(Rules.MULTILINE));
    add("BROAD",       () -> SsHtmlChecker.get(Rules.BROAD));
    add("IMG",         () -> SsHtmlChecker.get(Rules.MULTILINE.withElements("img")
                                 .withAttributes("img@src", "img@width", "img@height")));
    add("INLINE-TEXT", () -> SsHtmlChecker.get(Rules.INLINE.withOnRefusal(OnRefusal.TEXT_ONLY)));
  }
  static void add(String name, Supplier<SsHtml> ssHtml);
  static SsHtml get(String name);   // ignores case; an unknown name is an error that lists the names
  static Set<String> names();       // for --rules-list and usage()
}
```

`IMG` is an application that opts in to images. `INLINE-TEXT` shows stripped text instead of literal markup. With `PASS`, startup prints a warning that the restricted dataset fetches URLs and that its records 8 and 9 break Swing. `--rules-list` prints the names and exits, matching `--dataset-list`.

Suggested runs, each value given explicitly:

1. `--dataset=html-inline --rules=INLINE`, then `--rules=PLAIN`.
2. `--dataset=html-multiline --rules=MULTILINE`, then `--rules=INLINE`.
3. `--dataset=html-restricted --rules=MULTILINE`, then `IMG`, then `PASS`.

Interactive check of the write path: in Example1, type `<html><img src=...>` into a supplier name and save. Then open Example3 and see it refused in the combo.

## Implementation order and open questions

One PR on one branch, as four commits that each build and run, so they can be reviewed separately. When we agree, I squash them and force-push. I'd keep step 1, the pure move of the setup classes, as its own commit, because a move with no other changes reads and annotates better. hg-git note: if you have pulled the branch before the force-push, the old commits stay in your local repository until you strip them.

1. Move the database setup out of `MainClass` into `DemoDatabase.java` and give H2 a real `run()`. No behavior change.
2. Add `DemoOptions`, `DemoScript`, `DemoDataset`, `--dataset`, `--dataset-list`, `--dataset-check` and the fallback dialog, with only the unnamed dataset. Same data as today.
3. Add `DemoRules`, `--rules` and `--rules-list`.
4. Add `datasets/datasets.txt` and the `html-inline`, `html-multiline` and `html-restricted` directories, plus a README section on datasets and the suggested runs.

To verify during implementation (not questions):

- The SsHtml set in `main()` is still the one in `Globals` after `SeeSaw.init()`. `LookupDefaults` sets one only when none is set, so it should be.
- The `REPEAT` counts in the depth rows: the depth-30 row must be accepted and the depth-40 and depth-600 rows refused. The parser adds implied elements that count toward the depth, so the counts may need adjusting.
- `filterNewlines` and the two-line combo item (Set 2).

Later, not in this PR: server-independent data scripts, separate from the per-server schema; consistent file names behind `DemoScript`; MySQL versions of the html datasets; the example buttons in MainClass as one list of (label, Example::new); a startup option for ModelType.GLAZED or SWING, so both combo behaviors can be compared on the html datasets; make DemoUtil.runSqlStatements split safely when string literals contain ';', as html entities like \&amp; do (for example, mask the entities with a token such as @ENTITY@AMP@ before splitting and restore them after, or skip ';' inside quotes).
