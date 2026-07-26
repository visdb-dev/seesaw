/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */
package snippet_files;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.SQLException;
import java.util.EventListener;

import javax.swing.JCheckBox;

import com.nqadmin.swingset.utils.SSComponent;

/**
 * x
 */
@SuppressWarnings("serial")
// @start region=hook_example
public class MyCheckBox extends JCheckBox implements SSComponent {
  class MyCheckBoxListener implements ItemListener {
    @Override
    public void itemStateChanged(final ItemEvent ie) {
      // update the database with the new value
      try {
        dbChange(
            ()
                -> setColumnObject(
                    isSelected())); // @link substring="setColumnObject" target="SSComponent#setColumnObject" @link substring="dbChange" target="SSComponent#dbChange"
      } catch (SQLException ex) { log(xxx); } // @replace regex='xxx' replacement="..."
    }
  }
  MyCheckBox() {
    finishSSCommon();
  } // @link substring="finishSSCommon" target="SSComponent#finishSSCommon"
  @Override
  public void cleanField() {
    setSelected(false);
  }
  // ...
  private Hook hook;
  @Override
  public final Hook getSSComponentHook() {
    if (hook == null)
      hook = new Hook(this) {
        @Override
        protected void updateSSComponent() {
          Boolean value = getColumnObject(Boolean.class);
          setSelected(value == null ? false : value);
        }
        @Override
        protected MyCheckBoxListener getSSComponentListener() {
          return new MyCheckBoxListener();
        }
        @Override
        protected void addSSComponentListener(EventListener eventListener) {
          addItemListener((ItemListener) eventListener);
        }
        @Override
        protected void removeSSComponentListener(EventListener eventListener) {
          removeItemListener((ItemListener) eventListener);
        }
      };
    return hook;
  }
  // @end region=hook_example

  int xxx;
  void log(int x) {}
}
