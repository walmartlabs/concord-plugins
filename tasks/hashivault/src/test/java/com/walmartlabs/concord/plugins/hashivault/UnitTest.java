package com.walmartlabs.concord.plugins.hashivault;

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnitTest {

    @Test
    public void defaultActionTest() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("baseUrl", "http://example.com:8200");

        TaskParams params = TaskParams.of(new MapBackedVariables(vars), null);
        Assert.assertEquals(TaskParams.Action.READKV, params.action());
    }

    @Test
    public void requiredParametersTest() {
        TaskParams params = TaskParams.of(new MapBackedVariables(getMap()), null);
        try {
            params.baseUrl();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("'baseUrl' is required"));
        }

        try {
            params.apiToken();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("'apiToken' is required"));
        }

        try {
            params.path();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("'path' is required"));
        }

        params.kvPairs(); // default action

        // kvPairs required when action is writeKv
        params = TaskParams.of(new MapBackedVariables(getMap("action", "writeKv")), null);
        try {
            params.kvPairs();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("'kvPairs' is required"));
        }

        // cubbyhole is a v1 endpoint
        params = TaskParams.of(new MapBackedVariables(
                getMap("path", "cubbyhole/mysecret", "engineVersion", 2)), null);
        assertEquals(1, params.engineVersion());
    }

    private static Map<String, Object> getMap(Object... params) {
        if (params.length % 2 != 0) {
            throw new RuntimeException("Must have event number of parameters");
        }

        if (params.length == 0) {
            return Collections.emptyMap();
        }

        Map<String, Object> m = new HashMap<>(params.length / 2);
        for (int i=0; i<params.length; i+=2) {
            m.put(params[i].toString(), params[i+1]);
        }

        return m;
    }
}
