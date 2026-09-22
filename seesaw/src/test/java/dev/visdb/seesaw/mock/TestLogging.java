/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2025-2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.mock;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import dev.visdb.seesaw.utils.SsUtils;

/**
 * Logging for use during tests.
 * <br>in setUpClass: isJunit(); TestLogging.load(...);
 * <br>in tearDownClass: TestLogging.flush();
 * x
 */
public class TestLogging {
  private TestLogging() {}

  /** Not for logging per se, but to configure the handler for test logging. */
  private static final Logger logger = Logger.getLogger("dev.visdb.seesaw");

  /**
   * set up logger with default level INFO.
   */
  public static void load() {
    load(null);
  }

  /**
   * x
   * @param _level use "null" for default
   */
  public static void load(Level _level) {
    Level level = _level == null ? Level.INFO : _level;
    testLogging(level);
    System.setProperty(SsUtils.JUNIT_TEST_LOGGING, "true");
  }

  /**
   * Flush our stuff, but probably not needed since our TestStreamHandler
   * flushes each record.
   */
  public static void flush() {
    for (Handler handler : logger.getHandlers()) {
      handler.flush();
    }
  }

  /**
   * x
   */
  private static void testLogging(Level level) {
    testLoggingInternal(level);
    logger.log(Level.INFO, () -> "LOGGER ON, LEVEL " + level);
  }

  /**
   * x
   * @param _level
   */
  private static void testLoggingInternal(Level level) {
    //Level logger_level = Level.ALL;
    Level logger_level = level;
    Level handler_level = level;

    logger.setUseParentHandlers(false);

    logger.setLevel(logger_level);
    Handler[] handlers = logger.getHandlers();
    if (handlers.length != 0) {
      handlers[0].setLevel(handler_level);
      return;
    }

    //String fmt = "%4$s: %5$s [%1$tc]%n";
    String fmt = "%4$s: %5$s [%2$s]%6$s%n";
    //System.setProperty("java.util.logging.SimpleFormatter.format", fmt);
    SimpleFormatter formatter = new TestFormatter(fmt);
    StreamHandler handler = new TestStreamHandler(System.out, formatter);

    logger.addHandler(handler);
    handler.setLevel(handler_level);
  }

  /** During testing don't buffer. */
  private static class TestStreamHandler extends StreamHandler {
    public TestStreamHandler(OutputStream out, Formatter formatter) {
      super(out, formatter);
    }

    @Override
    public void publish(LogRecord record) {
      super.publish(record);
      flush();
    }
  }

  /** To distinguish formatter used in JUnit tests */
  private static class TestFormatter extends SimpleFormatter {
    private final String format;
    // SurrogateLogger.getSimpleFormat(SimpleFormatter::getLoggingProperty);

    /**
     * Create a {@code SimpleFormatter}.
     * @param format
     */
    public TestFormatter(String format) {
      this.format = format;
    }

    private static final String PREFIX = "dev.visdb.seesaw";

    /**
     * Format the given LogRecord. TRIM THE SOURCE NAME.
     * See SimpleFormatter for general documentation.
     */
    @Override
    public String format(LogRecord record) {
      //return super.format(record);
      return xformat(record);
    }

    private String xformat(LogRecord record) {
      ZonedDateTime zdt = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
      String source;
      if (record.getSourceClassName() != null) {
        source = record.getSourceClassName();
        if (source.startsWith(PREFIX))
          source = "SS" + source.substring(PREFIX.length());
        if (record.getSourceMethodName() != null) {
          source += " " + record.getSourceMethodName();
        }
      } else {
        source = record.getLoggerName();
      }
      String message = formatMessage(record);
      String throwable = "";
      if (record.getThrown() != null) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
          pw.println();
          record.getThrown().printStackTrace(pw)
          ;
        }
        throwable = sw.toString();
      }
      return String.format(format, zdt, source, record.getLoggerName(),
                           record.getLevel(), //record.getLevel().getLocalizedLevelName(),
                           message, throwable);
    }
  }
}
// vi: sw=2 ts=8
