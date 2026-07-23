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

package com.nqadmin.swingset.decorators;

import java.awt.Component;

import javax.swing.SwingUtilities;

import com.nqadmin.swingset.navigate.Utils;
import com.nqadmin.swingset.utils.SSComponent;

/** The state of a component, focused/dirty/modified.
 * Used to create a proper decoration */
// TODO: may want to treat as bit field for: error/focus/warning/dirty
public enum ComponentState
{
	/** not focused, no error, not modified */
	CLEAN,
	/** focus gained, no error, not modified */
	FOCUSED_CLEAN,
	/** modified without focus */
	MODIFIED,
	/** modified with focus */
	FOCUSED_MODIFIED,
	/** error with/without focus */
	ERROR,
	/** error with/without focus */
	FOCUSED_ERROR;

	/** focused?
	 * @return  */
	public boolean isFocused()
	{
		return this == FOCUSED_CLEAN || this == FOCUSED_MODIFIED || this == FOCUSED_ERROR;
	}

	/** modified?
	 * @return  */
	public boolean isModified()
	{
		return this == MODIFIED || this == FOCUSED_MODIFIED;
	}

	/** focused?
	 * @return  */
	public boolean isError()
	{
		return this == ERROR || this == FOCUSED_ERROR;
	}

	/**
	 * Determine the state of the component about focus/clean/dirty/error.
	 * @param comp
	 * @param valid
	 * @return the component state
	 */
	public static ComponentState getComponentState(SSComponent comp, SSComponent.ValidationResult valid)
	{
		ComponentState borderState;
		if (valid.all())
			borderState = comp.isDirty() ? MODIFIED : CLEAN;
		else
			borderState = ERROR;
		Component f = Utils.getKFM().getFocusOwner();
		if (f != null && SwingUtilities.isDescendingFrom(f, (Component) comp))
			borderState = switch (borderState) {
			case CLEAN -> FOCUSED_CLEAN;
			case MODIFIED -> FOCUSED_MODIFIED;
			case ERROR -> FOCUSED_ERROR;
			default -> throw new IllegalStateException("Unexpected value: " + (borderState));
			};
		return borderState;
	}
}
