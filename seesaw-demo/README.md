# ReadMe file for the SeeSaw DEMO

## DESCRIPTION

SeeSaw is an open source Java toolkit containing data-aware replacements for many of the standard Java Swing components. The seesaw-demo project contains sample/demo source code that uses the SeeSaw library.

By default, the sample/demo programs provided with SeeSaw utilize the h2 database, which is run in memory. The sample database is based on the suppliers-and-parts database referenced in the classic database textbook, "An Introduction to Database Systems," by C. J. Date.

You can view the [aggregated SeeSaw javadoc](https://github.com/visdb-dev/seesaw).

For questions regarding SeeSaw, send an email to: visdb@visdb.dev

## EXECUTION

The SeeSaw samples/demo requires Java 17 or later. The simplest way to run the demo is described on
the [main project's page](https://github.com/visdb-dev/seesaw) README. Essentially you download a
pom.xml into an empty directory and run some maven commands that execute the demo from artifacts
in the local maven repository. The repository does not need to be cloned.

The seesaw-demo uses an in-memory H2 database by default, but can be run using other databases with some effort. See [USING ALTERNATE DATABASE SERVERS](#using-alternate-database-servers) at the end of this document for more information.

Note that the default screen for placement of the demo can be specified using the environment variable: `SEESAW_PREFERRED_SCREEN`. For example, in a dual monitor Linux environment, you can type `export SEESAW_PREFERRED_SCREEN=1` prior to running the demo, and the SeeSaw demo will appear on the right screen (presuming the left monitor is the default). If `SEESAW_PREFERRED_SCREEN` is not found, `JAVA_PREFERRED_SCREEN` is used. If neither environment variable is present, the java/jvm default is used. Screen feature provided by `com.raelity:raelity-lib`.

## COMPILATION

Although you can run the demo without cloning the repository, you can build it yourself.

The SeeSaw samples/demo requires Java 17 or later.

Git/Maven:
  `git clone https://github.com/visdb-dev/seesaw.git`

  After cloning, you can use an IDE, e.g. Eclipse or NetBeans, to compile/run.
  Or you can use mvn directly and then run as shown here. Note that compiled
  jar files will be in the ./target subdirectory. The library must be installed
  into the maven repository; the demo build looks there.
  
  To build the SeeSaw Library, contributed modules, the seesaw-demo and install their artifacts in the local maven repository:

    cd ./seesaw
    mvn clean install

  Now you can `cd seesaw-demo-runner` which has a single pom.xml that supports running the demo from the maven artifacts:

    cd seesaw-demo-runner
    mvn exec:exec

## CLASS DESCRIPTIONS

### (updating documentation in progress)

### MainClass

Brings up a window to select specific demos and options.
- Buttons to launch each of the SeeSaw example/demo screens.
- Combo for type/method for JdbcRowSet creation.
- If `java.util.logging` (`JUL`) is used, there is a button to bring up a dialog to examine and/or set logging levels.

### Example1

This example displays data from the supplier_data table. SSTextFields are used to display supplier id, name, city, and status.

Record navigation is handled with a SSDataNavigator.

### Example2

This example displays data from the supplier_data table. SSTextFields are used to display supplier id, name, and city. SSComboBox is used to display status.

Record navigation is handled with a SSDataNavigator.

### Example3

This example displays data from the supplier_part_data table. SSTextFields are used to display supplier-part id and quantity. SSDBComboBoxes are used to display supplier name and part name based on queries against the supplier_data and part_data tables.

Record navigation is handled with a SSDataNavigator.

### Example4

This example displays data from the part_data table. SSTextFields are used to display part id, name, weight, and city. SSComboBox is used to display color.

Record navigation can be handled with a SSDataNavigator or with a SSDBComboBox.

Since the navigation can take place by multiple methods, the navigation controls have to be synchronized. This is accomplished with the SSSyncManager.

### Example4 Advanced

Extension of Example4, showing:
1. Custom handling of a missing Option (Red) in the Color SSComboBox.
2. Use of InputMap/ActionMap for custom key and extra button handling with F3-F11 mnemonics corresponding to the buttons on Navigator.
3. Use of InputMap/ActionMap to add "extra" First and Last record navigation buttons at the bottom of the screen.

### Example4 Using Helper

Same as Example4, but built by extending the SSFormViewScreenHelper helper class to organize construction.

### Example5

This example demonstrates the use of an SSDataGrid to display a tabular view of the part_data table.

For an editable table, users can delete rows by selecting the row to be deleted and pressing Ctrl-X. By default, a confirmation message is displayed before deletion.

### Example6

This example is similar to Example5, demonstrating the use of an SSDataGrid to display a tabular view of the part_data table. It adds a ComboRenderer for the color column.

### Example7

This example demonstrates the use of an SSDataGrid to display a tabular view of the supplier_part_data table.

It adds a ComboRenderer with a lookup to the supplier_data table for the supplier name, and adds a DateRenderer for the ship date column.

### Example7 Using Helper

Same as Example7, but built by extending the SSDataGridScreenHelper helper class to organize construction.

### Test Base Components

This example demonstrates all of the Base SeeSaw Components except for the SSDataGrid.

There is a separate example screen to demonstrate the Formatted SeeSaw Components.

### Test Formatted Components

This example demonstrates all of the Formatted SeeSaw Components.

There is a separate example screen to demonstrate the Base SeeSaw Components.

## USING ALTERNATE DATABASE SERVERS

seesaw-demo can work with user supplied connection properties and sql scripts to initialize a database that is then used for the demo. You should be in the directory `seesaw-demo-runner` or in the directory you created to run the demo without cloning the repository. Look at the help with

    mvn exec:exec -Dargs=-h

The connection properties is standard java format for a properties file.
Here is an example of a database connection property file used with mysql

    # This is a standard java properties file

    DB_DRIVER_CLASS = com.mysql.cj.jdbc.Driver
    DB_URL = jdbc:mysql://localhost/seesaw_demo_suppliers_and_parts
    user = some_user
    password = some_password
    serverTimezone = UTC

The properties "DB_DRIVER_CLASS" and "DB_URL" are used internally with
    Class.forName(driver_class)
    DriverManager.getConnection(url, props)

You can run the demo, without re-compiling, if you provide java the dbms server jar. Before running the demo, create the database seesaw_demo_suppliers_and_parts.  The sql scripts to initialize the MySQL database tables are included in seesaw-demo

    mvn exec:exec -Dxcp=:mysql-connector-java-8.0.21.jar \
        -Dargs="-v -p property_file mysql"

You can extract the MySQL script. The following command:

    mvn exec:exec -Dargs="-d mysql"

puts the following files into the current directory.

    dump.mysql.demo-app.sql
    dump.mysql.demo-components.sql

These files can be edited as needed for a different database. If the files are edited and saved under the names

    xdb.demo-app.sql
    xdb.demo-components.sql

You can use them as in this example

    mvn exec:exec -Dxcp=:some_db_driver.jar \
        -Dargs="-v -p property_file -s xdb.demo-app.sql -s xdb.demo-components.sql"

The user supplied connection properties and sql scripts initialize the database and then the demo is started.

There are other options...
