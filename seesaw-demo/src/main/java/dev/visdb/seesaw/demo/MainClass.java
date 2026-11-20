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
 * copyright (C) 2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package dev.visdb.seesaw.demo;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.rowset.spi.SyncFactory;
import javax.sql.rowset.spi.SyncFactoryException;
import javax.sql.rowset.spi.SyncProvider;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;

import com.raelity.lib.ui.Screens;

import dev.visdb.seesaw.SeeSaw;
import dev.visdb.seesaw.SsTextField;
import dev.visdb.seesaw.contrib.simplevalidation.SVUtils;
import dev.visdb.seesaw.datasources.DbSupport;
import dev.visdb.seesaw.datasources.RowSetOps.ForceConflict;
import dev.visdb.seesaw.datasources.products.DbSupportBase;
import dev.visdb.seesaw.datasources.products.DbSupportFactory;
import dev.visdb.seesaw.decorators.Decorator;
import dev.visdb.seesaw.html.SsHtml;
import dev.visdb.seesaw.navigate.Utils;
import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsUtils;
import dev.visdb.seesaw.utils.Version;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import static dev.visdb.seesaw.demo.DemoUtil.configureJavaUtilLogger;
import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * A JFrame with buttons to launch each of the SeeSaw example/demo screens.
 */
@SuppressWarnings("serial")
public class MainClass extends JFrame {
  /**
   * database
   */
  public static final String DATABASE_NAME = "suppliers_and_parts";

  static final Map<String, Object> globalHints = new HashMap<>();

  static {
    if (Boolean.FALSE) {
      // Get rid of unused warnings
      Objects.nonNull(new SsUtils.DebugRowSetListenerFlag());
      Objects.nonNull(new ForceConflict(0));
      Objects.nonNull(new H2Trace(""));
      Objects.nonNull(new MainClass().new LogManListener());
    }
  }

  static interface LoadDemoImages {}
  // https://h2database.com/html/features.html#trace_options
  // Globals.setOption(H2Trace.class, new H2Trace(";TRACE_LEVEL_SYSTEM_OUT=3"));
  static class H2Trace {
    private final String flags;
    H2Trace() {
      this.flags = "";
    }
    H2Trace(String flags) {
      this.flags = flags;
    }
    String getTraceUrlFlags() {
      return flags;
    }
  }
  @SuppressWarnings("ClassMayBeInterface")
  static class H2Workaround {}

  /**
   * ActionListener implementation to call code for each button.
   */
  private class MyButtonListener implements ActionListener {
    public MyButtonListener() {
      super();
    }

    @SuppressWarnings({"unused", "ResultOfObjectAllocationIgnored"})
    @Override
    public void actionPerformed(ActionEvent ae) {
      final Map<String, Object> hints = new HashMap<>(globalHints);
      Utils.dumpLatestEvents("New Window:");

      Object source = ae.getSource();

      if (source == btnExample1) {
        logger.log(DEBUG, "**** Opening Example1 ****");
        new Example1(dbConnection);
      } else if (source == btnExample2) {
        logger.log(DEBUG, "**** Opening Example2 ****");
        new Example2(dbConnection);
      } else if (source == btnExample3) {
        logger.log(DEBUG, "**** Opening Example3 ****");
        new Example3(dbConnection);
      } else if (source == btnExample4) {
        logger.log(DEBUG, "**** Opening Example4 ****");
        new Example4(dbConnection);
      } else if (source == btnExample4Advanced) {
        logger.log(DEBUG, "**** Opening Example4Advanced ****");
        new Example4Advanced(dbConnection);
        // } else if (source == btnExample4UsingHelper) {
        //   logger.log(DEBUG, "**** Opening Example4UsingHelper ****");
        //   JFrame e4JFrame = new JFrame("Example4 Using Helper");
        //   e4JFrame.setLocation(DemoUtil.getChildScreenLocation("Example4UsingHelper"));
        //   Example4UsingHelper example4 = new Example4UsingHelper(dbConnection, e4JFrame);
        //   e4JFrame.add(example4);
        //   example4.showUp(e4JFrame);
        //   e4JFrame.pack();
        //   // screen dimensions handled by SSScreenHelperCommon.setScreenSize()
        //   e4JFrame.setVisible(true);
      } else if (source == btnExample5) {
        logger.log(DEBUG, "**** Opening Example5 ****");
        new Example5(dbConnection);
      } else if (source == btnExample6) {
        logger.log(DEBUG, "**** Opening Example6 ****");
        new Example6(dbConnection);
      } else if (source == btnExample7) {
        logger.log(DEBUG, "**** Opening Example7 ****");
        new Example7(dbConnection);
        // } else if (source == btnExample7UsingHelper) {
        //   logger.log(DEBUG, "**** Opening Example7UsingHelper ****");
        //   JFrame e7JFrame = new JFrame("Example7 Using Helper");
        //   Example7UsingHelper example7 = new Example7UsingHelper(dbConnection, null);
        //   e7JFrame.add(example7);
        //   e7JFrame.setLocation(DemoUtil.getChildScreenLocation("Example7UsingHelper"));
        //   example7.showUp(e7JFrame);
        //   //e7JFrame.pack(); // NOT USING .pack() FOR DATA GRID SCREENS
        //   e7JFrame.setSize(MainClass.childScreenWidth, MainClass.childScreenHeight);
        //   e7JFrame.setVisible(true);
      } else if (source == btnTestBase) {
        logger.log(DEBUG, "**** Opening TestBaseComponents ****");
        new TestBaseComponents(dbConnection, hints);
      } else if (source == btnTestGrid) {
        logger.log(DEBUG, "**** TestGridComponents not implemented ****");
        // TODO
        // new TestGridComponents(dbConnection);
      } else if (source == btnTestFormatted) {
        logger.log(DEBUG, "**** Opening TestFormattedComponents ****");
        new TestFormattedComponents(dbConnection);
      }
    }
  }

  // Use a plain combobox to avoid using SS in main demo menu window
  //private class ComboRowSetSource extends SSComboBox {
  @SuppressWarnings("serial")
  private final class ComboRowSetSource extends JComboBox<DemoUtil.RowSetSource> {
    Popup popup;

    public ComboRowSetSource() {
      //super(ModelType.SWING);
      super(DemoUtil.RowSetSource.values());
      setMaximumSize(new Dimension(210, 25));
      //setAllowNull(false);
      DemoUtil.RowSetSource rsSource = DemoUtil.getWhichRowSetDefault();
      //setDisplayValues(DemoUtil.RowSetSource.class);
      //setChosenEnum(rsSource);
      setSelectedItem(rsSource);
      btnRowSetSource.setText("RowSet: " + rsSource.toString());
    }

    //@Override
    public DemoUtil.RowSetSource getChosenEnum() {
      // return (DemoUtil.RowSetSource) super.getChosenEnum();
      return (DemoUtil.RowSetSource) getSelectedItem();
    }

    @Override
    public void setSelectedItem(Object anObject) {
      super.setSelectedItem(anObject);
      DemoUtil.RowSetSource rsSource = getChosenEnum();
      DemoUtil.setWhichRowSetDefault(rsSource);
      btnRowSetSource.setText("RowSet: " + rsSource.toString());
      if (popup == null) {
        return;
      }
      popup.hide();
      popup = null;
    }
  }

  private class RowSetSourceMouseListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      if (comboRowSetSource.popup != null) {
        comboRowSetSource.popup.hide();
        comboRowSetSource.popup = null;
        return;
      }
      Point p = e.getLocationOnScreen();
      Popup popup = PopupFactory.getSharedInstance().getPopup(null, comboRowSetSource, p.x, p.y);
      comboRowSetSource.popup = popup;
      popup.show();
      SwingUtilities.invokeLater(() -> comboRowSetSource.showPopup());
    }
  }

  private class LogManListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {}
  }

  /** component dimension */
  protected static final int buttonHeight = 25;
  /** component dimension */
  protected static final int buttonWidth = 250;
  /** component dimension */
  protected static final Dimension buttonDim = new Dimension(buttonWidth, buttonHeight);

  /** component dimension */
  protected static final int gridColumnWidth = 60;

  private static final int labelHeight = 20;
  private static final int labelHeightTall = 100; // used for lists, textareas
  private static final int labelHeightVeryTall = 100; // used for images
  private static final int labelWidth = 200;

  /** component dimension default*/
  protected static final Dimension labelDim = new Dimension(labelWidth, labelHeight);
  /** component dimension for lists, text areas */
  protected static final Dimension labelDimTall = new Dimension(labelWidth, labelHeightTall);
  /** component dimension for images */
  protected static final Dimension labelDimVeryTall
      = new Dimension(labelWidth, labelHeightVeryTall);

  private static final int ssHeight = 20;
  private static final int ssHeightTall = 100; // used for lists, textareas
  private static final int ssHeightVeryTall = 200; // used for images
  private static final int ssWidth = 240;

  /** component dimension default*/
  protected static final Dimension ssDim = new Dimension(ssWidth, ssHeight);
  /** component dimension for lists, text areas */
  protected static final Dimension ssDimTall = new Dimension(ssWidth, ssHeightTall);
  /** component dimension for images */
  protected static final Dimension ssDimVeryTall = new Dimension(ssWidth, ssHeightVeryTall);

  /** component dimension */
  protected static final int childScreenHeight = 400;
  /** component dimension */
  protected static final int childScreenHeightTall = 800;
  /** component dimension */
  protected static final int childScreenWidth = 600;
  /** component dimension */
  protected static final int childScreenCount = 10;

  private Connection dbConnection = null;

  /**
   * buttons to launch code examples
   */
  private JButton btnExample1 = new JButton("Example1");
  private JButton btnExample2 = new JButton("Example2");
  private JButton btnExample3 = new JButton("Example3");
  private JButton btnExample4 = new JButton("Example4");
  private JButton btnExample4Advanced = new JButton("Example4 Advanced");
  // private JButton btnExample4UsingHelper = new JButton("Example4 Using Helper");
  private JButton btnExample5 = new JButton("Example5");
  private JButton btnExample6 = new JButton("Example6");
  private JButton btnExample7 = new JButton("Example7");
  // private JButton btnExample7UsingHelper = new JButton("Example7 Using Helper");
  private JButton btnTestBase = new JButton("Test Base Components");
  private JButton btnTestFormatted = new JButton("Test Formatted Components");
  private JButton btnTestGrid = new JButton("Test Grid Components");
  private JButton btnRowSetSource = new JButton("RowSet: XXXXXXXX");
  private ComboRowSetSource comboRowSetSource = new ComboRowSetSource();
  private JButton btnLogMan = new JButton("Manage Logging");

  /** Logger */
  private static final Logger logger = JStuff.getLogger();

  static DatabaseMetaData dbMeta;

  /**
   * Constructor for MainClass
   */
  @SuppressWarnings({"LeakingThisInConstructor", "OverridableMethodCallInConstructor", "UseOfSystemOutOrSystemErr"})
  public MainClass() {
    // SETUP WINDOW
    super(frameTitle());
    setSize(300, 300);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        DemoUtil.logConnectionUsage();
      }
    });

    // ECHO WORKING DIRECTORY
    logger.log(INFO, () -> sf("Working Directory = %s", System.getProperty("user.dir")));

    // INITIALIZE DATABASE
    databaseSetup.run();
    dbConnection = databaseSetup.getConnection();
    if (dbConnection == null) {
      logger.log(Level.ERROR, "Error initializing database. Exiting.");
      System.exit(1);
    }

    String databaseID = "";
    try {
      dbMeta = dbConnection.getMetaData();
      databaseID = dbMeta.getDatabaseProductName() + " " + dbMeta.getDatabaseProductVersion();
      System.err.printf(
          "Database product '%s', version '%s', MajMin '%s'\n", dbMeta.getDatabaseProductName(),
          dbMeta.getDatabaseProductVersion(),
          "" + dbMeta.getDatabaseMajorVersion() + ":" + dbMeta.getDatabaseMinorVersion());
    } catch (SQLException ex) {
    }

    DbSupport supp = DbSupportFactory.setDbSupportGlobalOption(dbConnection);
    if (supp == null) {
      logger.log(Level.ERROR, sf("No SSDBSupport found for '%s'", databaseID));
      Globals.setOption(DbSupport.class, new DbSupportBase(dbConnection) {});
    }

    // ADD ACTION LISTENERS FOR BUTTONS
    // TODO: can share listener OR add arg that can be switched on.
    btnExample1.addActionListener(new MyButtonListener());
    btnExample2.addActionListener(new MyButtonListener());
    btnExample3.addActionListener(new MyButtonListener());
    btnExample4.addActionListener(new MyButtonListener());
    btnExample4Advanced.addActionListener(new MyButtonListener());
    //btnExample4UsingHelper.addActionListener(new MyButtonListener());
    btnExample5.addActionListener(new MyButtonListener());
    btnExample6.addActionListener(new MyButtonListener());
    btnExample7.addActionListener(new MyButtonListener());
    //btnExample7UsingHelper.addActionListener(new MyButtonListener());
    btnTestBase.addActionListener(new MyButtonListener());
    btnTestGrid.addActionListener(new MyButtonListener());
    btnTestFormatted.addActionListener(new MyButtonListener());
    btnRowSetSource.addMouseListener(new RowSetSourceMouseListener());
    //btnLogMan.addActionListener(new LogManListener());

    // SET BUTTON DIMENSIONS
    btnExample1.setPreferredSize(buttonDim);
    btnExample2.setPreferredSize(buttonDim);
    btnExample3.setPreferredSize(buttonDim);
    btnExample4.setPreferredSize(buttonDim);
    btnExample4Advanced.setPreferredSize(buttonDim);
    //btnExample4UsingHelper.setPreferredSize(buttonDim);
    btnExample5.setPreferredSize(buttonDim);
    btnExample6.setPreferredSize(buttonDim);
    btnExample7.setPreferredSize(buttonDim);
    //btnExample7UsingHelper.setPreferredSize(buttonDim);
    btnTestBase.setPreferredSize(buttonDim);
    btnTestGrid.setPreferredSize(buttonDim);
    btnTestFormatted.setPreferredSize(buttonDim);
    btnRowSetSource.setPreferredSize(buttonDim);
    btnLogMan.setPreferredSize(buttonDim);

    // LAYOUT BUTTONS
    getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    getContentPane().add(btnExample1);
    getContentPane().add(btnExample2);
    getContentPane().add(btnExample3);
    getContentPane().add(btnExample4);
    getContentPane().add(btnExample4Advanced);
    //getContentPane().add(btnExample4UsingHelper);
    getContentPane().add(btnExample5);
    getContentPane().add(btnExample6);
    getContentPane().add(btnExample7);
    //getContentPane().add(btnExample7UsingHelper);
    getContentPane().add(btnTestBase);
    // getContentPane().add(this.btnTestGrid);
    getContentPane().add(btnTestFormatted);
    getContentPane().add(btnRowSetSource);
    if (DemoUtil.isUtilLogging()) {
      btnLogMan.setAction(DemoUtil.getLogManAction());
      getContentPane().add(btnLogMan);
    }

    // DISPLAY SCREEN
    setVisible(true);
    pack();
    Screens.translateToPrefScreen(this);
  }

  /*
   * defaults for optional command line arguments
   */
  private static boolean verbose = false;
  private static boolean dump = false;
  private static boolean no_load_images = false;
  private static boolean no_initialize_db = false;
  // private static boolean readme = false;
  private static String dbname = DbServer.H2.cmdName();
  private static String propertyFile = null;
  private static List<String> userSqlFiles = new ArrayList<>();
  /** The dataset to load; see DemoDataset. "" is the files at the top of the resources. */
  private static String dataset = DemoDataset.UNNAMED;
  private static boolean datasetList = false;
  private static boolean datasetCheck = false;
  /**
   * How database text that may be HTML is shown; a name from DemoRules.
   * Null: the library's defaults, by component type. Otherwise the SsHtml
   * is set in Globals, which overrides the default for every component.
   */
  private static String rules = null;
  private static boolean rulesList = false;

  private static DemoDatabase.DatabaseSetup databaseSetup = null;

  /* long option ids, they are not characters */
  private static final int OPT_STYLE = 1000;
  private static final int OPT_DATASET = 1001;
  private static final int OPT_DATASET_LIST = 1002;
  private static final int OPT_DATASET_CHECK = 1003;
  private static final int OPT_RULES = 1004;
  private static final int OPT_RULES_LIST = 1005;

  private static String cmdName = "SeeSawDemo";

  @SuppressWarnings({"ResultOfMethodCallIgnored", "UseOfSystemOutOrSystemErr"})
  private static void usage() {
    // TODO: specify don't load images
    String usage = """

        Run the SeeSaw demo. With no options/args use the self contained
        in memory database.
        
        CMD_NAME [-h] [-v] [-d] [-n] [-i] [-r] [-p fname] [-s sql]* [dbms-server]
        
            -h             help
            --style=[SV|BG|BORDER]         BORDER is the default
                           SeeSaw gives visual indication of input field state;
                           there are three decorator styles currently supported.
                           SV is the SimpleValidation framework.
            --dataset=name the dataset of sql scripts to load; the default is
                           the unnamed dataset, the files at the top of the
                           resources. Files a dataset lacks come from there.
            --dataset-list list the datasets with their descriptions, exit
            --dataset-check
                           for each dataset, list the files it has and the
                           files that come from the unnamed dataset, exit
            --rules=name   how database text that may be html is shown, for
                           every component; without it the library's
                           defaults, by component type, are used. PASS shows
                           all html as Swing does, which may fetch URLs.
                           See DemoRules.
            --rules-list   list the names for --rules, exit
            -v             verbose; output initialization sql as executed
            -d             dump/create sql scripts in local directory, exit
            -n             do NOT initialize database, just run demo
            -i             do NOT load images
            -p fname       properties file for jdbc database connection
                           'DB_URL', 'DB_DRIVER_CLASS' keys required
            -s sqlScript   sql file to initialize database, multiple OK
        
        If specified, dbms-server in {mysql}
        Internal mysql properties use database demo_suppliers_and_parts.
        After the sql files are run, the images are loaded, unless '-i'.
        Use '-n -p props' to run demo with a previously initialized database.
        Use '-d' or '-d mysql' to create local files with sql initialization.
        '--dataset' can not be used with '-s'.
        See seesaw-demo/README.txt for more information.
        
        Examples: SeeSawDemo is typically run with a maven command
            mvn exec:exec -Dargs="-d"   # dump sql that creates in memory database
            mvn exec:exec -Dargs="-d mysql"   # dump sql to create mysql database
            mvn exec:exec -Dxcp=":jdbc_driver" -Dargs="-p db_props -s initializer.sql"
        
        """;
    usage = usage.replace("CMD_NAME", cmdName);
    System.err.println(usage);
    System.exit(1);
  }

  private static String frameTitle() {
    DemoDataset ds = databaseSetup == null ? null : databaseSetup.options.dataset();
    return ds == null || ds.isUnnamed() ? "SeeSaw Demo" : "SeeSaw Demo [" + ds.name() + "]";
  }

  /** For --dataset-list. */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  private static void printDatasets() {
    for (DemoDataset ds : DemoDataset.available()) {
      System.out.printf("%-20s %s\n", ds.isUnnamed() ? "\"\"" : ds.name(), ds.description());
    }
  }

  /** For --dataset-check. */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  private static void checkDatasets() {
    for (DemoDataset ds : DemoDataset.available()) {
      if (ds.isUnnamed())
        continue;
      System.out.printf("%s: %s\n", ds.name(), ds.description());
      for (DbServer server : DbServer.values()) {
        List<String> own = new ArrayList<>();
        List<String> fallback = new ArrayList<>();
        for (DemoScript script : DemoScript.values()) {
          String fileName = script.fileName(server);
          if (fileName == null)
            continue;
          (ds.hasOwn(script, server) ? own : fallback).add(fileName);
        }
        System.out.printf("    %-6s own:      %s\n", server.cmdName(), String.join(" ", own));
        System.out.printf("    %-6s fallback: %s\n", server.cmdName(), String.join(" ", fallback));
      }
    }
  }

  /**
   * Report the files that a named dataset lacks, which come from the unnamed
   * dataset. They are logged, and also shown in a dialog, since a log line
   * is easy to miss; with -d, or without a display, they go to stderr.
   * @return false if the user canceled
   */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  private static boolean confirmFallbacks(DemoDataset ds, DbServer server,
                                          List<String> fallbacks, boolean dump) {
    if (ds.isUnnamed() || fallbacks.isEmpty())
      return true;
    for (String fileName : fallbacks) {
      logger.log(WARNING, () -> sf("Dataset %s has no '%s' for %s; using the unnamed dataset's",
                                   ds.name(), fileName, server.cmdName()));
    }
    StringBuilder sb = new StringBuilder();
    sb.append(sf("Dataset '%s' does not have these %s files;\n", ds.name(), server.cmdName()))
        .append("the files of the unnamed dataset are used instead:\n\n");
    for (String fileName : fallbacks) {
      sb.append("    ").append(fileName).append('\n');
    }
    if (dump || GraphicsEnvironment.isHeadless()) {
      System.err.println(sb);
      return true;
    }
    sb.append("\nOK to continue, Cancel to exit.");
    int[] answer = new int[1];
    try {
      SwingUtilities.invokeAndWait(() -> answer[0] = JOptionPane.showConfirmDialog(
          null, sb.toString(), "SeeSaw Demo: dataset " + ds.name(),
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE));
    } catch (InterruptedException | InvocationTargetException ex) {
      logger.log(Level.ERROR, "Dataset fallback dialog", ex);
      return true;
    }
    return answer[0] == JOptionPane.OK_OPTION;
  }

  private static void setDecoratorStyle(Decorator.DecoratorStyle style) {
    Globals.setOption(Decorator.DecoratorStyle.class, style);
  }

  /**
   * Main method for SeeSaw samples/demo
   * <p>
   *
   * @param _args - optional command line arguments, which are ignored by this
   *              program
   */
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public static void main(String[] _args) {
    configureJavaUtilLogger();
    Screens.setPrefScreen("SEESAW_PREFERRED_SCREEN");

    Globals.setOption(LoadDemoImages.class, new LoadDemoImages(){});
    Globals.setOption(H2Trace.class, new H2Trace());

    // To allow forcing baseValidate() and componentValidate() errors.
    if(Boolean.FALSE) Globals.setOption(SsTextField.DebugBaseComponentValidate.class,
                                        new SsTextField.DebugBaseComponentValidate() {});
    // disable simplevalidation border
    if(Boolean.FALSE) Globals.setOption(SVUtils.SimpleValidationBorderEnable.class,
                                        new SVUtils.SimpleValidationBorderEnable(false));

    // Override the library's html defaults for every component; null keeps them.
    // After options processed: Globals.setOption(SsHtml.class, DemoRules.get(rules)),
    // html handling for demo:  "PASS", "PLAIN", "INLINE", "MULTILINE",
    //                          "BROAD", "IMG", "INLINE-TEXT"
    if(Boolean.FALSE) rules = "MULTILINE";
    // Specify which dataset to use for this run. Points to scripts to load database.
    // html data: "", "html-inline", "html-multiline", "html-restricted"
    if(Boolean.FALSE) dataset = "html-inline";

    //
    // Pick decorator style, BORDER is default
    // May be overriden by command line option: --style=[SV|BG|BORDER]
    //
    // setDecoratorStyle(SVUtils.SIMPLE_VALIDATION);
    // setDecoratorStyle(Decorator.DecoratorStyle.BACKGROUND);

    // Globals.setOption(H2Trace.class, new H2Trace(";TRACE_LEVEL_SYSTEM_OUT=3"));
    // Globals.setOption(new SsUtils.DebugRowSetListenerFlag());
    //
    // SELECT VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME = 'info.VERSION';
    // fixed in H2 Version 2.3.230 (2024-07-15
    // Globals.setOption(H2Workaround.class, new H2Workaround());

    // For debug, enable this to force CachedRowSet conflict
    //Globals.setOption(new ForceConflict(1));

    SeeSaw.init();

    boolean some_error = false;
    System.err.printf("java:%s vm:%s date:%s os:%s\n", System.getProperty("java.version"),
                      System.getProperty("java.vm.version"),
                      System.getProperty("java.version.date"), System.getProperty("os.name"));
    System.err.printf("SeeSaw: %s\n", Version.get().toString());

    try {
      for (SyncProvider sp : Collections.list(SyncFactory.getRegisteredProviders())) {
        Class<?> c = sp.getClass();
        System.err.printf("SyncProvider: %s\n", c.getName());
        for (Class<?> iface : c.getInterfaces()) {
          System.err.println("    " + iface.getName());
        }
      }
    } catch (SyncFactoryException ex) {
      logger.log(DEBUG, "SyncFactory.getRegisteredProviders()", ex);
    }


    // longopts.add(new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'));
    // longopts.add(new LongOpt("outputdir", LongOpt.REQUIRED_ARGUMENT, sb, 'o'));
    // longopts.add(new LongOpt("maximum", LongOpt.OPTIONAL_ARGUMENT, null, 2));

    ArrayList<LongOpt> longopts = new ArrayList<>();
    longopts.add(new LongOpt("style", LongOpt.REQUIRED_ARGUMENT, null, OPT_STYLE));
    longopts.add(new LongOpt("dataset", LongOpt.REQUIRED_ARGUMENT, null, OPT_DATASET));
    longopts.add(new LongOpt("dataset-list", LongOpt.NO_ARGUMENT, null, OPT_DATASET_LIST));
    longopts.add(new LongOpt("dataset-check", LongOpt.NO_ARGUMENT, null, OPT_DATASET_CHECK));
    longopts.add(new LongOpt("rules", LongOpt.REQUIRED_ARGUMENT, null, OPT_RULES));
    longopts.add(new LongOpt("rules-list", LongOpt.NO_ARGUMENT, null, OPT_RULES_LIST));
    Getopt g = new Getopt(cmdName, _args, "hvdinrp:s:", longopts.toArray(LongOpt[]::new));

    String decoStyleString = null;
    boolean datasetSpecified = false;
    Decorator.DecoratorStyle decoStyle = null;
    int c;
    while ((c = g.getopt()) != -1) {
      switch (c) {
        case OPT_STYLE: decoStyleString = g.getOptarg(); break;
        case OPT_DATASET: dataset = g.getOptarg(); datasetSpecified = true; break;
        case OPT_DATASET_LIST: datasetList = true; break;
        case OPT_DATASET_CHECK: datasetCheck = true; break;
        case OPT_RULES: rules = g.getOptarg(); break;
        case OPT_RULES_LIST: rulesList = true; break;
        case 'v': verbose = true; break;
        case 'd': dump = true; break;
        case 'n': no_initialize_db = true; break;
        case 'i': no_load_images = true; break;
        // case 'r': readme = true; break;
        case 'p': propertyFile = g.getOptarg(); break;
        case 's': userSqlFiles.add(g.getOptarg()); break;
        case '?':
        case 'h':
        default: // TODO: not sure if default is OK here meaning all other characters indicate an
          // error??
          some_error = true;
          break; // getopt() already printed an error
      }
      if (decoStyleString != null) {
        decoStyle = switch(decoStyleString) {
          case "BORDER" -> Decorator.DecoratorStyle.BORDER;
          case "BG" -> Decorator.DecoratorStyle.BACKGROUND;
          case "SV" -> SVUtils.SIMPLE_VALIDATION;
          default -> {
            System.err.printf("Unknown style option value '%s'\n", decoStyleString);
            yield null;
          }
        };
      }
    }
    // if (longo != null)
    //   System.err.printf("*** longopt: '%s'\n", longo);

    if (decoStyle != null)
      setDecoratorStyle(decoStyle);

    if (verbose) {
      StringBuilder sb = new StringBuilder();
      sb.setLength(0);
      sb.append('\n').append(cmdName);
      for (String arg : _args) {
        sb.append(' ').append(arg);
      }
      System.err.println(sb.toString());
    }

    if (some_error) {
      usage();
    }

    if (datasetList) {
      printDatasets();
      System.exit(0);
    }
    if (datasetCheck) {
      checkDatasets();
      System.exit(0);
    }
    if (rulesList) {
      System.out.println(String.join(" ", DemoRules.names()));
      System.exit(0);
    }

    // if (readme) {
    // return;
    // }

    boolean databaseServerSpecified = false;

    int i = g.getOptind();
    if (i != _args.length) {
      // There are positional arguemnts. Can only be one.
      if (i + 1 != _args.length) {
        usage();
      }
      dbname = _args[i];
      databaseServerSpecified = true;
    }

    boolean useInternalSql = userSqlFiles.isEmpty();

    if (!useInternalSql) {
      if (propertyFile == null) {
        System.err.println("-p required if -s is used. Exiting.");
        some_error = true;
      } else {
        // there's both a property file and sql files
        if (databaseServerSpecified) {
          System.err.println("-s not allowed if database specified. Exiting.");
          some_error = true;
        }
      }
      if (dump) {
        System.err.println("-d not allowed when -s is used. Exiting.");
        some_error = true;
      }
      if (datasetSpecified && !dataset.isEmpty()) {
        System.err.println("--dataset not allowed when -s is used. Exiting.");
        some_error = true;
      }
    }

    DemoDataset demoDataset = null;
    try {
      demoDataset = DemoDataset.of(dataset);
    } catch (IllegalArgumentException ex) {
      System.err.printf("%s. Known datasets: %s. Exiting.\n", ex.getMessage(),
                        DemoDataset.available().stream().filter(d -> !d.isUnnamed())
                            .map(DemoDataset::name).toList());
      some_error = true;
    }

    SsHtml ssHtml = null;
    try {
      if (rules != null)
        ssHtml = DemoRules.get(rules);
    } catch (IllegalArgumentException ex) {
      System.err.println(ex.getMessage() + ". Exiting.");
      some_error = true;
    }

    DbServer server = null;
    if (useInternalSql) {
      server = DbServer.forName(dbname).orElse(null);
      if (server == null) {
        System.err.println("Unknown database server '" + dbname + "' . Exiting.");
        some_error = true;
      }
    }

    if (some_error) {
      System.exit(1);
    }
    assert demoDataset != null;

    if (ssHtml == null) {
      System.err.println("Rules: the library's defaults, by component type");
    } else {
      // An override of the library's defaults, for every component.
      Globals.setOption(SsHtml.class, ssHtml);
      System.err.printf("Rules: %s, SsHtml: %s, for every component\n",
                        rules, ssHtml.getName());
    }
    if (SsHtml.pass().equals(ssHtml)) {
      System.err.println("""
          WARNING: --rules=PASS shows database html as Swing does. Html from the
          html-restricted dataset fetches URLs, and two of its records make
          Swing throw or overflow the stack.""");
    }

    DemoOptions options = new DemoOptions(verbose, dump, no_load_images, no_initialize_db,
                                          propertyFile, userSqlFiles, demoDataset);
    databaseSetup = server == null ? new DemoDatabase.ExternalSetup(options)
                    : switch (server) {
                      case H2 -> new DemoDatabase.H2Setup(options);
                      case MYSQL -> new DemoDatabase.MysqlSetup(options);
                    };

    if (server != null) {
      List<String> fallbacks = demoDataset.fallbacks(databaseSetup.getDemoScripts(dump), server);
      if (!confirmFallbacks(demoDataset, server, fallbacks, dump)) {
        System.err.println("Canceled. Exiting.");
        System.exit(0);
      }
    }

    if (dump) {
      databaseSetup.dump();
      return;
    }

    // create application screen
    SwingUtilities.invokeLater(() -> new MainClass());
  }
} // end public class MainClass extends JFrame {
// vi: sw=2 ts=8
