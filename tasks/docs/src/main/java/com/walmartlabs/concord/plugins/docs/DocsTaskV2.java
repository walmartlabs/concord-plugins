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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.imports.ImportsListener;
import com.walmartlabs.concord.imports.NoopImportManager;
import com.walmartlabs.concord.runtime.v2.NoopImportsNormalizer;
import com.walmartlabs.concord.runtime.v2.ProjectLoaderV2;
import com.walmartlabs.concord.runtime.v2.model.Flow;
import com.walmartlabs.concord.runtime.v2.model.ProcessDefinition;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.walmartlabs.concord.plugins.docs.FlowDescriptionParser.FlowDescription;

@Named("docs")
public class DocsTaskV2 implements Task {

    private static final Logger log = LoggerFactory.getLogger(DocsTaskV2.class);

    private final Context context;

    @Inject
    public DocsTaskV2(Context context) {
        this.context = context;
    }

    @Override
    public TaskResult.SimpleResult execute(Variables input) throws Exception {
        var includeUndocumentedFlows = input.getBoolean("includeUndocumentedFlows", true);
        var flowsDir = getPathOrNull(input, "flowsDir");

        ProcessDefinition processDefinition;
        if (flowsDir == null) {
            log.info("Loading flows from current process");
            flowsDir = context.workingDirectory();
            processDefinition = context.execution().processDefinition();
        } else {
            log.info("Loading flows from '{}'", flowsDir);
            processDefinition = loadProcessDefinition(flowsDir);
        }

        var flowLinesByFileName = collectFlowLineNumbers(processDefinition.flows());
        var flowDescriptionByFileName = new LinkedHashMap<String, List<FlowDescription>>();
        var undocumentedFlowsByFileName = new LinkedHashMap<String, List<String>>();
        for (var entry : flowLinesByFileName.entrySet()) {
            var sourcePath = flowsDir.resolve(entry.getKey());
            if (!Files.exists(sourcePath)) {
                log.warn("Flows file '{}' does not exist", entry.getKey());
                continue;
            }

            var lines = Files.readAllLines(sourcePath);
            var descriptions = new ArrayList<FlowDescription>();
            var missing = new ArrayList<String>();

            for (var flowLineNum : entry.getValue()) {
                var desc = FlowDescriptionParser.parse(flowLineNum.flowName, lines, flowLineNum.lineNum);
                if (desc != null) {
                    descriptions.add(desc);
                } else {
                    if (includeUndocumentedFlows) {
                        descriptions.add(new FlowDescription(flowLineNum.lineNum + 1, flowLineNum.flowName, null, List.of(), List.of(), List.of()));
                    }
                    missing.add(flowLineNum.flowName);
                }
            }

            if (!descriptions.isEmpty()) {
                flowDescriptionByFileName.put(entry.getKey(), descriptions);
            }
            if (!missing.isEmpty()) {
                undocumentedFlowsByFileName.put(entry.getKey(), missing);
            }
        }

        var outputDir = Path.of(input.assertString("output"));
        MdBookGenerator.generate(input.assertString("bookTitle"), flowDescriptionByFileName, input.getString("sourceBaseUrl"), outputDir);

        logMissingFlowDocs(processDefinition.flows().size(), undocumentedFlowsByFileName);

        new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .writerWithDefaultPrettyPrinter()
                .writeValue(outputDir.resolve("flows.json").toFile(), flowDescriptionByFileName);

        return TaskResult.success();
    }

    private Path getPathOrNull(Variables input, String key) {
        var v = input.getString(key);
        if (v == null) {
            return null;
        }
        var p = normalize(context.workingDirectory(), v);
        if (Files.notExists(p)) {
            throw new UserDefinedException("'" + key + "' directory does not exists: " + p);
        }
        return p;
    }

    private ProcessDefinition loadProcessDefinition(Path flowsDir) {
        ProjectLoaderV2.Result loadResult;
        try {
            loadResult = new ProjectLoaderV2(new NoopImportManager())
                    .load(flowsDir, new NoopImportsNormalizer(), ImportsListener.NOP_LISTENER);
        } catch (Exception e) {
            log.error("Error while loading flows: {}", e.getMessage(), e);
            throw new RuntimeException("Error while loading flows: " + e.getMessage());
        }

        return loadResult.getProjectDefinition();
    }

    private static Map<String, List<FlowLineNum>> collectFlowLineNumbers(Map<String, Flow> flows) {
        return flows.entrySet().stream()
                .map(entry -> {
                    var loc = entry.getValue().location();
                    var fileName = loc.fileName();
                    int lineNum = loc.lineNum() - 1; // Concord line numbers are 1 based :)
                    if (fileName == null || lineNum < 0) {
                        return null;
                    }
                    return Map.entry(fileName, new FlowLineNum(entry.getKey(), lineNum));
                })
                .filter(Objects::nonNull)
                .collect(
                        Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                        )
                );
    }

    private static Path normalize(Path workDir, String path) {
        var p = Path.of(path);
        if (p.startsWith(workDir)) {
            return p.toAbsolutePath();
        }
        return workDir.resolve(p).toAbsolutePath();
    }

    private static void logMissingFlowDocs(int total, Map<String, List<String>> missing) {
        if (missing.isEmpty()) {
            return;
        }

        log.warn("Flows without description ({}/{}):", missing.values().stream().mapToInt(List::size).sum(), total);

        for (var entry : missing.entrySet()) {
            log.warn("{} ({}):\n{}", entry.getKey(), entry.getValue().size(), String.join("\n", entry.getValue()));
        }
    }

    record FlowLineNum (String flowName, int lineNum) {
    }
}
