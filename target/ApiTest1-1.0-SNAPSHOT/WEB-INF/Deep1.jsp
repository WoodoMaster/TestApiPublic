<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Account Information</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .form-container { margin-bottom: 30px; padding: 20px; border: 1px solid #ddd; border-radius: 5px; max-width: 500px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; }
        input[type="text"], input[type="password"] { width: 100%; padding: 8px; }
        button { background-color: #4CAF50; color: white; padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; }
        .error { color: red; margin-top: 10px; }
        .account-info { margin-top: 20px; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
<div class="form-container">
    <h2>Get Account Info</h2>
    <form action="account-servlet" method="POST">
        <div class="form-group">
            <label>API Key:</label>
            <input type="text" name="apiKey" required>
        </div>
        <div class="form-group">
            <label>Secret Key:</label>
            <input type="password" name="secretKey" required>
        </div>
        <div class="form-group">
            <label>
                <input type="checkbox" name="showZeroBalance" value="true"
                ${param.showZeroBalance ? 'checked' : ''}> Show Zero Balances
            </label>
        </div>
        <button type="submit">Get Account Info</button>
    </form>

    <c:if test="${not empty error}">
        <div class="error">Error: ${error}</div>
    </c:if>
</div>

<c:if test="${not empty accountInfo}">
    <div class="account-info">
        <h3>Account Details</h3>
        <p>Can Trade: ${accountInfo.canTrade}</p>
        <p>Can Withdraw: ${accountInfo.canWithdraw}</p>
        <p>Update Time: ${accountInfo.updateTime}</p>

        <h4>Balances</h4>
        <table>
            <tr>
                <th>Asset</th>
                <th>Free</th>
                <th>Locked</th>
                <th>Default</th>
            </tr>
            <c:forEach items="${accountInfo.balances}" var="balance">
                <c:if test="${balance.free > 0 || balance.locked > 0 || param.showZeroBalance}">
                    <tr>
                        <td>${balance.asset}</td>
                        <td>${balance.free}</td>
                        <td>${balance.locked}</td>
                        <td>${balance.defaultBalance ? 'Yes' : 'No'}</td>
                    </tr>
                </c:if>
            </c:forEach>
        </table>
    </div>
</c:if>
</body>
</html>
</html>