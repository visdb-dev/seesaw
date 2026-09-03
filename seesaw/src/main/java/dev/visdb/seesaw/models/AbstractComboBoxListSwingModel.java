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
package dev.visdb.seesaw.models;

import java.awt.Component;
import java.lang.System.Logger;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import dev.visdb.seesaw.utils.JStuff;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static dev.visdb.seesaw.utils.SsUtils.objectID;
import static java.lang.System.Logger.Level.*;

/**
 * This class encapsulates the list and list data used for SwingSet
 * list and combobox components.
 * The class holds a reference to a {@code List<ListItem>} and
 * it's method {@link #createListItem(java.lang.Object...) }
is a factory that creates ListItem objects. It does not have public
methods to modify the list; getRemodel and sub-classes provide that.
Using {@link #install(javax.swing.JComponent, dev.visdb.seesaw.models.AbstractComboBoxListSwingModel, javax.swing.ListCellRenderer)} a proxy that delegates
 * to this class is installed into a JList or JComboBox.
 * <p>
 * Where possible, this class and subclasses name methods
 * similarly to the List interface, such as "add*", "remove*".
 * <p>
 * The encapsulated data can be thought of as a two dimensional
 * array [ height X width ]; the height is the size of {@code List<ListItem>},
the width is the number of elements in a ListItem .
The number of elements in an ListItem is controlled by a property,
see {@link #setItemNumElems};
 * the property may only be changed when the item list is empty.
 * <p>
 * {@code createElementSlice} creates live, read-only, lists of
the individual elements of an ListItem; it is [ height X 1 ].
This live list track changes to the main list.
The slice always has the same number of
items as the main list.
<p>
The contents of an ListItem are read through the protected
{@link ListItem0} interface. The contents are modified through
 * methods in this class.
 * <p>
This class is not parameterized; all ListItem elements are Objects.
It is expected that sub-classes are parameterized and cast as needed.
<h2>Remodel</h2>
 * Inspections and modifications of the item list, and of its SSListItems,
 * are done through a Remodel Object see {@link #getRemodel() }. The
 * remodel object is "try with resource" compatible and subclasses
 * may implement locking in their implementations of takeWriteLock()
 * and releaseWriteLock(). See {@link GlazedListsKeyDisplayValueInfo}
 * for an example.
 * <p>
 * Compatible with GlazedLists AutoComplete feature;
 * in which case an EventList is set in the constructor.
 *
 * See {@link GlazedListsKeyDisplayValueInfo} usage in SSDBCombox
 *		for use Glazed AutoComplete feature
 * @since 4.0.0
 */
//
// There is an issue that arises from modifying the contents of a list item;
// in this case, the list itself is not modified. There is a fireContentsChanged
// event. This all is good when this is used as a model in a JComponent.
// The setElem method is the only one that would modify a list item in place.
//
// However, when this is used to mangage a GlazedList, there is no event
// listener. Glazed listens to changes in the EventList which this manages.
// When working on a glazed list, a newListItem must be created and then
// list.set(index, newListItem) and that notifies the event list. Note that
// it is generally insufficient to do something like
//     listItem = list.get(index)
//     modify-listItem
//     list.set(index, listItem)
// becase if the set is optimized by currentListItem.equals(setListItem)
// that will say that nothing is changed. So must create a new list item.
//
public abstract class AbstractComboBoxListSwingModel {
  /** when true, handle combo box selected item (about the events) */
  private boolean comboBoxModel;
  /** this class (it's proxy) has been installed into a JComponent */
  private boolean installed;
  /** Install this when not using glazed, and send events through this */
  private final ComboBoxModelProxy modelProxy;

  /** System Logger for component */
  private static final Logger logger = JStuff.getLogger();

  /** System Logger for component */
  private static final Logger eventLogger
      = JStuff.getLogger(AbstractComboBoxListSwingModel.class.getName() + ".events");

  /**
   * number of objects in the ListItem
   */
  private int itemNumElems;

  /**
   * For fast check of valid element index.
   */
  // validElemsMask = (1 << nElem) - 1
  // if (!(validElemsMask & (1 << index))) error
  private int validElemsMask;

  /**
   * The list of ListItem elements.
   */
  private final List<ListItem> itemList;

  /**
   * A read only list of ListItem elements.
   */
  private final List<ListItem> readOnlyItemList = new AbstractList<ListItem>() {
    @Override
    public ListItem get(int index) {
      return itemList.get(index);
    }

    @Override
    public int size() {
      return itemList.size();
    }
  };

  /**
   * Any created slice-list is added here.
   * If the number of SSListItem elements is changed,
   * then created lists that no longer have active slices are marked invalid
   * and slices that are now active are marked valid.
   * Keep a weakreference so they can go away gracefully.
   */
  private final List<WeakReference<ItemElementSlice>> createdLists = new ArrayList<>();

  /**
   * The constructor to create ListItem
   */
  private Constructor<?> listItemConstructor;

  /**
   * Construct an empty list info container.
   * @param itemNumElems number of elements in an ListItem
   */
  protected AbstractComboBoxListSwingModel(int itemNumElems) {
    this(itemNumElems, null);
    if (Boolean.FALSE) {
      Objects.isNull(sliceInfo(null));
    }
  }

  /**
   * Construct a info container; if the specified itemList is
   * null an array list is created.
   * Only use this constructor directly if you are sure you must.
   * <p>
   * If an itemList is passed in, <b>lose the reference</b>;
   * if the list, or its contents, are modified directly
   * then swing model events are lost.
   * @param itemNumElems number of elements in an ListItem
   * @param itemList list to manage, may be null
   */

  protected AbstractComboBoxListSwingModel(int itemNumElems, List<ListItem> itemList) {
    this.listItemFormatDelegate = new FormatDelegate();
    if (itemList != null && !itemList.isEmpty()) {
      throw new IllegalArgumentException("item list must be empty");
    }

    this.itemList = itemList != null ? itemList : new ArrayList<>();
    this.itemNumElems = itemNumElems;
    setupNumElems(itemNumElems);
    this.modelProxy = new ComboBoxModelProxy();
  }

  /**
   * Used for testing to set combo handling flag.
   * @param itemNumElems number of elements in an ListItem
   * @param isCombo in a combo box
   */
  /*package-test*/ AbstractComboBoxListSwingModel(int itemNumElems, boolean isCombo) {
    this(itemNumElems);

    comboBoxModel = isCombo;
  }

  /*package-test*/ boolean isComboBoxModel() {
    return comboBoxModel;
  }

  /*package-test*/ ComboBoxModelProxy getProxyJunitTextOnly() {
    return modelProxy;
  }

  static class EventLoggingDataListener implements ListDataListener {
    private final ListModel<?> model;
    public EventLoggingDataListener(ListModel<?> model) {
      this.model = model;
    }

    private String getMsg(ListDataEvent e) {
      int t = e.getType();
      String eventString = sf("%s[%d,%d]",
                              t == ListDataEvent.CONTENTS_CHANGED   ? "CH"
                              : t == ListDataEvent.INTERVAL_ADDED   ? "ADD"
                              : t == ListDataEvent.INTERVAL_REMOVED ? "REM"
                                                                    : "?",
                              e.getIndex0(), e.getIndex1());
      return sf("%s: %s", objectID(model), eventString);
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
      eventLogger.log(TRACE, () -> getMsg(e));
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
      eventLogger.log(TRACE, () -> getMsg(e));
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
      eventLogger.log(TRACE, () -> getMsg(e));
    }
  }

  /**
   * If event logging is enabled, log the model
   * unless the model is already logging.
   * @param model the model to log
   */
  // TODO: refine method for adding a model; maybe custom String tag.
  public static void addEventLogging(ListModel<?> model) {
    Objects.requireNonNull(model);
    if (!eventLogger.isLoggable(TRACE)) {
      return;
    }
    if (weakModelSet == null) {
      weakModelSet = Collections.newSetFromMap(new WeakHashMap<>());
    }
    if (weakModelSet.contains(model)) {
      return;
    }
    weakModelSet.add(model);
    model.addListDataListener(new EventLoggingDataListener(model));
  }
  private static Set<ListModel<?>> weakModelSet;

  //////////////////////////////////////////////////////////////////////////
  //
  // Installation
  // Make sure there's a ListItemFormat,
  // and install CellRenderer that uses it.
  //
  // TODO: uninstall
  //

  /**
   * Special case usage, grab the model configured for a JComboBox.
   * @param <T> model elements
   * @param model the model source
   * @return the model
   */
  protected static <T> MutableComboBoxModel<T> getSimpleComboBoxModel(
      AbstractComboBoxListSwingModel model) {
    if (model.installed) {
      throw new IllegalStateException("model already installed");
    }
    model.installed = true;
    model.comboBoxModel = true;
    @SuppressWarnings("unchecked")
    MutableComboBoxModel<T> m = (MutableComboBoxModel<T>) model.modelProxy;
    return m;
  }

  /**
   * Special case usage, grab the model configured for a JList.
   * @param <T> model elements
   * @param model the model source
   * @return the model
   */
  protected static <T> ListModel<T> getSimpleListModel(AbstractComboBoxListSwingModel model) {
    if (model.installed) {
      throw new IllegalStateException("model already installed");
    }
    model.installed = true;
    model.comboBoxModel = false;
    @SuppressWarnings("unchecked")
    ListModel<T> m = (ListModel<T>) model.modelProxy;
    return m;
  }

  /**
   * Creates and installs a ListCellRenderer into the JComponent. The new
   * renderer uses {@link #getListItemFormat() }
   * to get the value to render. The renderer is either
   * a {@code DefaultListCellRenderer} or a {@code  BasicComboBoxRenderer}
   * as appropriate.
   * <p>
   * The model is installed into the JComponent as a convenience.
   *
   * @param jc Jcomponent to set up with model; must be JList or JComboBox
   * @param model associated model
   */
  public static void install(JComponent jc, AbstractComboBoxListSwingModel model) {
    install(jc, model, null);
  }

  /**
   * Installs the specified ListCellRenderer into the JComponent.
   * The model is installed into the JComponent as a convenience.
   * If render is null, a new renderer is created from model.
   *
   * @param jc Jcomponent to set up with model
   * @param model associated model
   * @param render list cell renderer, may be null
   * @throws IllegalArgumentException if jc is not JList or JComboBox
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  // TODO Remove warning suppression post Java 8.
  public static void install(JComponent jc, AbstractComboBoxListSwingModel model,
                             ListCellRenderer<?> render) {
    Objects.requireNonNull(jc);
    Objects.requireNonNull(model);
    if (model.installed) {
      throw new IllegalStateException("model already installed");
    }

    model.installed = true;

    switch (jc) {
      case JList jl -> {
        ListCellRenderer<?> useRender = render == null ? model.new LocalListCellRenderer() : render;
        jl.setCellRenderer(useRender);
        jl.setModel(model.modelProxy);
        model.comboBoxModel = false;
      }
      case JComboBox jcb -> {
        ListCellRenderer<?> useRender
            = render == null ? model.new LocalComboBoxCellRenderer() : render;
        jcb.setRenderer(useRender);
        jcb.setModel(model.modelProxy);
        model.comboBoxModel = true;
      }
      default -> throw new IllegalArgumentException("must be JList or JComboBox");
    }
  }

  final private FormatDelegate listItemFormatDelegate;

  /**
   * When used with glazed lists, the format is set at install into combo
   * time and can not be changed. So glazed installs FormatDelegate, and
   * then setItemListFormat is set/changed as convenient.
   * <p>
   * If the format is changed after combo box initialization is complete,
   * then it is up to the user to force the combo to redraw.
   */
  @SuppressWarnings("serial")
  private static class FormatDelegate extends Format {
    private ListItemFormat listItemFormat;

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
      return listItemFormat != null ? listItemFormat.format(obj, toAppendTo, pos)
                                    : toAppendTo.append(obj);
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
      return listItemFormat != null ? listItemFormat.parseObject(source, pos) : source;
    }
  }

  /**
   * Delegates formatting on the fly.
   * @return Format to install in the model
   */
  protected Format getListItemFormatDelegate() {
    return listItemFormatDelegate;
  }

  /**
   * Set the format to use with this model.
   *
   * @param listItemFormat the format used with this model
   */
  public void setListItemFormat(ListItemFormat listItemFormat) {
    listItemFormatDelegate.listItemFormat = listItemFormat;
    if (modelProxy != null && !itemList.isEmpty()) {
      // assume everything changed
      modelProxy.fire.doFireContentsChanged(this, 0, itemList.size() - 1);
      modelProxy.fire.doFireContentsChanged(this, -1, -1);
    }
  }

  /**
   * Return the listItemFormat associated with this model.
   * @return the associated listItemFormat
   */
  public ListItemFormat getListItemFormat() {
    if (listItemFormatDelegate.listItemFormat == null) {
      listItemFormatDelegate.listItemFormat = new ListItemFormat();
    }
    return listItemFormatDelegate.listItemFormat;
  }

  /**
   * Cell renderer that works with a ListItemFormat.
   */
  protected class LocalListCellRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
      String stringValue = getListItemFormat().format(value);
      return super.getListCellRendererComponent(list, stringValue, index, isSelected, cellHasFocus);
    }
  }

  /**
   * Cell renderer that works with a ListItemFormat.
   */
  protected class LocalComboBoxCellRenderer extends BasicComboBoxRenderer {
    private static final long serialVersionUID = 1L;

    // In following, "JList<?> list" gets an error with 1.8 compiler,
    // and is OK with j-14 compiler. I remember the old one
    // has some inference issues with nested classes
    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    // TODO Remove warning suppression post Java 8.
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
      String stringValue = getListItemFormat().format(value);
      return super.getListCellRendererComponent(list, stringValue, index, isSelected, cellHasFocus);
    }
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // Swing Model methods
  //

  //
  // A ComboBoxModelProxy is created during construction, see modelProxy.
  // This proxy delegates to methods in the AbstractComboBoxListSwingModel,
  // it's enclosing class. The proxy is not used when glazed.
  //
  // The list is managed/modified using a Remodel object.
  // The Remodel has hooks for locking; the are used when
  // Glazedlists and it's EventList are used,
  // see GlazedListsKeyDisplayValueInfo.
  //
  // Locking is not provided when this class' proxy is installed
  // into a JList/JComboBox.
  // See https://github.com/bpangburn/swingset/issues/52
  // For a discussion of how that might be achieved.

  /**
   * This interface provides a method to get the real
   * model from the proxy model.
   */
  public interface ComboBoxListSwingModel {
    /**
     * Return the model being proxied.
     * @return the model
     */
    AbstractComboBoxListSwingModel getComboBoxListSwingModel();
  }

  private interface ComboBoxListFireProxy {
    void doFireContentsChanged(Object source, int index0, int index1);
    void doFireIntervalAdded(Object source, int index0, int index1);
    void doFireIntervalRemoved(Object source, int index0, int index1);
  }

  @SuppressWarnings("serial")
  /*package-test*/
  class ComboBoxModelProxy
      extends DefaultComboBoxModel<ListItem> implements ComboBoxListSwingModel {
    @Override
    public AbstractComboBoxListSwingModel getComboBoxListSwingModel() {
      return AbstractComboBoxListSwingModel.this;
    }

    private final ComboBoxListFireProxy fire = new ComboBoxListFireProxy() {
      @Override
      public void doFireContentsChanged(Object source, int index0, int index1) {
        fireContentsChanged(source, index0, index1);
      }

      @Override
      public void doFireIntervalAdded(Object source, int index0, int index1) {
        fireIntervalAdded(source, index0, index1);
      }

      @Override
      public void doFireIntervalRemoved(Object source, int index0, int index1) {
        fireIntervalRemoved(source, index0, index1);
      }
    };

    //
    // ListModel
    //

    /** {@inheritDoc } */
    @Override
    public int getSize() {
      return itemList.size();
    }

    /** {@inheritDoc } */
    @Override
    public ListItem getElementAt(int index) {
      if (comboBoxModel) {
        // The DefaultComboBoxModel never throws an exception.
        if (index >= 0 && index < itemList.size())
          return itemList.get(index);
        return null;
      }
      // DefaultListModel might throw an exception
      return itemList.get(index);
    }

    //
    // ComboBoxModel
    //

    private ListItem selectedObject;

    /** {@inheritDoc } */
    @Override
    public void setSelectedItem(Object anItem) {
      // TODO: exception if not combo?
      if (comboBoxModel) {
        if (!Objects.equals(selectedObject, anItem)) {
          if (anItem == null || anItem instanceof ListItem) {
            selectedObject = (ListItem) anItem;
            modelProxy.fire.doFireContentsChanged(this, -1, -1);
          } else {
            logger.log(WARNING, () -> "ComboBox#setSelectedItem(" + anItem + ") not SSListItem");
          }
        }
      }
    }

    /** {@inheritDoc } */
    @Override
    public ListItem getSelectedItem() {
      // TODO: exception if not combo?
      return selectedObject;
    }

    //
    // MutableComboBoxModel
    // NOTE the helper methods for keeping selection:
    //      comboAdjustSelectedAfterAdd and comboAdjustSelectedForRemove
    //

    /** {@inheritDoc } */
    @Override
    public void addElement(ListItem item) {
      add(item);
    }

    /** {@inheritDoc } */
    @Override
    public void insertElementAt(ListItem item, int index) {
      add(index, item);
    }

    /** {@inheritDoc } */
    @Override
    public void removeElement(Object obj) {
      remove(obj);
      if (!(obj instanceof ListItem)) {
        logger.log(WARNING, () -> "ComboBox#removeElement(" + obj + ") not SSListItem");
      }
    }

    /** {@inheritDoc } */
    @Override
    public void removeElementAt(int index) {
      remove(index);
    }

    //
    // DefaultComboBoxModel (make sure Vector never gets referenced)
    //

    /** {@inheritDoc} */
    @Override
    public void addAll(int index, Collection<? extends ListItem> c) {
      internalAddAll(index, c);
    }

    /** {@inheritDoc} */
    @Override
    public void addAll(Collection<? extends ListItem> c) {
      internalAddAll(c);
    }

    /** {@inheritDoc } */
    @Override
    public void removeAllElements() {
      clear();
    }

    /** {@inheritDoc } */
    @Override
    public int getIndexOf(Object anObject) {
      return itemList.indexOf(anObject);
    }
  }

  //
  // Helper methods for adjusting selected
  //

  /**
   * After adding what becomes the only list item in the list
   * adjust the selection if... See DefaultComboBoxModel
   *
   * @param item the item just added
   */
  private void comboAdjustSelectedAfterAdd(ListItem item) {
    if (comboBoxModel) {
      if (itemList.size() == 1 && modelProxy.selectedObject == null && item != null) {
        modelProxy.setSelectedItem(item);
      }
    }
  }

  /**
   * After adding to a list that was empty,
   * adjust the selection if... See DefaultComboBoxModel
   *
   * @param oldSize the size before additions
   */
  private void comboAdjustSelectedAfterAdd(int oldSize) {
    if (comboBoxModel) {
      if (oldSize == 0) {
        if (itemList.size() >= 1 && modelProxy.selectedObject == null) {
          ListItem item = itemList.get(0);
          if (item != null) {
            modelProxy.setSelectedItem(item);
          }
        }
      }
    }
  }

  private void comboAdjustSelectedForRemove(int index) {
    if (comboBoxModel) {
      if (modelProxy.getElementAt(index) == modelProxy.selectedObject) {
        if (index == 0) {
          modelProxy.setSelectedItem(
              modelProxy.getSize() == 1 ? null : modelProxy.getElementAt(index + 1));
        } else {
          modelProxy.setSelectedItem(modelProxy.getElementAt(index - 1));
        }
      }
    }
  }

  private void comboAdjustSelectedForClear() {
    // NOTE seems like should throw change event,
    // but guess it's covered by the remove event.
    modelProxy.selectedObject = null;
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // ListInfo methods
  //
  // Note that the methods that modify the item list are private
  // and fire events as needed.
  // They are exposed through Remodel.
  //

  /**
   * This is used to configure the number of elements in an SSListItem.
   * The itemList must be empty.
   *
   * @param nElems number of elements, 1,2,3,4-30 supported
   */
  private void setupNumElems(int nElems) {
    if (!itemList.isEmpty()) {
      throw new IllegalArgumentException(
          "Only change number of items in a ListItem, when SSItemList is empty");
    }
    if (nElems < 1 || nElems > 30) {
      throw new IllegalArgumentException("Only [1:30] items in a ListItem handled, not " + nElems);
    }
    validElemsMask = 0;
    Class<?> clazz = switch (nElems) {
      case 1 -> ListItem1.class;
      case 2 -> ListItem2.class;
      case 3 -> ListItem3.class;
      default -> ListItemAsArray.class;
    };
    try {
      listItemConstructor = clazz.getConstructor((new Object[0]).getClass());
    } catch (NoSuchMethodException | SecurityException ex) {
      throw new RuntimeException("SSAbstractListInfo impossible", ex);
    }
    validElemsMask = (1 << nElems) - 1;

    // Validate or invalidate any existing slices.
    // Also toss cleared references.
    for (Iterator<WeakReference<ItemElementSlice>> iterator = createdLists.iterator();
         iterator.hasNext();) {
      WeakReference<ItemElementSlice> elRef = iterator.next();
      ItemElementSlice el = elRef.get();
      if (el == null) {
        iterator.remove();
        continue;
      }
      el.isValid = el.elemIndex < nElems;
    }
  }

  /**
   * Retire any list slice that was garbage collected.
   * Usually used for testing.
   * A subclass may use this if it wants the affect.
   * @return number of active/referenced list slices
   */
  protected int checkCreatedLists() {
    // toss cleared references.
    for (Iterator<WeakReference<ItemElementSlice>> iterator = createdLists.iterator();
         iterator.hasNext();) {
      WeakReference<ItemElementSlice> elRef = iterator.next();
      ItemElementSlice el = elRef.get();
      if (el == null) {
        iterator.remove();
      }
    }
    return createdLists.size();
  }

  /** not used. package access for testing */
  static class SliceInfo {
    final int elemIndex;
    final boolean isValid;

    /**
     * state info
     * @param elemIndex elem index
     * @param isValid true if it's valid
     */
    public SliceInfo(int elemIndex, boolean isValid) {
      this.elemIndex = elemIndex;
      this.isValid = isValid;
    }

    /**
     * debug
     * @return string
     */
    @Override
    public String toString() {
      return "SliceInfo{"
          + "elemIndex=" + elemIndex + ", isValid=" + isValid + '}';
    }
  }
  /**
   * Some state of slice. Not used. Package access for testing.
   * @param l slice
   * @return state info
   */
  final SliceInfo sliceInfo(List<Object> l) {
    if (l instanceof ItemElementSlice slice) {
      return new SliceInfo(slice.elemIndex, slice.isValid);
    }
    return null;
  }

  /**
   * Get a read only reference to the item list managed by this container.
   * <p>
   * Note this is not locked.
   * @return the item list
   */
  public List<ListItem> getItemList() {
    return readOnlyItemList;
  }

  /**
   * @return number of elements in an ListItem
   */
  public int getItemNumElems() {
    return itemNumElems;
  }

  /**
   * Configure the number of elements contained in an SSListItem.
   * An exception is thrown if the item list is not empty.
   * ElementSlices are marked valid/invalid as appropriate.
   * @param itemNumElems number of elements in ListItem
   */
  protected void setItemNumElems(int itemNumElems) {
    try (Remodel _ = getRemodel()) {
      setupNumElems(itemNumElems);
    }
    this.itemNumElems = itemNumElems;
  }

  /**
   * Create a list item.
   * Exception if the number of elems does not match
   * the number of elements configured with
   * {@link #setItemNumElems } or constructor.
   * @param elems elems, in order, to set into the ListItem
   * @return created list item
   */
  protected ListItem createListItem(Object... elems) {
    if (elems.length != itemNumElems) {
      throw new IllegalArgumentException(
          "Only " + itemNumElems + " elements accpeted in a ListItem, not " + elems.length);
    }
    try {
      return (ListItem) listItemConstructor.newInstance((Object) elems);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
      throw new RuntimeException("SSAbstractListInfo impossible", ex);
    }
  }

  /**
   * From the list item at the specified list item index,
   * get the element at the specified element position.
   * @param listItemIndex index of the list item
   * @param elemIndex which element to extract
   * @return the element extracted from the list item.
   */
  private Object getElem(int listItemIndex, int elemIndex) {
    return getElem(itemList.get(listItemIndex), elemIndex);
  }

  /**
   * Get an element from the list item at the specified position.
   * @param listItem extract an element from this
   * @param elemIndex which element to extract
   * @return the element extracted from the list item.
   */
  private static Object getElem(ListItem listItem, int elemIndex) {
    return ((ListItem0) listItem).getElem(elemIndex);
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // Modifications, list or item.
  //
  // Typically should only be invoked from constructor or remodel.
  //
  // Exception is some combox model methods invoke directly.
  // If the models want locking, thier methods must handle locking
  //

  private boolean add(ListItem listItem) {
    int addAt = itemList.size();
    boolean isChanged = itemList.add(listItem);
    if (isChanged) {
      modelProxy.fire.doFireIntervalAdded(this, addAt, addAt);
      comboAdjustSelectedAfterAdd(listItem);
    }
    return isChanged;
  }

  private void add(int index, ListItem listItem) {
    itemList.add(index, listItem);
    modelProxy.fire.doFireIntervalAdded(this, index, index);
    comboAdjustSelectedAfterAdd(listItem);
  }

  private boolean internalAddAll(Collection<? extends ListItem> newItems) {
    // first new item goes here
    int oldSize = itemList.size();
    boolean isChanged = itemList.addAll(newItems);
    if (isChanged) {
      modelProxy.fire.doFireIntervalAdded(this, oldSize, itemList.size() - 1);
      comboAdjustSelectedAfterAdd(oldSize);
    }
    return isChanged;
  }

  private boolean internalAddAll(int index, Collection<? extends ListItem> newItems) {
    boolean isChanged = itemList.addAll(index, newItems);
    int oldSize = itemList.size();
    if (isChanged) {
      modelProxy.fire.doFireIntervalAdded(this, index, index + newItems.size() - 1);
      comboAdjustSelectedAfterAdd(oldSize);
    }
    return isChanged;
  }

  private ListItem set(int index, ListItem newItem) {
    ListItem oldVal = itemList.set(index, newItem);
    modelProxy.fire.doFireContentsChanged(this, index, index);
    return oldVal;
  }

  private void clear() {
    comboAdjustSelectedForClear();
    if (!itemList.isEmpty()) {
      int firstIndex = 0;
      int lastIndex = itemList.size() - 1;
      itemList.clear();
      modelProxy.fire.doFireIntervalRemoved(this, firstIndex, lastIndex);
    }
  }

  private ListItem remove(int index) {
    comboAdjustSelectedForRemove(index);
    ListItem item = itemList.remove(index);
    modelProxy.fire.doFireIntervalRemoved(this, index, index);
    return item;
  }

  private boolean remove(Object listItem) {
    int index = itemList.indexOf(listItem);
    if (index < 0) {
      return false;
    }
    // following fires event
    remove(index);
    return true;
  }

  /**
   * Using the list item at the specified list item index,
   * replace an element in the list item at the specified position.
   * @param listItemIndex operate on the list item at this index
   * @param elemIndex index of elem to replace
   * @param newElem elem to put into the list item
   * @return the previous contents of the list item at the specified position
   */
  // NOTE: setElem(listItem, elemIndex, newElem)
  // is a problem because this method wants to modify something inside
  // of a listItem, so the listItem's identity, via '==',
  // doesn't change. Best way to insure that changes are
  // correctly detected is to make a listItem immutable.
  // So make a copy/clone of the listItem.
  //
  // I think this behavior can be conditional on whether or not
  // glazed is used or if this is actually installed as a model.
  // But it doesn't seem worth the testing for a miniscule performance gain.
  // Don't want to fire everything changed since that's
  // probably be a big performance loss in most cases.
  private Object setElem(int listItemIndex, int elemIndex, Object newElem) {
    ListItemWrite0 listItem = (ListItemWrite0) itemList.get(listItemIndex);
    try {
      listItem = (ListItemWrite0) listItem.clone();
    } catch (CloneNotSupportedException ex) {
    }
    Object oldElem = listItem.getElem(elemIndex);
    listItem.setElem(elemIndex, newElem);
    itemList.set(listItemIndex, listItem);
    modelProxy.fire.doFireContentsChanged(this, listItemIndex, listItemIndex);
    return oldElem;
  }

  /**
   * for testing
   * @param listItem clone this
   * @return clone
   */
  ListItem getClone(ListItem listItem) {
    try {
      return (ListItem) ((ListItemWrite0) listItem).clone();
    } catch (CloneNotSupportedException ex) {
    }
    return null;
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // Slices
  //

  /**
   * This doesn't lock. Is that a problem?
   */
  private class ItemElementSlice extends AbstractList<Object> {
    private final int elemIndex;
    private final boolean isReadOnly = true; // NEVER TURN THIS OFF
    private boolean isValid;

    public ItemElementSlice(int objectIndex) {
      this.elemIndex = objectIndex;
      isValid = (validElemsMask & (1 << elemIndex)) != 0;
    }

    private boolean isShadow(AbstractComboBoxListSwingModel model) {
      return AbstractComboBoxListSwingModel.this == model;
    }

    @Override
    public Object get(int index) {
      checkValid();
      return getElem(index, elemIndex);
    }

    @Override
    public int size() {
      checkValid();
      return itemList.size();
    }

    // THIS IS A NICE IDEA, IT DOES THROW EVENTS. BUT TOO EASY
    // TO CIRCUMVENT LOCKING WHILE MODIFYING
    @Override
    public Object set(int index, Object element) {
      checkValid();
      if (isReadOnly) {
        return super.set(index, element);
      }
      return setElem(index, elemIndex, element);
    }

    private void checkValid() {
      if (!isValid) {
        throw new IllegalAccessError(
            sf("SSListItem element slice %d must be in [0:%d]", elemIndex, itemNumElems - 1));
      }
    }
  }

  /**
   * Create a list slice of the item list. There is no checking on the element index.
   * <p>
   * If a returned element slice is not valid according to the
   * itemNumElems property, {@link #setItemNumElems},
   * then an attempt to use that slice
   * causes an exception. An element slice becomes valid/invalid
   * dynamically as itemNumElems changes.
   * @param <T> list type
   * @param elemIndex position in {@code ListItem} of elements
   * @return list of elements at the specified position
   */
  protected <T> List<T> createElementSlice(int elemIndex) {
    if (elemIndex < 0) {
      throw new IllegalArgumentException("elemIndex must be positive");
    }
    ItemElementSlice el = new ItemElementSlice(elemIndex);
    createdLists.add(new WeakReference<>(el));
    @SuppressWarnings("unchecked")
    List<T> l = (List<T>) el;
    return l;
  }

  /**
   * Determine if the argument list is a slice of this item list.
   * A slice is a live list of an SSListItem element;
   * it is backed by an item list.
   * <p>
   * Note that if the list is a slice from a different item list,
   * then false is returned.
   * @param list check this list
   * @return true if the specified list is backed by this
   */
  public boolean hasShadow(List<?> list) {
    if (list instanceof ItemElementSlice slice) {
      return slice.isShadow(this);
    }
    return false;
  }

  /**
   * This method checks if the specified list is
   * a shadow of this itemlist.
   * If it is a shadow, then a copy of the list is created.
   * @see #hasShadow(java.util.List)
   * @param <T> type of list element
   * @param list list to check
   * @return a list disconnected from the item list.
   */
  public <T> List<T> getDisconnectedList(List<T> list) {
    return hasShadow(list) ? new ArrayList<>(list) : list;
  }

  /**
   * For debug.
   * @return the item list as a string
   */
  public String dump() {
    return itemList.stream()
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
        .toString();
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // Remodel
  //

  /**
   * This is called by {@link Remodel#verifyOpened()} which
   * is called by every method in Remodel. For debug or otherwise
   * consistency checks can be placed in this method.
   */
  protected abstract void checkState();

  /**
   * Remodel may be called from a remodel, this depends on reenterant
   * locks.
   * There may be more than one remodel active at a time and they
   * may be nested. They all share the same lock (if there's locking).
   * These counters essentially count the reentrant 
   */
  private int countLocksHeld;
  private int maxLocksHeld;

  /**
   * Track the depth of taken reentrent lock, and adjust remodel.isClosed.
   * This is invoked during Remodel construction, be careful.
   * If a sub-class has real locking, the lock should be taken
   * before super... call to here.
   * This method is responsible for incrementing countOpens
   * Other than that, if there is no locking then implement an empty method
   * @param remodel if null, then manual take
   */
  protected void remodelTakeWriteLock(Remodel remodel) {
    if (++countLocksHeld > maxLocksHeld)
      maxLocksHeld = countLocksHeld;
    if (remodel != null)
      remodel.isClosed = false;
  }

  /**
   * Track the depth of taken reentrent lock, and adjust remodel.isClosed.
   * This is invoked during Remodel close.
   * <p>
   * This method is responsible for decrementing countOpens
   * {@code remodel.isClosed = true}
   * to prevent re-use
   * @param remodel if null, then manual release
   */
  protected void remodelReleaseWriteLock(Remodel remodel) {
    countLocksHeld--;
    if (countLocksHeld < 0)
      throw new IllegalStateException("Too many remodel unlocks: "+objectID(remodel));
    if (countLocksHeld == 0) {
      logger.log(DEBUG, () ->
          sf("Remodel %s unlocked, max %d", objectID(remodel), maxLocksHeld));
      maxLocksHeld = 0;
    }
    if (remodel != null) {
      if (remodel.isClosed)
        throw new IllegalStateException("Remodel already closed: "+objectID(remodel));
      remodel.isClosed = true;
    }
  }

  /**
   * For debug. Throw if any locks are held.
   * @param remodel 
   */
  public void verifyNoLocksHeld(Remodel remodel) {
    if (countLocksHeld != 0)
      throw new IllegalStateException(
          sf("%s: countLocksHeld %d", objectID(remodel), countLocksHeld));
    if (remodel != null && !remodel.isClosed)
      throw new IllegalStateException(sf("%s: is not closed", objectID(remodel)));
  }

  /**
   * For debug. Return count of locks held (reentrant).
   * @return 
   */
  public int checkLocksHeld() {
    return countLocksHeld;
  }


  /**
   * This returns a Remodel which has method for
   * reading and writing the itemList and its contained listItems.
   * <p>
   * This is typically used in a try with resources
   * {@snippet :
   *     class ListInfo extends XxxListInfo {...}
   *
   *     try (ListInfo.Remodel remodel = listInfo.getRemodel()) {
   *     	// examine and modify the list info
   *     	if (!remodel.isEmpty()) { ... }
   *     	else { ... }
   *     }
   * }
   *
   * @return a Remodel permit
   */
  protected abstract Remodel getRemodel();

  // /**
  //  * This must be called after a modification.
  //  */
  // protected void buildEventListItems() {
  // 	// FOR NOW SIMPLY USE THE MAPPING, primary key, as the eventListItem
  // 	// eventList.clear();
  // 	// eventList.add(mappings);
  // }
  // TODO: class Inspect, similar to Remodel, with only read data.

  /**
   * This provides methods to perform inspections and
   * modification of XxxListInfo.
   * It is anticipated that subclass may want to use Remodel
   * as part of a locking scheme for multi-threaded
   * access to AbstractComboBoxListSwingModel.
   * The {@link #verifyOpened()}
   * method is useful for that.
   * Typically methods in a subclass invoke
   * super.someMethod which does {@code verifyOpened()}.
   * If a method in a subclass directly modifies
   * the item list or its contents it should call verifyOpened
   * as its first statement; this avoids modifications
   * after the lock is released.
   * <p>
   * This pattern should be used when working with the list to guarantee
   * exclusive access. It avoids many potential synchronization problems.
   * {@snippet lang="java" :
   *     try (Model.remodel remodel = keyVis.getRemodel()) {
   *         // ...
   *     }
   * }
   * When there is no locking, getRemodel() is fast; there is minimal overhead.
   * <p>
   * See {@link GlazedListsKeyDisplayValueInfo} for example of Remodel locking
   */
  protected abstract class Remodel implements AutoCloseable {
    /**
     * Has this been closed? Error if access while closed.
     * Start closed, gets opened when the lock is taken.
     */
    protected boolean isClosed = true;
    /** Track number of opens to detect usage when not open errors. */

    // /** if optimized indexOfItem, following means must rebuild optimizations */
    // protected boolean isModifiedLength = false;

    /** a Remodel */
    protected Remodel() {
      remodelTakeWriteLock(this);
    }

    /**
     * First statement of each method. Prevents use after close/unlock.
     *
     * @throws IllegalStateException if this Remodel is closed
     */
    protected void verifyOpened() {
      if (isClosed) {
        throw new IllegalStateException("Remodel completed; can not reuse: "+objectID(this));
      }
      checkState();
    }

    // /** if optimized indexOfItem, following means must rebuild optimizations */
    // protected boolean isModifiedLength = false;

    /**
     * Get a read only reference to the item list managed by this container
     * @return the item list
     */
    public List<ListItem> getItemList() {
      verifyOpened();
      return readOnlyItemList;
    }

    /** @return true if there are no list items in the item list */
    public boolean isEmpty() {
      verifyOpened();
      return itemList.isEmpty();
    }

    /**
     * @param listItem listItem to find in the itemList
     * @return index of listItem in the itemList
     */
    public int indexOf(Object listItem) {
      verifyOpened();
      return itemList.indexOf(listItem);
    }

    /**
     * Return the list item at the specified position in this item list.
     * @param index index of the item to return
     * @return the item at the specified position
     */
    public ListItem get(int index) {
      verifyOpened();
      return itemList.get(index);
    }

    /**
     * Appends the specified list item to the end of this list.
     * @param newItem item to be appended to this list
     * @return true if the item was appended
     */
    public boolean add(ListItem newItem) {
      verifyOpened();
      return AbstractComboBoxListSwingModel.this.add(newItem);
      // isModifiedLength = true;
    }

    /**
     * Inserts the specified list item at the specified position in this list.
     * @param index index at which the specified item is to be inserted
     * @param newItem list item to inserted
     */
    public void add(int index, ListItem newItem) {
      verifyOpened();
      AbstractComboBoxListSwingModel.this.add(index, newItem);
      // isModifiedLength = true;
    }

    /**
     * Appends all of the list items in the specified list
     * to the end of this list.
     * @param newItems items to add to this list.
     * @return true if the list changed
     */
    public boolean addAll(Collection<? extends ListItem> newItems) {
      verifyOpened();
      return AbstractComboBoxListSwingModel.this.internalAddAll(newItems);
      // isModifiedLength = true;
    }

    /**
     * Appends all of the list items in the specified list
     * to the end of this list.
     *
     * @param index insert the items at this position in the list
     * @param newItems items to add to this list.
     * @return true if the list changed
     */
    public boolean addAll(int index, Collection<? extends ListItem> newItems) {
      verifyOpened();
      return AbstractComboBoxListSwingModel.this.internalAddAll(index, newItems);
      // isModifiedLength = true;
    }

    /**
     * Replaces the item at the specified position in the list
     * with the specified item.
     * @param index index of the item to replace
     * @param newItem item to store at the index
     * @return the list item that was at that position
     */
    public ListItem set(int index, ListItem newItem) {
      verifyOpened();
      return AbstractComboBoxListSwingModel.this.set(index, newItem);
    }

    /** remove all list items from the itemList */
    public void clear() {
      verifyOpened();
      AbstractComboBoxListSwingModel.this.clear();
      // isModifiedLength = true;
    }

    /**
     * Remove the listItem at the specified position from the itemList.
     * @param index remove listItem at this position
     * @return the listItem that was removed from the list
     */
    public ListItem remove(int index) {
      verifyOpened();
      ListItem item = AbstractComboBoxListSwingModel.this.remove(index);
      // isModifiedLength = true;
      return item;
    }

    /**
     * Remove the listItem from ite itemList.
     * @param listItem item to remove from item list
     * @return true if the item was removed
     */
    public boolean remove(Object listItem) {
      verifyOpened();
      return AbstractComboBoxListSwingModel.this.remove(listItem);
      // isModifiedLength = true;
    }

    /**
     * From the list item at the specified list item index,
     * get the element at the specified element position.
     * @param listItemIndex index of the list item
     * @param elemIndex which element to extract
     * @return the element extracted from the list item.
     */
    public Object getElem(int listItemIndex, int elemIndex) {
      verifyOpened();
      return AbstractComboBoxListSwingModel.this.getElem(listItemIndex, elemIndex);
    }

    /**
     * From the list item,
     * get the element at the specified element position.
     * @param listItem the list item
     * @param elemIndex which element to extract
     * @return the element extracted from the list item.
     */
    public Object getElem(ListItem listItem, int elemIndex) {
      verifyOpened();
      return AbstractComboBoxListSwingModel.getElem(listItem, elemIndex);
    }

    /**
     * With the list item at the specified list item index,
     * replace an element in the list item at the specified position.
     * @param listItemIndex operate on the list item at this index
     * @param elemIndex index of elem to replace
     * @param newElem elem to put into the list item
     * @return the previous contents of the list item at the specified position
     */
    public Object setElem(int listItemIndex, int elemIndex, Object newElem) {
      verifyOpened();
      return AbstractComboBoxListSwingModel.this.setElem(listItemIndex, elemIndex, newElem);
    }

    /**
     * Release write lock.
     * Mark this Remodel closed, any further method invocations
     * throw an exception.
     */
    @Override
    final public void close() {
      // if (isModifiedLength) {
      // 	// TODO: rethink this
      // 	// NOT NEEDED UNTIL OPTIMIZATIONS
      // 	// buildEventListItems();
      // }
      remodelReleaseWriteLock(this);
    }
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // The ListItem container interface
  // and containers for an ListItem
  //

  /**
   * The access methods for an SSListItem.
   * An SSListItem typically has 1, 2 or 3 Objects in it.
   * The first Object is often a key column value, the next
   * 1 or 2 objects are typically database column values related to the key.
   */
  protected interface ListItem0 extends ListItem {
    /**
     * Get an item from the SSListItem.
     * <p>
     * Typically index == 0 is a primary key
     *
     * @param index which item to get
     * @return the object from the ListItem
     */
    Object getElem(int index);

    /**
     * @return a clone
     * @throws CloneNotSupportedException
     */

    Object clone() throws CloneNotSupportedException;
  }

  private interface ListItemWrite0 extends ListItem0, Cloneable {
    /**
     * Put an object into the ListItem.
     * @param index which item to set
     * @param object the object to put into the ListItem
     */
    void setElem(int index, Object object);
  }

  //
  // DO NOT LOOK BELOW THIS LINE
  //
  /////////////////////////////////////////////////////////////////////
  //
  // Use one of the following object, ListItem2 or ListItem3,
  // for an ListItem depending on 2 or 3 objects in the list item.
  // Note that this is smaller that having an array.
  //
  // If want to handle 4 objects, might use an array. See ListItemAsArray below
  // That's a container object + 4 data object + array object = 6 objects;
  // around 17% of storage, the array object, is unneeded overhead.
  //

  /**
   * An ListItem with 1 Objects.
   */
  private static class ListItem1 implements ListItemWrite0, Cloneable {
    Object arg0;

    @SuppressWarnings("unused")
    // TODO Unused warning is a false positive. Used by reflection.
    public ListItem1(Object[] elems) {
      arg0 = elems[0];
    }

    private static void checkIndex(int index) {
      if (index != 0) {
        throw new ArrayIndexOutOfBoundsException("Only 0 index available for this ListItem, not "
                                                 + index);
      }
    }

    @Override
    public Object getElem(int index) {
      checkIndex(index);
      return arg0;
    }

    @Override
    public void setElem(int index, Object object) {
      checkIndex(index);
      arg0 = object;
    }

    @Override
    public String toString() {
      return "{" + arg0 + '}';
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 67 * hash + Objects.hashCode(this.arg0);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final ListItem1 other = (ListItem1) obj;
      return Objects.equals(this.arg0, other.arg0);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }

  /**
   * An ListItem with 2 Objects.
   */
  private static class ListItem2 implements ListItemWrite0, Cloneable {
    Object arg0;
    Object arg1;

    @SuppressWarnings("unused")
    // TODO Unused warning is a false positive. Used by reflection.
    public ListItem2(Object[] elems) {
      arg0 = elems[0];
      arg1 = elems[1];
    }

    private static void checkIndex(int index) {
      if ((0b011 & (1 << index)) == 0) {
        throw new ArrayIndexOutOfBoundsException(
            "Only 0 or 1 index available for this ListItem, not " + index);
      }
    }

    @Override
    public Object getElem(int index) {
      checkIndex(index);
      return index == 0 ? arg0 : arg1;
    }

    @Override
    public void setElem(int index, Object object) {
      checkIndex(index);
      if (index == 0)
        arg0 = object;
      else
        arg1 = object;
    }

    @Override
    public String toString() {
      return "{" + arg0 + "," + arg1 + '}';
    }

    @Override
    public int hashCode() {
      int hash = 3;
      hash = 19 * hash + Objects.hashCode(this.arg0);
      hash = 19 * hash + Objects.hashCode(this.arg1);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final ListItem2 other = (ListItem2) obj;
      if (!Objects.equals(this.arg0, other.arg0)) {
        return false;
      }
      return Objects.equals(this.arg1, other.arg1);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }

  /**
   * An ListItem with 3 Objects.
   */
  private static class ListItem3 implements ListItemWrite0, Cloneable {
    Object arg0;
    Object arg1;
    Object arg2;

    @SuppressWarnings("unused")
    // TODO Unused warning is a false positive. Used by reflection.
    public ListItem3(Object[] elems) {
      arg0 = elems[0];
      arg1 = elems[1];
      arg2 = elems[2];
    }

    private static void checkIndex(int index) {
      if ((0b0111 & (1 << index)) == 0) {
        throw new ArrayIndexOutOfBoundsException(
            "Only 0, 1 or 2 index available for this ListItem, not " + index);
      }
    }

    @Override
    public Object getElem(int index) {
      checkIndex(index);
      return index == 0 ? arg0 : index == 1 ? arg1 : arg2;
    }

    @Override
    public void setElem(int index, Object object) {
      checkIndex(index);
      switch (index) {
        case 0 -> arg0 = object;
        case 1 -> arg1 = object;
        default -> arg2 = object;
      }
    }

    @Override
    public String toString() {
      return "{" + arg0 + "," + arg1 + "," + arg2 + '}';
    }

    @Override
    public int hashCode() {
      int hash = 3;
      hash = 47 * hash + Objects.hashCode(this.arg0);
      hash = 47 * hash + Objects.hashCode(this.arg1);
      hash = 47 * hash + Objects.hashCode(this.arg2);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final ListItem3 other = (ListItem3) obj;
      if (!Objects.equals(this.arg0, other.arg0)) {
        return false;
      }
      if (!Objects.equals(this.arg1, other.arg1)) {
        return false;
      }
      return Objects.equals(this.arg2, other.arg2);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }

  // This can be used for an arbitrary, but fixed, number of items
  // in an ListItem. Typically 4 or more items.
  private static class ListItemAsArray implements ListItemWrite0, Cloneable {
    Object[] elems;

    @SuppressWarnings("unused")
    // TODO Unused warning is a false positive. Used by reflection.
    public ListItemAsArray(Object[] elems) {
      this.elems = Arrays.copyOf(elems, elems.length);
    }

    @Override
    public Object getElem(int index) {
      return elems[index];
    }

    @Override
    public void setElem(int index, Object object) {
      elems[index] = object;
    }

    @Override
    public String toString() {
      return "{" + Arrays.toString(elems) + '}';
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 83 * hash + Arrays.deepHashCode(elems);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final ListItemAsArray other = (ListItemAsArray) obj;
      return Arrays.deepEquals(elems, other.elems);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      ListItemAsArray clone = (ListItemAsArray) super.clone();
      clone.elems = elems.clone();
      return clone;
    }
  }
}
