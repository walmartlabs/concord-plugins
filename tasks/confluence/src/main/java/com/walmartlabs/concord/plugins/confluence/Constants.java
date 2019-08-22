package com.walmartlabs.concord.plugins.confluence;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

public class Constants {

    public static final long CONNECTION_TIMEOUT = 30L;
    public static final long READ_TIMEOUT = 30L;
    public static final long WRITE_TIMEOUT = 30L;

    public static final String CONFLUENCE_ENTITY_TITLE = "title";
    public static final String CONFLUENCE_ENTITY_SPACEKEY = "spaceKey";
    public static final String CONFLUENCE_ENTITY_TYPE_PAGE = "page";
    public static final String CONFLUENCE_ENTITY_TYPE_FILE = "file";
    public static final String CONFLUENCE_ENTITY_TYPE_COMMENT = "comment";
    public static final String CONFLUENCE_ENTITY_REPRESENTATION = "storage";
    public static final String CONFLUENCE_ENTITY_TOKEN_KEY = "X-Atlassian-Token";
    public static final String CONFLUENCE_ENTITY_TOKEN_VALUE = "no-check";
    public static final String CLIENT_MEDIATYPE_JSON = "application/json; charset=utf-8";
    public static final String CLIENT_HEADER_AUTH = "Authorization";
    public static final String CLIENT_HEADER_ACCEPT = "Accept";

    private Constants() {
    }
}
