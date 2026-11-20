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
package dev.visdb.seesaw.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import dev.visdb.seesaw.utils.JStuff;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * Where the demo's scripts and load lists come from.
 * <p>
 * The unnamed dataset, {@code ""}, is the files at the top of the resources.
 * A named dataset is the directory {@code /datasets/<name>/}; it holds only
 * the files it changes. A file it does not have is taken from the unnamed
 * dataset, which is called a fallback.
 * <p>
 * The named datasets, with a description of each, are listed in
 * {@code /datasets/datasets.txt}, one {@code name: description} per line.
 * The list is needed because the entries of a resource directory in a jar
 * can not be listed.
 */
final class DemoDataset {
  /** The name of the unnamed dataset. */
  static final String UNNAMED = "";

  private static final String LIST = "/datasets/datasets.txt";
  private static final String DIR = "/datasets/";
  private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._][A-Za-z0-9._-]*");

  private static final Logger logger = JStuff.getLogger();

  private static List<DemoDataset> available;

  private final String name;
  private final String description;

  private DemoDataset(String name, String description) {
    this.name = name;
    this.description = description;
  }

  /**
   * @param name a dataset name, {@code ""} for the unnamed dataset
   * @return the dataset
   * @throws IllegalArgumentException if the name is not in the list of datasets
   */
  static DemoDataset of(String name) {
    for (DemoDataset dataset : available()) {
      if (dataset.name.equals(name))
        return dataset;
    }
    throw new IllegalArgumentException(sf("Unknown dataset '%s'", name));
  }

  /**
   * @return the unnamed dataset, then the named datasets in the order of the list
   */
  static synchronized List<DemoDataset> available() {
    if (available == null)
      available = Collections.unmodifiableList(readList());
    return available;
  }

  private static List<DemoDataset> readList() {
    List<DemoDataset> list = new ArrayList<>();
    list.add(new DemoDataset(UNNAMED, "The files at the top of the resources"));
    try (InputStream stream = MainClass.class.getResourceAsStream(LIST)) {
      if (stream == null) {
        logger.log(WARNING, () -> sf("Dataset list '%s' not found", LIST));
        return list;
      }
      BufferedReader br = new BufferedReader(
          new InputStreamReader(stream, StandardCharsets.UTF_8));
      String line;
      while ((line = br.readLine()) != null) {
        line = line.strip();
        if (line.isEmpty() || line.startsWith("#"))
          continue;
        int colon = line.indexOf(':');
        String name = (colon < 0 ? line : line.substring(0, colon)).strip();
        String description = colon < 0 ? "" : line.substring(colon + 1).strip();
        if (!NAME.matcher(name).matches()) {
          String bad = line;
          logger.log(WARNING, () -> sf("%s: bad dataset name in '%s'", LIST, bad));
          continue;
        }
        list.add(new DemoDataset(name, description));
      }
    } catch (IOException ex) {
      logger.log(ERROR, () -> sf("Reading '%s'", LIST), ex);
    }
    return list;
  }

  /** @return the name, {@code ""} for the unnamed dataset */
  String name() {
    return name;
  }

  /** @return the description from the list of datasets */
  String description() {
    return description;
  }

  /** @return true for the unnamed dataset */
  boolean isUnnamed() {
    return name.isEmpty();
  }

  /** @return the name for messages, {@code (unnamed)} for the unnamed dataset */
  String displayName() {
    return isUnnamed() ? "(unnamed)" : name;
  }

  /**
   * @param script the script
   * @param server the database server
   * @return the resource path in this dataset, whether it exists or not;
   *         null if the script has no file for the server
   */
  String ownPath(DemoScript script, DbServer server) {
    String fileName = script.fileName(server);
    if (fileName == null)
      return null;
    return isUnnamed() ? "/" + fileName : DIR + name + "/" + fileName;
  }

  /**
   * @param script the script
   * @param server the database server
   * @return true if this dataset has its own file for the script;
   *         always true for the unnamed dataset
   */
  boolean hasOwn(DemoScript script, DbServer server) {
    String path = ownPath(script, server);
    return isUnnamed() || path != null && MainClass.class.getResource(path) != null;
  }

  /**
   * The resource to use for a script: this dataset's own file, or else
   * the unnamed dataset's file. Which one is used is logged.
   * @param script the script
   * @param server the database server
   * @return the resource path
   * @throws IllegalArgumentException if the script has no file for the server
   */
  String resolve(DemoScript script, DbServer server) {
    String fileName = script.fileName(server);
    if (fileName == null)
      throw new IllegalArgumentException(sf("%s has no file for %s", script, server));
    if (hasOwn(script, server)) {
      String path = ownPath(script, server);
      logger.log(DEBUG, () -> sf("Dataset %s: %s uses '%s'", displayName(), script, path));
      return path;
    }
    String path = "/" + fileName;
    logger.log(INFO, () -> sf("Dataset %s has no '%s'; using '%s' (fallback)",
                              displayName(), fileName, path));
    return path;
  }

  /**
   * @param scripts the scripts that will be used
   * @param server the database server
   * @return the file names of the scripts that this dataset does not have;
   *         empty for the unnamed dataset
   */
  List<String> fallbacks(Collection<DemoScript> scripts, DbServer server) {
    List<String> list = new ArrayList<>();
    for (DemoScript script : scripts) {
      if (script.fileName(server) != null && !hasOwn(script, server))
        list.add(script.fileName(server));
    }
    return list;
  }

  @Override
  public String toString() {
    return displayName();
  }
}
// vi: sw=2 ts=8
