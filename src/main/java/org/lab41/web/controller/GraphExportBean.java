package org.lab41.web.controller;

import org.springframework.web.multipart.commons.CommonsMultipartFile;

/**
 * Created with IntelliJ IDEA. User: etryzelaar Date: 8/12/13 Time: 5:55 PM To change this template
 * use File | Settings | File Templates.
 */
public class GraphExportBean {
    private String format;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
