package com.ns.secrets.repository;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@RequiredArgsConstructor
public class EtcdRepository {
    private final Client client;

    public void put(String key, String value) {
        client.getKVClient().put(ByteSequence.from(key, UTF_8), ByteSequence.from(value, UTF_8)).join();
    }

    public String get(String key) {
        GetResponse response = client.getKVClient().get(ByteSequence.from(key, UTF_8)).join();
        return response.getKvs().isEmpty() ? null : response.getKvs().get(0).getValue().toString(UTF_8);
    }


    // etcd의 Lease 객체를 이용해서 TTL 지정
    public long grantLease(long ttlSeconds) {
        return client.getLeaseClient()
                .grant(ttlSeconds)
                .join()
                .getID();
    }

    // 초단위 TTL 경과시 자동으로 삭제
    public void putWithLease(String key, String value, long leaseId) {
        client.getKVClient()
                .put(ByteSequence.from(key, UTF_8), ByteSequence.from(value, UTF_8),
                        PutOption.newBuilder().withLeaseId(leaseId).build())
                .join();
    }

    // test
    public List<String> getAllKeys(String prefix) {
        GetResponse response = client.getKVClient().get(ByteSequence.from(prefix, UTF_8), GetOption.newBuilder().withPrefix(ByteSequence.from(prefix, UTF_8)).build()).join();
        return response.getKvs().stream()
                .map(kv -> kv.getKey().toString(UTF_8))
                .collect(Collectors.toList());
    }

    public void delete(String key) {
        client.getKVClient().delete(ByteSequence.from(key, UTF_8)).join();
    }

}

