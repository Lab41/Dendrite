package org.lab41.dendrite.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;

import java.io.IOException;

@Service
public class HistoryService {

    private static Logger logger = LoggerFactory.getLogger(HistoryService.class);

    private String historyStorage;

    @Autowired(required = true)
    public HistoryService(@Value("${history.properties}") String pathToProperties, ResourceLoader resourceLoader) throws IOException, ConfigurationException {

        logger.debug("Path to Properties: " + pathToProperties);

        // load configuration
        Resource resource = resourceLoader.getResource(pathToProperties);
        Configuration configuration = new PropertiesConfiguration(resource.getFile());

        // create directory
        historyStorage = configuration.getString("history.storage");

        // Make sure the directory exists.
        new File(historyStorage).mkdirs();
    }

    public Git projectGitRepository(ProjectMetadata projectMetadata) throws GitAPIException, IOException {
        File gitDir = new File(historyStorage, projectMetadata.getId().toString());

        // Make the target directory.
        Git git;
        if (gitDir.exists() && new File(gitDir, ".git").exists()) {
            git = Git.open(gitDir);
        } else {
            logger.debug("Creating git repository: %s", gitDir);

            gitDir.mkdirs();

            git = Git.init()
                    .setDirectory(gitDir)
                    .setBare(false)
                    .call();

            git.commit()
                    .setAuthor("", "")
                    .setMessage("commit")
                    .call();
        }

        return git;
    }
}
