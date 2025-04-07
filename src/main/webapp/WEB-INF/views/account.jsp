<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/accInfo.css">
<head>
    <meta charset="UTF-8">
    <title>Account Info</title>
</head>
<body>
<div class="container">
    <h1>Получить информацию об аккаунте</h1>
<form method="post">
    <label>API Key:</label><br>
    <input type="text" name="apiKey" required/><br>
    <label>Secret Key:</label><br>
    <input type="text" name="secretKey" required/><br>
    <button type="submit">Получить данные</button>
</form>
    <c:if test="${not empty error}">
        <div class="error">${error}</div>
    </c:if>

    <c:if test="${not empty payload}">
        <div class="payload">
            <h2>Информация об аккаунте:</h2>
            <ul>
                <c:forEach var="entry" items="${payload}">
                    <li><strong>${entry.key}</strong>: ${entry.value}</li>
                </c:forEach>
            </ul>
        </div>
    </c:if>
</div>
</body>
</html>