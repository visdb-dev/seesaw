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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import org.h2.tools.RunScript;

import dev.visdb.seesaw.models.DbCollection;
import dev.visdb.seesaw.models.DbStringCollection;
import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.JStuff;

import static java.lang.System.Logger.Level.*;

/**
 * How the demo gets its database connection and fills the database:
 * one {@link DatabaseSetup} for each kind of database server.
 * {@link MainClass#main} picks one; the {@code MainClass} constructor runs it.
 */
final class DemoDatabase {
  private static final Logger logger = JStuff.getLogger();

  /** Puts the images in base_test_data; see {@link DemoScript#DEMO_IMAGES}. */
  private static final String IMAGES_SQL = "UPDATE base_test_data"
                                           + " SET ss_image = ? WHERE base_test_pk = ?";

  private DemoDatabase() {
  }

  static abstract class DatabaseSetup implements Runnable {
    private Connection conn;
    final DemoOptions options;

    DatabaseSetup(DemoOptions options) {
      this.options = options;
    }

    abstract Properties getDatabaseProperties();

    /**
     * The scripts to run, in order, as names that
     * {@link #getBufferedReader(String)} opens.
     */
    abstract List<String> getScripts();

    /** The scripts that {@code -d} writes; by default the ones that run. */
    List<String> getDumpScripts() {
      return getScripts();
    }

    /** The resource that lists the images, see {@link DemoUtil#loadBinaries}. */
    String getImagesResource() {
      return "/" + DemoScript.DEMO_IMAGES.fileName(DbServer.H2);
    }

    /**
     * The scripts, from the demo's resources, that this setup uses;
     * empty if it does not use the resources.
     * Used to find, before anything is loaded, what a dataset lacks.
     * @param dump true for the scripts that {@code -d} writes
     */
    List<DemoScript> getDemoScripts(boolean dump) {
      return List.of();
    }

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

      if (options.noInitializeDb()) {
        return;
      }

      boolean ok = false;
      for (String script : getScripts()) {
        ok = false;
        BufferedReader br = getBufferedReader(script);
        if (br != null) {
          ok = DemoUtil.runSqlStatements(conn, br, options.verbose());
        }
        if (!ok) {
          break;
        }
      }

      if (ok && !options.noLoadImages()) {
        // Load up the images
        ok = DemoUtil.loadBinaries(conn, getImagesResource(), IMAGES_SQL, options.verbose());
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

    /** The file that {@code -d} writes for a script. */
    String getDumpName(String script) {
      DemoDataset dataset = options.dataset();
      String prefix = dataset.isUnnamed() ? "dump." : "dump." + dataset.name() + ".";
      return prefix + new File(script).getName();
    }

    void dump() {
      for (String script : getDumpScripts()) {
        boolean ok = false;
        try (BufferedReader br = getBufferedReader(script)) {
          if (br != null) {
            String name = getDumpName(script);
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

  /**
   * A setup whose scripts are the demo's resources for one database server,
   * found through the dataset in the options.
   */
  static abstract class ResourceSetup extends DatabaseSetup {
    private final DbServer server;

    ResourceSetup(DemoOptions options, DbServer server) {
      super(options);
      this.server = server;
    }

    DbServer getServer() {
      return server;
    }

    /** The scripts that run, in order. */
    abstract List<DemoScript> getLoadScripts();

    /** The scripts that {@code -d} writes, by default the ones that run. */
    List<DemoScript> getDumpDemoScripts() {
      return getLoadScripts();
    }

    @Override
    List<DemoScript> getDemoScripts(boolean dump) {
      if (dump)
        return getDumpDemoScripts();
      List<DemoScript> scripts = new ArrayList<>(getLoadScripts());
      if (!options.noLoadImages())
        scripts.add(DemoScript.DEMO_IMAGES);
      return scripts;
    }

    private List<String> resolve(List<DemoScript> scripts) {
      return scripts.stream().map(s -> options.dataset().resolve(s, server)).toList();
    }

    @Override
    List<String> getScripts() {
      return resolve(getLoadScripts());
    }

    @Override
    List<String> getDumpScripts() {
      return resolve(getDumpDemoScripts());
    }

    @Override
    String getImagesResource() {
      return options.dataset().resolve(DemoScript.DEMO_IMAGES, server);
    }
  }

  static class ExternalSetup extends DatabaseSetup {
    List<String> sqlFiles;

    public ExternalSetup(DemoOptions options) {
      super(options);
      sqlFiles = options.userSqlFiles();
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
      Properties info = getFileProperties(options.propertyFile());
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
  static class H2Setup extends ResourceSetup {
    private static final String DATABASE_PATH = "//localhost/~/h2/databases/";

    private static final boolean USE_IN_MEMORY_DATABASE = true;

    private static final boolean RUN_DEMO_SQL_SCRIPTS = true;
    private static final boolean RUN_TEST_SQL_SCRIPTS = true;
    private static final boolean RUN_DEBUG_SQL_SCRIPTS = false;

    H2Setup(DemoOptions options) {
      super(options, DbServer.H2);
    }

    @Override
    Properties getDatabaseProperties() {
      return null;
    }

    @Override
    List<DemoScript> getLoadScripts() {
      List<DemoScript> scripts = new ArrayList<>();
      if (RUN_DEMO_SQL_SCRIPTS)
        scripts.add(DemoScript.SUPPLIERS_AND_PARTS);
      if (RUN_TEST_SQL_SCRIPTS)
        scripts.add(DemoScript.COMPONENTS);
      if (RUN_DEBUG_SQL_SCRIPTS)
        scripts.add(DemoScript.DEBUG);
      return scripts;
    }

    /** The dump includes the blob script, for running the dumped scripts by hand. */
    @Override
    List<DemoScript> getDumpDemoScripts() {
      return List.of(DemoScript.SUPPLIERS_AND_PARTS, DemoScript.COMPONENTS,
                     DemoScript.COMPONENTS_BLOBS);
    }

    @Override
    List<DemoScript> getDemoScripts(boolean dump) {
      List<DemoScript> scripts = super.getDemoScripts(dump);
      if (!dump && !RUN_TEST_SQL_SCRIPTS)
        scripts.remove(DemoScript.DEMO_IMAGES);
      return scripts;
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

        // Open every script first; a missing script is fatal.
        List<BufferedReader> readers = new ArrayList<>();
        for (String script : getScripts()) {
          BufferedReader br = getBufferedReader(script);
          if (br == null) {
            logger.log(Level.ERROR, "\n*******\n*******\n*******");
            System.exit(1);
          }
          readers.add(br);
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
        for (BufferedReader br : readers) {
          RunScript.execute(result, br);
          br.close();
        }

        if (RUN_TEST_SQL_SCRIPTS && !options.noLoadImages()) {
          DemoUtil.loadBinaries(result, getImagesResource(), IMAGES_SQL, options.verbose());
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
  }

  static class MysqlSetup extends ResourceSetup {
    MysqlSetup(DemoOptions options) {
      super(options, DbServer.MYSQL);
    }

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
      if (options.propertyFile() != null) {
        info = getFileProperties(options.propertyFile());
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
    List<DemoScript> getLoadScripts() {
      return List.of(DemoScript.SUPPLIERS_AND_PARTS, DemoScript.COMPONENTS);
    }
  }
} // end final class DemoDatabase
// vi: sw=2 ts=8
