package org.lab41.rexster;

import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.config.GraphConfigurationContainer;
import com.tinkerpop.rexster.config.GraphConfigurationException;
import com.tinkerpop.rexster.server.AbstractMapRexsterApplication;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

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
    private static XMLConfiguration configurationProperties;


    @Autowired(required = true)
    public DendriteRexsterApplication(@Value("${rexster_xml}") String pathToXML, ResourceLoader resouceLoader) {

        logger.debug("Path to XML: " + pathToXML);
        Resource resource = resouceLoader.getResource(pathToXML);

        if (resource == null || !resource.exists()) {
            throw new RuntimeException("Unable to initialize RexsterApplicaton, Rexster xml configuration file is either null or does not exist. ");

        } else {
            configurationProperties = new XMLConfiguration();
            try {
                configurationProperties.load(resource.getInputStream());
            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "Could not load %s properties file. Message: %s", pathToXML, e.getMessage()), e);
            }

            try {
                final List<HierarchicalConfiguration> graphConfigs = configurationProperties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
                final GraphConfigurationContainer container = new GraphConfigurationContainer(graphConfigs);
                final Map<String, RexsterApplicationGraph> configuredGraphs = container.getApplicationGraphs();
                graphs.putAll(configuredGraphs);

            } catch (GraphConfigurationException e) {
                logger.error("Graph initialization fialed. Check rexster.xml", e);

                //Should failuere to intializat the graphs result in a shut down of the whole system.
                throw new RuntimeException("Graph Initialization failed");
            }
        }

    }

    // Begin Cut and Paste: TODO - Refactor to extend XmlRexsterApplication -------------------------------


}
