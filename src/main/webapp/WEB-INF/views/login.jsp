<%--
  Created by IntelliJ IDEA.
  User: kramachandran
  Date: 7/6/13
  Time: 11:28 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
</head>
<body>

<html><head><title>Login Page</title></head>
<body onload='document.f.j_username.focus();'>
<h3>Login with Username and Password</h3>

<form name='f' action='j_spring_security_check' method='POST'>
    <table>
        <tr><td>User:</td><td><input type='text' name='j_username' value=''></td></tr>
        <tr><td>Password:</td><td><input type='password' name='j_password'/></td></tr>
        <tr><td colspan='2'><input name="submit" type="submit" value="Login"/></td></tr>
    </table>
</form></body></html>
</body>
</html>