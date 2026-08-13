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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.io.StringReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.RowSet;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import dev.visdb.seesaw.core.CheckBox;
import dev.visdb.seesaw.core.ComboBox1;
import dev.visdb.seesaw.core.DBComboBox2;
import dev.visdb.seesaw.core.Image;
import dev.visdb.seesaw.core.Label;
import dev.visdb.seesaw.core.List1;
import dev.visdb.seesaw.core.Slider;
import dev.visdb.seesaw.core.TextArea;
import dev.visdb.seesaw.core.TextField;
import dev.visdb.seesaw.datasources.DbOps;
import dev.visdb.seesaw.datasources.products.DbOpsBase;
import dev.visdb.seesaw.datasources.products.DbOpsCreator;
import dev.visdb.seesaw.decorators.ComponentState;
import dev.visdb.seesaw.decorators.ComponentStateTextDecorator;
import dev.visdb.seesaw.decorators.TextComponentValidator;
import dev.visdb.seesaw.decorators.TextStyles;
import dev.visdb.seesaw.demo.datepicker.DbDatePicker;
import dev.visdb.seesaw.models.SSCollection;
import dev.visdb.seesaw.models.SSDbArray;
import dev.visdb.seesaw.navigate.RowsModel;
import dev.visdb.seesaw.utils.CentralLookup;
import dev.visdb.seesaw.utils.DataNavigator;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SSComponent;
import dev.visdb.seesaw.utils.SyncManager;

import static dev.visdb.seesaw.demo.TestBaseComponents.CompDim.*;
import static dev.visdb.seesaw.demo.TestBaseComponents.CompID.*;
import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * This example demonstrates all of the Base SwingSet Components
 * except for the SSDataGrid.
 * <p>
 * There is a separate example screen to demonstrate the
 * Formatted SwingSet Components.
 * <p>
 * IMPORTANT: The relationship of the DBComboBox2 and RowSet queries can have a
 * large negative impact on performance. See {@link SyncManager} for an
 * explanation.
 */

@SuppressWarnings("serial")
public class TestBaseComponents extends JFrame {
  // clang-format off
  enum CompID {
    NAV, PK, CHECK, COMBO, ENUM_COMBO, DB_COMBO, IMAGE, LABEL,
    LIST, LIST2, SLIDER, TEXT_AREA, TEXT_FIELD, TEXT_FIELD_B,
    DATE_PICKER,
  }

  enum CompDim {
    //NORMAL, TALL, VERY_TALL
    H1, H2, H3
  }

  // Thing in this set will not have their preferred height made smaller.
  private EnumSet<CompID> keepMinHeight = EnumSet.of(
      // H1
      // Commnet out the next line to get original test behavior
      NAV, PK, CHECK, COMBO, ENUM_COMBO, DB_COMBO, LABEL, SLIDER,
      TEXT_FIELD, TEXT_FIELD_B,
      DATE_PICKER

      // H2
      // LIST, LIST2, TEXT_AREA,

      // H3
      // IMAGE
  );

  private Map<CompID, CompInfo> compInfos = new EnumMap<>(CompID.class);
  private EnumSet<CompID> activeComps = EnumSet.allOf(CompID.class);
  // compInfo is typically both a JComponent and SSComponent.
  // TODO?: Could have both fields for the few cases where separate,
  //        typically a list wrapped in a scrollPane.
  private record CompInfo(String col, JComponent comp, JLabel label, CompDim dim, CompID compID) {
    CompInfo replace(JComponent newComp) { return new CompInfo(col, newComp, label, dim, compID); }
  }

  /**
   * Some components aren't fully initialized until well after startup,
   * so replace the component in the info.
   */
  private void replaceComponent(CompID eComp, JComponent comp) {
    compInfos.put(eComp, compInfos.get(eComp).replace(comp));
  }

  private void populateCompInfo() {
    // Everything in an array
    // This MUST be in the same order as the CompID enum.
    Object tComps[] = {
      null,                      cmbSSDBComboNav,     lblSSDBComboNav,     H1,
      "swingset_base_test_pk",   txtTestPK,           lblTestPK,           H1,
      "ss_check_box",            chkSSCheckBox,       lblSSCheckBox,       H1,
      "ss_combo_box",            cmbSSComboBox,       lblSSComboBox,       H1,
      "ss_combo_box",            cmbEnumSSComboBox,   lblEnumSSComboBox,   H1,
      "ss_db_combo_box",         cmbSSDBComboBox,     lblSSDBComboBox,     H1,
      "ss_image",                imgImage,            lblImage,            H3,
      "ss_label",                lblSSLabel2,         lblSSLabel,          H1,
      "ss_list",                 lstSSList,           lblSSList,           H2,
      "ss_list2",                lstSSList2,          lblSSList2,          H2,
      "ss_slider",               sliSSSlider,         lblSSSlider,         H1,
      "ss_text_area",            txtSSTextArea,       lblSSTextArea,       H2,
      "ss_text_field",           txtSSTextField,      lblSSTextField,      H1,
      "ss_text_field",           txtSSTextFieldB,     lblSSTextFieldB,     H1,
      "ss_date_field_null",      dpDatePicker,        lblDatePicker,       H1
    };
    int idx = 0;
    for (CompID compID : CompID.values()) {
      compInfos.put(compID, new CompInfo(
              (String) tComps[idx++],
          (JComponent) tComps[idx++],
              (JLabel) tComps[idx++],
             (CompDim) tComps[idx++],
                       compID));
    }
  }
  // clang-format on

  /**
   * Log4j2 Logger
   */
  private static final Logger logger = JStuff.getLogger();

  /**
   * map of 'hints' contianing info on which collection model to use
   */
  private final Map<String, Object> hints;

  /**
   * combo and list items
   */
  enum ComboEnum {
    A("Combo Enum 0"),
    B("Combo Enum 1"),
    C("Combo Enum 2"),
    D("Combo Enum 3");
    private final String displayVal;
    private ComboEnum(String _displayVal) {
      displayVal = _displayVal;
    }

    @Override
    public String toString() {
      return displayVal;
    }
  }
  /** only used manually for testing */
  enum ListEnum {
    A("List Enum 1"),
    B("List Enum 2"),
    C("List Enum 3"),
    D("List Enum 4"),
    E("List Enum 5"),
    F("List Enum 6"),
    G("List Enum 7");
    private final String displayVal;
    private ListEnum(String _displayVal) {
      displayVal = _displayVal;
    }

    @Override
    public String toString() {
      return displayVal;
    }
  }
  private static final String[] comboItems
      = {"Combo Item 0", "Combo Item 1", "Combo Item 2", "Combo Item 3"};
  //private static final int[] comboCodes = {0,1,2,3};
  private static final Integer[] comboCodesIntegers = new Integer[] {0, 1, 2, 3};
  private static final Object[] listCodes = {1, 2, 3, 4, 5, 6, 7};
  private static final String[] listItems
      = {"List Item 1", "List Item 2", "List Item 3", "List Item 4",
         "List Item 5", "List Item 6", "List Item 7"};

  /**
   * screen label declarations
   */
  JLabel lblSSDBComboNav = new JLabel("SSDBComboNav"); // SSDBComboBox used just for navigation
  JLabel lblTestPK = new JLabel("Record ID");
  JLabel lblSSCheckBox = new JLabel("CheckBox");
  JLabel lblSSComboBox = new JLabel("ComboBox");
  JLabel lblEnumSSComboBox = new JLabel("enumSSComboBox");
  JLabel lblSSDBComboBox = new JLabel("DBComboBox2");
  JLabel lblImage = new JLabel("Image");
  JLabel lblSSLabel = new JLabel("Label");
  JLabel lblSSList = new JLabel("List");
  JLabel lblSSList2 = new JLabel("List String");
  JLabel lblSSSlider = new JLabel("Slider");
  JLabel lblSSTextArea = new JLabel("TextArea");
  JLabel lblSSTextField = new JLabel("TextField");
  JLabel lblSSTextFieldB = new JLabel("SSTextFieldB");
  JLabel lblDatePicker = new JLabel("DbDatePicker");

  //
  // bound component declarations
  //

  // For a generic class with a generic builder, simplify by creating a wrapper
  // class. See declarations for cmbSSComboBox, cmbEnumSSComboBox following.
  // See SSComboBox for an example that exposes Buidler.
  static class MyComboBox extends ComboBox1<Integer, String> {
    MyComboBox() {
      super(new ComboBox1.Builder<Integer, String>() {});
    }
  }

  ComboBox1<Integer, String> cmbSSComboBox = new ComboBox1.Builder<Integer, String>() {}.build();
  MyComboBox cmbEnumSSComboBox = new MyComboBox();
  TextField txtTestPK = new TextField();
  CheckBox chkSSCheckBox = new CheckBox("labeled checkbox");
  DBComboBox2<Long, Object, Object> cmbSSDBComboBox;
  Image imgImage = new Image();
  Label lblSSLabel2 = new Label();
  final List1<Object, String> lstSSList;
  final List1<Object, String> lstSSList2;
  Slider sliSSSlider = new Slider();
  TextArea txtSSTextArea = new TextArea();
  TextField txtSSTextField = new TextField();
  TextField txtSSTextFieldB = new TextField();
  DbDatePicker dpDatePicker = new DbDatePicker();

  /**
   * database component declarations
   */
  Connection connection = null;
  DataNavigator navigator = null;
  RowsModel rowsModel;

  /**
   * combo navigator and sync manger
   */
  final DBComboBox2<Long, Object, Object> cmbSSDBComboNav; // DBComboBox2 used just for navigation
  final SyncManager<Long> syncManager;

  /**
   * Method to obtain proper data structure/model for List1 based on database used
   * @return collection model to use for lists based on underlying database
   */
  @SuppressWarnings("unused")
  private SSCollection getCollectionModel() {
    @SuppressWarnings("unchecked")
    Supplier<SSCollection> supl = (Supplier<SSCollection>) hints.get("collectionModel");
    return supl == null ? new SSDbArray(JDBCType.INTEGER) : supl.get();
  }

  /**
   * Constructor for Base Component Test
   * <p>
   * @param _dbConn database connection
   * @param _hints dynamic information on collection model, other
   */
  @SuppressWarnings({"LeakingThisInConstructor", "CallToPrintStackTrace"})
  public TestBaseComponents(Connection _dbConn, Map<String, Object> _hints) {
    // set screen title
    super("SeeSaw Base Component Test");
    DemoUtil.initExampleFrame(this, null);

    // imgImage.setResizeMode(ZoomCanvas.ResizeMode.CENTER_PANNING);

    // initialize some dynamic information
    hints = _hints;

    lstSSList = new List1<>(getCollectionModel());
    //lstSSList = new SSList(JDBCType.INTEGER); // SSCollection.getSuitableDbCollection()

    // lstSSList2 = new SSList(new SSDbStringCollection(
    // 		JDBCType.INTEGER, SSDbStringCollection.COMMA_SEP));
    lstSSList2 = new List1<>(JDBCType.INTEGER); // auto pick for String column

    populateCompInfo();
    //activeComps.remove(DATE_PICKER);

    //activeComps.removeAll(EnumSet.of(CHECK, LABEL));
    //activeComps.clear();
    //activeComps.addAll(EnumSet.of(
    //		PK, LABEL, LIST, TEXT_FIELD
    //		//NAV, LABEL, TEXT_FIELD
    //		// NAV, PK, CHECK, COMBO, ENUM_COMBO, DB_COMBO, IMAGE, LABEL,
    //		// LIST, SLIDER, TEXT_AREA, TEXT_FIELD
    //));

    activeComps.remove(LIST2);
    //activeComps.remove(TEXT_FIELD_B);

    // SET CONNECTION
    connection = _dbConn;

    // set screen dimensions
    setSize(MainClass.childScreenWidth, MainClass.childScreenHeightTall);

    // set screen position
    setLocation(DemoUtil.getChildScreenLocation(this.getName()));

    // TEST DbOpsCreator
    DbOpsCreator creator = (RowSet rs, RowsModel rowsModel1) -> createDbNav();
    CentralLookup lkup = CentralLookup.getDefault();
    DbOpsCreator prevCreator = lkup.lookup(DbOpsCreator.class);
    lkup.replace(DbOpsCreator.class, creator);

    // initialize database connection and components
    try {
      RowSet rowset = DemoUtil.getNewRowSet(connection);
      String sql = sf("SELECT %s FROM swingset_base_test_data", getColumnsSQL());
      logger.log(INFO, sql);
      rowset.setCommand(sql);
      rowset.execute();
      rowsModel = RowsModel.create(rowset, null);
      // navigator = new SSDataNavigator(rowsModel);
      navigator = new DataNavigator(rowsModel, DataNavigator.Lines.TWO);
    } catch (final SQLException se) {
      logger.log(Level.ERROR, "SQL Exception.", se);
    }
    String name = rowsModel.getDbOps().getClass().getSimpleName();
    if (!name.equals("TestBaseComponentsDbOps")) {
      throw new IllegalStateException(sf("wrong customizer %s", name));
    }
    lkup.remove(creator);
    if (prevCreator != null) {
      lkup.add(prevCreator);
    }

    if (!activeComps.contains(NAV)) {
      cmbSSDBComboNav = null;
      syncManager = null;
    } else {
      // setup navigator query
      final String query = "SELECT * FROM swingset_base_test_data;";
      //= (connection, query, "swingset_base_test_pk", "swingset_base_test_pk");
      cmbSSDBComboNav = new DBComboBox2.Builder<Long, Object, Object>() {}
                            .connection(connection)
                            .query(query)
                            .primaryKeyColumnName("swingset_base_test_pk")
                            .displayColumnName("swingset_base_test_pk")
                            .build();

      try {
        cmbSSDBComboNav.execute();
      } catch (final SQLException se) {
        logger.log(Level.ERROR, "SQL Exception.", se);
      } catch (final Exception e) {
        logger.log(Level.ERROR, "Exception.", e);
      }

      // Setup syncmanager, which will take care of keeping the combo navigator and
      // data navigator in sync.
      //
      // Before changing the query or re-executing the query for the combo box,
      // you have to call the .async() method.
      //
      // After calling .execute() on the combo navigator, call the .SYNC() method.
      syncManager = new SyncManager<>(cmbSSDBComboNav, rowsModel);
      syncManager.setSyncColumnName("swingset_base_test_pk");
      syncManager.sync();
    }

    // setup combo and list options
    if (activeComps.contains(COMBO)) {
      // TODO if getAllowNull() is true then add blank item to ComboBox
      cmbSSComboBox.setAllowNull(true);
      cmbSSComboBox.setDisplayValues(Arrays.asList(comboItems), Arrays.asList(comboCodesIntegers));
    }
    if (activeComps.contains(ENUM_COMBO)) {
      cmbEnumSSComboBox.setAllowNull(true);
      cmbEnumSSComboBox.setDisplayValues(ComboEnum.class);
    }

    // NOTE following enum has [0,N) mapping, but DB is [1,N]
    //lstSSList.setDisplayValues(ListEnum.class);
    if (activeComps.contains(LIST)) {
      lstSSList.setDisplayValues(Arrays.asList(listItems), Arrays.asList(listCodes));
    }
    if (activeComps.contains(LIST2)) {
      lstSSList2.setDisplayValues(Arrays.asList(listItems), Arrays.asList(listCodes));
    }

    if (activeComps.contains(DB_COMBO)) {
      final String dbComboQuery = "SELECT * FROM part_data;";
      // (connection, dbComboQuery, "part_id", "part_name");
      cmbSSDBComboBox = new DBComboBox2.Builder<Long, Object, Object>() {}
                            .connection(connection)
                            .query(dbComboQuery)
                            .primaryKeyColumnName("part_id")
                            .displayColumnName("part_name")
                            .build();
      cmbSSDBComboBox.setAllowNull(false);
      // TODO if getAllowNull() is false, user can still blank out the combo - we may want to prevent this
    }

    // set slider range
    sliSSSlider.setMaximum(25);

    // SSComponents are setup, save info that may have changed.
    replaceComponent(NAV, cmbSSDBComboNav);
    replaceComponent(DB_COMBO, cmbSSDBComboBox);

    // validators for the text fields
    Function<String, Boolean> validator = (str) -> str == null || !str.matches("(?i).*oops.{0,2}$");
    if (activeComps.contains(TEXT_FIELD)) {
      txtSSTextField.setPluginValidator(TextComponentValidator.create(validator));
    }
    if (activeComps.contains(TEXT_FIELD_B)) {
      txtSSTextFieldB.setPluginValidator(TextComponentValidator.create(validator));
      try {
        setupOurTextStyles();
      } catch (IOException ex) {
        logger.log(Level.ERROR, (String) null, ex);
      }
      txtSSTextFieldB.setTextDecorator(new ComponentStateTextDecorator(
          Map.of(ComponentState.ERROR, "testComponents_componentStateError",
                 ComponentState.MODIFIED, "testComponents_componentStateModified")));
    }

    // Bind the components to their database columns.
    // Note, this needs to happen before scrollPane might repace SSComponent.
    buildGui_bind();

    if (activeComps.contains(DB_COMBO)) {
      // Run db combo queries.
      try {
        cmbSSDBComboBox.execute();
      } catch (final SQLException se) {
        logger.log(Level.ERROR, "SQL Exception.", se);
      } catch (final Exception e) {
        logger.log(Level.ERROR, "Exception.", e);
      }
    }

    // Set the dimensions of the labels and components.
    buildGui_dim();

    JScrollPane lstScrollPane = null;
    if (activeComps.contains(LIST)) {
      lstScrollPane = new JScrollPane(lstSSList);
      lstScrollPane.setPreferredSize(MainClass.ssDimTall);
      lstSSList.setDecorateTarget(lstScrollPane);
      lstSSList.setFocusTarget(lstSSList);
    }

    JScrollPane lstScrollPane2 = null;
    if (activeComps.contains(LIST2)) {
      lstScrollPane2 = new JScrollPane(lstSSList2);
      lstScrollPane2.setPreferredSize(MainClass.ssDimTall);
      lstSSList2.setDecorateTarget(lstScrollPane2);
      lstSSList2.setFocusTarget(lstSSList2);
    }
    // Disable the primary key so the user can't change it.
    txtTestPK.setEnabled(false);

    // Setup the container and layout the components.
    JPanel mainPane = new JPanel(new GridBagLayout());

    // Add the components, there's a special case with the list scroll panes.
    buildGui_add(mainPane, lstScrollPane, lstScrollPane2);
    add(mainPane);
    add(navigator, BorderLayout.SOUTH);

    pack();

    // Make sure the window is not resized smaller.
    // If that happened, info would be obscured.

    // Whether or not setMinimumSize() actually caps the size during drag,
    // is platform dependent. This listener caps the size.
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        int w = getWidth();
        int h = getHeight();

        if (w < minWidth || h < minHeight) {
          // Force the width back to minimum, keep current height
          int maxW = Math.max(minWidth, w);
          int maxH = Math.max(minHeight, h);
          setSize(maxW, maxH);
          System.err.printf("W %d, H %d, getW %d, getH %d\n", maxW, maxH, getWidth(), getHeight());
        }
      }
    });

    minWidth = getWidth();
    minHeight = getHeight();
    setMinimumSize(new Dimension(minWidth, minHeight));

    // Make the JFrame visible.
    setVisible(true);
  }
  int minWidth;
  int minHeight;

  /**
   * Add styles used by this test; doesn't return until complete.
   */
  @SuppressWarnings("CallToPrintStackTrace")
  private void setupOurTextStyles() throws IOException {
    if (TextStyles.getStyle("testComponents_componentStateError") != null)
      return;
    StringReader reader = new StringReader("""
        {
          "testComponents_componentStateError": {
            "fontSize": 14,
            "italic": false,
            "strikethrough": true
          },
          "testComponents_componentStateModified": {
            "fontSize": "default",
            "italic": true,
            "strikethrough": false
          }
        }
        """);
    TextStyles.loadFromAnyThread(() -> TextStyles.loadStylesFromJson(reader, "TestBaseComponents"));
  }

  private DbOps createDbNav() {
    return new TestBaseComponentsDbOps();
  }

  /**
   * Various navigator overrides needed to support H2
   * <p>
   * H2 does not fully support updatable rowset so it must be
   * re-queried following insert and delete with rowset.execute()
   */
  private class TestBaseComponentsDbOps extends DbOpsBase {
    public TestBaseComponentsDbOps() {
      super(TestBaseComponents.this);
    }
    /**
     * Requery the rowset following a deletion. This is needed for H2.
     */
    @Override
    public void performPostDeletionOps(RowsModel rm) throws SQLException {
      super.performPostDeletionOps(rm);
      performRefreshOps();
    }

    /**
     * Requery the rowset following an insertion. This is needed for H2.
     */
    @Override
    public void performPostInsertOps(RowsModel rm) throws SQLException {
      super.performPostInsertOps(rm);
      //TestBaseComponents.this.cmbSSDBComboNav.setEnabled(true);
      performRefreshOps();
    }

    /**
     * Obtain and set the PK value for the new record & perform any other actions needed before an insert.
     */
    @Override
    public void performPreInsertOps() {
      //
      // WHERE IS THE PRIMARY KEY SET? See example1
      //

      // super clears the component values
      super.performPreInsertOps();

      setDefaultValues();
    }

    /**
     * Manage sync manager during a Refresh
     */
    @Override
    public void performRefreshOps() {
      super.performRefreshOps();
      if (syncManager == null)
        return;

      syncManager.async();
      try {
        cmbSSDBComboNav.execute();
      } catch (final SQLException se) {
        logger.log(Level.ERROR, "SQL Exception.", se);
      } catch (final Exception e) {
        logger.log(Level.ERROR, "Exception.", e);
      }
      syncManager.sync();
    }

    /**
     * Re-enable DB Navigator following insertion Cancel
     */
    @Override
    public void performCancelOps() {
      super.performCancelOps();
      if (cmbSSDBComboNav == null)
        return;

      cmbSSDBComboNav.setEnabled(true);
    }
  }

  private String getColumnsSQL() {
    String s = getActiveCompInfo()
                   .stream()
                   .filter((comp) -> comp.col != null)
                   .map((comp) -> comp.col)
                   .distinct()
                   .collect(Collectors.joining(", "));
    return s;
  }

  /** For enabled components, return list of descriptive records. */
  private Collection<CompID> getActiveComps() {
    return activeComps;
  }

  /** For enabled components, return list of records. */
  private List<CompInfo> getActiveCompInfo() {
    return getActiveComps().stream().map((eComp) -> compInfos.get(eComp)).toList();
  }

  private void buildGui_bind() {
    for (CompInfo comp : getActiveCompInfo()) {
      if (comp.col != null)
        rowsModel.bind((SSComponent) comp.comp, comp.col);
    }
  }

  private void buildGui_dim() {
    for (CompID compID : getActiveComps()) {
      CompInfo compInfo = compInfos.get(compID);

      Dimension targetDim = new Dimension(switch (compInfo.dim) {
        case H1 -> MainClass.ssDim;
        case H2 -> MainClass.ssDimTall;
        case H3 -> MainClass.ssDimVeryTall;
      });
      JComponent jc = compInfo.comp;
      if (keepMinHeight.contains(compID)) {
        int curHeight = jc.getPreferredSize().height;
        if (curHeight > targetDim.height)
          targetDim.height = curHeight;
      }
      jc.setPreferredSize(targetDim);
    }
  }

  private void buildGui_add(JComponent mainPane, JScrollPane jspList, JScrollPane jspList2) {
    GridBagConstraints gridPos = new GridBagConstraints();
    gridPos.gridx = 0;
    gridPos.gridy = 0;

    for (CompInfo compInfo : getActiveCompInfo()) {
      gridPos.gridx = 0;
      mainPane.add(compInfo.label, createConstraints(gridPos, null));

      gridPos.gridx = 1;
      GridBagConstraints extra = null;

      // Some special handling according to component
      JComponent jComp = compInfo.comp;
      jComp = switch (compInfo.compID) {
        case LIST -> jspList; // add the scrollPane
        case LIST2 -> jspList2; // add the scrollPane
        case IMAGE -> {
          extra = new GridBagConstraints();
          extra.fill = GridBagConstraints.BOTH;
          extra.weighty = 1.0;
          yield jComp;
        }
        default -> jComp;
      };
      mainPane.add(jComp, createConstraints(gridPos, extra));
      gridPos.gridy++;
    }
  }
  private static final Insets noInsets = new Insets(0, 0, 0, 0);
  private GridBagConstraints createConstraints(GridBagConstraints pos, GridBagConstraints extra) {
    // starting with extra, add in the pos,
    // then specialize according to left/right column.
    GridBagConstraints constraints
        = extra != null ? (GridBagConstraints) extra.clone() : new GridBagConstraints();
    constraints.gridx = pos.gridx;
    constraints.gridy = pos.gridy;
    if (pos.gridx == 0) {
      // label
      constraints.anchor = GridBagConstraints.FIRST_LINE_END;
      if (Objects.equals(noInsets, constraints.insets))
        constraints.insets = new Insets(0, 5, 0, 10);
      if (constraints.ipadx == 0)
        constraints.ipadx = 5;
    }
    if (pos.gridx == 1) {
      // ssComponent
      constraints.anchor = GridBagConstraints.LINE_START;
      constraints.insets = new Insets(0, 5, 0, 10);
      if (constraints.weightx == 0.0)
        constraints.weightx = .1;
    }
    return constraints;
  }

  /**
   * Method to set default values following an insert
   */
  public void setDefaultValues() {
    // Get the new record id.
    try (ResultSet rs
         = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)
               .executeQuery("SELECT nextval('swingset_base_test_seq') as nextVal;")) {
      rs.next();
      final int recordPK = rs.getInt("nextVal");
      txtTestPK.setText(String.valueOf(recordPK));
    } catch (final SQLException se) {
      logger.log(Level.ERROR, "SQL Exception occured during setting default values.", se);
    } catch (final Exception e) {
      logger.log(Level.ERROR, "Exception occured during setting default values.", e);
    }

    // SET OTHER DEFAULTS
    //		chkSSCheckBox.setSelected(false);
    //		cmbSSComboBox.setSelectedIndex(-1);
    //		cmbEnumSSComboBox.setSelectedIndex(-1);
    //		cmbSSDBComboBox.setSelectedIndex(-1);
    //		imgImage.clearImage();
    //		lblSSLabel2.setText(null);
    //		lstSSList.clearSelection();
    // TODO determine range for slider, 0 was not accepted
    //		sliSSSlider.setValue(1);
    //		txtSSTextArea.setText(null);
    //		txtSSTextField.setText(null);
  }
}
