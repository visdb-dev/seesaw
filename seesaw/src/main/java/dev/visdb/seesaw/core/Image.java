/*******************************************************************************
 * Copyright (C) 2003-2021, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
 * All rights reserved.
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
 *
 * Contributors:
 *   Prasanth R. Pasala
 *   Brian E. Pangburn
 *   Diego Gil
 *   Man "Bee" Vo
 *   Ernie R. Rael
 ******************************************************************************/
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2025-2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package dev.visdb.seesaw.core;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.Iterator;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import dev.visdb.seesaw.datasources.RowSetOps;
import dev.visdb.seesaw.navigate.RowsModel;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SSComponent;
import dev.visdb.seesaw.utils.SSUtils;
import dev.visdb.seesaw.utils.ZoomCanvas;
import dev.visdb.seesaw.utils.ZoomCanvas.ResizeMode;

import static dev.visdb.seesaw.core.Image.ScrollBarPolicy.*;
import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;
import static java.nio.file.StandardOpenOption.READ;
import static java.sql.JDBCType.*;

/**
 * Used to load, store, and display images stored in a database.
 * There are controls for zooming the image.
 */
// TODO: Image make all the load/store buttons/capabilities optional.
@SuppressWarnings("serial")
public class Image extends JPanel implements SSComponent, ScrollPaneConstants {
  // TODO: try to get this initialized
  private Path path;
  /**
   * This listener can read a file; create an image from the file;
   * write the image bits to the database, display the image.
   * The first step puts up a FileChooser.
   */
  protected class ImageListener implements ActionListener {
    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent ae) {
      if (getRowSet() == null) // TODO: is this check needed?
        return;

      JFileChooser fc = new JFileChooser();
      if (fc.showOpenDialog(btnUpdateImage) != JFileChooser.APPROVE_OPTION) { return; }

      Path tPath = fc.getSelectedFile().toPath();
      java.awt.Image image;
      try {
        image = createDbImageFromFile(tPath);
      } catch (IOException ioe) {
        SSUtils.reportError(logger, Image.this, "Error accessing image file", tPath, ioe);
        return;
      } catch (SQLException ex) {
        logger.log(Level.ERROR, (String) null, ex);
        return;
      }

      // Display the image
      path = tPath;

      setImage(image);
    }

    private java.awt.Image createDbImageFromFile(Path tPath) throws SQLException, IOException {
      // Read the image into a byte array
      ByteBuffer bb;
      try (SeekableByteChannel rbc = Files.newByteChannel(tPath, EnumSet.of(READ))) {
        int totalLength = (int) rbc.size();
        bb = ByteBuffer.allocate(totalLength);
        int bytesRead = rbc.read(bb);
        if (totalLength != bytesRead)
          throw new IOException(sf("Image expected %d bytes, got %d", totalLength, bytesRead));
      }
      byte[] bytes = bb.array();

      // Verify the bytes are a recognized image
      BufferedImage bimg = ZoomCanvas.bytes2image(bytes);
      if (bimg == null) throw new IOException("Unknown image format");

      // Stage the image to the database
      dbChange(() -> { setColumn(bytes); });

      return bimg;
    }
  } // end private class ImageListener

  /** Logger for component */
  private static final Logger logger = JStuff.getLogger();

  /** Button to update the image. */
  private JButton btnUpdateImage = new JButton("Update");

  /** Area to display the image */
  private ZoomCanvas canvas;

  /** scroll pane holding the image */
  private JScrollPane scrollPane;

  /**
   * Constructs a Image Object bound to the specified column in the specified
   * rowSet.
   *
   * @param rowsModel          - RowSet from/to which data has to be read/written
   * @param columnName - column in the rowSet to which the component should
   *                         be bound.
   */
  public Image(RowsModel rowsModel, String columnName) {
    this();
    rowsModel.bind(this, columnName);
  }

  /**
   * Construct a default Image Object.
   */
  @SuppressWarnings("OverridableMethodCallInConstructor")
  public Image() {
    //System.setProperty("sun.awt.noerasebackground", "true");
    //Toolkit.getDefaultToolkit().setDynamicLayout(false);
    addComponents();
    setupImageCanvasInScrollPane();
    finishSSCommon();

    setColumnReader((rs, cidx, _) -> { return rs.getBytes(cidx); });
    setColumnUpdater((rs, cidx, _, value) -> {
      if (value == null) {
        rs.updateNull(cidx);
        return RowSetOps.UPDATE_NULL;
      } else {
        rs.updateBytes(cidx, (byte[]) value);
        return new RowSetOps.DbUpdate(value);
      }
    });
  }

  /**
   * How is resize handled.
   * @return 
   */
  public ResizeMode getResizeMode() {
    return canvas.getResizeMode();
  }

  /**
   * How to handle resize.
   * @param resizeMode 
   */
  public void setResizeMode(ResizeMode resizeMode) {
    canvas.setResizeMode(resizeMode);
  }


  /** @return the scroll pane */
  protected JScrollPane getScrollPane() { return scrollPane; }

  /**
   * Easy way to set scroll bar policy for both directions. For example
   * {@snippet lang="java":
   * ALWAYS.setPolicy(scrollPane);
   * }
   */
  public enum ScrollBarPolicy {
    /** scroll bars always. */
    ALWAYS,
    /** scroll bars as needed. */
    AS_NEEDED,
    /** never have scroll bars. */
    NEVER;

    /**
     * Apply specified policy.
     * @param jsp target scroll pane
     */
    public void setPolicy(JScrollPane jsp) {
      jsp.setHorizontalScrollBarPolicy(switch (this) {
        case ALWAYS -> HORIZONTAL_SCROLLBAR_ALWAYS;
        case AS_NEEDED -> HORIZONTAL_SCROLLBAR_AS_NEEDED;
        case NEVER -> HORIZONTAL_SCROLLBAR_NEVER;
      });
      jsp.setVerticalScrollBarPolicy(switch (this) {
        case ALWAYS -> VERTICAL_SCROLLBAR_ALWAYS;
        case AS_NEEDED -> VERTICAL_SCROLLBAR_AS_NEEDED;
        case NEVER -> VERTICAL_SCROLLBAR_NEVER;
      });
    }
  }

  private Color nullBackground;
  private Color imageBackground = new Color(35, 35, 40);
  private class MyZoomCanvas extends ZoomCanvas {
    private MyZoomCanvas(JScrollPane scrollPane, DoubleSupplier zoomFactor,
                         Supplier<RenderingQuality> quality) {
      super(scrollPane, zoomFactor, quality);
    }

    @Override
    protected boolean handleResize(ComponentEvent e) {
      if (keepFit) {
        bestFit();
      }
      return keepFit;
    }
  }

  private void setupImageCanvasInScrollPane() {
    canvas = new MyZoomCanvas(scrollPane,
                              () -> zoomFactor,
                              () -> zoomSlider.getValueIsAdjusting()
                                       ? ZoomCanvas.RenderingQuality.MEDIUM
                                       : ZoomCanvas.RenderingQuality.HIGH);
    nullBackground = canvas.getBackground();
    scrollPane.getViewport().setView(canvas);

    imagePopup = createImagePopup();

    if (imagePopup != null) canvas.setToolTipText("""
        <html> Drag to pan image.
        <br>Try the context menu.
        </html>""");

    // Panning - click and drag image around.
    // Also used for popup.
    canvasMouseListener = new CanvasMouseListener();

    // Listeners must be added to both tracking pools for drag movements to register
    canvas.addMouseListener(canvasMouseListener);
    canvas.addMouseMotionListener(canvasMouseListener);
  }

  private class CanvasMouseListener extends MouseAdapter {
    private Point origin; // Stores where the mouse drag started

    @Override
    public void mousePressed(MouseEvent e) { // Panning
      // Capture the starting mouse point, and popup point
      origin = e.getPoint();
      requestFocusInWindow();
      if (tryPopup(e)) return;

      // Change cursor to a hand/grabbing cursor when clicking down
      canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    @Override
    public void mouseReleased(MouseEvent e) { // Panning
      // Capture popup point
      origin = e.getPoint();
      if (tryPopup(e)) return;

      // Restore standard default cursor when letting go
      canvas.setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mouseDragged(MouseEvent e) { // Panning
      if (origin == null) return;

      JViewport viewport = scrollPane.getViewport();
      Point viewPos = viewport.getViewPosition();

      // Calculate how many pixels the mouse moved from the starting origin
      int deltaX = origin.x - e.getX();
      int deltaY = origin.y - e.getY();

      // Shift the viewport view position by that delta
      int newX = viewPos.x + deltaX;
      int newY = viewPos.y + deltaY;

      // Clamp the scroll bounds to prevent scrolling past image borders
      int maxX = canvas.getWidth() - viewport.getWidth();
      int maxY = canvas.getHeight() - viewport.getHeight();

      if (newX < 0) newX = 0;
      if (newY < 0) newY = 0;
      if (newX > maxX) newX = Math.max(0, maxX);
      if (newY > maxY) newY = Math.max(0, maxY);

      viewport.setViewPosition(new Point(newX, newY));
    }
  }

  private CanvasMouseListener canvasMouseListener;
  private JPopupMenu imagePopup;

  /** @return true if a popupTrigger */
  private boolean tryPopup(MouseEvent e) {
    if (!e.isPopupTrigger()) return false;
    if (imagePopup != null) {
      Point pos = e.getPoint();
      imagePopup.show(e.getComponent(), pos.x, pos.y);
    }
    return true;
  }

  /**
   * @return popup menu for the image
   */
  protected JPopupMenu createImagePopup() {
    imagePopup = new JPopupMenu();
    imagePopup.add(menuAction("Center mouse point",
                              () -> { canvas.centerViewportOnCanvasPoint(canvasMouseListener.origin); }));
    imagePopup.add(menuAction("Fit image", () -> { bestFit(); }));
    imagePopup.add(menuAction("Zoom factor 1.0", () -> { resetZoom(); }));
    return imagePopup;
  }
  private Action menuAction(String s, Runnable l) {
    return new AbstractAction(s) {
      @Override
      public void actionPerformed(ActionEvent e) {
        l.run();
      }
    };
  }

  /**
   * Create controls; return goes SOUTH of a BoarderLayout.
   * Override to customize features and layout.
   * @return panel with buttons.
   */
  protected JComponent imageControls() {
    getButtonnUpdate().setText("Upd");
    getSliderReset().setText("Rst");

    //Box controls = new Box(BoxLayout.Y_AXIS);
    Box controls = Box.createVerticalBox();
    controls.add(getZoomSlider());
    JPanel buttons = new JPanel();
    buttons.add(getButtonnUpdate());
    buttons.add(getSliderReset());
    buttons.add(getCurrentZoom());
    controls.add(buttons);
    return controls;
  }

  /**
   * Set focus/decorator targets.
   */
  @Override
  public void customInit() {
    // Decorator.DecoratorStyle style = def.lookup(Decorator.DecoratorStyle.class);
    //setDecorateTarget(btnUpdateImage);
    setFocusTarget(btnUpdateImage);
  }

  /** {@inheritDoc } */
  @Override
  public void checkColumnType(JDBCType jdbcType) throws IllegalArgumentException {
    Set<JDBCType> allowed = Set.of(BLOB, BINARY, VARBINARY, LONGVARBINARY);
    if (!allowed.contains(jdbcType))
      throw new IllegalArgumentException(sf("Image column type must be one of %s", allowed));
  }

  /**
   * This component contains multiple components some of which can get focus.
   * @return true
   */
  @Override
  public boolean isComposite() {
    return true;
  }

  private void setBufferedImage(BufferedImage bi) {
    originalImage = bi;
    canvas.setImage(bi);
  }

  /** Adds the label and button to the panel */
  protected void addComponents() {
    buildZoomControls();
    trimHeight(getButtonnUpdate(), 20);
    trimHeight(getSliderReset(), 20);
    //trimHeight(currentZoomLabel, 20);

    setLayout(new BorderLayout());
    scrollPane = new JScrollPane(null, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                 ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    add(scrollPane);
    add(imageControls(), BorderLayout.SOUTH);
  }

  /**
   * Set the component's viewheight.
   *
   * @param jc
   * @param height
   */
  private void trimHeight(JComponent jc, int height) {
    Dimension prefSize = jc.getPreferredSize();
    jc.setPreferredSize(new Dimension(prefSize.width, height));
  }

  /**
   * Returns the button that indicates a new image has been selected and accepted.
   *
   * @return button that indicates a new image has been selected and accepted
   */
  protected JButton getButtonnUpdate() { return btnUpdateImage; }

  /** @return current zoom factor */
  protected double getZoomFactor() { return zoomFactor; }

  /** @return component used to adjust the zoom */
  protected JSlider getZoomSlider() { return zoomSlider; }

  /** @return component that display the current zoom */
  protected JLabel getCurrentZoom() { return currentZoomLabel; }

  /** @return component button that resets the zoom */
  protected JButton getSliderReset() { return sliderResetButton; }

  /** {@inheritDoc } */
  @Override
  public void cleanField() {
    setImage(null);
  }

  /**
   * Updates the value stored and displayed in the SwingSet component based on
   * getColumnText().
   * <p>
   * Call to this method should be coming from SSCommon and should already have
   * the Component listener removed.
   */
  public void updateComponent() {
    java.awt.Image image;
    try {
      byte[] imageData = (byte[]) getColumn();

      if (imageData != null) {
        logger.log(DEBUG, () -> sf("%s: Setting non-null image.", getColumnForLog()));
        image = ZoomCanvas.bytes2image(imageData);
      } else {
        logger.log(DEBUG, () -> sf("%s: Setting null image.", getColumnForLog()));
        image = null;
      }
    } catch (SQLException | IOException ex) {
      logger.log(Level.ERROR, getColumnForLog() + ": Exception.", ex);
      image = null;
    }

    setImage(image);
  }

  // only here for play at this time
  // TODO: could incorporate this into main
  //		 by continue with reading after getting format
  @SuppressWarnings("unused")
  private String getImageFormat(ImageInputStream iis) throws IOException {
    String formatName = null;
    Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
    if (readers.hasNext()) {
      ImageReader reader = readers.next();
      formatName = reader.getFormatName(); // Returns "JPEG", "png", etc.
      //System.out.println("Image Format: " + formatName);

      // Optionally continue reading the image with the same reader
      //reader.setInput(iis);
      //BufferedImage image = reader.read(0);
    } else throw new IOException("Unknown image format");
    return formatName;
  }

  private Hook hook;

  /** {@inheritDoc } */
  @Override
  public final Hook getSSComponentHook() {
    if (hook == null)
      hook = new Hook(this) {
        @Override
        protected void updateSSComponent() {
          updateComponent();
        }

        /** {@inheritDoc } */
        @Override
        protected ImageListener getSSComponentListener() {
          return new ImageListener();
        }

        /** {@inheritDoc } */
        @Override
        protected void addSSComponentListener(EventListener eventListener) {
          getButtonnUpdate().addActionListener((ActionListener) eventListener);
        }

        /** {@inheritDoc } */
        @Override
        protected void removeSSComponentListener(EventListener eventListener) {
          getButtonnUpdate().removeActionListener((ActionListener) eventListener);
        }
      };
    return hook;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return sf("%s{file=%s, %s}", getClass().getSimpleName(), path != null ? path.toString() : "",
              SSUtils.ssComponentToString(this));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Google search AI for adding zoom
  //

  private BufferedImage originalImage;
  private double zoomFactor = 1.0;
  /**
   * When true on resize keep zoom to fit.
   * Initialized to true when setImage(), stays true during resize.
   * A zoom action sets it to false.
   */
  private boolean keepFit;

  // Slider constants
  private static final int MIN_SLIDER = 10;
  private static final int MAX_SLIDER = 400;
  private static final int INIT_SLIDER = 100;
  private static final double MIN_ZOOM = 0.1;
  private static final double MAX_ZOOM = 4.0;

  /** zoom slider. */
  private JSlider zoomSlider;
  /** current zoom */
  private JLabel currentZoomLabel;
  /** set zoom to 1, no zoom */
  private JButton sliderResetButton;
  private boolean isUpdatingControls = false; // Prevents recursive listener execution

  private Point lockedViewportCenter = null;
  private double zoomFactorAtClick = 1.0;

  // Update your JSlider constructor and listeners inside the main constructor:
  private void setupSlider() {
    zoomSlider = new JSlider(JSlider.HORIZONTAL, MIN_SLIDER, MAX_SLIDER, INIT_SLIDER);
    zoomSlider.setMajorTickSpacing(100);
    zoomSlider.setMinorTickSpacing(25);
    // zoomSlider.setPaintTicks(true);
    // zoomSlider.setPaintLabels(true);

    // 1. Capture the visual center point ONCE when the user clicks/presses the slider
    zoomSlider.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // calculate the TRUE image center. Taking into account
        // the padding added because the image is CENTERED

        zoomFactorAtClick = zoomFactor;
        lockedViewportCenter = canvas.captureImagePointAtViewportCenter();

        logger.log(Level.DEBUG, sf("zoomAtClick %.2f, locked %s", zoomFactorAtClick,
                                   lockedViewportCenter.toString()));

        // Show the tooltip immediately upon user clicking down
        updateFloatingTooltip();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        // Clear the anchor lock when the user releases the slider
        lockedViewportCenter = null;

        // Hide the tooltip seamlessly when user releases the slider handle
        hideFloatingTooltip();
        canvas.repaint();
      }
    });

    // 2. Continuous updates while dragging use the LOCKED anchor point
    zoomSlider.addChangeListener(e -> {
      if (!isUpdatingControls) {
        setKeepFit(false); // User's zooming.
        double targetZoom = zoomSlider.getValue() / 100.0;
        updateZoom(targetZoom);

        // Force the tooltip to refresh text and update X coordinates
        // dynamically while dragging
        if (zoomSlider.getValueIsAdjusting()) { updateFloatingTooltip(); }
      }
    });
  }

  // If want zoom in/out buttons, flip between buttons and slider
  private void buildZoomControls() {
    // // 3. Build the Button Layout Panel
    // JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
    // JButton zoomInButton = new JButton("Zoom In (+)");
    // JButton zoomOutButton = new JButton("Zoom Out (-)");
    // JButton buttonReset = new JButton("Reset (100%)");
    //
    // zoomInButton.addActionListener(e -> stepZoom(0.25));
    // zoomOutButton.addActionListener(e -> stepZoom(-0.25));
    // zoomInButton.addActionListener(e -> zoomOneShot(zoomFactor + 0.25)); // USE
    // zoomOutButton.addActionListener(e -> zoomOneShot(zoomFactor - 0.25)); // USE
    // buttonReset.addActionListener(e -> resetZoom());
    //
    // buttonPanel.add(zoomOutButton);
    // buttonPanel.add(buttonReset);
    // buttonPanel.add(zoomInButton);

    // 4. Build the Slider Layout Panel
    // JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
    // zoomSlider = new JSlider(JSlider.HORIZONTAL, MIN_SLIDER, MAX_SLIDER, INIT_SLIDER);
    // zoomSlider.setMajorTickSpacing(100);
    // zoomSlider.setMinorTickSpacing(25);
    // zoomSlider.setPaintTicks(true);
    // zoomSlider.setPaintLabels(true);
    //
    // zoomSlider.addChangeListener(e -> {
    //     if (!isUpdatingControls) {
    //         zoomFactor = zoomSlider.getValue() / 100.0;
    //         updateImage();
    //     }
    // });
    //zoomSlider = createZoomSlider();
    setupSlider();
    currentZoomLabel = new JLabel();
    sliderResetButton = new JButton("Reset");
    sliderResetButton.addActionListener(e -> resetZoom());

    // // 5. Use CardLayout to easily swap between Button Panel and Slider Panel
    // CardLayout controlCardLayout = new CardLayout();
    // JPanel cardContainer = new JPanel(controlCardLayout);
    // cardContainer.add(buttonPanel, "BUTTONS");
    // cardContainer.add(sliderPanel, "SLIDER");

    // // 6. Build the Mode Selector Dropdown (Top of screen)
    // JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
    // selectorPanel.add(new JLabel("Control Mode:"));
    //
    // String[] modes = {"Buttons Control", "Slider Control"};
    // JComboBox<String> modeMenu = new JComboBox<>(modes);
    // modeMenu.addActionListener(e -> {
    //     int selectedIndex = modeMenu.getSelectedIndex();
    //     if (selectedIndex == 0) {
    //         controlCardLayout.show(cardContainer, "BUTTONS");
    //     } else {
    //         controlCardLayout.show(cardContainer, "SLIDER");
    //     }
    // });
    // selectorPanel.add(modeMenu);
  }

  private void setImage(java.awt.Image newImage) {
    if (newImage == null) {
      canvas.setBackground(nullBackground);
      setBufferedImage(null);
      updateZoom(0);
      return;
    }
    canvas.setBackground(imageBackground);

    // 1. Convert to BufferedImage if it isn't one already to ensure stable measurement
    if (newImage instanceof BufferedImage bufferedImage) {
      setBufferedImage(bufferedImage);
    } else {
      // Create an empty buffered image with the correct pixel channels
      BufferedImage bImage = new BufferedImage(newImage.getWidth(null), newImage.getHeight(null),
                                               BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = bImage.createGraphics();
      g2d.drawImage(newImage, 0, 0, null);
      g2d.dispose();
      setBufferedImage(ZoomCanvas.toCompatibleImage(bImage));
    }

    // 2. Calculate the optimal "Fit" zoom factor
    // We target the parent scroll pane's viewport size. If the window isn't
    // layout-rendered yet,
    // we fall back to standard 1.0 (100%) scaling.

    bestFit();
  }

  private void setKeepFit(boolean keepFit) {
    this.keepFit = keepFit;
    if (keepFit) NEVER.setPolicy(scrollPane);
    else ALWAYS.setPolicy(scrollPane);
  }

  // To get the viewort size exactly can do various things
  // Note jsp.revalidate(); jsp.validate(); is not safe since from event
  // could cause layout loops.
  // 1. invokeLater, then that's after relayout.
  // 2. manually subtract the scrollpane insets from the scrollpan.

  /**
   * Zoom the image so that it's as big as possible
   * and fits inside the scrollPane.
   */
  protected void bestFit() {
    if (originalImage == null) return;
    setKeepFit(true);
    // let the scrollbar layout settle
    // Using invokeLater so layout settles, gets a visible double draw.
    laterBestFit();
  }

  private void laterBestFit() {
    // For best fit, use the entire scrollPane area. The scrollBars may still be
    // present, even though they were turned off in setKeepFit(). To use
    // scrollPane.getViewport().getSize(), need to wait for layout to settle,
    // by using invokeLater, but that produces visual artifacts with double draw.
    // Instead calculate the viewport area from the scrollPane.

    Insets insets = scrollPane.getInsets();
    int viewwidth = scrollPane.getSize().width - insets.left - insets.right;
    int viewheight = scrollPane.getSize().height - insets.top - insets.bottom;
    double targetZoom;

    if (viewwidth > 0 && viewheight > 0) {
      // Take the smaller ratio to ensure the image is entirely visible
      double widthRatio = (double) viewwidth / originalImage.getWidth();
      double heightRatio = (double) viewheight / originalImage.getHeight();
      targetZoom = Math.min(widthRatio, heightRatio);

      // Safety bounds checks to ensure it complies with our UI Slider
      // constraints (10% to 400%)
      if (targetZoom < MIN_ZOOM) targetZoom = MIN_ZOOM;
      if (targetZoom > MAX_ZOOM) targetZoom = MAX_ZOOM;
    } else {
      // Does this belong here? Shoudl it be in setImage?
      redoImageWithDimensions();
      return;
    }
    // Initialize fitting zoom without anchoring corrections
    updateZoom(capZoom(targetZoom));

    // Snap scrollbars cleanly to center for best fit
    centerScrollPane();
  }

  private double capZoom(double targetZ) {
    double targetZoom = targetZ;
    if (targetZoom < MIN_ZOOM) targetZoom = MIN_ZOOM;
    if (targetZoom > MAX_ZOOM) targetZoom = MAX_ZOOM;
    return targetZoom;
  }

  /**
   * Executes a one-shot zoom adjustment while anchoring
   * the transformation to the current visual center.
   * @param targetZoom
   */
  protected void zoomOneShot(double targetZoom) {
    if (originalImage == null) return;
    setKeepFit(false); // User's zooming.

    this.zoomFactorAtClick = this.zoomFactor;
    this.lockedViewportCenter = canvas.captureImagePointAtViewportCenter();

    // Do the zoom, then clear state.
    updateZoom(capZoom(targetZoom));
    this.lockedViewportCenter = null;
  }

  /** Set the zoom to 1.0 */
  protected void resetZoom() {
    // Snaps cleanly back to standard 100% dimensions without drifting
    zoomOneShot(1.0);
  }

  private void reportZoom() { currentZoomLabel.setText(sf("%.2f", zoomFactor)); }

  // Ensures the hidden or visible slider matches the current zoom calculation
  private void syncSliderUI() {
    isUpdatingControls = true;
    zoomSlider.setValue((int) (zoomFactor * 100));
    isUpdatingControls = false;
  }

  private void updateZoom(double newZoomFactor) {
    this.zoomFactor = newZoomFactor;
    reportZoom();

    // Sync Slider
    syncSliderUI();

    if (originalImage == null) return;

    // Trigger immediate layout recalculation and screen redraw
    canvas.revalidate();
    canvas.repaint();

    // Process scrollbar locking using the anchor point
    if (lockedViewportCenter != null && zoomFactorAtClick > 0) {
      double cumulativeScaleChange = zoomFactor / zoomFactorAtClick;

      int projectedAnchorX = (int) (lockedViewportCenter.x * cumulativeScaleChange);
      int projectedAnchorY = (int) (lockedViewportCenter.y * cumulativeScaleChange);

      Dimension viewSize = scrollPane.getViewport().getSize();
      int newImgWidth = (int) (originalImage.getWidth() * zoomFactor);
      int newImgHeight = (int) (originalImage.getHeight() * zoomFactor);

      int futureLabelWidth = Math.max(viewSize.width, newImgWidth);
      int futureLabelHeight = Math.max(viewSize.height, newImgHeight);

      int futureOffsetX = (futureLabelWidth - newImgWidth) / 2;
      int futureOffsetY = (futureLabelHeight - newImgHeight) / 2;

      Point targetScrollPos = new Point((projectedAnchorX + futureOffsetX) - viewSize.width / 2,
                                        (projectedAnchorY + futureOffsetY) - viewSize.height / 2);

      // Update scrollbars smoothly in sync with the repaint
      scrollPane.getViewport().setViewPosition(targetScrollPos);
    }
  }

  /** Force the scroll pane to reset its scroll bars to the middle */
  protected void centerScrollPane() { SwingUtilities.invokeLater(this::centerScrollbarsInstantly); }

  private void centerScrollbarsInstantly() {
    scrollPane.getHorizontalScrollBar().setValue(
        (scrollPane.getHorizontalScrollBar().getMaximum()
         - scrollPane.getHorizontalScrollBar().getVisibleAmount())
        / 2);
    scrollPane.getVerticalScrollBar().setValue(
        (scrollPane.getVerticalScrollBar().getMaximum()
         - scrollPane.getVerticalScrollBar().getVisibleAmount())
        / 2);
  }

  /**
   * When the viewport is laid out, re-set the image.
   */
  protected void redoImageWithDimensions() {
    logger.log(DEBUG, "Start listening for viewport size");
    scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        // Triggered as soon as the viewport gets its initial size

        JViewport viewport = (JViewport) e.getComponent();
        Dimension size = viewport.getSize();
        if (size.height > 0 && size.width > 0) {
          setImage(originalImage);
          // Remove the listener, only need the initial size once
          viewport.removeComponentListener(this);
          logger.log(DEBUG, "Stop listening for viewport size");
        } else logger.log(DEBUG, "Keep listening for viewport size");
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Popup over slider thumb with zoom factor.
  //

  // import javax.swing.JToolTip;
  // import javax.swing.Popup;
  // import javax.swing.PopupFactory;

  // ... inside your ImageZoomApp class ...
  private JToolTip customTooltip;
  private Popup tooltipPopup;

  private void updateFloatingTooltip() {
    hideFloatingTooltip();

    if (zoomSlider == null || !zoomSlider.isShowing()) return;

    if (customTooltip == null) { customTooltip = zoomSlider.createToolTip(); }

    // Set active text layout format (e.g., "150%")
    customTooltip.setTipText((int) (zoomFactor * 100) + "%");

    // --- ACCURATE MATHEMATICAL THUMB-X CALCULATION ---

    // 1. Determine total component dimensions
    int sliderWidth = zoomSlider.getWidth();
    int min = zoomSlider.getMinimum();
    int max = zoomSlider.getMaximum();
    int val = zoomSlider.getValue();

    // 2. Establish approximate standard UI padding offsets
    // Swing horizontal sliders reserve a small padding margin on the left/right
    // edges so the thumb graphic doesn't visually bleed outside the component frame.
    int trackPaddingLeft = 10;
    int trackPaddingRight = 10;
    int usableWidth = sliderWidth - (trackPaddingLeft + trackPaddingRight);

    // 3. Compute relative percentage placement
    double positionPercentage = (double) (val - min) / (max - min);

    // 4. Trace the pixel center coordinate of the thumb
    int thumbX = trackPaddingLeft + (int) (positionPercentage * usableWidth);

    // --- SCREEN MATRIX TRANSLATION ---

    // Translate local slider coordinates out to global OS screen coordinates
    Point sliderScreenPos = zoomSlider.getLocationOnScreen();

    // Center alignment adjustment: Subtract half of the tooltip viewwidth
    // so the floating bubble balances directly over the thumb's core center-axis.
    int tooltipHalfWidth = customTooltip.getPreferredSize().width / 2;
    int popupX = sliderScreenPos.x + thumbX - tooltipHalfWidth;
    int popupY
        = sliderScreenPos.y - customTooltip.getPreferredSize().height - 5; // 5px padding above

    // Generate and reveal the absolute overlay panel window safely
    tooltipPopup
        = PopupFactory.getSharedInstance().getPopup(zoomSlider, customTooltip, popupX, popupY);
    tooltipPopup.show();
  }

  private void hideFloatingTooltip() {
    if (tooltipPopup != null) {
      tooltipPopup.hide();
      tooltipPopup = null;
    }
  }
}
