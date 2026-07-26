/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package com.nqadmin.swingset.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Works with a {@link BufferedImage}
 * in a JScrollPane
 * and some dynamic configuration Suppliers.
 * There are some helper method to support hardware acceleration.
 */
@SuppressWarnings("serial")
public class ZoomCanvas extends JPanel {
  /** Rendering quality. */
  public enum RenderingQuality { /**high.*/ HIGH, /**medium.*/ MEDIUM, /**low.*/ LOW }

  private final JScrollPane scrollPane;
  private final DoubleSupplier zoomFactor;
  private BufferedImage image;
  private final Supplier<RenderingQuality> quality;

  /**
   * Initializes the canvas independently of the parent Application context.
   * <p>
   * If quality is hooked to a slider's ValueIsAdjusting, remember
   * to trigger zoomCanvas.repaint() from the slider's mouseReleased
   * listener to draw final frame at non-adjusting quality.
   *
   * @param scrollPane The container scrollpane used to evaluate live viewport bounds.
   * @param zoomFactor current zoom lambda (e.g., () -> this.currentZoom)
   * @param quality rendering quality  lambda
   */
  public ZoomCanvas(JScrollPane scrollPane, DoubleSupplier zoomFactor,
                    Supplier<RenderingQuality> quality) {
    this.scrollPane = scrollPane;
    this.zoomFactor = zoomFactor;
    this.quality = quality;
    setLayout(null);
  }

  /**
   *
   * @param scrollPane
   * @param zoomFactor
   */
  public ZoomCanvas(JScrollPane scrollPane, DoubleSupplier zoomFactor) {
    this(scrollPane, zoomFactor, () -> RenderingQuality.MEDIUM);
  }

  /**
   * Updates the underlying image matrix and forces a canvas layout repaint.
   * @param newImage
   */
  public void setImage(BufferedImage newImage) {
    this.image = newImage;
    revalidate();
    repaint();
  }

  /**
   * Gets the active image currently being displayed.
   * @return
   */
  public BufferedImage getCurrentImage() { return this.image; }

  /**
   * @return size of zoomed image.
   */
  @Override
  public Dimension getPreferredSize() {
    Dimension viewSize = scrollPane.getViewport().getSize();

    // If there's no image, fill the viewport space completely
    if (image == null) { return viewSize; }

    double currentZoom = zoomFactor.getAsDouble();
    int w = (int) (image.getWidth() * currentZoom);
    int h = (int) (image.getHeight() * currentZoom);

    return new Dimension(Math.max(w, viewSize.width), Math.max(h, viewSize.height));
  }

  /**
   * Paint the zoomed/scaled image.
   */
  @Override
  protected void paintComponent(Graphics g) {
    // System.err.println("PAINT_COMPONENT zoom " + zoomFactor.getAsDouble());
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g.create();

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // 1. IF NO IMAGE IS SET -> Render centered placeholder text
    if (image == null) {
      g2d.setColor(getBackground());
      g2d.fillRect(0, 0, getWidth(), getHeight());

      //String text = "No Image Loaded. Please Select an Image.";
      String text = "No Image";
      g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
      g2d.setColor(Color.DARK_GRAY);

      FontMetrics metrics = g2d.getFontMetrics(g2d.getFont());
      int textX = (getWidth() - metrics.stringWidth(text)) / 2;
      int textY = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();

      g2d.drawString(text, textX, textY);
      g2d.dispose();
      return;
    }

    // Draw scaled image.
    // May be hardware-accelerated. If ...
    double currentZoom = zoomFactor.getAsDouble();
    int imgW = (int) (image.getWidth() * currentZoom);
    int imgH = (int) (image.getHeight() * currentZoom);

    int x = (getWidth() - imgW) / 2;
    int y = (getHeight() - imgH) / 2;

    setRenderingHints(g2d);

    g2d.drawImage(image, x, y, imgW, imgH, null);
    g2d.dispose();
  }

  private void setRenderingHints(Graphics2D g2d) {
    switch (quality.get()) {
      case LOW -> {
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
      }
      case MEDIUM -> {
        // Fast processing during drag operations
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
      }
      case HIGH -> {
        // Pristine rendering when static or clicking buttons
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      }
    }
  }

  /**
   * The various coordinate spaces when working with
   * a scaled image in a scroll pane.
   */
  public enum CoordSystem {
    /**
     * Screen pixels relative to the top-left (0,0) of the JScrollPane's Viewport window.
     * This represents the coordinates of your physical application window.
     */
    VIEWPORT,

    /**
     * Screen pixels relative to the top-left (0,0) of the inner scrollable JPanel surface.
     * This shifts when the scrollbars move.
     */
    CANVAS,

    /**
     * Pixels in the intermediate coordinate matrix *after* g2d.scale() is applied,
     * but *before* image offsets are applied. Measured in unscaled image units.
     */
    SCALED_IMAGE,

    /**
     * Raw, unscaled pixel coordinates directly mapping onto the underlying
     * BufferedImage indices (0 to width-1, 0 to height-1).
     */
    IMAGE
  }

  /**
   * Translate a point in one coordinate system to a another coordinate system.
   * @param fromPoint
   * @param fromCoordSystem
   * @param toCoordSystem
   * @return
   */
  public Point translate(Point fromPoint, CoordSystem fromCoordSystem, CoordSystem toCoordSystem) {
    // If the systems match, avoid all math.
    if (fromCoordSystem == toCoordSystem) { return new Point(fromPoint); }

    JPanel panel = this;
    double currentZoom = zoomFactor.getAsDouble();

    // 1. Get the current scrolling position from the viewport
    Point scrollPos = scrollPane.getViewport().getViewPosition();

    CoordSystem activeFromSystem = fromCoordSystem;

    // 2. Pre-process: If coming from VIEWPORT, convert it to CANVAS space first
    double workingPanelX = fromPoint.x;
    double workingPanelY = fromPoint.y;

    if (activeFromSystem == CoordSystem.VIEWPORT) {
      workingPanelX += scrollPos.x;
      workingPanelY += scrollPos.y;
      // Now treat it as a CANVAS coordinate for the intermediate calculations
      activeFromSystem = CoordSystem.CANVAS;
    }

    // 3. Calculate dynamic image-space offsets on the panel
    int panelWidth = panel.getWidth();
    int panelHeight = panel.getHeight();
    int screenImageWidth = (int) Math.round(image.getWidth() * currentZoom);
    int screenImageHeight = (int) Math.round(image.getHeight() * currentZoom);

    double offsetXInImageSpace = (panelWidth > screenImageWidth)
                                     ? ((panelWidth - screenImageWidth) / 2.0) / currentZoom
                                     : 0.0;
    double offsetYInImageSpace = (panelHeight > screenImageHeight)
                                     ? ((panelHeight - screenImageHeight) / 2.0) / currentZoom
                                     : 0.0;

    // 4. Step 1: Normalize everything down to raw unscaled IMAGE coordinates
    double imageX = 0;
    double imageY = 0;

    switch (activeFromSystem) {
      case IMAGE -> {
        imageX = fromPoint.x;
        imageY = fromPoint.y;
      }
      case SCALED_IMAGE -> {
        imageX = fromPoint.x - offsetXInImageSpace;
        imageY = fromPoint.y - offsetYInImageSpace;
      }
      case CANVAS -> {
        double scaledMouseX = workingPanelX / currentZoom;
        double scaledMouseY = workingPanelY / currentZoom;
        imageX = scaledMouseX - offsetXInImageSpace;
        imageY = scaledMouseY - offsetYInImageSpace;
      }
    }

    // 5. Step 2: Project out from IMAGE coordinates to the target destination system
    double targetX = 0;
    double targetY = 0;

    // If the target is VIEWPORT, we temporarily compute it as a CANVAS coordinate
    CoordSystem activeToSystem
        = (toCoordSystem == CoordSystem.VIEWPORT) ? CoordSystem.CANVAS : toCoordSystem;

    switch (activeToSystem) {
      case IMAGE -> {
        targetX = imageX;
        targetY = imageY;
      }
      case SCALED_IMAGE -> {
        targetX = imageX + offsetXInImageSpace;
        targetY = imageY + offsetYInImageSpace;
      }
      case CANVAS -> {
        double scaledX = imageX + offsetXInImageSpace;
        double scaledY = imageY + offsetYInImageSpace;
        targetX = scaledX * currentZoom;
        targetY = scaledY * currentZoom;
      }
    }

    // 6. Post-process: If the user explicitly requested VIEWPORT space,
    // subtract scroll offsets
    if (toCoordSystem == CoordSystem.VIEWPORT) {
      targetX -= scrollPos.x;
      targetY -= scrollPos.y;
    }

    return new Point((int) Math.floor(targetX), (int) Math.floor(targetY));
  }
}
