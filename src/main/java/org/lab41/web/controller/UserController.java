package org.lab41.web.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: etryzelaar Date: 8/13/13 Time: 6:58 PM To change this template
 * use File | Settings | File Templates.
 */
@Controller
public class UserController {

    @RequestMapping(value = "/api/user", method = RequestMethod.GET)
    public @ResponseBody Map<String,Object> userInformation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("name", authentication.getName());
        response.put("authorities", authentication.getAuthorities());

        return response;
    }
}
