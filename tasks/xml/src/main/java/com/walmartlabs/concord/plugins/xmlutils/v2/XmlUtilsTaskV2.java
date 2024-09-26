package com.walmartlabs.concord.plugins.xmlutils.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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

import com.walmartlabs.concord.plugins.xmlutils.XmlUtilsTaskCommon;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

@Named("xmlUtils")
@SuppressWarnings("unused")
public class XmlUtilsTaskV2 implements Task {

    private final XmlUtilsTaskCommon delegate;

    @Inject
    public XmlUtilsTaskV2(Context context) {
        this.delegate = new XmlUtilsTaskCommon(context.workingDirectory());
    }

    /**
     * Evaluates the expression and returns a single {@link String} value.
     */
    public String xpathString(String file, String expression) throws Exception {
        return delegate.xpathString(file, expression);
    }

    /**
     * @deprecated Use {@link #xpathString(String, String)} instead
     */
    @Deprecated(since = "1.42.0")
    public String xpathString(String workDir, String file, String expression) throws Exception {
        return xpathString(file, expression);
    }

    /**
     * Evaluates the expression and returns a list of {@link String} values.
     */
    public List<String> xpathListOfStrings(String file, String expression) throws Exception {
        return delegate.xpathListOfStrings(file, expression);
    }

    /**
     * @deprecated Use {@link #xpathListOfStrings(String, String)} instead
     */
    @Deprecated(since = "1.42.0")
    public List<String> xpathListOfStrings(String workDir, String file, String expression) throws Exception {
        return xpathListOfStrings(file, expression);
    }

    /**
     * Uses XPath to return {@code groupId + artifactId + version} attributes from a Maven pom.xml file.
     * Knows how to handle the {@code <parent>} tag, i.e. parent GAV values are merged with the pom's own GAV.
     */
    public Map<String, String> mavenGav(String file) throws Exception {
        return delegate.mavenGav(file);
    }

    /**
     * @deprecated Use {@link #mavenGav(String)} instead
     */
    @Deprecated(since = "1.42.0")
    public Map<String, String> mavenGav(String workDir, String file) throws Exception {
        return mavenGav(file);
    }
}
