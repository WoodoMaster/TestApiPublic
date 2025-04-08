<%@page contentType="text/html; charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
    <title>Real-time Ticker & Trading</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/realtime-ticker.css"/>
</head>
<body>
<div class="container">
    <h1>Real-time Ticker & Trading</h1>
    <div class="form-group">
        <label for="symbol">Select Symbol:</label>
        <c:choose>
            <c:when test="${not empty symbols}">
                <select id="symbol">
                    <option value="">-- Select Symbol --</option>
                    <c:forEach var="s" items="${symbols}">
                        <option value="${s}" ${s==selectedSymbol?'selected':''}>${s}</option>
                    </c:forEach>
                </select>
                <button id="subscribeBtn">Subscribe</button>
                <button id="unsubscribeBtn" disabled>Unsubscribe</button>
            </c:when>
            <c:otherwise>
                <p class="error">Failed to load symbols list. Refresh page.</p>
                <c:if test="${not empty symbolsError}">
                    <p class="error">${symbolsError}</p>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
    <div id="tickerData" style="display:none;">
        <div>
            <h2 id="currentSymbol"></h2>
            <p>Last Price: <span id="lastPrice">---</span></p>
            <p>24h Change: <span id="priceChange">---</span></p>
            <p>24h Change (%): <span id="priceChangePercent">---</span></p>
        </div>
        <div>
            <h3>Session Summary</h3>
            <table>
                <tr><td>Current</td><td id="summaryCurrentPrice">---</td></tr>
                <tr><td>Min</td><td id="summaryMinPrice">---</td></tr>
                <tr><td>Max</td><td id="summaryMaxPrice">---</td></tr>
            </table>
        </div>
        <table>
            <thead><tr><th>Param</th><th>Value</th></tr></thead>
            <tbody id="tickerDetails">
            <tr><td colspan="2">Waiting for data...</td></tr>
            </tbody>
        </table>
    </div>
    <div id="errorContainer" class="error" style="display:none;">
        <p id="errorMessage"></p>
    </div>
    <div class="order-section">
        <h2>Place Order</h2>
        <c:if test="${not empty orderSuccessMessage}">
            <div class="order-message order-success">${orderSuccessMessage}</div>
        </c:if>
        <c:if test="${not empty orderErrorMessage}">
            <div class="order-message order-error">
                    ${orderErrorMessage}
                <c:if test="${not empty orderErrorCode}">(Code: ${orderErrorCode})</c:if>
            </div>
        </c:if>

        <div class="api-key-warning"><strong>Warning:</strong> Entering API keys here is insecure for real use. Don’t use real keys.</div>
        <div class="order-form-container">
            <c:forEach var="side" items="${['BUY','SELL']}">
                <form action="${pageContext.request.contextPath}/order/place" method="post" class="order-form">
                    <h3>${side} Order</h3>
                    <input type="hidden" name="symbol" id="${side}Symbol"/>
                    <input type="hidden" name="side" value="${side}"/>
                    <label>API Key:</label>
                    <input type="password" name="apiKey" required />
                    <label>Secret Key:</label>
                    <input type="password" name="secretKey" required />
                    <label>Order Type:</label>
                    <select name="type" id="${side}Type" required>
                        <option value="LIMIT">LIMIT</option>
                        <option value="MARKET">MARKET</option>
                    </select>
                    <label>Quantity:</label>
                    <input type="number" step="any" name="quantity" placeholder="e.g., 0.01" required />
                    <div class="price-input-group" id="${side}PriceGroup">
                        <label>Price (LIMIT only):</label>
                        <input type="number" step="any" name="price" id="${side}Price" placeholder="e.g., 50000.50"/>
                    </div>
                    <button type="submit" class="${side=='BUY' ? 'buy-button' : 'sell-button'}">Place ${side} Order</button>
                </form>
            </c:forEach>

        </div>
    </div>
</div>
<script>
    let socket, currentSymbol='', sessionMin=null, sessionMax=null;

    const byId = id => document.getElementById(id);
    const show = el => el && (el.style.display='block');
    const hide = el => el && (el.style.display='none');

    document.addEventListener('DOMContentLoaded', ()=>{
        const sel = byId('symbol'), btnSub=byId('subscribeBtn'), btnUnsub=byId('unsubscribeBtn'),
            tickerData=byId('tickerData'), errC=byId('errorContainer'), errM=byId('errorMessage');

        // toggle LIMIT price fields
        ['BUY','SELL'].forEach(side=>{
            const typeSel=byId(side+'Type'), priceGroup=byId(side+'PriceGroup'), priceInput=byId(side+'Price');
            const toggle = ()=> {
                if(typeSel.value==='LIMIT'){ priceGroup.style.display='block'; priceInput.required=true; }
                else{ priceGroup.style.display='none'; priceInput.required=false; priceInput.value=''; }
            };
            toggle();
            typeSel.onchange=toggle;
        });

        btnSub.onclick = ()=>{
            if(!sel.value) { showError('Select symbol'); return; }
            currentSymbol = sel.value;
            ['BUY','SELL'].forEach(side=> byId(side+'Symbol').value = currentSymbol);
            resetSession();
            subscribeTicker(currentSymbol);
            sel.disabled=true; btnSub.disabled=true; btnUnsub.disabled=false;
            show(tickerData); hide(errC);
        };

        btnUnsub.onclick=()=>{
            socket?.close();
            sel.disabled=false; btnSub.disabled=false; btnUnsub.disabled=true;
            hide(tickerData); currentSymbol=''; resetSession();
        };
    });

    function subscribeTicker(sym){
        if(socket) socket.close();
        let ctx='${pageContext.request.contextPath}'; if(ctx==='/')ctx='';
        const url = (location.protocol==='https:'?'wss':'ws')+'://'+location.host+ctx+'/ws/ticker';
        socket=new WebSocket(url);
        socket.onopen=()=>socket.send(sym);
        socket.onmessage=e=>{
            try{
                const d=JSON.parse(e.data);
                if(d.error){ showError(d.error); socket.close(); return; }
                if(!d.symbol || d.symbol===currentSymbol) updateTicker(d);
            }catch(err){ showError('Parse error'); console.log(err); }
        };
        socket.onclose=()=>resetUI();
        socket.onerror=()=>{showError('WebSocket error'); socket.close();}
    }

    function updateTicker(d){
        byId('lastPrice').textContent=fmtNum(+d.lastPrice);
        byId('priceChange').textContent=fmtNum(+d.priceChange);
        byId('priceChangePercent').innerHTML=fmtPerc(+d.priceChangePercent);
        // min/max/session updates
        const p=+d.lastPrice;
        byId('summaryCurrentPrice').textContent=fmtNum(p);
        if(sessionMin===null||p<sessionMin) sessionMin=p;
        if(sessionMax===null||p>sessionMax) sessionMax=p;
        byId('summaryMinPrice').textContent=fmtNum(sessionMin);
        byId('summaryMaxPrice').textContent=fmtNum(sessionMax);

        // update details
        const tbody=byId('tickerDetails');
        tbody.innerHTML='';
        Object.entries(d).forEach(([k,v])=>{
            let tr=document.createElement('tr'),
                td1=document.createElement('td'),td2=document.createElement('td');
            td1.textContent=fmtKey(k);
            if(k==='priceChangePercent') td2.innerHTML=fmtPerc(+v);
            else if(k.toLowerCase().includes('time') && !isNaN(v)) td2.textContent=new Date(+v).toLocaleString();
            else if(!isNaN(v)) td2.textContent=fmtNum(+v);
            else td2.textContent=String(v);
            tr.append(td1, td2); tbody.append(tr);
        })
    }

    function resetSession(){
        sessionMin=sessionMax=null;
        ['summaryCurrentPrice','summaryMinPrice','summaryMaxPrice'].forEach(id=>byId(id).textContent='---');
        byId('currentSymbol').textContent=currentSymbol||'';
        ['lastPrice','priceChange','priceChangePercent'].forEach(id=>byId(id).textContent='---');
        byId('tickerDetails').innerHTML='<tr><td colspan="2">Waiting for data...</td></tr>';
    }

    function resetUI(){
        const t=byId('tickerData');
        hide(t); currentSymbol=''; resetSession();
        const sel=byId('symbol');
        sel.disabled=false;
        byId('subscribeBtn').disabled=false;
        byId('unsubscribeBtn').disabled=true;
    }

    function showError(msg){
        const errC=byId('errorContainer');
        byId('errorMessage').textContent=msg;
        show(errC); hide(byId('tickerData'));
    }

    function fmtKey(k){ return k.replace(/([A-Z])/g,' $1').replace(/^./, m=>m.toUpperCase()); }

    function fmtNum(v){
        if(v===null||v===undefined||isNaN(v)) return '---';
        let opts={minimumFractionDigits:2,maximumFractionDigits:8};
        const abs=Math.abs(v);
        if(abs===0) opts.maximumFractionDigits=2;
        else if(abs>10000) opts.maximumFractionDigits=2;
        else if(abs>=1) opts.maximumFractionDigits=4;
        else if(abs>0.0001) opts.maximumFractionDigits=6;
        else opts.maximumFractionDigits=8;
        return new Intl.NumberFormat('en-US',opts).format(v);
    }

    function fmtPerc(v){
        if(v===null||v===undefined||isNaN(v)) return '---';
        const pct=(v/100);
        return new Intl.NumberFormat('en-US',{style:'percent',minimumFractionDigits:2,maximumFractionDigits:2,signDisplay:'exceptZero'}).format(pct);
    }
</script>
</body>
</html>