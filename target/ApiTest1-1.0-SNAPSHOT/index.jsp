<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>API Controllers Comparison</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .container { max-width: 800px; margin: 0 auto; }
        h1 { color: #2c3e50; }
        .controller-list { list-style: none; padding: 0; }
        .controller-item { margin: 15px 0; padding: 15px;
            border: 1px solid #ddd; border-radius: 5px; }
        .controller-item a { text-decoration: none; color: #3498db;
            font-size: 1.1em; }
        .controller-item:hover { background-color: #f9f9f9; }
        .description { color: #7f8c8d; font-size: 0.9em; margin-top: 5px; }
    </style>
</head>
<body>
<div class="container">
    <h1>Select Controller Implementation</h1>
    <ul class="controller-list">
        <li class="controller-item">
            <a href="/account-info-gpt">Basic Servlet Implementation</a>
            <div class="description">
                GPT realization
            </div>
        </li>
        <li class="controller-item">
            <a href="/jsp-controller">JSP + Servlet Implementation</a>
            <div class="description">
                Separation of concerns with JSP for view rendering
            </div>
        </li>
        <li class="controller-item">
            <a href="/rest-controller">REST API Implementation</a>
            <div class="description">
                Modern RESTful approach with JSON responses
            </div>
        </li>
        <li class="controller-item">
            <a href="/spring-controller">Spring MVC Implementation</a>
            <div class="description">
                Spring Framework MVC with annotation-based configuration
            </div>
        </li>
    </ul>
</div>
</body>
</html>
