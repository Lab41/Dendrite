/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lab41.titan;

import com.thinkaurelius.titan.core.TitanFactory;
import com.tinkerpop.blueprints.Graph;
import org.springframework.stereotype.Service;

/**
 *
 * @author etryzelaar
 */
@Service("graphService")
public class GraphServiceImpl implements GraphService {
    private Graph graph;

    GraphServiceImpl() {
        graph = TitanFactory.open("berkeleyje-es.local");
    }
}
