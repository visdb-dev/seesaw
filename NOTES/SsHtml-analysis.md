# HTML in database text displayed by SeeSaw components

2026-11-16. Analysis only; no SeeSaw code was changed for it. The design that followed is in
`SsHtml-design.md`.

## Summary

- Swing renders any text that starts with `<html>` as HTML in labels, table cells, combo and list
  items, tooltips and option dialogs. SeeSaw passes database text to several of these unchanged.
- The HTML can not run code. It can make the application fetch URLs chosen by whoever wrote the
  text, imitate parts of the UI, and make the event thread throw.
- This matters only when someone who can write the database text is less trusted than the person
  viewing it.
- At least one SeeSaw application stores HTML in the database on purpose and shows it in an
  `SsLabel`, so HTML can not simply be turned off everywhere.
- The proposal to sanitize with jsoup is sound for that use, with corrections listed below.
  The direction under consideration is an `SsHtml` interface in SeeSaw, with a default that changes
  nothing and a jsoup implementation in a contrib module.

## 1. What Swing does with HTML text

Measured with a small test on JDK 21, against SeeSaw main at commit 98cd6cf.

**Trigger.** The text must start with exactly `<html>`, in any letter case, with no leading
whitespace. Text with the tag further in is shown literally. `<html lang=x>` was not treated as
HTML.

**Network and file fetches.** Image tags, stylesheet links, CSS `@import` and a body background
image are fetched as soon as `setText` runs. The fetch happens on the event thread, before anything
is painted. Consequences:

- A beacon: the author learns that a row was displayed, from which IP address, and the Java version
  (it is in the User-Agent header).
- Blind requests to hosts that only the viewer's machine can reach.
- `file:` and UNC paths are accepted. Not tested here, but on Windows an image on a `\\host\share`
  path is the well known way to leak NTLM credentials. A `file:` image does not disclose the file's
  content.
- A slow host stalls the UI.

**UI spoofing.** Fonts, colours, tables and images are all available. Form tags create real text
fields, password fields and buttons inside the label.

**Errors on the event thread.** A frameset tag without rows makes `setText` throw a
`RuntimeException`. 3000 nested tables gave a `StackOverflowError`.

**No code execution.** Script tags are ignored. An object tag with a `classid` created nothing,
and instantiated a class only when the JVM was started with `-Dswing.html.object=true`.

**Option dialogs.** `JOptionPane` splits a string message at newlines and shows each line in its
own label. Any line that starts with `<html>` is rendered as HTML.

**Switching it off.** `component.putClientProperty("html.disable", Boolean.TRUE)` makes the
component show the text literally. In the test no fetch happened with this set.

## 2. Which SeeSaw components are affected

| Component | Affected | Where |
|---|---|---|
| `SsLabel` | Yes | `SsLabel.java:162`, `setText(getColumnText())` |
| `SsTable` cells | Yes | JTable's default label renderer is used for text columns. Also `SsTable.java:441` (ComboRenderer) and `:608` (DateRenderer, for values that are not dates) |
| `SsComboBox1`, `SsComboBox2`, `SsDbComboBox2`, `SsList1` | Yes, when the first displayed element starts with `<html>` | `models/AbstractComboBoxListSwingModel.java:486` and `:506` |
| Error dialogs | Partly | See below |
| `SsTextField`, `SsTextArea`, formatted fields, table cell editors | No | Plain text documents |
| `SsCheckBox`, `SsSlider`, `SsImage`, `SsDataNavigator` | No | No database text reaches a label |

SeeSaw has no `JEditorPane` or `JTextPane`. Its only tooltip is a constant (`SsImage.java:322`).

**Dialogs.** `"...\n" + se.getMessage()` in `navigate/RowsActions.java` (11 places, for example
`:171`) and `table/SsTableModel.java:189` put the JDBC driver's message at the start of a line.
That is exploitable only when the driver's message begins with attacker text, which is unusual.
`utils/SsUtils.java:530` builds an HTML message from an exception message and a file path without
escaping them, but both come from the user's own file chooser.

## 3. Review of the jsoup proposal

The proposal: add a utility that cleans label text with jsoup and a small allowlist of tags
(`b`, `strong`, `i`, `em`, `u`, `br`, `p`, `sub`, `sup`), call it in the label's update method,
and offer three modes: plain text, sanitized HTML, trusted HTML.

### What is right

- The risk table agrees with the measurements above: no script execution, resource loading is the
  main issue, links in a label are not clickable.
- An allowlist of formatting tags with no attributes removes every fetch and every embedded
  control found in section 1.
- The three modes are a good way to frame the choice.
- Sanitizing must affect only what is displayed, never what is written back to the database.
  In SeeSaw the label's write-back is off by default (`SsLabel.java:71`), so this matters only when
  an application turns it on.
- jsoup is a reasonable choice of sanitizer. With HTML stored on purpose there is a real need for a
  sanitized mode, and with the sanitizer in an optional module the dependency is not a concern.

### Corrections

1. **It covers only the label.** Table cells and the combo and list renderers interpret HTML the
   same way and need the same treatment.
2. **It reviewed SwingSet, not SeeSaw.** The package, class and method names are SwingSet's.
   SeeSaw does not depend on SwingSet, so the code belongs in SeeSaw. In SeeSaw the label is
   updated in the component hook's `updateSsComponent()` at `SsLabel.java:162`.
3. **Plain text mode has a one line implementation that it does not mention.** That is the
   `html.disable` client property. No string transformation can do this job, because escaping the
   text changes what is displayed.
4. **The length limit throws.** An `IllegalArgumentException` for text over 16K would surface in
   the data binding path for a legitimate long value. Showing the text as plain text, or truncating
   it, is safer than throwing.
5. **The prefix test is wider than Swing's.** The regex accepts `<html lang=x>`, which Swing shows
   literally. The sanitizer would turn such text into rendered HTML. The test should be the same as
   Swing's: the first six characters equal `<html>`, ignoring case.
6. **Nesting depth is not limited.** Allowlisted tags still go through Swing's parser, and deep
   nesting is what caused the `StackOverflowError`. The 16K limit bounds this in practice, but it
   is worth a test.
7. **Font colour and tables are removed.** The proposal asks about this itself. An application
   that stores HTML probably uses more than bold and italic, so the allowlist should be the
   application's to set.
8. **The URL validator section does not apply.** SeeSaw has no hyperlink handling.

### Risks the proposal does not list

- Form tags create working fields and buttons inside the label.
- Malformed markup makes `setText` throw.
- Fetches happen on the event thread at `setText` time, not at paint time.

## 4. Direction under consideration

From SeeSaw's maintainer:

- An `SsHtml` interface that can check, sanitize or answer questions about text that may be HTML.
- The susceptible components call it before they display database text.
- It is found through `Globals` or `Lookup`, as other SeeSaw options are.
- The default implementation does nothing and accepts everything, so existing applications behave
  as they do today.
- A contrib module provides a jsoup implementation.

Points to settle in that design:

1. **The answer needs three outcomes, not only a string.** They are: show as is, show this
   replacement text, or show as plain text. The component implements the last one with
   `html.disable`, since a string can not express it.
2. **Call it everywhere database text reaches a label.** That is `SsLabel`, the three `SsTable`
   renderer paths and the two combo and list renderers in section 2.
3. **It is called on every cell paint in a table.** Text that does not start with `<html>` should
   return at once, and an implementation may want to cache results.
4. **It must not throw.** On any failure the component should fall back to plain text.
5. **It is display only.** The value in the RowSet and anything written back must stay as stored.
6. **A plain text implementation could ship in core.** It needs no dependency, and it suits the
   applications that never want HTML from the database. Whether "do nothing" or "plain text" should
   be the default is a compatibility decision for the maintainer.
7. **Per component choice.** An application may want HTML in one label and plain text in every
   table, so the call should say which component and column is asking.
8. **Dialogs are separate.** Keeping exception text off the start of a dialog line, and escaping
   the values in `SsUtils.reportError`, do not need the interface.

## 5. What an application can do today

- For a component that should never render HTML, set `html.disable` on it:
  `ssLabel.putClientProperty("html.disable", Boolean.TRUE)`. For tables and combos, set it on the
  renderer component.
- For a label that must show HTML from the database, restrict who can write that column, or clean
  the text before it is stored.
- Do not start the JVM with `-Dswing.html.object=true`.
