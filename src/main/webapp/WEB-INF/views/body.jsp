<%--
  Created by IntelliJ IDEA.
  User: kramachandran
  Date: 6/13/13
  Time: 4:32 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@page contentType="text/html;charset=UTF-8"%>

<ul class="menu">
    <li><a href="#/view1">view1</a></li>
    <li><a href="#/view2">view2</a></li>
</ul>

<div ng-view></div>

<div>Angular seed app: v<span app-version></span></div>