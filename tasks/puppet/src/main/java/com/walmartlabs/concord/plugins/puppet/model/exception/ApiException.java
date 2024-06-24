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

public class ApiException extends Exception {

    /**
     * HTTP status code
     **/
    private final int code;

    public static ApiException buildException(int statusCode, String errMessage) {
        String message;
        if ((400 <= statusCode) && (statusCode <= 499)) { // error
            message = "Client error occurred with API call (" + statusCode + "): " + errMessage;
        } else if ((500 <= statusCode) && (statusCode <= 599)) { // internal server error
            message = "Server error occurred with API call (" + statusCode + "): " + errMessage;
        } else {
            message = "Error occurred (HTTP Status " + statusCode + ") with API call: " + errMessage;
        }

        return new ApiException(statusCode, message);
    }

    private ApiException(int statusCode, String errMessage) {
        super(errMessage);
        this.code = statusCode;
    }

    public ApiException(String message) {
        super(message);
        this.code = 0;
    }

    public ApiException(Throwable t) {
        super(t);
        this.code = 0;
    }

    /**
     * @return HTTP status code
     */
    public int getCode() {
        return code;
    }
}
