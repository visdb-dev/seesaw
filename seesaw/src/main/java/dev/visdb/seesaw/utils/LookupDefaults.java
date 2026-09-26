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
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.System.Logger.Level;

import javax.swing.SwingWorker;

import org.openide.util.lookup.Lookups;

import dev.visdb.seesaw.decorators.BorderDecorator;
import dev.visdb.seesaw.decorators.Decorator;
import dev.visdb.seesaw.decorators.DecoratorSupplier;
import dev.visdb.seesaw.decorators.TextStyles;
import dev.visdb.seesaw.navigate.Utils;
import dev.visdb.seesaw.utils.SsUtils.DecoratorPanelAdjustSOUTH;

import static dev.visdb.seesaw.utils.JStuff.sf;

/**
 * This class is used internally to initialize CentralLookup defaults
 * and text style defaults.
 * For CentralLookup, only elements that are not already set are initialized.
 * It is invoked automatically when either SSComponent/SSCommon
 * or RowsModel is referenced (impossible to use library without these).
 * <p>
 * Certain initialization access to this library does not force this initialization,
 * for example first access to the library could be something like:
 * {@snippet lang="java":
 *     public static void main(String[] args) {
 *         // Add application defaults to CentralLookup.
 *         CentralLookup lkup = CentralLookup.getDefault();
 *         // lkup.add(anInstanceofSomething);
 *         // keep adding stuff as needed
 *         LookupDefaults.init();
 *         // ...
 *         DbSupport supp = DbSupportFactory.setupLookup(dbConnection);
 *     }
 * }
 */
public class LookupDefaults {
  private LookupDefaults() {}
  private static final System.Logger logger = System.getLogger(LookupDefaults.class.getName());
  static {
    Utils.getGlobalEventBus();
  }

  private static boolean initialized;
  /**
   * This is automatically called around first library use,
   * not including CentralLookup, to initialize
   * default CentralLookup elements that are required by the library
   * and are not already present or setup by the application.
   * <p>
   * Might be better to initialize where needed if no value.
   * But this does serve to document some of the things that can be put in lookup.
   */
  public static void init() {
    if (initialized)
      return;

    initStyles();

    CentralLookup lkup = CentralLookup.getDefault();

    // Make sure there's a default DecoratorStyle.
    if (lkup.lookup(Decorator.DecoratorStyle.class) == null)
      lkup.add(Decorator.DecoratorStyle.BORDER);

    // There should be a BorderDecoratorPaint.
    if (lkup.lookup(BorderDecorator.BorderDecoratorPaint.class) == null)
      lkup.add(new BorderDecorator.BorderDecoratorPaint());

    // BORDER and BACKGROUND decorators are available by path.

    // For debug.
    var decos = Lookups.forPath(DecoratorSupplier.DECORATOR_PATH)
        .lookupAll(DecoratorSupplier.class);
    for(DecoratorSupplier deco : decos) {
      logger.log(Level.INFO, sf("Available DecoratorSupplier: '%s'", deco));
    }

    // HACK for SimpleValidation, seems to be an off by one issue
    // TODO: could include style, allow multiple, then spin through
    //       looking for an adjustment that matches our style.
    //       But, for now, there's only SV that cares about this.
    if (lkup.lookup(DecoratorPanelAdjustSOUTH.class) == null) {
      lkup.add(new DecoratorPanelAdjustSOUTH(new Dimension(0, 1)));
    }


    initialized = true;
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
      String msg = "LookupDefaults.initStyles() invoked from EDT.";
      Exception ex = new Exception(msg);
      logger.log(Level.ERROR, msg, ex);

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
