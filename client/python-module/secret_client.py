import requests

class SecretClient:
    def __init__(self, base_url: str, token: str = None):
        self.base_url = base_url.rstrip("/")
        self.headers = {"Authorization": f"Bearer {token}"} if token else {}

    def save_raw(self, namespace: str, key: str, value: str):
        url = f"{self.base_url}/api/secrets/raw"
        payload = {"namespace": namespace, "key": key, "value": value}
        resp = requests.post(url, json=payload, headers=self.headers)
        resp.raise_for_status()
        return resp.json()

    def save_encrypted(self, namespace: str, key: str, value: str):
        url = f"{self.base_url}/api/secrets/encrypted"
        payload = {"namespace": namespace, "key": key, "value": value}
        resp = requests.post(url, json=payload, headers=self.headers)
        resp.raise_for_status()
        return resp.json()

    def get_raw(self, namespace: str, key: str):
        url = f"{self.base_url}/api/secrets/raw/{namespace}/{key}"
        resp = requests.get(url, headers=self.headers)
        resp.raise_for_status()
        return resp.json()

    def get_decrypted(self, namespace: str, key: str):
        url = f"{self.base_url}/api/secrets/decrypted/{namespace}/{key}"
        resp = requests.get(url, headers=self.headers)
        resp.raise_for_status()
        return resp.json()