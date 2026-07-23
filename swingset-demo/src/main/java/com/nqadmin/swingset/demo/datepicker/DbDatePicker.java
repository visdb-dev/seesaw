/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2024-2026 Ernie Rael.  All Rights Reserved.
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
package com.nqadmin.swingset.demo.datepicker;

import java.lang.System.Logger;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EventListener;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;
import com.github.lgooddatepicker.zinternaltools.DateChangeEvent;
import com.nqadmin.swingset.navigate.RowsModel;
import com.nqadmin.swingset.utils.JStuff;
import com.nqadmin.swingset.utils.SSComponent;

import static com.nqadmin.swingset.utils.JStuff.sf;
import static java.lang.System.Logger.Level.DEBUG;
import static java.sql.JDBCType.DATE;

/**
 * Date picker that gets it's value from a database column and sends
 * date changes back to the database. Undo/redo and more is supported,
 * see {@link SSComponent}.
 * The datapick is based on
 * <a href="https://github.com/LGoodDatePicker/LGoodDatePicker">LGoodDatePicker</a>.
 * <p>
 * It is an example of building a component that inter-operates with SS but is not
 * part of the SS library.
 */
@SuppressWarnings("serial")
public class DbDatePicker extends DatePicker implements SSComponent
{
	private class DbDatePickerListener implements EventListener,DateChangeListener {
		/** {@inheritDoc} */
		@Override
		public void dateChanged(final DateChangeEvent dce)
		{
			try {
				dbChange(() -> setColumnObject(dce.getNewDate()));
			} catch (SQLException ex) {
				logger.log(Logger.Level.ERROR, (String) null, ex);
			}
		}
	}
	/** System Logger for component. */
	private static final Logger logger = JStuff.getLogger();

	/**
	 * Create date picker and bind it to the specified column in the
	 * given RowSet.
	 *
	 * @param rowsModel       datasource to be used.
	 * @param boundColumnName name of the column to which this check box should
	 *                        be bound
	 */
	@SuppressWarnings("LeakingThisInConstructor")
	public DbDatePicker(RowsModel rowsModel, String boundColumnName)
	{
		this();
		rowsModel.bind(this, boundColumnName);
	}

	/**
	 * Create date picker.
	 */
	public DbDatePicker()
	{
		super(initialSettings());

		finishSSCommon();
	}
	
	private static DatePickerSettings initialSettings()
	{
		DatePickerSettings dps = new DatePickerSettings();
		return dps;
	}

	/**
	 * Set custom Decorate/FocusTarget.
	 * {@inheritDoc }
	 */
	@Override
	public void customInit()
	{
		// Decorator.DecoratorStyle style = def.lookup(Decorator.DecoratorStyle.class);

		// Highlight the date text field when this component gets focus.
		setDecorateTarget(getComponentDateTextField());
		setFocusTarget(getComponentDateTextField());
	}

	/** {@inheritDoc } */
	@Override
	public void checkColumnType(JDBCType jdbcType) throws IllegalArgumentException
	{
		if (jdbcType != DATE)
			throw new IllegalArgumentException(sf("Date Picker column type must be DATE"));
	}

	/**
	 * This component contains multiple components some of which can get focus.
	 * @return true
	 */
	@Override
	public boolean isComposite() {
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public void metadataChange() {
		getSettings().setAllowEmptyDates(getAllowNull());
	}
	
	/** {@inheritDoc } */
	@Override
	public void cleanField()
	{
		if (getAllowNull()) {
			clear();
		} else {
			setDateToToday();
		}
	}

	private Hook hook;

	/** {@inheritDoc } */
	@Override
	public final Hook getSSComponentHook()
	{
		if (hook == null)
			hook = new Hook(this) {
				@Override
				protected void updateSSComponent()
				{
					logger.log(DEBUG, () -> sf("%s: getBoundColumnText() - %s",getColumnForLog(), getColumnText()));
					LocalDate value = getColumnObject(LocalDate.class);
					setDate(value);
				}
				
				@Override
				protected EventListener getSSComponentListener()
				{
					return new DbDatePickerListener();
				}
				
				@Override
				protected void addSSComponentListener(EventListener eventListener)
				{
					addDateChangeListener((DateChangeListener) eventListener);
				}
				
				@Override
				protected void removeSSComponentListener(EventListener eventListener)
				{
					removeDateChangeListener((DateChangeListener) eventListener);
				}
			};
		return hook;
	}
}
