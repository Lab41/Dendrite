package org.lab41.dendrite.web.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

/**
 * Created by kramachandran on 8/4/14.
 */
public class GraphPermissonHandler implements PermissionEvaluator{
    Logger logger = LoggerFactory.getLogger("GraphPermissionHandler");
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}
