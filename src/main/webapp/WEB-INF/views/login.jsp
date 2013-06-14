<%--
  Created by IntelliJ IDEA.
  User: kramachandran
  Date: 6/13/13
  Time: 4:45 PM
  To change this template use File | Settings | File Templates.
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div class="container-fluid">
    <div class="row-fluid">
        <div class="hero-unit span8 offset2" >
            <p>Please fill out the following form to sign in to your Message Reader account.</p>

            <form method="post" class="form-horizontal" action="/spring/resources/j_spring_security_check">
                <fieldset>
                    <legend>
                        Account information
                    </legend>
                    <div class="control-group">
                        <label class="control-label" for="login">Login</label>
                        <div class="controls">
                            <input type="text" class="input" id="login" name="j_username" value="${login}"/>
                        </div>
                    </div>
                    <div class="control-group">
                        <label class="control-label" for="password">Password</label>
                        <div class="controls">
                            <input type="password" class="input" id="password" name="j_password"
                                   value="${password}"/>
                        </div>
                    </div>
                    <c:if test="${fn:length(error) gt 0}">
                        <div class="alert alert-error">${error}</div>
                    </c:if>
                    <div>
                        <input type="submit" class="btn btn-primary" value="Sign in"/>
                        <a href="/" class="btn">Cancel</a>
                    </div>
                </fieldset>
            </form>
        </div>
    </div>
</div>