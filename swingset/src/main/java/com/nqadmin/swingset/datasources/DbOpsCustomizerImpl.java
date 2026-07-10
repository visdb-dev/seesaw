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
package com.nqadmin.swingset.datasources;


import java.awt.Container;
import java.lang.System.Logger;

import com.nqadmin.swingset.utils.JStuff;
import com.nqadmin.swingset.utils.SSComponent;
import com.nqadmin.swingset.utils.SSUtils;

import static java.lang.System.Logger.Level.*;

/**
 * Custom implementation of DbOpsCustomizer that overrides performPreInsertOps() to
 * clear/reset the various database-aware fields on a screen when the user adds
 * a new record. An instance of this class can be created for the container
 * where the fields are to be cleared and passed to the data navigator.
 * <p>
 * The data navigator will call the performPreInsertOps() method whenever the
 * user presses the insert button on the navigator. This functions recursively
 * clears (null, empty string, deselect) any SwingSet components or other
 * JComponents on the screen
 * <p>
 * This recursive behavior performed on all the components inside the JPanel or
 * JTabbedPane inside the specified container.
 */
public class DbOpsCustomizerImpl implements DbOpsCustomizer {

	/**
	 * Logger for component
	 */
	protected static final Logger logger = JStuff.getLogger();

	/**
	 * Screen where components to be cleared are located.
	 */
	protected Container container = null;

	/**
	 * Constructs a DbOpsCustomizerImpl with the specified container.
	 *
	 * @param container	GUI Container to scan for Swing components to clear/reset
	 */
	public DbOpsCustomizerImpl(final Container container) {
		this.container = container;
	}

	/**
	 * Performs pre-insertion operations, in particular {@link #cleanComponents(Container) }.
	 */
	@Override
	public void performPreInsertOps() {

		logger.log(DEBUG, "About to call setComponents() to clear values.");
		cleanComponents(container);

	}

	/**
	 * In the specified container, clear JTextFields, reset combo boxes to empty
	 * item and make other components of interest "clean".
	 * Typically done for a new record/row. Uses
	 * {@link SSUtils#visitSSComponents(Container, Consumer) }
	 * to run {@link SSComponent#cleanField() }.
	 * <p>
	 * This is done for all SwingSet components, text fields, and text areas,
	 * recursively looking in to the JTabbedPanes and JPanels inside the given
	 * container as needed.
	 *
	 * @param container container in which to recursively initialize components
	 */
	protected void cleanComponents(final Container container) {
		SSUtils.visitSSComponents(container, comp -> comp.cleanField());
	}
} // end public class DbOpsCustomizerImpl
