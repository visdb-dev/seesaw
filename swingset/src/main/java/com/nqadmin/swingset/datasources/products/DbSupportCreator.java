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
package com.nqadmin.swingset.datasources.products;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import com.nqadmin.swingset.datasources.DbSupport;

/**
 * Should be at least one of these for a supported database. It creates a
 * {@link DbSupport} that works with the database specified by the
 * DatabaseMetaData.
 * These are placed in {@code META-INF} using an annotation like
 * {@snippet lang="java":
 * @ServiceProvider(path="SS/DbSupport/H2", service=DbSupportCreator.class)
 * }
 * Note in this example that {@code dbMeta.getDatabaseProductName().equals("H2")}.
 * <p>
 * Any {@code DbSupportCreator}s found are tried in order.
 * The first creator that matches the metadata returns non null, the search
 * is terminated and that value is returned.
 * Normally {@code position} is not used with the annotation, but if there are
 * multiple creators that match, it's use might be needed.
 * <a href="https://bits.netbeans.org/dev/javadoc/org-openide-util-lookup/org/openide/util/lookup/ServiceProvider.html">
 * See @ServiceProvider javadoc</a>.
 */
public interface DbSupportCreator {
  /**
   * Create a DbSupport for the connection. The DatabaseMetaData belongs
   * to the connection; use to refer to dbMeta productVersion or ...
   * If this creator can't construct a DbSupport for the meta data return null;
   *
   * @param sharedConnection
   * @param dbMeta
   * @return may be null
   */
  DbSupport create(Connection sharedConnection, DatabaseMetaData dbMeta);
}
