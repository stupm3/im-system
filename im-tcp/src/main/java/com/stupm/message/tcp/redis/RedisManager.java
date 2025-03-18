package com.stupm.message.tcp.redis;

import com.stupm.message.codec.config.BootstrapConfig;
import com.stupm.message.tcp.receiver.UserLoginMessageListener;
import org.redisson.api.RedissonClient;

public class RedisManager {
    private static RedissonClient redisson;
    public static void init(BootstrapConfig config){
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        redisson = singleClientStrategy.getRedissonClient(config.getIm().getRedis());
        UserLoginMessageListener userLoginMessageListener = new UserLoginMessageListener(config.getIm().getLoginModel());
        userLoginMessageListener.listenerUserLogin();
    }

    public static RedissonClient getRedissonClient(){
        return redisson;
    }
}
