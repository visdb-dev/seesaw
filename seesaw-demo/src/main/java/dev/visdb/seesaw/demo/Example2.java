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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.sql.RowSet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.netbeans.validation.api.ui.ValidationGroup;
import org.netbeans.validation.api.ui.ValidationItem;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;
import org.netbeans.validation.api.ui.swing.ValidationPanel;

import dev.visdb.seesaw.SsComboBox1;
import dev.visdb.seesaw.SsTextField;
import dev.visdb.seesaw.contrib.simplevalidation.SVUtils;
import dev.visdb.seesaw.datasources.DbOps;
import dev.visdb.seesaw.datasources.products.DbOpsBase;
import dev.visdb.seesaw.decorators.TextComponentValidator;
import dev.visdb.seesaw.navigate.RowsModel;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsDataNavigator;

/**
 * This example displays data from the supplier_data table.
 * SSTextFields are used to display supplier id, name,
 * and city. SSComboBox is used to display status.
 * <p>
Record navigation is handled with a SsDataNavigator.
 */
@SuppressWarnings("serial")
public class Example2 extends JFrame {
  /**
   * Log4j2 Logger
   */
  private static final Logger logger = JStuff.getLogger();

  /**
   * screen label declarations
   */
  JLabel lblSupplierID = new JLabel("Supplier ID");
  JLabel lblSupplierName = new JLabel("Name");
  JLabel lblSupplierCity = new JLabel("City");
  JLabel lblSupplierStatus = new JLabel("Status");

  /**
   * bound component declarations
   */
  SsTextField txtSupplierID = new SsTextField();
  SsTextField txtSupplierName = new SsTextField();
  SsTextField txtSupplierCity = new SsTextField();
  /** see {@link TestBaseComponents.MyComboBox} */
  SsComboBox1<Integer, String> cmbSupplierStatus
      = new SsComboBox1.Builder<Integer, String>() {}.build();

  /**
   * database component declarations
   */
  Connection connection = null;
  SsDataNavigator navigator = null;
  RowsModel rowsModel;

  /**
   * Constructor for Example2
   * <p>
   * @param _dbConn - database connection
   */
  @SuppressWarnings("LeakingThisInConstructor")
  public Example2(Connection _dbConn) {
    // Set screen title
    super("SeeSaw Example2");
    DemoUtil.initExampleFrame(this, null);

    JFrame frame = this;

    // Set connection
    connection = _dbConn;

    // Set screen dimensions
    setSize(MainClass.childScreenWidth, MainClass.childScreenHeight);

    // Set screen position
    setLocation(DemoUtil.getChildScreenLocation(this.getName()));

    // Set a validator.
    final boolean USE_SIMPLE_VALIDATION = true;
    Function<String, Boolean> validateSupplierName = (str) -> {
      return str == null || !str.matches("(?i).*oops.{0,2}$");
    };
    Function<String, Boolean> validateSupplierCity = (str) -> {
      return str == null || !str.matches(".*X");
    };
    ValidationItem decoSupplierName = null;
    ValidationItem decoSupplierCity = null;
    if (USE_SIMPLE_VALIDATION) {
      SwingValidationGroup.setComponentName(txtSupplierName, "Supplier Name");
      decoSupplierName = SVUtils.decorator(txtSupplierName, SVUtils.getStringValidator(
          validateSupplierName, () -> "Supplier name can not end with 'oops..'"));
      decoSupplierCity = SVUtils.decorator(txtSupplierCity, SVUtils.getStringValidator(
          validateSupplierCity, () -> "City can not end in 'X'"));
    } else {
      txtSupplierName.setPluginValidator(TextComponentValidator.create(validateSupplierName));
      txtSupplierCity.setPluginValidator(
          TextComponentValidator.create(validateSupplierCity));
    }

    // Initialize database connection and components
    try {
      RowSet rowset = DemoUtil.getNewRowSet(connection);
      rowset.setCommand("SELECT * FROM supplier_data");
      rowset.execute();
      rowsModel = RowsModel.create(rowset, createDbNav());
      navigator = new SsDataNavigator(rowsModel);
    } catch (final SQLException se) {
      logger.log(Level.ERROR, "SQL Exception.", se);
    }

    // Setup the combo box options to be displayed and their corresponding values
    //	 lets assume the status code to text mappings
    // 		10 -> BAD
    // 		20 -> BETTER
    // 		30 -> GOOD
    cmbSupplierStatus.setDisplayValues(List.of("Bad", "Better", "Good"), List.of(10, 20, 30));

    // Bind the components to the database columns
    rowsModel.bind(Map.of(txtSupplierID, "supplier_id",
                          txtSupplierName, "supplier_name",
                          txtSupplierCity, "city",
                          cmbSupplierStatus, "status"));
    //this.cmbSupplierStatus.setSelectedIndex(1);

    // Set label dimensions
    lblSupplierID.setPreferredSize(MainClass.labelDim);
    lblSupplierName.setPreferredSize(MainClass.labelDim);
    lblSupplierCity.setPreferredSize(MainClass.labelDim);
    lblSupplierStatus.setPreferredSize(MainClass.labelDim);

    // Set bound component dimensions
    txtSupplierID.setPreferredSize(MainClass.ssDim);
    txtSupplierName.setPreferredSize(MainClass.ssDim);
    txtSupplierCity.setPreferredSize(MainClass.ssDim);
    cmbSupplierStatus.setPreferredSize(MainClass.ssDim);

    // Setup the container and layout the components
    //final Container contentPane = getContentPane();
    final Container contentPane = new JPanel();
    contentPane.setLayout(new GridBagLayout());
    final GridBagConstraints constraints = new GridBagConstraints();

    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weightx = .40;
    constraints.anchor = GridBagConstraints.WEST;
    contentPane.add(lblSupplierID, constraints);
    constraints.gridy = 1;
    contentPane.add(lblSupplierName, constraints);
    constraints.gridy = 2;
    contentPane.add(lblSupplierCity, constraints);
    constraints.gridy = 3;
    contentPane.add(lblSupplierStatus, constraints);

    constraints.gridx = 1;
    constraints.gridy = 0;
    constraints.weightx = .60;
    constraints.anchor = GridBagConstraints.CENTER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    contentPane.add(txtSupplierID, constraints);
    constraints.gridy = 1;
    contentPane.add(txtSupplierName, constraints);
    constraints.gridy = 2;
    contentPane.add(txtSupplierCity, constraints);
    constraints.gridy = 3;
    contentPane.add(cmbSupplierStatus, constraints);

    constraints.gridx = 0;
    constraints.gridy = 4;
    constraints.gridwidth = 2;
    contentPane.add(navigator, constraints);

    // DISABLE THE PRIMARY KEY
    txtSupplierID.setEnabled(false);

    // Set up the simple validation panel.
    JPanel uiPanel;
    if (USE_SIMPLE_VALIDATION) {
      ValidationPanel valiPanel = new ValidationPanel();
      valiPanel.setInnerComponent(contentPane);
      ValidationGroup group = valiPanel.getValidationGroup();
      group.addItem(decoSupplierName, false);
      group.addItem(decoSupplierCity, false);
      uiPanel = valiPanel;
    } else {
      uiPanel = (JPanel) contentPane;
    }

    // MAKE THE JFRAME VISIBLE
    frame.add(uiPanel);
    frame.setVisible(true);
    frame.pack();
  }

  private DbOps createDbNav() {
    /**
     * Various navigator overrides needed to support H2
     * H2 does not fully support updatable rowset so it must be
     * re-queried following insert and delete with rowset.execute()
     */
    return new DbOpsBase(this) {
      /**
       * Obtain and set the PK value for the new record and perform any other
       * actions needed before an insert.
       */
      @Override
      public void performPreInsertOps() {
        // SSDBNavImpl will clear the component values
        super.performPreInsertOps();

        try (final ResultSet rs
             = connection
                   .createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)
                   .executeQuery("SELECT nextval('supplier_data_seq') as nextVal;");) {
          // Get the new record id.
          rs.next();
          final int supplierID = rs.getInt("nextVal");
          txtSupplierID.setText(String.valueOf(supplierID));

          // // Set other defaults
          // 	 txtSupplierName.setText(null);
          // 	 txtSupplierCity.setText(null);
          // 	 cmbSupplierStatus.setSelectedValue(0);

        } catch (final SQLException se) {
          logger.log(Level.ERROR, "SQL Exception occured initializing new record.", se);
        } catch (final Exception e) {
          logger.log(Level.ERROR, "Exception occured initializing new record.", e);
        }
      }
    };
  }
}
// vi: sw=2 ts=8
