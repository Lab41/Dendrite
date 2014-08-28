package org.lab41.dendrite.jobs;

import com.google.common.base.Preconditions;
import com.google.common.collect.UnmodifiableIterator;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BranchCommitSubsetJob extends AbstractGraphCommitJob {

    static Logger logger = LoggerFactory.getLogger(BranchCommitSubsetJob.class);

    static int SIZE = 1000;

    String query;
    int steps;

    Map<Object, Vertex> vertices = new HashMap<>();
    Set<Object> edges = new HashSet<>();

    public BranchCommitSubsetJob(MetaGraph metaGraph,
                                 JobMetadata.Id jobId,
                                 ProjectMetadata.Id projectId,
                                 BranchMetadata.Id branchId,
                                 GraphMetadata.Id srcGraphId,
                                 GraphMetadata.Id dstGraphId,
                                 String query, int steps) {
        super(metaGraph, jobId, projectId, branchId, srcGraphId, dstGraphId);

        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(steps >= 0);

        this.query = query;
        this.steps = steps;
    }

    @Override
    protected void copyGraph(DendriteGraph srcGraph, DendriteGraph dstGraph) {
        // Make sure the source graph's elasticsearch is ready.
        srcGraph.getElasticSearchClient().admin().cluster().prepareHealth()
                .setWaitForYellowStatus()
                .execute().actionGet();

        // Make sure Elasticsearch is up to date.
        srcGraph.getElasticSearchClient().admin().indices().prepareRefresh()
                .execute().actionGet();

        // Build our ElasticSearch query.
        Client client = srcGraph.getElasticSearchClient();
        Preconditions.checkNotNull(client);

        QueryBuilder queryBuilder;

        if (query.isEmpty()) {
            queryBuilder = QueryBuilders.matchAllQuery();
        } else {
            queryBuilder = QueryBuilders.queryString(query);
        }

        SearchRequestBuilder srb = client.prepareSearch(srcGraph.getIndexName())
                .setTypes("vertex")
                .setQuery(queryBuilder)
                .setSize(SIZE)
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000));

        TitanTransaction srcTx = srcGraph.newTransaction();

        try {
            TitanTransaction dstTx = dstGraph.newTransaction();

            try {
                for (SearchHit hit: scan(srcGraph.getElasticSearchClient(), srb)) {
                    Vertex vertex = srcTx.getVertex(hit.getId());
                    copyVertex(srcTx, dstTx, vertex, 0);
                }
            } catch (Exception e) {
                dstTx.rollback();
                throw e;
            }

            dstTx.commit();
        } catch (Exception e) {
            srcTx.rollback();
            throw e;
        }

        srcTx.commit();

        // Be nice and make sure that the new ES indices has been fully created.
        dstGraph.getElasticSearchClient().admin().indices().prepareRefresh()
                .execute().actionGet();
    }

    private Vertex copyVertex(TitanTransaction srcTx, TitanTransaction dstTx, Vertex srcVertex, int step) {
        Vertex dstVertex;

        if (vertices.containsKey(srcVertex.getId())) {
            dstVertex = vertices.get(srcVertex.getId());
        } else {
            dstVertex = dstTx.addVertex(srcVertex.getId());
            vertices.put(srcVertex.getId(), dstVertex);

            copyProperties(srcVertex, dstVertex);
        }

        step += 1;

        if (step <= steps) {
            for (Edge srcEdge: srcVertex.getEdges(Direction.BOTH)) {
                copyEdge(srcTx, dstTx, srcEdge, step);
            }
        }

        return dstVertex;
    }

    private void copyEdge(TitanTransaction srcTx, TitanTransaction dstTx, Edge srcEdge, int step) {
        Vertex srcInVertex = srcEdge.getVertex(Direction.IN);
        Vertex srcOutVertex = srcEdge.getVertex(Direction.OUT);

        Vertex dstInVertex = copyVertex(srcTx, dstTx, srcInVertex, step);
        Vertex dstOutVertex = copyVertex(srcTx, dstTx, srcOutVertex, step);

        if (!edges.contains(srcEdge.getId())) {
            edges.add(srcEdge.getId());

            Edge dstEdge = dstTx.addEdge(
                    srcEdge.getId(),
                    dstOutVertex,
                    dstInVertex,
                    srcEdge.getLabel());

            copyProperties(srcEdge, dstEdge);
        }
    }

    private void copyProperties(Element src, Element dst) {
        for (String key: src.getPropertyKeys()) {
            if (!key.equals("_id")) {
                dst.setProperty(key, src.getProperty(key));
            }
        }
    }

    private Iterable<SearchHit> scan(final Client client, final SearchRequestBuilder srb) {
        return new Iterable<SearchHit>() {
            @Override
            public Iterator<SearchHit> iterator() {
                return new ScanIterator(client, srb);
            }
        };
    }

    private class ScanIterator extends UnmodifiableIterator<SearchHit> {
        Client client;
        SearchRequestBuilder srb;
        SearchResponse response;
        Iterator<SearchHit> iterator;

        public ScanIterator(Client client, SearchRequestBuilder srb) {
            this.client = client;
            this.srb = srb;
            this.response = srb.execute().actionGet();
            logger.debug("Executed scan in {} ms", response.getTookInMillis());
            this.iterator = response.getHits().iterator();
        }

        @Override
        public boolean hasNext() {
            if (iterator.hasNext()) {
                return true;
            }

            response = client.prepareSearchScroll(response.getScrollId())
                    .setScroll(srb.request().scroll())
                    .execute().actionGet();

            logger.debug("Executed scan in {} ms", response.getTookInMillis());

            iterator = response.getHits().iterator();

            return iterator.hasNext();
        }

        @Override
        public SearchHit next() {
            return iterator.next();
        }
    }
}
