package main

import (
	"log"
	"strconv"
	"strings"
	"time"

	"secret-solution/internal/config"
	"secret-solution/internal/etcd"
	"secret-solution/internal/git"
	"secret-solution/internal/permission"
	"secret-solution/internal/policy"
	"secret-solution/internal/policyBinding"
	"secret-solution/internal/secret"
	"secret-solution/internal/user"

	"github.com/spf13/cobra"
)

func main() {
	cfg, _ := config.NewConfig()
	etcdRepo, _ := etcd.NewEtcdRepository(cfg.Etcd.Endpoints, time.Duration(cfg.Etcd.RequestTimeoutSeconds)*time.Second)
	aesEncryptor, _ := secret.NewAesEncryptor(cfg.Crypto.AesKey)
	gitService, _ := git.NewGitService(cfg.Git.RepoURL, cfg.Git.LocalRepoPath)
	userService, _ := user.NewUserService(cfg)
	policyService, _ := policy.NewPolicyService(cfg)
	policyBindingService := policyBinding.NewPolicyBindingService(etcdRepo)
	permissionService := permission.NewPermissionService(policyService, policyBindingService, userService)
	secretService := secret.NewSecretService(etcdRepo, aesEncryptor, permissionService)

	rootCmd := &cobra.Command{
		Use:   "secret-solution",
		Short: "CLI for secret-solution",
	}

	rootCmd.AddCommand(NewSecretCmd(secretService))
	rootCmd.AddCommand(NewGitCmd(gitService))
	rootCmd.AddCommand(NewPolicyCmd(policyService))
	rootCmd.AddCommand(NewUserCmd(userService))
	rootCmd.AddCommand(NewPolicyBindingCmd(policyBindingService))

	if err := rootCmd.Execute(); err != nil {
		log.Fatal(err)
	}
}

// -------------------------
// Secret CLI
// -------------------------
func NewSecretCmd(svc *secret.SecretService) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "secret",
		Short: "Manage secrets",
	}

	cmd.AddCommand(&cobra.Command{
		Use:   "get [user_id] [namespace] [key]",
		Short: "Get decrypted secret",
		Args:  cobra.ExactArgs(3),
		Run: func(cmd *cobra.Command, args []string) {
			val, err := svc.GetDecrypted(args[0], args[1], args[2])
			if err != nil {
				log.Fatal(err)
			}
			log.Println("Decrypted:", val)
		},
	})

	cmd.AddCommand(&cobra.Command{
		Use:   "get-raw [user_id] [namespace] [key]",
		Short: "Get raw secret",
		Args:  cobra.ExactArgs(3),
		Run: func(cmd *cobra.Command, args []string) {
			val, err := svc.GetRaw(args[0], args[1], args[2])
			if err != nil {
				log.Fatal(err)
			}
			log.Println("Raw:", val)
		},
	})

	cmd.AddCommand(&cobra.Command{
		Use:   "get-all [user_id] [namespace]",
		Short: "Get all secrets in namespace",
		Args:  cobra.ExactArgs(2),
		Run: func(cmd *cobra.Command, args []string) {
			secrets, err := svc.GetAllSecrets(args[0], args[1])
			if err != nil {
				log.Fatal(err)
			}
			log.Println("Secrets:", secrets)
		},
	})

	// save-raw
	var rawTTL bool
	var rawTTLSeconds int64
	saveRaw := &cobra.Command{
		Use:   "save-raw [user_id] [namespace] [key] [value]",
		Short: "Save raw secret",
		Args:  cobra.ExactArgs(4),
		Run: func(cmd *cobra.Command, args []string) {
			if err := svc.SaveRaw(args[0], args[1], args[2], args[3], rawTTL, rawTTLSeconds); err != nil {
				log.Fatal(err)
			}
			log.Println("Saved raw secret")
		},
	}
	saveRaw.Flags().BoolVarP(&rawTTL, "ttl", "t", false, "Set TTL")
	saveRaw.Flags().Int64VarP(&rawTTLSeconds, "ttl-seconds", "s", 0, "TTL seconds")
	cmd.AddCommand(saveRaw)

	// save-encrypted
	var encTTL bool
	var encTTLSeconds int64
	saveEnc := &cobra.Command{
		Use:   "save-encrypted [user_id] [namespace] [key] [value]",
		Short: "Save encrypted secret",
		Args:  cobra.ExactArgs(4),
		Run: func(cmd *cobra.Command, args []string) {
			if err := svc.SaveEncrypted(args[0], args[1], args[2], args[3], encTTL, encTTLSeconds); err != nil {
				log.Fatal(err)
			}
			log.Println("Saved encrypted secret")
		},
	}
	saveEnc.Flags().BoolVarP(&encTTL, "ttl", "t", false, "Set TTL")
	saveEnc.Flags().Int64VarP(&encTTLSeconds, "ttl-seconds", "s", 0, "TTL seconds")
	cmd.AddCommand(saveEnc)

	return cmd
}

// -------------------------
// Git CLI
// -------------------------
func NewGitCmd(svc *git.GitService) *cobra.Command {
	cmd := &cobra.Command{
		Use:   "git",
		Short: "Manage Git repository",
	}

	cmd.AddCommand(&cobra.Command{
		Use:   "sync",
		Short: "Sync local git with remote",
		Args:  cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			changed, err := svc.Sync()
			if err != nil {
				log.Fatal(err)
			}
			log.Println("Changed files:", changed)
		},
	})

	cmd.AddCommand(&cobra.Command{
		Use:   "log [count]",
		Short: "Show recent commit logs",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			count, _ := strconv.Atoi(args[0])
			logs, _ := svc.GetRecentCommitMessages(count)
			for _, msg := range logs {
				log.Println(msg)
			}
		},
	})

	cmd.AddCommand(&cobra.Command{
		Use:   "rollback [commit_hash]",
		Short: "Rollback to specific commit",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			if err := svc.RollbackToCommit(args[0]); err != nil {
				log.Fatal(err)
			}
			log.Println("Rolled back to", args[0])
		},
	})

	cmd.AddCommand(&cobra.Command{
		Use:   "checkout [branch_name]",
		Short: "Checkout branch",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			if err := svc.CheckoutBranch(args[0]); err != nil {
				log.Fatal(err)
			}
			log.Println("Checked out to", args[0])
		},
	})

	return cmd
}

// -------------------------
// Policy CLI
// -------------------------
func NewPolicyCmd(svc *policy.PolicyService) *cobra.Command {
	cmd := &cobra.Command{Use: "policy", Short: "Manage policies"}

	cmd.AddCommand(&cobra.Command{
		Use:   "get [policy_id]",
		Short: "Get policy by ID",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			p, _ := svc.GetPolicyByID(args[0])
			log.Println("Policy:", p)
		},
	})

	cmd.AddCommand(&cobra.Command{
		Use:   "get-all",
		Short: "Get all policies",
		Args:  cobra.NoArgs,
		Run: func(cmd *cobra.Command, args []string) {
			policies, _ := svc.GetAllPolicies()
			log.Println("Policies:", policies)
		},
	})

	return cmd
}

// -------------------------
// User CLI
// -------------------------
func NewUserCmd(svc *user.UserService) *cobra.Command {
	cmd := &cobra.Command{Use: "user", Short: "Manage users"}

	cmd.AddCommand(&cobra.Command{
		Use:   "get-group [user_id]",
		Short: "Get user's group",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			group, _ := svc.GetUserGroup(args[0])
			log.Println("Group:", group)
		},
	})

	return cmd
}

// -------------------------
// Policy Binding CLI
// -------------------------
func NewPolicyBindingCmd(svc *policyBinding.PolicyBindingService) *cobra.Command {
	cmd := &cobra.Command{Use: "policy-binding", Short: "Manage policy bindings"}

	cmd.AddCommand(&cobra.Command{
		Use:   "bind [member_id] [member_type] [policy_ids]",
		Short: "Bind policies to member",
		Args:  cobra.ExactArgs(3),
		Run: func(cmd *cobra.Command, args []string) {
			policies := strings.Split(args[2], ",")
			if err := svc.BindPoliciesToMember(args[0], policyBinding.MemberType(args[1]), policies); err != nil {
				log.Fatal(err)
			}
			log.Println("Bound policies to member")
		},
	})

	cmd.AddCommand(&cobra.Command{
		Use:   "unbind [member_id] [member_type]",
		Short: "Unbind policies from member",
		Args:  cobra.ExactArgs(2),
		Run: func(cmd *cobra.Command, args []string) {
			if err := svc.UnbindPoliciesFromMember(args[0], policyBinding.MemberType(args[1])); err != nil {
				log.Fatal(err)
			}
			log.Println("Unbound policies from member")
		},
	})

	cmd.AddCommand(&cobra.Command{
		Use:   "get [member_id] [member_type]",
		Short: "Get policy bindings for member",
		Args:  cobra.ExactArgs(2),
		Run: func(cmd *cobra.Command, args []string) {
			binding, _ := svc.GetPolicyBindingForMember(args[0], policyBinding.MemberType(args[1]))
			log.Println("Binding:", binding)
		},
	})

	return cmd
}
