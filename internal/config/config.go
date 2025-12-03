package config

import (
	"log"
	"os"
	"strconv"
	"strings"
)

type AppConfig struct {
	App struct {
		Environment string
		LogLevel    string
		HttpPort    int
		GrpcPort    int
		LogPath     string
	}
	Etcd struct {
		Endpoints             []string
		RequestTimeoutSeconds int
	}
	Git struct {
		RepoURL             string
		LocalRepoPath       string
		SyncIntervalMinutes int
	}
	Crypto struct {
		AesKey string
	}
}

func NewConfig() (*AppConfig, error) {
	cfg := &AppConfig{}

	aesKey := os.Getenv("AES_KEY")
	if aesKey == "" {
		log.Fatal("AES_KEY가 설정되지 않았습니다.")
	}
	cfg.Crypto.AesKey = aesKey

	gitRepo := os.Getenv("GIT_REPO_URL")
	if gitRepo == "" {
		log.Fatal("GIT_REPO_URL이 설정되지 않았습니다.")
	}
	cfg.Git.RepoURL = gitRepo

	etcdURLs := os.Getenv("ETCD_URLS")
	if etcdURLs == "" {
		log.Fatal("ETCD_URLS가 설정되지 않았습니다.")
	}
	cfg.Etcd.Endpoints = strings.Split(etcdURLs, ",")
	log.Printf("ETCD endpoints: %+v", cfg.Etcd.Endpoints)

	httpPortStr := os.Getenv("APP_HTTP_PORT")
	if httpPortStr == "" {
		log.Fatal("APP_HTTP_PORT가 설정되지 않았습니다.")
	}
	httpPort, err := strconv.Atoi(httpPortStr)
	if err != nil {
		log.Fatalf("APP_HTTP_PORT 값이 정수가 아닙니다: %v", err)
	}
	cfg.App.HttpPort = httpPort

	cfg.App.Environment = getEnvOrDefault("APP_ENVIRONMENT", "dev")
	cfg.App.LogLevel = getEnvOrDefault("APP_LOG_LEVEL", "info")
	cfg.App.LogPath = getEnvOrDefault("APP_LOG_PATH", "/var/log/app.log")

	if grpcPortStr := os.Getenv("APP_GRPC_PORT"); grpcPortStr != "" {
		if port, err := strconv.Atoi(grpcPortStr); err == nil {
			cfg.App.GrpcPort = port
		} else {
			cfg.App.GrpcPort = 50051
		}
	} else {
		cfg.App.GrpcPort = 50051
	}

	if timeoutStr := os.Getenv("ETCD_REQUEST_TIMEOUT"); timeoutStr != "" {
		if t, err := strconv.Atoi(timeoutStr); err == nil {
			cfg.Etcd.RequestTimeoutSeconds = t
		} else {
			cfg.Etcd.RequestTimeoutSeconds = 5
		}
	} else {
		cfg.Etcd.RequestTimeoutSeconds = 5
	}

	cfg.Git.LocalRepoPath = getEnvOrDefault("GIT_LOCAL_REPO_PATH", "/root/git-repo")
	if syncStr := os.Getenv("GIT_SYNC_INTERVAL"); syncStr != "" {
		if t, err := strconv.Atoi(syncStr); err == nil {
			cfg.Git.SyncIntervalMinutes = t
		} else {
			cfg.Git.SyncIntervalMinutes = 5
		}
	} else {
		cfg.Git.SyncIntervalMinutes = 5
	}

	return cfg, nil
}

func getEnvOrDefault(key, def string) string {
	if val := os.Getenv(key); val != "" {
		return val
	}
	return def
}
