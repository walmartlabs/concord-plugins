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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class FlowDescriptionParserTest {

    @Test
    void testParseFullCommentBlock() {
        var lines = """
            flows:
              ##
              # Creates a compute instance
              #
              # in:
              #   label: string, mandatory, label for the instance
              #   hostname: string, optional, hostname of the instance
              # out:
              #   result: string, mandatory, Some result
              # tags: compute, infra
              ##
              createInstance:
            """.lines().toList();

        var result = FlowDescriptionParser.parse("createInstance", lines, lines.size() - 1);

        assertNotNull(result);
        assertEquals("createInstance", result.name());
        assertEquals("Creates a compute instance", result.description());

        assertEquals(List.of("compute", "infra"), result.tags());

        assertEquals(2, result.in().size());

        var param1 = result.in().get(0);
        assertEquals("label", param1.name());
        assertEquals("string", param1.type());
        assertTrue(param1.required());
        assertEquals("label for the instance", param1.description());

        var param2 = result.in().get(1);
        assertEquals("hostname", param2.name());
        assertEquals("string", param2.type());
        assertFalse(param2.required());
        assertEquals("hostname of the instance", param2.description());

        assertEquals(1, result.out().size());
        var oparam1 = result.out().get(0);
        assertEquals("result", oparam1.name());
        assertEquals("string", oparam1.type());
        assertTrue(oparam1.required());
        assertEquals("Some result", oparam1.description());
    }

    @Test
    void testMissingCommentBlockReturnsNull() {
        List<String> lines = List.of(
                "flows:",
                "  createInstance:"
        );

        var result = FlowDescriptionParser.parse("createInstance", lines, 1);
        assertNull(result);
    }

    @Test
    void testOnlyDescription() {
        List<String> lines = List.of(
                "flows:",
                "  ##",
                "  # Just a description without inputs",
                "  ##",
                "  simpleFlow:"
        );

        var result = FlowDescriptionParser.parse("simpleFlow", lines, 4);
        assertNotNull(result);
        assertEquals("Just a description without inputs", result.description());
        assertTrue(result.in().isEmpty());
        assertTrue(result.out().isEmpty());
        assertTrue(result.tags().isEmpty());
    }

    @Test
    void testParsingWithoutDescription() {
        var lines = """
            flows:
              #
              # in:
              #   label: string, mandatory, label for the instance
              #   hostname: string, optional, hostname of the instance
              # out:
              #   result: string, mandatory, Some result
              # tags: compute, infra
              createInstance:
            """.lines().toList();

        var result = FlowDescriptionParser.parse("createInstance", lines, 8);

        assertNotNull(result);
        assertEquals("createInstance", result.name());
        assertNull(result.description());

        assertEquals(2, result.in().size());
        assertEquals(1, result.out().size());
        assertEquals(2, result.tags().size());
    }

    @Test
    void testParamParsingWithoutDescription() {
        var lines = """
            flows:
              #
              # in:
              #   label: string, mandatory
              #   hostname: string, optional,     
              # out:
              #   result: string, mandatory,
              # tags: compute, infra
              createInstance:
            """.lines().toList();

        var result = FlowDescriptionParser.parse("createInstance", lines, 8);

        assertNotNull(result);
        assertEquals("createInstance", result.name());
        assertNull(result.description());

        assertEquals(2, result.in().size());

        var param1 = result.in().get(0);
        assertEquals("label", param1.name());
        assertEquals("string", param1.type());
        assertTrue(param1.required());
        assertNull(param1.description());

        var param2 = result.in().get(1);
        assertEquals("hostname", param2.name());
        assertEquals("string", param2.type());
        assertFalse(param2.required());
        assertNull(param2.description());

        assertEquals(1, result.out().size());
        var oparam1 = result.out().get(0);
        assertEquals("result", oparam1.name());
        assertEquals("string", oparam1.type());
        assertTrue(oparam1.required());
        assertNull(oparam1.description());

        assertEquals(2, result.tags().size());
    }
}
