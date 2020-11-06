package com.walmartlabs.concord.plugins.puppet;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
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

import com.walmartlabs.concord.plugins.puppet.model.PuppetResult;
import com.walmartlabs.concord.plugins.puppet.model.cfg.DbQueryCfg;
import com.walmartlabs.concord.plugins.puppet.model.cfg.RbacCfg;
import com.walmartlabs.concord.plugins.puppet.model.dbquery.DbQueryPayload;
import com.walmartlabs.concord.plugins.puppet.model.exception.InvalidValueException;
import com.walmartlabs.concord.plugins.puppet.model.exception.MissingParameterException;
import com.walmartlabs.concord.plugins.puppet.model.token.TokenPayload;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.plugins.puppet.Constants.Actions;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.*;

@Named("puppet")
public class PuppetTaskV2 implements Task {

    private static final Logger log = LoggerFactory.getLogger(PuppetTaskV2.class);

    private final Map<String, Object> defaults;
    private final SecretService secretService;

    @Inject
    public PuppetTaskV2(SecretService secretService, Context taskContext) {
        this.secretService = secretService;
        defaults = taskContext.variables().getMap(PARAMS_KEY, new HashMap<>());
    }

    @Override
    public TaskResult.SimpleResult execute(Variables input) throws Exception {
        Map<String, Object> vars = input.toMap();
        boolean ignoreErrors = MapUtils.getBoolean(
                vars,
                IGNORE_ERRORS_KEY,
                MapUtils.getBoolean(defaults, IGNORE_ERRORS_KEY, false)
        );

        PuppetResult result;

        try {
            result = getResult(vars, defaults);
        } catch (Exception ex) {
            // Determine if task should fail gracefully
            if (ignoreErrors) {
                result = new PuppetResult(false, null, ex.getMessage());
            } else {
                throw ex;
            }
        }

        return TaskResult.of(result.isOk(), result.getError())
                .value("data", result.getData());
    }

    private PuppetResult getResult(
            Map<String, Object> params,
            Map<String, Object> defaultParams) throws Exception {

        String action = MapUtils.getString(params, ACTION_KEY, Actions.NONE);

        Map<String, Object> merged = UtilsV2.mergeParams(params, defaultParams);

        switch (action) {
            case Actions.CREATE_API_TOKEN:
                return new PuppetResult(true, createToken(merged), null);
            case Actions.DB_QUERY:
                return new PuppetResult(true, dbQuery(merged), null);
            case Actions.NONE:
                throw new MissingParameterException(ACTION_KEY);
            default:
                throw new InvalidValueException(ACTION_KEY, action, "Not a supported action.");
        }
    }

    /**
     * Executes a PuppetDB query
     *
     * @param params merged global and task parameters
     * @return Query results
     * @throws Exception when query cannot be executed
     */
    private List<Map<String, Object>> dbQuery(Map<String, Object> params) throws Exception {
        DbQueryCfg cfg = UtilsV2.createCfg(params, secretService, DbQueryCfg.class);
        PuppetClient client = new PuppetClient(cfg);

        Utils.debug(log, cfg.doDebug(),
                String.format("Executing query: %s", cfg.getQueryString())
        );

        log.info("Starting Puppet Query on {}", cfg.getBaseUrl());

        return client.dbQuery(new DbQueryPayload(cfg));
    }

    /**
     * Create an API token
     *
     * @param params merged global and task parameters
     * @return the new API token
     * @throws Exception When something goes wrong communicating with the API
     */
    private String createToken(Map<String, Object> params) throws Exception {
        RbacCfg cfg = UtilsV2.createCfg(params, secretService, RbacCfg.class);
        PuppetClient client = new PuppetClient(cfg);
        TokenPayload payload = new TokenPayload(cfg);

        return client.createToken(payload);
    }
}
