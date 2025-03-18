package com.stupm.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.stupm.common.BaseErrorCode;
import com.stupm.common.ResponseVO;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.GateWayErrorCode;
import com.stupm.common.exception.ApplicationExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@Component
public class GatewayInterceptor implements HandlerInterceptor {

    @Autowired
    private IdentityCheck identityCheck;




    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(1 == 1)
                return true;
        String appId = request.getParameter("appId");
        if(StringUtils.isBlank(appId)) {
            resp(ResponseVO.errorResponse(GateWayErrorCode.APPID_NOT_EXIST), response);
            return false;
        }
        String operator = request.getParameter("operator");
        if(StringUtils.isBlank(operator)) {
            resp(ResponseVO.errorResponse(GateWayErrorCode.OPERATER_NOT_EXIST) ,response);
            return false;
        }
        String userSign = request.getParameter("userSign");
        if(StringUtils.isBlank(userSign)) {
            resp(ResponseVO.errorResponse(GateWayErrorCode.USERSIGN_IS_ERROR), response);
            return false;
        }

        ApplicationExceptionEnum applicationExceptionEnum = identityCheck.checkUserSign(operator, appId, userSign);
        if(applicationExceptionEnum != BaseErrorCode.SUCCESS) {
            resp(ResponseVO.errorResponse(applicationExceptionEnum), response);
            return false;
        }

        return true;
    }

    private void resp(ResponseVO responseVO, HttpServletResponse response){
        PrintWriter writer = null;
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=utf-8");
        try{
            String resp = JSONObject.toJSONString(responseVO);
            writer = response.getWriter();
            writer.write(resp);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(writer!=null){
                writer.close();
            }
        }
    }
}
