<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Account Info</title>
    <style>
        body { font-family: Arial; background-color: #f4f4f4; padding: 20px; }
        h2 { color: #333; }
        input, button { margin: 5px 0; padding: 8px; width: 300px; }
        .result { margin-top: 20px; padding: 10px; background: #fff; border: 1px solid #ccc; }
    </style>
</head>
<body>

<h2>Получение информации об аккаунте</h2>
<form method="post" action="account-info">
    <label>API Key:</label><br>
    <input type="text" name="apiKey" required/><br>
    <label>Secret Key:</label><br>
    <input type="text" name="secretKey" required/><br>
    <button type="submit">Получить данные</button>
</form>

<c:if test="${not empty payload}">
    <div class="result">
        <p><strong>Статус:</strong> ${status}</p>
        <p><strong>Можно торговать:</strong> ${payload.canTrade}</p>
        <p><strong>Можно вносить средства:</strong> ${payload.canDeposit}</p>
        <p><strong>Можно выводить:</strong> ${payload.canWithdraw}</p>
        <h3>Балансы:</h3>
        <ul>
            <c:forEach var="balance" items="${payload.balances}">
                <li>${balance.asset}: ${balance.free} (Заблокировано: ${balance.locked})</li>
            </c:forEach>
        </ul>
    </div>
</c:if>

</body>
</html>