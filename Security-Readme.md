# Dendrite - Security 

Dendrite uses Spring security in two ways : 
 
 1. At the controller level - it filters incoming requests to check and see if it should honor a request. 
 2. At the service level - it uses the pre-method invocation filter to check and see if the current user 
    has access to the graph in question by checking the ownership of project.
        