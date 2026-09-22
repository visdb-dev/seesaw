/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package dev.visdb.seesaw.contrib.lgooddatepicker;

import java.lang.System.Logger;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EventListener;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;
import com.github.lgooddatepicker.zinternaltools.DateChangeEvent;

import dev.visdb.seesaw.navigate.RowsModel;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsComponent;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.DEBUG;
import static java.sql.JDBCType.DATE;

/**
 * Date picker that gets it's value from a database column and sends
 * date changes back to the database. Undo/redo and more is supported,
 * see {@link SsComponent}.
 * The datapicker is based on
 * <a href="https://github.com/LGoodDatePicker/LGoodDatePicker">LGoodDatePicker</a>.
 * <p>
 * It is an example of building a component that inter-operates with SS but is not
 * part of the SS library.
 */
@SuppressWarnings("serial")
public class SsLGoodDatePicker extends DatePicker implements SsComponent {
  private class DbDatePickerListener implements EventListener, DateChangeListener {
    /** {@inheritDoc} */
    @Override
    public void dateChanged(DateChangeEvent dce) {
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
  public SsLGoodDatePicker(RowsModel rowsModel, String boundColumnName) {
    this();
    rowsModel.bind(this, boundColumnName);
  }

  /**
   * Create date picker.
   */
  public SsLGoodDatePicker() {
    super(initialSettings());

    finishSsCommon();
  }

  private static DatePickerSettings initialSettings() {
    DatePickerSettings dps = new DatePickerSettings();
    return dps;
  }

  /**
   * Set custom Decorate/FocusTarget.
   * {@inheritDoc }
   */
  @Override
  public void customInit() {
    // Decorator.DecoratorStyle style = def.lookup(Decorator.DecoratorStyle.class);

    // Highlight the date text field when this component gets focus.
    setDecorateTarget(getComponentDateTextField());
    setFocusTarget(getComponentDateTextField());
  }

  /** {@inheritDoc } */
  @Override
  public void checkColumnType(JDBCType jdbcType) throws IllegalArgumentException {
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
  public void cleanField() {
    if (getAllowNull()) {
      clear();
    } else {
      setDateToToday();
    }
  }

  private Hook hook;

  /** {@inheritDoc } */
  @Override
  public final Hook getSsComponentHook() {
    if (hook == null)
      hook = new Hook(this) {
        @Override
        protected void updateSsComponent() {
          logger.log(DEBUG,
                     () -> sf("%s: getBoundColumnText() - %s", getColumnForLog(), getColumnText()));
          LocalDate value = getColumnObject(LocalDate.class);
          setDate(value);
        }

        @Override
        protected EventListener getSsComponentListener() {
          return new DbDatePickerListener();
        }

        @Override
        protected void addSsComponentListener(EventListener eventListener) {
          addDateChangeListener((DateChangeListener) eventListener);
        }

        @Override
        protected void removeSsComponentListener(EventListener eventListener) {
          removeDateChangeListener((DateChangeListener) eventListener);
        }
      };
    return hook;
  }
}
// vi: sw=2 ts=8
