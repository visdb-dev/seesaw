/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.utils;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.lang.System.Logger;
import java.util.Optional;
import java.util.Set;

/**
 * Convenience methods for Java.
 */
public class JStuff {
  private JStuff() {}

  /**
   * Shorthand for "String.format(fmt, args)".
   * @param fmt format
   * @param args args
   * @return string
   */
  public static String sf(String fmt, Object... args) {
    return args.length == 0 ? fmt : String.format(fmt, args);
  }

  /**
   * Return the Logger for the caller.
   *
   * @return the Logger
   */
  public static Logger getLogger() {
    Class<?> cc = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass();
    return getLogger(cc.getName());
  }

  /**
   * Return the logger name for the caller.
   * @return logger name
   */
  public static String getLoggerName() {
    Class<?> cc = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass();
    return cc.getName();
  }

  /**
   * Return a logger for the name.
   * @param loggerName name
   * @return logger
   */
  public static Logger getLogger(String loggerName) {
    return System.getLogger(loggerName);
  }

  /**
   * Get a "class.method" name from the call stack.
   * The skip param indicates how far down the stack to look for
   * the caller's frame. For example, if {@code getCaller(skip)} is
   * used in a message supplier in a log statement need to skip more
   * than other cases:
   * {@snippet :
   *     void someMethod() {
   *         log(Level, () -> String.format("Called by: {%s}", getCaller(4)))
   *     }
   * }
   * logs the name of the method that called someMethod.
   *
   * @param skip
   * @return "simpleClassName.methodName"
   */
  public static String getCaller(int skip) {
    Optional<StackFrame> caller
        = StackWalker.getInstance(Set.of(Option.RETAIN_CLASS_REFERENCE), skip + 1)
              .walk(s -> s.skip(skip).findFirst());
    // if (Boolean.FALSE) {
    // 	// For a verbose mode.
    // 	StackFrame frame = caller.get();
    // 	Objects.nonNull(frame.getFileName());
    // 	Objects.nonNull(frame.getLineNumber());
    // }
    String meth = caller.isEmpty() ? null
                                   : caller.get().getDeclaringClass().getSimpleName() + '.'
                                         + caller.get().getMethodName();
    return meth;
  }
}
// vi: sw=2 ts=8
