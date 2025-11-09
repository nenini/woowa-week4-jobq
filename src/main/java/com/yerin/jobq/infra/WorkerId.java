package com.yerin.jobq.infra;

import java.net.InetAddress;
import java.util.UUID;

public final class WorkerId {
    private WorkerId() {}
    public static String consumerName() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (Exception e) {
            return "worker-" + UUID.randomUUID();
        }
    }
}
