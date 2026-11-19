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
 * copyright (C) 2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/

package dev.visdb.seesaw.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import org.h2.tools.RunScript;

import dev.visdb.seesaw.models.DbCollection;
import dev.visdb.seesaw.models.DbStringCollection;
import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.JStuff;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * How the demo gets its database connection and fills the database:
 * one {@link DatabaseSetup} for each kind of database server.
 * {@link MainClass#main} picks one; the {@code MainClass} constructor runs it.
 */
final class DemoDatabase {
  private static final Logger logger = JStuff.getLogger();

  private DemoDatabase() {
  }

  static abstract class DatabaseSetup implements Runnable {
    private Connection conn;

    abstract Properties getDatabaseProperties();

    abstract List<String> getScripts();

    Connection getConnection() {
      return conn;
    }

    void setConnection(Connection conn) {
      this.conn = conn;
    }

    /**
     * Find resource; an alternate implementation could take the resource as a file.
     *
     * @param resourceName name of resource
     * @return BufferReader for specified resource
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    BufferedReader getBufferedReader(String resourceName) {
      InputStream stream = MainClass.class.getResourceAsStream(resourceName);
      if (stream == null) {
        System.err.println("Script '" + resourceName + "' not found. Exiting.");
        return null;
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(stream));
      return br;
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void run() {
      Properties info = getDatabaseProperties();
      if (info == null) {
        return;
      }
      String clazz = info.getProperty("DB_DRIVER_CLASS", "");

      if (clazz.isEmpty()) {
        System.err.println("'DB_DRIVER_CLASS' not found in property file");
        conn = null;
        return;
      }

      try {
        Class.forName(clazz);
      } catch (ClassNotFoundException ex) {
        System.err.println("Class '" + clazz + "' not found");
        conn = null;
        return;
      }

      conn = DemoUtil.getConnection(info.getProperty("DB_URL"), info);
      if (conn == null) {
        return;
      }

      if (MainClass.no_initialize_db) {
        return;
      }

      boolean ok = false;
      for (String script : getScripts()) {
        ok = false;
        BufferedReader br = getBufferedReader(script);
        if (br != null) {
          ok = DemoUtil.runSqlStatements(conn, br, MainClass.verbose);
        }
        if (!ok) {
          break;
        }
      }

      if (ok && !MainClass.no_load_images) {
        // Load up the images
        String sql = "UPDATE base_test_data"
                     + " SET ss_image = ? WHERE base_test_pk = ?";
        ok = DemoUtil.loadBinaries(conn, "/demo-images.txt", sql, MainClass.verbose);
      }

      if (!ok) {
        conn = null;
      }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    Properties getFileProperties(String fname) {
      try {
        Properties props = new Properties();
        FileReader reader = new FileReader(fname);
        props.load(reader);
        return props;
      } catch (FileNotFoundException ex) {
        System.err.println("Property file '" + fname + "' not found");
      } catch (IOException ex) {
        logger.log(Level.ERROR, "IO exception.", ex);
      }
      return null;
    }

    void dump() {
      for (String script : getScripts()) {
        boolean ok = false;
        try (BufferedReader br = getBufferedReader(script)) {
          if (br != null) {
            String name = "dump." + new File(script).getName();
            try (FileWriter fout = new FileWriter(name)) {
              char buf[] = new char[4 * 1024];
              int n;
              while ((n = br.read(buf)) > 0) {
                fout.write(buf, 0, n);
              }
            }
          }
          ok = true;
        } catch (IOException ex) {
          logger.log(Level.ERROR, "IO exception.", ex);
        }
        if (!ok) {
          break;
        }
      }
    }
  }

  static class ExternalSetup extends DatabaseSetup {
    List<String> sqlFiles;

    public ExternalSetup(List<String> _sqlFiles) {
      sqlFiles = _sqlFiles;
    }

    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    BufferedReader getBufferedReader(String fileName) {
      try {
        return new BufferedReader(new FileReader(fileName));
      } catch (FileNotFoundException ex) {
        System.err.println("Script '" + fileName + "' not found. Exiting.");
        return null;
      }
    }

    @Override
    Properties getDatabaseProperties() {
      Properties info = getFileProperties(MainClass.propertyFile);
      return info;
    }

    @Override
    List<String> getScripts() {
      return sqlFiles;
    }
  }

  /**
   * The demo's own in memory H2 database, loaded from the scripts in the
   * resources. Also used to dump the H2 sql files.
   */
  static class H2Setup extends DatabaseSetup {
    private static final String DATABASE_PATH = "//localhost/~/h2/databases/";

    private static final boolean USE_IN_MEMORY_DATABASE = true;

    private static final String DATABASE_SCRIPT_DEMO = "suppliers_and_parts.sql";
    private static final String DATABASE_SCRIPT_TEST = "tests.sql";
    private static final String DATABASE_SCRIPT_TEST_IMAGES = "tests_load_blobs.sql";
    private static final String DATABASE_SCRIPT_DEBUG = "debug.sql";

    private static final boolean RUN_DEMO_SQL_SCRIPTS = true;
    private static final boolean RUN_TEST_SQL_SCRIPTS = true;
    private static final boolean RUN_DEBUG_SQL_SCRIPTS = false;

    @Override
    Properties getDatabaseProperties() {
      return null;
    }

    /**
     * Initialize the database connection and load the database content
     * from the scripts.
     */
    @Override
    public void run() {
      Connection result = null;

      // INITIALIZE DATABASE CONNECTION AND COMPONENTS
      try {
        Class.forName("org.h2.Driver");

        logger.log(DEBUG, () -> sf("Resource path: %s.", MainClass.class.getPackage().getName()));
        logger.log(DEBUG, "Resource path: %s.",
                   MainClass.class.getClassLoader().getResource(DATABASE_SCRIPT_DEMO));

        InputStream inStreamDemo = null;
        InputStream inStreamTest = null;
        InputStream inStreamDebug = null;

        boolean has_startup_error = false;

        if (RUN_DEMO_SQL_SCRIPTS) {
          inStreamDemo = MainClass.class.getClassLoader().getResourceAsStream(DATABASE_SCRIPT_DEMO);
          if (inStreamDemo == null) {
            logger.log(Level.ERROR, () -> "Please add the file " + DATABASE_SCRIPT_DEMO
                                          + " to the classpath, package "
                                          + MainClass.class.getPackage().getName());
            has_startup_error = true;
          }
        }

        if (RUN_TEST_SQL_SCRIPTS) {
          inStreamTest = MainClass.class.getClassLoader().getResourceAsStream(DATABASE_SCRIPT_TEST);
          if (inStreamTest == null) {
            logger.log(Level.ERROR, () -> "Please add the file " + DATABASE_SCRIPT_TEST + " and "
                                          + DATABASE_SCRIPT_TEST_IMAGES
                                          + " to the classpath, package "
                                          + MainClass.class.getPackage().getName());
            has_startup_error = true;
          }
        }

        if (RUN_DEBUG_SQL_SCRIPTS) {
          inStreamDebug
              = MainClass.class.getClassLoader().getResourceAsStream(DATABASE_SCRIPT_DEBUG);
          if (inStreamDebug == null) {
            logger.log(Level.ERROR, () -> "Please add the file " + DATABASE_SCRIPT_DEBUG
                                          + " to the classpath, package "
                                          + MainClass.class.getPackage().getName());
            has_startup_error = true;
          }
        }

        if (has_startup_error) {
          logger.log(Level.ERROR, "\n*******\n*******\n*******");
          System.exit(1);
        }

        if (USE_IN_MEMORY_DATABASE) {
          result = DriverManager.getConnection("jdbc:h2:mem:" + MainClass.DATABASE_NAME
                    + Globals.getOption(MainClass.H2Trace.class).getTraceUrlFlags());
          logger.log(INFO, "Established connection to in-memory database.");
        } else {
          // ASSUMING DATABASE IS IN LOCAL ./h2/databases/ FOLDER WITH DEFAULT USERNAME OF
          // sa AND BLANK PASSWORD
          // USEFUL FOR WORKING WITH DATASET FOR SEESAW TESTS
          result = DriverManager.getConnection(
              "jdbc:h2:tcp:" + DATABASE_PATH + MainClass.DATABASE_NAME, "sa", "");
          logger.log(INFO, "Established connection to database server.");
        }

        // RUN SCRIPTS AND CLOSE STREAMS
        if (RUN_DEMO_SQL_SCRIPTS) {
          assert inStreamDemo != null;
          RunScript.execute(result, new InputStreamReader(inStreamDemo));
          inStreamDemo.close();
        }

        if (RUN_TEST_SQL_SCRIPTS) {
          assert inStreamTest != null;
          RunScript.execute(result, new InputStreamReader(inStreamTest));
          inStreamTest.close();

          if (!MainClass.no_load_images) {
            String sql = "UPDATE base_test_data"
                         + " SET ss_image = ? WHERE base_test_pk = ?";
            DemoUtil.loadBinaries(result, "/demo-images.txt", sql, MainClass.verbose);
          }
        }

        if (RUN_DEBUG_SQL_SCRIPTS) {
          assert inStreamDebug != null;
          RunScript.execute(result, new InputStreamReader(inStreamDebug));
          inStreamDebug.close();
        }

      } catch (final IOException ioe) {
        logger.log(Level.ERROR, "IO Exception.", ioe);
      } catch (final SQLException se) {
        logger.log(Level.ERROR, "SQL Exception.", se);
      } catch (final ClassNotFoundException cnfe) {
        logger.log(Level.ERROR, "Class Not Found Exception.", cnfe);
      }

      setConnection(result);
    }

    @Override
    List<String> getScripts() {
      return Arrays.asList(new String[] {"/" + DATABASE_SCRIPT_DEMO, "/" + DATABASE_SCRIPT_TEST,
                                         "/" + DATABASE_SCRIPT_TEST_IMAGES});
    }
  }

  static class MysqlSetup extends DatabaseSetup {
    @Override
    public void run() {
      super.run();
      MainClass.globalHints.put("collectionModel",
          (Supplier<DbCollection>) ()
              -> new DbStringCollection(JDBCType.INTEGER, DbStringCollection.COMMA_SEP));
      // () -> new SSStringArrayModel(JDBCType.INTEGER));
    }

    @Override
    Properties getDatabaseProperties() {
      // <dependency>
      // <groupId>mysql</groupId>
      // <artifactId>mysql-connector-java</artifactId>
      // <version>8.0.21</version>
      // </dependency>
      Properties info;
      if (MainClass.propertyFile != null) {
        info = getFileProperties(MainClass.propertyFile);
      } else {
        info = new Properties();
        info.put("DB_DRIVER_CLASS", "com.mysql.cj.jdbc.Driver");
        info.put("DB_URL", "jdbc:mysql://localhost/demo_suppliers_and_parts");
        // DB_NAME NOT USED
        info.put("DB_NAME", "demo_suppliers_and_parts");
        info.put("user", "root");
        info.put("serverTimezone", "UTC");
      }
      return info;
    }

    @Override
    List<String> getScripts() {
      return Arrays.asList(
          new String[] {
            "/mysql.demo-app.sql",
            "/mysql.demo-components.sql"
          });
    }
  }
} // end final class DemoDatabase
// vi: sw=2 ts=8
