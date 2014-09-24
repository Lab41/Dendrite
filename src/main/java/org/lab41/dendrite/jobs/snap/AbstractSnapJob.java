/*
 * Copyright 2014 In-Q-Tel/Lab41
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab41.dendrite.jobs.snap;

import org.apache.commons.io.IOUtils;
import org.lab41.dendrite.jobs.AbstractGraphUpdateJob;
import org.lab41.dendrite.jobs.ExportEdgeListJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractSnapJob extends AbstractGraphUpdateJob {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSnapJob.class);

    protected AbstractSnapJob(MetaGraph metaGraph, JobMetadata.Id jobId, DendriteGraph graph) {
        super(metaGraph, jobId, graph);
    }

    @Override
    protected void updateGraph() throws Exception {
        Path tmpDir = Files.createTempDirectory("tmp");

        try {
            Path inputPath = tmpDir.resolve("input");

            try {
                runExport(inputPath);

                Path outputPath = tmpDir.resolve("output");

                try {
                    runSnap(inputPath, outputPath);

                    runImport(outputPath);
                } finally {
                    Files.delete(outputPath);
                }

            } finally {
                Files.delete(inputPath);
            }

        } finally {
            Files.delete(tmpDir);
        }
    }

    private void runExport(Path exportPath) throws Exception {
        try (OutputStream os = new FileOutputStream(exportPath.toFile())) {
            ExportEdgeListJob exportEdgeListJob = new ExportEdgeListJob(metaGraph, jobId, graph, os);
            exportEdgeListJob.call();
        }
    }

    private void runSnap(Path inputFile, Path outputFile) throws Exception {
        String cmd = getSnapCommand(inputFile, outputFile);

        logger.debug("running: " + cmd);

        Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});

        int exitStatus = p.waitFor();

        logger.debug("snap finished with ", exitStatus);

        if (exitStatus != 0) {
            String stdout = IOUtils.toString(p.getInputStream());
            String stderr = IOUtils.toString(p.getErrorStream());

            throw new Exception("Snap process failed: [" + exitStatus + "]:\n" + stdout + "\n" + stderr);
        }
    }

    protected abstract String getSnapCommand(Path inputFile, Path outputFile);

    private void runImport(Path outputFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(outputFile.toFile()));
        String line;

        while ((line = br.readLine()) != null) {
            // skip over any comments.
            if (line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\t");
            importLine(parts);
        }
    }

    protected abstract void importLine(String[] parts);
}
