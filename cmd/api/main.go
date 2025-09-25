package main

import (
	"log"
	"net/http"
	"strconv"
	"time"

	"secret-solution/internal/config"
	"secret-solution/internal/etcd"
	"secret-solution/internal/git"
	"secret-solution/internal/permission"
	"secret-solution/internal/policy"
	"secret-solution/internal/policyBinding"
	"secret-solution/internal/secret"
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
	userService, _ := user.NewUserService(cfg)
	policyService, _ := policy.NewPolicyService(cfg)
	policyBindingService := policyBinding.NewPolicyBindingService(etcdRepo)
	permissionService := permission.NewPermissionService(policyService, policyBindingService, userService)
	secretService := secret.NewSecretService(etcdRepo, aesEncryptor, permissionService)

	router := gin.Default()

	// -------------------------
	// Secret API
	// -------------------------
	secretGroup := router.Group("/secrets")
	{
		secretGroup.GET("/:userId/:namespace/:key", func(c *gin.Context) {
			val, err := secretService.GetDecrypted(c.Param("userId"), c.Param("namespace"), c.Param("key"))
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, gin.H{"decrypted": val})
		})

		secretGroup.POST("/raw", func(c *gin.Context) {
			var req struct {
				UserID    string `json:"user_id"`
				Namespace string `json:"namespace"`
				Key       string `json:"key"`
				Value     string `json:"value"`
			}
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
				return
			}
			if err := secretService.SaveRaw(req.UserID, req.Namespace, req.Key, req.Value, false, 0); err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, gin.H{"status": "saved"})
		})

		secretGroup.POST("/encrypted", func(c *gin.Context) {
			var req struct {
				UserID    string `json:"user_id"`
				Namespace string `json:"namespace"`
				Key       string `json:"key"`
				Value     string `json:"value"`
			}
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
				return
			}
			if err := secretService.SaveEncrypted(req.UserID, req.Namespace, req.Key, req.Value, false, 0); err != nil {
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
	policyGroup := router.Group("/policy")
	{
		policyGroup.GET("/:policyId", func(c *gin.Context) {
			p, err := policyService.GetPolicyByID(c.Param("policyId"))
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, p)
		})

		policyGroup.GET("/", func(c *gin.Context) {
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
	userGroup := router.Group("/user")
	{
		userGroup.GET("/:userId/group", func(c *gin.Context) {
			group, err := userService.GetUserGroup(c.Param("userId"))
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
	pbGroup := router.Group("/policy-binding")
	{
		pbGroup.POST("/bind", func(c *gin.Context) {
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
