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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Named;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlUtilsTaskCommon {

    private final Path workDir;

    public XmlUtilsTaskCommon(Path workDir) {
        this.workDir = workDir;
    }

    /**
     * Evaluates the expression and returns a single {@link String} value.
     */
    public String xpathString(String file, String expression) throws Exception {
        Node n = (Node) eval(workDir, file, expression, XPathConstants.NODE);

        if (n == null) {
            throw new IllegalArgumentException("Node not found: " + expression + " (file: " + file + ")");
        }

        if (n.getNodeType() != Node.TEXT_NODE) {
            throw new IllegalArgumentException("The expression's (" + expression + ") result is not a string: " + n
                    + " (file: " + file + ")");
        }

        return n.getTextContent();
    }

    /**
     * Evaluates the expression and returns a list of {@link String} values.
     */
    public List<String> xpathListOfStrings(String file, String expression) throws Exception {
        NodeList l = (NodeList) eval(workDir, file, expression, XPathConstants.NODESET);

        if (l == null) {
            throw new IllegalArgumentException("Node not found: " + expression + " (file: " + file + ")");
        }

        List<String> result = new ArrayList<>(l.getLength());
        for (int i = 0; i < l.getLength(); i++) {
            Node n = l.item(i);
            if (n.getNodeType() != Node.TEXT_NODE) {
                throw new IllegalArgumentException("Node value is not a string: " + n
                        + " (expression: " + expression + ", file: " + file + ")");
            }

            result.add(n.getTextContent());
        }

        return result;
    }

    /**
     * Uses XPath to return {@code groupId + artifactId + version} attributes from a Maven pom.xml file.
     * Knows how to handle the {@code <parent>} tag, i.e. parent GAV values are merged with the pom's own GAV.
     */
    public Map<String, String> mavenGav(String file) throws Exception {
        Document document = assertDocument(workDir, file);
        XPath xpath = XPathFactory.newInstance().newXPath();

        Map<String, String> parentGav = toGav(file, xpath, document,
                "/project/parent/*[local-name()='groupId' or local-name()='artifactId' or local-name()='version']");

        Map<String, String> ownGav = toGav(file, xpath, document,
                "/project/*[local-name()='groupId' or local-name()='artifactId' or local-name()='version']");

        Map<String, String> result = new HashMap<>(parentGav);
        result.putAll(ownGav);
        return result;
    }

    private static Object eval(Path workDir, String file, String expression, QName returnType) throws Exception {
        Document document = assertDocument(workDir, file);
        XPath xpath = XPathFactory.newInstance().newXPath();
        return xpath.evaluate(expression, document, returnType);
    }

    private static Document assertDocument(Path workDir, String file) throws Exception {
        Path src = workDir.resolve(file);
        if (!Files.exists(src)) {
            throw new IllegalArgumentException("File not found: " + file);
        }

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(src.toFile());
    }

    private static Map<String, String> toGav(String file, XPath xpath, Document document, String expression) throws Exception {
        NodeList l = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);

        Map<String, String> result = new HashMap<>(l.getLength());
        for (int i = 0; i < l.getLength(); i++) {
            Node n = l.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                throw new IllegalArgumentException("Unknown node type: " + n
                        + " (expression: " + expression + ", file: " + file + ")."
                        + " Invalid input data?");
            }

            String value = n.getFirstChild().getNodeValue();
            result.put(n.getNodeName(), value);
        }

        return result;
    }
}
