package org.fcrepo.examples.models.integration;
/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * This integration test loads each set of content models independently, in
 * separate workspaces. Tests should fail if the CND files are invalid as a set.
 *
 * @author Gregory Jansen
 */
public class ExampleNodeTypesIT {

    private static Logger logger = getLogger(ExampleNodeTypesIT.class);

    private static final int SERVER_PORT = Integer.parseInt(System
            .getProperty("test.port", "8080"));

    private static final String HOSTNAME = "localhost";

    private static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + "/fedora/rest";

    private static final String modelSetsPath =
            "src/test/resources/model-sets";

    private static File modelSetsDirectory = null;

    private PoolingHttpClientConnectionManager connectionManager = null;

    private static CloseableHttpClient client;

    public ExampleNodeTypesIT() {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
        client =
                HttpClientBuilder.create().setConnectionManager(
                        connectionManager).build();
        modelSetsDirectory = new File(modelSetsPath);
        if (!modelSetsDirectory.exists()) {
            throw new Error("Cannot find model sets");
        }
    }

    @Test
    public void testModelSets() throws Exception {
        for (final File f : modelSetsDirectory.listFiles()) {
            if (!f.isDirectory()) {
                logger.warn("Found a file in model sets directory: {}", f
                        .getPath());
            } else {
                logger.debug("Testing model set: {}", f.getPath());
                for (final File cnd : f.listFiles(new FilenameFilter() {
                    @Override
                    public final boolean accept(final File dir, final String name) {
                        return name.endsWith(".cnd");
                    }
                })) {
                    ingestNodeTypes(cnd);
                }
            }
        }
    }

    /**
     * curl -X POST -H "Content-Type: text/cnd" -d "@cnd.txt"
     * "http://localhost:8080/rest/fcr:nodetypes"
     *
     * @param cndFile
     * @throws Exception
     */
    private void ingestNodeTypes(final File cndFile)
            throws Exception {
        final HttpPost post = new HttpPost(serverAddress + "/fcr:nodetypes");
        post.setHeader("Content-Type", "text/cnd");
        logger.debug("POST: {}", post.getURI());
        post.setEntity(new FileEntity(cndFile));
        final HttpResponse response = client.execute(post);
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Expected NO CONTENT response.", NO_CONTENT
                .getStatusCode(), status);
    }

}
