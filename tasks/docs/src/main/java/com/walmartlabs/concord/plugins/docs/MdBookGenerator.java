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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static com.walmartlabs.concord.plugins.docs.FlowDescriptionParser.FlowDescription;

public final class MdBookGenerator {

    private static final Comparator<SummaryEntry> SUMMARY_COMPARATOR = Comparator.comparing(
            e -> Arrays.asList(e.title().split("/")),
            (a, b) -> {
                int len = Math.min(a.size(), b.size());
                for (int i = 0; i < len; i++) {
                    int cmp = a.get(i).compareTo(b.get(i));
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                return Integer.compare(a.size(), b.size());
            });

    private record SummaryEntry (String title, String link) {
    }

    public static void generate(String title, LinkedHashMap<String, List<FlowDescription>> flowDescriptionByFileName, String sourceBaseUrl, Path outputDir) throws IOException  {
        var outputSrcDir = outputDir.resolve("src");
        Files.createDirectories(outputSrcDir);

        var flowPagePath = new HashMap<String, Path>();
        for (var entry : flowDescriptionByFileName.entrySet()) {
            var outFile = writeFlowsPage(entry.getKey(), entry.getValue(), sourceBaseUrl, outputSrcDir);
            flowPagePath.put(entry.getKey(), outFile);
        }
        writeSummary(flowPagePath, outputSrcDir);
        writeBook(title, outputDir);
    }

    private static void writeSummary(Map<String, Path> filesToPaths, Path outputDir) throws IOException {
        var sorted = filesToPaths.entrySet().stream()
                .map(e -> {
                    var title = e.getKey();
                    var link = "./" + outputDir.relativize(e.getValue().toAbsolutePath());
                    return new SummaryEntry(title, link);
                })
                .sorted(SUMMARY_COMPARATOR)
                .toList();

        var output = outputDir.resolve("SUMMARY.md");

        try (var writer = Files.newBufferedWriter(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("# Summary\n\n");

            var unfoldedEntries = new ArrayList<SummaryEntry>();
            String currentTopLevel = null;
            String currentFolderPath = null;
            for (var entry : sorted) {
                var parts = entry.title().split("/");
                if (parts.length < 2) {
                    unfoldedEntries.add(entry);
                    continue;
                }

                var topLevel = parts[0];
                var fileName = parts[parts.length - 1];
                var folderPath = String.join("/", Arrays.copyOfRange(parts, 1, parts.length - 1));

                if (!topLevel.equals(currentTopLevel)) {
                    writer.write("\n# " + topLevel + "\n\n");
                    currentTopLevel = topLevel;
                    currentFolderPath = null;
                }

                if (!folderPath.equals(currentFolderPath)) {
                    writer.write("- [" + folderPath + "]()\n");
                    currentFolderPath = folderPath;
                }

                writer.write("  - [" + fileName + "](" + entry.link() + ")\n");
            }

            if (!unfoldedEntries.isEmpty()) {
                if (unfoldedEntries.size() != filesToPaths.size()) {
                    writer.write("---\n");
                }

                for (var entry : unfoldedEntries) {
                    writer.write("  - [" + entry.title() + "](" + entry.link() + ")\n");
                }
            }
        }
    }

    private static Path writeFlowsPage(String flowFileName, List<FlowDescription> flows, String sourceBaseUrl, Path outputDir) throws IOException {
        var output = outputDir.resolve(flowFileName + ".md");
        Files.createDirectories(output.getParent());

        var normalizedBaseUrl = normalizeUrl(sourceBaseUrl);
        try (var writer = Files.newBufferedWriter(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("# " + flowFileName + "\n\n");

            for (var flow : flows) {
                writer.write("## `" + flow.name() + "`\n\n");
                writer.write("**Description**:  \n" + orNone(flow.description()) + "\n\n");

                if (!flow.tags().isEmpty()) {
                    writer.write("**Tags**: `" + String.join("`, `", flow.tags()) + "`\n\n");
                }

                writeParamTable(writer, "Inputs", flow.in());
                writeParamTable(writer, "Outputs", flow.out());

                if (sourceBaseUrl != null) {
                    var link = normalizedBaseUrl + flowFileName + "#L" + flow.lineNum();
                    writer.write("<a href=\"" + link + "\" target=\"_blank\" rel=\"noopener noreferrer\">View source code</a>\n\n");
                }

                writer.write("---\n\n");
            }
        }
        return output.toAbsolutePath();
    }

    private static String normalizeUrl(String sourceBaseUrl) {
        if (sourceBaseUrl == null || sourceBaseUrl.endsWith("/")) {
            return sourceBaseUrl;
        }

        return sourceBaseUrl + "/";
    }

    private static void writeBook(String title, Path outputDir) throws IOException {
        var output = outputDir.resolve("book.toml");
        try (var writer = Files.newBufferedWriter(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("[book]\n");
            writer.write("authors = [\"concord-docs\"]\n");
            writer.write("language = \"en\"\n");
            writer.write("src = \"src\"\n");
            writer.write("title = \""+ title + "\"\n");
        }
    }

    private static void writeParamTable(Appendable writer, String label, List<FlowDescription.Parameter> params) throws IOException {
        if (params == null || params.isEmpty()) {
            writer.append("**").append(label).append("**:  \n_None_\n\n");
            return;
        }

        writer.append("**").append(label).append("**:\n\n");
        writer.append("| Name | Type | Required | Description |\n");
        writer.append("|------|------|----------|-------------|\n");

        for (var p : params) {
            writer.append(String.format("| `%s` | %s | %s | %s |\n",
                    p.name(),
                    p.type(),
                    p.required() ? "✓" : "",
                    escapeMd(p.description())));
        }

        writer.append("\n");
    }

    private static String orNone(String s) {
        return (s == null || s.isBlank()) ? "_None_" : s;
    }

    private static String escapeMd(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("|", "\\|")
                .replace("_", "\\_")
                .replace("*", "\\*");
    }
}
