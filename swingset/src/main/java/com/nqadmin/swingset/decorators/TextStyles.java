/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package com.nqadmin.swingset.decorators;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.text.*;

import com.nqadmin.swingset.utils.JStuff;

import static com.nqadmin.swingset.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

// Build with the aid of google search.
// Won't use it again except for tiny stuff.

//
// TODO: CLEANUP DEFAULT/NO-DEFAULT behavior, related javadoc
//

/**
 * This class manages a hierarchy of named TextStyles; a TextStyle is an
 * {@link AttributeSet} with a limited list of attribute names, see below.
 * There are methods to apply an {@code AttributeSet} to either a
 * {@code JTextCompoent} or {@code JTextArea}.
 * <p>
 * The styles are usually
 * read from a resource/file.
 * After styles are loaded, then {@link #executeFullStyleDiagnostics(boolean)}
 * is run and results logged. If there's an exception/error during loading
 * and/or diagnostics fail then the load is backed out.
 * See {@link LoadStatus}.
 * <p>
 * Other than the methods that operate on {@code JTextComponents}
 * only {@link #getStyleNames()} and {@link #getStyle(java.lang.String)}
 * can be called from the {@link EventQueue}. If a method is called from an
 * inappropriate thread an {@code IllegalStateException} is thrown.
 * 
 * <style>
 *   th, td {
 *     padding-left: 15px; 
 *     padding-right: 15px; 
 *     padding-top: 1px; 
 *     padding-bottom: 1px; 
 *   }
 * </style>
 * 
 * <table border="1">
 * <caption>Attributes in a TextStyle</caption>
 * <thead>
 * <tr>
 * <th> Attribute Name </th><th> Acceptable Values </th><th>Default</th>
 * <th> Target Component</th>
 * </tr>
 * </thead>
 * 
 * <tbody>
 * <tr>
 * <td> foreground </td><td> Color: #ff2277, name </td><td>BLACK</td>
 * <td> JTextField, JTextArea </td>
 * </tr><tr>
 * <td> background </td><td> Color: #ff2277, name </td><td>WHITE</td>
 * <td> JTextField, JTextArea </td>
 * </tr><tr>
 * <td> opaque </td><td> true,false </td><td>true</td>
 * <td> JTextField, JTextArea </td>
 * </tr><tr>
 * <td> fontFamily </td><td> String </td><td>"Monospaced"</td>
 * <td> JTextField, JTextArea </td>
 * </tr><tr>
 * <td> fontSize </td><td> int </td><td>12</td>
 * <td> JTextField, JTextArea </td>
 * </tr><tr>
 * <td> bold </td><td> true, false </td><td>false</td>
 * <td> JTextField, JTextArea </td>
 * </tr><tr>
 * <td> italic </td><td> true, false </td><td>false</td>
 * <td> JTextField, JTextArea </td>
 * </tr><tr>
 * <td> underline </td><td> true, false </td><td>false</td>
 * <td> JTextField, JTextArea </td>
 * </tr><tr>
 * <td> strikethrough </td><td> true, false </td><td>false</td>
 * <td> JTextField, JTextArea </td>
 * </tr><tr>
 * <td> alignment </td><td> "left", "center", "right" </td><td>"left"</td>
 * <td> JTextField Only  </td>
 * </tr><tr>
 * <td> linewrap </td><td> true, false </td><td>false</td>
 * <td> JTextArea Only  </td>
 * </tr><tr>
 * <td> wordwrap </td><td> true, false </td><td>false</td>
 * <td> JTextArea Only  </td>
 * </tr><tr>
 * <td> scrollbars </td>
 * <td> "both", "vertical", "horizontal",<br>"asneeded", "none" </td><td>"asneeded"</td>
 * <td> JTextArea Only </td>
 * </tr><tr>
 * <td> autoscroll </td><td> true, false </td><td>ignore</td>
 * <td> JTextArea Only </td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * <ul>
 * <li>
 * There are two special words, <b>"keep"</b> and <b>"default"</b>, which are acceptable
 * values for any style attribute. "keep" means that the current component
 * value be unaltered when the style is applied. "default" means use the default value.
 * <li>
 * If an attribute is not specified, then "keep" is assumed.
 * <li>
 * If a color starts with "#" try {@link Color#decode(String)}, next try
 * matching the value to a name of a color as seen in {@link Color}'s fields.
 * <li>
 * If a style is applied to a component, and the style has attributes that are
 * not applicable to the component, the attributes are ignored.
 * <li>
 * The scroll related styles are only applied if the JTextArea is embedded in a ScrollPane
 * </ul>
 * 
 * There are two supported
 * formats for specifying styles, json and properties.
 * 
 * <pre>
 * {
 *   "default": {
 *     "foreground": "#333333",                default.foreground=#333333   
 *     "background": "#FFFFFF",                default.background=#FFFFFF
 *     "fontFamily": "Monospaced",             default.fontFamily=Monospaced
 *     "fontSize": 16,                         default.fontSize=16
 *     "alignment": "left"                     default.alignment=left
 *   },
 *   "warning": {
 *     "inherits": "default",                  warning.inherits=default
 *     "background": "#FFF3CD",                warning.background=#FFF3CD
 *     "alignment": "center"                   warning.alignment=center
 *   },
 *   "criticalError": {
 *     "inherits": "warning",                  criticalError.inherits=warning
 *     "alignment": "right",                   criticalError.alignment=right
 *     "foreground": "#721C24",                criticalError.foreground=#721C24
 *   }
 * }
 * </pre>
 * Each produces, as displayed by {@link #generateDiagnosticTrees(StringBuilder) }:
 * <pre>
 * Resolved Structural Hierarchy Trees:
 * └── default
 *       • foreground = java.awt.Color[r=51,g=51,b=51]
 *       • background = java.awt.Color[r=255,g=255,b=255]
 *       • family = Monospaced
 *       • size = 16
 *       • alignment = left
 *     └── warning
 *           • background = java.awt.Color[r=255,g=243,b=205]
 *           • alignment = center
 *         └── criticalError
 *               • alignment = right
 *               • foreground = java.awt.Color[r=114,g=28,b=36]
 * </pre>
 */
public class TextStyles {
    private static final Logger logger = JStuff.getLogger();
    
    // Custom attribute keys
    private static final String OPAQUE = "opaque";
    private static final String LINE_WRAP_KEY = "linewrap";
    private static final String WORD_WRAP_KEY = "wordwrap";
    private static final String SCROLLBARS_KEY = "scrollbars";
    private static final String AUTOSCROLL_KEY = "autoscroll";
	private static final String KEEP = "keep";
	private static final String DEFAULT = "default";

    // Global registries populated by file loader
    private static final Map<String, SimpleAttributeSet> registry = new ConcurrentHashMap<>();
    private static final Map<String, String> inheritanceMap = new ConcurrentHashMap<>();
	private static final Map<String, AttributeSet> readOnlyRegistry = new ConcurrentHashMap<>();
	// These two maps are used during loading so that EDT/EQ can safely do getStyle
	// without lock and so load can be undone if there's an error.
	private static final Set<String> initialStyleNames
			= Collections.newSetFromMap(new ConcurrentHashMap<>());
	private static final Set<String> newStyleNames
			= Collections.newSetFromMap(new ConcurrentHashMap<>());
	private static final WeakHashMap<JComponent, ComponentMemento> resetMementos
			= new WeakHashMap<>();

	/** Client property key */
	public static final Object STYLE_NAME = new Object();

	/**
	 * Use as {@link #applyStyle(JComponent, RESET) } to restore to value
	 * before any TextStyles were applied.
	 */
	public static final AttributeSet RESET = new AttributeSetDelegate(null);

	// During apply, the original AI code modified components even if they
	// were not set in properties. This flag controls that behavior.
	// The following set to true, does the AI code.
	// When the following is false, only things specified in Style are modified.
	// TODO:
	//		Some defaults seems good. For example, default strikethrough
	//		false feels right. So DOCUMENT THE DEFAULTS.
	//		But there's weirdness. For example foreground has a default BLACK,
	//		but background does not; which means after changing background to
	//		something weird, then applying something without background leaves
	//		the weird background.

	private TextStyles() { }

	/**
	 * Get a Set of all the StyleNames.
	 * @return 
	 */
	public static Set<String> getStyleNames() {
		Set<String> currentStyleNames = new HashSet<>(registry.keySet());
		currentStyleNames.removeAll(newStyleNames);
		return currentStyleNames;
	}

	/**
	 * Return the Style, ie AttributeSet, for the name,
	 * that is guaranteed not to change over time.
	 * @param styleName
	 * @return AttributeSet or null if doesn't exist
	 */
	public static AttributeSet getStyle(String styleName) {
		if (styleName == null || newStyleNames.contains(styleName))
			return null;
		AttributeSet style = readOnlyRegistry.get(styleName);
		if (style != null)
			return style;
		AttributeSet resolvedStyle = getResolvedStyle(styleName);
		if (resolvedStyle == null)
			return null;
		SimpleAttributeSet newAttrs = new SimpleAttributeSet();
		flatten(getResolvedStyle(styleName), newAttrs);
		newAttrs.removeAttribute(StyleConstants.ResolveAttribute);
		AttributeSet roAttrs = new AttributeSetDelegate(newAttrs);
		readOnlyRegistry.put(styleName, roAttrs);
		return roAttrs;
	}

	private static void flatten(AttributeSet origAttrs, MutableAttributeSet newAttrs) {
		if (origAttrs == null)
			return;
		flatten(origAttrs.getResolveParent(), newAttrs);
		newAttrs.addAttributes(origAttrs);
	}

	@SuppressWarnings("unused")
	// package for testing
	/*private*/ static synchronized void clearStyles() {
		verifyNotEDT();
		logger.log(INFO, () -> sf("{%s} is clearing all styles.", JStuff.getCaller(5)));
		registry.clear();
		inheritanceMap.clear();
		readOnlyRegistry.clear();
		initialStyleNames.clear();
		newStyleNames.clear();
	}

	static void verifyNotEDT() {
		if (EventQueue.isDispatchThread())
			throw new IllegalStateException("Synchronized method called from EDT.");
	}

	static void verifyEDT() {
		if (!EventQueue.isDispatchThread())
			throw new IllegalStateException("Swing method not called from EDT.");
	}

    /**
     * Executes the validation scanner and structural hierarchy printer independently,
     * gathers their generated text outputs and log them INFO.
	 * If toConsole, then also prints them to the console.
	 * 
	 * @param toConsole 
	 * @return true if everythings OK.
     */
	@SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static synchronized LoadStatus executeFullStyleDiagnostics(boolean toConsole) {
		verifyNotEDT();
        // 1. Run validation engine and gather report string
        StringBuilder sb = new StringBuilder(600);
		boolean healthy = runDiagnostics(sb);
		String diagnostics = sb.toString();
		logger.log(INFO, () -> sf("\n%s", diagnostics));
		if (toConsole)
			System.out.print(sb.toString());
		if (!healthy)
			logger.log(ERROR, () -> sf("Style diagnotics failed."));

        // 2. Locate root nodes and generate structural tree map string
        sb.setLength(0);
		generateDiagnosticTrees(sb);
		String trees = sb.toString();
        sb.append("=======================================================\n");
        
		logger.log(INFO, () -> sf("\n%s", sb.toString()));
		if (toConsole)
			System.out.print(sb.toString());
		return new LoadStatus(healthy, diagnostics, trees);
    }

	/**
     * This structural hierarchy printer builds the text block string.
	 * @param sb
	 */
	public static synchronized void generateDiagnosticTrees(StringBuilder sb) {
		verifyNotEDT();
        sb.append("Resolved Structural Hierarchy Trees:\n");

        Set<String> roots = new HashSet<>(registry.keySet());
        roots.removeAll(inheritanceMap.keySet());

        for (String root : roots) {
            generateDiagnosticTree(sb, root, "", true);
        }
	}
	
    /**
     * This validation scanner analyzes configurations, runs a full property count
	 * metric calculation, and compiles the final verification text block string.
	 * @param sb accumulate the output in here.
	 * @return true if no problems
     */
    public static synchronized boolean runDiagnostics(StringBuilder sb) {
		verifyNotEDT();
        sb.append("\n=======================================================\n");
        sb.append(  "        THEME VALIDATION & DIAGNOSTIC REPORT\n");
        sb.append(  "=======================================================\n");
        
        boolean systemHealthy = true;
        int brokenInheritancePointers = 0;
        int circularLoopFailures = 0;

        // Perform integrity validations
        for (Map.Entry<String, String> entry : inheritanceMap.entrySet()) {
            String child = entry.getKey();
            String parent = entry.getValue();
            
            if (!registry.containsKey(parent)) {
                sb.append("[VALIDATION ERROR] Configuration '").append(child)
                  .append("' inherits from missing parent '").append(parent).append("'\n");
                systemHealthy = false;
                brokenInheritancePointers++;
            }
            
            try {
                checkCircularReference(child, new HashSet<>());
            } catch (IllegalStateException e) {
                sb.append("[VALIDATION ERROR] ").append(e.getMessage()).append("\n");
                systemHealthy = false;
                circularLoopFailures++;
            }
        }

        if (systemHealthy) {
            sb.append("[STATUS] Validation successful! No broken pointers or cyclic rules found.\n\n");
        } else {
            sb.append("[STATUS] Validation failed. See errors logged above.\n\n");
        }

        // --- NEW METRICS SUMMARY CALCULATION SECTION ---
        int totalProfilesLoaded = registry.size();
        int customInheritedProfiles = inheritanceMap.size();
        int standaloneRootProfiles = totalProfilesLoaded - customInheritedProfiles;
        
        int totalIndividualAttributesParsed = 0;
        for (SimpleAttributeSet attrSet : registry.values()) {
            if (attrSet != null) {
                totalIndividualAttributesParsed += attrSet.getAttributeCount();
				if (attrSet.isDefined(StyleConstants.NameAttribute))
					totalIndividualAttributesParsed -= 1;
            }
        }

        sb.append("SUMMARY COUNT METRICS:\n");
        sb.append("  • Total Profiles Loaded       = ").append(totalProfilesLoaded).append("\n");
        sb.append("  • Standalone Root Profiles    = ").append(standaloneRootProfiles).append("\n");
        sb.append("  • Inherited Child Profiles    = ").append(customInheritedProfiles).append("\n");
        sb.append("  • Total Attributes Configured = ").append(totalIndividualAttributesParsed).append("\n");
        
        if (!systemHealthy) {
            sb.append("  • Broken Parent Links Found   = ").append(brokenInheritancePointers).append("\n");
            sb.append("  • Circular Reference Loops    = ").append(circularLoopFailures).append("\n");
        }
        sb.append("\n");

        return systemHealthy;
    }

	/**
     * Recursively traverses configuration branches, formats the multi-component 
     * attribute outputs, and returns a single formatted tree structural block string.
	 * 
	 * @param sb accumulates the formatted tree
	 * @param styleName
	 * @param prefix indent so display nests
	 * @param isLast
	 */
    static void generateDiagnosticTree(StringBuilder sb, String styleName, String prefix, boolean isLast) {
		verifyNotEDT();
        sb.append(prefix).append(isLast ? "└── " : "├── ").append(styleName).append("\n");
        
        SimpleAttributeSet localAttributes = registry.get(styleName);
        if (localAttributes != null) {
            String attrPrefix = prefix + (isLast ? "    " : "│   ");
            
            // Map over native StyleConstants
            appendStyleConstantIfPresent(sb, attrPrefix, localAttributes);
            
            // Map over JTextArea and Scroll configuration parameters explicitly
            appendCustomAttributeIfPresent(sb, attrPrefix, localAttributes, LINE_WRAP_KEY, "JTextArea Line Wrap");
            appendCustomAttributeIfPresent(sb, attrPrefix, localAttributes, WORD_WRAP_KEY, "JTextArea Word Wrap");
            appendCustomAttributeIfPresent(sb, attrPrefix, localAttributes, SCROLLBARS_KEY, "JScrollPane Policy");
            appendCustomAttributeIfPresent(sb, attrPrefix, localAttributes, AUTOSCROLL_KEY, "JScrollPane Autoscroll");
        }

        // Locate downstream dependents
        List<String> children = new ArrayList<>();
        for (Map.Entry<String, String> entry : inheritanceMap.entrySet()) {
            if (entry.getValue().equals(styleName)) {
                children.add(entry.getKey());
            }
        }

        for (int i = 0; i < children.size(); i++) {
            boolean lastChild = (i == children.size() - 1);
            generateDiagnosticTree(sb, children.get(i), prefix + (isLast ? "    " : "│   "), lastChild);
        }
    }

    /**
     * Inspects standard StyleConstants parameters to log out core properties.
     */
    private static void appendStyleConstantIfPresent(StringBuilder sb, String prefix, AttributeSet style) {
        // Query attributes directly from the specific profile block
        // style.getAttributeNames().asIterator().forEachRemaining(name -> {
        // });
        for (Enumeration<?> names = style.getAttributeNames(); names.hasMoreElements();) {
            Object name = names.nextElement();
            
            // Filter out custom properties handled by the secondary parser block
            switch(name.toString()) {
            case LINE_WRAP_KEY, WORD_WRAP_KEY, SCROLLBARS_KEY, AUTOSCROLL_KEY -> {
                continue;
            }
            }
			if (name == StyleConstants.NameAttribute)
				continue;
            
            Object value = style.getAttribute(name);
            String cleanName = name.toString();

			boolean isSpecial = KEEP.equals(value) || DEFAULT.equals(value);
            
            // Translate the abstract styling keys to human-friendly layout strings
            // Taken out, better to match style files, See StyleDiagnosticEngine

            // Most known key names are simple lower case words except Alignment.
            // And translate value to knows strings
            if (!isSpecial && cleanName.contains("Alignment")) {
                cleanName = "alignment";
                int align = StyleConstants.getAlignment(style);
                value = (align == StyleConstants.ALIGN_CENTER) ? "center" : (align == StyleConstants.ALIGN_RIGHT) ? "right" : "left";
            }

            sb.append(prefix).append("  • ").append(cleanName).append(" = ").append(value).append("\n");
        }
    }

    /**
     * Helper mapping method to securely extract custom context keys out of target
	 * AttributeSet objects.
     */
    private static void appendCustomAttributeIfPresent(StringBuilder sb, String prefix, AttributeSet style, String key, @SuppressWarnings("unused") String label) {
        Object val = style.getAttribute(key);
        if (val == null)
            return;
        sb.append(prefix).append("  • ").append(key).append(" = ").append(val).append("\n");
    }

    private static void checkCircularReference(String current, Set<String> visited) {
		logger.log(DEBUG, () -> sf("checkCircularReference: %s: %s", current, visited.toString()));
        if (visited.contains(current)) {
            throw new IllegalStateException("Circular inheritance path detected: " + String.join(" -> ", visited) + " -> " + current);
        }
        visited.add(current);
        String parent = inheritanceMap.get(current);
        if (parent != null) {
            checkCircularReference(parent, visited);
        }
    }

    private static AttributeSet getResolvedStyle(String targetStyle) {
        return getResolvedStyleHelper(targetStyle, new HashSet<>());
    }

	// It seems like, in getResolvedStyleHelper, the line
	//     if(currentParentStyle != null && !Objects.equals(parentStyle, currentParentStyle))
	// could be replaced by
	//     if(currentParentStyle != null && parentStyle != currentParentStyle)
	//
	// Is it worth doing something to avoid re-resolving every time?
	//
	// TODO: if(!resolved) resolve evertythinge
	//   OR: resolve as load
	//   OR: if (!resolved(name)) as load
	//   OR: resolveChainHierarchy(styleName); short circuits
	// return getResolvedStyle(styleName);
	// return registry.get(current);
	//
    private static AttributeSet getResolvedStyleHelper(String currentName, Set<String> visited) {
		logger.log(DEBUG, sf("resolveChainHierarchyHelper: %s: %s", currentName, visited.toString()));
        if (visited.contains(currentName)) {
            throw new IllegalStateException("resolveChainHierarchyHelper: Circular inheritance path detected: " + String.join(" -> ", visited) + " -> " + currentName);
        }
        
        SimpleAttributeSet currentStyle = registry.get(currentName);
        if (currentStyle == null) return null;
        
        visited.add(currentName);
        String parentName = inheritanceMap.get(currentName);

        if (parentName != null && registry.containsKey(parentName)) {
            AttributeSet parentStyle = getResolvedStyleHelper(parentName, visited);
			AttributeSet currentParentStyle = currentStyle.getResolveParent();
			if(currentParentStyle != null && parentStyle != currentParentStyle) {
				String s = sf("%s old/new parent: %s != %s",
						parentName, parentStyle, currentParentStyle);
				logger.log(ERROR, s);
				throw new IllegalStateException(s);
			}
            if (parentStyle != null) {
                currentStyle.setResolveParent(parentStyle);
				logger.log(DEBUG, sf("setResolveParent: %s %s", currentName, parentName));
            }
        }
        return currentStyle;
    }

// InputStream is = getClass().getClassLoader().getResourceAsStream("config.json");
// String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

// BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

	/**
	 * Returned by load*. If error, then no changes made. diagnostics and trees
	 * are the results of running executeFullStyleDiagnostics after the load
	 * finishes.
	 */
	public record LoadStatus(boolean ok, String diagnostics, String trees){}

	/**
	 * Load styles from a file.
	 * If an exception occurs while processing, any changes
	 * made up to the exception are backed out.
	 * 
	 * @param path must have either {@code .properties} or {@code .json} extension
	 * @return 
	 * @throws IOException 
	 */
    public static synchronized LoadStatus loadStyles(Path path) throws IOException {
		verifyNotEDT();
        String lowerPath = path.toString().toLowerCase();
        if (lowerPath.endsWith(".json")) {
            return loadStylesFromJson(path);
        } else if (lowerPath.endsWith(".properties")) {
            return loadStylesFromProperties(path);
        } else {
            throw new IllegalArgumentException("Unsupported file extension.");
        }
    }


	private static void loadStylesStarting() {
		initialStyleNames.clear();
		newStyleNames.clear();
		initialStyleNames.addAll(registry.keySet());
	}

	private static LoadStatus loadStylesCompleted(boolean cleanFinish) {
		// If not a cleanFinish or diagnostics fail, then back out any changes.

		LoadStatus status = executeFullStyleDiagnostics(false);
		if (!(cleanFinish && status.ok)) {
			newStyleNames.stream().forEach(item -> {
				registry.remove(item);
				inheritanceMap.remove(item);
			});
			// make sure ok is false
			status = new LoadStatus(false, status.diagnostics, status.trees);
		}
		initialStyleNames.clear();
		newStyleNames.clear();
		return status;
	}

    private static LoadStatus loadStylesFromProperties(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return loadStylesFromProperties(reader);
		}
	}

	/**
	 * Load styles in properties format.
	 * If an exception occurs while processing, any changes
	 * made up to the exception are backed out.
	 * @param reader
	 * @return 
	 * @throws IOException 
	 */
    public static synchronized LoadStatus loadStylesFromProperties(Reader reader) throws IOException {
		verifyNotEDT();
        Properties props = new Properties();
        try (reader) {
            props.load(reader);
        }
		return loadStylesFromProperties(props);
    }

    private static LoadStatus loadStylesFromProperties(Properties props) throws IOException {
		boolean cleanFinish = false;
		loadStylesStarting();
		LoadStatus status;
		try {
			for (String key : props.stringPropertyNames()) {
				int firstDot = key.indexOf('.');
				if (firstDot == -1) continue;
				String configName = key.substring(0, firstDot);
				String propertyName = key.substring(firstDot + 1);
				String value = props.getProperty(key);
				
				if (initialStyleNames.contains(configName))
					throw new IllegalArgumentException("StyleName already exists");
				newStyleNames.add(configName); // happens before registry changed
				if ("inherits".equalsIgnoreCase(propertyName)) {
					inheritanceMap.put(configName, value.trim());
				} else {
					SimpleAttributeSet attributeSet = registry.computeIfAbsent(
							configName, k -> {
								SimpleAttributeSet attrSet = new SimpleAttributeSet();
								attrSet.addAttribute(StyleConstants.NameAttribute, configName);
								return attrSet;
							});
					mapPropertyToAttributeSet(attributeSet, propertyName, value);
				}
			}
			cleanFinish = true;
		} finally {
			status = loadStylesCompleted(cleanFinish);
		}
		return status;
    }

    private static LoadStatus loadStylesFromJson(Path path) throws IOException {
		return loadStylesFromJson(Files.readString(path));
	}

	/**
	 * Load styles in json format.
	 * If an exception occurs while processing, any changes
	 * made up to the exception are backed out.
	 * 
	 * @param reader
	 * @return 
	 * @throws IOException 
	 */
    public static synchronized LoadStatus loadStylesFromJson(Reader reader) throws IOException {
		verifyNotEDT();
		StringWriter sw = new StringWriter(700);
		reader.transferTo(sw);
		return loadStylesFromJson(sw.toString());

	//
	// TODO: use a JSON library. Jackson/
	//
	}

    private static LoadStatus loadStylesFromJson(String _content) throws IOException {
		boolean cleanFinish = false;
		loadStylesStarting();
		LoadStatus status;
		try {
			String content = _content.trim();
			if (content.startsWith("{") && content.endsWith("}")) {
				content = content.substring(1, content.length() - 1).trim();
			}
			Pattern blockPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^}]+)\\}");
			Matcher blockMatcher = blockPattern.matcher(content);
			
			while (blockMatcher.find()) {
				String configName = blockMatcher.group(1);
				String body = blockMatcher.group(2);
				if (initialStyleNames.contains(configName))
					throw new IllegalArgumentException(sf("StyleName '%s' already exists", configName));
				newStyleNames.add(configName); // happens before registry changed
				if (registry.containsKey(configName))
					throw new IllegalArgumentException(sf("StyleName '%s' already defined in this load", configName));

				
				SimpleAttributeSet attributeSet = registry.computeIfAbsent(
						configName, k -> {
							SimpleAttributeSet attrSet = new SimpleAttributeSet();
							attrSet.addAttribute(StyleConstants.NameAttribute, configName);
							return attrSet;
						});
				Pattern pairPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]+)\"|([^,\\s]+))");
				Matcher pairMatcher = pairPattern.matcher(body);
				
				while (pairMatcher.find()) {
					String propertyName = pairMatcher.group(1);
					String value = pairMatcher.group(2) != null ? pairMatcher.group(2) : pairMatcher.group(3);
					value = value.trim();
					
					if ("inherits".equalsIgnoreCase(propertyName)) {
						inheritanceMap.put(configName, value);
					} else {
						mapPropertyToAttributeSet(attributeSet, propertyName, value);
					}
				}
			}
			cleanFinish = true;
		} finally {
			status = loadStylesCompleted(cleanFinish);
		}
		return status;
    }

	/** like Runnable, but IOException */
	public interface RunIO {

		/**
		 * Run operation that might throw IOException.
		 * @throws IOException
		 */
		void run() throws IOException;
	}

	/**
	 * Convenience method; if this is called on the EDT, run the load on different thread.
	 * Warning: if EDT, this will hang the EDT until load completes;
	 * which is quickly unless blocked by a current load from a file.
	 * @param load runnable that does the load
	 * @throws java.io.IOException
	 */
	// TODO: could add dialog with spinner
	public static void loadFromAnyThread(RunIO load) throws IOException {
		//System.err.printf("******* LOADING: %s\n", Instant.now());
		if (!EventQueue.isDispatchThread()) {
			load.run();
		} else {
			SwingWorker<Object, Object> sw = new SwingWorker<>() {
				@Override
				protected Object doInBackground() throws Exception {
					load.run();
					return null;
				}
			};
			sw.execute();
			Throwable ex = null;
			try {
				sw.get(); // Waits for completion.
			} catch (InterruptedException x) {
				ex = x;
			} catch (ExecutionException x) {
				ex = x.getCause();
			}
			if (ex != null) {
				if (ex instanceof IOException iox)
					throw iox;
				throw new IOException("Problem loading TextStyles", ex);
			}
		}
		//System.err.printf("******* LOADED: %s\n", Instant.now());
	}

	/** Convert, and return, style name to AttributeSet name */
	private static Object styleAttrName2AttrName(String key) {
		Object attrName = switch (key) {
        case "alignment" -> StyleConstants.Alignment;
        case "foreground" -> StyleConstants.Foreground;
        case "background" -> StyleConstants.Background;
        case "opaque" -> OPAQUE;
        case "fontFamily" -> StyleConstants.FontFamily;
        case "fontSize" -> StyleConstants.FontSize;
        case "bold" -> StyleConstants.Bold;
        case "italic" -> StyleConstants.Italic;
        case "underline" -> StyleConstants.Underline;
        case "strikethrough" -> StyleConstants.StrikeThrough;

        // JTextArea Only Layouts (Stored as custom attribute objects)
        case "linewrap" -> LINE_WRAP_KEY;
        case "wordwrap" -> WORD_WRAP_KEY;
        case "scrollbars" -> SCROLLBARS_KEY;
        case "autoscroll" -> AUTOSCROLL_KEY;
		default -> throw new IllegalArgumentException(sf("Unhandled style attribute name '%s'", key));
		};
		return attrName;
	}

	/**
	 * keep: mean do not modify existing value;<br>
	 * useDflt: if value is null;<br>
	 * dflt: null means special handling.
	 */
	record ValueDefault(boolean keep, boolean useDflt, Object value, Object dflt) {
		ValueDefault(Object value, Object dflt) {
			this(value == null || KEEP.equals(value), DEFAULT.equals(value),
					value, dflt);
		}
	}

	private static ValueDefault styleAttrName2Default(String key, AttributeSet attrSet) {
		Object dflt;
		Object attrName = switch (key) {
        case "foreground" -> { dflt = Color.BLACK; yield StyleConstants.Foreground; }
        case "background" -> { dflt = Color.WHITE; yield StyleConstants.Background; }
		case "opaque" -> { dflt = true; yield OPAQUE; }
        case "fontFamily" -> { dflt = "Monospaced"; yield StyleConstants.FontFamily; }
        case "fontSize" -> { dflt = 12; yield StyleConstants.FontSize; }
        case "bold" -> { dflt = false; yield StyleConstants.Bold; }
        case "italic" -> { dflt = false; yield StyleConstants.Italic; }
        case "underline" -> { dflt = false; yield StyleConstants.Underline; }
        case "strikethrough" -> { dflt = false; yield StyleConstants.StrikeThrough; }

		// JTextField only
        case "alignment" -> { dflt = StyleConstants.ALIGN_LEFT; yield StyleConstants.Alignment; }
        // JTextArea Only Layouts (Stored as custom attribute objects)
        case "linewrap"   -> { dflt = false; yield LINE_WRAP_KEY; }
        case "wordwrap"   -> { dflt = false; yield WORD_WRAP_KEY; }
        case "scrollbars" -> { dflt = "asneeded"; yield SCROLLBARS_KEY; }
        case "autoscroll" -> { dflt = null; yield AUTOSCROLL_KEY; }
		default -> throw new IllegalArgumentException(sf("Unhandled style attribute name '%s'", key));
		};

		return new ValueDefault(attrSet.getAttribute(attrName), dflt);
	}

    /**
     * Maps raw properties out of files to the SimpleAttributeSet.
     */
    private static void mapPropertyToAttributeSet(SimpleAttributeSet attrSet, String key, String value) {
		Object attrName = styleAttrName2AttrName(key);
		if (attrSet.isDefined(attrName)) {
			String msg = sf("AttrName '%s' already set in '%s'",
					key, attrSet.getAttribute(StyleConstants.NameAttribute));
			logger.log(WARNING, msg);
			throw new IllegalArgumentException(msg);
		}
		if (value.equals("keep")) {
			attrSet.addAttribute(attrName, "keep");
			return;
		}
		if (value.equals("default")) {
			attrSet.addAttribute(attrName, "default");
			return;
		}

        switch (key) {
        // JTextField Only Layout
        case "alignment" -> {
            String cleanAlign = value.trim().toLowerCase();
            int alignValue = switch(cleanAlign) {
            case "center" -> StyleConstants.ALIGN_CENTER;
            case "right" -> StyleConstants.ALIGN_RIGHT;
            case "left" -> StyleConstants.ALIGN_LEFT;
            default -> { throw new IllegalArgumentException(sf("'%s' not for '%s'", cleanAlign, key)); }
            };
            StyleConstants.setAlignment(attrSet, alignValue);
        }

        // Shared Core Attributes
        case "foreground" -> StyleConstants.setForeground(attrSet, parseColor(value));
        case "background" -> StyleConstants.setBackground(attrSet, parseColor(value));
        case "opaque" -> attrSet.addAttribute(OPAQUE, Boolean.valueOf(value));
        case "fontFamily" -> StyleConstants.setFontFamily(attrSet, value);
        case "fontSize" -> StyleConstants.setFontSize(attrSet, Integer.parseInt(value));
        case "bold" -> StyleConstants.setBold(attrSet, Boolean.parseBoolean(value));
        case "italic" -> StyleConstants.setItalic(attrSet, Boolean.parseBoolean(value));
        case "underline" -> StyleConstants.setUnderline(attrSet, Boolean.parseBoolean(value));
        case "strikethrough" -> StyleConstants.setStrikeThrough(attrSet, Boolean.parseBoolean(value));

        // JTextArea Only Layouts (Stored as custom attribute objects)
        case "linewrap" -> attrSet.addAttribute(LINE_WRAP_KEY, Boolean.valueOf(value));
        case "wordwrap" -> attrSet.addAttribute(WORD_WRAP_KEY, Boolean.valueOf(value));

        // Scroll Specific
        case "scrollbars" -> {
            String val = value.trim().toLowerCase();
            switch (val) {
            case "horizontal", "vertical", "both", "none", "asneeded" -> { }
            default -> { throw new IllegalArgumentException(sf("'%s' not for '%s'", val, key)); }
            }
            attrSet.addAttribute(SCROLLBARS_KEY, val);
        }
        case "autoscroll" -> attrSet.addAttribute(AUTOSCROLL_KEY, Boolean.valueOf(value));
        }
		// Something should always gets set.
		if (!attrSet.isDefined(attrName))
			throw new IllegalStateException(sf("Nothing was set for %s", attrName));
    }
    
    /**
     * Tries parsing a color string as a standard hex literal first (e.g., "#FF0000").
     * If that fails, uses reflection to look up case-insensitive color names in java.awt.Color,
     * ignoring any underscores (e.g., "dark_GRAY", "darkGray", "black").
     */
    private static Color parseColor(String value) {
        if (value == null) return Color.BLACK;

        String val = value.trim();
        
        // 1. Try resolving standard Web/Hex constants
        if (val.startsWith("#")) {
            try {
                return Color.decode(val);
            } catch (NumberFormatException _) {
				logger.log(DEBUG, () -> sf("Invalid hex format: %s. Attempting string lookup.", val));
            }
        }
        
        // 2. Fall back to case-insensitive, symbol-agnostic reflection lookup
        String cleanValue = val.replace("_", "").toLowerCase();
        
        // Quick custom map for standard colors to speed up runtime lookup
        Color color = switch (cleanValue) {
        case "black"       -> Color.BLACK;
        case "blue"        -> Color.BLUE;
        case "cyan"        -> Color.CYAN;
        case "darkgray"    -> Color.DARK_GRAY;
        case "gray"        -> Color.GRAY;
        case "green"       -> Color.GREEN;
        case "lightgray"   -> Color.LIGHT_GRAY;
        case "magenta"     -> Color.MAGENTA;
        case "orange"      -> Color.ORANGE;
        case "pink"        -> Color.PINK;
        case "red"         -> Color.RED;
        case "white"       -> Color.WHITE;
        case "yellow"      -> Color.YELLOW;
        default -> null;
        };
        if (color != null)
            return color;
        
        // Deep reflection fallback for environment-specific or extended Look-And-Feel colors
        try {
            for (java.lang.reflect.Field field : Color.class.getFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
						&& field.getType() == Color.class) {
                    String fieldName = field.getName().replace("_", "").toLowerCase();
                    if (fieldName.equals(cleanValue)) {
                        return (Color) field.get(null);
                    }
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException e) {
			String msg = sf("Reflection failed for color property: %s", val);
			logger.log(ERROR, msg , e);
			RuntimeException ex = e instanceof IllegalAccessException
					? new IllegalArgumentException(msg, e)
					: (RuntimeException)e;
			throw ex;
        }
        
        // 3. Ultimate safe fallback baseline
		logger.log(WARNING, () -> sf("Color '%s' unrecognized. Defaulting to BLACK.", val));
        return Color.BLACK;
    }

    // This is probably good enough...
    private static JScrollPane findScrollPane(JTextArea c) {
        return (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, c);
    }

    /**
     * Styling engine for: JComponent.
	 * The style's name is saved as the JCompoent's
	 * {@link #STYLE_NAME} ClientProperty.
	 * 
	 * @param jComponent
	 * @param style 
     */
	public static void applyStyle(JComponent jComponent, AttributeSet style) {
		verifyEDT();
		ComponentMemento resetMemento = resetMementos.get(jComponent);
		if (style == RESET) {
			// Could remove the resetMemento map entry, but performance waste
			// since RESET is common; instead could have "forgetResetMemento" method
			if (resetMemento != null) {
				resetMemento.restoreTo(jComponent);
			}
			jComponent.putClientProperty(STYLE_NAME, null);
			return;
		}

		// There are no exceptions thrown during applyStyle, so just do it now.
		// If there is a problem, this could be a bread crumb.
		jComponent.putClientProperty(STYLE_NAME, style.getAttribute(StyleConstants.NameAttribute));

		if (resetMemento == null)
			resetMementos.put(jComponent, getMemento(jComponent));

		if (jComponent instanceof JTextArea textArea) {
			applyStyle(textArea, style);
			return;
		}

		// [JTextField Only Attribute]: Horizontal Component Position Alignment
		if (jComponent instanceof JTextField textField) {
			ValueDefault vd = styleAttrName2Default("alignment", style);
			if (!vd.keep) {
				int alignment = (int) (vd.useDflt() ? vd.dflt() : vd.value());
				switch (alignment) {
				case StyleConstants.ALIGN_CENTER: textField.setHorizontalAlignment(JTextField.CENTER); break;
				case StyleConstants.ALIGN_RIGHT:  textField.setHorizontalAlignment(JTextField.RIGHT); break;
				case StyleConstants.ALIGN_LEFT:
				case StyleConstants.ALIGN_JUSTIFIED: // JTextField does not support true body justification, fall back to left
				default:
					textField.setHorizontalAlignment(JTextField.LEFT); break;
				}
			}
		}
		
		// Apply shared core layout decorations (Colors & Fonts)
		applyCoreStyle(jComponent, style);
	}

    /**
     * Styling engine for: JTextArea
	 * 
	 * @param textArea
	 * @param style 
     */
    private static void applyStyle(JTextArea textArea, AttributeSet style) {
		verifyEDT();
        // [JTextArea Only Attribute]: Multi-line Line wrapping configuration
		boolean isOn;
		ValueDefault vd = styleAttrName2Default("linewrap", style);
		if (!vd.keep()) {
			isOn = (boolean) (vd.useDflt ? vd.dflt() : vd.value());
			textArea.setLineWrap(isOn);
		}

        // [JTextArea Only Attribute]: Word boundary wrap breaking configuration
		vd = styleAttrName2Default("wordwrap", style);
		if (!vd.keep()) {
			isOn = (boolean) (vd.useDflt ? vd.dflt() : vd.value());
			textArea.setWrapStyleWord(isOn);
		}

        // Apply shared core layout decorations (Colors & Fonts)
        applyCoreStyle(textArea, style);

        JScrollPane jsp = findScrollPane(textArea);
        if (jsp == null)
            return;

		vd = styleAttrName2Default("scrollbars", style);
		if (!vd.keep()) {
			String policy = (String) (vd.useDflt ? vd.dflt() : vd.value());
			int vsb = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
			int hsb = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED;
            switch (policy) {
                case "asneeded" -> {
                    vsb = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
                    hsb = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED;
                }
                case "vertical" -> {
                    vsb = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;
                    hsb = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
                }
                case "horizontal" -> {
                    vsb = JScrollPane.VERTICAL_SCROLLBAR_NEVER;
                    hsb = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS;
                }
                case "both" -> {
                    vsb = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;
                    hsb = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS;
                }
                case "none" -> {
                    vsb = JScrollPane.VERTICAL_SCROLLBAR_NEVER;
                    hsb = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
                }
            }
			jsp.setVerticalScrollBarPolicy(vsb);
			jsp.setHorizontalScrollBarPolicy(hsb);
		}

		vd = styleAttrName2Default("autoscroll", style);
		// NOTE: for autoscroll, the default is to do nothing
		if (!vd.useDflt()) {
			DefaultCaret caret = (DefaultCaret) textArea.getCaret();
			if (!vd.keep())
				caret.setUpdatePolicy((boolean) vd.value()
						? DefaultCaret.ALWAYS_UPDATE : DefaultCaret.NEVER_UPDATE); 
		}
    }

    /**
     * Extracts and applies properties common to all JTextComponent fields.
	 * @param component
	 * @param style
     */
    private static void applyCoreStyle(JComponent component, AttributeSet style) {
        // 1. Process Core Component Foreground/Background Colors

		ValueDefault vd = styleAttrName2Default("foreground", style);
		if (!vd.keep()) {
			component.setForeground((Color)(vd.useDflt ? vd.dflt() : vd.value()));
		}

		vd = styleAttrName2Default("background", style);
		if (!vd.keep()) {
			component.setBackground((Color)(vd.useDflt ? vd.dflt() : vd.value()));
		}

		vd = styleAttrName2Default("opaque", style);
		if (!vd.keep()) {
			component.setOpaque((boolean) (vd.useDflt() ? vd.dflt() : vd.value()));
		}

        // 2. Process Core Font Typography configurations

        Map<TextAttribute, Object> fontAttributes = new HashMap<>();
		Font currentFont = component.getFont();

		vd = styleAttrName2Default("fontFamily", style);
		String family = vd.keep() ? currentFont.getFamily()
				: (String) (vd.useDflt() ?  vd.dflt() : vd.value());
        
		vd = styleAttrName2Default("fontSize", style);
		int size = vd.keep ? currentFont.getSize()
				: (int) (vd.useDflt() ? vd.dflt() : vd.value());

        fontAttributes.put(TextAttribute.FAMILY, family);
        fontAttributes.put(TextAttribute.SIZE, (float) size);
        
		Map<TextAttribute, ?> currentFontAttributes = currentFont.getAttributes();

		vd = styleAttrName2Default("underline", style);
		boolean isOn = vd.keep()
				? TextAttribute.UNDERLINE_ON.equals(
						currentFontAttributes.get(TextAttribute.UNDERLINE))
				: vd.useDflt() ? false : (boolean)vd.value();
		if (isOn)
			fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

		vd = styleAttrName2Default("strikethrough", style);
		isOn = vd.keep()
				? TextAttribute.STRIKETHROUGH_ON.equals(
						currentFontAttributes.get(TextAttribute.STRIKETHROUGH))
				: vd.useDflt() ? false : (boolean)vd.value();
		if (isOn)
			fontAttributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);

		int currentFontStyle = currentFont.getStyle();
        int fontStyle = Font.PLAIN;
		vd = styleAttrName2Default("bold", style);
		isOn = vd.keep() ? (currentFontStyle & Font.BOLD) != 0
				: (boolean) (vd.useDflt() ? vd.dflt() : vd.value());
		fontStyle |= isOn ? Font.BOLD : 0;

		vd = styleAttrName2Default("italic", style);
		isOn = vd.keep() ? (currentFontStyle & Font.ITALIC) != 0
				: (boolean) (vd.useDflt() ? vd.dflt() : vd.value());
		fontStyle |= isOn ? Font.ITALIC : 0;

        Font baseFont = new Font(family, fontStyle, size);
        component.setFont(baseFont.deriveFont(fontAttributes));
        component.repaint();
    }

	static ComponentMemento getMemento(JComponent c) {
		return new ComponentMemento(c);
	}

	/**
	 * For saving and restoring component state changed by styles.
	 */
	public static class ComponentMemento {
        // Shared Core Core Properties
        private final Color foreground;
        private final Color background;
        private final Font font;
        private final boolean opaque;

		// JTextField Specific Properties
		private int alignment;

        // JTextArea Specific Properties
        private final boolean isTextArea;
        private boolean lineWrap;
        private boolean wordWrap;
        private int vsb;
        private int hsb;

        /**
         * Capture state snapshot;
         * Accepts any standard JTextComponent (JTextField, JTextArea, JPasswordField, etc.)
		 * @param jComponent
         */
        public ComponentMemento(JComponent jComponent) {
            Objects.requireNonNull(jComponent);
			verifyEDT();
            // Capture Shared Core Layout Parameters
            foreground = jComponent.getForeground();
            background = jComponent.getBackground();
            font = jComponent.getFont();
            opaque = jComponent.isOpaque();


			// JTextField parameters
            if (jComponent instanceof JTextField textField) {
				alignment = textField.getHorizontalAlignment();
			}
			
			// Identify type and pull multi-line parameters if it's a JTextArea
            if (jComponent instanceof JTextArea textArea) {
                isTextArea = true;
                lineWrap = textArea.getLineWrap();
                wordWrap = textArea.getWrapStyleWord();
                JScrollPane jsp = findScrollPane(textArea);
                if (jsp != null) {
                    vsb =  jsp.getVerticalScrollBarPolicy();
                    hsb =  jsp.getHorizontalScrollBarPolicy();
                }
            } else {
                isTextArea = false;
            }
        }

        /**
         * Restore state snapshot;
         * Restores all core attributes and conditionally re-applies multi-line rules.
		 * @param jComponent
         */
        public void restoreTo(JComponent jComponent) {
			Objects.requireNonNull(jComponent);
			verifyEDT();
            // Restore Shared Layout States
            jComponent.setForeground(foreground);
            jComponent.setBackground(background);
            jComponent.setFont(font);
            jComponent.setOpaque(opaque);

            // Safely re-apply specific wrap layers only if targets match up

            if (!isTextArea && jComponent instanceof JTextField textField) {
				textField.setHorizontalAlignment(alignment);
			}

            if (isTextArea && jComponent instanceof JTextArea textArea) {
                //JTextArea textArea = (JTextArea) jComponent;
                textArea.setLineWrap(lineWrap);
                textArea.setWrapStyleWord(wordWrap);
                JScrollPane jsp = findScrollPane(textArea);
                if (jsp != null) {
                    jsp.setVerticalScrollBarPolicy(vsb);
                    jsp.setHorizontalScrollBarPolicy(hsb);
                }
            }
            
            jComponent.repaint();
        }

		/** {@inheritDoc} */
		@Override
		public String toString()
		{
			String hexFore = sf("#%06x", (0xFFFFFF & foreground.getRGB()));
			String hexBack = sf("#%06x", (0xFFFFFF & background.getRGB()));
			Map<TextAttribute, ?> currentFontAttributes = font.getAttributes();
			boolean underline = TextAttribute.UNDERLINE_ON.equals(
					currentFontAttributes.get(TextAttribute.UNDERLINE));
			boolean strikethrough = TextAttribute.STRIKETHROUGH_ON.equals(
					currentFontAttributes.get(TextAttribute.STRIKETHROUGH));
			String stringAlignment = switch (alignment) {
			case JTextField.LEFT -> "left";
			case JTextField.CENTER -> "center";
			case JTextField.RIGHT -> "right";
			default -> "" + alignment;
			};

			return "TextComponentStyleMemento{" + "foreground=" + hexFore
					+ ", background=" + hexBack + ", font=" + font + ", opaque=" + opaque
					+ ", underline=" + underline + ", strikethrough=" + strikethrough
					+ ", alignment=" + stringAlignment + ", isTextArea=" + isTextArea
					+ ", lineWrap=" + lineWrap + ", wordWrap=" + wordWrap
					+ ", vsb=" + vsb + ", hsb=" + hsb + '}';
		}

		/** {@inheritDoc} */
		@Override
		public int hashCode()
		{
			int hash = 7;
			return hash;
		}

		/** {@inheritDoc} */
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final ComponentMemento other = (ComponentMemento) obj;
			if (this.opaque != other.opaque)
				return false;
			if (this.alignment != other.alignment)
				return false;
			if (this.isTextArea != other.isTextArea)
				return false;
			if (this.lineWrap != other.lineWrap)
				return false;
			if (this.wordWrap != other.wordWrap)
				return false;
			if (this.vsb != other.vsb)
				return false;
			if (this.hsb != other.hsb)
				return false;
			if (!Objects.equals(this.foreground, other.foreground))
				return false;
			if (!Objects.equals(this.background, other.background))
				return false;
			return Objects.equals(this.font, other.font);
		}
    }

	/**
	 * Provides a read only AttributeSet of any subclass.
	 */
	public static final class AttributeSetDelegate implements AttributeSet {
		private final AttributeSet attrs;

		/**
		 * 
		 * @param attrs 
		 */
		public AttributeSetDelegate(AttributeSet attrs)
		{
			this.attrs = attrs;
		}

		/** {@inheritDoc} */
		@Override
		public int getAttributeCount()
		{
			return attrs.getAttributeCount();
		}

		/** {@inheritDoc} */
		@Override
		public boolean isDefined(Object attrName)
		{
			return attrs.isDefined(attrName);
		}

		/** {@inheritDoc} */
		@Override
		public boolean isEqual(AttributeSet attr)
		{
			return attrs.isEqual(attr);
		}

		/** {@inheritDoc} */
		@Override
		public AttributeSet copyAttributes()
		{
			return attrs.copyAttributes();
		}

		/** {@inheritDoc} */
		@Override
		public Object getAttribute(Object key)
		{
			return attrs.getAttribute(key);
		}

		/** {@inheritDoc} */
		@Override
		public Enumeration<?> getAttributeNames()
		{
			return attrs.getAttributeNames();
		}

		/** {@inheritDoc} */
		@Override
		public boolean containsAttribute(Object name, Object value)
		{
			return attrs.containsAttribute(name, value);
		}

		/** {@inheritDoc} */
		@Override
		public boolean containsAttributes(AttributeSet attributes)
		{
			return attrs.containsAttributes(attributes);
		}

		/** {@inheritDoc} */
		@Override
		public AttributeSet getResolveParent()
		{
			return attrs.getResolveParent();
		}

		/** {@inheritDoc}
		 * @return  */
		@Override
		public int hashCode()
		{
			return attrs.hashCode();
		}
		
		/** {@inheritDoc} */
		@Override
		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		public boolean equals(Object obj)
		{
			return attrs.equals(obj);
		}

		/** {@inheritDoc}
		 * @return  */
		@Override
		public String toString()
		{
			return attrs.toString();
		}
		

	}
}