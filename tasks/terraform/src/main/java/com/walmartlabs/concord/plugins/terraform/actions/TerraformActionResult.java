package com.walmartlabs.concord.plugins.terraform.actions;

import java.io.Serializable;
import java.util.Map;

public class TerraformActionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public static TerraformActionResult ok(Map<String, Object> data) {
        return new TerraformActionResult(true, null, data, null, null, false);
    }

    public static TerraformActionResult ok(Map<String, Object> data, String output) {
        return new TerraformActionResult(true, null, data, output, null, false);
    }

    public static TerraformActionResult error(String error) {
        return new TerraformActionResult(false, error, null, null, null, false);
    }

    public static TerraformActionResult noChanges(String output, String planPath) {
        return new TerraformActionResult(true, null, null, output, planPath, false);
    }

    public static TerraformActionResult hasChanges(String output, String planPath) {
        return new TerraformActionResult(true, null, null, output, planPath, true);
    }

    private final boolean ok;
    private final String error;
    private final Map<String, Object> data;
    private final String output;
    private final String planPath;
    private final Boolean hasChanges;

    public TerraformActionResult(boolean ok, String error, Map<String, Object> data, String output, String planPath, Boolean hasChanges) {
        this.ok = ok;
        this.error = error;
        this.data = data;
        this.output = output;
        this.planPath = planPath;
        this.hasChanges = hasChanges;
    }

    public boolean isOk() {
        return ok;
    }

    public String getError() {
        return error;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getOutput() {
        return output;
    }

    public String getPlanPath() {
        return planPath;
    }

    public Boolean getHasChanges() {
        return hasChanges;
    }
}
