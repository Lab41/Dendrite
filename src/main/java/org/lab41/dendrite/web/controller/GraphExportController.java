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

package org.lab41.dendrite.web.controller;

import com.tinkerpop.blueprints.util.io.gml.GMLWriter;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONWriter;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.DendriteGraphTx;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.services.HistoryService;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.requests.GraphExportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class GraphExportController extends AbstractController {

    static Logger logger = LoggerFactory.getLogger(GraphExportController.class);

    @Autowired
    MetaGraphService metaGraphService;

    @Autowired
    HistoryService historyService;

    @PreAuthorize("hasPermission(#graphId, 'graph', 'admin')")
    @RequestMapping(value = "/api/graphs/{graphId}/file-export", method = RequestMethod.POST)
    public ResponseEntity<byte[]> export(@PathVariable String graphId,
                                         @Valid GraphExportRequest item,
                                         BindingResult result) {

        if (result.hasErrors()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        DendriteGraph graph = metaGraphService.getDendriteGraph(graphId);
        if (graph == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        logger.debug("exporting graph '" + graphId + "'");

        String format = item.getFormat();
        HttpHeaders headers = new HttpHeaders();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        DendriteGraphTx tx = graph.buildTransaction().readOnly().start();

        try {
            if (format.equalsIgnoreCase("GraphSON")) {
                headers.setContentType(new MediaType("application", "vnd.rexster+json"));
                headers.set("Content-Disposition", "attachment; filename=\"graph.json\"");

                GraphSONWriter.outputGraph(tx, byteArrayOutputStream);
            } else if (format.equalsIgnoreCase("GraphML")) {
                headers.setContentType(new MediaType("application", "vnd.rexster+xml"));
                headers.set("Content-Disposition", "attachment; filename=\"graph.xml\"");

                GraphMLWriter.outputGraph(tx, byteArrayOutputStream);
            } else if (format.equalsIgnoreCase("GML")) {
                headers.setContentType(new MediaType("application", "vnd.rexster+gml"));
                headers.set("Content-Disposition", "attachment; filename=\"graph.gml\"");

                GMLWriter.outputGraph(tx, byteArrayOutputStream);
            } else {
                tx.rollback();

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            tx.rollback();

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        tx.commit();

        return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
    }

    @PreAuthorize("hasPermission(#graphId, 'graph', 'admin')")
    @RequestMapping(value = "/api/graphs/{graphId}/file-save", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> save(@PathVariable String graphId,
                                                    @Valid GraphExportRequest item,
                                                    BindingResult result) throws IOException, GitAPIException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();

        if (result.hasErrors()) {
            response.put("status", "error");
            response.put("msg", result.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        MetaGraphTx metaGraphTx = metaGraphService.buildTransaction().readOnly().start();
        GraphMetadata graphMetadata;
        Git git;

        try {
            graphMetadata = metaGraphTx.getGraph(graphId);
            git = historyService.projectGitRepository(graphMetadata.getProject());
            metaGraphTx.commit();
        } catch (Throwable t) {
            metaGraphTx.rollback();
            throw t;
        }

        DendriteGraph graph = metaGraphService.getDendriteGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "cannot find graph '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        logger.debug("saving graph '" + graphId + "'");

        // extract the storage location for the history
        String format = item.getFormat();

        DendriteGraphTx tx = graph.buildTransaction().readOnly().start();

        try {
            try {
                String path;

                if (format.equalsIgnoreCase("GraphSON")) {
                    path = new File(git.getRepository().getWorkTree(), graphId + ".json").getPath();
                    GraphSONWriter.outputGraph(tx, path);
                } else if (format.equalsIgnoreCase("GraphML")) {
                    path = new File(git.getRepository().getWorkTree(), graphId + ".xml").getPath();
                    GraphMLWriter.outputGraph(tx, path);
                } else if (format.equalsIgnoreCase("GML")) {
                    path = new File(git.getRepository().getWorkTree(), graphId + ".gml").getPath();
                    GMLWriter.outputGraph(tx, path);
                } else {
                    tx.rollback();

                    response.put("status", "error");
                    response.put("msg", "unknown format '" + format + "'");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                git.add()
                        .addFilepattern(".")
                        .call();

                git.commit()
                        .setAuthor(authentication.getName(), "")
                        .setMessage("commit")
                        .call();
            } finally {
                git.close();
            }
        } catch (IOException e) {
            tx.rollback();

            response.put("status", "error");
            response.put("msg", "exception: " + e.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        tx.commit();

        response.put("status", "ok");

        return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
    }
}
