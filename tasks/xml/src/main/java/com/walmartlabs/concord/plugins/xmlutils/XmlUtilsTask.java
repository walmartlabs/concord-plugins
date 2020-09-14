package com.walmartlabs.concord.plugins.xmlutils;

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

import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.Task;

import javax.inject.Named;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Named("xmlUtils")
@SuppressWarnings("unused")
public class XmlUtilsTask implements Task {

    /**
     * Evaluates the expression and returns a single {@link String} value.
     */
    public String xpathString(@InjectVariable("workDir") String workDir, String file, String expression) throws Exception {
        return delegate(workDir).xpathString(file, expression);
    }

    /**
     * Evaluates the expression and returns a list of {@link String} values.
     */
    public List<String> xpathListOfStrings(@InjectVariable("workDir") String workDir, String file, String expression) throws Exception {
        return delegate(workDir).xpathListOfStrings(file, expression);
    }

    /**
     * Uses XPath to return {@code groupId + artifactId + version} attributes from a Maven pom.xml file.
     * Knows how to handle the {@code <parent>} tag, i.e. parent GAV values are merged with the pom's own GAV.
     */
    public Map<String, String> mavenGav(@InjectVariable("workDir") String workDir, String file) throws Exception {
        return delegate(workDir).mavenGav(file);
    }

    @Override
    public void execute(Context ctx) {
        throw new RuntimeException("The task can only be used in expressions");
    }

    private static XmlUtilsTaskCommon delegate(String workDir) {
        return new XmlUtilsTaskCommon(Paths.get(workDir));
    }
}
