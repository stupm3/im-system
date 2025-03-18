package com.stupm.message.tcp.utils;

import com.alibaba.fastjson.JSONObject;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.ConnectStatusEnum;
import com.stupm.common.enums.command.UserEventCommand;
import com.stupm.common.model.UserClientDTO;
import com.stupm.common.model.UserSession;
import com.stupm.message.codec.pack.user.UserStatusChangeNotifyPack;
import com.stupm.message.codec.proto.MessageHeader;
import com.stupm.message.tcp.publish.MqMessageProducer;
import com.stupm.message.tcp.redis.RedisManager;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionSocketHolder {
    private static final Map<UserClientDTO  , NioSocketChannel> socketChannelMap = new ConcurrentHashMap<>();
    public static void put(UserClientDTO dto, NioSocketChannel socketChannel) {
        socketChannelMap.put(dto , socketChannel);
    }

    public static NioSocketChannel get(UserClientDTO dto) {
        return socketChannelMap.get(dto);
    }

    public static void remove(UserClientDTO dto) {
        socketChannelMap.remove(dto);
    }
    public static void remove(NioSocketChannel socketChannel) {
        socketChannelMap.entrySet().removeIf(entry -> entry.getValue() == socketChannel);
    }
    public static List<NioSocketChannel> get(Integer appId , String id) {

        Set<UserClientDTO> channelInfos = socketChannelMap.keySet();
        List<NioSocketChannel> channels = new ArrayList<>();

        channelInfos.forEach(channel ->{
            if(channel.getAppId().equals(appId) && id.equals(channel.getUserId())){
                channels.add(socketChannelMap.get(channel));
            }
        });

        return channels;
    }
    public static void removeUserSession(NioSocketChannel nioSocketChannel){
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) nioSocketChannel
                .attr(AttributeKey.valueOf(Constants.Imei)).get();
        SessionSocketHolder.remove(new UserClientDTO(appId,userId,clientType,imei));
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<Object, Object> map = redissonClient.getMap(appId +
                Constants.RedisConstants.UserSessionConstants + userId);
        map.remove(clientType+":"+imei);

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setAppId(appId);
        messageHeader.setImei(imei);
        messageHeader.setClientType(clientType);

        UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
        userStatusChangeNotifyPack.setAppId(appId);
        userStatusChangeNotifyPack.setUserId(userId);
        userStatusChangeNotifyPack.setStatus(ConnectStatusEnum.OFFLINE_STATUS.getCode());
        MqMessageProducer.sendMessage(userStatusChangeNotifyPack,messageHeader, UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());

        nioSocketChannel.close();
    }
    public static void offlineUserSession(NioSocketChannel channelHandlerContext) {
        String userId = (String) channelHandlerContext.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) channelHandlerContext.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) channelHandlerContext.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) channelHandlerContext.attr(AttributeKey.valueOf(Constants.Imei)).get();
        SessionSocketHolder.remove(new UserClientDTO(appId,userId,clientType,imei));
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String , String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        String sessionStr = map.get(clientType.toString() + ":" + imei);
        if(StringUtils.isNotBlank(sessionStr)){
            UserSession userSession = JSONObject.parseObject(sessionStr, UserSession.class);
            userSession.setConnectState(ConnectStatusEnum.OFFLINE_STATUS.getCode());
            map.put(clientType.toString() , JSONObject.toJSONString(userSession));
        }
        channelHandlerContext.closeFuture();
    }

}
