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
package com.nqadmin.swingset.datasources;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.nqadmin.swingset.utils.CentralLookup;
import com.nqadmin.swingset.utils.SSUtils;

import static java.lang.System.Logger.Level.ERROR;

/**
 * Generate DbSupport instances.
 * <p>
 * TODO: handle registration based on ProductName. The handler is responsible
 *       for sorting out versions.
 */
public class DbSupportFactory
{
	private DbSupportFactory() { }
	private static final System.Logger logger = SSUtils.getLogger();
	// Something simple for now.
	private static final HashMap<String, Function<Connection, DbSupport>> creators
			= new HashMap<>(Map.of("H2", (Connection conn) -> new H2DbSupport(conn)));

	/**
	 * Create a DbSupport that works with the specified connection according
	 * to its metadata; put it into the CentralLookup.
	 * The connection is used as the shared connection and to fetch DatabaseMetaData.
	 * @param conn
	 * @return an DbSupport instance that is put into CentralLookup or null.
	 */
	public static DbSupport setupLookup(Connection conn) {
		CentralLookup lkup = CentralLookup.getDefault();

		DbSupport dbSupport = null;
		try {
			DatabaseMetaData dbMeta = conn.getMetaData();
			String name = dbMeta.getDatabaseProductName();
			Function<Connection, DbSupport> creator = creators.get(name);
			if (creator != null)
				dbSupport = creator.apply(conn);
		} catch (SQLException ex) {
			logger.log(ERROR, (String) null, ex);
		}
		if (dbSupport != null)
			lkup.replace(DbSupport.class, dbSupport);

		return dbSupport;
	}

}
