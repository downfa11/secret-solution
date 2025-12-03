package token

import (
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

type claims struct {
	UserID string `json:"user_id"`
	jwt.RegisteredClaims
}

type JWTService struct {
	secretKey []byte
}

func NewJWTService(secret string) *JWTService {
	return &JWTService{
		secretKey: []byte(secret),
	}
}

func (s *JWTService) GenerateToken(userID string) (string, error) {
	expirationTime := time.Now().Add(24 * time.Hour) // 토큰 만료 시간 설정
	claims := &claims{
		UserID: userID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(expirationTime),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString(s.secretKey)
	if err != nil {
		return "", fmt.Errorf("토큰 서명 실패: %w", err)
	}
	return tokenString, nil
}

func (s *JWTService) ValidateToken(tokenString string) (string, error) {
	token, err := jwt.ParseWithClaims(tokenString, &claims{}, func(token *jwt.Token) (interface{}, error) {
		return s.secretKey, nil
	})
	if err != nil {
		return "", fmt.Errorf("토큰 파싱 실패: %w", err)
	}
	if !token.Valid {
		return "", fmt.Errorf("토큰이 유효하지 않습니다")
	}
	claims, ok := token.Claims.(*claims)
	if !ok {
		return "", fmt.Errorf("토큰 클레임 추출 실패")
	}
	return claims.UserID, nil
}
