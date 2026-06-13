/* *****************************************************************************
 * Copyright (C) 2024, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
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
 * ****************************************************************************/
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2024-2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package com.nqadmin.swingset.datasources;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.RowSet;

import com.nqadmin.swingset.utils.SSUtils;

/**
 * Simple assists for working with database. There is typically a shared
 * connection, the {@code defaultConnection}.
 * TODO: clarify spec how many connections are supported.
 * TODO: instead of RowSet should take RSC since different columns
 *       could be from different database.
 */
// TODO: clarify semantics of how many connections are supported
public class DefaultSSDBSupport implements SSDBSupport
{
	private final Connection sharedConnection;

	/**
	 * Save the specified connection as the sharedConnection.
	 * It can be retrieved with a {@code null} param to
	 * {@link #getSharedConnection(javax.sql.RowSet)}.
	 * 
	 * @param sharedConnection 
	 */
	public DefaultSSDBSupport(Connection sharedConnection)
	{
		if (!SSUtils.isJunit())
			Objects.requireNonNull(sharedConnection);
		this.sharedConnection = sharedConnection;
	}

	/**
	 * {@inheritDoc }
	 * 
	 * @param <R>
	 * @param rs
	 * @param func
	 * @return 
	 */
	@Override
	public <R> R runWithConnection(RowSet rs, FuncSQL<Connection, R> func)
			throws SQLException
	{
		Connection conn01 = getSharedConnection(rs);
		if (conn01 != null)
			return func.apply(conn01);

	    Connection conn = getConnection(rs);
	    if (conn == null)
	        throw new SQLException("No database connection available for RowSet. "
	                + "dataSourceName=" + rs.getDataSourceName()
	                + ", url=" + rs.getUrl());

	    try (conn) {
	        return func.apply(conn);
	    }
	}

	/**
	 * {@inheritDoc }
	 * Typically the default connection 
	 */
	// TODO: Probably should handle more than one connection, like multiple databases
	// TODO: Is there a better way to tell if connections is good for the RowSet?
	// TODO: Seems to require that all column from same catalog; OK?
	@Override
	public Connection getSharedConnection(RowSet rs) throws SQLException
	{
		if (sharedConnection == null)
			return null;
		if (sharedConnection.isClosed())
			throw new IllegalStateException("Shared connection isClosed");
		if (rs == null)
			return sharedConnection;
		if (Objects.equals(sharedConnection.getCatalog(),
				rs.getMetaData().getCatalogName(1)))
			return sharedConnection;
		return null;
	}

	/**
	 * {@inheritDoc }
	 * @param rs
	 * @return
	 * @throws SQLException 
	 */
	@Override
	public Connection getConnection(RowSet rs) throws SQLException
	{
		String dsName = rs.getDataSourceName();
		if (dsName != null) {
			try {
				if (ctx == null)
					ctx = new InitialContext();
				// TODO: keep a local map of dsName to DataSource ???
				DataSource ds = (DataSource)ctx.lookup(dsName);
				return ds.getConnection();
			} catch (NamingException ex) {
			}
		}

		String url = rs.getUrl();
		if (url != null) {
			return DriverManager.getConnection(url);
		}
		
		return null;
	}
	private InitialContext ctx;
}
