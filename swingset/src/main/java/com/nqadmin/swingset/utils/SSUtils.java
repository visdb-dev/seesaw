/* *****************************************************************************
 * Copyright (C) 2020, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
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
 * ****************************************************************************/
package com.nqadmin.swingset.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.SharedMetricRegistries;

/**
 *
 * @since 4.0.0
 */
public class SSUtils {

	private SSUtils() {
	}

	//private static final Logger logger = LogManager.getLogger();

	/**
	 * Returns an unmodifiable list containing an arbitrary number of elements.
	 * This is not particularly efficient for small lists, but until java-9...
	 * @param <T> type of elements in the list
	 * @param args the elements of the list
	 * @return list
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	static <T> List<T> listOf(T... args) {
		Object[] arr = new Object[0];
		System.arraycopy(args, 0, arr, 0, args.length);
		return (List<T>) Collections.unmodifiableList(Arrays.asList(arr));
	}


	///////////////////////////////////////////////////////////////////////
	//
	// SwingSet metrics
	//

	/** name of the metrics */
	private static final String SWING_SET = "SwingSet";

	/**
	 * Metrics logged through this logger.
	 */
	private static final Logger loggerSwingSetMetrics = LogManager.getLogger("SwingSetMetrics");

	/**
	 * Don't expect to have more than one metrics reporter,
	 * but it doesn't cost much to handle multiple reporters.
	 */
	private static final Set<ScheduledReporter> reporters = Collections.synchronizedSet(new HashSet<>());
	private static MetricRegistry swingSetMetricsRegistry;
	private static String csvSwingSetMetricsDirectory;

	private static final boolean ENABLE_CSV_REPORTING = true;
	private static final boolean ENABLE_CONSOLE_REPORTING = true;

	 /**
	 * Returns the default registry for the SwingSet metrics
	 * and sets it up for some reporting.
	 * @return SwingSet metric registry
	 */
	public synchronized static MetricRegistry getMetrics() {
		if(swingSetMetricsRegistry == null) {
			loggerSwingSetMetrics.info("Setting up SwingSet metrics");

			// First time, set up the default SwingSet metrics registry.
			swingSetMetricsRegistry = new MetricRegistry();
			// put in the shared registry just in case...
			SharedMetricRegistries.add(SWING_SET, swingSetMetricsRegistry);

			// First time, set up reporting
			initializeReporting();
		}

		// return the swing set metric registry
		return swingSetMetricsRegistry;
	}

	private static void initializeReporting() {
		
		// TODO: how to configure reporting time paramters
		// in addition to shutdown.
		
		// Set up some reporting
		ScheduledReporter reporter;
		
		// console reporting
		if(ENABLE_CONSOLE_REPORTING) {
			reporter = ConsoleReporter.forRegistry(swingSetMetricsRegistry)
					.outputTo(new MetricsLog4jStream(loggerSwingSetMetrics))
					.convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.build();
			//
			//reporter.start(5, 10, TimeUnit.SECONDS);
			//
			requestShutdownReport(reporter);
		}
		
		if(ENABLE_CSV_REPORTING) {
			// CSV reporter, use sub dir of log4j's directory
			File csvDir = getCsvMetricsDirectory();
			if(csvDir == null) {
				loggerSwingSetMetrics.info("Unable to determine metrics csv directory");
			} else {
				// put csv files in "data" directory under log4j directory
				csvDir.mkdir();
				loggerSwingSetMetrics.info("metrics csv directory: " + csvDir);
				reporter = CsvReporter.forRegistry(swingSetMetricsRegistry)
						.convertRatesTo(TimeUnit.SECONDS)
						.convertDurationsTo(TimeUnit.MILLISECONDS)
						.build(csvDir);
				//
				//reporter.start(5, 10, TimeUnit.SECONDS);
				//
				requestShutdownReport(reporter);
			}
		}
	}

	/**
	 * The directory where csv metrics are saved. May not exist.
	 * This is a subdirectory of log4j results.
	 * @return directory where csv metrics are saved
	 */
	public synchronized static File getCsvMetricsDirectory() {
		File dir;
		if (csvSwingSetMetricsDirectory == null) {
			dir = getFirstFileAppenderLocation(loggerSwingSetMetrics);
			if (dir != null) {
				dir = new File(dir.getAbsoluteFile(), "metrics_data");
				csvSwingSetMetricsDirectory = dir.getAbsolutePath();
			}
		} else {
			return new File(csvSwingSetMetricsDirectory);
		}
		return dir;
	}

	/**
	 * Use this to direct {@link ConsoleReporter} to log4j.
	 */
	public static class MetricsLog4jStream extends PrintStream {
		private final Logger logger;

		/**
		 * Create stream that flushes to log4j.
		 * @param logger target logger
		 */
		public MetricsLog4jStream(Logger logger) {
			super(new ByteArrayOutputStream());
			this.logger = logger;
			print('\n');
		}

		/** {@inheritDoc} */
		@Override
		public void flush() {
			super.flush();
			logger.info(out.toString());
			// Could reset, but why keep the empty array around.
			out = new ByteArrayOutputStream();
			print('\n');
		}
	}

	/**
	 * Arrange for a report during shutdown for the specified reporter.
	 * Only the first request for the specified reporter has an effect.
	 * @param scheduledReporter report this during shutdown
	 */
	public synchronized static void requestShutdownReport(ScheduledReporter scheduledReporter) {
		if (reporters.isEmpty()) {
			// Nothing queued up so far,
			// create a shutdown hook, and disable the log4j shutdown hook.

			// report the metrics at shutdown
			// Don't let log4j shutdown until metrics are reported
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {

				try {
					reporters.forEach((r) -> r.close());
				} finally {
					LogManager.shutdown();
				}
			}, "FinalSwingSetMetrics"));

			// The metrics shutdown hook is registered, LogManager.shutdown()
			// will be called after report. Disable log4j auto shutdown.
			if (!disableLog4jShutdownHook()) {
				loggerSwingSetMetrics.info("Could not disable log4j shutdown hook");
			}
		}
		reporters.add(scheduledReporter);
	}

	//
	// This is here only to avoid not used warning.
	//
	static {
		if (Boolean.FALSE) {
			getFirstFileAppenderLocation(loggerSwingSetMetrics);
			getFileAppenderLocation(loggerSwingSetMetrics, "FileAppender");
		}
	}

	private static File getFirstFileAppenderLocation(Logger _logger) {
		try {
			org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger)LogManager.getLogger(_logger);
			LoggerContext context = coreLogger.getContext();
			Configuration configuration = context.getConfiguration();
			for (Appender appender : configuration.getAppenders().values()) {
				if(appender instanceof FileAppender) {
					File f = new File(((FileAppender)appender).getFileName());
					return f.getParentFile();
				}
			}
		} catch(Exception ex) {
		}
		return null;
	}

	private static String getFileAppenderLocation(Logger _logger, String _name) {
		try {
			org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger)LogManager.getLogger(_logger);
			LoggerContext context = coreLogger.getContext();
			Configuration configuration = context.getConfiguration();
			FileAppender fa = (FileAppender)configuration.getAppender(_name);
			return fa.getFileName();
		} catch(Exception ex) {
		}
		return null;
	}

	/**
	 * Don't do this unless you arrange to do LogManager.shutdown().
	 */
	private static boolean disableLog4jShutdownHook() {
		try {
			// check if log4j-core is around
			Class.forName("org.apache.logging.log4j.core.impl.Log4jContextFactory");
		} catch (ClassNotFoundException ex) {
			return false;
		}

		final LoggerContextFactory factory = LogManager.getFactory();
		
		if (factory instanceof Log4jContextFactory) {
			Log4jContextFactory contextFactory = (Log4jContextFactory) factory;
			
			((DefaultShutdownCallbackRegistry) contextFactory.getShutdownCallbackRegistry()).stop();
			return true;
		}
		return false;
	}
}
