/* *****************************************************************************
 * Copyright (C) 2026, Ernie R Rael. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * ****************************************************************************/
package dev.visdb.seesaw.decorators;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.text.AttributeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.javaprop.JavaPropsMapper;
import tools.jackson.dataformat.javaprop.JavaPropsSchema;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * x
 */
public class TextStylesTest {
  /** x */
  public TextStylesTest() {}

  /** x */
  @BeforeAll
  public static void setUpClass() {}

  /** x */
  @AfterAll
  public static void tearDownClass() {
    TextStyles.clearStyles();
  }

  /** x */
  @BeforeEach
  public void setUp() {
    TextStyles.clearStyles();
  }

  /** x */
  @AfterEach
  public void tearDown() {}

  String properties_ok = """
        default.alignment=left
        default.foreground=#333333
        default.background=#FFFFFF
        default.linewrap=true
        default.wordwrap=false
        # both, vertical, horizontal, none
        default.scrollbars=vertical
        default.autoscroll=true
        
        warning.inherits=default
        warning.alignment=center
        warning.background=#FFF3CD
        
        criticalError.inherits=warning
        criticalError.alignment=right
        criticalError.foreground=#721C24
        """;

  String json_ok = """
		{
		  "default": {
			"foreground": "#333333",
			"background": "#FFFFFF",
			"fontFamily": "Monospaced",
			"fontSize": 16,
			"alignment": "left"
		  },
		  "warning": {
			"inherits": "default",
			"background": "#FFF3CD",
			"alignment": "right"
		  },
		  "criticalError": {
			"inherits": "warning",
			"foreground": "#721C24",
			"alignment": "right",
			"bold": true
		  },
		  "criticalError_2": {
			"inherits": "default_2",
			"foreground": "#721C24",
			"alignment": "center",
			"bold": true
		  },
		  "default_2": {
			"foreground": "#333333",
			"background": "#FFFFFF",
			"fontFamily": "Serif",
			"fontSize": 14,
			"alignment": "left"
		  }
		}
        """;

  String expect_json_ok = """

		=======================================================
		        THEME VALIDATION & DIAGNOSTIC REPORT
		=======================================================
		[STATUS] Validation successful! No broken pointers or cyclic rules found.

		SUMMARY COUNT METRICS:
		  • Total Profiles Loaded       = 5
		  • Standalone Root Profiles    = 2
		  • Inherited Child Profiles    = 3
		  • Total Attributes Configured = 18

		""";

  String expect_json_ok_trees = """
        Resolved Structural Hierarchy Trees:
        └── default
              • foreground = java.awt.Color[r=51,g=51,b=51]
              • background = java.awt.Color[r=255,g=255,b=255]
              • family = Monospaced
              • size = 16
              • alignment = left
            └── warning
                  • background = java.awt.Color[r=255,g=243,b=205]
                  • alignment = right
                └── criticalError
                      • foreground = java.awt.Color[r=114,g=28,b=36]
                      • alignment = right
                      • bold = true
        └── default_2
              • foreground = java.awt.Color[r=51,g=51,b=51]
              • background = java.awt.Color[r=255,g=255,b=255]
              • family = Serif
              • size = 14
              • alignment = left
            └── criticalError_2
                  • foreground = java.awt.Color[r=114,g=28,b=36]
                  • alignment = center
                  • bold = true
        """;

  String json_errors = """
		{
		  "default": {
			"inherits": "warning",
			"foreground": "#333333",
			"background": "#FFFFFF",
			"fontFamily": "Monospaced",
			"fontSize": 16,
			"alignment": "left"
		  },
		  "warning": {
			"inherits": "default",
			"background": "#FFF3CD",
			"alignment": "center"
		  },
		  "criticalError": {
			"inherits": "warning",
			"foreground": "#721C24",
			"alignment": "right",
			"bold": true
		  },
		  "criticalError_2": {
			"inherits": "black_hole",
			"foreground": "#721C24",
			"alignment": "right",
			"bold": true
		  }
		}
        """;

  String expect_json_errors = """

		=======================================================
		        THEME VALIDATION & DIAGNOSTIC REPORT
		=======================================================
		[VALIDATION ERROR] Configuration 'criticalError_2' inherits from missing parent 'black_hole'
		[VALIDATION ERROR] Circular inheritance path detected: default -> warning -> default
		[VALIDATION ERROR] Circular inheritance path detected: criticalError -> default -> warning -> warning
		[VALIDATION ERROR] Circular inheritance path detected: default -> warning -> warning
		[STATUS] Validation failed. See errors logged above.

		SUMMARY COUNT METRICS:
		  • Total Profiles Loaded       = 4
		  • Standalone Root Profiles    = 0
		  • Inherited Child Profiles    = 4
		  • Total Attributes Configured = 13
		  • Broken Parent Links Found   = 1
		  • Circular Reference Loops    = 3

		""";

  String keep_json = """
		{
			"init1": {
				"foreground": "blue",
				"background": "yellow",
				"opaque": false,
				"fontFamily": "Serif",
				"fontSize": 10,
				"bold": true,
				"italic": true,
				"underline": true,
				"strikethrough": true,
				"alignment": "right",
				"linewrap"  : "keep",
				"wordwrap"  : "keep",
				"scrollbars": "keep",
				"autoscroll": "keep"
			},

			"try1": {
				"foreground": "keep",
				"background": "keep",
				"opaque": "keep",
				"fontFamily": "keep",
				"fontSize": 14,
				"bold": "keep",
				"italic": false,
				"underline": false,
				"strikethrough": "keep",
				"alignment": "center",
				"linewrap"  : "keep",
				"wordwrap"  : "keep",
				"scrollbars": "keep",
				"autoscroll": "keep"
			},
			"try2": {
				"foreground": "keep",
				"background": "keep",
				"opaque": true,
				"fontFamily": "keep",
				"fontSize": "keep",
				"bold": false,
				"italic": "keep",
				"underline": "keep",
				"strikethrough": false,
				"alignment": "keep",
				"linewrap"  : "keep",
				"wordwrap"  : "keep",
				"scrollbars": "keep",
				"autoscroll": "keep"
			},
			"default": {
				"foreground": "default",
				"background": "default",
				"opaque": "default",
				"fontFamily": "default",
				"fontSize": "default",
				"bold": "default",
				"italic": "default",
				"underline": "default",
				"strikethrough": "default",
				"alignment": "default",
				"linewrap"  : "default",
				"wordwrap"  : "default",
				"scrollbars": "default",
				"autoscroll": "default"
			},
			"empty": {
			}
		}
		""";

  /** Recursively get all the names in the AttributeSet */
  private Set<Object> getAllAttributeNames(AttributeSet as) {
    if (as == null)
      return Collections.emptySet();
    Set<Object> accum = new HashSet<>();
    for (Iterator<?> it = as.getAttributeNames().asIterator(); it.hasNext();) {
      accum.add(it.next());
    }
    accum.addAll(getAllAttributeNames(as.getResolveParent()));
    return accum;
  }

  private Set<String> getAttributeStringPairs(AttributeSet as, Set<Object> attrNames) {
    return attrNames.stream()
        .map(attrName -> attrName.toString() + ":" + as.getAttribute(attrName))
        .collect(Collectors.toSet());
  }

  private Set<String> getAttributeStringPairs(AttributeSet as) {
    return getAttributeStringPairs(as, getAllAttributeNames(as));
  }

  /**
   * write contents to random file, return the new file's name.
   * If dir is null use system temporary dir.
   */
  private Path createToRandomFileName(Path dir, String ext, String content) {
    Path path;
    try {
      FileAttribute<Set<PosixFilePermission>> fattrib
          = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--"));
      path = dir == null ? Files.createTempFile(null, ext, fattrib)
                         : Files.createTempFile(dir, null, ext, fattrib);
      Files.writeString(path, content);
    } catch (IOException ex) {
      System.getLogger(TextStylesTest.class.getName())
          .log(System.Logger.Level.ERROR, (String) null, ex);
      path = null;
    }
    return path;
  }

  /**
   * PLAY WITH JACKSON FOR PROPERTIES.
   * @throws java.lang.Exception
   */
  @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testLoadStylesWithJacksonProperties() throws Exception {
    System.out.println("loadStylesFromJacksonProperties_Reader");

    // Create a Strict Properties bucket that intercept duplicates
    @SuppressWarnings("unused")
    Properties strictProps = new Properties() {
      @Override
      public synchronized Object put(Object key, Object value) {
        if (containsKey(key)) {
          throw new IllegalArgumentException(sf("Duplicate key found: %s=%s", key, value));
        }
        return super.put(key, value);
      }
    };

    String properties = """
        default.alignment=left
        default.foreground=#333333
        default.background=#FFFFFF
        #default.foreground=green
        default.foreground=green
      """;
    @SuppressWarnings("unused")
    Reader reader = new StringReader(properties);
    //Path path = createToRandomFileName(null, ".properties", properties);

    String prop0 = """
        default.alignment=left
        #=foo
        default.background=green
      """;
    Properties props = new Properties();
    props.load(new StringReader(prop0));

    JavaPropsMapper mapper = JavaPropsMapper
                                 .builder()
                                 // .configure(...) configure features here if needed
                                 .build();

    JavaPropsSchema schemaWithDots = JavaPropsSchema.emptySchema();

    // Map<String, Map<String, String>>
    JavaType mapType = mapper.getTypeFactory().constructMapType(
        MyMap.class, mapper.getTypeFactory().constructType(String.class),
        mapper.getTypeFactory().constructMapType(MyMap.class, String.class, String.class));

    Map<String, Map<String, String>> configMap;

    // 4. Bind the validated flat object directly into your deep nested Map structure
    configMap = mapper.readPropertiesAs(props, schemaWithDots, mapType);

    System.err.println(configMap == null ? "null" : "not null");
  }
  
  @SuppressWarnings("serial")
  static class MyMap<K,V> extends LinkedHashMap<K,V> {
    public MyMap() { }
    
    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public V put(K key, V value) {
      System.err.printf("PUT(%s,%s)\n", key, value);
      if(((String)key).isBlank())
        throw new IllegalArgumentException(sf("blank key, value '%s'", value));
      return super.put(key, value);
    }
  }
  
  /**
   * PLAY WITH JACKSON FOR JSON.
   * @throws java.lang.Exception
   */
  // @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testLoadStylesWithJackson() throws Exception {
    System.out.println("loadStylesFromJson_Reader");
    // https://jenkov.com/tutorials/java-json/jackson-objectmapper.html
    String json = """
      {
        "configA": { "timeout": 5000, "enabled": true },
        "configB": { "url": "https://example.com" }
      }
      """;
    Reader reader = new StringReader(json);

    ObjectMapper mapper
        = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();

    // Use TypeReference to cleanly parse directly into nested maps
    @SuppressWarnings("unused")
    Map<String, Map<String, String>> configMap
        = mapper.readValue(reader, new TypeReference<Map<String, Map<String, String>>>() {});

    System.err.println("");
  }

  /**
   * Test of getStyleNames method, of class TextStyles.
   * @throws java.io.IOException
   */
  @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testGetStyleNames() throws IOException {
    System.out.println("getStyleNames");

    Reader reader = new StringReader(json_ok);
    TextStyles.loadStylesFromJson(reader, null);

    Set<String> expect
        = Set.of("default", "warning", "criticalError", "default_2", "criticalError_2");
    Set<String> result = TextStyles.getStyleNames();
    assertEquals(expect, result);
  }

  /**
   * Test of getStyle method, of class TextStyles.
   * @throws java.io.IOException
   */
  @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testGetStyle() throws IOException {
    System.out.println("getStyle");

    Reader reader = new StringReader(json_ok);
    TextStyles.loadStylesFromJson(reader, null);

    AttributeSet result = TextStyles.getStyle("foo");
    assertEquals(null, result);

    Set<String> expect = Set.of("name:criticalError", "bold:true", "size:16", "Alignment:2",
                                "family:Monospaced", "background:java.awt.Color[r=255,g=243,b=205]",
                                "foreground:java.awt.Color[r=114,g=28,b=36]");
    AttributeSet style1 = TextStyles.getStyle("criticalError");
    Set<String> asPairs = getAttributeStringPairs(style1);
    assertEquals(expect, asPairs);

    // Try it again should get the same item.
    AttributeSet style2 = TextStyles.getStyle("criticalError");
    assertTrue(style1 == style2);
  }
  // https://assertj.github.io/doc/
  /**
   * Use this to generate an error while doing .properties.
   * @throws Exception
   */
  @Test
  @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
  public void testLoadStylesFromProperties() throws Exception {
    System.out.println("loadStylesFromProperties");

    String props = """
        default.alignment=left
        default=green
      """;
    Reader reader0 = new StringReader(props);
    @SuppressWarnings("unused")
    TextStyles.TextStylesException ex;
    ex = assertThrows(TextStyles.TextStylesException.class, () -> {
      TextStyles.loadStylesFromProperties(reader0, "StylesFromProperties0");
    });
    assertThat(ex.getMessage()).startsWith(
        "File: StylesFromProperties0: Key must have exactly one '.' not 0:");

    props = """
        default.alignment=left
        default.foo.bar=green
      """;
    Reader reader2 = new StringReader(props);
    ex = assertThrows(TextStyles.TextStylesException.class,() -> {
      TextStyles.loadStylesFromProperties(reader2, "StylesFromProperties2");
    });
    assertThat(ex.getMessage()).startsWith(
        "File: StylesFromProperties2: Key must have exactly one '.' not 2:");

    props = """
        default.alignment=left
        =foo
        default.foo.bar=green
      """;
    Reader readera = new StringReader(props);
    ex = assertThrows(TextStyles.TextStylesException.class,() -> {
      TextStyles.loadStylesFromProperties(readera, "StylesFromProperties3");
    });
    assertThat(ex.getMessage()).isEqualTo(
        "File: StylesFromProperties3: blank key or value '':'foo'");

    props = """
        default.alignment=left
        foo.bar=
        default.foo.bar=green
      """;
    Reader readerb = new StringReader(props);
    ex = assertThrows(TextStyles.TextStylesException.class,() -> {
      TextStyles.loadStylesFromProperties(readerb, "StylesFromProperties3");
    });
    assertThat(ex.getMessage()).isEqualTo(
        "File: StylesFromProperties3: blank key or value 'foo.bar':''");
    System.err.println("");
  }

  /**
   * Test of loadStyles method, of class TextStyles.
   * @throws java.lang.Exception
   */
  @Test
  @SuppressWarnings({"UseOfSystemOutOrSystemErr", "ThrowableResultIgnored"})
  public void testLoadStyles() throws Exception {
    System.out.println("loadStyles");
    Path path = createToRandomFileName(null, ".properties", properties_ok);
    if (path == null) {
      fail("Could not create path");
      return;
    }
    System.out.printf("    %s\n", path.toString());

    try {
      TextStyles.loadStyles(path);
      Set<String> asPairs = getAttributeStringPairs(TextStyles.getStyle("warning"));

      Set<String> expect
          = Set.of("name:warning", "scrollbars:vertical", "linewrap:true", "wordwrap:false",
                   "Alignment:1", "foreground:java.awt.Color[r=51,g=51,b=51]", "autoscroll:true",
                   "background:java.awt.Color[r=255,g=243,b=205]");
      assertEquals(expect, asPairs);
    } finally {
      Files.delete(path);
    }

    // Did properties, now add something new with json.
    String json = """
		{
		    "criticalError_2": {
		        "inherits": "warning",
		        "foreground": "#010203"
                  #, "one": "two"
		    }
		}
		""";
    String expect = """

			=======================================================
			        THEME VALIDATION & DIAGNOSTIC REPORT
			=======================================================
			[STATUS] Validation successful! No broken pointers or cyclic rules found.

			SUMMARY COUNT METRICS:
			  • Total Profiles Loaded       = 4
			  • Standalone Root Profiles    = 1
			  • Inherited Child Profiles    = 3
			  • Total Attributes Configured = 13

			""";
    Reader reader = new StringReader(json);
    TextStyles.LoadStatus status = TextStyles.loadStylesFromJson(reader, "testLoadStyles-1");
    assertTrue(status.ok());
    assertEquals(expect, status.diagnostics());

    // Error if adding something that exists
    Reader reader2 = new StringReader("""
			{
			    "warning": {
			        "foreground": "#040506"
			    }
			}
			""");
    assertThrows(TextStyles.TextStylesException.class,
                 () -> { TextStyles.loadStylesFromJson(reader2, "testLoadStyles-2"); });
    // Problem stuff discarded, should still be ok
    StringBuilder sb = new StringBuilder();
    sb.setLength(0);
    boolean ok = TextStyles.runDiagnostics(sb);
    assertTrue(ok);
    assertEquals(expect, sb.toString());

    // Error if adding something that exists
    //
    // An error occurs but some things have loaded.
    // In this case, "oops" is loaded, then an error,
    // It should get backed out, so no change.
    //
    Reader reader3 = new StringReader("""
			{
			    "oops": {
			        "foreground": "#040506"
			    },
			    "warning": {
			        "foreground": "#040506"
			    }
			}
			""");
    assertThrows(TextStyles.TextStylesException.class,
                 () -> { TextStyles.loadStylesFromJson(reader3, "testLoadStyles-3"); });
    sb.setLength(0);
    ok = TextStyles.runDiagnostics(sb);
    assertTrue(ok);
    assertEquals(expect, sb.toString());
  }

  //System.out.println(sb.toString());
  // int i = 0;
  // for (; i< Math.min(expect_json_ok_trees.length(), sb.length()); i++)
  // 	if (expect_json_ok_trees.charAt(i) != sb.charAt(i)) break;
  /**
   * Test of loadJsonConfigurations method, of class TextStyles.
   * @throws java.lang.Exception
   */
  @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testLoadStylesFromJson_Reader() throws Exception {
    System.out.println("loadStylesFromJson_Reader");

    // File has no errors. Use the Reader.

    Reader reader = new StringReader(json_ok);
    TextStyles.LoadStatus status
        = TextStyles.loadStylesFromJson(reader, "testLoadStylesFromJson_Reader");
    assertTrue(status.ok());
    assertEquals(expect_json_ok, status.diagnostics());
    assertEquals(expect_json_ok_trees, status.trees());
  }

  /**
   * Test of loadJsonConfgurations method, of class TextDecoratorStyles.
   * Loading file has many errors.
   * @throws java.lang.Exception
   */
  @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testLoadStylesFromJson_Reader_2() throws Exception {
    System.out.println("loadStylesFromJson_Reader_2");

    // Lots of errors

    // Read a configuration with errors

    Reader reader = new StringReader(json_errors);
    TextStyles.LoadStatus status = TextStyles.loadStylesFromJson(reader, null);
    assertFalse(status.ok());
    assertEquals(expect_json_errors, status.diagnostics());

    // Load should leave things in a good state.
    StringBuilder sb = new StringBuilder();
    boolean ok = TextStyles.runDiagnostics(sb);
    assertTrue(ok);
  }

  /**
   * Test of applyStyle method, of class TextStyles;
also a test of memento.
   * @throws java.io.IOException
   * @throws java.lang.InterruptedException
   * @throws java.lang.reflect.InvocationTargetException
   */
  @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testApplyStyle_JTextField_AttributeSet()
      throws IOException, InterruptedException, InvocationTargetException {
    System.out.println("applyStyle");

    Reader reader = new StringReader(json_ok);
    TextStyles.loadStylesFromJson(reader, null);

    EventQueue.invokeAndWait(() -> {
      JTextField textField = new JTextField();
      TextStyles.ComponentMemento mementoOrig = TextStyles.getMemento(textField);
      AttributeSet style = TextStyles.getStyle("warning");
      TextStyles.applyStyle(textField, style);
      TextStyles.ComponentMemento mementoNew = TextStyles.getMemento(textField);

      String expect = "ComponentMemento{foreground=#333333, background=#fff3cd,"
          + " opaque=true, font=java.awt.Font[family=Monospaced,name=Monospaced,"
          + "style=plain,size=16], underline=false, strikethrough=false,"
          + " isTextField=true, alignment=right, isTextArea=false, lineWrap=false,"
          + " wordWrap=false, hasScrollPane=false, vsb=0, hsb=0}";
      assertEquals(expect, mementoNew.toString());

      mementoOrig.restoreTo(textField);
      TextStyles.ComponentMemento mementoRestored = TextStyles.getMemento(textField);
      assertEquals(mementoOrig, mementoRestored);
    });
  }

  /**
   * Check out "keep".
   * @throws IOException
   * @throws InterruptedException
   * @throws InvocationTargetException
   */
  @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testApplyStyle_JTextField_keep()
      throws IOException, InterruptedException, InvocationTargetException {
    System.out.println("applyStyle keep");

    Reader reader = new StringReader(keep_json);
    TextStyles.loadStylesFromJson(reader, null);

    EventQueue.invokeAndWait(() -> {
      JTextField textField = new JTextField();
      TextStyles.ComponentMemento mementoOrig = TextStyles.getMemento(textField);
      //System.out.println(sf("mementoOrig %s\n", mementoOrig));
      String expectOrig
          = "ComponentMemento{foreground=#333333, background=#ffffff, opaque=true,"
          + " font=javax.swing.plaf.FontUIResource[family=Dialog,name=Dialog,style=plain,size=12],"
          + " underline=false, strikethrough=false,"
          + " isTextField=true, alignment=leading,"
          + " isTextArea=false, lineWrap=false,"
          + " wordWrap=false, hasScrollPane=false, vsb=0, hsb=0}";
      assertThat(mementoOrig.toString()).isEqualTo(expectOrig);

      TextStyles.applyStyle(textField, TextStyles.getStyle("init1"));
      TextStyles.ComponentMemento mementoInit1 = TextStyles.getMemento(textField);
      //System.out.println(sf("mementoInit1 %s\n", mementoInit1));
      String expectInit1
          = "ComponentMemento{foreground=#0000ff, background=#ffff00, opaque=false,"
          + " font=java.awt.Font[family=Serif,name=Serif,style=bolditalic,size=10],"
          + " underline=true, strikethrough=true,"
          + " isTextField=true, alignment=right,"
          + " isTextArea=false, lineWrap=false, wordWrap=false,"
          + " hasScrollPane=false, vsb=0, hsb=0}";
      assertThat(mementoInit1.toString()).isEqualTo(expectInit1);

      // when style attriute not included, should keep current value
      TextStyles.applyStyle(textField, TextStyles.getStyle("empty"));
      assertThat(mementoInit1.toString()).isEqualTo(expectInit1);

      TextStyles.applyStyle(textField, TextStyles.getStyle("try1"));
      TextStyles.ComponentMemento mementoTry = TextStyles.getMemento(textField);
      //System.out.println(sf("mementoInit1Try1 %s\n", mementoTry));
      String expectInit1Try1
          = "ComponentMemento{foreground=#0000ff, background=#ffff00, opaque=false,"
          + " font=java.awt.Font[family=Serif,name=Serif,style=bold,size=14],"
          + " underline=false, strikethrough=true,"
          + " isTextField=true, alignment=center,"
          + " isTextArea=false, lineWrap=false, wordWrap=false,"
          + " hasScrollPane=false, vsb=0, hsb=0}";
      assertThat(mementoTry.toString()).isEqualTo(expectInit1Try1);

      // when style attriute not included, should keep current value
      TextStyles.applyStyle(textField, TextStyles.getStyle("empty"));
      assertThat(mementoTry.toString()).isEqualTo(expectInit1Try1);

      TextStyles.applyStyle(textField, TextStyles.getStyle("default"));
      TextStyles.ComponentMemento mementoDefault = TextStyles.getMemento(textField);
      //System.out.println(sf("mementoInit1Try1Default %s\n", mementoDefault));
      String expectDefault
          = "ComponentMemento{foreground=#000000, background=#ffffff, opaque=true,"
          + " font=java.awt.Font[family=Monospaced,name=Monospaced,style=plain,size=12],"
          + " underline=false, strikethrough=false,"
          + " isTextField=true, alignment=left,"
          + " isTextArea=false, lineWrap=false, wordWrap=false,"
          + " hasScrollPane=false, vsb=0, hsb=0}";
      assertThat(mementoDefault.toString()).isEqualTo(expectDefault);

      // when style attriute not included, should keep current value
      TextStyles.applyStyle(textField, TextStyles.getStyle("empty"));
      assertThat(mementoDefault.toString()).isEqualTo(expectDefault);

      textField = new JTextField();
      mementoOrig = TextStyles.getMemento(textField);
      //System.out.println(sf("mementoOrig %s\n", mementoOrig));
      assertThat(mementoOrig.toString()).isEqualTo(expectOrig);

      TextStyles.applyStyle(textField, TextStyles.getStyle("init1"));
      mementoInit1 = TextStyles.getMemento(textField);
      //System.out.println(sf("mementoInit %s\n", mementoInit1));
      assertThat(mementoInit1.toString()).isEqualTo(expectInit1);

      TextStyles.applyStyle(textField, TextStyles.getStyle("try2"));
      mementoTry = TextStyles.getMemento(textField);
      System.out.println(sf("mementoInit1Try2 %s\n", mementoTry));
      // String expectInit1Try2
      String expectInit1Try2
          = "ComponentMemento{foreground=#0000ff, background=#ffff00, opaque=true,"
          + " font=java.awt.Font[family=Serif,name=Serif,style=italic,size=10],"
          + " underline=true, strikethrough=false,"
          + " isTextField=true, alignment=right,"
          + " isTextArea=false, lineWrap=false, wordWrap=false,"
          + " hasScrollPane=false, vsb=0, hsb=0}";
      assertThat(mementoTry.toString()).isEqualTo(expectInit1Try2);

      TextStyles.applyStyle(textField, TextStyles.getStyle("default"));
      mementoDefault = TextStyles.getMemento(textField);
      System.out.println(sf("mementoInit1Try2Default %s\n", mementoDefault));
      assertThat(mementoDefault.toString()).isEqualTo(expectDefault);
    });
  }

  /**
   * Test of applyStyle method, of class TextDecoratorStyles.
   */
  // / @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testApplyStyle_JTextArea_AttributeSet() {
    System.out.println("applyStyle");
    JTextArea textArea = null;
    AttributeSet style = null;
    TextStyles.applyStyle(textArea, style);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   *
   * @param args
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(() -> new SwingIt());
  }
  private static class SwingIt {
      private final JFrame frame;

    private SwingIt() {
      String dir = "/tmp"; // get the style files from this directory
      Function<String, Path> toPath = (fn) -> Path.of(dir, fn);
      
      frame = new JFrame("Validating Chain Style Loader");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(500, 180);
      frame.setLayout(new FlowLayout());
      
      JTextField textField = new JTextField("Validating alignment Styles!", 30);
      frame.add(textField);
      
      JButton btnLoadJson = new JButton("Load JSON (Critical Error Style)");
      // btnLoadJson.addActionListener(e -> loadDiagnoseAndApply(
      // 		textField, toPath.apply("styles.json"), "criticalError"));
      btnLoadJson.addActionListener(
          e -> new LoadThenApply(toPath.apply("styles.json"), "criticalError",
              textField, STYLES_JSON).execute());
      
      JButton btnRESET = new JButton("RESET");
      btnRESET.addActionListener(
          e -> TextStyles.applyStyle(textField, TextStyles.RESET));
      
      JButton btnLoadProps = new JButton("Load Props (Warning Style)");
      btnLoadProps.addActionListener(
          e -> new LoadThenApply(toPath.apply("styles.properties"), "warning",
              textField, STYLES_PROPERTIES).execute());
      Box box = Box.createVerticalBox();
      btnLoadJson.setAlignmentX(Component.CENTER_ALIGNMENT);
      btnLoadProps.setAlignmentX(Component.CENTER_ALIGNMENT);
      btnRESET.setAlignmentX(Component.CENTER_ALIGNMENT);
      box.add(btnLoadJson);
      box.add(btnLoadProps);
      box.add(btnRESET);
      frame.add(box, BorderLayout.SOUTH);
      frame.setVisible(true);
    }
    
    class LoadThenApply extends SwingWorker<Object, Object> {
      private final Path path;
      private final String styleName;
      private final JTextField field;
      private final String contents;
      
      public LoadThenApply(Path path, String styleName, JTextField field, String contents) {
        this.path = path;
        this.styleName = styleName;
        this.field = field;
        this.contents = contents;
      }
      
      @Override
      protected Object doInBackground() throws Exception {
        if (Files.exists(path)) {
          if (!contents.equals(Files.readString(path))) {
            int x = JOptionPane.showConfirmDialog(frame,
                """
                Rewrite file?
                Cancel exits.
                """, sf("%s different", path), JOptionPane.YES_NO_CANCEL_OPTION);
            switch(x) {
              case JOptionPane.YES_OPTION -> Files.writeString(path, contents);
              case JOptionPane.NO_OPTION -> { }
              case JOptionPane.CANCEL_OPTION -> System.exit(0);
            }
          }
        } else {
          int x = JOptionPane.showConfirmDialog(frame,
              """
              Create file?
              Cancel exits.
              """, sf("%s not found", path), JOptionPane.YES_NO_CANCEL_OPTION);
          switch(x) {
            case JOptionPane.YES_OPTION -> Files.writeString(path, contents);
            case JOptionPane.NO_OPTION -> { return null; }
            case JOptionPane.CANCEL_OPTION -> System.exit(0);
          }
        }
        
        TextStyles.clearStyles();
        TextStyles.loadStyles(path);
        return Boolean.TRUE;
      }
      
      @Override
      protected void done() {
        try {
          if (!Objects.equals(true, get()))
            return;
          AttributeSet style = TextStyles.getStyle(styleName);
          if (style != null) {
            TextStyles.applyStyle(field, style);
          } else {
            JOptionPane.showMessageDialog(null, sf("Style '%s' not found.", styleName));
          }
        } catch (InterruptedException | ExecutionException ex) {
          System.getLogger(TextStylesTest.class.getName())
              .log(System.Logger.Level.ERROR, (String) null, ex);
        }
      }
    }
    
    // For display, used with main
    private static final String STYLES_JSON =
        """
              {
                "default": {
                  "foreground": "#333333",
                  "background": "#FFFFFF",
                  "fontFamily": "Monospaced",
                  "fontSize": 16,
                  "alignment": "left"
                },
                "warning": {
                  "inherits": "default",
                  "background": "#FFF3CD",
                  "alignment": "center"
                },
                "criticalError": {
                  "inherits": "warning",
                  "foreground": "#721C24",
                  "alignment": "right",
                  "bold": true,
                  "strikethrough": true,
                  "underline": "default"
                }
              }
              """;
    private static final String STYLES_PROPERTIES =
        """
                    default.alignment=left
                    default.foreground=#333333
                    default.background=#FFFFFF
                    default.linewrap=true
                    default.wordwrap=false
                    # both, vertical, horizontal, none
                    default.scrollbars=vertical
                    default.autoscroll=true
              
                    warning.inherits=default
                    warning.alignment=center
                    warning.background=#FFF3CD
              
                    criticalError.inherits=warning
                    criticalError.alignment=right
                    criticalError.foreground=#721C24
                    criticalError.strikethrough=true
                    criticalError.underline=default
                    """;
  }
}
