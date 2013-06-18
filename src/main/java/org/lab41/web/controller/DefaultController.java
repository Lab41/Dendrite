/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lab41.web.controller;

import java.util.ArrayList;
import java.util.List;
import org.lab41.titan.GraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author etryzelaar
 */
@Controller
public class DefaultController {

    @Autowired
    private GraphService graphService;

    @RequestMapping("/items")
    public @ResponseBody List<String> getItems() {
        List<String> items = new ArrayList();
        return items;
    }

}