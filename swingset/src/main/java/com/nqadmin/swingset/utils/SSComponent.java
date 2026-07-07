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
package com.nqadmin.swingset.utils;

import java.awt.Component;
import java.sql.Array;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.EventListener;
import java.util.function.Supplier;

import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.swing.JComponent;

import com.nqadmin.swingset.core.CheckBox;
import com.nqadmin.swingset.datasources.RSC;
import com.nqadmin.swingset.datasources.RowSetOps;
import com.nqadmin.swingset.datasources.DbSupport.DbReader;
import com.nqadmin.swingset.datasources.DbSupport.DbUpdater;
import com.nqadmin.swingset.datasources.DbSupport.RunnableSQL;
import com.nqadmin.swingset.decorators.Decorator;
import com.nqadmin.swingset.decorators.TextDecorator;
import com.nqadmin.swingset.decorators.Validator;
import com.nqadmin.swingset.formatting.SSFormat;
import com.nqadmin.swingset.formatting.SSFormattedTextField;
import com.nqadmin.swingset.navigate.ColumnChangeStartEvent;
import com.nqadmin.swingset.navigate.RowsModel;
import com.nqadmin.swingset.navigate.UndoRedo;
import com.nqadmin.swingset.navigate.UndoRedo.Change;

import static com.nqadmin.swingset.utils.SSUtils.findRowsModel;

/**
 * This Interface presents a {@link RowSet} column as seen by the visual
 * components in the SS library. It has default methods supporting binding
 * a {@linkplain RowSet} column to this component, DBMS access with type conversion,
 * undo/redo, validation, decoration. Most of these default methods bounce
 * to an internal SSCommon class and would be declared final if java
 * supported that. A database column is associated
 * with this component via {@link RowsModel#bind(SSComponent,String) }.
 * There are only two methods, and the Hook, that need to be implemented
 * by the visual component: {@link #cleanField() }, {@link #getSSComponentHook() }.
 * See {@link Hook} for a complete example.
 * <p>
 * The basic components for application use and/or subclassing are found in
 * <a href="../core/package-summary.html">core components</a>.
 * They are subclassed into a
 * <a href="../package-summary.html">compatibility layer</a>
 * as a replacement for the
 * <a href="https://github.com/bpangburn/swingset">original SwingSet library</a>.
 * <p>
 * There are several methods that are typically overridden; they
 * perform customization, receive notification, and do verification:
 * {@link #customInit() },
 * {@link #checkColumnType(JDBCType) }, {@link #metadataChange() },
 * {@link #finishBind() }, {@link #baseValidate() }, {@link #componentValidate()},
 * and {@link #createDefaultDecorator()}.
 * <p>
 * The methods for reading and updating database columns are
 * {@link #getColumnText() getColumn*()} and {@link #setColumnText(String) setColumn*}.
 * With {@link #getColumnText() }, {@link #setColumnText(String) }
 * and {@link #getColumnObject(Class) } values are converted as needed,
 * see {@link com.nqadmin.swingset.datasources.ConvertType}.
 * For unhandled {@code JDBCType}s, {@link #getColumn() }
 * and {@link #setColumn(Object) } are available;
 * see {@link #setColumnReader(DbReader)}
 * and {@link #setColumnUpdater(DbUpdater) }.
 * <p>
 * There are methods about column state, status and metadata;
 * like {@link #getAllowNull() [sg]etAllowNull()}, {@link #isDirty() },
 * {@link #getColumnJDBCType() }.
 * <p>
 * There are multiple levels of validation. 
 * The validators are executed in the
 * following order, see {@link allValidate};
 * if there's an error, validation stops, and see {@link ValidationResult}.
 * <ol>
 * <li>{@code baseValidate()} - method, defaults true<br>
 * This checks that the SSComponent's value is more or less correct.
 * For example a mask formatter's valid indicator,
 * {@link javax.swing.JFormattedTextField#isEditValid};
 * or a {@link com.nqadmin.swingset.core.TextField} subclass could check that there's only characters.
 * <li>{@code componentValidate()} - method default true<br>
 * A subclass of something that does baseValidate, can use this for more
 * specific validation.
 * <li>optionally check rowsModel.hasError(SSComponent)
 * <li>{@code pluginValidate} - Application specific validation, per component instance.
 * <br>
 * Set at run time with {@link #setPluginValidator(Validator)}.
 * For example check for specific values; or other columns or ...
 * Can apply constraints while editing and avoid errors at commit.
 * </ol>
 * <p>
 * There are {@link Decorator}s that visually indicate the components state.
 * <p>
 * When creating an SSComponent, typical usage: {@snippet lang="java":
 *     class MyComponent extends SomeJComponent implements SScomponent {
 *         MyComponent() {
 *             // ...
 *             // finishSSCommon() MUST BE CALLED AT END OF CONSTRUCTOR
 *             finishSSCommon(); // @link substring="finishSSCommon" target="SSComponent#finishSSCommon"
 *         }
 *     }
 * }
 */
public interface SSComponent extends RSC
{
	/** Initialize component to an empty or default value.
	 * Action could be conditioned on getAllowNull() or whatever.
	 */
	void cleanField();

	/**
	 * Return the {@linkplain Hook} used by SSCommon.
	 * An SSComponent must create a Hook.
	 * See {@link Hook} for a complete example.
	 * <p>
	 * <b>Generally should not be used except by SSCommon</b>.
	 * @return Hook
	 */
	Hook getSSComponentHook();

	
	/**
	 * An SSComponent must create a Hook. The hook is used by the SS library
	 * to tell the component to read and display the current database value.
	 * Here's a complete example based on {@link CheckBox}.
	 * {@snippet lang="java" class=MyCheckBox region=hook_example}
	 * <p>
	 * <b>Generally should not be used except by SSCommon</b>.
	 */
	abstract class Hook
	{
		private final SSComponent ssComponent;

		/**
		 * Create.
		 * @param ssComponent
		 */
		protected Hook(SSComponent ssComponent)
		{
			this.ssComponent = ssComponent;
		}

		/**
		 * Updates the value stored and displayed in the SwingSet component
		 * based on value obtained from the database with getColumn*().
		 * <p>
		 * Before this method, invoked by SSCommon, is called the component's
		 * listener is removed; it is restored when the call returns.
		 * The idea is to prevent event loops.
		 */
		protected abstract void updateSSComponent();

		/**
		 * Return the listener used by this SSComponent to detect changes
		 * in value for the current component.
		 * <p>
		 * A component generally has exactly one listener for
		 * detecting JComponent changes related to a database column, so this method
		 * will only be called one time in the SSCommon constructor to obtain
		 * that listener.
		 * <p>
		 * Usually the developer returns an instance of an inner class
		 * that implements the appropriate listener for the superclass
		 * JComponent, for example ItemListener for an SSComponent class
		 * extending JCheckBox, ChangeListener for a class extending JSlider.
		 * <p>
		 * If the component is JTextComponent then the implementation can return
		 * an instance of {@link SSTextSupport#getSSDocumentListener(JTextComponent) }
		 * which helps with per keystroke component decoration.
		 * For example: {@snippet :
		 *     protected SSDocumentListener getSSComponentListener() {
		 *     	   return SSTextSupport.getSSDocumentListener(TextField.this);
		 *     }
		 * }
		 * But typically, just subclass {@link com.nqadmin.swingset.core.TextField}
		 * and it's taken care of.
		 * 
		 * @return event listener that triggers database column update
		 */
		protected abstract EventListener getSSComponentListener();

		/**
		 * Method to add SSComponent's listener.
		 * 
		 * @param eventListener 
		 */
		protected abstract void addSSComponentListener(EventListener eventListener);

		/**
		 * Method to remove SSComponent's listener.
		 * 
		 * @param eventListener
		 */
		protected abstract void removeSSComponentListener(EventListener eventListener);

		private SSCommon ssCommon;
		/**
		 * Returns ssCommon for the current Swingset component.
		 *
		 * @return common SwingSet component data and methods
		 */
		final SSCommon getSSCommon() {
			if (ssCommon == null)
				ssCommon = SSCommon.createStart(ssComponent);
			return ssCommon;
		}

		/**
		 * Invoked by ssComponent.finishSSCommon.
		 */
		final void finishSSCommon() {
			// Make sure ssCommon is initialized before doing createFinish.
			// createFinish invokes the SSComponent to initialize some
			// things; and SSComponent may use getSSCommon. Don't want to
			// construct a 2nd SSCommon.
			getSSCommon();
			SSCommon ssCommon2 = SSCommon.createFinish(ssComponent, ssCommon);
			if (ssCommon != ssCommon2) {
				throw new IllegalStateException("Multiple SSCommon created");
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// The methods beyond this point have default implementations.
	//
	// The first group are commonly overriden.
	//
	// There are also some validation/decoration related methods near the end
	// of this file that are commonly overridden:
	//			baseValidate, componentValidate, createDefaultDecorator.
	//

	/**
	 * Method to allow Developer to add functionality when SwingSet component is
	 * instantiated.
	 * <p>
	 * It will actually be called from SSCommon.init() once the SSCommon data member
	 * is instantiated.
	 */
	default void customInit() {}

	/** Invoked during bind, component should verify that the database column's
	 * JDBCType is ok and handled by this component.
	 * 
	 * @param jdbcType column JDBCType
	 * @throws IllegalArgumentException if can't handle JDBCType
	 */
	default void checkColumnType(JDBCType jdbcType) throws IllegalArgumentException { }

	/**
	 * Override this method for notification of a change in metadata.
	 * Typically from setRowSet() or bind().
	 */
	default void metadataChange() { }

	/**
	 * Invoked by the infrastructure at the end of bind;
	 * default sets up primary keys for CachedRowSet.
	 * Invoke super if override.
	 */
	// TODO: currently called once per column, only needed once per RowSet?
	default void finishBind()
	{
		// Primary keys for SyncResolver, joins
		if (getRowSet() instanceof CachedRowSet)
			SSUtils.setupDefaultPrimaryKeys(this);
	}


	////////////////////////////////////////////////////////////////////////////
	//
	// The rest of these methods are convenience methods that bounce
	// to SSCommon and typically are not overridden.
	//

	/**
	 * Returns the SSCommon associated with this Swingset component.
	 * 
	 * <b>Generally only used by SSComponent and SSCommon. This method
	 * is exporting a non public class.</b>.
	 *
	 * @return common SwingSet component data and methods
	 */
	@SuppressWarnings("NonPublicExported")
	default SSCommon getSSCommon() {
		return getSSComponentHook().getSSCommon();
	}

	/**
	 * This should be invoked as the last statement
	 * in the SSComponent's constructor, but before bind.
	 */
	default void finishSSCommon() {
		getSSComponentHook().finishSSCommon();
	}
	
	/**
	 * Sets the RowsModel and column name to which the component is to be bound.
	 * <p>
	 * Takes care of setting RowSet and Column Name for ssCommon and then calls
	 * bind(this.ssCommon);
	 *
	 * @param rowsModel holds RowSet to be used.
	 * @param columnName Name of the column to which this check box should be bound
	 * 
	 * @deprecated Use {@link RowsModel#bind(Map) RowsModel.bind(...)}
	 */
	@Deprecated
	default void bind(RowsModel rowsModel, String columnName)
	{
		getSSCommon().bind(rowsModel, columnName);
	}

	/**
	 * Indicate whether or not the Component has been bound to a RowSet.
	 * A fully bound component has information based on RowSet's metadata,
	 * for example jdbc column type and isNullable.
	 * @return true if fullyBound
	 */
	default boolean isFullyBound()
	{
		return getSSCommon().isFullyBound();
	}

	/**
	 * Transition support.
	 * @param rowSet
	 * @param columnName
	 * @deprecated Use {@link RowsModel#bind(Map) RowsModel.bind(...)}
	 */
	@Deprecated
	default void bind(RowSet rowSet, String columnName)
	{
		bind(findRowsModel(rowSet), columnName);
	}

	/**
	 * Setup additional focus transfer keys.
	 * Typically invoke super if override.
	 */
	default void configureTraversalKeys()
	{
		SSCommon.configureTraversalKeys((JComponent)this);
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//
	// Read/Write column data
	//

	/**
	 * Returns a String representation of the value in the bound database column.
	 * <p>
	 * If null, it will return an empty string.
	 *
	 * @return the database value as String to display in the SSComponent, may be from undo/redo stack.
	 */
	@Override
	default String getColumnText() {
		return getSSCommon().getColumnText();
	}

	/**
	 * Returns the Object for the bound database column
	 * as returned by {@link RowSet#getObject(int) }.
	 * <p>
	 * Note a null is never converted into ""; use getColumnText for that.
	 * @return the value to display in the SSComponent, may be from undo/redo stack.
	 */
	// TODO: put this in RSC?
	default Object getColumnObject() {
		return getSSCommon().getColumnObject();
	}

	/** {@inheritDoc } */
	@Override
	default <T> T getColumnObject(Class<T> clazz) {
		return getSSCommon().getColumnObject(clazz);
	}

	/**
	 * Returns the Array for the bound database column
	 * as returned by {@link RowSet#getArray(int) }.
	 * <p>
	 * @return the value to display in the SSComponent, may be from undo/redo stack.
	 */
	default Array getColumnArray() {
		return getSSCommon().getColumnArray();
	}

	/**
	 * Sets the value of the bound database column using the SSComponent's
	 * {@link DbUpdater}. See {@link #getColumnUpdater() }.
	 * NPE if no columnReader. Useful for dealing with JDBCTypes not handled
	 * internally.
	 * 
	 * @return the value to display in the SSComponent, may be from undo/redo stack.
	 * @throws java.sql.SQLException
	 */
	default Object getColumn() throws SQLException {
		return getSSCommon().getColumn();
	}

	/**
	 * Get the columnReader used by {@link #getColumn()} and internally,
	 * when not null, for capturing initial value for undo/redo.
	 * This is useful for dealing with ColumnTypes that are are not
	 * handled internally, like BLOB and VARBINARY.
	 * For exampe, see {@link com.nqadmin.swingset.core.Image} source code.
	 * 
	 * The {@code columnReader} is typically invoked like
	 * {@code .apply(comp.getRowSet(), comp.getColumnIndex(), comp)}.
	 * The comp is rarely used, and provided for complex situations. The
	 * columnReader return the value fetched from the datbase.
	 * 
	 * @return the DbReader used to fetch values from the database
	 */
	default DbReader<RowSet, Integer, SSComponent> getColumnReader() {
		return getSSCommon().getColumnReader();
	}

	/**
	 * Set the columnReader used by {@link #getColumn()} and internally for capturing
	 * initial value. This is useful for dealing with ColumnTypes that are are not
	 * handled internally, like BLOB and VARBINARY.
	 * For exampe, see {@link com.nqadmin.swingset.core.Image} source code.
	 * 
	 * The {@code columnReader} is typically invoked like
	 * {@code .apply(comp.getRowSet(), comp.getColumnIndex(), comp)}.
	 * The comp is rarely used, and provided just in case.
	 * For example with a BLOB or BINARY column 
	 * {@snippet lang="java" class=DbReaderDbUpdater region=setColumnReader}
	 * @param columnReader the DbReader used to fetch values from the database
	 */
	default void setColumnReader(DbReader<RowSet, Integer, SSComponent> columnReader) {
		getSSCommon().setColumnReader(columnReader);
	}

	/**
	 * Used when making a change to the database.
	 * Typically used by a component listener. It avoids extra RowSet events.
	 * See {@link Hook} for example usage.
	 * May bring up a dialog if there is no row to change, which would
	 * usually indicate some kind of internal error.
	 * @param r code that changes the database
	 * @throws java.sql.SQLException
	 */
	default void dbChange(RunnableSQL r) throws SQLException
	{
			getSSCommon().dbChange(r);
	}

	/**
	 * Updates the value of the bound database column;
	 * method used by SwingSet component listeners to update the underlying RowSet.
	 * The real action, like null handling and conversion checking, happens
	 * in {@link RowSetOps#updateColumnText(com.nqadmin.swingset.utils.SSComponent, java.lang.String) }.
	 * Does not commit the update row.
	 *
	 * @param columnText the value to set in the bound database column
	 * @return true if no error
	 */
	default boolean setColumnText(final String columnText) {
		return getSSCommon().setColumnText(columnText);
	}

	/**
	 * Updates the value of the bound database column;
	 * method used by SwingSet component listeners to update the underlying RowSet.
	 * Does not commit the update row.
	 *
	 * @param columnObject the value to set in the bound database column
	 * @return true if no error
	 */
	default boolean setColumnObject(final Object columnObject) {
		return getSSCommon().setColumnObject(columnObject);
	}

	/**
	 * Updates the bound database column with the specified Array.
	 * <p>
	 * Used for SSList or other component where multiple items can be selected.
	 * See {@link com.nqadmin.swingset.core.List1} and
	 * {@link com.nqadmin.swingset.models.SSCollection} for low level
	 * details on how arrays are read and written.
	 * Does not commit the update row.
	 *
	 * @param columnArray Array to write to bound database column
	 * @return true if no error
	 * @throws SQLException thrown if there is a problem writing the array to the
	 *                      RowSet
	 */
	default boolean setColumnArray(final Array columnArray) throws SQLException {
		return getSSCommon().setColumnArray(columnArray);
	}

	/**
	 * Updates the value of the bound database column;
	 * method used by SwingSet component listeners to update the underlying RowSet.
	 * Sets the value of the bound database column using the SSComponent's
	 * {@link DbUpdater}. See {@link #getColumnUpdater() }.
	 * NPE if no columnUpdater. Useful for dealing with JDBCTypes not handled
	 * internally.
	 * Does not commit the update row.
	 * 
	 * @param value to write to the database, may write to the undo/redo stack.
	 * @return true if no error
	 */
	default boolean setColumn(Object value) {
		return getSSCommon().setColumn(value);
	}

	/**
	 * Get the columnUpdater used by {@link #setColumn(Object)}.
	 * This is useful for dealing with ColumnTypes that are are not
	 * handled internally, like BLOB and VARBINARY.
	 * For exampe, see {@link com.nqadmin.swingset.core.Image} source code.
	 * 
	 * The {@code columnUpdater} is typically invoked like
	 * {@code .apply(comp.getRowSet(), comp.getColumnIndex(), comp, value)}.
	 * The comp is rarely used, and provided for complex situations.
	 * 
	 * @return the DbUpdater used to update to the database
	 */
	default DbUpdater<RowSet, Integer, SSComponent, Object> getColumnUpdater() {
		return getSSCommon().getColumnUpdater();
	}

	/**
	 * Set the columnUpdater used by {@link #setColumn(Object)}.
	 * This is useful for dealing with ColumnTypes that are are not
	 * handled internally, like BLOB and VARBINARY.
	 * For exampe, see {@link com.nqadmin.swingset.core.Image} source code.
	 * 
	 * The {@code columnUpdater} is typically invoked like
	 * {@code .apply(comp.getRowSet(), comp.getColumnIndex(), comp, value)}.
	 * The comp is rarely used, and provided just in case.
	 * For example with a BLOB or BINARY column: 
	 * {@snippet lang="java" class=DbReaderDbUpdater region=setColumnUpdater}
	 * 
	 * @param columnUpdater the DbUpdater used to update the database
	 */
	default void setColumnUpdater(DbUpdater<RowSet,Integer,SSComponent,Object> columnUpdater) {
		getSSCommon().setColumnUpdater(columnUpdater);
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//
	// Column information, like index/name, some derived from metadata
	//

	/**
	 * Retrieves the allowNull flag for the bound database column.
	 * Initialized from RowSet metadata. May be overridden.
	 *
	 * @return true if bound database column can contain null values, otherwise
	 *         returns false
	 */
	@Override
	default boolean getAllowNull() {
		return getSSCommon().getAllowNull();
	}

	/**
	 * Sets the allowNull flag for the bound database column.
	 * This overrides the RowSet metadata.
	 * Set to null to go back to the database metadata.
	 *
	 * @param allowNull flag to indicate if the bound database column can be null
	 */
	default void setAllowNull(final boolean allowNull) {
		getSSCommon().setAllowNull(allowNull);
	}

	/**
	 * @return true if this component's value is different from what's in the database
	 */
	default boolean isDirty() {
		return getRowsModel() != null && getRowsModel().isDirty(this);
	}

	/**
	 * A component may have a display/parse format. Especially used in
	 * conjunction with, but not limited to, {@linkplain SSFormattedTextField}.
	 * @param format format for this component
	 */
	default void setSSFormat(SSFormat format) { getSSCommon().setSSFormat(format); }

	/**
	 * {@inheritDoc }
	 */
	@Override
	default SSFormat getSSFormat() { return getSSCommon().getSSFormat(); }

	/**
	 * Returns the index of the database column to which the SwingSet component is
	 * bound.
	 *
	 * @return returns the index of the column to which the SwingSet component is
	 *         bound
	 */
	@Override
	default int getColumnIndex() {
		return getSSCommon().getColumnIndex();
	}

	/**
	 * Returns the database column name bound to the Swingset component
	 *
	 * @return the bound column name
	 */
	@Override
	default String getColumnName() {
		return getSSCommon().getColumnName();
	}

	/**
	 * Returns the JDBCType representing the bound database column data type.
	 *
	 * @return the enum value corresponding to the data type of the bound column
	 */
	@Override
	default JDBCType getColumnJDBCType() {
		return getSSCommon().getColumnJDBCType();
	}

	/**
	 * Returns the integer code representing the bound database column data type.
	 * <p>
	 * Based on java.sql.Types
	 *
	 * @return the data type of the bound column
	 * @deprecated use {@link #getColumnJDBCType}
	 */
	@Deprecated
	default int getBoundColumnType() {
		return getColumnJDBCType().getVendorTypeNumber();
	}

	/**
	 * Returns the bound column name in square brackets
	 *
	 * @return the bound column name in square brackets
	 */
	@Override
	default String getColumnForLog() {
		return getSSCommon().getColumnForLog();
	}

	/**
	 * Get the backup text for log entries which is only used if columnName is null.
	 * 
	 * @return text for log entries, null if never set
	 */
	default String getLogColumnName() {
		return getSSCommon().getLogColumnName();
	}

	/**
	 * Set the text for log entries which is only used if columnName is null.
	 * @param logColumnName show this in log entry if columnName is null
	 */
	default void setLogColumnName(final String logColumnName) {
		getSSCommon().setLogColumnName(logColumnName);
	}

	/**
	 * Sets the database column name bound to the Swingset component
	 *
	 * @param boundColumnName the columnName to set
	 * @deprecated Use {@link RowsModel#bind(Map) RowsModel.bind(...)}
	 */
	@Deprecated
	default void setBoundColumnName(final String boundColumnName) {
		getSSCommon().setBoundColumnName(boundColumnName);
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//
	// General state, like around the RowSet.
	//

	/**
	 * Returns the RowsModel, encapulating a RowSet, associated with this
	 * components column.
	 *
	 * @return the rowsModel
	 */
	@Override
	default RowsModel getRowsModel() {
		return getSSCommon().getRowsModel();
	}

	/**
	 * Returns the RowSet containing queried data from the database.
	 *
	 * @return the rowSet
	 */
	// TODO: deprecate in favor of RowsModel, it's widely used.
	@Override
	default RowSet getRowSet() {
		return getSSCommon().getRowSet();
	}

	/**
	 * Determine if there's a row that can be modified; dialog if not.
	 * Typically used in an SSComponent's listener.
	 * @return true if there's a row
	 */
	// TODO: move to Utils or SSUtils. Around onActiveRow.
	default boolean checkRowOK() {
		return getSSCommon().checkRowOK();
	}

	/**
	 * Determine if there's a row that can be modified; optionally dialog if not.
	 * If there's no row, only dialog if dialogOK is true.
	 * A nested check never does the dialog.
	 * Typically used in an SSComponent's listener.
	 *
	 * @param dialogOK if null or evaluates true then dialog if no row.
	 * @return true if there's a row
	 */
	default boolean checkRowOK(Supplier<Boolean> dialogOK) {
		return getSSCommon().checkRowOK(dialogOK);
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//
	// Undo/Redo related
	//

	/**
	 * Setup action bindings for undo/redo.
	 */
	default void setupUndoRedoKeys() {
		SSCommon.setupUndoRedoKeys(this);
	}

	/**
	 * Add a change to this components undo/redo stack.
	 * @param ev modification event
	 * @throws java.sql.SQLException
	 */
	default void addUndoableChange(ColumnChangeStartEvent ev) throws SQLException {
		getSSCommon().addUndoableChange(ev);
	}

	/**
	 * Set the component to the new value.
	 * <p>
	 * WARNING: do not override unless ...
	 * @param cmd undo or redo
	 * @param change new value
	 * @throws java.sql.SQLException
	 */
	default void undoRedoUpdateObject(UndoRedo cmd, Change change) throws SQLException
	{
		getSSCommon().undoRedoUpdateObject(cmd, change);
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//
	// Validation/Decoration
	//

	/**
	 * Typically an SSComponent is both the decorate component and focus component
	 * for use with decorators.
	 * But an SSComponent may be grouped with other components, and some
	 * other component may be the the focused component.
	 * @return the component that gets focus when this component is active
	 */
	default Component getFocusTarget() {
		return getSSCommon().getFocusTarget();
	}

	/**
	 * Typically an SSComponent is both the decorate component and focus component
	 * for use with decorators.
	 * But an SSComponent may be grouped with other components, and some
	 * other component may be the the focused component.
	 * @param focusTarget 
	 */
	default void setFocusTarget(Component focusTarget) {
		getSSCommon().setFocusTarget(focusTarget);
	}

	/**
	 * Typically an SSComponent is both the decorate component and focus component
	 * for use with decorators.
	 * But an SSComponent may be grouped with other components, and some
	 * other component may be the the focused component.
	 * @return the component that is decorate decorated 
	 */
	default JComponent getDecorateTarget() {
		return getSSCommon().getDecorateTarget();
	}

	/**
	 * Typically an SSComponent is both the decorate component and focus component
	 * for use with decorators.
	 * But an SSComponent may be grouped with other components, and some
	 * other component may be decorate decorated.
	 * @param decorateTarget
	 */
	default void setDecorateTarget(JComponent decorateTarget) {
		getSSCommon().setDecorateTarget(decorateTarget);
	}

	/**
	 * Install the specified pluginValidator into this component.
	 * Each instance can have a unique validator.
	 * This is run after all the other validations succeed.
	 * @param pluginValidator validator to install
	 */
	default void setPluginValidator(Validator pluginValidator) {
		getSSCommon().setPluginValidator(pluginValidator);
	}

	/**
	 * Override this to do a low level validation of whether or not the
	 * component data is valid.
	 * For example, a mask formatter indicates valid; generally simple
	 * constraints that are context independent; e.g. {@literal month <= 12}.
	 * There may be additional checks defined by {@link #componentValidate() }
	 * and/or a {@link Validator}, see
	 * {@link #setPluginValidator(Validator) }; those are checked after baseValidate.
	 * The default implementation returns true.
	 * 
	 * @return false for error in data, otherwise true
	 */
	 //{@link #setPluginValidator(com.nqadmin.swingset.decorators.Validator) }; those are check after baseValidate.
	default boolean baseValidate() { return true; }

	/**
	 * This has component specific validation, it is called after baseValidate.
	 * @return true if successful validation for the type of component
	 */
	default boolean componentValidate() { return true; }

	/**
	 * The results of doing SSComponent validations.
	 * 
	 * @param base result of baseValidate()
	 * @param comp result of base and componentValidate()
	 * @param other this component is the target of an error event,
	 * see {@link RowsModel#hasError(SSComponent) }
	 * @param plugin result of pluginValidate
	 * @param all true if everything validated
	 */
	record ValidationResult(
			boolean base, boolean comp, boolean other, boolean plugin, boolean all){}

	/**
	 * Run the validators: baseValidate, componentValidate,
	 * possibly check RowsModel.hasError(),
	 * pluginValidate.
	 * The checks are done in order, they stop with any failure.
	 * @return result
	 */
	default ValidationResult allValidate() {
		boolean baseValid = baseValidate();
		boolean compValid = baseValid && componentValidate();
		boolean otherValid = compValid;
		if (compValid && !getSSCommon().skipValidateHasError) {
			RowsModel rowsModel = getRowsModel();
			if (rowsModel != null && rowsModel.getRowSet() != null)
				otherValid = !rowsModel.hasError(this);
		}
		boolean pluginValid = otherValid && getSSCommon().pluginValidate();

		return new ValidationResult(baseValid, compValid, otherValid, pluginValid, pluginValid);
	}

	/**
	 * Create and return the default {@link Decorator}
	 * setup during construction. The default is generally good for
	 * a single {@linkplain JComponent}, for example a {@linkplain com.nqadmin.swingset.SSTextField}.
	 * When a visual component is made up of multiple components a custom
	 * decorator may be required.
	 * 
	 * @return decorator
	 */
	default Decorator createDefaultDecorator() {
		return SSCommon.createDefaultDecorator();
	}

	/**
	 * Return the decorator used by this component.
	 * @return the decorator
	 */
	default Decorator getDecorator() {
		return getSSCommon().getDecorator();
	}

	/**
	 * Install the given decorator.
	 * @param deco decorator to install
	 */
	default void setDecorator(Decorator deco) {
		getSSCommon().setDecorator(deco);
	}

	/**
	 * Run the decorator.
	 * @return true if component data valid
	 */
	default boolean decorate() {
		return getSSCommon().decorate();
	}

	/**
	 * Create and return this components {@link TextDecorator};
	 * setup during construction.
	 * If this method is not overriden, a textDecorator that does nothing.
	 * 
	 * @return textDecorator
	 */
	default TextDecorator createDefaultTextDecorator() {
		return SSCommon.createDefaultTextDecorator();
	}

	/**
	 * Return the text decorator used by this component.
	 * @return the textDecorator
	 */
	default TextDecorator getTextDecorator() {
		return getSSCommon().getTextDecorator();
	}

	/**
	 * Install the specified text decorator.
	 * @param textDeco decorator to install
	 */
	default void setTextDecorator(TextDecorator textDeco) {
		getSSCommon().setTextDecorator(textDeco);
	}

	/** Run the decorator */
	default void decorateText() {
		getSSCommon().decorateText();
	}

	// Methods that have the word Bound in them
	// :g/Bound.*{/
	// default String getBoundColumnText() { return getColumnText(); }
	// default Object getBoundColumnObject() { return getColumnObject(); }
	// default <T> T getBoundColumnObject(Class<T> type) { return getColumnObject(type); }
	// default void setBoundColumnText(final String boundColumnText) { setColumnText(boundColumnText); }
	// default void setBoundColumnObject(final Object boundColumnObject) { setColumnObject(boundColumnObject); }
	// default void setBoundColumnArray(final SSArray boundColumnArray) throws SQLException { setColumnArray(boundColumnArray); }
	// default int getBoundColumnIndex() { return getColumnIndex(); }
	// default String getBoundColumnName() { return getColumnName(); }
	// default JDBCType getBoundColumnJDBCType() { return getColumnJDBCType(); }
	// default int getBoundColumnType() { return getColumnType(); }
	// default void setBoundColumnName(final String boundColumnName) { setColumnName(boundColumnName); }
}
