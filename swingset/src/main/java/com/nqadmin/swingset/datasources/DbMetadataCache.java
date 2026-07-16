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

import com.nqadmin.swingset.utils.SSUtils;

/**
 * Metadata cache.
 */
public class DbMetadataCache
{
	//
	// Make this a selective cache. In particular Static/Driver info,
	// capabilities/Limits. But not tables & Column details.
	// Then no worry about needing to flush in some cases.
	//
	// Create: SSDbMetaDataCache.java

	//private Map<String, DatabaseMetaData> metaDatas;
	//Set<Connection> connections
	//       = Collections.newSetFromMap(new WeakHashMap<Connection, Boolean>());
	//private Set<Connection> connections; // known connections, have metadata, weakset

	private DatabaseMetaData metaData;
	private boolean metaDataCacheEnabled;

	/**
	 * Fetch the databasemetadata for the connection returned by getSharedConnection.
	 * The databasemetadata is cached, so only the first call connects to
	 * the database. Note: some information is not necessarily safe to cache;
	 * google search: "with jdbc is it safe to cache databasemetadata"
	 * 
	 * @param conn get metadata for this connection
	 * @param refresh true indicates that the cached metadata should be refreshed from the database
	 * @return metadata or null if can't get metadata, e.g. no connection.
	 * @throws java.sql.SQLException
	 */
	// TODO: handle any/multiple connections, currently only caching for shared conn
	// NOTE: If schema changes table/column info changes
	public DatabaseMetaData getMetaData(Connection conn, boolean refresh) throws SQLException {
		Connection shconn = getSharedConnection();
		if (conn != shconn || !metaDataCacheEnabled)
			return conn.getMetaData();
		if (refresh || metaData == null) {
			metaData = null; // in case there's an error/exception
			if (conn != null)
				metaData = conn.getMetaData();
		}
		return metaData;
	}

	/**
	 * Flush any cached metadata for specified connection.
	 * This may only flush table and column details, i.e. things that depend
	 * on the schema.
	 * 
	 * @param conn flush metadata for this connection
	 */
	public void flushMetaData(Connection conn)
	{
		Connection shconn = null;
		try {
			shconn = getSharedConnection();
		} catch (SQLException ex) { } // Can't happen with null argument.
		if (conn != shconn)
			return;
		metaData = null;
	}

	// TODO: cache this
	private Connection getSharedConnection() throws SQLException {
		return SSUtils.dbSupport().getSharedConnection();
	}
	
}
