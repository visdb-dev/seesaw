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
 * 3. Neither the productName of the copyright holder nor the names of its contributors
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
package com.nqadmin.swingset.datasources.products;

import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;

import org.openide.util.lookup.Lookups;

import com.nqadmin.swingset.datasources.DbSupport;
import com.nqadmin.swingset.utils.CentralLookup;
import com.nqadmin.swingset.utils.JStuff;
import com.nqadmin.swingset.utils.SSUtils;

import static java.lang.System.Logger.Level.ERROR;

/**
 * Generate DbSupport instances;
 * Searches for classes that can create them. 
 * See {@link DbSupportCreator}.
 */
// Would be nice to have this run automatically. Might need a "using" method
// somewhere that initializes database related stuff.
public class DbSupportFactory
{
	private DbSupportFactory() { }
	private static final System.Logger logger = JStuff.getLogger();

	/**
	 * Create a DbSupport that works with the specified connection according
	 * to its metadata; put it into the CentralLookup.
	 * It replaces any
	 * {@code DbSupport} that might already be in there.
	 * See {@link SSUtils#dbSupport()} for best access method.
	 * The connection is used as the shared connection and to
	 * examine DatabaseMetaData.
	 * @param sharedConnection
	 * @return an DbSupport instance that is put into CentralLookup or null.
	 */
	public static DbSupport addDbSupportToLookup(Connection sharedConnection) {
		DbSupport dbSupport = createDbSupport(sharedConnection);
		if (dbSupport != null)
			CentralLookup.getDefault().replace(DbSupport.class, dbSupport);
		return dbSupport;
	}

	/**
	 * Create a DbSupport that works with the specified connection according
	 * to its metadata.
	 * The connection is used as the shared connection and to
	 * examine DatabaseMetaData.
	 * @param sharedConnection
	 * @return an DbSupport instance that is put into CentralLookup or null.
	 */
	public static DbSupport createDbSupport(Connection sharedConnection) {
		DatabaseMetaData dbMeta;
		String productName;
		try {
			dbMeta = sharedConnection.getMetaData();
			productName = dbMeta.getDatabaseProductName();
		} catch (SQLException ex) {
			logger.log(ERROR, (String) null, ex);
			return null;
		}
		logger.log(Level.INFO, () -> "Creating DbSupport for " + productName);
		Collection<? extends DbSupportCreator> creators = Lookups.forPath(
				"SS/DbSupport/" + productName).lookupAll(DbSupportCreator.class);
		if (creators.isEmpty()) 
			logger.log(ERROR, () -> "No DbSupportCreator for " + productName);

		DbSupport dbSupport = null;
		for (DbSupportCreator creator : creators) {
			if((dbSupport = creator.create(sharedConnection, dbMeta)) != null)
				break;
		}
		if (dbSupport == null) {
			logger.log(ERROR, () -> "Failed to create DbSupport for " + productName);
			return null;
		}
		return dbSupport;
	}
}
