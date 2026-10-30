This directory has a copy of the pom seen in the projects README on github at
https://github.com/visdb-dev/seesaw.

In this directory you can run the full interactive demo with maven command
as described below. All the dependent artifacts are taken from the local
repository. They are fetched from maven central if needed.

Run the demo using standard Maven commands:
   * **Default Mode:** `mvn exec:exec`
   * **Show Help:** `mvn exec:exec -Dargs=-h`
   * **Custom Arguments:** `mvn exec:exec -Dargs="--flag1 --flag2"`
   * **log4j instead of JUL:** `mvn exec:exec -Plog4j -Dargs=-h`
