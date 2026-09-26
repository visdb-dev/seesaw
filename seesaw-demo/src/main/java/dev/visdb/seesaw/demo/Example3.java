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
package dev.visdb.seesaw.demo;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.RowSet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import dev.visdb.seesaw.SsDbComboBox2;
import dev.visdb.seesaw.SsTextField;
import dev.visdb.seesaw.datasources.DbOps;
import dev.visdb.seesaw.datasources.products.DbOpsBase;
import dev.visdb.seesaw.formatting.SsDateField;
import dev.visdb.seesaw.formatting.SsIntegerField;
import dev.visdb.seesaw.navigate.RowsModel;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsDataNavigator;
import dev.visdb.seesaw.utils.SsUtils;

import static dev.visdb.seesaw.formatting.SsFormat.DATE_MMDDYYYY_SLASH;
import static java.lang.System.Logger.Level.*;

/**
 * This example displays data from the supplier_part_data table.
 * SSTextFields are used to display supplier-part id and quantity.
 * DBComboBox2 are used to display supplier name and part name
 * based on queries against the supplier_data and part_data tables.
 * <p>
Record navigation is handled with a SsDataNavigator.
 */
@SuppressWarnings("serial")
public class Example3 extends JFrame {
  /**
   * Log4j2 Logger
   */
  private static final Logger logger = JStuff.getLogger();

  /**
   * screen label declarations
   */
  JLabel lblSupplierPartID = new JLabel("Supplier-Part ID");
  JLabel lblSupplierName = new JLabel("Supplier");
  JLabel lblPartName = new JLabel("Part");
  JLabel lblQuantity = new JLabel("Quantity");
  JLabel lblShipDate = new JLabel("Ship Date");

  /**
   * bound component declarations
   */
  SsTextField txtSupplierPartID = new SsTextField();
  SsDbComboBox2<Long, Object, Object> cmbSupplierName = null;
  SsDbComboBox2<Long, Object, Object> cmbPartName = null;

  SsIntegerField txtQuantity = new SsIntegerField();
  SsDateField txtShipDate = new SsDateField(DATE_MMDDYYYY_SLASH);

  /**
   * database component declarations
   */
  Connection connection = null;
  SsDataNavigator navigator = null;
  RowsModel rowsModel;

  @SuppressWarnings("unused")
  RowSet getRowSet() {
    return rowsModel.getRowSet();
  }

  /**
   * Constructor for Example3
   * <p>
   * @param _dbConn - database connection
   */
  @SuppressWarnings("LeakingThisInConstructor")
  public Example3(Connection _dbConn) {
    // SET SCREEN TITLE
    super("SeeSaw Example3");
    DemoUtil.initExampleFrame(this, null);

    // SET CONNECTION
    connection = _dbConn;

    // SET SCREEN DIMENSIONS
    setSize(MainClass.childScreenWidth, MainClass.childScreenHeight);

    // SET SCREEN POSITION
    setLocation(DemoUtil.getChildScreenLocation(this.getName()));

    // INITIALIZE DATABASE CONNECTION AND COMPONENTS
    try {
      RowSet rowset = DemoUtil.getNewRowSet(connection);
      rowset.setCommand("SELECT * FROM supplier_part_data");
      rowset.execute();
      rowsModel = RowsModel.create(rowset, createDbNav());
      navigator = new SsDataNavigator(rowsModel);
    } catch (final SQLException se) {
      logger.log(Level.ERROR, "SQL Exception.", se);
    }

    // SETUP DB COMBO QUERIES
    String query = "SELECT * FROM supplier_data;";
    // (connection, query, "supplier_id", "supplier_name");
    cmbSupplierName = new SsDbComboBox2.Builder<Long, Object, Object>() {}
                          .connection(connection)
                          .query(query)
                          .primaryKeyColumnName("supplier_id")
                          .displayColumnName("supplier_name")
                          .build();

    query = "SELECT * FROM part_data;";
    //(connection, query, "part_id", "part_name");
    cmbPartName = new SsDbComboBox2.Builder<Long, Object, Object>() {}
                      .connection(connection)
                      .query(query)
                      .primaryKeyColumnName("part_id")
                      .displayColumnName("part_name")
                      .build();

    // BIND THE COMPONENTS TO THE DATABASE COLUMNS
    cmbPartName.setAllowNull(false);

    rowsModel.bind(txtSupplierPartID, "supplier_part_id");
    rowsModel.bind(cmbSupplierName, "supplier_id");
    rowsModel.bind(cmbPartName, "part_id");
    rowsModel.bind(txtQuantity, "quantity");
    rowsModel.bind(txtShipDate, "ship_date");

    // RUN DB COMBO QUERIES
    try {
      cmbPartName.execute();
      cmbSupplierName.execute();

    } catch (final SQLException se) {
      logger.log(Level.ERROR, "SQL Exception.", se);
    } catch (final Exception e) {
      logger.log(Level.ERROR, "Exception.", e);
    }

    // SET LABEL DIMENSIONS
    lblSupplierPartID.setPreferredSize(MainClass.labelDim);
    lblSupplierName.setPreferredSize(MainClass.labelDim);
    lblPartName.setPreferredSize(MainClass.labelDim);
    lblQuantity.setPreferredSize(MainClass.labelDim);
    lblShipDate.setPreferredSize(MainClass.labelDim);

    // SET BOUND COMPONENT DIMENSIONS
    txtSupplierPartID.setPreferredSize(MainClass.ssDim);
    cmbSupplierName.setPreferredSize(MainClass.ssDim);
    cmbPartName.setPreferredSize(MainClass.ssDim);
    txtQuantity.setPreferredSize(MainClass.ssDim);
    txtShipDate.setPreferredSize(MainClass.ssDim);

    // SETUP THE CONTAINER AND LAYOUT THE COMPONENTS
    final Container uiPanel = new JPanel(new GridBagLayout());
    setContentPane(SsUtils.createDecoratorPanel(uiPanel));

    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    // constraints.weightx = .40;
    // constraints.anchor = GridBagConstraints.WEST;
    uiPanel.add(lblSupplierPartID, constraints);
    constraints.gridy = 1;
    uiPanel.add(lblSupplierName, constraints);
    constraints.gridy = 2;
    uiPanel.add(lblPartName, constraints);
    constraints.gridy = 3;
    uiPanel.add(lblQuantity, constraints);
    constraints.gridy = 4;
    uiPanel.add(lblShipDate, constraints);

    constraints.gridx = 1;
    constraints.gridy = 0;
    // constraints.weightx = .60;
    // constraints.anchor = GridBagConstraints.CENTER;
    // constraints.fill = GridBagConstraints.HORIZONTAL;
    uiPanel.add(txtSupplierPartID, constraints);
    constraints.gridy = 1;
    uiPanel.add(cmbSupplierName, constraints);
    constraints.gridy = 2;
    uiPanel.add(cmbPartName, constraints);
    constraints.gridy = 3;
    uiPanel.add(txtQuantity, constraints);
    constraints.gridy = 4;
    uiPanel.add(txtShipDate, constraints);

    constraints.gridx = 0;
    constraints.gridy = 5;
    constraints.gridwidth = 2;
    uiPanel.add(navigator, constraints);

    // DISABLE THE PRIMARY KEY
    txtSupplierPartID.setEnabled(false);

    //			cmbSupplierName.addDisplayValue("Adams2", (long)5);
    //			cmbSupplierName.updateDisplayValue((long)55, "Hello");

    // MAKE THE JFRAME VISIBLE
    setVisible(true);
    pack();
  }

  /**
   * Various navigator overrides needed to support H2
   * H2 does not fully support updatable rowset so it must be
   * re-queried following insert and delete with rowset.execute()
   */
  private DbOps createDbNav() {
    return new DbOpsBase(this) {
      /**
       * Obtain and set the PK value for the new record & perform any other actions needed before an insert.
       */
      @Override
      public void performPreInsertOps() {
        // SSDBNavImpl will clear the component values
        super.performPreInsertOps();

        // GET THE NEW RECORD ID.
        // .createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)
        try (ResultSet rs = connection.createStatement().executeQuery(
                 "SELECT nextval('supplier_part_data_seq') as nextVal;")) {
          rs.next();
          int supplierPartID = rs.getInt("nextVal");
          txtSupplierPartID.setText(String.valueOf(supplierPartID));
        } catch (final SQLException se) {
          logger.log(Level.ERROR, "SQL Exception occured initializing new record.", se);
        } catch (final Exception e) {
          logger.log(Level.ERROR, "Exception occured initializing new record.", e);
        }
        // SET OTHER DEFAULTS
        logger.log(DEBUG, "Setting default for Supplier Name mapping to 2.");
        cmbSupplierName.setChosenKey((long) 2);
        //					 cmbPartName.setSelectedValue(0);
        //					 txtQuantity.setText("0");
      }
    };
  }
}
// vi: sw=2 ts=8
