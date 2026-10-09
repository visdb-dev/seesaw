### (documentation in progress)
# Seesaw database aware components

[javadoc](https://visdb-dev.github.io/seesaw/).

## Running the Demo (Standalone)

You can run the full interactive demo without cloning this repository. 

1. Create a new, empty directory on your machine and navigate into it:
   ```bash
   mkdir demo-runner && cd demo-runner
   ```

2. Create a file named `pom.xml` and paste the following content into it:
   <details>
       <summary>Click to expand to show pom.xml content</summary>

   ```xml
   <project xmlns="http://apache.org" 
            xmlns:xsi="http://w3.org"
            xsi:schemaLocation="http://apache.org http://apache.org">
       <modelVersion>4.0.0</modelVersion>
   
       <groupId>dev.visdb.seesaw</groupId>
       <artifactId>seesaw-demo-runner</artifactId>
       <version>1.0.0</version>
       <packaging>pom</packaging>
   
       <properties>
           <args></args>
           <demo.args>${args}</demo.args>
           <xcp></xcp>
           <seesaw.version>0.5.1</seesaw.version> 
           <exec-maven-plugin.version>3.6.4</exec-maven-plugin.version>
           <log4j.version>2.26.1</log4j.version>
       </properties>
   
       <dependencies>
           <dependency>
               <groupId>dev.visdb.seesaw</groupId>
               <artifactId>seesaw-demo</artifactId>
               <version>${seesaw.version}</version>
           </dependency>
       </dependencies>
   
       <build>
           <plugins>
               <plugin>
                   <groupId>org.codehaus.mojo</groupId>
                   <artifactId>exec-maven-plugin</artifactId>
                   <version>${exec-maven-plugin.version}</version>
                   <configuration>
                       <executable>java</executable>
                       <commandlineArgs>
                           -classpath %classpath${xcp}
                           dev.visdb.seesaw.demo.MainClass ${demo.args}
                       </commandlineArgs>
                       <classpathScope>test</classpathScope> 
                       <successCodes>
                           <successCode>0</successCode>
                           <successCode>1</successCode>
                       </successCodes>
                   </configuration>
               </plugin>
           </plugins>
       </build>
   
       <profiles>
           <profile>
               <id>log4j</id>
               <dependencies>
                   <!-- inject the log4j System.Logger bridge -->
                   <dependency>
                       <groupId>org.apache.logging.log4j</groupId>
                       <artifactId>log4j-jpl</artifactId>
                       <version>${log4j.version}</version>
                   </dependency>
               </dependencies>
           </profile>
       </profiles>
   </project>
   ```

   </details>

3. Run the demo using standard Maven commands:
   * **Default Mode:** `mvn exec:exec`
   * **Show Help:** `mvn exec:exec -Dargs=-h`
   * **Custom Arguments:** `mvn exec:exec -Dargs="--flag1 --flag2"`
   * **log4j instead of JUL:** `mvn exec:exec -Plog4j -Dargs=-h`

## DESCRIPTION



Seesaw is an open source Java toolkit containing data-aware replacements for many of the standard Java Swing components. It is a rewrite of most of the original [SwingSet](https://github.com/bpangburn/swingset) with several new features; there is a [wrapper](https://github.com/errael/swingset) that uses seesaw to provide the SwingSet API.


The Seesaw feature-set currently includes:

1. data-aware replacements for JTextField, JTextArea, JComboBox, JCheckBox, JLabel, JSlider, & JFormattedTextField
2. binding of a "hidden" numeric column for combo boxes with text choices
   (e.g., 0, 1, & 2 are stored for "Yes," "No," & "Maybe," respectively)
3. population of combo boxes based on columns in a database query (can also be used for combo box-based record navigation)
4. a data-aware image component with image support
5. a graphical record navigator
    (a) allows for database traversal, insertion, deletion, commit, and rollback
    (b) supplies current record index (editable) and total record count
6. a data grid component for creating datasheet/spreadsheet/table views of queries
    (a) allows cut & paste to/from spreadsheet programs or other data grids
    (b) allows custom column headings
    (c) allows hiding of specified columns
    (d) allows disabling of specified columns
    (e) allows columns to be displayed as text boxes or combo boxes
    (f) allows addition and deletion of records
    (g) allows deletion of multiple, non-consecutive records
    (h) allows data entry "masks" to be applied to text columns
7. formatted fields for various types like currency, percent, SSN, date etc.

<!--
More information on SwingSet is available from:
https://github.com/bpangburn/swingset

visdb#NO-SPAM#@visdb.dev
-->
For questions regarding Seesaw, contact info at
[visdb-dev](https://github.com/visdb-dev)

## SCREENSHOTS

<!--
    Standard Markdown doesn't let you resize/restrict image dimensions.
    If your PNG is massive, use an HTML <img> tag to constrain it: 

    <img src="assets/screenshot.png" alt="App Screenshot" width="500">
-->

### Seesaw Demo - Example 1
![Seesaw Demo - Example 1](docs/ss4/img/example1.png)

### Seesaw Demo - Example 2
![Seesaw Demo - Example 2](docs/ss4/img/example2.png)

### Seesaw Demo - Example 3
![Seesaw Demo - Example 3](docs/ss4/img/example3.png)

### Seesaw Demo - Example 4
![Seesaw Demo - Example 4](docs/ss4/img/example4.png)

### Seesaw Demo - Example 5
![Seesaw Demo - Example 5](docs/ss4/img/example5.png)
