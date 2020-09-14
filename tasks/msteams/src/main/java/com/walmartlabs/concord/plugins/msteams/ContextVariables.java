package com.walmartlabs.concord.plugins.msteams;

import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.sdk.Context;

import java.util.Map;

public class ContextVariables implements Variables {

    private final Context context;

    public ContextVariables(Context context) {
        this.context = context;
    }

    @Override
    public Object get(String key) {
        return context.getVariable(key);
    }

    @Override
    public void set(String key, Object value) {
        throw new IllegalStateException("Unsupported");
    }

    @Override
    public boolean has(String key) {
        return context.getVariable(key) != null;
    }

    @Override
    public Map<String, Object> toMap() {
        return context.toMap();
    }
}