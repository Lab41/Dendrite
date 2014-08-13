package org.lab41.dendrite.web.security;

import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.*;
import org.lab41.dendrite.services.MetaGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * Created by kramachandran on 8/4/14.
 */
@Service
public class GraphPermissonHandler implements PermissionEvaluator {
    Logger logger = LoggerFactory.getLogger("GraphPermissionHandler");

    @Autowired
    MetaGraphService metaGraphService;

    private boolean checkUserForBranch(Authentication authentication, Serializable targetId) {
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();
        boolean authenticated;

        try {
            BranchMetadata branchMetadata = tx.getBranch((String) targetId);
            if (branchMetadata == null) {
                return false;
            }

            ProjectMetadata projectMetadata = branchMetadata.getProject();
            if (projectMetadata == null) {
                return false;
            }

            authenticated = checkAuthAgainstUserOfProject(tx, authentication, projectMetadata);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        return authenticated;
    }

    private boolean checkAuthAgainstUserOfProject(MetaGraphTx tx, Authentication authentication, ProjectMetadata projectMetadata) {
        UserMetadata currentUser = tx.getOrCreateUser(authentication);

        for (UserMetadata user : projectMetadata.getUsers()) {
            if (user.equals(currentUser)) {
                return true;
            }
        }
        return false;
    }

    boolean checkUserForProject(Authentication authentication, Serializable targetId) {
        MetaGraphTx tx = metaGraphService.buildTransaction().start();
        boolean authenticated;

        try {
            ProjectMetadata projectMetadata = tx.getProject((String) targetId);
            if (projectMetadata == null) {
                return false;
            }

            authenticated = checkAuthAgainstUserOfProject(tx, authentication, projectMetadata);
        } catch (Throwable t){
            tx.rollback();
            throw t;
        }

        tx.commit();

        return authenticated;
    }

    boolean checkUserForGraph(Authentication authentication, Serializable targetId) {
        MetaGraphTx tx = metaGraphService.buildTransaction().start();
        boolean authenticated;

        try {
            GraphMetadata graphMetadata = tx.getGraph((String) targetId);
            if (graphMetadata == null) {
                return false;
            }

            ProjectMetadata projectMetadata = graphMetadata.getProject();
            if (projectMetadata == null) {
                return false;
            }

            authenticated = checkAuthAgainstUserOfProject(tx, authentication, projectMetadata);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        return authenticated;
    }

    boolean checkUserForJob(Authentication authentication, Serializable targetId) {
        MetaGraphTx tx = metaGraphService.buildTransaction().start();
        boolean authenticated;

        try {
            JobMetadata jobMetadata = tx.getJob((String) targetId);
            if (jobMetadata == null) {
                return false;
            }

            ProjectMetadata projectMetadata = jobMetadata.getProject();
            if (projectMetadata == null) {
                return false;
            }

            authenticated = checkAuthAgainstUserOfProject(tx, authentication, projectMetadata);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        return authenticated;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        //TODO fill this out
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        boolean retVal;

        switch(targetType) {
            case "project":
                retVal = checkUserForProject(authentication, targetId);
                break;
            case "graph":
                retVal = checkUserForGraph(authentication, targetId);
                break;
            case "branch":
                retVal = checkUserForBranch(authentication, targetId);
                break;
            case "job":
                retVal = checkUserForJob(authentication, targetId);
                break;
            default:
                retVal = false;
        }
        return retVal;
    }
}
