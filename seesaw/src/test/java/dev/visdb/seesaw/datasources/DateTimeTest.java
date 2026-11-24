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
package dev.visdb.seesaw.datasources;

import java.sql.JDBCType;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DateTimeTest {

  /** @return the first parse that works, null if none */
  private static TemporalAccessor parse(JDBCType jdbcType, String text) {
    for (DateTimeFormatter formatter : DateTime.getDateTimeParsers(jdbcType)) {
      try {
        return jdbcType == JDBCType.DATE
                   ? LocalDate.parse(text, formatter)
                   : formatter.parseBest(text, LocalDateTime::from, LocalDate::from);
      } catch (DateTimeParseException ex) {
        // try the next one
      }
    }
    return null;
  }

  @Test
  void testDateParsers() {
    LocalDate expect = LocalDate.of(2026, 1, 5);
    assertThat(parse(JDBCType.DATE, "1/5/2026")).isEqualTo(expect);
    assertThat(parse(JDBCType.DATE, "01/05/2026")).isEqualTo(expect);
    assertThat(parse(JDBCType.DATE, "2026-01-05")).isEqualTo(expect);
    assertThat(parse(JDBCType.DATE, "2026-1-5")).isEqualTo(expect);
    assertThat(parse(JDBCType.DATE, "20260105")).isEqualTo(expect);
    assertThat(parse(JDBCType.DATE, "2/30/2026")).isNull();
    assertThat(parse(JDBCType.DATE, "2026-2-30")).isNull();
  }

  @Test
  void testTimestampToStringParses() {
    // Timestamp.toString() has a fraction of 1 to 9 digits, ".0" if none.
    for (String s : new String[] {"2026-11-23 10:15:30", "2026-11-23 10:15:30.5",
                                  "2026-11-23 10:15:30.123",
                                  "2026-11-23 10:15:30.123456789"}) {
      Timestamp ts = Timestamp.valueOf(s);
      assertThat(parse(JDBCType.TIMESTAMP, ts.toString()))
          .as(ts.toString())
          .isEqualTo(ts.toLocalDateTime());
    }
    assertThat(parse(JDBCType.TIMESTAMP, "11/23/2026 10:15:30.0"))
        .isEqualTo(LocalDateTime.of(2026, 11, 23, 10, 15, 30));
    assertThat(parse(JDBCType.TIMESTAMP, "2026-11-23 10:15:30.")).isNull();
    // A TIMESTAMP has no offset.
    assertThat(parse(JDBCType.TIMESTAMP, "2026-11-23 10:15:30.0 -08:00")).isNull();
    assertThat(parse(JDBCType.TIMESTAMP, "11/23/2026 10:15:30.000 +00:00")).isNull();
  }
}
