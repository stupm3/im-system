package com.stupm.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.stupm.common.BaseErrorCode;
import com.stupm.common.config.AppConfig;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.GateWayErrorCode;
import com.stupm.common.exception.ApplicationExceptionEnum;
import com.stupm.common.utils.SigAPI;
import com.stupm.service.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class IdentityCheck {
    private static final Logger logger = LoggerFactory.getLogger(IdentityCheck.class);
    @Autowired
    UserService userService;

    @Autowired
    AppConfig appConfig;

    @Autowired
    StringRedisTemplate stringRedisTemplate;


    public ApplicationExceptionEnum checkUserSign(String operator , String appId, String userSign){

        String cacheUserSig = stringRedisTemplate.opsForValue().get(appId + Constants.RedisConstants.UserSign + ":" + operator + userSign);
        if(StringUtils.isNotBlank(cacheUserSig) && Long.valueOf(cacheUserSig) > System.currentTimeMillis() / 1000){
            return BaseErrorCode.SUCCESS;
        }
        String privateKey = appConfig.getPrivateKey();
        JSONObject jsonObject = SigAPI.decodeUserSig(userSign);
        Long expireTime = 0L;
        Long expireSec = 0L;
        String decoderAppid = "";
        String decoderOperator = "";
        try{
            decoderAppid =  jsonObject.getString("TLS.appId");
            String expire = jsonObject.get("TLS.expire").toString();
            System.out.println(decoderOperator);
            decoderOperator = jsonObject.getString("TLS.operator");
            String expireTimeStr = jsonObject.getString("TLS.expireTime");
            expireSec = Long.valueOf(expire) / 1000;
            expireTime = Long.valueOf(expireTimeStr) + expireSec;
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        if(!decoderOperator.equals(operator)){
            return GateWayErrorCode.USERSIGN_OPERATE_NOT_MATE;
        }
        if(!appId.equals(decoderAppid)){
            return GateWayErrorCode.USERSIGN_IS_ERROR;
        }
        if(expireSec == 0L){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }
        Long now = System.currentTimeMillis() ;
        if(expireTime < System.currentTimeMillis()  / 1000){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }
        String key = appId + Constants.RedisConstants.UserSign+":" + operator + userSign;
        Long eTime =  expireTime - System.currentTimeMillis() / 1000;

        stringRedisTemplate.opsForValue().set(key , expireTime.toString(), eTime , TimeUnit.SECONDS);

        return BaseErrorCode.SUCCESS;

    }

}
