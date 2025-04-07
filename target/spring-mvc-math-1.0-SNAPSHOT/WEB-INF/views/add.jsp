<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Addition</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/style.css">
</head>
<body>
<h1>Addition</h1>
<form method="post">
    <input type="number" name="num1" step="any" required>
    <input type="number" name="num2" step="any" required>
    <button type="submit">Add</button>
</form>
<c:if test="${not empty result}">
    <p>Result: ${result}</p>
</c:if>
<a href="${pageContext.request.contextPath}/">Home</a>
</body>
</html>