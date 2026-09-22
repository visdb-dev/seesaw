/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */
package snippet_files;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.SQLException;
import java.util.EventListener;

import javax.swing.JCheckBox;

import dev.visdb.seesaw.utils.SsComponent;

/**
 * x
 */
@SuppressWarnings("serial")
// @start region=hook_example
public class MyCheckBox extends JCheckBox implements SsComponent {
  class MyCheckBoxListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent ie) {
      // update the database with the new value
      try {
        dbChange(() -> setColumnObject( isSelected())); // @link substring="setColumnObject" target="SsComponent#setColumnObject" @link substring="dbChange" target="SsComponent#dbChange"
      } catch (SQLException ex) {
        log(xxx); // @replace regex='xxx' replacement="..."
      }
    }
  }
  MyCheckBox() {
    finishSsCommon(); // @link substring="finishSsCommon" target="SsComponent#finishSsCommon"
  }
  @Override
  public void cleanField() {
    setSelected(false);
  }
  // ...
  private Hook hook;
  @Override
  public final Hook getSsComponentHook() {
    if (hook == null)
      hook = new Hook(this) {
        @Override
        protected void updateSsComponent() {
          Boolean value = getColumnObject(Boolean.class);
          setSelected(value == null ? false : value);
        }
        @Override
        protected MyCheckBoxListener getSsComponentListener() {
          return new MyCheckBoxListener();
        }
        @Override
        protected void addSsComponentListener(EventListener eventListener) {
          addItemListener((ItemListener) eventListener);
        }
        @Override
        protected void removeSsComponentListener(EventListener eventListener) {
          removeItemListener((ItemListener) eventListener);
        }
      };
    return hook;
  }
  // @end region=hook_example

  int xxx;
  void log(int x) {}
}
// vi: sw=2 ts=8
