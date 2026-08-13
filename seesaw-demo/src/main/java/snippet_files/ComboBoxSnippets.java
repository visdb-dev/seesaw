package snippet_files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.sql.RowSet;
import javax.sql.rowset.JdbcRowSet;
import javax.swing.JFrame;

import com.google.common.reflect.TypeToken;

import dev.visdb.seesaw.core.ComboBox1;
import dev.visdb.seesaw.core.DBComboBox2;
import dev.visdb.seesaw.core.Item1;
import dev.visdb.seesaw.navigate.RowsModel;

/**
 * javadoc examples.
 */
@SuppressWarnings("serial")
public class ComboBoxSnippets extends JFrame {
  Connection conn;
  // Param type capture is important so that values read from the database
  // are correctly converted to the right type.

  // You may have
  // Simple example where you lock in the types, don't add anything
  // else, MyDbComboBox.Builder just works.

  // @start region=MyDbComboBox
  static class MyDbComboBox extends DBComboBox2<Integer, String, Byte> {
    public static class Builder
        extends DBComboBox2.AbstractBuilder<Integer, String, Byte, Builder> {
      @Override
      protected Builder self() {
        return this;
      }
      @Override
      public MyDbComboBox build() {
        return new MyDbComboBox(this);
      }
    }
    private MyDbComboBox(Builder builder) { super(builder); }
  }

  MyDbComboBox dbCombo = new MyDbComboBox
                             .Builder() // NOTE: "{ }" not needed
                             .primaryKeyColumnName("keyCol")
                             .displayColumnName("dispCol")
                             .build();
  // System.out.println(""+dbCombo.getD2Type()); // output: class java.lang.Byte
  // @end region=MyDbComboBox

  // @start region=ExtendableDbComboBox
  static class DbComboBox2Extra<D2, D3> extends DBComboBox2<Integer, String, D2> {
    private final D3 d3Value; // Note: not part of combo list item.
    private final TypeToken<D3> d3TypeToken;

    public abstract static class AbstractBuilder<D2, D3, T extends AbstractBuilder<D2, D3, T>>
        extends DBComboBox2.AbstractBuilder<Integer, String, D2, T> {
      private D3 d3Value;
      // captures D3 for whatever runtime class extends this Abstractbuilder
      private final TypeToken<D3> d3TypeToken = new TypeToken<D3>(getClass()) {};

      public AbstractBuilder() {
        // check if TypeToken is generic; verifyTypeClass throws useful msg
        verifyTypeClass(d3TypeToken, getClass());
      }

      public T d3Data(D3 val) {
        d3Value = val;
        return self();
      }
    }

    // Regular Builder for direct instantiation
    public static class Builder<D2, D3> extends AbstractBuilder<D2, D3, Builder<D2, D3>> {
      // self type idiom
      @Override
      protected Builder<D2, D3> self() {
        return this;
      }

      @Override
      public DbComboBox2Extra<D2, D3> build() {
        return new DbComboBox2Extra<>(this);
      }
    }

    protected DbComboBox2Extra(AbstractBuilder<D2, D3, ?> builder) {
      super(builder);
      d3Value = builder.d3Value;
      d3TypeToken = builder.d3TypeToken;
    }

    public D3 getD3() { return d3Value; }
    public TypeToken<D3> getD3TypeToken() { return d3TypeToken; }
  }
  // @end region=ExtendableDbComboBox
  void useExCombo() {
    // @start region=ExtendableDbComboBoxExample
    DbComboBox2Extra<Double, List<Double>> cbExtra
        = new DbComboBox2Extra
              .Builder<Double, List<Double>>() {} // <- "{ }" NEEDED
              .primaryKeyColumnName("aaa")
              .displayColumnName("bbb")
              .d3Data(new ArrayList<>())
              .build();
    cbExtra.getD3().addAll(List.of(7D, 6D, 5D));
    System.out.println("" + cbExtra.getKeyType());
    System.out.println("" + cbExtra.getDisplayValueType());
    System.out.println("" + cbExtra.getD2Type());
    System.out.println("" + cbExtra.getD3TypeToken());
    System.out.println("" + cbExtra.getD3());
    // @end region=ExtendableDbComboBoxExample
  }

  RowsModel rowsModel;
  DBComboBox2<Long, String, Long> combo;

  // @start region=init
  /**
   * Create NavigateActions for shipment_data and ComboBox to select part_id.
   * The ComboBox displays part_name and provides part_id.
   * Add the comboBox to this JFrame.
   * @param connection used by SSDBComboBox
   * @param rowSet to connect to shipment_data
   */
  void init(Connection connection, JdbcRowSet rowSet) {
    try {
      // Table to examine and traverse.
      rowSet.setCommand("SELECT * FROM shipment_data;");
      rowSet.execute();

      // 2nd arg is DbOps, if null use default.
      rowsModel = RowsModel.create(rowSet, null);

      // Query for the combobox to map part_id to part_name.
      String query = "SELECT * FROM part_data;";

      // Create an instance of the DBComboBox2 with the connection object,
      // query, and column names.
      combo = new DBComboBox2
                  .Builder<Long, String, Long>() {} // <- "{ }" NEEDED
                  .connection(connection)
                  .query(query)
                  .primaryKeyColumnName("part_id")
                  .displayColumnName("part_name")
                  .build();

      // Execute the query.
      combo.execute();

      // Specifies the column bound to the combo.
      rowsModel.bind(combo, "part_id");

    } catch (Exception ex) {
      // Exception handler here...
    }

    // Add the ssdbcombobox to the JFrame.
    getContentPane().add(combo);
  }
  // @end region=init

  @SuppressWarnings("unused")
  void autoGen() {
    // @start region=auto_gen
    ComboBox1<Long, String> combobox = new ComboBox1<>() {};
    List<String> options = List.of("111", "2222", "33333");
    combobox.setDisplayValues(options);
    // @end region=auto_gen
  }

  RowSet rowSet;

  @SuppressWarnings("unused")
  void customKey() {
    // @start region=custom_key
    ComboBox1<Integer, String> combobox = new ComboBox1<>() {};
    List<String> options = List.of("111", "2222", "33333");
    // The keys used in "my_column".
    List<Integer> keys = List.of(1, 5, 7);
    combobox.setDisplayValues(options, keys);

    // Next line is assuming rowsModel has been initialized
    // and "my_column" is a column in its rowSet.
    rowsModel.bind(combobox, "my_column");
    // @end region=custom_key
  }

  /** x */
  @SuppressWarnings("serial")
  // @start region=chosen_item
  public class ComboBoxIntString extends ComboBox1<Integer, String> {
    public static class Item extends Item1<Integer, String> {
      public Item(Integer getKey, String getDisplayValue) { super(getKey, getDisplayValue); }
    }
    @Override
    public Item getChosenItem() {
      return new Item(getChosenKey(), getChosenDisplayValue());
    }
  }
  // @end region=chosen_item
}
