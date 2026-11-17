# SsHtml: a proposal for the SeeSaw main library

Revision 6, 2026-11-17. Companion to the analysis of HTML in database text
(`SsHtml-analysis.md`).
Measurements are from small tests on JDK 21.

Status: all five steps of section 8 are implemented. Package `dev.visdb.seesaw.html` has
`SsHtml`, `Rules`, `SsHtmlChecker` and `SsHtmlPresenter`; `SsLabel`, the combo and list
renderers and the `SsTable` renderers use them. Section 10 lists what changed while
implementing; where it differs from the earlier sections, section 10 is right.

## 1. Decided so far

- The verifier is called everywhere database text reaches a label type component.
- It never throws, and it affects display only.
- The default is the checker.
- `SsHtml` is the verifier itself. Its static methods handle the Global and supply the built in
  implementations.
- The checker uses the JRE's own HTML parser. A contrib sanitizer is held off; it may have
  options for how to rewrite.
- A component's verifier is held in `SsCommon`, not in a client property.
- Dialogs are a separate, smaller fix.
- Three rule sets: `Rules.INLINE`, `Rules.DEFAULT`, `Rules.BROAD` (section 5).
- Logging is immediate, once per new name (section 7).
- The checker's default on refusal is `LITERAL`. A developer can move to `TEXT_ONLY` for release.

## 2. How the default is found

Recommendation: the Global holds the instance.

```java
Globals.setOption(SsHtml.class, SsHtml.checker(Rules.INLINE));
```

`SsHtml.getDefault()` returns that instance. The library initialization sets it to
`SsHtml.checker()` when the app has set nothing, following the pattern in `LookupDefaults`.
`SsHtml.checker()` uses `Rules.DEFAULT`, a constant in the `Rules` class (section 5). So the
default rules are in one place, and an app changes them with the one line above.

Consistency: SeeSaw already does both.

| Existing use | Global holds | Implementations found by |
|---|---|---|
| `DbSupport` (`DbSupportFactory.setDbSupportGlobalOption`) | the instance | creators registered on a path |
| `DbOpsCreator` (`RowsModel.findDbOps`) | the instance | set by the app |
| `DecoratorStyle` (`SsUtils.findDecoratorFactory`) | a name | factories registered on a path |

The instance form fits `SsHtml` because implementations take arguments, and a name can not carry
rules. A later search for a best fit can be a finder that ends by setting the instance, as
`DbSupportFactory` does.

## 3. The interface

`SsHtml` knows nothing about components. It maps text to a decision.

```java
/** Decides how text that may be HTML is displayed. Immutable; may be shared. */
public interface SsHtml {

  /** The test Swing uses: the first six characters are "<html>", ignoring case. */
  static boolean isHtml(String text) { ... }

  /** The Global's instance, or checker() if none is set. */
  static SsHtml getDefault() { ... }

  // The implementations in core.
  static SsHtml pass() { ... }                 // show everything as Swing would
  static SsHtml plain() { ... }                // never render HTML
  static SsHtml checker() { ... }              // checker(Rules.DEFAULT)
  static SsHtml checker(Rules rules) { ... }

  /** Decide how the text is displayed. Never throws, never returns null. */
  Display display(String text);

  /** Describe the text; for applications and tests. */
  Report inspect(String text);

  /**
   * @param text       what to pass to setText
   * @param renderHtml false: the component must show text literally
   * @param notAllowed what was refused, as "element" or "element@attribute"; empty if nothing
   */
  record Display(String text, boolean renderHtml, Set<String> notAllowed) {
    /** True if the verifier did not accept the original as it was. */
    public boolean hasRisk() { ... }
  }

  interface Report {
    boolean isHtml();
    /** Element names used; empty Optional if this implementation can not tell. */
    Optional<Set<String>> elements();
    /** Attribute names used, as "element@attribute"; empty Optional if unknown. */
    Optional<Set<String>> attributes();
    Display display();
  }
}
```

### Display

Revision 2 had an `Action` with three values. Your question about stripping and showing the rest
literally is a fourth case that an enum did not cover, so `Display` is now two independent facts:
the text, and whether it may be rendered as HTML.

| Outcome | `text` | `renderHtml` |
|---|---|---|
| Show as is | the original | true |
| Show the original literally | the original | false |
| Show cleaned HTML | a replacement | true |
| Show stripped text literally | a replacement | false |

`setText(display.text())` is always the right text. `renderHtml` must be honoured too: when it is
false the component needs `html.disable` set, otherwise an original that starts with `<html>`
would still be rendered.

Text that is not HTML always comes back unchanged with `renderHtml` true. That is the 2 ns path.

### Setting up the component

Your questions about `prepare`: the text never depends on the component, and the only thing done
to the component is to set or clear `html.disable`. So the two are better separated, and
`prepare` is removed from `SsHtml`.

The component part is one small static method in SeeSaw, not on the interface:

```java
/**
 * Make the component show text literally, or allow it to render HTML.
 * For components whose look and feel honours "html.disable": JLabel and its subclasses
 * (including the default table, list and combo renderers), AbstractButton, JToolTip.
 * Call before setText; Swing reads the flag only when the text changes.
 */
static void setHtmlEnabled(JComponent comp, boolean enabled)
```

`SsCommon` then offers what a call site needs, using the component's verifier:

```java
/** Decide, set up the component, log if needed, and return the text for setText. */
String displayText(JComponent comp, String text)
```

It also catches anything thrown by a misbehaving custom verifier and falls back to literal text.
Section 6 shows the call sites.

### Optional or null

`Optional<Set<String>>` is used in `Report` because "unknown" and "none" are different answers
and this is not on a hot path. Null would also work.

## 4. The checker

`javax.swing.text.html.parser.ParserDelegator` is the parser Swing's label renderer itself uses.
With a `ParserCallback` it reports each element and its attributes, for example
`font @color @size`, `img @src`, `body @background`, `p @style`.

- **No dependency.** It is in `java.desktop`.
- **No parser disagreement.** The checker and the renderer are the same parser. A `<style>`
  element after body content was not reported, and the label did not apply it either.
- **It did not fail on hostile input.** 3000 nested tables and 20000 nested `<b><i>` pairs parsed
  without error. The `StackOverflowError` seen earlier is in rendering, so the checker also limits
  length or nesting depth.
- **Cost.** Prefix check about 2 ns; parse 5 to 10 µs; the `setText` of HTML that Swing does
  anyway 170 to 500 µs. No cache is needed.

### When something is not allowed

The checker never passes on HTML it has rewritten. It has two behaviours, chosen in `Rules`:

| On refusal | Shows | Display |
|---|---|---|
| `LITERAL` (default) | the whole original, markup visible | original, `renderHtml` false |
| `TEXT_ONLY` | the text content with all markup removed | replacement, `renderHtml` false |

`TEXT_ONLY` is easy with the same parser, since it reports the text between the tags, and it is
safe because the result is shown literally. So stripping and showing the remainder literally does
not need a custom `SsHtml`.

Keeping the allowed markup and dropping only the refused parts is the third possibility. That
needs HTML written back out, which is where sanitizer bugs live, and it is what the contrib
sanitizer is for.

Extending: the checker would be a public class with one protected method that receives the text
and the parse result when something is refused, and returns a `Display`. A subclass that wants to
rewrite overrides that one method and inherits the parsing and the rules. Any other custom
`SsHtml` can wrap a checker: clean, then ask the checker about the result.

## 5. Rules

`Rules` is an immutable value: the allowed elements, the allowed attributes, the refusal
behaviour and a size limit. It is an argument of `SsHtml.checker(Rules)`. `pass()` and `plain()`
have no rules. A contrib implementation may accept the same `Rules`, but nothing requires it.

### What is actually dangerous

I rendered every element Swing knows, alone and with every attribute Swing knows set to a URL.

| Finding | Elements or attributes |
|---|---|
| Creates a component in the label | `applet`, `object`, `param`, `input`, `select`, `textarea` |
| Makes `setText` throw | `frame`, `frameset`, `noframes` |
| Fetches the URL | `src` on `img`; `background` on 23 elements, including `body`, `table`, `tr`, `div`, `p`, `li`, `h1` to `h6`; a `style` attribute with `background`, `background-image` or `list-style-image` |
| Nothing observed | the other 63 elements, and the other attributes |

This probe is not complete. It set one attribute at a time, so it missed `link`, which fetches
only with `rel=stylesheet` and `href` together, as the first test showed.

### Can the default be "anything safe"

It could, and it would leave a lot: most elements do nothing harmful. Three reasons not to make
it the default:

- "Safe" would be defined by a probe like the one above, and that probe already missed a case.
  A list of things known to be useful fails closed when something was overlooked.
- It would be safe for one JDK version. A positive list does not depend on what a later JDK does
  with an obscure element.
- Not fetching is not the same as not misleading. Fewer elements mean less room to imitate the UI.

A broad set is still worth having for apps that want it. Proposed named sets, each containing the
one before:

| Name | Contents | Meant for |
|---|---|---|
| `Rules.INLINE` | `b i u s em strong small big sub sup tt code font span`; attributes `color size face` | Table cells, combo and list items, one line labels |
| `Rules.DEFAULT` | `INLINE` plus `br p div center pre blockquote hr`, `h1` to `h6`, `ul ol li dl dt dd`, `table tr td th caption`; attributes `align valign width height border bgcolor colspan rowspan cellpadding cellspacing nowrap` | Labels showing formatted text |
| `Rules.BROAD` | `DEFAULT` plus `address basefont cite dfn dir kbd menu nobr samp var`; attributes `alink clear compact dir halign hspace lang link noshade start text title type value vlink vspace` | Apps with existing rich HTML that want to block only the known dangers |

None of them allows `img`, `src`, `background` or `style`.

### Getting and changing a rule set

`Rules` is immutable in the way `String` is: nothing changes an instance, and each `with` method
returns a new one. The three sets are constants, and an app derives its own from one of them:

```java
Rules.DEFAULT                                    // a constant; also INLINE and BROAD
Rules.DEFAULT.withOnRefusal(OnRefusal.TEXT_ONLY) // a new Rules; DEFAULT is unchanged
Rules.INLINE.withElements("br").withMaxLength(4000)
Rules.DEFAULT.withElements("img").withAttributes("img@src")   // the app's responsibility
Rules.DEFAULT.withoutElements("table", "tr", "td", "th", "caption")
Rules.of(elements, attributes)                   // from nothing
```

So the refusal behaviour is a property of the `Rules`, set with `withOnRefusal`. Because
instances never change, a `Rules` or a checker built from it can be shared by any number of
components without one affecting another. Javadoc on each constant would say what it adds and why the rest is left
out, which also serves as the explanation of how to use `SsHtml`.

### Tables

Being stricter for tables is only a different `Rules` given to the same checker. Suggestion: start
tables on the global default like everything else, and document `Rules.INLINE` as the choice for
tables. Block HTML does not work well in a cell anyway, since the row height is fixed.

## 6. Where the verifier lives, and the call sites

### Components

`SsCommon` holds the component's verifier, with a setter and getter reached through
`SsComponent`. Unset means `SsHtml.getDefault()`, resolved when text is displayed.

```java
setText(getSsCommon().displayText(this, text));                // SsLabel.java:162
```

The combo and list renderers in `AbstractComboBoxListSwingModel` (`:486`, `:506`) belong to a
model that belongs to a component, so they use that component's `SsCommon` the same way.

### SsTable

One verifier for the table to begin with: `ssTable.setHtmlVerifier(v)`. Per column choice fits
the object you have in mind between an `RSC` and an `SsComponent`, which would also carry an
`SsFormat`.

SeeSaw should install its own default renderer. Today `SsTable` installs default editors
(`SsTable.java:1242`) but no renderer, so text columns use JTable's own, which can not call the
verifier. A small subclass of `DefaultTableCellRenderer` that overrides `setValue`, installed
with `setDefaultRenderer(Object.class, ...)`, covers text columns. `ComboRenderer` (`:441`) and
`DateRenderer` (`:608`) get the same one line.

A table has one label object per renderer and points it at each cell in turn. A flag set on it
for one cell is still set for the next, so the flag is set or cleared for every cell. With
`displayText` doing that, the renderers need no special care.

## 7. Logging

Done by the caller (`SsCommon`, or the table), because only the caller knows the source.
`Display.notAllowed()` supplies the names without a second parse.

- **What:** a WARNING with the column, the row when known, the names that were not allowed, and
  the first 60 or so characters of the text, escaped.
- **How often:** each component or column keeps the set of names it has already reported. A text
  is reported only if it brings a name not yet in the set. So new attributes are reported, and
  repaints of the same cell are not.
- **A cap:** unknown element names are whatever the author typed, so the set is capped, for
  example at 20 names, then one "further reports suppressed" line.
- **After the rows are processed:** a table has no such moment. Cells are checked when they are
  painted, and rows off screen are not checked until scrolled into view. What can be done is to
  collect findings and write one summary line per column a second or so after the last one. That
  is an optional refinement; reporting each new name when it is first seen is simpler.

## 8. Suggested order of work

1. `SsHtml`, `Display`, `Report`, `Rules`, `setHtmlEnabled`, and the `pass` and `plain`
   implementations.
2. The checker, the three rule sets, and tests.
3. The verifier in `SsCommon` with `displayText` and logging; the call sites in `SsLabel` and the
   combo and list renderers. (`displayText` became `SsHtmlPresenter.prepare`, see section 10.)
4. The `SsTable` default renderer, `ComboRenderer` and `DateRenderer`; the table wide setter.
5. A line in `NOTES/NEXT_RELEASE`: with the checker as default, an app that stores images or
   styles in displayed text will see the markup until it sets its own rules or `pass()`.

Held off: a contrib sanitizer.

## 9. Still open

Nothing in the design. Dialogs that show database or exception text are a separate matter and
are not changed.

## 10. Found while implementing

- **`title` is not in `Rules.BROAD`.** Written outside the `head`, for example inside a
  `blockquote`, it puts a component in the label. The render test found this; the first probe
  had only tried it alone. The `title` that the parser adds to a `head` is harmless and accepted.
- **Elements the parser adds are checked too.** `<li>` by itself implies a `ul`, and the renderer
  gets it. So rules that allow `li` must allow `ul`. Only the implied paragraph and the implied
  `title` are exempt; `html`, `head` and `body` are always allowed.
- **Two parser markers could have hidden an element.** The parser marks implied elements and
  unknown closing tags with attributes. An attribute written in the text with the same name must
  not be taken for the marker. Both cases are handled and tested.
- **`Display.hasRisk()` is derived:** it is true when `notAllowed` is not empty. Text shown
  literally must name a reason, so `plain()` reports `html` as what it refused, and the limits
  report `(length)` and `(depth)`.
- **`SsHtml.getName()`** gives a short name for logs: `PASS`, `PLAIN`, `CHECKER(DEFAULT)`,
  `CHECKER(custom,TEXT_ONLY)`.
- **The implementations register** with `@ServiceProvider` at the path `SS/Html`
  (`SsHtml.PATH`): `SsHtmlPass`, `SsHtmlPlain` and `SsHtmlChecker`, whose no argument constructor
  uses the default rules. `SsHtml.getAvailable()` lists them, and startup logs each one, so a
  developer sees that there is a choice. A checker with other rules is a variation of a
  registered one and is not listed.
- **The default is set explicitly.** `LookupDefaults.initDefaults()` puts `SsHtml.checker()` in
  `Globals` unless the application has set one, and logs the default. `SsHtml.getDefault()` only
  falls back to the checker when nothing is set, which means the library was not initialized.
- **`SsHtmlPresenter` instead of `SsCommon.displayText`.** The part that involves a component
  and the log is a small class of its own, because the table and the list models need it without
  an `SsCommon`. One presenter serves one source of text: `SsCommon` has one for its column, a
  combo or list model uses its component's, and `SsTable` has one for each column. A call site
  is `setText(presenter.prepare(this, text))`. It also holds the names already reported.
- **`SsComponent` has `setSsHtml`, `getSsHtml` and `getSsHtmlPresenter`.** Unset means
  `SsHtml.getDefault()`, resolved each time text is displayed.
- **`SsTable` has `setSsHtml` and `getSsHtml` for the whole table**, and a protected
  `TextRenderer` installed as the default renderer for `Object`. `ComboRenderer` and
  `DateRenderer` extend it. A renderer the application installs itself is not checked, unless
  it extends `TextRenderer` and uses `setCellText`.
- **`ComboRenderer` never puts the underlying value in the label.** It used to let the super
  class set the cell value as text and then replace it with the display value.
- **The Glazed combo box got its renderer replaced.** `AutoCompleteSupport.install` installs
  its own renderer, which did not go through the model's. `SsComboBox2` now sets the model's
  renderer after the install, so both model types are checked the same way.
- **An application's own `html.disable` is respected.** The presenter clears the flag only if
  it set it. And since Swing reads the flag only when the text changes, the presenter redoes
  the label's view when the flag changes while the text stays the same.
- **Text the application sets with `setText` is not checked**, only text that comes through the
  binding or a renderer.
- **An unknown element before any content is dropped by the parser**, and so by the renderer;
  `<html><xyz>text` is accepted and shows `text`. After content it is reported and refused.
- **Limits:** 32768 characters and a nesting depth of 32. Rendering was fine at depth 256 and
  failed at 512 for tables.
- **A render test backs the rule sets.** `RulesRenderTest` gives the JDK's label everything each
  constant allows: every element with every allowed attribute set to a URL, and every element
  inside every other. It fails on a fetch, a component in the label or an exception. It first
  proves that it can detect each of those.
