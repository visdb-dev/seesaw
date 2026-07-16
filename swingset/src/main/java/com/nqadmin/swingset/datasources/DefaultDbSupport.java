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
public class DefaultDbSupport implements DbSupport
{
	private final Connection sharedConnection;

	/**
	 * Save the specified connection as the sharedConnection.
	 * 
	 * @param sharedConnection 
	 */
	public DefaultDbSupport(Connection sharedConnection)
	{
		if (!SSUtils.isJunit())
			Objects.requireNonNull(sharedConnection);
		this.sharedConnection = sharedConnection;
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public Connection getSharedConnection() throws SQLException
	{
		if (sharedConnection.isClosed())
			throw new IllegalStateException("Shared connection isClosed");
		return sharedConnection;
	}

	private InitialContext ctx;
	/**
	 * {@inheritDoc }
	 * @param rs
	 * @return
	 * @throws SQLException 
	 */
	@Override
	public Connection getConnection(RowSet rs) throws SQLException
	{
		Objects.requireNonNull(rs);
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
}
