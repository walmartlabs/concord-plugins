package com.walmartlabs.concord.plugins.docs;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FlowDescriptionParser {

    public record FlowDescription (int lineNum, String name, String description, List<Parameter> in, List<Parameter> out, List<String> tags) {

        record Parameter (String name, String type, boolean required, String description) {
        }
    }

    public static FlowDescription parse(String flowName, List<String> lines, int flowLineNum) {
        if (!lines.get(flowLineNum).contains(flowName)) {
            throw new IllegalArgumentException("Invalid flowLineNum " + flowLineNum + ": expected flow name '" + flowName+ "' at line '" + lines.get(flowLineNum) + "'");
        }

        var descriptionStart = findDescriptionStart(lines, flowLineNum);
        if (descriptionStart == -1) {
            return null;
        }

        return parseCommentBlock(flowName, lines, descriptionStart, flowLineNum);
    }

    private static FlowDescription parseCommentBlock(String flowName, List<String> lines, int startLine, int endLine) {
        enum State { DESCRIPTION, INPUTS, OUTPUTS, OTHER }

        var state = State.DESCRIPTION;
        var descriptionBuilder = new StringBuilder();
        var inputs = new ArrayList<FlowDescription.Parameter>();
        var outputs = new ArrayList<FlowDescription.Parameter>();
        var tags = List.<String>of();

        for (var i = startLine; i < endLine; i++) {
            var line = lines.get(i).trim();
            if (!line.startsWith("#")) {
                continue;
            }

            var content = line.replaceFirst("#+", "").trim();

            if ("in:".equalsIgnoreCase(content)) {
                state = State.INPUTS;
                continue;
            } else if ("out:".equalsIgnoreCase(content)) {
                state = State.OUTPUTS;
                continue;
            } else if (content.toLowerCase().startsWith("tags:")) {
                var tagsContent = content.substring(5).trim();
                if (!tagsContent.isEmpty()) {
                    tags = Arrays.asList(tagsContent.split("\\s*,\\s*"));
                }
                state = State.OTHER;
                continue;
            }

            switch (state) {
                case DESCRIPTION -> {
                    if (!content.isEmpty()) {
                        if (!descriptionBuilder.isEmpty()) {
                            descriptionBuilder.append(" ");
                        }
                        descriptionBuilder.append(content);
                    }
                }
                case INPUTS -> {
                    var param = parseParameterLine(content);
                    if (param != null) {
                        inputs.add(param);
                    }
                }
                case OUTPUTS -> {
                    var param = parseParameterLine(content);
                    if (param != null) {
                        outputs.add(param);
                    }
                }
                case OTHER -> {
                    // ignore other lines
                }
            }
        }

        return new FlowDescription(endLine + 1, flowName, descriptionBuilder.isEmpty() ? null : descriptionBuilder.toString(), inputs, outputs, tags);
    }

    private static int findDescriptionStart(List<String> lines, int flowLineNum) {
        if (flowLineNum >= lines.size()) {
            return -1;
        }

        int result = -1;
        var expectedIndent = getIndent(lines.get(flowLineNum));
        for (var i = flowLineNum - 1; i >= 0; i--) {
            var line = lines.get(i);
            var trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }
            var lineIndent = getIndent(line);
            if (lineIndent != expectedIndent) {
                break;
            }
            if (trimmedLine.startsWith("#")) {
                result = i;
            } else {
                break;
            }
        }
        return result;
    }

    private static int getIndent(String line) {
        var index = 0;
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        return index;
    }

    private static FlowDescription.Parameter parseParameterLine(String content) {
        int nameEnd = content.indexOf(':');
        if (nameEnd == -1) {
            return null;
        }

        int typeEnd = content.indexOf(',', nameEnd);
        if (typeEnd == -1) {
            return null;
        }

        int requiredEnd = content.indexOf(',', typeEnd + 1);
        if (requiredEnd == -1) {
            requiredEnd = content.length();
        }

        var name = content.substring(0, nameEnd).trim();
        var type = content.substring(nameEnd + 1, typeEnd).trim();

        var requiredString = content.substring(typeEnd + 1, requiredEnd).trim();
        var required = requiredToBoolean(requiredString);
        String description = null;
        if (requiredEnd < content.length()) {
            description = content.substring(requiredEnd + 1).trim();
            if (description.isEmpty()) {
                description = null;
            }
        }

        return new FlowDescription.Parameter(name, type, required, description);
    }

    private static boolean requiredToBoolean(String str) {
        return "mandatory".equalsIgnoreCase(str) || "required".equalsIgnoreCase(str);
    }

    private FlowDescriptionParser() {
    }
}
