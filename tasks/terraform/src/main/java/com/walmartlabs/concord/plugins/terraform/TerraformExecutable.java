package com.walmartlabs.concord.plugins.terraform;

import java.nio.file.Path;

/**
 * A Terraform executable file may exist in a number of contexts (local filesystem
 * vs container filesystem) and may be detected in a shell PATH or explicitly
 * added to a particular working directory. {@link TerraformExecutable}
 * encapsulates these properties to help make sense of them in downstream usage
 * of the executable
 */
public class TerraformExecutable {

    final private Path executablePath;
    final private boolean sourceInContainer;

    public TerraformExecutable(Path executablePath, boolean sourceInContainer) {
        this.executablePath = executablePath;
        this.sourceInContainer = sourceInContainer;
    }

    public boolean isSourceInContainer() {
        return sourceInContainer;
    }

    public Path getExecutablePath() {
        return executablePath;
    }

    public boolean hasPath() {
        return executablePath != null;
    }
}
