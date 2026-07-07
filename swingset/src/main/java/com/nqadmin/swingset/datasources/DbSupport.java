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
 * copyright (C) 2024-2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/

package com.nqadmin.swingset.datasources;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.RowSet;

import com.nqadmin.swingset.core.DBComboBox2;
import com.nqadmin.swingset.datasources.RowSetOps.DbUpdate;
import com.nqadmin.swingset.utils.SSComponent;
import com.nqadmin.swingset.utils.SSSyncManager;


/**
 * Database specific operations and access strategy.
 * An implementation manages and uses a {@code sharedConnection},
 * <p>
 * This needs work, clean up. It evolved as someplace to put stuff that
 * shouldn't be in mainline code; and it provides a way to find out where those
 * places are. 
 */
public interface DbSupport {
	/**
	 * For the typical simple cases run the columnRead to set the value.
	 * Note that the columnUpdater typically ignores the comp argument, but
	 * there for special cases.
	 *
	 * @param comp
	 * @return
	 * @throws java.sql.SQLException
	 */
	static Object runDbReader(SSComponent comp) throws SQLException {
		return comp.getColumnReader()
				.apply(comp.getRowSet(), comp.getColumnIndex(), comp);
	}

	/**
	 * For the typical simple cases run the columnUpdater to set the value.
	 * Note that the columnUpdater typically ignores the comp argument, but
	 * there for special cases.
	 * 
	 * @param comp
	 * @param value
	 * @return actual item written to the database, throws if nothing written
	 * @throws SQLException
	 */
	static DbUpdate runDbUpdater(SSComponent comp, Object value) throws SQLException {
		return runDbUpdater(comp, value, comp.getColumnUpdater());
	}

	/**
	 * For the typical simple cases run the columnUpdater to set the value.
	 * Note that the columnUpdater typically ignores the comp argument, but
	 * there for special cases.
	 *
	 * @param comp
	 * @param value
	 * @param columnUpdater
	 * @return actual item written to the database, throws if nothing written
	 * @throws SQLException
	 */
	// TODO: any reason to make this public?
	private static DbUpdate runDbUpdater(SSComponent comp, Object value,
			DbUpdater<RowSet, Integer, SSComponent, Object> columnUpdater)
			throws SQLException {
		return columnUpdater.apply(comp.getRowSet(), comp.getColumnIndex(), comp, value);
	}

	/**
	 * Create a query that contains the row number of a non "order by" query.
	 * When used in conjunction with a {@link DBComboBox2} which is acting as
	 * a combobox navigator, the row number is used to avoid sequential searches
	 * table searches in {@link SSSyncManager}.
	 * For example, given {@snippet lang="java":
	 * sup.createRownumQuery("part_id, part_name",
	 *                       "rown",
	 *                       "part_data",
	 *                       "ORDER BY part_name");
	 * }
	 * The {@code H2} {@code DbSupport} uses the the {@code H2} builtin
	 * function {@code ROWNUM()}
	 * and returns the string {@snippet :
	 * SELECT part_id, part_name, ROWNUM() AS rown
	 * FROM part_data
	 * ORDER BY part_name;
	 * }
	 * <p>
	 * Note that Mariadb has ROWNUM() and Oracle has ROWNUM; they perform
	 * a similar function. Other databases use ROW_NUMBER() in more complex
	 * windowed queries, example fragment of a more complex query: {@snippet :
	 * ROW_NUMBER() OVER(ORDER BY (SELECT NULL))
	 * }
	 * refer to your database documentation, or AI assistant, when implementing
	 * this method.
	 * 
	 * @param selectColumns could be "*"
	 * @param rownumberColumn
	 * @param tableName
	 * @param trailingClause for example "ORDER BY someCol"
	 * @return query or null
	 */
	// Mariadb: ROWNUM(), oracle has ROWNUM.
	default String createRownumQuery(String selectColumns, String rownumberColumn,
			String tableName, String trailingClause) {
		return null;
	}

	/**
	 * Run the function with a connection to the database associated
	 * with the specified {@code RowSet}, return the result.
	 * @param <R>
	 * @param rs if null, just use the {@code sharedConnection}
	 * @param func
	 * @return
	 * @throws java.sql.SQLException
	 */
	<R> R runWithConnection(RowSet rs, FunctionSQL<Connection, R> func) throws SQLException;

	/**
	 * Return a connection for quick use that
	 * connects to the database where the row set comes from.
	 * <em>Do not close</em> the returned connection after use.
	 * Using this to create a ResultSet/JdbcRowSet that stays open
	 * is <em>not quick</em>.
	 * 
	 * @param rs row set from target database or null for a default connection.
	 * @return connection or null if none found
	 * @throws java.sql.SQLException
	 */
	Connection getSharedConnection(RowSet rs) throws SQLException;

	/**
	 * Return a connection that connects to the database where the row set comes from;
	 * <em>close when finished</em>.
	 * Tries url, dataSource.
	 * @param rs row set from target database
	 * @return connection
	 * @throws java.sql.SQLException
	 */
	Connection getConnection(RowSet rs) throws SQLException;

	/**
	 * Like Runnable, but throws.
	 */
	interface RunnableSQL {
		/**
		 * @throws SQLException
		 */
		void run() throws SQLException;
	}

	/**
	 *
	 * @param <T>
	 */
	interface ConsumerSQL<T> {

		/**
		 *
		 * @param t
		 * @throws SQLException
		 */
		public void accept(T t) throws SQLException;
	}

	/**
	 * Like Function, but only has apply method that throws SQLException.
	 * @param <T>
	 * @param <R>
	 */
	interface FunctionSQL<T,R> {

		/**
		 * Run the function.
		 * @param t
		 * @return
		 * @throws SQLException
		 */
		public R apply(T t) throws SQLException;
	}

	/**
	 * Like BiFunction, but throws SQLException.
	 * 
	 * @param <T>
	 * @param <U>
	 * @param <R>
	 */
	interface BiFuncSQL<T,U,R> {

		/**
		 * Run the function
		 * @param t
		 * @param u
		 * @return
		 * @throws SQLException
		 */
		public R apply(T t, U u) throws SQLException;
	}

	/**
	 * Three arg function taking rowSet,colIdx,comp that throws SQLException
	 * and returns an Object.
	 *
	 * @param <T>
	 * @param <U>
	 * @param <V>
	 */

	interface DbReader<T,U,V>  {

		/**
		 * Run the function
		 *
		 * @param t
		 * @param u
		 * @param v
		 * @return value read from database
		 * @throws SQLException
		 */

		public Object apply(T t, U u, V v) throws SQLException;
	}

	/**
	 * Four arg function taking rowSet,colIdx,comp,value that throws SQLException
	 * and returns an Object.
	 *
	 * @param <T>
	 * @param <U>
	 * @param <V>
	 * @param <W>
	 */
	interface DbUpdater<T,U,V,W>  {

		/**
		 * Run the function
		 *
		 * @param t
		 * @param u
		 * @param v
		 * @param w
		 * @return The actual item written to the database
		 * @throws SQLException
		 */

		public DbUpdate apply(T t, U u, V v, W w) throws SQLException;
	}
}
