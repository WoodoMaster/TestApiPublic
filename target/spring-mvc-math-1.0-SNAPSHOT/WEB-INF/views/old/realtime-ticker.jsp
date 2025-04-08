<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<html>
<head>
    <title>Real-time Ticker Information</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/realtime-ticker.css">
</head>
<body>
<div class="container">
    <h1>Real-time Ticker Information</h1>

    <div class="form-group">
        <label for="symbol">Select Symbol:</label>
        <c:choose>
            <c:when test="${not empty symbols}">
                <select id="symbol" required>
                    <option value="">-- Select Symbol --</option>
                    <c:forEach var="sym" items="${symbols}">
                        <option value="${sym}">${sym}</option>
                    </c:forEach>
                </select>
                <button id="subscribeBtn">Subscribe</button>
                <button id="unsubscribeBtn" disabled>Unsubscribe</button>
            </c:when>
            <c:otherwise>
                <p class="error">Failed to load symbols list. Please refresh the page or check server logs.</p>
                <c:if test="${not empty symbolsError}">
                    <p class="error">${symbolsError}</p>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- Ticker Data Container --%>
    <div id="tickerData" class="ticker-data-container" style="display: none;"> <%-- Hide initially --%>
        <div class="price-change-container">
            <%-- Price Info remains --%>
            <div class="price-info">
                <h2 id="currentSymbol"></h2>
                <div class="price">Last Price: <span id="lastPrice">---</span></div>
                <div class="change">24h Change: <span id="priceChange">---</span></div>
                <div class="percent-change">24h Change (%): <span id="priceChangePercent">---</span></div>
            </div>

            <%-- NEW: Min/Max/Current Summary Table --%>
            <div class="min-max-current-container">
                <h3>Session Price Summary</h3>
                <table class="min-max-table">
                    <thead>
                    <tr>
                        <th>Metric</th>
                        <th>Value</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>Current</td>
                        <td><span id="summaryCurrentPrice">---</span></td>
                    </tr>
                    <tr>
                        <td>Min (Session)</td>
                        <td><span id="summaryMinPrice">---</span></td>
                    </tr>
                    <tr>
                        <td>Max (Session)</td>
                        <td><span id="summaryMaxPrice">---</span></td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <%-- Detailed Ticker Table remains --%>
        <table class="ticker-details">
            <thead>
            <tr>
                <th>Parameter</th>
                <th>Value</th>
            </tr>
            </thead>
            <tbody id="tickerDetails">
            <tr><td colspan="2">Waiting for data...</td></tr>
            </tbody>
        </table>
    </div>

    <div id="errorContainer" class="error" style="display: none;">
        <h2>Error</h2>
        <p id="errorMessage"></p>
    </div>
</div>

<script>
    let socket;
    let currentSymbol = '';
    // Variables to track min/max price during the current subscription session
    let sessionMinPrice = null;
    let sessionMaxPrice = null;

    // --- FIX: Declare UI element variables here (outside DOMContentLoaded) ---
    let symbolSelect;
    let subscribeBtn;
    let unsubscribeBtn;
    let tickerDataContainer;
    let errorContainer;
    let tickerDetailsBody;
    // --- End FIX ---


    document.addEventListener('DOMContentLoaded', function() {
        // --- FIX: Assign the variables inside DOMContentLoaded ---
        symbolSelect = document.getElementById('symbol');
        subscribeBtn = document.getElementById('subscribeBtn');
        unsubscribeBtn = document.getElementById('unsubscribeBtn');
        tickerDataContainer = document.getElementById('tickerData');
        errorContainer = document.getElementById('errorContainer');
        tickerDetailsBody = document.getElementById('tickerDetails');
        // --- End FIX ---


        // Safety check in case the elements aren't found (e.g., if symbols failed to load)
        if (!symbolSelect || !subscribeBtn || !unsubscribeBtn) {
            console.error("Crucial UI elements not found. Subscription buttons disabled.");
            showError("Initialization error: UI elements missing. Cannot subscribe.");
            if(subscribeBtn) subscribeBtn.disabled = true;
            if(unsubscribeBtn) unsubscribeBtn.disabled = true;
            return; // Stop further execution if elements are missing
        }

        subscribeBtn.addEventListener('click', function() {
            const symbol = symbolSelect.value;
            if (!symbol) {
                showError('Please select a symbol');
                return;
            }

            currentSymbol = symbol;
            // Reset session min/max for new subscription
            sessionMinPrice = null;
            sessionMaxPrice = null;
            document.getElementById('summaryCurrentPrice').textContent = '---';
            document.getElementById('summaryMinPrice').textContent = '---';
            document.getElementById('summaryMaxPrice').textContent = '---';

            // Reset other UI elements
            tickerDetailsBody.innerHTML = '<tr><td colspan="2">Subscribing...</td></tr>';
            document.getElementById('currentSymbol').textContent = symbol;
            document.getElementById('lastPrice').textContent = '---';
            document.getElementById('priceChange').textContent = '---';
            document.getElementById('priceChangePercent').innerHTML = '---';

            subscribeToTicker(symbol);
            symbolSelect.disabled = true;
            subscribeBtn.disabled = true;
            unsubscribeBtn.disabled = false;
            tickerDataContainer.style.display = 'block';
            errorContainer.style.display = 'none';
        });

        unsubscribeBtn.addEventListener('click', function() {
            if (socket) {
                socket.close();
                console.log("WebSocket closed by user."); // Added log
            } else {
                console.log("Unsubscribe clicked, but no active socket."); // Added log
            }
            symbolSelect.disabled = false;
            subscribeBtn.disabled = false;
            unsubscribeBtn.disabled = true;
            tickerDataContainer.style.display = 'none';
            currentSymbol = '';
            // Reset min/max (optional, will be reset on next subscribe anyway)
            sessionMinPrice = null;
            sessionMaxPrice = null;
            document.getElementById('currentSymbol').textContent = '';
            // Check if tickerDetailsBody exists before updating
            if (tickerDetailsBody) {
                tickerDetailsBody.innerHTML = '<tr><td colspan="2">Unsubscribed</td></tr>';
            }
        });
    });

    function subscribeToTicker(symbol) {
        if (socket && socket.readyState !== WebSocket.CLOSED) {
            console.log('Closing existing WebSocket connection before new subscription.');
            socket.close();
        }
        const wsProtocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
        const contextPath = '${pageContext.request.contextPath}';
        // Ensure contextPath logic is robust, especially if context path is root "/"
        const baseWsUrl = wsProtocol + window.location.host + (contextPath && contextPath !== '/' ? contextPath : '');
        const wsUrl = (window.location.protocol === 'https:' ? 'wss://' : 'ws://')
            + window.location.host
            + '/ApiTest1_war/ws/ticker';


        console.log('Connecting to WebSocket:', wsUrl);
        socket = new WebSocket(wsUrl);

        socket.onopen = function() {
            console.log('WebSocket connection established');
            console.log('Sending symbol:', symbol);
            // Ensure socket is open before sending
            if (socket.readyState === WebSocket.OPEN) {
                socket.send(symbol);
            } else {
                console.error("Socket not open when trying to send symbol.");
                showError("Failed to send subscription request.");
                // Optionally trigger unsubscribe logic here
                if(unsubscribeBtn) unsubscribeBtn.click();
            }
        };

        socket.onmessage = function(event) {
            console.log('WebSocket message received:', event.data);
            try {
                const data = JSON.parse(event.data);

                if (data.error) {
                    showError(data.error);
                    // Check if unsubscribeBtn exists before clicking
                    if(unsubscribeBtn) unsubscribeBtn.click();
                    return;
                }

                // Ensure data is for the currently subscribed symbol, or handle general updates if needed
                if ((data.symbol && data.symbol === currentSymbol) || !data.symbol) { // Be careful with !data.symbol, ensure it's expected
                    updateTickerData(data);
                } else {
                    console.log("Received data for different symbol, ignoring:", data.symbol);
                }

            } catch (e) {
                console.error('Error parsing WebSocket message:', e, "Data:", event.data);
                showError('Error processing data from server');
                // Consider closing the connection if data is consistently bad
                // if(unsubscribeBtn) unsubscribeBtn.click();
            }
        };

        socket.onclose = function(event) {
            // --- FIX: Now has access to UI variables via outer scope ---
            console.log('WebSocket connection closed:', event.code, event.reason);
            // Only show error and reset UI if the close was *not* initiated by the user clicking unsubscribe
            // Check if unsubscribeBtn exists and check its disabled state
            if(unsubscribeBtn && !unsubscribeBtn.disabled) {
                showError(`WebSocket connection closed unexpectedly (Code: ${event.code}). Please try subscribing again.`);
                // Check if elements exist before manipulating
                if (symbolSelect) symbolSelect.disabled = false;
                if (subscribeBtn) subscribeBtn.disabled = false;
                if (unsubscribeBtn) unsubscribeBtn.disabled = true; // Already closed, disable unsubscribe
                if (tickerDataContainer) tickerDataContainer.style.display = 'none';
                currentSymbol = '';
                sessionMinPrice = null; // Reset on close
                sessionMaxPrice = null;
            } else {
                console.log("WebSocket closed normally (likely initiated by user unsubscribe).");
            }
        };

        socket.onerror = function(error) {
            // --- FIX: Now has access to UI variables via outer scope ---
            console.error('WebSocket error:', error);
            showError('WebSocket connection error. Check console for details.');
            // Check if elements exist before manipulating
            if (symbolSelect) symbolSelect.disabled = false;
            if (subscribeBtn) subscribeBtn.disabled = false;
            if (unsubscribeBtn) unsubscribeBtn.disabled = true;
            if (tickerDataContainer) tickerDataContainer.style.display = 'none';
            currentSymbol = '';
            sessionMinPrice = null; // Reset on error
            sessionMaxPrice = null;
            // Ensure socket is properly closed if an error occurs
            if (socket && socket.readyState !== WebSocket.CLOSED) {
                socket.close();
            }
        };
    }

    function updateTickerData(data) {
        // Check if required elements exist before updating
        const lastPriceEl = document.getElementById('lastPrice');
        const priceChangeEl = document.getElementById('priceChange');
        const priceChangePercentEl = document.getElementById('priceChangePercent');
        const summaryCurrentPriceEl = document.getElementById('summaryCurrentPrice');
        const summaryMinPriceEl = document.getElementById('summaryMinPrice');
        const summaryMaxPriceEl = document.getElementById('summaryMaxPrice');

        if (!lastPriceEl || !priceChangeEl || !priceChangePercentEl ||
            !summaryCurrentPriceEl || !summaryMinPriceEl || !summaryMaxPriceEl ||
            !tickerDetailsBody) {
            console.error("Ticker UI elements missing, cannot update data.");
            return;
        }


        // Update main price display
        lastPriceEl.textContent = formatNumber(parseFloat(data.lastPrice));
        priceChangeEl.textContent = formatNumber(parseFloat(data.priceChange));
        priceChangePercentEl.innerHTML = formatPercent(parseFloat(data.priceChangePercent));

        // --- Update Min/Max/Current Summary ---
        const currentPrice = parseFloat(data.lastPrice);
        if (!isNaN(currentPrice)) {
            // Update Current Price in the summary table
            summaryCurrentPriceEl.textContent = formatNumber(currentPrice);
            // Update Session Min Price
            if (sessionMinPrice === null || currentPrice < sessionMinPrice) {
                sessionMinPrice = currentPrice;
                summaryMinPriceEl.textContent = formatNumber(sessionMinPrice);
            }
            // Update Session Max Price
            if (sessionMaxPrice === null || currentPrice > sessionMaxPrice) {
                sessionMaxPrice = currentPrice;
                summaryMaxPriceEl.textContent = formatNumber(sessionMaxPrice);
            }
        } else {
            console.warn("Received invalid lastPrice for summary:", data.lastPrice);
            // Optionally clear or show '---' if price is invalid
            // summaryCurrentPriceEl.textContent = '---';
            return; // Don't update details table if price is fundamentally wrong
        }
        // --- End Summary Update ---

        // --- Update details table (remains mostly the same) ---
        tickerDetailsBody.innerHTML = ''; // Clear previous details

        const displayOrder = [
            'symbol', 'openPrice', 'highPrice', 'lowPrice', 'lastPrice',
            'volume', 'quoteVolume', 'priceChange', 'priceChangePercent',
            'weightedAvgPrice', 'prevClosePrice', 'lastQty', 'bidPrice', 'askPrice',
            'openTime', 'closeTime'
        ];

        const addedKeys = new Set();
        displayOrder.forEach(key => {
            if (data.hasOwnProperty(key)) {
                addRow(tickerDetailsBody, key, data[key]);
                addedKeys.add(key);
            }
        });

        // Add any remaining keys not in the display order
        for (const [key, value] of Object.entries(data)) {
            if (!addedKeys.has(key)) {
                addRow(tickerDetailsBody, key, value);
            }
        }
        // --- End Details Table Update ---
    }

    // addRow function remains the same as before
    function addRow(tableBody, key, value) {
        const row = document.createElement('tr');

        const paramCell = document.createElement('td');
        paramCell.textContent = formatKeyName(key);
        row.appendChild(paramCell);

        const valueCell = document.createElement('td');
        let displayValue = value;

        // Attempt to format based on key name conventions or value type
        if (key.toLowerCase().includes('time') && typeof value === 'number' && value > 1000000000) { // Basic check for timestamp
            try {
                displayValue = new Date(value).toLocaleString();
            } catch (e) {
                console.warn("Could not format timestamp:", value);
                displayValue = value; // Fallback to original value
            }
        } else if (key === 'priceChangePercent') {
            valueCell.innerHTML = formatPercent(parseFloat(value));
            row.appendChild(valueCell);
            tableBody.appendChild(row);
            return; // Already appended
        } else if (typeof value === 'string' && !isNaN(parseFloat(value)) && (key.toLowerCase().includes('price') || key.toLowerCase().includes('qty') || key.toLowerCase().includes('volume') || key.toLowerCase().includes('change'))) {
            // Format strings that look like numbers, especially for price/qty/volume
            displayValue = formatNumber(parseFloat(value));
        } else if (typeof value === 'number') {
            displayValue = formatNumber(value);
        } else if (value === null || value === undefined) {
            displayValue = '---';
        }
        // Add boolean handling if needed:
        // else if (typeof value === 'boolean') {
        //     displayValue = value ? 'Yes' : 'No';
        // }

        valueCell.textContent = String(displayValue); // Ensure it's a string
        row.appendChild(valueCell);
        tableBody.appendChild(row);
    }

    // formatKeyName function remains the same
    function formatKeyName(key) {
        // Handle potential snake_case or camelCase to Title Case
        return key
            .replace(/([A-Z])/g, ' $1') // Add space before capital letters
            .replace(/_/g, ' ')       // Replace underscores with spaces
            .replace(/^./, str => str.toUpperCase()); // Capitalize the first letter
    }

    // formatNumber function remains the same
    function formatNumber(value) {
        if (value === null || value === undefined || isNaN(value)) return '---';
        // Dynamic formatting based on value magnitude
        let options = {
            minimumFractionDigits: 2,
            maximumFractionDigits: 8 // Default max
        };
        const absValue = Math.abs(value);

        if (absValue === 0) {
            options.maximumFractionDigits = 2;
        } else if (absValue > 10000) {
            options.maximumFractionDigits = 2; // Large numbers
        } else if (absValue >= 1) {
            options.maximumFractionDigits = 4; // Medium numbers
        } else if (absValue > 0.0001) {
            options.maximumFractionDigits = 6; // Small decimals
        } else {
            options.maximumFractionDigits = 8; // Very small decimals
        }

        // Avoid unnecessary trailing zeros beyond minimumFractionDigits
        const formatted = new Intl.NumberFormat('en-US', options).format(value);
        // Optional: Trim trailing zeros if they go beyond minFractionDigits, but this is complex with Intl.NumberFormat
        return formatted;
    }


    // formatPercent function remains the same
    function formatPercent(value) {
        if (value === null || value === undefined || isNaN(value)) return '--- <span class="neutral">%</span>'; // Added neutral class
        const formattedValue = (value / 100); // Convert percentage value (e.g., 1.49) to decimal (0.0149) for Intl formatting
        const formattedString = new Intl.NumberFormat('en-US', {
            style: 'percent',
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
            signDisplay: 'exceptZero' // Show sign for non-zero values
        }).format(formattedValue);

        const span = document.createElement('span');
        span.textContent = formattedString;
        // Add class based on value
        if (value > 0) {
            span.className = 'positive';
        } else if (value < 0) {
            span.className = 'negative';
        } else {
            span.className = 'neutral'; // Class for zero change
        }
        return span.outerHTML;
    }

    // showError function remains the same
    function showError(message) {
        // Check if errorContainer and errorMessage exist
        const localErrorContainer = document.getElementById('errorContainer');
        const localErrorMessage = document.getElementById('errorMessage');
        const localTickerDataContainer = document.getElementById('tickerData');

        if (localErrorMessage && localErrorContainer) {
            localErrorMessage.textContent = message;
            localErrorContainer.style.display = 'block';
        } else {
            console.error("Error container not found, logging error:", message); // Fallback
        }

        // Hide ticker data if error occurs
        if(localTickerDataContainer) {
            localTickerDataContainer.style.display = 'none';
        }
    }
</script>
</body>
</html>