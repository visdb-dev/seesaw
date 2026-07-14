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
 * copyright (C) 2024-2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package com.nqadmin.swingset.core;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.nqadmin.swingset.models.SSListItem;
import com.nqadmin.swingset.utils.JStuff;
import com.nqadmin.swingset.utils.SSUtils;

import static com.nqadmin.swingset.datasources.ConvertType.convertToType;
import static com.nqadmin.swingset.datasources.RowSetOps.getJDBCColumnType;
import static com.nqadmin.swingset.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * Similar to the ComboBox2, but used when both
 * {@code <K>}, the 'bound' value, and {@code <D>}, the
 * 'display' value, are pulled from a database table.
 * {@code <K>} is 
 * the 'key' and the display value the 'text' which appears in the combo box.
 * <p>
 * This is sometimes used as a Navigator in conjunction with
 * {@link com.nqadmin.swingset.utils.SSSyncManager}.
 * Generally the key represents a foreign key to another
 * table, and the combobox displays the {@code <D>}.
 * <p>
 * Optional data for combobox's item, {@code <D2>}, may be specified,
 * see {@link #setD2ColumnName(String) }. This data may be displayed by the
 * SSListItem, see {@link #setD2DisplayEnabled(boolean)} and {@link getListItemFormat()}
 * <p>
 * <b>Refer to {@link ComboBox2} for warnings and caveats.</b>
 * <p>
 * <a id="builders-and-generics"></a>
 * <h2> Builders and generic type parameter capture</h2>
 * <p>
 * The ComboBox hierarchy depends on generic parameter type capture. It is used
 * so that the data read from a database column is converted to the concrete
 * type specified by the parameter type.
 * <p>
 * When you have a DBComboBox2 with param types that you frequently use, it is
 * convenient to incorporate it into a re-usable class.
 * Here's a simple example where you lock in the types, don't add anything
 * else, MyDbComboBox.Builder just works.
 * {@snippet class=ComboBoxSnippets region=MyDbComboBox}
 * <p>
 * This next example, DbComboBox2Extra, does a lot.
 * <ol>
 * <li> lock in {@code <K>} and {@code <D>} types
 * <li> leaves {@code <D2>} for programmer
 * <li> adds new generic {@code <D3>} for programmer
 * <li> captures the type of {@code <D3>}
 * <li> has an AbstractBuilder so the class and Builder are extendable
 * <li> has a concrete Builder to instantiate the class
 * </ol>
 * {@snippet class=ComboBoxSnippets region=ExtendableDbComboBox}
 * And a usage example
 * {@snippet class=ComboBoxSnippets region=ExtendableDbComboBoxExample}
 * With output {@snippet :
 *     class java.lang.Integer
 *     class java.lang.String 
 *     class java.lang.Double
 *     java.util.List<java.lang.Double>
 *     [7.0, 6.0, 5.0]
 * }
 * <p>
 * <h2>Example with two tables</h2>
 * <ol>
 * <li>part_data (part_id, part_name, ...)
 * <li>shipment_data (shipment_id, part_id, quantity, ...)
 * </ol>
 * <p>
 * Assume you would like to develop a screen for the shipment_data table
 * including a combobox where the user can choose a part
 * and a textbox where the user can specify a quantity.
 * <p>
 * In the combobox you would want to display the part name rather than part_id
 * so that it is easier for the user to choose. At the same time you want to
 * store the id of the part chosen by the user in the shipment table.
 * 
 * {@snippet class=ComboBoxSnippets region=init}
 * 
 * @param <K> list item key type
 * @param <D> list item displayValue type
 * @param <D2> list item optional extra data field type
 */
@SuppressWarnings("serial")
public class DBComboBox2<K,D,D2> extends ComboBox2<K,D,D2>
{
	/** Logger for component */
	private static final Logger logger = JStuff.getLogger();

	/**
	 * The column name used to query the values for the bound column keys.
	 * This is generally the PK of the table to which a foreign key is mapped.
	 * NOTE: This is NOT the bound column. It is the source of the keys.
	 */
	private String primaryKeyColumnName;

	/** The database column that populates the displayValue of combo list item. */
	private String displayColumnName;

	/** The database column that populates the (optional) data of combo list item. */
	private String d2ColumnName;

	/** Query used to populate combo box. */
	private String query;

	/** database connection to populate combobox, optional. */
	private Connection connection;

	/** Format for any date columns displayed in combo box. */
	// TODO: Use a SSFormat.
	private String dateFormat = DEFAULT_DATE_FORMAT;

	/** counter for # times that execute() method is called - for testing. */
	protected int executeCount = 0;

	// TODO: configuration option
	/** default model type */
	public static final ModelType DEFAULT_MODEL_DB_COMBO2 = ModelType.GLAZED;
	private static final String DEFAULT_DATE_FORMAT = "MM/dd/yyyy";

	/**
	 * To build a DBComboBox2 with the specified parameters.
	 * @param <K>
	 * @param <D>
	 * @param <D2> 
	 * @param <T> 
	 */
	public abstract static class AbstractBuilder<K, D, D2, T extends AbstractBuilder<K, D, D2, T>>
			extends ComboBox2.AbstractBuilder<K, D, D2, T>
	{
		// all parameters are optional, at least for now
		private Connection connection;
		private String query;
		private String primaryKeyColumnName;
		private String displayColumnName;
		private String d2ColumnName;
		private String dateFormat = DEFAULT_DATE_FORMAT;

		/**
		 * AbstractBuilder
		 */
		public AbstractBuilder() {
			// DBComboBox2 has a different default than ComboBox2, so set that up.
			super.modelType(DEFAULT_MODEL_DB_COMBO2);
		}

		/**
		 * Database connection to use to populate the combobox.
		 * If not set, use shared connection from lookup.
		 * @param val
		 * @return 
		 */
		public T connection(Connection val) {
			connection = val;
			return self();
		}
		/**
		 * query used to retrieve the values to display in
		 * the combo from the database. 
		 * @param val
		 * @return 
		 */
		public T query(String val) {
			checkString(val, "query");
			query = val;
			return self();
		}
		/**
		 * column name from the query to populate the {@code Key}
		 * field of the combobox list item.
		 * @param val
		 * @return 
		 */
		public T primaryKeyColumnName(String val) {
			checkString(val, "primaryKeyColumnName");
			primaryKeyColumnName = val;
			return self();
		}
		/**
		 * column name from the query to populate the {@code displayValue}
		 * field of the combobox list item.
		 * @param val
		 * @return 
		 */
		public T displayColumnName(String val) {
			checkString(val, "displayColumnName");
			displayColumnName = val;
			return self();
		}
		/**
		 * Sets the column name from the query to populate the
		 * {@code <D2>} field of the combobox list item.
		 * If null, the default, or blank then there is no {@code <D2>}
		 * Use this if there's extra data to store in the
		 * combobox list item.
		 * @param val
		 * @return 
		 */
		public T d2ColumnName(String val) {
			d2ColumnName = val;
			return self();
		}
		/**
		 * 
		 * When a displayed column is of type date use this format to display it.
		 * For the pattern refer SimpleDateFormat in java.text package.
		 * @param val
		 * @return 
		 */
		public T dateFormat(String val) {
			dateFormat = val;
			return self();
		}
	}

	/** Builder.
	 * @param <K>
	 * @param <D>
	 * @param <D2> */
	public static class Builder<K, D, D2> extends AbstractBuilder<K, D, D2, Builder<K, D, D2>> {

		/** self type idiom */
		@Override
		protected Builder<K, D, D2> self() { return this; }

		/** Create DBComboBox2 */
		@Override
		public DBComboBox2<K, D, D2> build() { return new DBComboBox2<>(this); }

	}

	private static void checkString(String val, String tag) {
		if (val == null || val.isBlank())
			throw new IllegalArgumentException(tag + " must not be null or blank");
	}

	/**
	 *
	 * @param builder
	 */
	protected DBComboBox2(AbstractBuilder<K, D, D2, ?> builder) {
		super(builder);
		// TODO: error checking: query/primaryK, displayC all must be set.
		if (builder.primaryKeyColumnName == null || builder.primaryKeyColumnName.isBlank()
				|| builder.displayColumnName == null || builder.displayColumnName.isBlank())
			throw new IllegalArgumentException(
					"Specify both primaryKeyColumnName and displayColumnName.");


		connection = builder.connection;
		query = builder.query;
		primaryKeyColumnName = builder.primaryKeyColumnName;
		displayColumnName = builder.displayColumnName;

		d2ColumnName = builder.d2ColumnName;
		keyVisual.setD2Enabled(hasD2());

		dateFormat = builder.dateFormat;
		getListItemFormat().setFormat(JDBCType.DATE, new SimpleDateFormat(dateFormat));
	}

	/**
	 * Create a DBComboBox2.
	 */
	public DBComboBox2() {
		this(new Builder<>());

	}

	/**
	 * Executes the query specified with setQuery(), populates combobox,
	 * and turns on AutoCompleteSupport.
	 * 
	 * @throws Exception may occur querying data or turning on AutoComplete
	 */
	// See https://stackoverflow.com/questions/15210771/autocomplete-with-glazedlists
	// for info on modifying lists.
	// TODO: What's the deal with the Exception?
	public void execute() throws Exception {

		logger.log(DEBUG, () -> sf("%s setting execute count: %d",
				getColumnForLog(), executeCount++));
		// (re)query data
		queryData();


		// since the list was likely blank when the component was bound we need to update
		// the component again so it can get the text from the list we don't want to do
		// this if the component is unbound as with an DBComboBox used for navigation.
		// TODO: rework combo navigation somehow; maybe there's a navigator "thing".
		if (getRowSet() != null) {
			// 	call using getSSCommon()
			SSUtils.updateSSComponent_HACK(this);
		}
	}

	/**
	 * Populates the list model with the data by fetching it from the database.
	 */
	private void queryData() {
		logger.log(DEBUG, () -> sf("%s Query [%s].", getColumnForLog(), getQuery()));

		// this.data.getReadWriteLock().writeLock().lock();
		try (Model.Remodel remodel = keyVisual.getRemodel()) {
			logger.log(TRACE, () -> sf("%s Clearing eventList.", getColumnForLog()));
			remodel.clear();
			nullItem = null;

			logger.log(DEBUG, () -> sf("%s Nulls allowed? %s.",
									   getColumnForLog(), getAllowNull()));
			adjustForNullItem();

			Statement statement = getConnection().createStatement();
			try (ResultSet rs = statement.executeQuery(getQuery());) {
				
				List<SSListItem> newItems = new ArrayList<>();
				while (rs.next()) {
					// TODO: multikey
					// NOTE: direct ResultSet access.
					// TODO: Can't use RowSetOps.getColumnObject(comp, class)
					//       because RSC take a RowSet (not a ResultSet),
					//       maybe more so because there's the undo/redo stuff.
					K pk = convertToType(rs.getObject(getPrimaryKeyColumnName()), getKeyType());
					D opt = convertToType(rs.getObject(displayColumnName), getDisplayValueType());
					logger.log(TRACE, () -> sf("%s pk: %s, opt: %s",
							pk, getColumnForLog(), opt));
					D2 opt2 = hasD2() ? convertToType(rs.getObject(d2ColumnName), getD2Type())
							: null;
					logger.log(TRACE, () -> sf("%s opt2: %s", getColumnForLog(), opt2));
					
					newItems.add(remodel.createKeyDisplayValueItem(pk, opt, opt2));
				}
				remodel.addAll(newItems);

				// Configure the listItemFormat with this queries column types
				establishListItemFormat(
						getJDBCColumnType(rs, rs.findColumn(displayColumnName)),
						hasD2() ? getJDBCColumnType(rs, rs.findColumn(d2ColumnName)) : null);
			}
		} catch (final SQLException se) {
			logger.log(Level.ERROR, getColumnForLog() + ": SQL Exception.", se);
		} catch (final java.lang.NullPointerException npe) {
			// TODO: why is NullPointerException here?
			logger.log(Level.ERROR, getColumnForLog() + ": Null Pointer Exception.", npe);
		}
	}

	/**
	 * Returns the Connection to the database to populate combobox.
	 *
	 * @return the connection
	 */
	private Connection getConnection() throws SQLException {
		return connection == null
				? SSUtils.dbSupport().getSharedConnection(null) : connection;
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	protected boolean isComboBoxNavigator() {
		return getColumnName() == null;
	}

	/**
	 * Retrieves the database column (normally a primary key) from which
	 * to query the keys for the bound column.
	 *
	 * @return name of the PK value to query for the bound column keys
	 */
	public String getPrimaryKeyColumnName() {
		return primaryKeyColumnName;
	}

	/**
	 * Returns the pattern in which dates are displayed.
	 *
	 * @return
	 */
	public String getDateFormat() {
		return dateFormat;
	}

	/**
	 * When a display column is of type date you can choose the format in which it
	 * has to be displayed. For the pattern refer SimpleDateFormat in java.text package.
	 *
	 * @param dateFormat pattern in which dates have to be displayed
	 */
	public void setDateFormat(final String dateFormat) {
		this.dateFormat = dateFormat;
		getListItemFormat().setFormat(JDBCType.DATE, new SimpleDateFormat(dateFormat));
	}

	/**
	 * Returns the column name whose values are displayed in the combo box.
	 *
	 * @return returns the name of the column used to get values for combo box
	 *         items.
	 */
	public String getDisplayColumnName() {
		return displayColumnName;
	}

	// NOTE: IF A LIST OF THE STRINGS IN COMBOBOX IS WANTED,
	//		 THEN THE FOLLOWING CAN BE USED.

	// List<String> displayValues = new ArrayList<>();
	// try (Model.Remodel remodel = comboInfo.getRemodel()) {
	// 	List<SSListItem> items = remodel.getEventList();
	// 	for(SSListItem item : items) {
	// 		displayValues.add(getListItemFormat.format(item));
	// 	}
	// }
	// return displayValues;

	/**
	 * Returns the rowSet column name used to populate the {@code <D2>} field of
	 * the combobox list item.
	 *
	 * @return returns the column name {@code <D2>} values
	 */
	public String getD2ColumnName() {
		return d2ColumnName;
	}

	/**
	 * Sets the column name . If there's extra data to store in the 
	 * combobox list item then use this.
	 *
	 * @param d2ColumnName column name whose values populate {@code <D2>}.
	 */
	public void setD2ColumnName(final String d2ColumnName) {
		this.d2ColumnName = d2ColumnName;
		keyVisual.setD2Enabled(hasD2());
	}

	/**
	 * Returns the text displayed in the combobox.
	 *
	 * @return value corresponding to the selected item in the combo. return null if
	 *         no item is selected.
	 */
	public String getSelectedStringValue() {
		Object currentItem = getSelectedItem();
		return currentItem != null ? getListItemFormat().format(currentItem) : null;
	}

	/**
	 * 
	 * @return
	 */
	@Override
	public final boolean hasD2() {
		return d2ColumnName != null && !d2ColumnName.isEmpty();
	}

	/**
	 * {@inheritDoc }
	 * @throws IllegalStateException if d2 enabled
	 */
	// TODO: Needed? Remove.
	@Override
	public void setChosenDisplayValue(D displayValue) {
		if (hasD2()) {
			throw new IllegalStateException("d2 enabled");
		}
		super.setChosenDisplayValue(displayValue);
	}

	/**
	 * Returns the query used to retrieve values from database for the combo box.
	 *
	 * @return returns the query used.
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Sets the query used to display items in the combo box.
	 *
	 * @param query query to be used to get values from database (to display combo
	 *               box items)
	 */
	// TODO: Should this method call execute?
	public void setQuery(final String query) {
		checkString("query", query);
		this.query = query;
	}

	/** {@inheritDoc } */
	// TODO: Needed? Remove.
	@Override
	public boolean updateDisplayValue(K key, D displayValue)
	{
		boolean result = false;
		try {
			result = super.updateDisplayValue(key, displayValue);
		} catch (final Exception e) {
			logger.log(Level.ERROR, getColumnForLog() + ": Exception.", e);
		}
		if (!result) {
			logger.log(WARNING, () -> sf("%s: Unable to update Keys of [%s] with DisplayValue of '%s'.",
				getColumnForLog(), key, displayValue));
		}

		return result;
	}

	/**
	 * Unconditionally throws UnsupportedOperationException.
	 * @param displayValues
	 * @param keys 
	 */
	@Override
	public void setDisplayValues(List<D> displayValues, List<K> keys)
	{
		throw new UnsupportedOperationException("DBComboBox doesn't support");
	}

	/**
	 * Unconditionally throws UnsupportedOperationException.
	 * @param displayValues 
	 */
	@Override
	public void setDisplayValues(List<D> displayValues)
	{
		throw new UnsupportedOperationException("DBComboBox doesn't support");
	}

	/**
	 * Unconditionally throws UnsupportedOperationException.
	 * @param <T>
	 * @param enumDisplayValues 
	 */
	@Override
	public <T extends Enum<T>> void setDisplayValues(Class<T> enumDisplayValues) {
		throw new UnsupportedOperationException("DBComboBox doesn't support");
	}

	/**
	 * After this, make some adjustments.
	 * {@inheritDoc }
	 * 
	 * Deprecated in SSComponentInterface.
	 * @deprecated Use bind()
	 */
	@Override
	@Deprecated
	public void setBoundColumnName(String boundColumnName) {
		super.setBoundColumnName(boundColumnName);
		adjustForNullItem();
	}

} // end public class DBComboBox
