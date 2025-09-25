package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"html/template"
	"io/ioutil"
	"log"
	"net/http"
)

type SecretClient struct {
	BaseURL string
	Token   string
}

func NewSecretClient(baseURL, token string) *SecretClient {
	return &SecretClient{
		BaseURL: baseURL,
		Token:   token,
	}
}

func (c *SecretClient) doRequest(method, path string, body interface{}) ([]byte, error) {
	url := c.BaseURL + path
	var buf bytes.Buffer
	if body != nil {
		if err := json.NewEncoder(&buf).Encode(body); err != nil {
			return nil, err
		}
	}
	req, _ := http.NewRequest(method, url, &buf)
	req.Header.Set("Content-Type", "application/json")
	if c.Token != "" {
		req.Header.Set("Authorization", "Bearer "+c.Token)
	}

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 400 {
		data, _ := ioutil.ReadAll(resp.Body)
		return nil, fmt.Errorf("%s", string(data))
	}
	return ioutil.ReadAll(resp.Body)
}

func (c *SecretClient) SaveRaw(namespace, key, value string) (map[string]interface{}, error) {
	data := map[string]string{"namespace": namespace, "key": key, "value": value}
	resp, err := c.doRequest("POST", "/api/secrets/raw", data)
	if err != nil {
		return nil, err
	}
	var result map[string]interface{}
	_ = json.Unmarshal(resp, &result)
	return result, nil
}

func (c *SecretClient) SaveEncrypted(namespace, key, value string) (map[string]interface{}, error) {
	data := map[string]string{"namespace": namespace, "key": key, "value": value}
	resp, err := c.doRequest("POST", "/api/secrets/encrypted", data)
	if err != nil {
		return nil, err
	}
	var result map[string]interface{}
	_ = json.Unmarshal(resp, &result)
	return result, nil
}

func (c *SecretClient) GetRaw(namespace, key string) (map[string]interface{}, error) {
	resp, err := c.doRequest("GET", fmt.Sprintf("/api/secrets/raw/%s/%s", namespace, key), nil)
	if err != nil {
		return nil, err
	}
	var result map[string]interface{}
	_ = json.Unmarshal(resp, &result)
	return result, nil
}

func (c *SecretClient) GetDecrypted(namespace, key string) (map[string]interface{}, error) {
	resp, err := c.doRequest("GET", fmt.Sprintf("/api/secrets/decrypted/%s/%s", namespace, key), nil)
	if err != nil {
		return nil, err
	}
	var result map[string]interface{}
	_ = json.Unmarshal(resp, &result)
	return result, nil
}

// ------------------ 웹서버 ------------------
var htmlTpl = `
<!DOCTYPE html>
<html>
<head><title>Secret Demo</title></head>
<body>
<h1>Secret Demo</h1>

<h2>Save Raw</h2>
<form method="post" action="/save_raw">
Namespace: <input name="namespace"><br>
Key: <input name="key"><br>
Value: <input name="value"><br>
<button type="submit">Save</button>
</form>

<h2>Get Raw</h2>
<form method="get" action="/get_raw">
Namespace: <input name="namespace"><br>
Key: <input name="key"><br>
<button type="submit">Get</button>
</form>

<h2>Save Encrypted</h2>
<form method="post" action="/save_encrypted">
Namespace: <input name="namespace"><br>
Key: <input name="key"><br>
Value: <input name="value"><br>
<button type="submit">Save</button>
</form>

<h2>Get Decrypted</h2>
<form method="get" action="/get_decrypted">
Namespace: <input name="namespace"><br>
Key: <input name="key"><br>
<button type="submit">Get</button>
</form>

{{if .Result}}
<h3>Result:</h3>
<pre>{{.Result}}</pre>
{{end}}

</body>
</html>
`

func main() {
	client := NewSecretClient("http://localhost:8080", "dummy-token")
	tpl := template.Must(template.New("index").Parse(htmlTpl))

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		tpl.Execute(w, nil)
	})

	http.HandleFunc("/save_raw", func(w http.ResponseWriter, r *http.Request) {
		if r.Method == "POST" {
			r.ParseForm()
			res, err := client.SaveRaw(r.FormValue("namespace"), r.FormValue("key"), r.FormValue("value"))
			data := map[string]interface{}{"Result": res}
			if err != nil {
				data["Result"] = err.Error()
			}
			tpl.Execute(w, data)
		}
	})

	http.HandleFunc("/get_raw", func(w http.ResponseWriter, r *http.Request) {
		ns := r.URL.Query().Get("namespace")
		key := r.URL.Query().Get("key")
		res, err := client.GetRaw(ns, key)
		data := map[string]interface{}{"Result": res}
		if err != nil {
			data["Result"] = err.Error()
		}
		tpl.Execute(w, data)
	})

	http.HandleFunc("/save_encrypted", func(w http.ResponseWriter, r *http.Request) {
		if r.Method == "POST" {
			r.ParseForm()
			res, err := client.SaveEncrypted(r.FormValue("namespace"), r.FormValue("key"), r.FormValue("value"))
			data := map[string]interface{}{"Result": res}
			if err != nil {
				data["Result"] = err.Error()
			}
			tpl.Execute(w, data)
		}
	})

	http.HandleFunc("/get_decrypted", func(w http.ResponseWriter, r *http.Request) {
		ns := r.URL.Query().Get("namespace")
		key := r.URL.Query().Get("key")
		res, err := client.GetDecrypted(ns, key)
		data := map[string]interface{}{"Result": res}
		if err != nil {
			data["Result"] = err.Error()
		}
		tpl.Execute(w, data)
	})

	log.Println("Server running at http://localhost:5000")
	http.ListenAndServe(":5000", nil)
}
