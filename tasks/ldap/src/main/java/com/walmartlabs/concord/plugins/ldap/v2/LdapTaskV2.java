package com.walmartlabs.concord.plugins.ldap.v2;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.plugins.ldap.LdapSearchParams;
import com.walmartlabs.concord.plugins.ldap.LdapTaskCommon;
import com.walmartlabs.concord.plugins.ldap.TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import com.walmartlabs.concord.sdk.MapUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;

@Named("ldap")
@SuppressWarnings("unused")
public class LdapTaskV2 implements Task {

    private final Context context;

    private final LdapTaskCommon delegate = new LdapTaskCommon();

    @Inject
    public LdapTaskV2(Context context) {
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) {
        Map<String, Object> result = delegate.execute(TaskParams.of(input, context.defaultVariables().toMap()));
        return TaskResult.of(MapUtils.getBoolean(result, "success", false))
                .values(result);
    }

    public boolean isMemberOf(String userDn, String groupDn) {
        LdapSearchParams searchParams = TaskParams.searchParams(new MapBackedVariables(Collections.emptyMap()), context.defaultVariables().toMap());
        return new LdapTaskCommon().isMemberOf(searchParams, userDn, groupDn);
    }
}
