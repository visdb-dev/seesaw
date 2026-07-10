/*******************************************************************************
 * Copyright (C) 2003-2021, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
 * All rights reserved.
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
 * 
 * Contributors:
 *   Prasanth R. Pasala
 *   Brian E. Pangburn
 *   Diego Gil
 *   Man "Bee" Vo
 *   Ernie R. Rael
 ******************************************************************************/
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2025-2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package com.nqadmin.swingset.models;

import java.lang.System.Logger;
import java.sql.JDBCType;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

import com.nqadmin.swingset.models.AbstractComboBoxListSwingModel.ListItem0;
import com.nqadmin.swingset.utils.JStuff;

import static com.nqadmin.swingset.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * Use this to produce a string representation of an SSListItem.
 * Configure the order in which the list item elements are formatted,
 * each element type, and optionally Format,
 * with {@link #addElemType(int, java.sql.JDBCType, java.text.Format) }.
 * After this object is created, by default
 * element 0 is formatted with toString(). Start with {@link #clear()}
 * to set up a different formatting specification.
 * <p>
 * Each JDBCType can have a default Format specified; use
 * {@link #setFormat(java.sql.JDBCType, java.text.Format) }
 * to set a Format for a type.
 * There are preset defaults for the date/time types
 * {@snippet :
 * // @link substring="SimpleDateFormat" target="java.text.SimpleDateFormat" type="link" :
 * These builtin defaults use SimpleDateFormat; the initial patterns are:
 *    JDBCType.DATE        "yyyy-MM-dd"              ISO_LOCAL_DATE
 *    JDBCType.TIME        "HH:mm:ss"                ISO_LOCAL_TIME
 *    JDBCType.TIMESTAMP   "yyyy-MM-dd'T'HH:mm:ss"   ISO_LOCAL_DATE_TIME
 * }
 * When an elem is formatted, first a format assigned to the elem is checked,
 * then the default format for the elem type is checked,
 * if neither is available, then toString() is used to format the elem.
 * the default time formats use .
 * <p>
 * Use {@link #format(java.lang.Object)}, where the argument
 * is an SSListItem, to get the String representation. Note
 * that if format's argument is not an SSListItem, then an
 * empty String is produced.
 * <p>
 * This is compatible with GlazedLists AutoCompleteSupport.
 * 
 * @since 4.0.0
 */
// TODO: Change to use java.time formatters.
// TODO: API for specifying system wide defaults? Lookup?
// TODO: interoperability with SSFormat
@SuppressWarnings("serial")
public class SSListItemFormat extends Format {
	//TODO: Put default formats into a common location (not in SSListItemFormat).
	/** default date format */
	public static final String DATE_DEFAULT = "yyyy-MM-dd";
	/** default time format */
	public static final String TIME_DEFAULT = "HH:mm:ss";
	/** default timestamp format */
	public static final String TIMESTAMP_DEFAULT = "yyyy-MM-dd'T'HH:mm:ss";
	/** default elem separator */
	public static final String DEFAULT_SEPARATOR = " | ";
	private static final FieldPosition FP0 = new FieldPosition(0);

	private String separator = DEFAULT_SEPARATOR;
	/** elemInfos.get(elemIndex) == elemInfo. */
	protected List<ElemInfo> elemInfos = new ArrayList<>(4);
	/** format these elem in order of List. */
	protected List<Integer> itemElemIndexes = new ArrayList<>(4);

	// allow customization of date/time formats
	private final EnumMap<JDBCType, Format> formats = new EnumMap<>(JDBCType.class);

	private static final Logger logger = JStuff.getLogger();

	/**
	 * Encapsulate info about element in SSListInfo.
	 */
	protected static class ElemInfo {
		/** type of the elem */
		final JDBCType type;
		/** Format to use with this elem, may be null */
		final Format format;

		/**
		 * Type and format for an SSListItem.
		 * @param type type
		 * @param format format
		 */
		protected ElemInfo(JDBCType type, Format format) {
			this.type = type;
			this.format = format;
		}
	}

	
	/**
	 * Create a Format. Use {@code addElemType} to specify
	 * elements, in order, that are formatted.
	 * By default, element 0 is formatted with toString()
	 */
	@SuppressWarnings("OverridableMethodCallInConstructor")
	public SSListItemFormat() {
		// format elment 0 with toString()
		addElemType(0, JDBCType.NULL);

		// initialize default format patterns
		formats.put(JDBCType.DATE,      new SimpleDateFormat(DATE_DEFAULT));
		formats.put(JDBCType.TIME,      new SimpleDateFormat(TIME_DEFAULT));
		formats.put(JDBCType.TIMESTAMP, new SimpleDateFormat(TIMESTAMP_DEFAULT));
	}

	/**
	 * Clear list item element information in preparation
	 * to establish elements to format.
	 * Note that default formatting patterns are not restored.
	 */
	public void clear() {
		elemInfos.clear();
		itemElemIndexes.clear();
	}

	/**
	 * Add element for formatting.Elements are formatted in
	 * the same order as they are added. If the same elemIndex
 	 * is added, the previous information is discarded.
	 * 
	 * @param elemIndex ListItem elemIndex for formatting
	 * @param jdbcType type of element
	 * @param format format to use for the element, may be null
	 */
	public void addElemType(int elemIndex, JDBCType jdbcType, Format format) {
		Objects.requireNonNull(jdbcType);
		// first make sure there's room
		while (elemIndex >= elemInfos.size()) {
			elemInfos.add(null);
		}
		elemInfos.set(elemIndex, new ElemInfo(jdbcType, format));

		// SSListItem is formatted in the order the items are added
		Integer indexAsObject = elemIndex;
		itemElemIndexes.remove(indexAsObject);
		itemElemIndexes.add(indexAsObject);
	}

	/**
	 * Add element for formatting.Elements are formatted in
	 * the same order as they are added. If the same elemIndex
 	 * is added, the previous information is discarded.
	 * The default Format for this type is used.
	 * 
	 * @param elemIndex ListItem elemIndex for formatting
	 * @param jdbcType type of element
	 */
	public void addElemType(int elemIndex, JDBCType jdbcType) {
		addElemType(elemIndex, jdbcType, null);
	}

	/**
	 * Set the default Format for the specified jdbc type.
	 * Only the {@link Format#format(Object, StringBuffer, java.text.FieldPosition)}
	 * method is used with the argument Format.
	 * @param jdbcType all elements of this type use the specified format
	 * @param format the format
	 * @return the previous format
	 */
	public Format setFormat(JDBCType jdbcType, Format format) {
		return formats.put(jdbcType, format);
	}

	/**
	 * Get the default Format for the specified JDBCType.
	 * @param jdbcType format for this
	 * @return format or null if no format has been set
	 */
	public Format getFormat(JDBCType jdbcType) {
		return formats.get(jdbcType);
	}

	/**
	 * The separator is goes between elements in a formatted string.
	 * @param separator the separator
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * @return the separator
	 */
	public String getSeparator() {
		return separator;
	}
	
	/**
	 * This implementation does not create Object from String.
	 * @param source text
	 * @param pos pos
	 * @return the original string
	 */
	@Override
	public Object parseObject(String source, ParsePosition pos) {
		// Do not create objects from here
		return source;
	}
	
	/**
	 * Note that pos is ignored.
	 * @param _listItem item being formatted
	 * @param toAppendTo StringBuffer being worked on
	 * @param pos pos
	 * @return StringBuffer being worked on
	 */
	@Override
	public StringBuffer format(Object _listItem, StringBuffer toAppendTo, FieldPosition pos) {
		if (_listItem != null && _listItem instanceof ListItem0) {
			// GlazedLists guarantees only format(Object), so ignore pos.
			ListItem0 listItem = (ListItem0)_listItem;
			for (int i = 0; i < itemElemIndexes.size(); i++) {
				// if this isn't the first element, add the separator
				if (i != 0) {
					toAppendTo.append(separator);
				}
				int elemIndex = itemElemIndexes.get(i);
				appendValue(toAppendTo, elemIndex, listItem);
			}
		}
		return toAppendTo;
	}

	/**
	 * This method allows overriding classes to access the list item
	 * elements directly without going through remodel.
	 *
	 * @param elemIndex index of element
	 * @param listItem container holding the element
	 * @return the element
	 */
	protected Object getElem(int elemIndex, SSListItem listItem) {
		return ((ListItem0) listItem).getElem(elemIndex);
	}

	/**
	 * Format the indicated element, by default use toString().
	 * @param sb append string value to this
	 * @param elemIndex index of element
	 * @param listItem container holding the element
	 */
	protected void appendValue(StringBuffer sb, int elemIndex, SSListItem listItem) {
		Object elem = getElem(elemIndex, listItem);
		if (elem == null) {
			return;
		}
		
		ElemInfo elemInfo = elemInfos.get(elemIndex);
		JDBCType jdbcType = elemInfo.type;
		Format format = elemInfo.format;
		if (format == null) {
			format = formats.get(jdbcType);
		}
		if (format != null) {
			try {
				format.format(elem, sb, FP0);
				return;
			} catch (Exception ex) {
				logger.log(ERROR, sf("can't format %s with %s. Exception: %s",
						elem.toString(), format.toString(), ex.getMessage()));
			}
		}
		// No formatter, or formatter got an exception
		sb.append(elem.toString());
	}
	
}
