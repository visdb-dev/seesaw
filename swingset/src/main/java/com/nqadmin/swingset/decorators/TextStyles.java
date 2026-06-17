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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.text.*;

import com.nqadmin.swingset.utils.SSUtils;

import static com.nqadmin.swingset.utils.SSUtils.sf;
import static java.lang.System.Logger.Level.*;

// Build with the aid of google search.
// Won't use it again except for tiny stuff.

//
// TODO: CLEANUP DEFAULT/NO-DEFAULT behavior, related javadoc
//

// TABLE WITHOUT DEFAULTS
// <table border="1">
// <caption>Attributes in a TextStyle</caption>
// <thead>
// <tr>
// <th> Attribute Name </th><th> Acceptable Values </th><th> Target Component</th>
// </tr>
// </thead>
// 
// <tbody>
// <tr>
// <td> foreground </td><td> Color: #ff2277, name </td><td> JTextField, JTextArea </td>
// </tr><tr>
// <td> background </td><td> Color: #ff2277, name </td><td> JTextField, JTextArea </td>
// </tr><tr>
// <td> fontFamily </td><td> String </td><td> JTextField, JTextArea </td>
// </tr><tr>
// <td> fontSize </td><td> int </td><td> JTextField, JTextArea </td>
// </tr><tr>
// <td> bold </td><td> true, false </td><td> JTextField, JTextArea </td>
// </tr><tr>
// <td> italic </td><td> true, false </td><td> JTextField, JTextArea </td>
// </tr><tr>
// <td> underline </td><td> true, false </td><td> JTextField, JTextArea </td>
// </tr><tr>
// <td> strikethrough </td><td> true, false </td><td> JTextField, JTextArea </td>
// </tr><tr>
// <td> alignment </td><td> "left", "center", "right" </td><td> JTextField Only  </td>
// </tr><tr>
// <td> linewrap </td><td> true, false </td><td> JTextArea Only  </td>
// </tr><tr>
// <td> wordwrap </td><td> true, false </td><td> JTextArea Only  </td>
// </tr><tr>
// <td> scrollbars </td><td> "both", "vertical", "horizontal",<br>"asneeded", "none" </td><td> JTextArea Only </td>
// </tr><tr>
// <td> autoscroll </td><td> true, false </td><td> JTextArea Only </td>
// </tr>
// </tbody>
// </table>

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
 * <td> background </td><td> Color: #ff2277, name </td><td>not opaque</td>
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
    private static final Logger logger = SSUtils.getLogger();
    
    // Custom attribute keys for JTextArea specific wrapping features
    private static final String LINE_WRAP_KEY = "linewrap";
    private static final String WORD_WRAP_KEY = "wordwrap";
    private static final String SCROLLBARS_KEY = "scrollbars";
    private static final String AUTOSCROLL_KEY = "autoscroll";

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
	private static final boolean APPLY_DOES_DEFAULTS = true;

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
		if (newStyleNames.contains(styleName))
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

	static synchronized void clearStyles() {
		verifyNotEDT();
		logger.log(INFO, () -> sf("{%s} is clearing all styles.", SSUtils.getCaller(5)));
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
        // Enumeration<?> names = style.getAttributeNames();
        // while (names.hasMoreElements()) {
            Object name = names.nextElement();
            
            // Filter out custom properties handled by the secondary parser block
            switch(name.toString()) {
            case LINE_WRAP_KEY, WORD_WRAP_KEY, SCROLLBARS_KEY, AUTOSCROLL_KEY -> {
                continue;
            }
            }
            
            Object value = style.getAttribute(name);
            String cleanName = name.toString();
            
            // Translate the abstract styling keys to human-friendly layout strings
            // Taken out, better to match style files, See StyleDiagnosticEngine

            // Most known key names are simple lower case words except Alignment.
            // And translate value to knows strings
            if (cleanName.contains("Alignment")) {
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


	private static void loadStylesStarted() {
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
		loadStylesStarted();
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
							configName, k -> new SimpleAttributeSet());
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
		loadStylesStarted();
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

				
				SimpleAttributeSet attributeSet = registry.computeIfAbsent(configName, k -> new SimpleAttributeSet());
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

    /**
     * Maps raw properties out of files to the SimpleAttributeSet.
     */
    private static void mapPropertyToAttributeSet(SimpleAttributeSet attributeSet, String key, String value) {
        switch (key) {
        // JTextField Only Layout
        case "alignment" -> {
            String cleanAlign = value.trim().toLowerCase();
            int alignValue = switch(cleanAlign) {
            case "center" -> StyleConstants.ALIGN_CENTER;
            case "right" -> StyleConstants.ALIGN_RIGHT;
            case "left" -> StyleConstants.ALIGN_LEFT;
            case "justify" -> StyleConstants.ALIGN_JUSTIFIED;
            default -> { throw new IllegalArgumentException(sf("'%s' not for '%s'", cleanAlign, key)); }
            };
            StyleConstants.setAlignment(attributeSet, alignValue);
        }

        // Shared Core Attributes
        case "foreground" -> StyleConstants.setForeground(attributeSet, parseColor(value));
        case "background" -> StyleConstants.setBackground(attributeSet, parseColor(value));
        case "fontFamily" -> StyleConstants.setFontFamily(attributeSet, value);
        case "fontSize" -> StyleConstants.setFontSize(attributeSet, Integer.parseInt(value));
        case "bold" -> StyleConstants.setBold(attributeSet, Boolean.parseBoolean(value));
        case "italic" -> StyleConstants.setItalic(attributeSet, Boolean.parseBoolean(value));
        case "underline" -> StyleConstants.setUnderline(attributeSet, Boolean.parseBoolean(value));
        case "strikethrough" -> StyleConstants.setStrikeThrough(attributeSet, Boolean.parseBoolean(value));

        // JTextArea Only Layouts (Stored as custom attribute objects)
        case "linewrap" -> attributeSet.addAttribute(LINE_WRAP_KEY, Boolean.valueOf(value));
        case "wordwrap" -> attributeSet.addAttribute(WORD_WRAP_KEY, Boolean.valueOf(value));

        // New Scroll Specific Conversions
        case "scrollbars" -> {
            String val = value.trim().toLowerCase();
            switch (val) {
            case "horizontal", "vertical", "both", "none", "asneeded" -> { }
            default -> { throw new IllegalArgumentException(sf("'%s' not for '%s'", val, key)); }
            }
            attributeSet.addAttribute(SCROLLBARS_KEY, val);
        }
        case "autoscroll" -> attributeSet.addAttribute(AUTOSCROLL_KEY, Boolean.valueOf(value));
        }
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
			logger.log(ERROR, () -> sf("Reflection failed for color property: %s", val), e);
        }
        
        // 3. Ultimate safe fallback baseline
		logger.log(WARNING, () -> sf("Color '%s' unrecognized. Defaulting to BLACK.", val));
        return Color.BLACK;
    }

    /**
     * Styling engine for: JTextField
	 * 
	 * @param textField
	 * @param style 
     */
	public static synchronized void applyStyle(JTextField textField, AttributeSet style) {
		verifyEDT();
		// [JTextField Only Attribute]: Horizontal Component Position Alignment
		boolean hasAlignment = style.getAttribute(StyleConstants.Alignment) != null;
		if (hasAlignment || APPLY_DOES_DEFAULTS) {
			int alignment = StyleConstants.getAlignment(style);
			switch (alignment) {
			case StyleConstants.ALIGN_CENTER: textField.setHorizontalAlignment(JTextField.CENTER); break;
			case StyleConstants.ALIGN_RIGHT:  textField.setHorizontalAlignment(JTextField.RIGHT); break;
			case StyleConstants.ALIGN_LEFT:
			case StyleConstants.ALIGN_JUSTIFIED: // JTextField does not support true body justification, fall back to left
			default:
				textField.setHorizontalAlignment(JTextField.LEFT); break;
			}
		}
		
		// Apply shared core layout decorations (Colors & Fonts)
		applyCoreTextAttributes(textField, style);
	}

    /**
     * Styling engine for: JTextArea
	 * 
	 * @param textArea
	 * @param style 
     */
    public static synchronized void applyStyle(JTextArea textArea, AttributeSet style) {
		verifyEDT();
        // [JTextArea Only Attribute]: Multi-line Line wrapping configuration
        Object lineWrapAttr = style.getAttribute(LINE_WRAP_KEY);
        if (lineWrapAttr instanceof Boolean attr) {
            textArea.setLineWrap(attr);
        } else if (APPLY_DOES_DEFAULTS) {
			textArea.setLineWrap(false); // Default standard
        }

        // [JTextArea Only Attribute]: Word boundary wrap breaking configuration
        Object wordWrapAttr = style.getAttribute(WORD_WRAP_KEY);
        if (wordWrapAttr instanceof Boolean attr) {
            textArea.setWrapStyleWord(attr);
        } else if (APPLY_DOES_DEFAULTS) {
            textArea.setWrapStyleWord(false); // Default standard
        }

        // Apply shared core layout decorations (Colors & Fonts)
        applyCoreTextAttributes(textArea, style);

        JScrollPane jsp = findScrollPane(textArea);
        if (jsp == null)
            return;

        int vsb = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
        int hsb = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED;
		if (!APPLY_DOES_DEFAULTS) {
			vsb = -1;
			hsb = -1;
		}

        if (style.getAttribute(SCROLLBARS_KEY) instanceof String policy) {
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
        }
		if (vsb != -1) { // if not APPLY_DOES_DEFAULTS
			jsp.setVerticalScrollBarPolicy(vsb);
			jsp.setHorizontalScrollBarPolicy(hsb);
		}

		// NOTE: never had a default
        if (style.getAttribute(AUTOSCROLL_KEY) instanceof Boolean autoScroll) {
            DefaultCaret caret = (DefaultCaret) textArea.getCaret();
            caret.setUpdatePolicy(autoScroll ? DefaultCaret.ALWAYS_UPDATE : DefaultCaret.NEVER_UPDATE); 
        }
    }

    // This is probably good enough...
    private static JScrollPane findScrollPane(JTextArea c) {
        return (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, c);
    }

    /**
     * Extracts and applies properties common to all JTextComponent fields.
     */
    private static void applyCoreTextAttributes(JComponent textComponent, AttributeSet style) {
        // 1. Process Core Component Foreground/Background Colors

        // Color fg = StyleConstants.getForeground(style);
        // textComponent.setForeground(fg != null ? fg : Color.BLACK);
		// NOTE: never returns null, defaults black
        textComponent.setForeground(StyleConstants.getForeground(style));

		// TODO: APPLY_DOES_DEFAULT handling
		// NOTE: never returns null, defaults black
        if (style.getAttribute(StyleConstants.Background) != null) {
			Color bg = StyleConstants.getBackground(style);
            textComponent.setBackground(bg);
            textComponent.setOpaque(true);
        } else if (APPLY_DOES_DEFAULTS) {
            textComponent.setOpaque(false);
        }

        // 2. Process Core Font Typography configurations

        Map<TextAttribute, Object> fontAttributes = new HashMap<>();
		Font currentFont = textComponent.getFont();

		String family;
		if (APPLY_DOES_DEFAULTS) {
			// NOTE: never returns null, so "family == null" is always false
			family = StyleConstants.getFontFamily(style);
			if (family == null) family = "SansSerif";
		} else {
			family = style.getAttribute(StyleConstants.Family) != null
					? StyleConstants.getFontFamily(style)
					: currentFont.getFamily();
		}
        
        int size;
		if (APPLY_DOES_DEFAULTS) {
			// NOTE: "size == 0" awlay false (unless set 0 by user
			size = StyleConstants.getFontSize(style);
			if (size <= 0) size = 12;
		} else {
			size = style.getAttribute(StyleConstants.FontSize) != null
					? StyleConstants.getFontSize(style)
					: currentFont.getSize();
		}

        fontAttributes.put(TextAttribute.FAMILY, family);
        fontAttributes.put(TextAttribute.SIZE, (float) size);
        
		Map<TextAttribute, ?> currentFontAttributes = currentFont.getAttributes();

		//
		// TODO:
		//		NOTE: the javadoc for StyleConstants.isUnderline does not match
		//		what the code does. Or it's confusing at best.
		//
		//		There is a problem with underline and strikethrough.
		//		There is no way to know if the attribute has been set.
		//		There is UNDERLINE_ON, but no UNDERLINE_OFF.
		//		Probably the fix is to introduce UNDERLINE_OFF
		//		and check for it as needed.
		//		BUT our parser sets the attribute to true/false so
		//		everthing is probably OK.
		//

		if (APPLY_DOES_DEFAULTS) {
			if (StyleConstants.isUnderline(style)) {
				fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
			}
		} else {
			if (style.getAttribute(StyleConstants.Underline) != null) {
				if (StyleConstants.isUnderline(style))
					fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
			} else {
				if (TextAttribute.UNDERLINE_ON.equals(
						currentFontAttributes.get(TextAttribute.UNDERLINE)))
					fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
			}
		}

		if (APPLY_DOES_DEFAULTS) {
			if (StyleConstants.isStrikeThrough(style)) {
				fontAttributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			}
		} else {
			if (style.getAttribute(StyleConstants.StrikeThrough) != null) {
				if (StyleConstants.isStrikeThrough(style)) {
					fontAttributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
				}
			} else {
				if (TextAttribute.STRIKETHROUGH_ON.equals(
						currentFontAttributes.get(TextAttribute.STRIKETHROUGH)))
					fontAttributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			}
		}

        int fontStyle = Font.PLAIN;
		if (APPLY_DOES_DEFAULTS) {
			if (StyleConstants.isBold(style)) fontStyle |= Font.BOLD;
			if (StyleConstants.isItalic(style)) fontStyle |= Font.ITALIC;
		} else {
			int currentFontStyle = currentFont.getStyle();
			fontStyle |= style.getAttribute(StyleConstants.Bold) != null
					? (StyleConstants.isBold(style) ? Font.BOLD : 0)
					: (currentFontStyle & Font.BOLD);
			fontStyle |= style.getAttribute(StyleConstants.Italic) != null
					? (StyleConstants.isItalic(style) ? Font.ITALIC : 0)
					: (currentFontStyle & Font.ITALIC);
		}

        Font baseFont = new Font(family, fontStyle, size);
        textComponent.setFont(baseFont.deriveFont(fontAttributes));
        textComponent.repaint();
    }

	/**
	 * For saving and restoring component state change by styles.
	 */
	public static class TextComponentStyleMemento {
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
		 * @param textComponent
         */
        public TextComponentStyleMemento(JTextComponent textComponent) {
            Objects.requireNonNull(textComponent);
			verifyEDT();
            // Capture Shared Core Layout Parameters
            this.foreground = textComponent.getForeground();
            this.background = textComponent.getBackground();
            this.font = textComponent.getFont();
            this.opaque = textComponent.isOpaque();

			// JTextField parameters
            if (textComponent instanceof JTextField textField) {
				this.alignment = textField.getHorizontalAlignment();
			}
			
			// Identify type and pull multi-line parameters if it's a JTextArea
            if (textComponent instanceof JTextArea textArea) {
                this.isTextArea = true;
                this.lineWrap = textArea.getLineWrap();
                this.wordWrap = textArea.getWrapStyleWord();
                JScrollPane jsp = findScrollPane(textArea);
                if (jsp != null) {
                    vsb =  jsp.getVerticalScrollBarPolicy();
                    hsb =  jsp.getHorizontalScrollBarPolicy();
                }
            } else {
                this.isTextArea = false;
            }
        }

        /**
         * Restore state snapshot;
         * Restores all core attributes and conditionally re-applies multi-line rules.
		 * @param textComponent
         */
        public void restoreTo(JTextComponent textComponent) {
			Objects.requireNonNull(textComponent);
			verifyEDT();
            // Restore Shared Layout States
            textComponent.setForeground(this.foreground);
            textComponent.setBackground(this.background);
            textComponent.setFont(this.font);
            textComponent.setOpaque(this.opaque);

            // Safely re-apply specific wrap layers only if targets match up

            if (!this.isTextArea && textComponent instanceof JTextField textField) {
				textField.setHorizontalAlignment(this.alignment);
			}

            if (this.isTextArea && textComponent instanceof JTextArea textArea) {
                //JTextArea textArea = (JTextArea) textComponent;
                textArea.setLineWrap(this.lineWrap);
                textArea.setWrapStyleWord(this.wordWrap);
                JScrollPane jsp = findScrollPane(textArea);
                if (jsp != null) {
                    jsp.setVerticalScrollBarPolicy(vsb);
                    jsp.setHorizontalScrollBarPolicy(hsb);
                }
            }
            
            textComponent.repaint();
        }

		/** {@inheritDoc} */
		@Override
		public String toString()
		{
			return "TextComponentStyleMemento{" + "foreground=" + foreground + ", background=" + background + ", font=" + font + ", opaque=" + opaque + ", alignment=" + alignment + ", isTextArea=" + isTextArea + ", lineWrap=" + lineWrap + ", wordWrap=" + wordWrap + ", vsb=" + vsb + ", hsb=" + hsb + '}';
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
			final TextComponentStyleMemento other = (TextComponentStyleMemento) obj;
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