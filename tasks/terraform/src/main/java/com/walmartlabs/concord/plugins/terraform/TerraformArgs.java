package com.walmartlabs.concord.plugins.terraform;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2022 Walmart Inc., Concord Authors
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

import java.nio.file.Path;
import java.util.List;

/**
 * Class for generating a collection of Terraform CLI arguments. Most can be
 * added with the {@link #add(String)} and {@link #add(String, String)} methods.
 * Arguments which contain a file path should be added with the {@link #add(Path)}
 * or {@link #add(String, Path)} so the path can be relativized depending on
 * where the <code>terraform</code> command is executed (e.g. local vs container)
 */
public interface TerraformArgs {
    /** @return true if terraform version support -chdir option */
    boolean hasChdir();
    /** Add a Terraform CLI option (e.g. "-auto-approve", "-json") */
    TerraformArgs add(String opt);

    /**
     * Add an option and value argument.
     * @param opt Terraform CLI option (e.g. "-input")
     * @param value Value for given option (e.g. "false")
     */
    TerraformArgs add(String opt, String value);

    /**
     * Add a path argument. Value will be translated to valid container path when
     * executing in a container.
     * @param path Absolute path within process' working directory
     */
    TerraformArgs add(Path path);

    /**
     * Add an option and path value argument. Value will be translated to valid
     * container path when executing in a container.
     * @param opt Terraform CLI option (e.g. "-var-file", "-out")
     * @param path Absolute path within process' working directory
     */
    TerraformArgs add(String opt, Path path);

    /**
     * @return Full list of <code>terraform</code> arguments in the order they
     * were added.
     */
    List<String> get();
}
