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

package dev.visdb.seesaw.html;

import java.awt.Component;
import java.awt.EventQueue;
import java.net.InetSocketAddress;
import java.sql.JDBCType;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import javax.sql.RowSet;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicHTML;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.visdb.seesaw.SsComboBox1;
import dev.visdb.seesaw.SsComboBox2;
import dev.visdb.seesaw.SsLabel;
import dev.visdb.seesaw.SsList1;
import dev.visdb.seesaw.SsTable;
import dev.visdb.seesaw.datasources.products.DbSupportFactory;
import dev.visdb.seesaw.html.Rules.OnRefusal;
import dev.visdb.seesaw.html.SsHtml.Display;
import dev.visdb.seesaw.mock.H2;
import dev.visdb.seesaw.mock.TestLogging;
import dev.visdb.seesaw.mock.Util;
import dev.visdb.seesaw.navigate.RowsModel;
import dev.visdb.seesaw.utils.SsUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SsHtmlPresenter, and that the components which put database text in a
 * label go through SsHtml: SsLabel, SsTable's renderers, and the renderers
 * of the combo boxes and the list.
 * <p>
 * The text that must not be rendered has an image on a local server;
 * nothing may be fetched from it.
 */
public class SsHtmlComponentsTest {
  private static final String GOOD = "<html><b>good</b>";
  private static final String PLAIN = "plain <b>text</b>";
  private static String bad;

  private static HttpServer server;
  private static final List<String> fetched = new CopyOnWriteArrayList<>();

  @BeforeAll
  static void setUpClass() throws Exception {
    Util.seesawInit();
    SsUtils.isJunit(); // Make sure it's set; it can not tell on the event thread.
    TestLogging.load();
    DbSupportFactory.setDbSupportGlobalOption(H2.getCon());
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      fetched.add(exchange.getRequestURI().getPath());
      exchange.sendResponseHeaders(404, -1);
      exchange.close();
    });
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    bad = "<html><img src='http://127.0.0.1:" + server.getAddress().getPort() + "/x.png'>bad";
  }

  @AfterAll
  static void tearDownClass() throws InterruptedException {
    // Anything that was fetched has arrived by now.
    Thread.sleep(500);
    server.stop(0);
    assertThat(fetched).isEmpty();
  }

  private interface Body {
    void run() throws Exception;
  }

  private static void onEdt(Body body) throws Exception {
    Exception[] ex = new Exception[1];
    EventQueue.invokeAndWait(() -> {
      try {
        body.run();
      } catch (Exception ex01) {
        ex[0] = ex01;
      }
    });
    if (ex[0] != null)
      throw ex[0];
  }

  /** Did the label make an HTML view of its text. */
  private static boolean rendersHtml(Component comp) {
    return ((JLabel) comp).getClientProperty(BasicHTML.propertyKey) != null;
  }

  private static void assertShows(Component comp, String text, boolean rendered) {
    assertThat(((JLabel) comp).getText()).isEqualTo(text);
    assertThat(rendersHtml(comp)).as(text).isEqualTo(rendered);
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // SsHtmlPresenter
  //

  @Test
  void testPresenter() throws Exception {
    onEdt(() -> {
      SsHtmlPresenter presenter = new SsHtmlPresenter(() -> "testPresenter");
      assertThat(presenter.getSsHtml()).isSameAs(SsHtml.getDefault());
      JLabel label = new JLabel();

      label.setText(presenter.prepare(label, PLAIN));
      assertShows(label, PLAIN, false);
      label.setText(presenter.prepare(label, null));
      assertThat(label.getText()).isNull();
      label.setText(presenter.prepare(label, GOOD));
      assertShows(label, GOOD, true);
      label.setText(presenter.prepare(label, bad));
      assertShows(label, bad, false);
      assertThat(label.getClientProperty("html.disable")).isEqualTo(Boolean.TRUE);
      // The same component, the next text; as for a renderer.
      label.setText(presenter.prepare(label, GOOD));
      assertShows(label, GOOD, true);
      assertThat(label.getClientProperty("html.disable")).isNull();

      presenter.setSsHtml(SsHtml.plain());
      assertThat(presenter.getSsHtml()).isSameAs(SsHtml.plain());
      label.setText(presenter.prepare(label, GOOD));
      assertShows(label, GOOD, false);

      presenter.setSsHtml(SsHtml.checker(Rules.DEFAULT.withOnRefusal(OnRefusal.TEXT_ONLY)));
      label.setText(presenter.prepare(label, bad));
      assertShows(label, "bad", false);

      presenter.setSsHtml(null);
      assertThat(presenter.getSsHtml()).isSameAs(SsHtml.getDefault());
    });
  }

  /** A component that the application made plain stays plain. */
  @Test
  void testPresenterLeavesDisabled() throws Exception {
    onEdt(() -> {
      SsHtmlPresenter presenter = new SsHtmlPresenter(() -> "testPresenterLeavesDisabled");
      JLabel label = new JLabel();
      label.putClientProperty("html.disable", Boolean.TRUE);
      label.setText(presenter.prepare(label, bad));
      assertShows(label, bad, false);
      label.setText(presenter.prepare(label, GOOD));
      assertShows(label, GOOD, false);
      assertThat(label.getClientProperty("html.disable")).isEqualTo(Boolean.TRUE);
    });
  }

  /** An SsHtml that fails gets the text shown literally. */
  @Test
  void testPresenterFailingSsHtml() throws Exception {
    class Failing implements SsHtml {
      boolean returnNull;

      @Override
      public String getName() {
        return "FAILING";
      }

      @Override
      public Display display(String text) {
        if (returnNull)
          return null;
        throw new IllegalStateException("test");
      }

      @Override
      public Report inspect(String text) {
        return null;
      }
    }

    onEdt(() -> {
      Failing failing = new Failing();
      SsHtmlPresenter presenter = new SsHtmlPresenter(() -> "testPresenterFailingSsHtml");
      presenter.setSsHtml(failing);
      JLabel label = new JLabel();
      // Not HTML, so it is not asked.
      label.setText(presenter.prepare(label, PLAIN));
      assertShows(label, PLAIN, false);
      label.setText(presenter.prepare(label, GOOD));
      assertShows(label, GOOD, false);
      failing.returnNull = true;
      Display display = presenter.display(bad);
      assertThat(display.renderHtml()).isFalse();
      assertThat(display.text()).isSameAs(bad);
      assertThat(display.notAllowed()).containsExactly(SsHtmlChecker.REFUSED_ERROR);
    });
  }

  /** Many different refused names; only checks that nothing goes wrong. */
  @Test
  void testPresenterManyReports() throws Exception {
    onEdt(() -> {
      SsHtmlPresenter presenter = new SsHtmlPresenter(() -> "testPresenterManyReports");
      JLabel label = new JLabel();
      for (int i = 0; i < 3 * SsHtmlPresenter.MAX_REPORTED; i++) {
        // An unknown element before any content is dropped by the parser.
        String text = "<html>a<x" + i + ">text";
        label.setText(presenter.prepare(label, text));
        assertShows(label, text, false);
        // The same again is not reported again.
        label.setText(presenter.prepare(label, text));
      }
    });
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // Components with a database.
  // Columns: 0 tbl_pk, 1 c_text, 2 c_int, 3 c_date
  //

  private static RowSet rowSet(String where) throws Exception {
    RowSet rs = H2.getRowSetCleanDB("""
        CREATE TABLE tbl
        (
            tbl_pk INTEGER PRIMARY KEY,
            c_text VARCHAR(200),
            c_int INT,
            c_date DATE
        );
        """);
    try (var statement = H2.getCon().prepareStatement("INSERT INTO tbl VALUES (?, ?, ?, ?)")) {
      String[] texts = {GOOD, bad, PLAIN};
      for (int i = 0; i < texts.length; i++) {
        statement.setInt(1, i + 1);
        statement.setString(2, texts[i]);
        statement.setInt(3, i + 1);
        statement.setString(4, "2000-01-1" + i);
        statement.executeUpdate();
      }
    }
    rs.setCommand("SELECT * FROM tbl" + where + " ORDER BY tbl_pk");
    return rs;
  }

  private static SsLabel ssLabel(int pk) throws Exception {
    SsLabel[] label = new SsLabel[1];
    onEdt(() -> label[0] = new SsLabel(RowsModel.create(rowSet(" WHERE tbl_pk = " + pk)),
                                       "c_text"));
    // The RowsModel does some of its work in events; let them finish.
    onEdt(() -> {});
    return label[0];
  }

  @Test
  void testSsLabel() throws Exception {
    SsLabel good = ssLabel(1);
    onEdt(() -> assertShows(good, GOOD, true));

    SsLabel refused = ssLabel(2);
    onEdt(() -> {
      assertShows(refused, bad, false);
      assertThat(refused.getSsHtml()).isSameAs(SsHtml.getDefault());
      // Text that the application sets is not checked.
      refused.setText(GOOD);
      assertThat(refused.getText()).isEqualTo(GOOD);
    });

    SsLabel plain = ssLabel(3);
    onEdt(() -> assertShows(plain, PLAIN, false));
  }

  @Test
  void testSsLabelOwnSsHtml() throws Exception {
    SsLabel[] label = new SsLabel[1];
    onEdt(() -> {
      label[0] = new SsLabel();
      label[0].setSsHtml(SsHtml.plain());
      assertThat(label[0].getSsHtml()).isSameAs(SsHtml.plain());
      RowsModel.create(rowSet(" WHERE tbl_pk = 1")).bind(label[0], "c_text");
    });
    onEdt(() -> assertShows(label[0], GOOD, false));
  }

  private static Component cell(SsTable table, int row, int column) {
    return table.prepareRenderer(table.getCellRenderer(row, column), row, column);
  }

  @Test
  void testSsTable() throws Exception {
    SsTable[] p_table = new SsTable[1];
    onEdt(() -> p_table[0] = new SsTable(RowsModel.create(rowSet(""))));
    onEdt(() -> {
      SsTable table = p_table[0];
      assertThat(table.getValueAt(1, 1)).isEqualTo(bad);
      assertThat(table.getSsHtml()).isSameAs(SsHtml.getDefault());

      // The renderer is one component; go down the column and back up.
      assertShows(cell(table, 0, 1), GOOD, true);
      assertShows(cell(table, 1, 1), bad, false);
      assertShows(cell(table, 2, 1), PLAIN, false);
      assertShows(cell(table, 1, 1), bad, false);
      assertShows(cell(table, 0, 1), GOOD, true);
      // Other columns are not affected by what is in this one.
      assertThat(((JLabel) cell(table, 1, 2)).getText()).isEqualTo("2");

      table.setSsHtml(SsHtml.plain());
      assertThat(table.getSsHtml()).isSameAs(SsHtml.plain());
      assertShows(cell(table, 0, 1), GOOD, false);
      assertShows(cell(table, 1, 1), bad, false);

      table.setSsHtml(SsHtml.checker(Rules.INLINE.withOnRefusal(OnRefusal.TEXT_ONLY)));
      assertShows(cell(table, 0, 1), GOOD, true);
      assertShows(cell(table, 1, 1), "bad", false);

      table.setSsHtml(null);
      assertShows(cell(table, 1, 1), bad, false);
    });
  }

  @Test
  void testSsTableComboAndDateRenderers() throws Exception {
    SsTable[] p_table = new SsTable[1];
    onEdt(() -> p_table[0] = new SsTable(RowsModel.create(rowSet(""))));
    onEdt(() -> {
      SsTable table = p_table[0];
      // The display values may come from a database too.
      table.setComboRenderer(2, new Object[] {GOOD, bad, PLAIN}, new Object[] {1, 2, 3});
      assertShows(cell(table, 0, 2), GOOD, true);
      assertShows(cell(table, 1, 2), bad, false);
      assertShows(cell(table, 2, 2), PLAIN, false);

      // The underlying value is never put in the label, only its display value.
      table.setComboRenderer(1, new Object[] {"one", "two", "three"},
                             new Object[] {GOOD, bad, PLAIN});
      assertShows(cell(table, 0, 1), "one", false);
      assertShows(cell(table, 1, 1), "two", false);

      // The date renderer gives a value that is not a date to its super.
      table.setDateRenderer(1);
      assertShows(cell(table, 0, 1), GOOD, true);
      assertShows(cell(table, 1, 1), bad, false);
      table.setDateRenderer(3);
      assertThat(((JLabel) cell(table, 0, 3)).getText()).isNotEmpty();
    });
  }

  //////////////////////////////////////////////////////////////////////////
  //
  // Combo boxes and the list
  //

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Component item(JComboBox combo, int index) {
    ListCellRenderer renderer = combo.getRenderer();
    return renderer.getListCellRendererComponent(new JList<>(), combo.getItemAt(index), index,
                                                 false, false);
  }

  private void checkCombo(SsComboBox1<Integer, String> combo) {
    combo.setAllowNull(false);
    combo.setDisplayValues(List.of(GOOD, bad, PLAIN), List.of(1, 2, 3));
    assertThat(combo.getItemCount()).isEqualTo(3);
    assertShows(item(combo, 0), GOOD, true);
    assertShows(item(combo, 1), bad, false);
    assertShows(item(combo, 2), PLAIN, false);
    assertShows(item(combo, 0), GOOD, true);

    combo.setSsHtml(SsHtml.plain());
    assertShows(item(combo, 0), GOOD, false);
    assertShows(item(combo, 1), bad, false);

    combo.setSsHtml(SsHtml.checker(Rules.INLINE.withOnRefusal(OnRefusal.TEXT_ONLY)));
    assertShows(item(combo, 0), GOOD, true);
    assertShows(item(combo, 1), "bad", false);
  }

  @Test
  void testSsComboBoxSwing() throws Exception {
    onEdt(() -> checkCombo(new SsComboBox1.Builder<Integer, String>() {}.build()));
  }

  /** GlazedLists installs its own renderer; it must not be the one in use. */
  @Test
  void testSsComboBoxGlazed() throws Exception {
    onEdt(() -> checkCombo(new SsComboBox1.Builder<Integer, String>() {}
        .modelType(SsComboBox2.ModelType.GLAZED).build()));
  }

  @Test
  void testSsList() throws Exception {
    onEdt(() -> {
      @SuppressWarnings("serial")
      SsList1<Object, String> list = new SsList1<>(JDBCType.INTEGER) {};
      list.setDisplayValues(List.of(GOOD, bad, PLAIN));
      ListCellRenderer<? super dev.visdb.seesaw.models.ListItem> renderer = list.getCellRenderer();
      assertThat(list.getModel().getSize()).isEqualTo(3);
      assertShows(renderer.getListCellRendererComponent(
          list, list.getModel().getElementAt(0), 0, false, false), GOOD, true);
      assertShows(renderer.getListCellRendererComponent(
          list, list.getModel().getElementAt(1), 1, false, false), bad, false);
      assertShows(renderer.getListCellRendererComponent(
          list, list.getModel().getElementAt(2), 2, false, false), PLAIN, false);

      list.setSsHtml(SsHtml.pass());
      assertThat(list.getSsHtml()).isSameAs(SsHtml.pass());
      list.setSsHtml(SsHtml.plain());
      assertShows(renderer.getListCellRendererComponent(
          list, list.getModel().getElementAt(0), 0, false, false), GOOD, false);
    });
  }
}
// vi: sw=2 ts=8
