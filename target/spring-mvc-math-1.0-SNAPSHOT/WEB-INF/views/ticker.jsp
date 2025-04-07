<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<html>
<head>
    <title>Информация о тикере</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/ticker.css">
</head>
<body>

<div class="container">
    <h1>Информация о тикере (24ч)</h1>
    <form method="POST" action="${pageContext.request.contextPath}/ticker">
        <div class="form-group">
            <label for="symbol">Выберите актив:</label>
            <c:choose>
                <c:when test="${not empty symbols}">
                    <select id="symbol" name="symbol" required>
                        <option value="">-- Выберите символ --</option>
                        <c:forEach var="sym" items="${symbols}">
                            <option value="${sym}" ${sym == selectedSymbol ? 'selected' : ''}>${sym}</option>
                        </c:forEach>
                    </select>
                    <button type="submit">Получить данные</button>
                </c:when>
                <c:otherwise>
                    <p class="error">Не удалось загрузить список символов. Обновите страницу или проверьте логи сервера.</p>
                    <c:if test="${not empty symbolsError}">
                        <p class="error">${symbolsError}</p>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>
    </form>

    <c:if test="${not empty tickerError}">
        <div class="error">
            <h2>Ошибка</h2>
            <p>${tickerError}</p>
        </div>
    </c:if>

    <c:if test="${not empty tickerData}">
             <h2>Данные для ${selectedSymbol}</h2>
        <table class="ticker-data">
            <thead>
            <tr>
                <th>Параметр</th>
                <th>Значение</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="entry" items="${tickerData}">
                <tr>
                    <td>${entry.key}</td>
                    <td>
                        <c:choose>
                            <c:when test="${entry.key == 'priceChangePercent'}">
                                <c:set var="percentValue"><fmt:parseNumber type="number" value="${entry.value}" /></c:set>
                                <span class="${percentValue >= 0 ? 'positive' : 'negative'}">
                                        <fmt:formatNumber value="${percentValue}" type="percent" minFractionDigits="2" maxFractionDigits="2"/>
                                     </span>
                            </c:when>
                            <c:when test="${entry.key == 'priceChange'}">
                                <c:set var="changeValue"><fmt:parseNumber type="number" value="${entry.value}" /></c:set>
                                <span class="${changeValue >= 0 ? 'positive' : 'negative'}">
                                         <fmt:formatNumber value="${changeValue}" type="number" minFractionDigits="2" maxFractionDigits="8"/>
                                      </span>
                            </c:when>
                            <c:otherwise>
                                ${entry.value}
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

</div>

</body>
</html>
