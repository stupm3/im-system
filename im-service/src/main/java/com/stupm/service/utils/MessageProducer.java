package com.stupm.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.command.Command;
import com.stupm.common.model.ClientInfo;
import com.stupm.common.model.UserSession;
import com.stupm.message.codec.proto.MessagePack;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class MessageProducer {
    private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    UserSessionUtils userSessionUtils;

    private static final String queueName = Constants.RabbitConstants.MessageService2Im;

    public boolean sendMessage(UserSession userSession, Object message){
        try{
            logger.info("send message == {}" , message.toString());
            rabbitTemplate.convertAndSend(queueName,userSession.getBrokerId()+"",message);
            return true;
        }catch(Exception e){
            logger.error("send message error {}",e.getMessage());
            return false;
        }
    }

    public boolean sendPack(String toId, Command command , Object msg , UserSession userSession){
        MessagePack messagePack = new MessagePack();
        messagePack.setCommand(command.getCommand());
        messagePack.setToId(toId);
        messagePack.setClientType(userSession.getClientType());
        messagePack.setAppId(userSession.getAppId());
        messagePack.setImei(userSession.getImei());
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(msg));
        messagePack.setData(jsonObject);
        String message = JSONObject.toJSONString(messagePack);
        boolean result = sendMessage(userSession,message);

        return result;
    }
//  发送所有端
    public List<ClientInfo> sendToUser(Integer appId,String toId , Command command ,Object data ){
        List<UserSession> userSession = userSessionUtils.getUserSession(appId, toId);
        List<ClientInfo> list = new ArrayList<>();

        for(UserSession userSession1 : userSession){
            boolean b = sendPack(toId, command, data, userSession1);
            if(b){
                list.add(new ClientInfo(userSession1.getAppId(),userSession1.getClientType(),userSession1.getImei()));
            }
        }

        return list;
    }

    public void sendToUser(String toId , Integer clientType , String imei , Command command ,Object data ,Integer appId){
        if(clientType != null && StringUtils.isNotBlank(imei)){
            ClientInfo clientInfo = new ClientInfo(appId, clientType, imei);
            sendToUser(toId,command,data,clientInfo);
        }else{
            sendToUser(appId,toId,command,data);
        }
    }

    //指定
    public void sendToUser( String toId , Command command , Object data , ClientInfo clientInfo ){
        UserSession userSession = userSessionUtils.getUserSession(clientInfo.getAppId(), toId, clientInfo.getClientType(), clientInfo.getImei());
        sendPack(toId,command,data,userSession);
    }
    public void sendToUserAnotherClient( String toId , Command command , Object data , ClientInfo clientInfo ){
        List<UserSession> userSession = userSessionUtils.getUserSession(clientInfo.getAppId(), toId);
        for(UserSession userSession1 : userSession){
            if(!isMatch(userSession1,clientInfo)){
                sendPack(toId,command,data,userSession1);
            }
        }
    }
    private boolean isMatch(UserSession sessionDto, ClientInfo clientInfo) {
        return Objects.equals(sessionDto.getAppId(), clientInfo.getAppId())
                && Objects.equals(sessionDto.getImei(), clientInfo.getImei())
                && Objects.equals(sessionDto.getClientType(), clientInfo.getClientType());
    }

}
