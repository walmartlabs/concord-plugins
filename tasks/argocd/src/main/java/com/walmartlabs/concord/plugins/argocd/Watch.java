/**
 * Concord
 * -----
 * Copyright (C) 2017 - 2021 Walmart Inc.
 * -----
 */
package com.walmartlabs.concord.plugins.argocd;

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

import okhttp3.Call;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.function.Function;

public class Watch<T> implements AutoCloseable {

    private final Call call;
    private final ResponseBody response;
    private final Function<String, T> converter;

    public Watch(Call call, ResponseBody response, Function<String, T> converter) {
        this.call = call;
        this.response = response;
        this.converter = converter;
    }

    public T next() {
        try {
            String line = response.source().readUtf8Line();
            if (line == null) {
                throw new RuntimeException("Null response from the server");
            }
            return converter.apply(line);
        } catch (IOException e) {
            throw new RuntimeException("IO Exception during next event", e);
        }
    }

    public boolean hasNext() {
        try {
            return !response.source().exhausted();
        } catch (InterruptedIOException e) {
            if ("timeout".equals(e.getMessage())) {
                throw new RuntimeException("Timeout waiting");
            }
            throw new RuntimeException("IO Exception during hasNext event", e);
        } catch (IOException e) {
            throw new RuntimeException("IO Exception during hasNext event", e);
        }
    }

    @Override
    public void close() {
        call.cancel();
        response.close();
    }
}
