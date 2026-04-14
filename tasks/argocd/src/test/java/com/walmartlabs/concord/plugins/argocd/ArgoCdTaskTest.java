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

import com.walmartlabs.concord.plugins.argocd.openapi.model.V1alpha1HelmParameter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArgoCdTaskTest {

    @Test
    void addOrReplaceParamUpdatesExistingParam() {
        V1alpha1HelmParameter existing = new V1alpha1HelmParameter()
                .name("image.tag")
                .value("old");
        List<V1alpha1HelmParameter> params = new ArrayList<>();
        params.add(existing);

        TaskParams.SetAppParams.HelmParam replacement = mock(TaskParams.SetAppParams.HelmParam.class);
        when(replacement.name()).thenReturn("image.tag");
        when(replacement.value()).thenReturn("new");

        ArgoCdTask.addOrReplaceParam(params, replacement);

        assertEquals(1, params.size());
        assertEquals("new", params.get(0).getValue());
    }
}
