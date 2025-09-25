package etcd

import (
	"context"
	"fmt"
	"log"
	"time"

	clientv3 "go.etcd.io/etcd/client/v3"
)

type KeyValue struct {
	Key   string
	Value string
}

type EtcdRepository interface {
	Put(key, value string) error
	Get(key string) (string, error)
	Delete(key string) error
	GrantLease(ttlSeconds int64) (clientv3.LeaseID, error)
	PutWithLease(key, value string, leaseID clientv3.LeaseID) error
	GetWithPrefix(prefix string) ([]KeyValue, error)
	GetAllKeys(prefix string) ([]string, error)
	Close() error
}

type etcdRepository struct {
	client         *clientv3.Client
	requestTimeout time.Duration
}

func NewEtcdRepository(endpoints []string, requestTimeout time.Duration) (EtcdRepository, error) {
	cli, err := clientv3.New(clientv3.Config{
		Endpoints:   endpoints,
		DialTimeout: 5 * time.Second,
	})
	if err != nil {
		log.Printf("etcd 클라이언트 생성 실패: %v", err)
		return nil, fmt.Errorf("etcd 클라이언트 생성 실패: %w", err)
	}

	return &etcdRepository{
		client:         cli,
		requestTimeout: requestTimeout,
	}, nil
}

func (r *etcdRepository) Close() error {
	if r.client != nil {
		return r.client.Close()
	}
	return nil
}

func (r *etcdRepository) Put(key, value string) error {
	ctx, cancel := context.WithTimeout(context.Background(), r.requestTimeout)
	defer cancel()
	_, err := r.client.Put(ctx, key, value)
	if err != nil {
		return fmt.Errorf("etcd put 키 저장 실패: %w", err)
	}
	return nil
}

func (r *etcdRepository) Get(key string) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), r.requestTimeout)
	defer cancel()
	resp, err := r.client.Get(ctx, key)
	if err != nil {
		return "", fmt.Errorf("etcd get 키 가져오기 실패: %w", err)
	}
	if len(resp.Kvs) == 0 {
		return "", fmt.Errorf("키 '%s'를 찾을 수 없습니다", key)
	}
	return string(resp.Kvs[0].Value), nil
}

func (r *etcdRepository) Delete(key string) error {
	ctx, cancel := context.WithTimeout(context.Background(), r.requestTimeout)
	defer cancel()
	_, err := r.client.Delete(ctx, key)
	if err != nil {
		return fmt.Errorf("etcd delete 키 삭제 실패: %w", err)
	}
	return nil
}

func (r *etcdRepository) GrantLease(ttlSeconds int64) (clientv3.LeaseID, error) {
	ctx, cancel := context.WithTimeout(context.Background(), r.requestTimeout)
	defer cancel()
	lease, err := r.client.Grant(ctx, ttlSeconds)
	if err != nil {
		return 0, fmt.Errorf("lease 발급 실패: %w", err)
	}
	return lease.ID, nil
}

func (r *etcdRepository) PutWithLease(key, value string, leaseID clientv3.LeaseID) error {
	ctx, cancel := context.WithTimeout(context.Background(), r.requestTimeout)
	defer cancel()
	_, err := r.client.Put(ctx, key, value, clientv3.WithLease(leaseID))
	if err != nil {
		return fmt.Errorf("lease와 함께 put 저장 실패: %w", err)
	}
	return nil
}

func (r *etcdRepository) GetWithPrefix(prefix string) ([]KeyValue, error) {
	ctx, cancel := context.WithTimeout(context.Background(), r.requestTimeout)
	defer cancel()
	resp, err := r.client.Get(ctx, prefix, clientv3.WithPrefix())
	if err != nil {
		return nil, fmt.Errorf("prefix로 etcd에서 가져오기 실패: %w", err)
	}

	var kvs []KeyValue
	for _, kv := range resp.Kvs {
		kvs = append(kvs, KeyValue{
			Key:   string(kv.Key),
			Value: string(kv.Value),
		})
	}

	return kvs, nil
}

// prefix 모든 키를 string[] slice 반환
func (r *etcdRepository) GetAllKeys(prefix string) ([]string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), r.requestTimeout)
	defer cancel()
	resp, err := r.client.Get(ctx, prefix, clientv3.WithPrefix(), clientv3.WithKeysOnly())
	if err != nil {
		return nil, fmt.Errorf("etcd GetAllKeys 실패: %w", err)
	}
	keys := make([]string, 0, len(resp.Kvs))
	for _, kv := range resp.Kvs {
		keys = append(keys, string(kv.Key))
	}
	return keys, nil
}
