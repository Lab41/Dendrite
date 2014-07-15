package org.lab41.dendrite.web.beans;

import javax.validation.constraints.NotNull;
import java.util.Properties;

public class CreateGraphBean {
    @NotNull
    private Properties properties;

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
