
<%--
    Document   : emptyLayout
    Created on : Sep 7, 2012, 10:26:46 PM
    Author     : kramachandran
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>JSP Page</title>
    <tiles:useAttribute id="list" name="css_js_list" classname="java.util.List"  />

    <c:forEach var="item" items="${list}">

        <tiles:insertAttribute value="${item}" flush="true" />

    </c:forEach>

</head>
<body>

<div class="contain-fluid">


    <div class="menu row-fluid">

        <tiles:insertAttribute name="menu" ignore="true"/>
    </div>
    <div class="header row-fluid">
        <tiles:insertAttribute name="header" />
    </div>
    <div class="body row-fluid">
        <tiles:insertAttribute name="body" />
    </div>


    <footer>
        <p>&copy; InQTel 2012</p>
    </footer>
</div>
</body>
</html>