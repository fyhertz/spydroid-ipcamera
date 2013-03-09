/*
 * $HeadURL: http://svn.apache.org/repos/asf/httpcomponents/httpcore/trunk/module-main/src/main/java/org/apache/http/protocol/HttpRequestHandlerRegistry.java $
 * $Revision: 630662 $
 * $Date: 2008-02-24 11:40:51 -0800 (Sun, 24 Feb 2008) $
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.http;

import java.util.Map;

import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.UriPatternMatcher;

/**
 * A slightly modified version of org.apache.http.protocol.HttpRequestHandlerRegistry 
 * that allows the registry to be modified while some threads may be using it.
 * Recent versions of this file are thread-safe, but Android seems to be using an old version. 
 */
public class ModifiedHttpRequestHandlerRegistry extends HttpRequestHandlerRegistry {

    private final UriPatternMatcher matcher;

    public ModifiedHttpRequestHandlerRegistry() {
        matcher = new UriPatternMatcher();
    }

    public synchronized void register(final String pattern, final HttpRequestHandler handler) {
        matcher.register(pattern, handler);
    }

    public synchronized void unregister(final String pattern) {
        matcher.unregister(pattern);
    }

    public synchronized void setHandlers(final Map map) {
        matcher.setHandlers(map);
    }

    public synchronized HttpRequestHandler lookup(final String requestURI) {
    	// This is the only function that will often be called by threads of the HTTP server
    	// and it seems like a rather small crtical section to me, so it should not slow things down much
        return (HttpRequestHandler) matcher.lookup(requestURI);
    }
	
}
