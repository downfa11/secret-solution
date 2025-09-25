// server.js
const express = require('express');
const bodyParser = require('body-parser');
const SecretClient = require('./secretClient');

const client = new SecretClient('http://localhost:8080', 'dummy-token');
const app = express();
app.use(bodyParser.urlencoded({ extended: true }));

const htmlTemplate = (result = '') => `
<!DOCTYPE html>
<html>
<body>
<h1>Secret Demo</h1>

<form method="post" action="/save_raw">
Namespace: <input name="namespace"><br>
Key: <input name="key"><br>
Value: <input name="value"><br>
<button type="submit">Save Raw</button>
</form>

<form method="get" action="/get_raw">
Namespace: <input name="namespace"><br>
Key: <input name="key"><br>
<button type="submit">Get Raw</button>
</form>

<form method="post" action="/save_encrypted">
Namespace: <input name="namespace"><br>
Key: <input name="key"><br>
Value: <input name="value"><br>
<button type="submit">Save Encrypted</button>
</form>

<form method="get" action="/get_decrypted">
Namespace: <input name="namespace"><br>
Key: <input name="key"><br>
<button type="submit">Get Decrypted</button>
</form>

${result ? `<pre>${JSON.stringify(result, null, 2)}</pre>` : ''}
</body>
</html>
`;

app.get('/', (req, res) => res.send(htmlTemplate()));

app.post('/save_raw', async (req, res) => {
    try {
        const data = await client.saveRaw(req.body.namespace, req.body.key, req.body.value);
        res.send(htmlTemplate(data));
    } catch (e) {
        res.send(htmlTemplate(e.message));
    }
});

app.get('/get_raw', async (req, res) => {
    try {
        const data = await client.getRaw(req.query.namespace, req.query.key);
        res.send(htmlTemplate(data));
    } catch (e) {
        res.send(htmlTemplate(e.message));
    }
});

app.post('/save_encrypted', async (req, res) => {
    try {
        const data = await client.saveEncrypted(req.body.namespace, req.body.key, req.body.value);
        res.send(htmlTemplate(data));
    } catch (e) {
        res.send(htmlTemplate(e.message));
    }
});

app.get('/get_decrypted', async (req, res) => {
    try {
        const data = await client.getDecrypted(req.query.namespace, req.query.key);
        res.send(htmlTemplate(data));
    } catch (e) {
        res.send(htmlTemplate(e.message));
    }
});

app.listen(5000, () => console.log('Server running on http://localhost:5000'));