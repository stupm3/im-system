package com.stupm.common.route.algorithm.loop;

import com.stupm.common.enums.UserErrorCode;
import com.stupm.common.exception.ApplicationException;
import com.stupm.common.route.RouteHandle;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class LoopHandle implements RouteHandle {

    private AtomicLong index = new AtomicLong();

    @Override
    public String routeServer(List<String> serverAdr, String key) {
        int size = serverAdr.size();
        if(size == 0){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        Long l = index.incrementAndGet() % size;
        if(l < 0){
            l = 0L;
        }
        return serverAdr.get(l.intValue());
    }
}
