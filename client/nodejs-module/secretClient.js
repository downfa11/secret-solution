// secretClient.js
const axios = require('axios');

class SecretClient {
    constructor(baseURL, token) {
        this.baseURL = baseURL;
        this.headers = token ? { Authorization: `Bearer ${token}` } : {};
    }

    async saveRaw(namespace, key, value) {
        const res = await axios.post(`${this.baseURL}/api/secrets/raw`, { namespace, key, value }, { headers: this.headers });
        return res.data;
    }

    async saveEncrypted(namespace, key, value) {
        const res = await axios.post(`${this.baseURL}/api/secrets/encrypted`, { namespace, key, value }, { headers: this.headers });
        return res.data;
    }

    async getRaw(namespace, key) {
        const res = await axios.get(`${this.baseURL}/api/secrets/raw/${namespace}/${key}`, { headers: this.headers });
        return res.data;
    }

    async getDecrypted(namespace, key) {
        const res = await axios.get(`${this.baseURL}/api/secrets/decrypted/${namespace}/${key}`, { headers: this.headers });
        return res.data;
    }
}

module.exports = SecretClient;