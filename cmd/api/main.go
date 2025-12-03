package main

import (
	"log"
	"net/http"
	"strconv"
	"time"

	"secret-solution/internal/config"
	"secret-solution/internal/etcd"
	"secret-solution/internal/git"
	"secret-solution/internal/middleware"
	"secret-solution/internal/permission"
	"secret-solution/internal/policy"
	"secret-solution/internal/policyBinding"
	"secret-solution/internal/secret"
	"secret-solution/internal/token"
	"secret-solution/internal/user"

	"github.com/gin-gonic/gin"
)

func main() {
	cfg, err := config.NewConfig()
	if err != nil {
		log.Fatalf("config init failed: %v", err)
	}

	etcdRepo, err := etcd.NewEtcdRepository(cfg.Etcd.Endpoints, time.Duration(cfg.Etcd.RequestTimeoutSeconds)*time.Second)
	if err != nil {
		log.Fatalf("etcd init failed: %v", err)
	}
	aesEncryptor, err := secret.NewAesEncryptor(cfg.Crypto.AesKey)
	if err != nil {
		log.Fatalf("AES init failed: %v", err)
	}
	gitService, _ := git.NewGitService(cfg.Git.RepoURL, cfg.Git.LocalRepoPath)

	jwtService := token.NewJWTService(cfg.Crypto.AesKey)

	userService, _ := user.NewUserService(cfg, etcdRepo, jwtService)

	if err := userService.SyncAndStoreTokens(); err != nil {
		log.Fatalf("사용자 토큰 동기화 및 저장 실패: %v", err)
	}

	policyService, _ := policy.NewPolicyService(cfg)
	policyBindingService := policyBinding.NewPolicyBindingService(etcdRepo)
	permissionService := permission.NewPermissionService(policyService, policyBindingService, userService)
	secretService := secret.NewSecretService(etcdRepo, aesEncryptor, permissionService)

	router := gin.Default()

	protected := router.Group("/")
	protected.Use(middleware.AuthMiddleware(jwtService, etcdRepo))

	// -------------------------
	// Secret API
	// -------------------------
	secretGroup := protected.Group("/secrets")
	{
		secretGroup.GET("/:namespace/:key", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			namespace := c.Param("namespace")
			key := c.Param("key")
			resource := secretService.ResourcePath(namespace, key)

			if !permissionService.CheckPermission(userID.(string), []string{"secret:read", "secret:decrypt"}, resource) {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}

			val, err := secretService.GetDecrypted(userID.(string), namespace, key)
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}

			c.JSON(http.StatusOK, gin.H{"decrypted": val})
		})

		secretGroup.GET("/raw/:namespace/:key", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			namespace := c.Param("namespace")
			key := c.Param("key")
			resource := secretService.ResourcePath(namespace, key)

			if !permissionService.CheckPermission(userID.(string), []string{"secret:read"}, resource) {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}

			val, err := secretService.GetRaw(userID.(string), namespace, key)
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}

			c.JSON(http.StatusOK, gin.H{"raw": val})
		})

		secretGroup.POST("/raw", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			var req struct {
				Namespace string `json:"namespace"`
				Key       string `json:"key"`
				Value     string `json:"value"`
			}
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
				return
			}

			resource := secretService.ResourcePath(req.Namespace, req.Key)
			if !permissionService.CheckPermission(userID.(string), []string{"secret:write"}, resource) {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}

			if err := secretService.SaveRaw(userID.(string), req.Namespace, req.Key, req.Value, false, 0); err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}

			c.JSON(http.StatusOK, gin.H{"status": "saved"})
		})

		secretGroup.POST("/encrypted", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			var req struct {
				Namespace string `json:"namespace"`
				Key       string `json:"key"`
				Value     string `json:"value"`
			}
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
				return
			}

			resource := secretService.ResourcePath(req.Namespace, req.Key)
			if !permissionService.CheckPermission(userID.(string), []string{"secret:write", "secret:encrypt"}, resource) {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}

			if err := secretService.SaveEncrypted(userID.(string), req.Namespace, req.Key, req.Value, false, 0); err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}

			c.JSON(http.StatusOK, gin.H{"status": "saved"})
		})
	}

	// -------------------------
	// Git API
	// -------------------------
	gitGroup := router.Group("/git")
	{
		gitGroup.POST("/sync", func(c *gin.Context) {
			changed, err := gitService.Sync()
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, gin.H{"changed_files": changed})
		})
	}

	// -------------------------
	// Policy API
	// -------------------------
	policyGroup := protected.Group("/policy")
	{
		policyGroup.GET("/:policyId", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			if !permissionService.CheckPermission(userID.(string), []string{"policy:read"}, c.Param("policyId")) {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}
			p, err := policyService.GetPolicyByID(c.Param("policyId"))
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, p)
		})

		policyGroup.GET("/", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			if !permissionService.CheckPermission(userID.(string), []string{"policy:read-list"}, "all") {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}
			policies, err := policyService.GetAllPolicies()
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, policies)
		})
	}

	// -------------------------
	// User API
	// -------------------------
	userGroup := protected.Group("/user")
	{
		userGroup.GET("/:userId/group", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			targetUserID := c.Param("userId")
			if !permissionService.CheckPermission(userID.(string), []string{"user:read-user-group"}, targetUserID) {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}
			group, err := userService.GetUserGroup(targetUserID)
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, gin.H{"group": group})
		})
	}

	// -------------------------
	// Policy Binding API
	// -------------------------
	pbGroup := protected.Group("/policy-binding")
	{
		pbGroup.POST("/bind", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			if !permissionService.CheckPermission(userID.(string), []string{"policy-binding:bind"}, "all") {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}
			var req struct {
				MemberID   string   `json:"member_id"`
				MemberType string   `json:"member_type"`
				PolicyIDs  []string `json:"policy_ids"`
			}
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
				return
			}
			err := policyBindingService.BindPoliciesToMember(req.MemberID, policyBinding.MemberType(req.MemberType), req.PolicyIDs)
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, gin.H{"status": "bound"})
		})

		pbGroup.POST("/unbind", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			if !permissionService.CheckPermission(userID.(string), []string{"policy-binding:unbind"}, "all") {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}
			var req struct {
				MemberID   string `json:"member_id"`
				MemberType string `json:"member_type"`
			}
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
				return
			}
			err := policyBindingService.UnbindPoliciesFromMember(req.MemberID, policyBinding.MemberType(req.MemberType))
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, gin.H{"status": "unbound"})
		})

		pbGroup.GET("/get/:memberId/:memberType", func(c *gin.Context) {
			userID, _ := c.Get("userID")
			if !permissionService.CheckPermission(userID.(string), []string{"policy-binding:read-binding"}, c.Param("memberId")) {
				c.JSON(http.StatusForbidden, gin.H{"error": "권한이 없습니다."})
				return
			}
			binding, err := policyBindingService.GetPolicyBindingForMember(c.Param("memberId"), policyBinding.MemberType(c.Param("memberType")))
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, binding)
		})
	}

	httpPort := cfg.App.HttpPort
	srv := &http.Server{
		Addr:    ":" + strconv.Itoa(httpPort),
		Handler: router,
	}

	log.Printf("HTTP API server running on port %d", httpPort)
	if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("Server failed: %v", err)
	}
}
