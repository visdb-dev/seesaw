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
package dev.visdb.seesaw.datasources;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import dev.visdb.seesaw.utils.SsUtils;

/**
 * Metadata cache. Only static and Driver info, capabilities, and limits should
 * be referenced. Information that is subject to change as the database is
 * running should not be referenced.
 * <p>
 * <b>NOTE:</b> There's only one database, found in SsUtils.dbSupport.getSharedConnection.
 *
 */
public class DbMetaDataCache {
  private DbMetaDataCache() {}

  //
  // The databasemetadata is cached, so only the first call connects to
  // the database. Note: some information is not necessarily safe to cache;
  // google search: "with jdbc is it safe to cache databasemetadata"
  //
  // Make this a selective cache. In particular Static/Driver info,
  // capabilities/Limits. But not tables & Column details.
  // Then no worry about needing to flush in some cases.
  //
  // TODO: this class could provide methods that are "safe", with exactly
  //       the same names as used in DatabaseMetaData, and return this
  //       rather than the metadata itself. Then there would be no chance
  //       of referencing non-cachable data.
  //

  private static DatabaseMetaData metaData;

  // TODO: flush/refresh/enable, other maintenance methods?
  // All accesses to metadata could go through here by adding
  // a "refresh" flag. Is that useful or confusing?

  /**
   * @return the MetaData cache for the default database from getSharedConnection().
   * @throws java.sql.SQLException
   */
  public static DatabaseMetaData get() throws SQLException {
    if (metaData == null) {
      Connection shconn = SsUtils.dbSupport().getSharedConnection();
      if (shconn == null)
        return null;
      metaData = shconn.getMetaData();
    }
    return metaData;
  }
}
