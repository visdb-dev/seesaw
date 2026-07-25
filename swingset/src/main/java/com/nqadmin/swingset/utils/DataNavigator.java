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
 * copyright (C) 2024-2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package com.nqadmin.swingset.utils;


import java.awt.Dimension;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;

import com.nqadmin.swingset.navigate.RowNumberSpinner;
import com.nqadmin.swingset.navigate.RowsModel;

import static com.nqadmin.swingset.navigate.RowsAction.*;

/**
 * UI component used for data navigation. It provides buttons for
 * navigating forwards and backwards through a rowSet, displaying the current
 * row number, commit and undo,
 * re-fetch the rows in the rowSet from the database,
 * insert and delete the displayed row, and display the number of records
 * in the rowSet.
 * <p>
 * <img src="doc-files/ssdatanavigator.png" alt="SSDataNavigator image"
 * style="display: inline-block; margin-left: 40px;">
 * <br>or<br> 
 * <img src="doc-files/ssdatanavigator_2lines.png" alt="SSDataNavigator image"
 * style="display: inline-block; margin-left: 40px;">
 * <p>
 * There are a variety of properties
 * to control allowed actions on a RowSet, e.g.
 * AllowInsert, AllowDelete, ...; see {@link RowsModel#setAllowInsert}, ...
 * <p>
 * For example if you are displaying three columns using the TextField and the
 * user changes the text in the text fields then the columns will be updated to
 * the new values when the user presses commit. If the user wants to
 * revert the changes he made he can press the Undo button.
 * <p>
 * The enabled/disabled state of the buttons is dependent on the edit state
 * of the SSComponents of the RowSet that the DataNavigator controls.
 * Normally the navigation buttons are disabled if any field of the row is
 * modified; either commit the row or undo the changes to allow navigation.
 * If any field is in error, then only the undo button is enabled; or, of course,
 * edit the field to remove the error.
 */
// NOTE: NavigateState.setAutoCommit() is not public and never referenced.
// When auto commit mode is enabled, the navigation buttons remain enabled;
// a navigation automatically commits any changes. Once navigation takes place
// changes can't be reverted using Undo button (has to be done manually by the user).
@SuppressWarnings("serial")
public class DataNavigator extends JPanel
{
	/** The RowSet's actions/models for the buttons are in here. */
	private RowsModel rowsModel;

	/** This panel's original action map is the parent of any navActionMap. */
	private final ActionMap parentActionMap;
	private final ActionMap navActionMap;

	/**
	 * The number of lines for the navigator.
	 */
	public enum Lines { /** one line */ ONE, /** two line */ TWO }

	/**
	 * Constructs the DataNavigator with the given RowsModel.
	 *
	 * @param rowsModel   the RowsModel to which the navigator is bound to
	 */
	public DataNavigator(RowsModel rowsModel)
	{
		this(rowsModel, Lines.ONE, null);
	}

	/**
	 * Constructs the DataNavigator with the given RowsModel.
	 *
	 * @param rowsModel   the RowsModel to which the navigator is bound to
	 * @param nLines      number of display lines for the navigator
	 */
	public DataNavigator(RowsModel rowsModel, Lines nLines)
	{
		this(rowsModel, nLines, null);
	}

	/**
	 * Constructs the DataNavigator with the given RowsModel
	 * and sets the size of the buttons on the navigator to the given size
	 *
	 * @param rowsModel   the RowsModel to which the navigator is bound to
	 * @param _buttonSize the size to which the button on navigator have to be set
	 */
	public DataNavigator(RowsModel rowsModel, Dimension _buttonSize) {
		this(rowsModel, Lines.ONE, _buttonSize);
	}

	/**
	 * Constructs the DataNavigator with the given RowsModel
	 * and sets the size of the buttons on the navigator to the given size
	 *
	 * @param rowsModel   the RowsModel to which the navigator is bound to
	 * @param _buttonSize the size to which the button on navigator have to be set
	 * @param nLines      number of display lines for the navigator
	 */
	@SuppressWarnings({"LeakingThisInConstructor", "OverridableMethodCallInConstructor"})
	public DataNavigator(RowsModel rowsModel, Lines nLines, Dimension _buttonSize) {
		Objects.requireNonNull(rowsModel);
		rowNumberSpinner = new RowNumberSpinner(rowsModel);
		uiComponents = uiComponents();
		uiButtons = uiButtons();
		uiComponentsTop = uiComponentsTop();
		uiComponentsBottom = uiComponentsBottom();

		parentActionMap = getActionMap();
		navActionMap = new ActionMap();
		// Insert the navigate actions in front of the original actions.
		navActionMap.setParent(parentActionMap);
		setActionMap(navActionMap);

		rowSpinnerSize = new Dimension(65, 20);
		rowCountSize = new Dimension(80, 20);
		buttonSize = _buttonSize == null ? new Dimension(40, 20) : _buttonSize;

		rowNumberSpinner.removeTinyArrows(rowSpinnerSize);
		rowNumberSpinner.setWindowUpDownKeysEnable(true);
		rowNumberSpinner.addChangeListener((ChangeEvent e) -> {
			updateLblRowCount();
		});

		setRowsModel(rowsModel);
		hideActionText(); // suppress the Action name from appearing next to the button icon.
		setButtonSizes();
		// For some reason the spinner text gets stretched, so nail it down.
		rowNumberSpinner.setMaximumSize(rowSpinnerSize);
		// lblRowCount.setFont(new Font("Dialog", Font.BOLD, 12));
		// lblRowCount.setEditable(false);
		// lblRowCount.setBorder(UIManager.getBorder("TextField.border"));

		if (nLines == Lines.ONE)
			createOneLineNavigator();
		else
			createTwoLineNavigator();
	}

	/**
	 * Set the navigator to use a different RowsModel ;
	 * swap in the new navigate ActionMap.
	 *
	 * @param rowsModel data for navigator
	 * @deprecated maybe temporarily, use RowsModel.setRowSet
	 */
	// TODO: setModel(RowsModel)
	// want this to be private
	@Deprecated
	protected void setRowsModel(RowsModel rowsModel)
	{
		Objects.requireNonNull(rowsModel);
		if (this.rowsModel != null)
			throw new IllegalStateException("RowsModel already set");
		this.rowsModel = rowsModel;

		installRowsModel(rowsModel);
	}

	/**
	 * Set the navigator to use a different row set;
	 * swap in the new navigate ActionMap.
	 *
	 * @param RowsModel for navigator
	 */
	private void installRowsModel(RowsModel rowsModel)
	{
		// Fill Actions for the navigator with actions from the new rowsModel/RowSet.
		rowsModel.fillNavActionMap(navActionMap);

		// set the actions to the new navAction
		firstButton.setAction(navActionMap.get(ACT_FIRST));
		previousButton.setAction(navActionMap.get(ACT_PREVIOUS));
		nextButton.setAction(navActionMap.get(ACT_NEXT));
		lastButton.setAction(navActionMap.get(ACT_LAST));
		commitButton.setAction(navActionMap.get(ACT_COMMIT));
		undoButton.setAction(navActionMap.get(ACT_REVERT));
		refreshButton.setAction(navActionMap.get(ACT_REFRESH));
		addButton.setAction(navActionMap.get(ACT_ADD));
		deleteButton.setAction(navActionMap.get(ACT_DELETE));

		updateLblRowCount();
	}

	private void updateLblRowCount()
	{
		Comparable<?> max = rowNumberSpinner.getModel().getMaximum();
		lblRowCount.setText(max != null ? "of " + max : "");
	}

	/**
	 * Adds the navigator components to the navigator panel.
	 */
	protected final void createOneLineNavigator() {
		Box box = Box.createHorizontalBox();
		uiComponents.forEach(uiItem -> box.add(uiItem));

		// add buttons to the navigator
		add(box);
	}

	/**
	 * Adds the navigator components to the navigator panel.
	 */
	protected final void createTwoLineNavigator() {
		// Create two horizontal boxes.
		Box topBox = Box.createHorizontalBox();
		uiComponentsTop.forEach(uiItem -> topBox.add(uiItem));

		Box bottomBox = Box.createHorizontalBox();
		uiComponentsBottom.forEach(uiItem -> bottomBox.add(uiItem));

		Box box = Box.createVerticalBox();
		box.add(topBox);
		box.add(bottomBox);

		box.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(2, 2, 2, 2),
				BorderFactory.createCompoundBorder(
						UIManager.getBorder("TitledBorder.border"),
						BorderFactory.createEmptyBorder(2, 2, 2, 2))));
		// add buttons to the navigator
		add(box);
	}

	/**
	 * Use the up/down arrow keys while this spinner's window is focused
	 * to adjust row number.
	 * 
	 * @param enable true enables up/down keys when window has focus
	 */
	public void setWindowUpDownKeysEnable(boolean enable) {
		rowNumberSpinner.setWindowUpDownKeysEnable(enable);
	}

	// There used to be a bunch of "do*ButtonClick()" methods; not used anywhere.
	// Could replace with "doButtonClick(RowsAction)" if needed for whatever.

	/**
	 * Returns the size of buttons on the data navigator.
	 *
	 * @return returns a Dimension object representing the size of each button on
	 *         the data navigator.
	 */
	// TODO: Dimension copy?
	public Dimension getButtonSize() {
		return buttonSize;
	}

	/**
	 * Return the RowsModel for this navigator.
	 * @return 
	 */
	public RowsModel getRowsModel()
	{
		return rowsModel;
	}
	
	/**
	 * Prevent the navigator buttons from displaying the Action name with the icon.
	 */
	private void hideActionText() {
		uiButtons.forEach(uiItem -> uiItem.setHideActionText(true));
	}

	/**
	 * This will make all the components in the navigator to either focusable
	 * components or non focusable components. Set to false if you don't want any of
	 * the buttons or text fields in the navigator to receive the focus else true.
	 * The default value is true.
	 *
	 * @param focusable - false if you don't want the navigator to receive focus
	 *                  else false.
	 */
	@Override
	public void setFocusable(final boolean focusable) {
		uiButtons.forEach(uiItem -> uiItem.setFocusable(focusable));
		rowNumberSpinner.setFocusable(focusable);
	}

	/**
	 * Sets the preferredSize and the MinimumSize of the buttons to the specified
	 * size
	 *
	 * @param _buttonSize the required dimension of the buttons
	 */
	public void setButtonSize(final Dimension _buttonSize) {
		final Dimension oldValue = buttonSize;
		buttonSize = _buttonSize;
		firePropertyChange("buttonSize", oldValue, buttonSize);
		setButtonSizes();
	}

	/**
	 * Sets the dimensions for the navigator components.
	 */
	protected void setButtonSizes() {
		uiButtons.forEach(uiItem -> uiItem.setPreferredSize(buttonSize));
		rowNumberSpinner.setPreferredSize(rowSpinnerSize);
		lblRowCount.setPreferredSize(rowCountSize);

		lblRowCount.setHorizontalAlignment(SwingConstants.CENTER);

		uiButtons.forEach(uiItem -> uiItem.setMinimumSize(buttonSize));
		rowNumberSpinner.setMinimumSize(rowSpinnerSize);
		lblRowCount.setMinimumSize(rowCountSize);
	}

	//////////////////////////////////////////////////////////////////////
	//
	// Components/dimensions
	//

	/** Button to add a record to the RowSet. */
	protected final JButton addButton = new JButton();

	/** Button to commit screen changes to the RowSet. */
	protected final JButton commitButton = new JButton();

	/** Button to delete the current record in the RowSet. */
	protected final JButton deleteButton = new JButton();

	/** Button to navigate to the first record in the RowSet. */
	protected final JButton firstButton = new JButton();

	/** Button to navigate to the last record in the RowSet. */
	protected final JButton lastButton = new JButton();

	/** Button to navigate to the next record in the RowSet. */
	protected final JButton nextButton = new JButton();

	/** Button to navigate to the previous record in the RowSet. */
	protected final JButton previousButton = new JButton();
	
	/** Button to refresh the screen based on any changes to the RowSet. */
	protected final JButton refreshButton = new JButton();

	/** Button to revert screen changes based on the RowSet. */
	protected final JButton undoButton = new JButton();

	/** Navigator button dimensions. */
	private Dimension buttonSize;

	/** Label to display the total number of records in the RowSet. */
	private final JLabel lblRowCount = new JLabel();
	// private final JTextField lblRowCount = new JTextField();

	/** Component for viewing/changing the current record number. */
	protected final RowNumberSpinner rowNumberSpinner;

	/** Current record spinner dimensions. */
	private final Dimension rowSpinnerSize;
	private final Dimension rowCountSize;

	private final List<JComponent> uiComponents;
	private final List<AbstractButton> uiButtons;
	private final List<JComponent> uiComponentsTop;
	private final List<JComponent> uiComponentsBottom;

	/** These are added in order to this JPanel */
	private List<JComponent> uiComponents()
	{
		return List.of(
				firstButton,
				previousButton,
				rowNumberSpinner,
				nextButton,
				lastButton,
				commitButton,
				undoButton,
				refreshButton,
				addButton,
				deleteButton,
				lblRowCount
		);
	}
	private List<JComponent> uiComponentsTop() {
		return List.of(
				firstButton,
				previousButton,
				rowNumberSpinner,
				nextButton,
				lastButton,
				lblRowCount
		);
	}
	private List<JComponent> uiComponentsBottom() {
		return List.of(
				commitButton,
				undoButton,
				refreshButton,
				addButton,
				deleteButton
		);
	}

	/** The buttons can often be handled en masse */
	private List<AbstractButton> uiButtons()
	{
		return List.of(
				firstButton,
				previousButton,
				nextButton,
				lastButton,
				commitButton,
				undoButton,
				refreshButton,
				addButton,
				deleteButton
		);
	}
}

