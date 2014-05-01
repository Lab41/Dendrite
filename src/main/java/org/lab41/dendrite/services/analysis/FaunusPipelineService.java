package org.lab41.dendrite.services.analysis;

import com.thinkaurelius.faunus.FaunusGraph;
import com.thinkaurelius.faunus.FaunusPipeline;
import com.thinkaurelius.faunus.mapreduce.FaunusJobControl;
import com.thinkaurelius.faunus.mapreduce.FaunusCompiler;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FaunusPipelineService extends AnalysisService {

    static Logger logger = LoggerFactory.getLogger(FaunusPipelineService.class);

    public void configureGraph(FaunusGraph faunusGraph, Path tmpDir, DendriteGraph graph) {
        org.apache.commons.configuration.Configuration config = graph.getConfiguration();

        faunusGraph.getConf().set("mapred.jar", "../faunus/target/faunus-0.4.2-dendrite-hadoop2-SNAPSHOT-job.jar");

        Configuration faunusConfig = faunusGraph.getConf();

        setProp(faunusConfig, "faunus.graph.input.titan.storage.backend", config.getString("storage.backend", null));
        setProp(faunusConfig, "faunus.graph.input.titan.storage.hostname", config.getString("storage.hostname", null));
        setProp(faunusConfig, "faunus.graph.input.titan.storage.port", config.getString("storage.port", null));
        setProp(faunusConfig, "faunus.graph.input.titan.storage.tablename", config.getString("storage.tablename", null));

        setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.backend", config.getString("storage.index.search.backend", null));
        setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.client-only", config.getString("storage.index.search.client-only", null));
        setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.cluster-name", config.getString("storage.index.search.cluster-name", null));
        setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.directory", config.getString("storage.index.search.directory", null));
        setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.hostname", config.getString("storage.index.search.hostname", null));
        setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.index-name", config.getString("storage.index.search.index-name", null));
        setProp(faunusConfig, "faunus.graph.input.titan.storage.index.search.local-mode", config.getString("storage.index.search.local-mode", null));
        faunusGraph.setSideEffectOutputFormat(TextOutputFormat.class);
        faunusGraph.setOutputLocation(tmpDir);
        faunusGraph.setOutputLocationOverwrite(true);

        setProp(faunusConfig, "faunus.graph.output.titan.storage.backend", config.getString("storage.backend", null));
        setProp(faunusConfig, "faunus.graph.output.titan.storage.hostname", config.getString("storage.hostname", null));
        setProp(faunusConfig, "faunus.graph.output.titan.storage.port", config.getString("storage.port", null));
        setProp(faunusConfig, "faunus.graph.output.titan.storage.tablename", config.getString("storage.tablename", null));
        faunusConfig.setBoolean("faunus.graph.output.titan.storage.read-only", false);
        faunusConfig.setBoolean("faunus.graph.output.titan.storage.batch-loading", false);

        // FIXME: https://github.com/thinkaurelius/faunus/issues/167. I would prefer to leave this off, but we end up tripping over an exception.
        //faunusConfig.setBoolean("faunus.graph.output.titan.infer-schema", false);
        faunusConfig.setBoolean("faunus.graph.output.titan.infer-schema", true);

        setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.backend", config.getString("storage.index.search.backend", null));
        setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.client-only", config.getString("storage.index.search.client-only", null));
        setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.cluster-name", config.getString("storage.index.search.cluster-name", null));
        setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.directory", config.getString("storage.index.search.directory", null));
        setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.hostname", config.getString("storage.index.search.hostname", null));
        setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.index-name", config.getString("storage.index.search.index-name", null));
        setProp(faunusConfig, "faunus.graph.output.titan.storage.index.search.local-mode", config.getString("storage.index.search.local-mode", null));

        faunusGraph.getConf().set("faunus.graph.output.blueprints.script-file", "dendrite/dendrite-import.groovy");
    }

    private void setProp(Configuration config, String key, String value) {
        if (value != null) {
            config.set(key, value);
        }
    }

}
