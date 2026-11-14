/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package dev.visdb.seesaw.utils;

import java.io.IOException;
import java.lang.System.Logger;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.openide.util.lookup.ServiceProvider;

import dev.visdb.seesaw.SeeSaw;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * Limits applied when decoding an image, for example from a BLOB column
 * or a file, so that a very large or crafted image can not exhaust memory.
 * <p>
 * The limits have built-in defaults which can be changed with JVM
 * system properties. The properties are read once, by {@link SeeSaw#init() };
 * set them on the command line, for example
 * {@code -Dseesaw.image.maxBytes=67108864}, or before {@code SeeSaw.init()}.
 * Until then the built-in defaults are in force.
 * <ul>
 * <li>{@value #PROP_MAX_BYTES}: maximum size of the encoded image in bytes,
 *     default {@value #DEFAULT_MAX_BYTES} (32 MiB).
 * <li>{@value #PROP_MAX_PIXELS}: maximum width times height of the decoded
 *     image, default {@value #DEFAULT_MAX_PIXELS} (40 megapixels).
 *     A decoded image typically needs 4 bytes per pixel, and twice that
 *     while it is being converted for display.
 * <li>{@value #PROP_FORMATS}: comma separated list of the image formats that
 *     are decoded, default "{@value #DEFAULT_FORMATS}". The names are the
 *     {@link javax.imageio.ImageReader#getFormatName() } of the
 *     reader, ignoring case; for example {@code png,jpeg,gif,bmp,tiff}.
 *     "*" allows any format that has a reader. The default formats are the
 *     ones whose readers size their allocations from the image dimensions,
 *     and so are covered by the pixel limit.
 * </ul>
 * A property value that is not valid is ignored, with a warning,
 * and the default is used.
 */
public final class ImageLimits {
  private ImageLimits() { }

  private static final Logger logger = JStuff.getLogger();

  /** System property for the maximum encoded image size in bytes. */
  public static final String PROP_MAX_BYTES = "seesaw.image.maxBytes";
  /** System property for the maximum number of pixels, width * height. */
  public static final String PROP_MAX_PIXELS = "seesaw.image.maxPixels";
  /** System property for the image formats that are decoded. */
  public static final String PROP_FORMATS = "seesaw.image.formats";

  /** Default maximum encoded image size in bytes, 32 MiB. */
  public static final long DEFAULT_MAX_BYTES = 32L * 1024 * 1024;
  /** Default maximum number of pixels, 40 megapixels. */
  public static final long DEFAULT_MAX_PIXELS = 40_000_000L;
  /** Default image formats. */
  public static final String DEFAULT_FORMATS = "png,jpeg,gif";

  /** Formats value that allows any format. */
  private static final String ANY_FORMAT = "*";

  // These start out as the defaults, so the limits are in force even if
  // the initialization has not run. There is no static block and no
  // call to the library initialization from this class; see
  // SeeSawInitialization about deadlock.
  private static volatile long maxBytes = DEFAULT_MAX_BYTES;
  private static volatile long maxPixels = DEFAULT_MAX_PIXELS;
  private static volatile Set<String> formats = parseFormats(DEFAULT_FORMATS);

  /**
   * Read the properties during library initialization.
   */
  @ServiceProvider(service = SeeSawInitialization.class, path = LookupDefaults.INIT_PATH)
  public static class Init implements SeeSawInitialization {
    /** {@inheritDoc } */
    @Override
    public void init() {
      initImageLimits();
    }
  }

  /**
   * Set the limits from the system properties.
   */
  static void initImageLimits() {
    maxBytes = readLimit(PROP_MAX_BYTES, DEFAULT_MAX_BYTES);
    maxPixels = readLimit(PROP_MAX_PIXELS, DEFAULT_MAX_PIXELS);
    formats = readFormats();
  }

  /**
   * Thrown when an image exceeds one of the limits.
   */
  @SuppressWarnings("serial")
  public static class ImageLimitException extends IOException {
    /**
     * @param message description of the limit that was exceeded
     */
    public ImageLimitException(String message) {
      super(message);
    }
  }

  /**
   * @return maximum encoded image size in bytes
   */
  public static long maxBytes() {
    return maxBytes;
  }

  /**
   * @return maximum number of pixels, width * height
   */
  public static long maxPixels() {
    return maxPixels;
  }

  /**
   * @return the lower case names of the formats that are decoded;
   *         "*" if any format is allowed
   */
  public static Set<String> formats() {
    return formats;
  }

  /**
   * Check the size of an encoded image.
   * @param nBytes size of the encoded image
   * @throws ImageLimitException if nBytes is more than {@link #maxBytes() }
   */
  public static void checkBytes(long nBytes) throws ImageLimitException {
    long limit = maxBytes;
    if (nBytes > limit)
      throw new ImageLimitException(sf("Image is %d bytes, limit is %d (%s)",
                                       nBytes, limit, PROP_MAX_BYTES));
  }

  /**
   * Check the dimensions of an image before it is decoded.
   * @param width image width
   * @param height image height
   * @throws ImageLimitException if width * height is more than
   *         {@link #maxPixels() }, or a dimension is not positive
   */
  public static void checkDimensions(int width, int height) throws ImageLimitException {
    if (width <= 0 || height <= 0)
      throw new ImageLimitException(sf("Image has invalid size %dx%d", width, height));
    long limit = maxPixels;
    long nPixels = (long)width * height;
    if (nPixels > limit)
      throw new ImageLimitException(sf("Image is %dx%d, %d pixels, limit is %d (%s)",
                                       width, height, nPixels, limit, PROP_MAX_PIXELS));
  }

  /**
   * Check the format of an image before it is decoded.
   * @param formatName the reader's format name
   * @throws ImageLimitException if the format is not one of {@link #formats() }
   */
  public static void checkFormat(String formatName) throws ImageLimitException {
    Set<String> allowed = formats;
    if (allowed.contains(ANY_FORMAT))
      return;
    if (formatName == null || !allowed.contains(formatName.toLowerCase(Locale.ROOT)))
      throw new ImageLimitException(sf("Image format '%s' is not one of %s (%s)",
                                       formatName, allowed, PROP_FORMATS));
  }

  private static long readLimit(String prop, long dflt) {
    String val = System.getProperty(prop);
    if (val == null)
      return dflt;
    long limit = parseLimit(val);
    if (limit > 0) {
      logger.log(INFO, () -> sf("%s=%d", prop, limit));
      return limit;
    }
    logger.log(WARNING, () -> sf("Ignoring %s='%s', not a positive integer; using %d",
                                 prop, val, dflt));
    return dflt;
  }

  /**
   * @return the limit, or -1 if val is not a positive integer
   */
  static long parseLimit(String val) {
    try {
      long limit = Long.parseLong(val.trim());
      return limit > 0 ? limit : -1;
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

  private static Set<String> readFormats() {
    Set<String> dflt = parseFormats(DEFAULT_FORMATS);
    String val = System.getProperty(PROP_FORMATS);
    if (val == null)
      return dflt;
    Set<String> set = parseFormats(val);
    if (!set.isEmpty()) {
      logger.log(INFO, () -> sf("%s=%s", PROP_FORMATS, set));
      return set;
    }
    logger.log(WARNING, () -> sf("Ignoring %s='%s', no format names; using %s",
                                 PROP_FORMATS, val, dflt));
    return dflt;
  }

  /**
   * @return the lower case format names in the comma separated list
   */
  static Set<String> parseFormats(String val) {
    Set<String> set = new LinkedHashSet<>();
    for (String name : val.split(",")) {
      name = name.trim().toLowerCase(Locale.ROOT);
      if (!name.isEmpty())
        set.add(name);
    }
    return Set.copyOf(set);
  }
}
// vi: sw=2 ts=8
