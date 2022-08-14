package com.walmartlabs.concord.plugins.taurus;

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

import java.io.IOException;
import java.nio.file.Path;

public class BinaryResolver {

    private final BinaryDownloader binaryDownloader;

    public BinaryResolver(BinaryDownloader binaryDownloader) {
        this.binaryDownloader = binaryDownloader;
    }

    public Path resolve(String url) throws Exception {
        return binaryDownloader.download(url);
    }

    public interface BinaryDownloader {
        Path download(String url) throws IOException;
    }
}
