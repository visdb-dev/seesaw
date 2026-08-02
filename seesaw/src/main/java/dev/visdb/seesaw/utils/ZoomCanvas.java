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

package dev.visdb.seesaw.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

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

  /** How to handle resize. */
  public enum ResizeMode {
    /** Keep the same point centered during resize. */
    CENTER_PANNING,
    /** Crop/pad right/bottom edge movement; lock top/left edge movement. */
    CROP_PAD
  }

  private final JScrollPane scrollPane;
  private final DoubleSupplier zoomFactor;
  private BufferedImage image;
  private final Supplier<RenderingQuality> quality;
  private ResizeMode resizeMode = ResizeMode.CROP_PAD; // true for "Other" style.

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
    setupScrollPaneCropPadCenterPanListener();
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
   * How is resize handled.
   * @return 
   */
  public ResizeMode getResizeMode() {
    return resizeMode;
  }

  /**
   * How to handle resize.
   * @param resizeMode 
   */
  public void setResizeMode(ResizeMode resizeMode) {
    this.resizeMode = resizeMode;
  }

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
   * The returned image is a "compatible" image,
   * {@link GraphicsConfiguration#createCompatibleImage(int, int, int) }
   * 
   * @param bytes
   * @return image for the bytes
   * @throws IOException
   */
  public static BufferedImage bytes2image(byte[] bytes) throws IOException {
    try (InputStream is = new ByteArrayInputStream(bytes);) {
      BufferedImage bimg = toCompatibleImage(ImageIO.read(is));
      return bimg;
    }
  }

  /**
   * When you load your image from a file, convert it into a Hardware-Compatible
   * Image immediately. This caches the image directly inside VRAM (Video RAM),
   * making g2d.drawImage() run at hardware speed.
   * <p>
   * Usage: When loading the file, wrap it: {@snippet :
   *     originalImage = toCompatibleImage(ImageIO.read(file));
   * }
   *
   * @param image
   * @return
   */
  public static BufferedImage toCompatibleImage(BufferedImage image) {
    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                   .getDefaultScreenDevice()
                                   .getDefaultConfiguration();

    // Create a canvas customized exactly for the local GPU layout
    // NOTE: wonder about the volatile versions of createCompatibleImage?
    BufferedImage compatibleImage
        = gc.createCompatibleImage(image.getWidth(), image.getHeight(), image.getTransparency());

    Graphics2D g2d = compatibleImage.createGraphics();
    g2d.drawImage(image, 0, 0, null);
    g2d.dispose();

    return compatibleImage;
  }

  /**
   * Adjust scrollPane so {@code canvasPoint} is centered in JScrollPane.
   * @param canvasPoint
   */
  public void centerViewportOnCanvasPoint(Point canvasPoint) {
    if (image == null) return;

    JViewport viewport = scrollPane.getViewport();
    Dimension viewportSize = viewport.getSize();

    // Calculate the target top-left position for the viewport
    int targetX = canvasPoint.x - (viewportSize.width / 2);
    int targetY = canvasPoint.y - (viewportSize.height / 2);

    // Bound the values so we don't scroll past the panel borders
    Dimension canvasSize = getSize(); // The custom JPanel size
    targetX = Math.max(0, Math.min(targetX, canvasSize.width - viewportSize.width));
    targetY = Math.max(0, Math.min(targetY, canvasSize.height - viewportSize.height));

    // Apply the new position to the viewport
    viewport.setViewPosition(new Point(targetX, targetY));
  }

  /**
   * Captures the current visual center point of the viewport,
   * normalized strictly to the underlying image's pixel grid.
   * @return the image point that is centered in the viewport.
   */
  public Point captureImagePointAtViewportCenter() {
    if (image == null) return new Point(0, 0);

    // 1. Get current physical component sizes
    int canvasWidth = getWidth();
    int canvasHeight = getHeight();

    double currentZoom = zoomFactor.getAsDouble();
    int imgWidth = (int) (image.getWidth() * currentZoom);
    int imgHeight = (int) (image.getHeight() * currentZoom);

    // 2. Find the padding offsets inside the centered canvas
    int imageOffsetX = Math.max(0, (canvasWidth - imgWidth) / 2);
    int imageOffsetY = Math.max(0, (canvasHeight - imgHeight) / 2);

    // 3. Find the visual center point of what the user sees
    Point viewPos = scrollPane.getViewport().getViewPosition();
    Dimension viewSize = scrollPane.getViewport().getSize();

    int viewCenterX = viewPos.x + viewSize.width / 2;
    int viewCenterY = viewPos.y + viewSize.height / 2;

    // 4. Return the normalized coordinate relative to the photo bounds
    return new Point(viewCenterX - imageOffsetX, viewCenterY - imageOffsetY);
  }

  private void setupScrollPaneCropPadCenterPanListener() {
    scrollPane.addComponentListener(new ComponentAdapter() {
      private Dimension oldViewportSize;

      @Override
      public void componentResized(ComponentEvent e) {
        Dimension newViewportSize = scrollPane.getViewport().getSize();
        try {
          // Skip if it's the very first initial layout pass or there's no image
          if (oldViewportSize == null || image == null)
            return;
          // Give canvas extender a chance.
          if (handleResize(e))
            return;

          // Only calculate if the window size actually changed
          if (newViewportSize.width == oldViewportSize.width
              && newViewportSize.height == oldViewportSize.height) {
            return;
          }
          
          switch (resizeMode) {
            // center-panning calculation
            case CENTER_PANNING -> recenterViewOnResize(oldViewportSize, newViewportSize);
            // top-left pinning, bottom-right crop/pad stability calculation
            case CROP_PAD -> stabilizeImageOnResize(oldViewportSize, newViewportSize);
          }
        } finally {
          oldViewportSize = new Dimension(newViewportSize);
        }
      }
    });
  }

  /**
   * Called when a scrollPane component resize occurs.
   * This can short circuit {@link ResizeMode} for special handling, like bestFit.
   * @param e the resize event
   * @return true to finish resize handling, else false continues handling.
   */
  protected boolean handleResize(ComponentEvent e) {
    return false; // continue normal handling.
  }

  /**
   * For CropPad
   * Strip away the dynamic JPanel centering padding, targets the raw image bounds,
   * and forces the scrollpane to match the anchor precisely.
   * @param oldViewportSize
   */
  private void stabilizeImageOnResize(Dimension oldViewportSize, Dimension newViewportSize) {
    if (image == null) return;

    // 1. Get current zoom and image specs
    double currentZoom = zoomFactor.getAsDouble();
    int imgW = (int) (image.getWidth() * currentZoom);
    int imgH = (int) (image.getHeight() * currentZoom);

    // 2. Calculate the old centering padding offsets that were active BEFORE the layout refreshes
    int oldCanvasWidth = Math.max(oldViewportSize.width, imgW);
    int oldCanvasHeight = Math.max(oldViewportSize.height, imgH);
    int oldOffsetX = (oldCanvasWidth - imgW) / 2;
    int oldOffsetY = (oldCanvasHeight - imgH) / 2;

    // 3. Find the exact position of the view relative to the IMAGE'S top-left pixel (0,0)
    Point currentScrollPos = scrollPane.getViewport().getViewPosition();
    int relativeImageX = currentScrollPos.x - oldOffsetX;
    int relativeImageY = currentScrollPos.y - oldOffsetY;
    //Notice the following is negative of relativeImage[XY] just calculated
    //Point v = translateCoord(new Point(0, 0), CoordSystem.IMAGE, CoordSystem.VIEWPORT);

    // 4. Calculate what the NEW centering padding offsets WILL BE in the new window dimensions
    int newCanvasWidth = Math.max(newViewportSize.width, imgW);
    int newCanvasHeight = Math.max(newViewportSize.height, imgH);
    int newOffsetX = (newCanvasWidth - imgW) / 2;
    int newOffsetY = (newCanvasHeight - imgH) / 2;

    // 5. Reconstruct the absolute target scroll position by mapping the relative coordinate back
    int targetScrollX = relativeImageX + newOffsetX;
    int targetScrollY = relativeImageY + newOffsetY;

    // 6. Clamp the scroll parameters so they don't break scrollpane maximum boundaries
    int maxScrollX = newCanvasWidth - newViewportSize.width;
    int maxScrollY = newCanvasHeight - newViewportSize.height;

    if (targetScrollX < 0) targetScrollX = 0;
    if (targetScrollY < 0) targetScrollY = 0;
    if (targetScrollX > maxScrollX) targetScrollX = Math.max(0, maxScrollX);
    if (targetScrollY > maxScrollY) targetScrollY = Math.max(0, maxScrollY);

    // 7. Snap the viewport position instantly
    Point finalScrollPos = new Point(targetScrollX, targetScrollY);
    javax.swing.SwingUtilities.invokeLater(
        () -> { scrollPane.getViewport().setViewPosition(finalScrollPos); });
  }

  /**
   * For CenterPanning.
   * Determines where the viewport's center used to be, tracks how much the
   * viewable space grew or shrank, and applies the compensation to the scrollbars.
   * @param oldViewportSize
   */
  private void recenterViewOnResize(Dimension oldViewportSize, Dimension newViewportSize) {
    if (image == null) return;

    // 1. Get the current top-left scroll position
    Point currentScrollPos = scrollPane.getViewport().getViewPosition();

    // 2. Find the absolute pixel coordinate that was dead-center in the old window size
    int oldCenterX = currentScrollPos.x + (oldViewportSize.width / 2);
    int oldCenterY = currentScrollPos.y + (oldViewportSize.height / 2);

    // 3. Subtract half of the NEW window size to find the new target scroll position
    // This calculation shifts the scroll window around the locked center point
    int targetScrollX = oldCenterX - (newViewportSize.width / 2);
    int targetScrollY = oldCenterY - (newViewportSize.height / 2);

    // 4. Calculate maximum bounds to prevent scrolling out into empty background space
    int maxScrollX = getWidth() - newViewportSize.width;
    int maxScrollY = getHeight() - newViewportSize.height;

    // 5. Clamp the values safely within bounds
    if (targetScrollX < 0) targetScrollX = 0;
    if (targetScrollY < 0) targetScrollY = 0;
    if (targetScrollX > maxScrollX) targetScrollX = Math.max(0, maxScrollX);
    if (targetScrollY > maxScrollY) targetScrollY = Math.max(0, maxScrollY);

    // 6. Update the viewport coordinates immediately
    Point finalScrollPos = new Point(targetScrollX, targetScrollY);
    javax.swing.SwingUtilities.invokeLater(
        () -> { scrollPane.getViewport().setViewPosition(finalScrollPos); });
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
     * Raw, unscaled pixel coordinates directly mapping onto the underlying
     * BufferedImage indices (0 to width-1, 0 to height-1).
     */
    IMAGE,

    /**
     * Pixels in the intermediate coordinate matrix *after* g2d.scale() is applied,
     * but *before* image offsets are applied. Measured in unscaled image units.
     */
    SCALED_IMAGE,
  }

  /**
   * Translate a point in one coordinate system to the same position
   * in another coordinate system.
   * @param fromPoint
   * @param fromCoordSystem
   * @param toCoordSystem
   * @return coordinates of the position in the target coordinate system
   */
  public Point translateCoord(Point fromPoint, CoordSystem fromCoordSystem, CoordSystem toCoordSystem) {
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
