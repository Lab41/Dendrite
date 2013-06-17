
<%--
    Document   : emptyLayout
    Created on : Sep 7, 2012, 10:26:46 PM
    Author     : kramachandran
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<!DOCTYPE html>
<html lang="en" ng-app="myApp">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title><tiles:getAsString name="title" /></title>
    <link rel="stylesheet" href="css/app.css" />
</head>
<body>

<tiles:insertAttribute name="body" />

<script src="webjars/angularjs/1.1.5/angular.min.js"></script>
<script src="js/app.js"></script>
<script src="js/services.js"></script>
<script src="js/controllers.js"></script>
<script src="js/filters.js"></script>
<script src="js/directives.js"></script>

</body>
</html>
