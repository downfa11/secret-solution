package config

import (
	"log"
	"os"
)

type AppConfig struct {
	App struct {
		Environment string `yaml:"environment"`
		LogLevel    string `yaml:"log_level"`
		HttpPort    int    `yaml:"http_port"`
		GrpcPort    int    `yaml:"grpc_port"`
		LogPath     string `yaml:"log_path"`
	} `yaml:"app"`
	Etcd struct {
		Endpoints             []string `yaml:"endpoints"`
		RequestTimeoutSeconds int      `yaml:"request_timeout_seconds"`
	} `yaml:"etcd"`
	Git struct {
		RepoURL             string `yaml:"repo_url"`
		LocalRepoPath       string `yaml:"local_repo_path"`
		SyncIntervalMinutes int    `yaml:"sync_interval_minutes"`
	} `yaml:"git"`
	Crypto struct {
		AesKey string `yaml:"aes_key"`
	} `yaml:"crypto"`
	Policies struct {
		Location string `yaml:"location"`
	} `yaml:"policies"`
}

func NewConfig() (*AppConfig, error) {
	config := &AppConfig{}

	if etcdURL := os.Getenv("ETCD_URL"); etcdURL != "" {
		config.Etcd.Endpoints = []string{etcdURL}
		log.Printf("ETCD endpoints: %+v", config.Etcd.Endpoints)
	}
	if gitRepoURL := os.Getenv("GIT_REPO_URL"); gitRepoURL != "" {
		config.Git.RepoURL = gitRepoURL
		log.Printf("Git Repo URL: %s", gitRepoURL)
	}
	if aesKey := os.Getenv("AES_KEY"); aesKey != "" {
		config.Crypto.AesKey = aesKey
	}

	return config, nil
}
