package com.stupm.common.route.algorithm.random;

import com.stupm.common.enums.UserErrorCode;
import com.stupm.common.exception.ApplicationException;
import com.stupm.common.route.RouteHandle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomHandle implements RouteHandle {
    @Override
    public String routeServer(List<String> serverAdr, String key) {
        int size = serverAdr.size();
        if(size == 0){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        int i = ThreadLocalRandom.current().nextInt(size);
        return serverAdr.get(i);
    }
}
