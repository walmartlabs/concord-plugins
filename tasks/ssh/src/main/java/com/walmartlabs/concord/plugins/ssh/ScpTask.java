package com.walmartlabs.concord.plugins.ssh;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2025 Walmart Inc., Concord Authors
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

import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.walmartlabs.concord.plugins.ssh.JSchUtils.initJsch;
import static com.walmartlabs.concord.plugins.ssh.JSchUtils.sshExec;

@Named("scp")
public class ScpTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(ScpTask.class);

    @Override
    public TaskResult execute(Variables input) throws Exception {
        var host = input.assertString("host");
        var user = input.assertString("user");
        var password = input.getString("password");
        var identities = input.getList("identities", List.<String>of());
        var port = input.getInt("port", 22);
        var timeout = input.getInt("timeout", 30000);
        var src = input.assertString("src");
        var dest = input.assertString("dest");

        if (!dest.startsWith("/")) {
            throw new IOException("The 'dest' path must be absolute: " + dest);
        }

        var localPath = Paths.get(src);
        if (!Files.exists(localPath)) {
            throw new IOException("The 'src' file doesn't exist: " + src);
        }
        if (!Files.isReadable(localPath)) {
            throw new IOException("The 'src' file is not readable: " + src);
        }

        log.info("Sending local file {} to {}@{}:{}{}...", src, user, host, port, dest);

        var lastModified = Files.getLastModifiedTime(localPath).toMillis();
        var fileSize = Files.size(localPath);

        var jsch = initJsch(identities);
        try (var exec = sshExec(jsch, user, password, host, port, timeout)) {
            var channel = exec.channel();
            channel.setCommand("scp -p -t " + dest);
            channel.connect();

            var out = channel.getOutputStream();
            var in = channel.getInputStream();

            checkAck(in);

            // last modified
            out.write(("T" + (lastModified / 1000) + " 0 " + (lastModified / 1000) + " 0\n").getBytes());
            out.flush();
            checkAck(in);

            // file size
            out.write(("C0644 " + fileSize + " " + localPath.getFileName().toString() + "\n").getBytes());
            out.flush();
            checkAck(in);

            // send content
            try (var fis = new BufferedInputStream(Files.newInputStream(localPath))) {
                fis.transferTo(out);
            }

            // end of content
            out.write(0);
            out.flush();
            checkAck(in);

            return TaskResult.success();
        }
    }

    private void checkAck(InputStream in) throws IOException {
        int b = in.read();

        if (b == 0) {
            return;
        }

        if (b == -1) {
            throw new IOException("EOF");
        }

        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1) {
                throw new IOException("SCP error: " + sb);
            }
            throw new IOException("SCP fatal error: " + sb);
        }

        throw new IllegalStateException("Unknown SCP error");
    }
}
