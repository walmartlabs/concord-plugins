package com.walmartlabs.concord.plugins.taurus;

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

import com.walmartlabs.concord.plugins.taurus.v2.TaurusTaskV2;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

public class TaurusTaskV2IT extends AbstractIT {

    @Test
    public void testV2() throws Exception {
        prepareScenario();

        Context ctx = Mockito.mock(Context.class);

        Mockito.when(ctx.workingDirectory()).thenReturn(workDir());
        // TODO test more inputs as default variables
        Mockito.when(ctx.defaultVariables()).thenReturn(new MapBackedVariables(Collections.emptyMap()));

        TestDependencyManager testDM = new TestDependencyManager("taurus");
        TaurusTaskV2 t = new TaurusTaskV2(ctx, testDM::resolve);

        t.execute(new MapBackedVariables(getArgs()));
    }
}
