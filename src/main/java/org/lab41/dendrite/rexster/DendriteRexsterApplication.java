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

import com.google.common.base.Preconditions;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.config.GraphConfigurationContainer;
import com.tinkerpop.rexster.config.GraphConfigurationException;
import com.tinkerpop.rexster.protocol.EngineConfiguration;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.server.AbstractMapRexsterApplication;
import com.tinkerpop.rexster.server.RexsterProperties;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initial cut at a "dendrite" version  of  {@link com.tinkerpop.rexster.server.XmlRexsterApplication}
 * <p/>
 * Cut and pasted from an older version of the XmlRexsterApplication. Does not have the dynamic graph reloading
 * capablities.
 *
 * @author kramachandran
 */
@Service
public class DendriteRexsterApplication extends AbstractMapRexsterApplication {

    Logger logger = LoggerFactory.getLogger(DendriteRexsterApplication.class);

    private RexsterProperties properties;
    private Map<String, HierarchicalConfiguration> configMap;

    @Autowired(required = true)
    public DendriteRexsterApplication(@Value("${rexster_xml}") String pathToXML, ResourceLoader resourceLoader) {

        logger.debug("Path to XML: " + pathToXML);
        Resource resource = resourceLoader.getResource(pathToXML);

        if (resource == null || !resource.exists()) {
            throw new RuntimeException("Unable to initialize RexsterApplicaton, Rexster xml configuration file is either null or does not exist. ");

        } else {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();

            try {
                xmlConfiguration.load(resource.getInputStream());
            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "Could not load %s properties file. Message: %s", pathToXML, e.getMessage()), e);
            }

            properties = new RexsterProperties(xmlConfiguration);
            final List<HierarchicalConfiguration> graphConfigs = properties.getGraphConfigurations();
            final GraphConfigurationContainer container;

            try {
                container = new GraphConfigurationContainer(graphConfigs);
            } catch (GraphConfigurationException e) {
                logger.error("GraphMetadata initialization fialed. Check rexster.xml", e);

                // Failure to initialize the graphs should result in shutting down the whole system.
                throw new RuntimeException("GraphMetadata Initialization failed");
            }

            graphs.putAll(container.getApplicationGraphs());

            // There is unfortunately no way to look up the config for a graph, which makes a connection to Faunus
            // difficult. So instead we'll cache all the configs so we can look things up.
            configMap = new HashMap<String, HierarchicalConfiguration>();

            for(Object graphConfig: graphConfigs) {
                HierarchicalConfiguration config = (HierarchicalConfiguration) graphConfig;
                String name = config.getString(Tokens.REXSTER_GRAPH_NAME, "");
                Preconditions.checkArgument(!name.isEmpty());

                configMap.put(name, config);
            }
        }

        configureScriptEngine();
    }

    private void configureScriptEngine() {
        // the EngineController needs to be configured statically before requests start serving so that it can
        // properly construct ScriptEngine objects with the correct reset policy. allow scriptengines to be
        // configured so that folks can drop in different gremlin flavors.
        final List<EngineConfiguration> configuredScriptEngines = new ArrayList<EngineConfiguration>();
        final List<HierarchicalConfiguration> configs = this.properties.getScriptEngines();
        for(HierarchicalConfiguration config : configs) {
            configuredScriptEngines.add(new EngineConfiguration(config));
        }

        EngineController.configure(configuredScriptEngines);
    }

    public HierarchicalConfiguration getConfig(String graphName) {
        return configMap.get(graphName);
    }

    public HierarchicalConfiguration getStorageConfig(String graphName) {
        return getConfig(graphName).configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
        //.configurationAt(GraphDatabaseConfiguration.STORAGE_NAMESPACE);
    }

    // Begin Cut and Paste: TODO - Refactor to extend XmlRexsterApplication -------------------------------

}
