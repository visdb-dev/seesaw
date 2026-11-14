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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.utils.ImageLimits.ImageLimitException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ImageLimits and the checks in ZoomCanvas.bytes2image.
 * ZoomCanvas.toCompatibleImage does not work headless, so a successful
 * decode uses ZoomCanvas.decode; bytes2image is only used where it fails.
 */
public class ImageLimitsTest {

  @Test
  void testDefaults() {
    // No properties are set for tests; testInitFromProperties restores them.
    assertThatCode(() -> ImageLimits.checkBytes(ImageLimits.DEFAULT_MAX_BYTES))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> ImageLimits.checkBytes(ImageLimits.DEFAULT_MAX_BYTES + 1))
        .isInstanceOf(ImageLimitException.class);
  }

  @Test
  void testCheckDimensions() {
    assertThatCode(() -> ImageLimits.checkDimensions(1, 1)).doesNotThrowAnyException();
    // Overflows int; must be computed as long.
    assertThatThrownBy(() -> ImageLimits.checkDimensions(100_000, 100_000))
        .isInstanceOf(ImageLimitException.class);
    assertThatThrownBy(() -> ImageLimits.checkDimensions(0, 10))
        .isInstanceOf(ImageLimitException.class);
  }

  @Test
  void testParseLimit() {
    assertThat(ImageLimits.parseLimit("1")).isEqualTo(1);
    assertThat(ImageLimits.parseLimit(" 67108864 ")).isEqualTo(67108864);
    assertThat(ImageLimits.parseLimit("0")).isEqualTo(-1);
    assertThat(ImageLimits.parseLimit("-5")).isEqualTo(-1);
    assertThat(ImageLimits.parseLimit("")).isEqualTo(-1);
    assertThat(ImageLimits.parseLimit("32m")).isEqualTo(-1);
  }

  @Test
  void testParseFormats() {
    assertThat(ImageLimits.parseFormats(ImageLimits.DEFAULT_FORMATS))
        .containsExactlyInAnyOrder("png", "jpeg", "gif");
    assertThat(ImageLimits.parseFormats(" PNG, ,Bmp,")).containsExactlyInAnyOrder("png", "bmp");
    assertThat(ImageLimits.parseFormats(" , ")).isEmpty();
  }

  /**
   * The properties are read by initImageLimits, which the library
   * initialization runs; not when the class is loaded.
   */
  @Test
  void testInitFromProperties() throws IOException {
    byte[] bmp = image("bmp");
    try {
      System.setProperty(ImageLimits.PROP_MAX_BYTES, "1000");
      System.setProperty(ImageLimits.PROP_MAX_PIXELS, "junk");
      System.setProperty(ImageLimits.PROP_FORMATS, "PNG, bmp");
      // Not read yet.
      assertThat(ImageLimits.maxBytes()).isEqualTo(ImageLimits.DEFAULT_MAX_BYTES);

      ImageLimits.initImageLimits();
      assertThat(ImageLimits.maxBytes()).isEqualTo(1000);
      // A bad value is ignored.
      assertThat(ImageLimits.maxPixels()).isEqualTo(ImageLimits.DEFAULT_MAX_PIXELS);
      assertThat(ImageLimits.formats()).containsExactlyInAnyOrder("png", "bmp");

      assertThatThrownBy(() -> ImageLimits.checkBytes(1001))
          .isInstanceOf(ImageLimitException.class)
          .hasMessageContaining(ImageLimits.PROP_MAX_BYTES);
      assertThatCode(() -> ImageLimits.checkFormat("BMP")).doesNotThrowAnyException();
      assertThatThrownBy(() -> ImageLimits.checkFormat("JPEG"))
          .isInstanceOf(ImageLimitException.class);
      assertThat(ZoomCanvas.decode(bmp)).isNotNull();
      assertThatThrownBy(() -> ZoomCanvas.bytes2image(new byte[1001]))
          .isInstanceOf(ImageLimitException.class);

      System.setProperty(ImageLimits.PROP_FORMATS, "*");
      ImageLimits.initImageLimits();
      assertThatCode(() -> ImageLimits.checkFormat("anything")).doesNotThrowAnyException();
    } finally {
      System.clearProperty(ImageLimits.PROP_MAX_BYTES);
      System.clearProperty(ImageLimits.PROP_MAX_PIXELS);
      System.clearProperty(ImageLimits.PROP_FORMATS);
      ImageLimits.initImageLimits();
    }
    assertThat(ImageLimits.maxBytes()).isEqualTo(ImageLimits.DEFAULT_MAX_BYTES);
    assertThat(ImageLimits.formats()).containsExactlyInAnyOrder("png", "jpeg", "gif");
  }

  @Test
  void testDecodeDefaultFormats() throws IOException {
    for (String format : new String[] {"png", "jpeg", "gif"}) {
      BufferedImage bimg = ZoomCanvas.decode(image(format));
      assertThat(bimg.getWidth()).isEqualTo(8);
      assertThat(bimg.getHeight()).isEqualTo(6);
    }
  }

  @Test
  void testFormatNotAllowed() throws IOException {
    for (String format : new String[] {"bmp", "tiff", "wbmp"}) {
      byte[] bytes = image(format);
      assertThatThrownBy(() -> ZoomCanvas.bytes2image(bytes))
          .isInstanceOf(ImageLimitException.class)
          .hasMessageContaining(ImageLimits.PROP_FORMATS);
    }
  }

  /**
   * A small BMP that says it has a 2GB embedded PNG; the BMP reader
   * allocates that much. It is rejected because of its format.
   */
  @Test
  void testBmpHugeAllocation() throws IOException {
    byte[] bmp = image("bmp");
    ByteBuffer bb = ByteBuffer.wrap(bmp).order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(30, 5); // biCompression: BI_PNG
    bb.putInt(34, 2_000_000_000); // biSizeImage
    assertThatThrownBy(() -> ZoomCanvas.bytes2image(bmp))
        .isInstanceOf(ImageLimitException.class)
        .hasMessageContaining(ImageLimits.PROP_FORMATS);
  }

  @Test
  void testCorruptImageIsIOException() throws IOException {
    // Whatever the decoder does with bad data, it is an IOException.
    Random random = new Random(42);
    for (String format : new String[] {"png", "jpeg", "gif"}) {
      byte[] good = image(format);
      for (int i = 0; i < 2000; i++) {
        byte[] bytes = good.clone();
        for (int k = 0; k < 3; k++) {
          bytes[random.nextInt(bytes.length)] = (byte)random.nextInt();
        }
        try {
          ZoomCanvas.decode(i % 5 == 0 ? Arrays.copyOf(bytes, random.nextInt(bytes.length))
                                       : bytes);
        } catch (IOException ex) {
          // expected for some
        }
      }
    }
  }

  @Test
  void testByteLimit() {
    byte[] bytes = new byte[(int)ImageLimits.DEFAULT_MAX_BYTES + 1];
    assertThatThrownBy(() -> ZoomCanvas.bytes2image(bytes))
        .isInstanceOf(ImageLimitException.class)
        .hasMessageContaining(ImageLimits.PROP_MAX_BYTES);
  }

  private static byte[] image(String format) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int type = format.equals("wbmp") ? BufferedImage.TYPE_BYTE_BINARY
                                     : BufferedImage.TYPE_INT_RGB;
    assertThat(ImageIO.write(new BufferedImage(8, 6, type), format, out)).isTrue();
    return out.toByteArray();
  }

  @Test
  void testNotAnImage() {
    byte[] bytes = "this is not an image".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> ZoomCanvas.bytes2image(bytes))
        .isInstanceOf(IOException.class)
        .isNotInstanceOf(ImageLimitException.class)
        .hasMessageContaining("Unknown image format");
  }

  @Test
  void testEmptyBytes() {
    assertThatThrownBy(() -> ZoomCanvas.bytes2image(new byte[0]))
        .isInstanceOf(IOException.class);
  }

  @Test
  void testTooManyPixels() throws IOException {
    // A small PNG whose header claims 50000 x 50000 pixels.
    byte[] png = pngWithHeaderSize(50_000, 50_000);
    assertThatThrownBy(() -> ZoomCanvas.bytes2image(png))
        .isInstanceOf(ImageLimitException.class)
        .hasMessageContaining(ImageLimits.PROP_MAX_PIXELS);
  }

  /**
   * Write a 1x1 PNG, then change the width/height in the IHDR chunk
   * and fix its CRC. The pixel data is not valid for the new size,
   * but only the header is read before the limit check.
   */
  private static byte[] pngWithHeaderSize(int width, int height) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", out);
    byte[] png = out.toByteArray();
    // 8 byte signature, 4 byte length, "IHDR" at 12, data at 16 (13 bytes), CRC at 29
    ByteBuffer bb = ByteBuffer.wrap(png);
    bb.putInt(16, width);
    bb.putInt(20, height);
    CRC32 crc = new CRC32();
    crc.update(png, 12, 4 + 13);
    bb.putInt(29, (int)crc.getValue());
    return png;
  }
}
// vi: sw=2 ts=8
