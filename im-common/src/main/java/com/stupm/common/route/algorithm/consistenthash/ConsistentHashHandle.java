package com.stupm.common.route.algorithm.consistenthash;

import com.stupm.common.route.RouteHandle;

import java.util.List;

public class ConsistentHashHandle implements RouteHandle {

    private AbstractConsistentHash consistentHash;

    public void setConsistentHash(AbstractConsistentHash consistentHash) {
        this.consistentHash = consistentHash;
    }

    @Override
    public String routeServer(List<String> serverAdr, String key) {
        return consistentHash.process(serverAdr, key);
    }
}
