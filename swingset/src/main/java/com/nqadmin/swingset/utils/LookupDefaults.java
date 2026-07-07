/* *****************************************************************************
 * Copyright (C) 2026, Ernie R Rael. All rights reserved.
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
 * ****************************************************************************/
package com.nqadmin.swingset.utils;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.System.Logger.Level;

import javax.swing.SwingWorker;

import com.nqadmin.swingset.decorators.BackgroundDecorator;
import com.nqadmin.swingset.decorators.BorderDecorator;
import com.nqadmin.swingset.decorators.Decorator;
import com.nqadmin.swingset.decorators.DecoratorSupplier;
import com.nqadmin.swingset.decorators.TextStyles;

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
public class LookupDefaults
{
	private LookupDefaults() { }
	private static final System.Logger logger = System.getLogger(LookupDefaults.class.getName());

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

		//
		// There should be a DecoratorStyle.
		//
		Decorator.DecoratorStyle style = lkup.lookup(Decorator.DecoratorStyle.class);
		if (style == null)
			lkup.add(Decorator.DecoratorStyle.BORDER);

		//
		// There should be BORDER and BACKGROUND decorators.
		//
		var decos = lkup.lookupAll(DecoratorSupplier.class);

		boolean hasBorder = false;
		boolean hasBackground = false;
		for (var deco : decos) {
			if (deco.getDecoratorStyle().equals(Decorator.DecoratorStyle.BORDER))
				hasBorder = true;
			if (deco.getDecoratorStyle().equals(Decorator.DecoratorStyle.BACKGROUND))
				hasBackground = true;
		}
		if (!hasBorder)
			lkup.add(new DecoratorSupplier(() -> {return new BorderDecorator();}));
		if (!hasBackground)
			lkup.add(new DecoratorSupplier(() -> {return new BackgroundDecorator();}));

		//
		// There should be a BorderDecoratorPaint.
		//
		if (lkup.lookup(BorderDecorator.BorderDecoratorPaint.class) == null)
			lkup.add(new BorderDecorator.BorderDecoratorPaint());

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
				protected Object doInBackground() throws Exception
				{
					initStyles();
					return null;
				}
			}.execute();
			return;
		}

		Reader reader = new StringReader(DEFAULT_STYLES_JSON);
		try {
			TextStyles.loadStylesFromJson(reader);
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