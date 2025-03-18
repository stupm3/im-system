package com.stupm.service.utils;

import com.stupm.common.ResponseVO;
import com.stupm.common.config.AppConfig;
import com.stupm.common.utils.HttpRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CallbackService {
    private static final Logger logger = LoggerFactory.getLogger(CallbackService.class);

    @Autowired
    private HttpRequestUtils requestUtils;

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private HttpRequestUtils httpRequestUtils;

    public void callback(Integer appId, String callbackCommand ,String jsonBody){
        try {
            httpRequestUtils.doPost("",Object.class,builderUrlParams(appId,callbackCommand), jsonBody,null);
        } catch (Exception e) {
            logger.error("回调 {} : {} 出现异常",e);
        }
    }

    public Map builderUrlParams(Integer appId, String command) {
        Map map = new HashMap();
        map.put("appId", appId);
        map.put("command", command);
        return map;
    }
    public ResponseVO beforeCallback(Integer appId, String callbackCommand ,String jsonBody){
        try {
            ResponseVO responseVO = httpRequestUtils.doPost("", ResponseVO.class, builderUrlParams(appId, callbackCommand), jsonBody, null);
            return responseVO;
        } catch (Exception e) {
            logger.error("回调 {} : {} 出现异常",e);
            ResponseVO.successResponse();
        }
        return null;
    }
}
