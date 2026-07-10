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
package com.nqadmin.swingset.utils;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.lang.System.Logger;
import java.util.Optional;
import java.util.Set;

/**
 * Convenience methods for Java.
 */
public class JStuff
{
	private JStuff() { }

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
		Class<?> cc = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass();
		return getLogger(cc.getName());
	}

	/**
	 * Return the logger name for the caller.
	 * @return logger name
	 */
	public static String getLoggerName() {
		Class<?> cc = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass();
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
		Optional<StackFrame> caller = StackWalker.getInstance(
				Set.of(Option.RETAIN_CLASS_REFERENCE), skip+1).walk(s ->
						s.skip(skip)
								.findFirst());
		// if (Boolean.FALSE) {
		// 	// For a verbose mode.
		// 	StackFrame frame = caller.get();
		// 	Objects.nonNull(frame.getFileName());
		// 	Objects.nonNull(frame.getLineNumber());
		// }
		String meth = caller.isEmpty() ? null
				: caller.get().getDeclaringClass().getSimpleName() + '.' + caller.get().getMethodName();
		return meth;
	}
}
