/* *****************************************************************************
 * Copyright (C) 2026, Ernie R Rael. All rights reserved.
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
 * ****************************************************************************/
package dev.visdb.seesaw.utils;

import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.swing.JScrollPane;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.utils.ZoomCanvas.CoordSystem;

import static org.junit.jupiter.api.Assertions.*;

/** x */
public class ZoomCanvasTest {
  /** x */
  public ZoomCanvasTest() {}

  /** x */
  @BeforeAll
  public static void setUpClass() {}

  /** x */
  @AfterAll
  public static void tearDownClass() {}

  /** x */
  @BeforeEach
  public void setUp() {
    // 1. Create a mock image (100x100 pixels)
    mockImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);

    // 2. Set up zoom factor
    zoomFactor = 2.0; // Scaled image is 200x200 screen pixels

    // 3. Set up the scroll pane
    mockScrollPane = new JScrollPane();

    // 4. Set up the canvas panel.
    // override getWidth/getHeight to simulate a large panel that centers the image.
    canvas = new ZoomCanvas(mockScrollPane, () -> zoomFactor) {
      @Override
      // 300 width leaves a 50px margin on left/right
      public int getWidth() {
        return 300;
      }
      @Override
      // 300 height leaves a 50px margin on top/bottom
      public int getHeight() {
        return 300;
      }
    };
    // 5. Set up the scroll pane viewport
    mockScrollPane.getViewport().setView(canvas);
    // 6. add the image to the canvas
    canvas.setImage(mockImage);

    // Simulate that the user has scrolled 10 pixels down and 20 pixels right
    mockScrollPane.getViewport().setViewPosition(new Point(20, 10));
  }

  /** x */
  @AfterEach
  public void tearDown() {}

  private JScrollPane mockScrollPane;
  private BufferedImage mockImage;
  private double zoomFactor;
  private ZoomCanvas canvas;

  @Test
  @DisplayName(
      "Identical coordinate system conversion should bypass math but return a unique cloned object")
  void
  testIdenticalSystemOptimization() {
    Point originalPoint = new Point(45, 90);

    Point translatedPoint = canvas.translateCoord(originalPoint, CoordSystem.IMAGE, CoordSystem.IMAGE);

    // Values must match
    assertEquals(originalPoint, translatedPoint);
    // It must NOT be the same object instance in memory (Immutability protection)
    assertNotSame(originalPoint, translatedPoint);
  }

  @Test
  @DisplayName(
      "Translate CANVAS coordinates to raw IMAGE pixels with dynamic margins and zoom applied")
  void
  testCanvasToImageTranslation() {
    // Calculation check:
    // Scaled image is 200x200 inside a 300x300 canvas.
    // Offset margin is (300 - 200) / 2 = 50 pixels.
    // Image space offset is 50 / 2.0 = 25 units.

    // Let's click at canvas pixel (150, 150) -> exact center of the canvas
    Point canvasPoint = new Point(150, 150);

    Point imagePoint = canvas.translateCoord(canvasPoint, CoordSystem.CANVAS, CoordSystem.IMAGE);

    // (150 / 2.0) - 25 = 75 - 25 = 50
    assertEquals(new Point(50, 50), imagePoint,
                 "Center of the canvas should map to center of the image");
  }

  @Test
  @DisplayName(
      "Translate VIEWPORT coordinates to IMAGE pixels while accounting for active scroll positions")
  void
  testViewportToImageTranslationWithScrolling() {
    // Scroll position is set to X=20, Y=10 in setUp()
    // If a user clicks at (130, 140) on the physical window (Viewport):
    // Panel coordinate becomes: X = 130 + 20 = 150, Y = 140 + 10 = 150
    Point viewportPoint = new Point(130, 140);

    Point imagePoint = canvas.translateCoord(viewportPoint, CoordSystem.VIEWPORT, CoordSystem.IMAGE);

    // After accounting for the scroll offset, it resolves to panel (150, 150) -> image (50, 50)
    assertEquals(new Point(50, 50), imagePoint);

    // Point v = canvas.translateCoord(new Point(0, 0), CoordSystem.IMAGE, CoordSystem.VIEWPORT);
    // System.err.println(v.toString());
  }

  @Test
  @DisplayName("Translate IMAGE pixels out to VIEWPORT coordinates considering layout padding and "
               + "scroll offsets")
  void
  testImageToViewportTranslation() {
    // Target image pixel is the top-left corner (0,0)
    Point imagePoint = new Point(0, 0);

    Point viewportPoint = canvas.translateCoord(imagePoint, CoordSystem.IMAGE, CoordSystem.VIEWPORT);

    // Math sequence:
    // Image (0,0) -> Scaled Image (25, 25) -> Panel (50, 50)
    // Panel (50, 50) minus Scroll position (20, 10) = Viewport (30, 40)
    assertEquals(new Point(30, 40), viewportPoint);
  }

  // /**
  //  * Test of translateCoord method, of class ZoomCanvas.
  //  */
  // @Test
  // public void testTranslate()
  // {
  // 	System.out.println("translateCoord");
  // 	Point fromPoint = null;
  // 	ZoomCanvas.CoordSystem fromCoordSystem = null;
  // 	ZoomCanvas.CoordSystem toCoordSystem = null;
  // 	ZoomCanvas instance = null;
  // 	Point expResult = null;
  // 	Point result = instance.translateCoord(fromPoint, fromCoordSystem, toCoordSystem);
  // 	assertEquals(expResult, result);
  // 	// TODO review the generated test code and remove the default call to fail.
  // 	fail("The test case is a prototype.");
  // }

  // /**
  //  * Test of setImage method, of class ZoomCanvas.
  //  */
  // @Test
  // public void testSetImage()
  // {
  // 	System.out.println("setImage");
  // 	BufferedImage newImage = null;
  // 	ZoomCanvas instance = null;
  // 	instance.setImage(newImage);
  // 	// TODO review the generated test code and remove the default call to fail.
  // 	fail("The test case is a prototype.");
  // }

  // /**
  //  * Test of getCurrentImage method, of class ZoomCanvas.
  //  */
  // @Test
  // public void testGetCurrentImage()
  // {
  // 	System.out.println("getCurrentImage");
  // 	ZoomCanvas instance = null;
  // 	BufferedImage expResult = null;
  // 	BufferedImage result = instance.getCurrentImage();
  // 	assertEquals(expResult, result);
  // 	// TODO review the generated test code and remove the default call to fail.
  // 	fail("The test case is a prototype.");
  // }

  // /**
  //  * Test of getPreferredSize method, of class ZoomCanvas.
  //  */
  // @Test
  // public void testGetPreferredSize()
  // {
  // 	System.out.println("getPreferredSize");
  // 	ZoomCanvas instance = null;
  // 	Dimension expResult = null;
  // 	Dimension result = instance.getPreferredSize();
  // 	assertEquals(expResult, result);
  // 	// TODO review the generated test code and remove the default call to fail.
  // 	fail("The test case is a prototype.");
  // }

  // /**
  //  * Test of paintComponent method, of class ZoomCanvas.
  //  */
  // @Test
  // public void testPaintComponent()
  // {
  // 	System.out.println("paintComponent");
  // 	Graphics g = null;
  // 	ZoomCanvas instance = null;
  // 	instance.paintComponent(g);
  // 	// TODO review the generated test code and remove the default call to fail.
  // 	fail("The test case is a prototype.");
  // }
}
