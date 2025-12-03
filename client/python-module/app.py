from flask import Flask, request, render_template_string
from secret_client import SecretClient

app = Flask(__name__)

client = SecretClient("http://localhost:8080", token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiYWxpY2UiLCJleHAiOjE3NTg5MDExODZ9.t1rn6cYUeMANPbIEfnCIhH-qIqXD5beEkrYIBrDR3VY"
                         )

HTML_TEMPLATE = """
<!DOCTYPE html>
<html>
<head>
    <title>Secret Demo</title>
</head>
<body>
    <h1>Secret Demo App</h1>
    <h2>Save Raw Secret</h2>
    <form method="post" action="/save_raw_ui">
        Namespace: <input type="text" name="namespace"><br>
        Key: <input type="text" name="key"><br>
        Value: <input type="text" name="value"><br>
        <button type="submit">Save Raw</button>
    </form>

    <h2>Get Raw Secret</h2>
    <form method="get" action="/get_raw_ui">
        Namespace: <input type="text" name="namespace"><br>
        Key: <input type="text" name="key"><br>
        <button type="submit">Get Raw</button>
    </form>

    <h2>Save Encrypted Secret</h2>
    <form method="post" action="/save_encrypted_ui">
        Namespace: <input type="text" name="namespace"><br>
        Key: <input type="text" name="key"><br>
        Value: <input type="text" name="value"><br>
        <button type="submit">Save Encrypted</button>
    </form>

    <h2>Get Decrypted Secret</h2>
    <form method="get" action="/get_decrypted_ui">
        Namespace: <input type="text" name="namespace"><br>
        Key: <input type="text" name="key"><br>
        <button type="submit">Get Decrypted</button>
    </form>

    {% if result %}
    <h3>Result:</h3>
    <pre>{{ result }}</pre>
    {% endif %}
</body>
</html>
"""

@app.route("/", methods=["GET"])
def index():
    return render_template_string(HTML_TEMPLATE)

@app.route("/save_raw_ui", methods=["POST"])
def save_raw_ui():
    namespace = request.form["namespace"]
    key = request.form["key"]
    value = request.form["value"]
    try:
        result = client.save_raw(namespace, key, value)
    except Exception as e:
        result = str(e)
    return render_template_string(HTML_TEMPLATE, result=result)

@app.route("/get_raw_ui", methods=["GET"])
def get_raw_ui():
    namespace = request.args["namespace"]
    key = request.args["key"]
    try:
        result = client.get_raw(namespace, key)
    except Exception as e:
        result = str(e)
    return render_template_string(HTML_TEMPLATE, result=result)

@app.route("/save_encrypted_ui", methods=["POST"])
def save_encrypted_ui():
    namespace = request.form["namespace"]
    key = request.form["key"]
    value = request.form["value"]
    try:
        result = client.save_encrypted(namespace, key, value)
    except Exception as e:
        result = str(e)
    return render_template_string(HTML_TEMPLATE, result=result)

@app.route("/get_decrypted_ui", methods=["GET"])
def get_decrypted_ui():
    namespace = request.args["namespace"]
    key = request.args["key"]
    try:
        result = client.get_decrypted(namespace, key)
    except Exception as e:
        result = str(e)
    return render_template_string(HTML_TEMPLATE, result=result)

if __name__ == "__main__":
    app.run(debug=True, port=5000)