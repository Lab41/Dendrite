/**
 * Copyright 2013 In-Q-Tel/Lab41
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

package org.lab41.dendrite.rexster;

import com.codahale.metrics.MetricRegistry;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.protocol.EngineConfiguration;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.server.RexsterApplication;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.lab41.dendrite.graph.DendriteGraph;
import org.lab41.dendrite.graph.DendriteGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DendriteRexsterApplication implements RexsterApplication {

    Logger logger = LoggerFactory.getLogger(DendriteRexsterApplication.class);

    private MetricRegistry metricRegistry;

    private final long startTime = System.currentTimeMillis();

    private DendriteGraphFactory graphFactory;

    @Autowired
    public DendriteRexsterApplication(DendriteGraphFactory graphFactory) {
        this.graphFactory = graphFactory;

        configureScriptEngine();
    }

    @Override
    public Graph getGraph(String id) {
        return graphFactory.getGraph(id);
    }

    @Override
    public RexsterApplicationGraph getApplicationGraph(String id) {
        DendriteGraph graph = graphFactory.getGraph(id);
        if (graph == null) {
            return null;
        }
        return graph.getRexsterGraph();
    }

    @Override
    public Set<String> getGraphNames() {
        return graphFactory.getGraphNames();
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        if (metricRegistry == null) {
            metricRegistry = new MetricRegistry();
        }

        return metricRegistry;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public void stop() {
        graphFactory.stop();
    }

    private void configureScriptEngine() {
        // the EngineController needs to be configured statically before requests start serving so that it can
        // properly construct ScriptEngine objects with the correct reset policy. allow script engines to be
        // configured so that folks can drop in different gremlin flavors.

        HierarchicalConfiguration config = new HierarchicalConfiguration();

        config.setProperty("name", "gremlin-groovy");
        config.setProperty("reset-threshold", "-1");

        List<String> imports = new ArrayList<>();
        imports.add("com.tinkerpop.gremlin.*");
        imports.add("com.tinkerpop.gremlin.java.*");
        imports.add("com.tinkerpop.gremlin.pipes.filter.*");
        imports.add("com.tinkerpop.gremlin.pipes.sideeffect.*");
        imports.add("com.tinkerpop.gremlin.pipes.transform.*");
        imports.add("com.tinkerpop.blueprints.*");
        imports.add("com.tinkerpop.blueprints.impls.*");
        imports.add("com.tinkerpop.blueprints.impls.tg.*");
        imports.add("com.tinkerpop.blueprints.impls.neo4j.*");
        imports.add("com.tinkerpop.blueprints.impls.neo4j.batch.*");
        imports.add("com.tinkerpop.blueprints.impls.orient.*");
        imports.add("com.tinkerpop.blueprints.impls.orient.batch.*");
        imports.add("com.tinkerpop.blueprints.impls.dex.*");
        imports.add("com.tinkerpop.blueprints.impls.rexster.*");
        imports.add("com.tinkerpop.blueprints.impls.sail.*");
        imports.add("com.tinkerpop.blueprints.impls.sail.impls.*");
        imports.add("com.tinkerpop.blueprints.util.*");
        imports.add("com.tinkerpop.blueprints.util.io.*");
        imports.add("com.tinkerpop.blueprints.util.io.gml.*");
        imports.add("com.tinkerpop.blueprints.util.io.graphml.*");
        imports.add("com.tinkerpop.blueprints.util.io.graphson.*");
        imports.add("com.tinkerpop.blueprints.util.wrappers.*");
        imports.add("com.tinkerpop.blueprints.util.wrappers.batch.*");
        imports.add("com.tinkerpop.blueprints.util.wrappers.batch.cache.*");
        imports.add("com.tinkerpop.blueprints.util.wrappers.event.*");
        imports.add("com.tinkerpop.blueprints.util.wrappers.event.listener.*");
        imports.add("com.tinkerpop.blueprints.util.wrappers.id.*");
        imports.add("com.tinkerpop.blueprints.util.wrappers.partition.*");
        imports.add("com.tinkerpop.blueprints.util.wrappers.readonly.*");
        imports.add("com.tinkerpop.blueprints.oupls.sail.*");
        imports.add("com.tinkerpop.blueprints.oupls.sail.pg.*");
        imports.add("com.tinkerpop.blueprints.oupls.jung.*");
        imports.add("com.tinkerpop.pipes.*");
        imports.add("com.tinkerpop.pipes.branch.*");
        imports.add("com.tinkerpop.pipes.filter.*");
        imports.add("com.tinkerpop.pipes.sideeffect.*");
        imports.add("com.tinkerpop.pipes.transform.*");
        imports.add("com.tinkerpop.pipes.util.*");
        imports.add("com.tinkerpop.pipes.util.iterators.*");
        imports.add("com.tinkerpop.pipes.util.structures.*");
        imports.add("org.apache.commons.configuration.*");
        imports.add("com.thinkaurelius.titan.core.*");
        imports.add("com.thinkaurelius.titan.core.attribute.*");
        imports.add("com.thinkaurelius.titan.core.util.*");
        imports.add("com.thinkaurelius.titan.example.*");
        imports.add("org.apache.commons.configuration.*");
        imports.add("com.tinkerpop.gremlin.Tokens.T");
        imports.add("com.tinkerpop.gremlin.groovy.*");
        config.setProperty("imports", imports);

        List<String> staticImports = new ArrayList<>();
        staticImports.add("com.tinkerpop.blueprints.Direction.*");
        staticImports.add("com.tinkerpop.blueprints.TransactionalGraph$Conclusion.*");
        staticImports.add("com.tinkerpop.blueprints.Compare.*");
        staticImports.add("com.thinkaurelius.titan.core.attribute.Geo.*");
        staticImports.add("com.thinkaurelius.titan.core.attribute.Text.*");
        staticImports.add("com.thinkaurelius.titan.core.TypeMaker$UniquenessConsistency.*");
        staticImports.add("com.tinkerpop.blueprints.Query$Compare.*");
        config.setProperty("static-imports", staticImports);

        EngineConfiguration engineConfiguration = new EngineConfiguration(config);

        List<EngineConfiguration> engineConfigurations = new ArrayList<>();
        engineConfigurations.add(engineConfiguration);

        EngineController.configure(engineConfigurations);
    }
}
