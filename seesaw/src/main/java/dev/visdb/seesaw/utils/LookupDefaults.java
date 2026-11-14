/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.utils;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.beans.Beans;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.System.Logger.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.openide.util.lookup.Lookups;

import dev.visdb.seesaw.SeeSaw;
import dev.visdb.seesaw.decorators.BorderDecorator;
import dev.visdb.seesaw.decorators.Decorator;
import dev.visdb.seesaw.decorators.DecoratorFactory;
import dev.visdb.seesaw.decorators.TextStyles;
import dev.visdb.seesaw.navigate.Utils;
import dev.visdb.seesaw.utils.SsUtils.DecoratorPanelAdjustSOUTH;

import static dev.visdb.seesaw.utils.JStuff.sf;

/**
 * This class is used internally to do the library initialization:
 * run the registered {@link SeeSawInitialization}, then
 * initialize CentralLookup defaults and text style defaults.
 * For CentralLookup, only elements that are not already set are initialized.
 * <p>
 * An application starts this with {@link SeeSaw#init() }; for example
 * {@snippet lang="java":
 *     public static void main(String[] args) {
 *         // Add application defaults to CentralLookup.
 *         CentralLookup lkup = CentralLookup.getDefault();
 *         // lkup.add(anInstanceofSomething);
 *         // keep adding stuff as needed
 *         SeeSaw.init();
 *         // ...
 *         DbSupport supp = DbSupportFactory.setupLookup(dbConnection);
 *     }
 * }
 * If that was not done then the initialization happens, and that is
 * reported as an error, when either SSComponent/SSCommon or RowsModel is
 * first referenced, see {@link #checkInit() }. Some access to the library
 * does not go through those classes, and so is before that initialization.
 */
public class LookupDefaults {
  private LookupDefaults() {}
  private static final System.Logger logger = System.getLogger(LookupDefaults.class.getName());
  static {
    Utils.getGlobalEventBus();
  }

  /**
   * Lookup path for {@link SeeSawInitialization}.
   */
  public static final String INIT_PATH = "SS/Initialization";

  private enum State { NOT_STARTED, RUNNING, DONE }
  private static volatile State state = State.NOT_STARTED;

  /**
   * Do the library initialization; use {@link SeeSaw#init() }.
   * Only the first call does anything. A call from another thread while
   * the initialization is running waits for it to finish. A call from
   * within the initialization itself returns immediately.
   * <p>
   * First the {@link SeeSawInitialization} registered at {@link #INIT_PATH}
   * are run. Then the default CentralLookup elements that are required by
   * the library, and are not already present or setup by the application,
   * are initialized.
   * <p>
   * Might be better to initialize where needed if no value.
   * But this does serve to document some of the things that can be put in lookup.
   */
  public static void init() {
    if (state == State.DONE)
      return;
    synchronized (LookupDefaults.class) {
      // Either finished while waiting for the lock,
      // or RUNNING: this thread is already in init().
      if (state != State.NOT_STARTED)
        return;
      state = State.RUNNING;
      try {
        runInitializers();
        initDefaults();
      } finally {
        state = State.DONE;
      }
    }
  }

  /**
   * Check that the library has been initialized, see {@link SeeSaw#init() }.
   * If not, report it, see {@link #PROP_NO_INIT_DIALOG}, and then do the
   * initialization.
   * <p>
   * This is for use by the library, from the classes that can not be used
   * without initialization.
   */
  public static void checkInit() {
    if (state == State.NOT_STARTED)
      reportNotInitialized();
    init();
  }

  /**
   * System property; if "true" then there is no dialog when the library is
   * used and {@link SeeSaw#init() } has not been called. It is still logged.
   * There is never a dialog if the JVM is headless.
   */
  public static final String PROP_NO_INIT_DIALOG = "seesaw.init.noDialog";

  /**
   * The library is being used and SeeSaw.init() has not been called.
   * This is the one place that reports it: log an error, with a stack trace
   * that shows the first use, and tell the person running the application.
   * <p>
   * This does not wait for the dialog. It is called from a static
   * initializer, which should not block; and waiting here for the
   * event dispatch thread could deadlock.
   */
  private static void reportNotInitialized() {
    String msg = "SeeSaw.init() has not been called. The application must call it"
        + " before using the library. Initializing now.";
    logger.log(Level.ERROR, msg, new Throwable(msg));

    if (GraphicsEnvironment.isHeadless() || Boolean.getBoolean(PROP_NO_INIT_DIALOG))
      return;
    EventQueue.invokeLater(() -> JOptionPane.showMessageDialog(
        null,
        sf("""
           SeeSaw.init() was not called before the library was used.
           The application must call SeeSaw.init() during startup.

           The library has now been initialized; the log shows where
           it was first used. This dialog can be turned off with
           -D%s=true""", PROP_NO_INIT_DIALOG),
        "SeeSaw is not initialized", JOptionPane.ERROR_MESSAGE));
  }

  /** Run all the SeeSawInitialization; one that fails does not stop the others. */
  private static void runInitializers() {
    for (var initializer : Lookups.forPath(INIT_PATH).lookupAll(SeeSawInitialization.class)) {
      try {
        logger.log(Level.DEBUG, () -> sf("SeeSawInitialization: '%s'", initializer));
        initializer.init();
      } catch (Exception | LinkageError ex) {
        logger.log(Level.ERROR, sf("SeeSawInitialization '%s' failed.", initializer), ex);
      }
    }
  }

  private static void initDefaults() {
    initStyles();

    // Make sure there's a default DecoratorStyle.
    if (Globals.getOption(Decorator.DecoratorStyle.class) == null)
      Globals.setOption(Decorator.DecoratorStyle.class, Decorator.DecoratorStyle.BORDER);

    // There should be a BorderDecoratorPaint.
    if (Globals.getOption(BorderDecorator.BorderDecoratorPaint.class) == null)
      Globals.setOption(BorderDecorator.BorderDecoratorPaint.class,
                     new BorderDecorator.BorderDecoratorPaint());

    // BORDER and BACKGROUND decorators are available by path.

    // For debug.
    var decos = Lookups.forPath(DecoratorFactory.DECORATOR_PATH)
        .lookupAll(DecoratorFactory.class);
    for(DecoratorFactory deco : decos) {
      logger.log(Level.INFO, sf("Available DecoratorFactory: '%s'", deco));
    }

    // HACK for SimpleValidation, seems to be an off by one issue
    // TODO: could include style, allow multiple, then spin through
    //       looking for an adjustment that matches our style.
    //       But, for now, there's only SV that cares about this.
    if (Globals.getOption(DecoratorPanelAdjustSOUTH.class) == null) {
      Globals.setOption(DecoratorPanelAdjustSOUTH.class,
                        new DecoratorPanelAdjustSOUTH(new Dimension(0, 1)));
    }
  }

  private static boolean initializedStyles;

  /**
   * Set up the default styles; does nothing if any styles are already set.
   * If an app wants to add some of it own styles, with names that do not
   * conflict, this should be called first rather than waiting for the
   * default initialization.
   * If an app want to replace the default styles then add them before
   * this is called.
   */
  public static void initStyles() {
    if (initializedStyles)
      return;

    if (!TextStyles.getStyleNames().isEmpty())
      return;

    if (EventQueue.isDispatchThread()) {
      if (!Beans.isDesignTime()) {
        String msg = "LookupDefaults.initStyles() invoked from EDT.";
        Throwable stack = new Throwable(msg);
        logger.log(Level.ERROR, msg, stack);
      }

      // Problem if EDT tries to access TextStyles before loading complete.
      new SwingWorker<Object, Object>() {
        @Override
        protected Object doInBackground() throws Exception {
          initStyles();
          return null;
        }
      }.execute();
      return;
    }

    Reader reader = new StringReader(DEFAULT_STYLES_JSON);
    try {
      TextStyles.loadStylesFromJson(reader, "LookupDefaults");
    } catch (IOException ex) {
      logger.log(Level.ERROR, (String) null, ex);
    }
    initializedStyles = true;
  }
  /** This can be manually loaded if the Styles get cleared. */
  public static final String DEFAULT_STYLES_JSON = """
        {
          "negative_number": {
            "foreground": "red"
          }
        }
        """;
}
// vi: sw=2 ts=8
