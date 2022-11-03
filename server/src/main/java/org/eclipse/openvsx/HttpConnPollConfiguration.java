/********************************************************************************
 * Copyright (c) 2022 Gitpod and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HttpConnPollConfiguration {

    private final PoolingHttpClientConnectionManager connectionManager;
    private final Integer connectionRequestTimeout;
    private final Integer connectTimeout;
    private final Integer socketTimeout;

    public HttpConnPollConfiguration(PoolingHttpClientConnectionManager connectionManager, Integer connectionRequestTimeout,
            Integer connectTimeout, Integer socketTimeout) {
        this.connectionManager = connectionManager;
        this.connectionRequestTimeout = connectionRequestTimeout;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
    }

    public PoolingHttpClientConnectionManager getConnectionManager() {
        return connectionManager;
    }
    /**
     *  the time to wait for a connection from the connection manager/pool
     */
    public Integer getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }
    /**
     * the time to establish the connection with the remote host
     */
    public Integer getConnectTimeout() {
        return connectTimeout;
    }
    /**
     * the time waiting for data – after establishing the connection; maximum time of inactivity between two data packets
     */
    public Integer getSocketTimeout() {
        return socketTimeout;
    }

}
