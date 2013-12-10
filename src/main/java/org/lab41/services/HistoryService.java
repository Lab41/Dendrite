package org.lab41.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class HistoryService {

    Logger logger = LoggerFactory.getLogger(HistoryService.class);

    @Autowired(required = true)
    public HistoryService(@Value("${history.properties}") String pathToProperties, ResourceLoader resourceLoader) throws IOException, ConfigurationException {

        logger.debug("Path to Properties: " + pathToProperties);

        // load configuration
        Resource resource = resourceLoader.getResource(pathToProperties);
        Configuration configuration = new PropertiesConfiguration(resource.getFile());

        // create directory
        historyStorage = configuration.getString("history.storage");
        File file = new File(historyStorage);
        file.mkdirs();
    }

    public String getHistoryStorage() {
        return historyStorage;
    }

    private String historyStorage;
}
