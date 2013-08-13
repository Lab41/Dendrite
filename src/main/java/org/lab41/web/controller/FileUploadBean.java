package org.lab41.web.controller;

import org.springframework.web.multipart.commons.CommonsMultipartFile;

/**
 * Created with IntelliJ IDEA. User: etryzelaar Date: 8/12/13 Time: 5:55 PM To change this template
 * use File | Settings | File Templates.
 */
public class FileUploadBean {
    private String format;
    private CommonsMultipartFile file;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public CommonsMultipartFile getFile() {
        return file;
    }

    public void setFile(CommonsMultipartFile file) {
        this.file = file;
    }
}
