/* *****************************************************************************
 * Copyright (C) 2025-2026, Ernie R Rael. All rights reserved.
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
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/

package dev.visdb.seesaw.utils;

import java.lang.System.Logger;
import java.sql.SQLException;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import static dev.visdb.seesaw.navigate.Utils.postColumnChangeStartError;
import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * Support for use with SsComponents that extend JTextComponent.
 */
public class SsTextSupport {
  private SsTextSupport() {}

  /** Logger for component */
  private static final Logger logger = JStuff.getLogger();

  /**
   * Returns SsDocumentListener; error if not SsComponent.
   * <p>
   * Should only be called once per component.
   *
   * @param jtc Listener for this.
   * @return SsDocumentListener for a JTextComponent
   */
  public static SsDocumentListener getSsDocumentListener(JTextComponent jtc) {
    if (!(jtc instanceof SsComponent comp))
      throw new IllegalArgumentException("Not an SsComponent");
    // TODO: assert not called before
    return new SsTextSupport.SsDocumentListener(comp);
  }

  /**
   * Document listener provided for convenience for SwingSet Components that are
   * based on JTextComponents. SwingSet components that need a Document listener
   * to trigger a change to the bound RowSet should return an instance of
   * SsCommonDocumentListener() when implementing the abstract method
   * getSsComponentListener().
   * <p>
   * This listener updates the underlying RowSet when there is a change to the Document
   * object. E.g., a call to setText() on a JTextField.
   * <p>
   * removeUpdate() and insertUpdate() both call changedUpdate().
   * changedUpdate() uses counters and SwingUtilities.invokeLater() to only update
   * the display after the last method is called.
   * <p>
   * DocumentListener events generally, but not always get fired twice any time
   * there is an update to the JTextField: a removeUpdate() followed by
   * insertUpdate()
   * <a href="https://stackoverflow.com/questions/15209766/why-jtextfield-settext-will-fire-documentlisteners-removeupdate-before-change#15213813">as described here</a>.
   */
  // <a href="https://stackoverflow.com/questions/3953208/value-change-listener-to-jtextfield">partial solution</a>.
  public static class SsDocumentListener implements DocumentListener {
    /**
     * variables needed to consolidate calls to removeUpdate() and insertUpdate()
     * from DocumentListener
     */
    private int lastChange = 0;
    private int lastNotifiedChange = 0;
    private final SsCommon ssCommon;

    /**
     * Create DocumentListener for the component.
     * @param comp associated component
     */
    public SsDocumentListener(SsComponent comp) {
      this.ssCommon = comp.getSsCommon();
    }

    private void updateTextComponent() {
      String text = ((JTextComponent) ssCommon.getSsComponent()).getText();
      // update decorator per keystroke.
      // ISSUE: when decorator uses NavigateState.errorComponents,
      // as accessed through RowsModel.hasError(comp), that state may
      // still be there, since it's updated by RowSet event,
      // which comes from setColumnText.
      try {
        ssCommon.skipValidateHasError = true;
        if (!ssCommon.decorate()) {
          postColumnChangeStartError(ssCommon.getSsComponent(), text);
          return;
        }
      } finally {
        ssCommon.skipValidateHasError = false;
      }
      ssCommon.setColumnText(text);
    }

    /**
     * Coalesce events so that updateTextComponent is only called one.
     * {@inheritDoc}
     */
    @Override
    public void changedUpdate(DocumentEvent de) {
      lastChange++;
      logger.log(TRACE,
                 () -> sf("%s - changedUpdate(): lastChange=%s, lastNotifiedChange=%s",
                          ssCommon.getColumnForLog(), lastChange, lastNotifiedChange));
      // Delay updateTextComponent until all Document listeners inovked for event.
      // See: https://stackoverflow.com/questions/3953208/value-change-listener-to-jtextfield
      SwingUtilities.invokeLater(() -> {
        if (lastNotifiedChange != lastChange) {
          lastNotifiedChange = lastChange;
          try {
            ssCommon.dbChange(() -> updateTextComponent());
          } catch (SQLException ex) {
            logger.log(Logger.Level.ERROR, (String) null, ex);
          }
        }
      });
    }

    /** {@inheritDoc} */
    @Override
    public void insertUpdate(DocumentEvent de) {
      logger.log(TRACE, () -> sf("%s - insertUpdate().", ssCommon.getColumnForLog()));
      changedUpdate(de);
    }

    /** {@inheritDoc} */
    @Override
    public void removeUpdate(DocumentEvent de) {
      logger.log(TRACE, () -> sf("%s - removeUpdate().", ssCommon.getColumnForLog()));
      changedUpdate(de);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Everything following is private, targetted for removal
  //

  /**
   * For JTextField to track previous text field value.
   * Used in conjunction with {@link SsDocumentListener}.
   * <p>
   * Part of the fix for<br>
   * Text field has wrong value after error while editing<br>
   * https://github.com/bpangburn/swingset/issues/175<br>
   * Which came in with<br>
   * Fix error recovery after errors during SsTextField edit<br>
   * https://github.com/bpangburn/swingset/pull/178<br>
   *
   */
  @SuppressWarnings(value = {"serial", "unused"})
  private static class SsPlainDocument extends PlainDocument {
    DocumentFilter filter;
    private final SsCommon ssCommon;
    /**
     * Create DocumentListener for the component.
     * @param comp associated component
     */
    public SsPlainDocument(SsComponent comp) {
      this.ssCommon = comp.getSsCommon();
    }

    void capturePrevious(DocumentFilter.FilterBypass fb) {
      try {
        String prev = fb.getDocument().getText(0, fb.getDocument().getLength());
        logger.log(TRACE, () -> "Capture previous text value: " + prev);
        if (ssCommon.getEventListener() instanceof SsDocumentListenerWithRestoreOnError listener)
          listener.previousValue = prev;
      } catch (BadLocationException ex) {
        logger.log(DEBUG, "Capture previous text value", ex);
      }
    }

    /** {@inheritDoc} */
    @Override
    public DocumentFilter getDocumentFilter() {
      if (filter == null)
        filter = new DocumentFilter() {
          @Override
          public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text,
                              AttributeSet attrs) throws BadLocationException {
            capturePrevious(fb);
            super.replace(fb, offset, length, text, attrs);
          }

          @Override
          public void insertString(DocumentFilter.FilterBypass fb, int offset, String string,
                                   AttributeSet attr) throws BadLocationException {
            capturePrevious(fb);
            super.insertString(fb, offset, string, attr);
          }

          @Override
          public void remove(DocumentFilter.FilterBypass fb, int offset, int length)
              throws BadLocationException {
            capturePrevious(fb);
            super.remove(fb, offset, length);
          }
        };
      return filter;
    }
  }

  /**
   * Document listener provided for convenience for SwingSet Components that are
   * based on JTextComponents. SwingSet components that need a Document listener
   * to trigger a change to the bound RowSet should return an instance of
   * SsCommonDocumentListener() when implementing the abstract method
   * getSsComponentListener().
   * <p>
   * A typical implementation might look like: {@code
   return getSsCommon().getSsDocumentListener();
}
   * <p>
   * This listener updates the underlying RowSet when there is a change to the Document
   * object. E.g., a call to setText() on a JTextField. If the update has an error
   * the text field is reverted to the current contents of the database.
   * <p>
   * DocumentListener events generally, but not always get fired twice any time
   * there is an update to the JTextField: a removeUpdate() followed by
   * insertUpdate(). See:
   * https://stackoverflow.com/questions/15209766/why-jtextfield-settext-will-fire-documentlisteners-removeupdate-before-change#15213813
   * <p>
   * Using partial solution here from here:
   * https://stackoverflow.com/questions/3953208/value-change-listener-to-jtextfield
   * <p>
   * Having removeUpdate() and insertUpdate() both call changedUpdate().
   * changedUpdate() uses counters and SwingUtilities.invokeLater() to only update
   * the display on the last method called.
   */
  private static class SsDocumentListenerWithRestoreOnError implements DocumentListener {
    /**
     * variables needed to consolidate calls to removeUpdate() and insertUpdate()
     * from DocumentListener
     */
    private int lastChange = 0;
    private int lastNotifiedChange = 0;
    private String previousValue = null;
    /** True when listener is temporarily removed. */
    private final SsCommon ssCommon;

    /**
     * Create DocumentListener for the component.
     * @param comp associated component
     */
    public SsDocumentListenerWithRestoreOnError(SsComponent comp) {
      this.ssCommon = comp.getSsCommon();
    }

    /** {@inheritDoc} */
    @Override
    public void changedUpdate(DocumentEvent de) {
      lastChange++;
      logger.log(TRACE, () -> sf("%s - changedUpdate(): lastChange=%s, lastNotifiedChange=%s",
                                 ssCommon.getColumnForLog(), lastChange, lastNotifiedChange));
      // Delay updateTextComponent until all Document listeners inovked for event.
      // See: https://stackoverflow.com/questions/3953208/value-change-listener-to-jtextfield
      SwingUtilities.invokeLater(() -> {
        if (lastNotifiedChange != lastChange) {
          lastNotifiedChange = lastChange;
          try {
            ssCommon.dbChange(() -> updateTextComponent());
          } catch (SQLException ex) {
            logger.log(Logger.Level.ERROR, (String) null, ex);
          }
        }
      });
    }

    /** This could be in updateTextComponent (like as an array element). */
    private boolean listenerNeedsRestoration;

    private void updateTextComponent() {
      String text = ((JTextComponent) ssCommon.getSsComponent()).getText();
      // update decorator per keystroke.
      // ISSUE: when decorator uses NavigateState.errorComponents,
      // as accessed through RowsModel.hasError(comp), that state may
      // still be there, since it's updated by RowSet event,
      // which comes from setColumnText.
      try {
        ssCommon.skipValidateHasError = true;
        if (!ssCommon.decorate()) {
          postColumnChangeStartError(ssCommon.getSsComponent(), text);
          return;
        }
      } finally {
        ssCommon.skipValidateHasError = false;
      }
      boolean ok = true;
      //boolean inErrorState = ssCommon.getRowsModel().hasError(ssCommon.getSsComponent());
      try {
        ok = ssCommon.setColumnText(text);
      } finally {
        if (!ok) {
          if (ssCommon.isRestoreOnError_NOT_USED()) {
            // restore previous text value
            if (previousValue != null) {
              if (ssCommon.isSsComponentListenerAdded()) {
                // avoid generating events while restoring text
                ssCommon.removeSsComponentListener();
                listenerNeedsRestoration = true;
              }
              try {
                logger.log(DEBUG, () -> sf("%s: restoring previous value '%s'",
                                           ssCommon.getColumnForLog(), previousValue));
                ((JTextComponent) ssCommon.getSsComponent()).setText(previousValue);
              } finally {
                if (listenerNeedsRestoration) {
                  listenerNeedsRestoration = false;
                  ssCommon.addSsComponentListener();
                }
              }
              // RESTORE ERROR STATE
            }
          }
          // Things may have changed, re-decorate.
          ssCommon.decorate();
        }
        previousValue = null; // Seems safer, is this the right spot?
      }
    }

    /** {@inheritDoc} */
    @Override
    public void insertUpdate(DocumentEvent de) {
      logger.log(TRACE, () -> sf("%s - insertUpdate().", ssCommon.getColumnForLog()));
      changedUpdate(de);
    }

    /** {@inheritDoc} */
    @Override
    public void removeUpdate(DocumentEvent de) {
      logger.log(TRACE, () -> sf("%s - removeUpdate().", ssCommon.getColumnForLog()));
      changedUpdate(de);
    }

  } // end protected class SsDocumentListener
}
