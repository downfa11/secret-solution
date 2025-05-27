package com.ns.secrets.service;


import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.options.PutOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElectionService {

//    private static final String LEADER_KEY = "/secrets/leader";
//    private static final long LEASE_TTL = 10L; // seconds
//
//    private final Client etcdClient;
//    private Lease leaseClient;
//    private long leaseId;
//
//    private volatile boolean isLeader = false;
//    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//
//
//    @PostConstruct
//    public void start() {
//        leaseClient = etcdClient.getLeaseClient();
//        tryToAcquireLeadership();
//    }
//
//    private void tryToAcquireLeadership() {
//        leaseClient.grant(LEASE_TTL).thenCompose(grantResponse -> {
//            leaseId = grantResponse.getID();
//            ByteSequence leaseIdBytes = ByteSequence.from(Long.toString(leaseId).getBytes(StandardCharsets.UTF_8));
//            ByteSequence key = ByteSequence.from(LEADER_KEY.getBytes(StandardCharsets.UTF_8));
//
//            return etcdClient.getKVClient()
//                    .put(key, leaseIdBytes, PutOption.newBuilder().withLeaseId(leaseId).build());
//        }).thenAccept(putResponse -> {
//            isLeader = true;
//            log.info("Acquired leader {}", leaseId);
//            scheduler.scheduleAtFixedRate(this::keepAlive, LEASE_TTL / 2, LEASE_TTL / 2, TimeUnit.SECONDS);
//        }).exceptionally(ex -> {
//            isLeader = false;
//            log.info("tryToAcquireLeadership retrying 5s");
//            scheduler.schedule(this::tryToAcquireLeadership, 5, TimeUnit.SECONDS);
//            return null;
//        });
//    }
//
//    private void keepAlive() {
//        leaseClient.keepAliveOnce(leaseId).exceptionally(ex -> {
//            isLeader = false;
//            scheduler.shutdown();
//            tryToAcquireLeadership();
//            return null;
//        });
//    }
//
//    public boolean isLeader() {
//        return isLeader;
//    }
//
//    @PreDestroy
//    public void shutdown() {
//        scheduler.shutdown();
//        leaseClient.revoke(leaseId);
//    }
}
