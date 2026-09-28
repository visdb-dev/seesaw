/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package dev.visdb.seesaw.utils;

import java.util.List;

import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Global option set/get.
 */
// TODO: maybe add something like OptionChanged(optionClass, runnableNotify)
public class Globals {
  private Globals() { }

  private static final InstanceContent content = new InstanceContent();
  private static final AbstractLookup globals = new AbstractLookup(content);
  
  /**
   * 
   * Get the option; throws an exception if there is more than one
   * item of the specified type in the lookup.
   * @param <T> the type of the option
   * @param clazz class of the option to retrieve
   * @return
   */
  public static <T> T getOption(Class<T> clazz) {
    return getOption(globals.lookupResult(clazz));
  }
  
  /**
   * Many items in the lookup act as options and an option can only have one value.
   * This method first removes any existing values of the given option type
   * and then adds the new option value to the lookup.
   * @param <T> type of the Option to replace
   * @param clazz class of the option to remove
   * @param option the item that replaces the existing value of the option
   */
  public static <T> void setOption(Class<T> clazz, T option) {
    List<? extends T> all = (List<? extends T>)globals.lookupResult(clazz).allInstances();
    if (all.size() > 1)
      throw new IllegalStateException(JStuff.sf("Only one value, not %s", all));
    if (!all.isEmpty())
      content.remove(all.get(0));
    content.add(option);
  }

  /** Get the option from a lookup result.
   * @param <T> 
   * @param result The option's in here.
   * @return 
   */
  public static <T> T getOption(Lookup.Result<T> result) {
    List<? extends T> all = (List<? extends T>)result.allInstances();
    if (all.size() > 1)
      throw new IllegalStateException("An option can only have one value");
    return all.isEmpty() ? null : all.get(0);
  }

  /**
   * Result for option.
   * 
   * @param <T>
   * @param clazz
   * @return 
   */
  public static <T> Lookup.Result<T> lookupResult(Class<T> clazz) {
    return globals.lookupResult(clazz);

  }
}
// vi: sw=2 ts=8