package com.stupm.message.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.stupm.common.ResponseVO;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.ConnectStatusEnum;
import com.stupm.common.enums.command.GroupEventCommand;
import com.stupm.common.enums.command.MessageCommand;
import com.stupm.common.enums.command.SystemCommand;
import com.stupm.common.enums.command.UserEventCommand;
import com.stupm.common.model.UserClientDTO;
import com.stupm.common.model.UserSession;
import com.stupm.common.model.message.CheckSendMessageRequest;
import com.stupm.message.codec.pack.LoginPack;
import com.stupm.message.codec.pack.message.ChatMessageAck;
import com.stupm.message.codec.pack.user.LoginAckPack;
import com.stupm.message.codec.pack.user.UserStatusChangeNotifyPack;
import com.stupm.message.codec.proto.Message;
import com.stupm.message.codec.proto.MessagePack;
import com.stupm.message.tcp.feign.FeignMessageService;
import com.stupm.message.tcp.publish.MqMessageProducer;
import com.stupm.message.tcp.redis.RedisManager;
import com.stupm.message.tcp.utils.SessionSocketHolder;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    private Integer brokerId;

    private FeignMessageService feignMessageService;


    public NettyServerHandler(Integer brokerId , String logicUrl) {
        this.brokerId = brokerId;
        feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(FeignMessageService.class, logicUrl);
    }




    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) throws Exception {
        Integer command = message.getMessageHeader().getCommand();
        if(command == SystemCommand.LOGIN.getCommand()){
            LoginPack loginPack = JSON.parseObject(JSONObject.toJSONString(message.getMessagePack()), new TypeReference<LoginPack>() {
            }.getType());
            channelHandlerContext.attr(AttributeKey.valueOf(Constants.UserId)).set(loginPack.getUserId());
            channelHandlerContext.attr(AttributeKey.valueOf(Constants.AppId)).set(message.getMessageHeader().getAppId());
            channelHandlerContext.attr(AttributeKey.valueOf(Constants.ClientType)).set(message.getMessageHeader().getClientType());
            channelHandlerContext.attr(AttributeKey.valueOf(Constants.Imei)).set(message.getMessageHeader().getImei());

            UserSession userSession = new UserSession();
            userSession.setBrokerId(brokerId);
            try{
                InetAddress localHost = InetAddress.getLocalHost();
                userSession.setBrokerHost(localHost.getHostAddress());
            }catch (Exception e){
                e.printStackTrace();
            }
            userSession.setAppId(message.getMessageHeader().getAppId());
            userSession.setClientType(message.getMessageHeader().getClientType());
            userSession.setUserId(loginPack.getUserId());
            userSession.setConnectState(ConnectStatusEnum.ONLINE_STATUS.getCode());
            userSession.setImei(message.getMessageHeader().getImei());
            RedissonClient redissonClient = RedisManager.getRedissonClient();
            RMap<String , String> map = redissonClient.getMap(message.getMessageHeader().getAppId() + Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());
            map.put(message.getMessageHeader().getClientType() +":" + message.getMessageHeader().getImei() , JSONObject.toJSONString(userSession));

            SessionSocketHolder.put( new UserClientDTO(message.getMessageHeader().getAppId(),
                    loginPack.getUserId(),
                    message.getMessageHeader().getClientType(),message.getMessageHeader().getImei())
                    ,(NioSocketChannel) channelHandlerContext.channel());

            UserClientDTO userClientDTO = new UserClientDTO();
            userClientDTO.setUserId(loginPack.getUserId());
            userClientDTO.setImei(message.getMessageHeader().getImei());
            userClientDTO.setAppId(message.getMessageHeader().getAppId());
            userClientDTO.setClientType(message.getMessageHeader().getClientType());
            RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
            topic.publish(JSON.toJSONString(userClientDTO));

            UserStatusChangeNotifyPack pack = new UserStatusChangeNotifyPack();
            pack.setAppId(message.getMessageHeader().getAppId());
            pack.setUserId(loginPack.getUserId());
            pack.setStatus(ConnectStatusEnum.ONLINE_STATUS.getCode());
            MqMessageProducer.sendMessage(pack,message.getMessageHeader(), UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());

            MessagePack<LoginAckPack> loginSuccess = new MessagePack<>();
            LoginAckPack loginAckPack = new LoginAckPack();
            loginAckPack.setUserId(loginPack.getUserId());
            loginSuccess.setCommand(SystemCommand.LOGIN_ACK.getCommand());
            loginSuccess.setImei(message.getMessageHeader().getImei());
            loginSuccess.setData(loginAckPack);
            loginSuccess.setAppId(message.getMessageHeader().getAppId());
            loginSuccess.setClientType(message.getMessageHeader().getClientType());
            channelHandlerContext.writeAndFlush(loginSuccess);

        }else if(command == SystemCommand.LOGOUT.getCommand()){
            SessionSocketHolder.removeUserSession((NioSocketChannel) channelHandlerContext.channel());
        }else if(command == SystemCommand.PING.getCommand()){
            channelHandlerContext.attr(AttributeKey.valueOf(Constants.LastReadTime)).set(System.currentTimeMillis());

        }else if(command == MessageCommand.MSG_P2P.getCommand() || command == GroupEventCommand.MSG_GROUP.getCommand()){
            try {
                CheckSendMessageRequest checkSendMessageRequest = new CheckSendMessageRequest();
                checkSendMessageRequest.setAppId(message.getMessageHeader().getAppId());
                checkSendMessageRequest.setCommand(message.getMessageHeader().getCommand());
                JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(message.getMessagePack()));
                String fromId = jsonObject.getString("fromId");
                String toId;
                if(command == MessageCommand.MSG_P2P.getCommand()){
                    toId = jsonObject.getString("toId");
                }else{
                    toId = jsonObject.getString("groupId");
                }
                checkSendMessageRequest.setFromId(fromId);
                checkSendMessageRequest.setToId(toId);
                ResponseVO responseVO = feignMessageService.checkSendMessage(checkSendMessageRequest);
                if(responseVO.isOk()){
                    MqMessageProducer.sendMessage(message , command);
                }else {
                    Integer ackCommand;
                    if(command == MessageCommand.MSG_P2P.getCommand()){
                        ackCommand = MessageCommand.MSG_ACK.getCommand();
                    }else{
                        ackCommand = GroupEventCommand.GROUP_MSG_ACK.getCommand();
                    }
                    ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString("messageId"));
                    responseVO.setData(chatMessageAck);
                    MessagePack<ResponseVO> messagePack = new MessagePack();
                    messagePack.setData(responseVO);
                    messagePack.setCommand(ackCommand);
                    channelHandlerContext.writeAndFlush(messagePack);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        else{
            MqMessageProducer.sendMessage(message , command);
        }

    }


}
