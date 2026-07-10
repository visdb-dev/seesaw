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

import java.lang.System.Logger;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.text.AttributeSet;

import com.nqadmin.swingset.utils.JStuff;
import com.nqadmin.swingset.utils.SSComponent;

import static com.nqadmin.swingset.utils.JStuff.sf;
import static java.lang.System.Logger.Level.DEBUG;

/**
 * This TextDecorator applies a named TextStyle based on ComponentState.
 * There is a map of ComponentState to text style name.
 * If no style name is available for a ComponentState, {@link TextStyles#RESET}
 * is used.
 */
public class ComponentStateTextDecorator extends BaseTextDecorator
{
	private static final Logger logger = JStuff.getLogger();

	private final EnumMap<ComponentState, String> styleNames = new EnumMap<>(ComponentState.class);

	/** Create using specified map.
	 * @param map
	 */
	public ComponentStateTextDecorator(Map<ComponentState, String> map)
	{
		styleNames.putAll(map);
	}

	/** Decorate the text according to {@code valid}.
	 * @param valid
	 */
	public void decorateText(SSComponent.ValidationResult valid)
	{
		ComponentState state = ComponentState.getComponentState(getSSComponent(), valid);
		AttributeSet style = TextStyles.getStyle(styleNames.get(state));
		TextStyles.applyStyle(jComp(), style != null ? style : TextStyles.RESET);
		logger.log(DEBUG, sf("Style: %s",
				jComp().getClientProperty(TextStyles.STYLE_NAME)));
	}
	
	/**
	 * Validate the component and decorate the text accordingly.
	 */
	@Override
	public void decorateText()
	{
		SSComponent.ValidationResult valid = getSSComponent().allValidate();
		decorateText(valid);
	}
}
