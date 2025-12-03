package middleware

import (
	"fmt"
	"net/http"
	"secret-solution/internal/etcd"
	"secret-solution/internal/token"
	"strings"

	"github.com/gin-gonic/gin"
)

func AuthMiddleware(jwtService *token.JWTService, etcdRepo etcd.EtcdRepository) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization 헤더가 누락되었습니다"})
			c.Abort()
			return
		}

		parts := strings.Split(authHeader, " ")
		if len(parts) != 2 || parts[0] != "Bearer" {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "유효하지 않은 토큰 형식입니다"})
			c.Abort()
			return
		}

		tokenString := parts[1]
		userID, err := jwtService.ValidateToken(tokenString)
		if err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "토큰 검증 실패: " + err.Error()})
			c.Abort()
			return
		}

		// Etcd에 저장된 토큰값과 일치하는지 확인
		etcdKey := fmt.Sprintf("/secrets-app/tokens/%s", userID)
		storedToken, err := etcdRepo.Get(etcdKey)
		if err != nil || storedToken != tokenString {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "토큰이 유효하지 않거나 폐기되었습니다"})
			c.Abort()
			return
		}

		c.Set("userID", userID)
		c.Next()
	}
}
