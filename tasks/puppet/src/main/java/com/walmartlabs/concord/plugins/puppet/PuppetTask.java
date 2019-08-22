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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.plugins.puppet.model.PuppetResult;
import com.walmartlabs.concord.plugins.puppet.model.cfg.DbQueryCfg;
import com.walmartlabs.concord.plugins.puppet.model.cfg.RbacCfg;
import com.walmartlabs.concord.plugins.puppet.model.dbquery.DbQueryPayload;
import com.walmartlabs.concord.plugins.puppet.model.exception.InvalidValueException;
import com.walmartlabs.concord.plugins.puppet.model.exception.MissingParameterException;
import com.walmartlabs.concord.plugins.puppet.model.token.TokenPayload;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.InjectVariable;
import com.walmartlabs.concord.sdk.SecretService;
import com.walmartlabs.concord.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.plugins.puppet.Constants.Actions.ACTION_CREATE_API_TOKEN;
import static com.walmartlabs.concord.plugins.puppet.Constants.Actions.ACTION_DB_QUERY;
import static com.walmartlabs.concord.plugins.puppet.Constants.Keys.*;

@Named("puppet")
public class PuppetTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(PuppetTask.class);

    @InjectVariable("puppetParams")
    private Map<String, Object> defaults;

    // 'ignoreErrors' and 'action' variables are not part of PuppetConfiguration
    // because they are needed before a config can be created

    @InjectVariable(IGNORE_ERRORS_KEY)
    private Boolean ignoreErrors;

    @InjectVariable(ACTION_KEY)
    private String action;

    @Inject
    protected SecretService secretService;


    @Override
    public void execute(Context ctx) throws Exception {
        PuppetResult result;

        try {
            result = getResult(ctx);

        } catch (Exception ex) {
            // Determine if task should fail gracefully
            if (ignoreErrors != null && ignoreErrors) {
                result = new PuppetResult(false, null, ex.getMessage());
            } else {
                throw ex;
            }
        }

        ObjectMapper om = new ObjectMapper();
        ctx.setVariable(OUT_VARIABLE_KEY, om.convertValue(result, Map.class));
    }

    private PuppetResult getResult(Context ctx) throws Exception {
        if (action == null || action.trim().isEmpty()) {
            throw new MissingParameterException(ACTION_KEY);
        }

        switch (action) {
            case ACTION_CREATE_API_TOKEN:
                return new PuppetResult(true, createToken(ctx), null);
            case ACTION_DB_QUERY:
                return new PuppetResult(true, dbQuery(ctx), null);
            default:
                throw new InvalidValueException(ACTION_KEY, action, "Not a supported action.");
        }
    }

    /**
     * Executes a PuppetDB query
     * @param ctx Concord process {@link Context}
     * @return Query results
     * @throws Exception when query cannot be executed
     */
    private List dbQuery(Context ctx) throws Exception {
        DbQueryCfg cfg = Utils.createCfg(ctx, secretService, defaults, DbQueryCfg.class);
        PuppetClient client = new PuppetClient(cfg);

        Utils.debug(log, cfg.doDebug(),
                String.format("Executing query: %s", cfg.getQueryString())
        );
        Utils.debug(log, cfg.doDebug(),
                String.format("Task result will be saved as '%s' variable.", OUT_VARIABLE_KEY));


        log.info("Starting Puppet Query on {}", cfg.getBaseUrl());

        return client.dbQuery(new DbQueryPayload(cfg));
    }

    /**
     * Create an API token
     * @param ctx Concord process {@link Context}
     * @return the new API token
     * @throws Exception When something goes wrong communicating with the API
     */
    private String createToken(Context ctx) throws Exception {
        RbacCfg cfg = Utils.createCfg(ctx, secretService, defaults, RbacCfg.class);
        PuppetClient client = new PuppetClient(cfg);
        TokenPayload payload = new TokenPayload(cfg);

        return client.createToken(payload);
    }
}
