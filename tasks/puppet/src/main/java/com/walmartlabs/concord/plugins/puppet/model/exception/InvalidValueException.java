package com.walmartlabs.concord.plugins.puppet.model.exception;

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

import java.util.Arrays;

/**
 * Throw when a parameter is set with a value that is invalid
 */
public class InvalidValueException extends RuntimeException {
    /**
     * Creates object with message detailing what parameter value is invalid with a list of allowed
     * values
     *
     * @param name    Parameter for which the invalid value was given
     * @param v       Value given
     * @param allowed Array of allows object values
     */
    public InvalidValueException(String name, Object v, Object[] allowed) {
        super("Value '" + v + "' given for '" + name + "' is invalid. Allowed values are: " + Arrays.toString(allowed));
    }

    /**
     * Creates object with message detailing what parameter value is invalid with an extra message.
     *
     * @param name     Parameter for which an invalid value was given
     * @param v        Value given
     * @param extraMsg Custom message to append
     */
    public InvalidValueException(String name, Object v, String extraMsg) {
        super("Value '" + v + "' given for '" + name + "' is invalid. " + extraMsg);
    }
}
