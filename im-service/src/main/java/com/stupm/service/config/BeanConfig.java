package com.stupm.service.config;

import cn.hutool.core.lang.Snowflake;
import com.stupm.common.config.AppConfig;
import com.stupm.common.enums.RouteHashMethodEnum;
import com.stupm.common.enums.UrlRouteWayEnum;
import com.stupm.common.route.RouteHandle;
import com.stupm.common.route.algorithm.consistenthash.AbstractConsistentHash;
import com.stupm.common.route.algorithm.consistenthash.ConsistentHashHandle;
import com.stupm.common.route.algorithm.consistenthash.TreeMapConsistentHash;
import com.stupm.common.route.algorithm.loop.LoopHandle;
import com.stupm.common.route.algorithm.random.RandomHandle;
import com.stupm.service.utils.SnowflakeIdWorker;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.TreeMap;

@Configuration
public class BeanConfig {




    @Autowired
    private AppConfig appConfig;

    @Bean
    public ZkClient buildZkClient() {
        return new ZkClient(appConfig.getZkAddr() , appConfig.getZkConnectTimeOut());
    }


    @Bean
    public RouteHandle routeHandle() throws Exception {
        Integer imRouteWay = appConfig.getImRouteWay();
        String routeWay = "";
        UrlRouteWayEnum handlerEnum = UrlRouteWayEnum.getHandler(imRouteWay);
        routeWay = handlerEnum.getClazz();
        RouteHandle handle = (RouteHandle) Class.forName(routeWay).newInstance();
        if(handlerEnum == UrlRouteWayEnum.HASH){
            Method setConsistentHash = Class.forName(routeWay).getMethod("setConsistentHash", AbstractConsistentHash.class);
            Integer consistentHashWay = appConfig.getConsistentHashWay();
            String hashWay = "";
            RouteHashMethodEnum hasHandler = RouteHashMethodEnum.getHandler(consistentHashWay);
            hashWay = hasHandler.getClazz();
            AbstractConsistentHash routeHandle = (AbstractConsistentHash) Class.forName(hashWay).newInstance();
            setConsistentHash.invoke(handle, routeHandle);
        }
        return handle;
    }

    @Bean
    public EasySqlInjector easySqlInjector() {
        return new EasySqlInjector();
    }

    @Bean
    public SnowflakeIdWorker snowflake() {
        return new SnowflakeIdWorker(0 );
    }
}
