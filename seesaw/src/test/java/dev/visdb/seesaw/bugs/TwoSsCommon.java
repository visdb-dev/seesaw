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
package dev.visdb.seesaw.bugs;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.SsImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * x
 */
public class TwoSsCommon {
  /** x */
  public TwoSsCommon() {}

  /** x */
  @BeforeAll
  public static void setUpClass() {}

  /** x */
  @AfterAll
  public static void tearDownClass() {}

  /** x */
  @BeforeEach
  public void setUp() {}

  /** x */
  @AfterEach
  public void tearDown() {}

  @SuppressWarnings("serial")

  class TestImage extends SsImage {
    @SuppressWarnings("unused")
    Object xxx;

    public TestImage() {
      finishSsCommon();
    }

    @Override
    public void customInit() {
      super.customInit();
      xxx = getSsCommon();
    }
  }

  /**
   * Test of getStyleNames method, of class TextStyles.
   * @throws java.io.IOException
   */
  @Test
  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  public void testOneSSCommon() throws IOException {
    System.out.println("FocusTarget");

    TestImage testImage = new TestImage();
    assertTrue(testImage.xxx == testImage.getSsCommon());
  }
}
// vi: sw=2 ts=8
