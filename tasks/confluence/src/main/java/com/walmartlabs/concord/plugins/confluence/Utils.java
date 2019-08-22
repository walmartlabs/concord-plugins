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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.walmartlabs.concord.sdk.Context;

import java.io.FileReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    @SuppressWarnings("unchecked")
    public static String getPageId(Context ctx, String url, String pageTitle, String spaceKey) {
        Map<String, Object> results;

        try {
            results = new ConfluenceClient(ctx)
                    .url(url + "content/")
                    .getWithQueryParams(Constants.CONFLUENCE_ENTITY_TITLE, pageTitle, Constants.CONFLUENCE_ENTITY_SPACEKEY, spaceKey);

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while retrieving page id...", e);
        }
        List<Map> resultsList = (List<Map>) results.get("results");
        if (!resultsList.isEmpty()) {
            Map resultsMap = resultsList.get(0);
            return (String) resultsMap.get("id");
        } else {
            throw new RuntimeException("Page with title '" + pageTitle + "' does not exist in space '" + spaceKey + "'");
        }
    }

    @SuppressWarnings("unchecked")
    public static String getPageCurrentVersion(Context ctx, String url, int pageId) {
        Map<String, Object> results;

        try {
            results = new ConfluenceClient(ctx)
                    .url(url + "content/" + pageId + "/history")
                    .get();

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while retrieving page version...", e);
        }

        Map<String, Object> resultsList = (Map<String, Object>) results.get("lastUpdated");
        if (!resultsList.isEmpty()) {
            return resultsList.get("number").toString();
        } else {
            throw new RuntimeException("Could not find any 'lastUpdated' details for page# '" + pageId + "'");
        }
    }

    @SuppressWarnings("unchecked")
    public static String getPageContent(Context ctx, String url, int pageId) {
        Map<String, Object> results;

        try {
            results = new ConfluenceClient(ctx)
                    .url(url + "content/" + pageId + "?expand=body.storage")
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while retrieving page content...", e);
        }

        Map<String, Object> bodyList = (Map<String, Object>) results.get("body");
        if (!bodyList.isEmpty()) {
            Map<String, Object> storageList = (Map<String, Object>) bodyList.get("storage");
            if (!storageList.isEmpty()) {
                return storageList.get("value").toString();
            } else {
                throw new RuntimeException("Could not find any associated content/body for page# '" + pageId + "'");
            }
        } else {
            throw new RuntimeException("Could not find any associated content/body for page# '" + pageId + "'");
        }
    }

    private static Object getScope(Context ctx, Map<String, Object> params) {
        Map<String, Object> ctxParams = ctx != null ? ctx.toMap() : Collections.emptyMap();
        Map<String, Object> result = new HashMap<>();
        result.putAll(ctxParams);
        if (params != null && !params.isEmpty()) {
            result.putAll(params);
        }
        return result;
    }

    public static Map<String, Object> applyTemplate(Context ctx, String template, Map<String, Object> templateParams) throws Exception {
        StringWriter out = new StringWriter();
        Map<String, Object> m = new HashMap<>();
        if (template == null) {
            m = Collections.emptyMap();
            return m;
        }
        Path baseDir = Paths.get((String) ctx.getVariable(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY));
        try (FileReader in = new FileReader(baseDir.resolve(template).toFile())) {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(in, template);

            Object scope = getScope(ctx, templateParams);
            mustache.execute(out, scope);

        }
        m.put("content", out.toString());
        return m;
    }
}
