package secret

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"io"
)

type AesEncryptor interface {
	Encrypt(plainText string) (string, error)
	Decrypt(cipherText string) (string, error)
}

type aesEncryptor struct {
	key []byte
}

func NewAesEncryptor(key string) (AesEncryptor, error) {
	if len(key) != 32 {
		return nil, fmt.Errorf("key must be exactly 32 bytes for AES-256. current length: %d", len(key))
	}
	return &aesEncryptor{key: []byte(key)}, nil
}

func (e *aesEncryptor) Encrypt(plainText string) (string, error) {
	c, err := aes.NewCipher(e.key)
	if err != nil {
		return "", err
	}
	gcm, err := cipher.NewGCM(c)
	if err != nil {
		return "", err
	}
	nonce := make([]byte, gcm.NonceSize())
	if _, err = io.ReadFull(rand.Reader, nonce); err != nil {
		return "", err
	}
	cipherText := gcm.Seal(nonce, nonce, []byte(plainText), nil)
	return hex.EncodeToString(cipherText), nil
}

func (e *aesEncryptor) Decrypt(cipherText string) (string, error) {
	data, err := hex.DecodeString(cipherText)
	if err != nil {
		return "", err
	}
	c, err := aes.NewCipher(e.key)
	if err != nil {
		return "", err
	}
	gcm, err := cipher.NewGCM(c)
	if err != nil {
		return "", err
	}
	nonceSize := gcm.NonceSize()
	if len(data) < nonceSize {
		return "", fmt.Errorf("ciphertext too short")
	}
	nonce, cipherData := data[:nonceSize], data[nonceSize:]
	plainText, err := gcm.Open(nil, nonce, cipherData, nil)
	if err != nil {
		return "", err
	}
	return string(plainText), nil
}
