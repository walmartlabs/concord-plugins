package com.walmartlabs.concord.plugins.puppet;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2019 Walmart Inc.
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

import com.walmartlabs.concord.plugins.puppet.model.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApiExceptionTest {

    @Test
    public void testWithStatusCode() {
        ApiException ex = ApiException.buildException(404, "not found");
        assertEquals(404, ex.getCode());
        assert(ex.getMessage().contains("404") && ex.getMessage().contains("not found"));

        ex = ApiException.buildException(504, "timeout");
        assertEquals(504, ex.getCode());
        assert(ex.getMessage().contains("Server error"));

        ex = ApiException.buildException(306, "unused");
        assertEquals(306, ex.getCode());
        assert(ex.getMessage().contains("Error occurred"));
    }


    @Test
    public void testMessage() {
        ApiException ex = new ApiException("error occurred");
        assertEquals("error occurred", ex.getMessage());
    }
}
