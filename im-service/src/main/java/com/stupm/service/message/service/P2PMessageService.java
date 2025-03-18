package com.stupm.service.message.service;


import cn.hutool.core.collection.CollectionUtil;
import com.stupm.common.ResponseVO;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.ConversationTypeEnum;
import com.stupm.common.enums.command.MessageCommand;
import com.stupm.common.model.ClientInfo;
import com.stupm.common.model.message.MessageReceiveAckPack;
import com.stupm.common.model.message.OfflineMessageContent;
import com.stupm.message.codec.pack.message.ChatMessageAck;
import com.stupm.common.model.message.MessageContent;
import com.stupm.message.codec.proto.MessageReceiveServerAckPack;
import com.stupm.service.message.model.req.SendMessageReq;
import com.stupm.service.message.model.resp.SendMessageResp;
import com.stupm.service.sequence.RedisSequence;
import com.stupm.service.utils.ConversationIdGenerate;
import com.stupm.service.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class P2PMessageService {

    private static final Logger logger = LoggerFactory.getLogger(P2PMessageService.class);

    @Autowired
    CheckSendMessageService checkSendMessageService;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    MessageStoreService messageStoreService;

    @Autowired
    RedisSequence redisSequence;

    private final ThreadPoolExecutor threadPoolExecutor;
    {
        AtomicInteger atomicInteger = new AtomicInteger(0);

        threadPoolExecutor = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("message-process-thread" + atomicInteger.getAndIncrement());
                return thread;
            }
        });
    }
    public void process(MessageContent messageContext){
        MessageContent messageFromMessageIdCache = messageStoreService.getMessageFromMessageIdCache(messageContext.getAppId(),messageContext.getMessageId(),MessageContent.class);
        if(messageFromMessageIdCache != null){
            threadPoolExecutor.execute(()->{
                ack(messageFromMessageIdCache , ResponseVO.successResponse());
                syncToSender(messageFromMessageIdCache);
                List<ClientInfo> dispatch = dispatch(messageFromMessageIdCache);
                if(CollectionUtil.isNotEmpty(dispatch)){
                    receiveAck(messageContext);
                }

            });
            return;
        }
        String key = messageContext.getAppId() +":"+ Constants.SeqConstants.Message+":" + ConversationIdGenerate.generateP2PId(messageContext.getFromId() , messageContext.getToId());
        Long sequence = redisSequence.getSequence(key);
        messageContext.setMessageSequence(sequence );

        threadPoolExecutor.execute(()->{
            messageStoreService.storeP2PMessage(messageContext);
            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(messageContext, offlineMessageContent);
            offlineMessageContent.setConversationType(ConversationTypeEnum.P2P.getCode());
            messageStoreService.storeOfflineMessage(offlineMessageContent);
                ack(messageContext , ResponseVO.successResponse());
                syncToSender(messageContext);
                List<ClientInfo> dispatch = dispatch(messageContext);
                messageStoreService.setMessageFromMessageIdCache(messageContext.getAppId(),messageContext.getMessageId(),messageContext);
                if(CollectionUtil.isEmpty(dispatch)){
                    receiveAck(messageContext);
                }
            });

    }

    private List<ClientInfo> dispatch(MessageContent messageContext){
        List<ClientInfo> clientInfos = messageProducer.sendToUser(messageContext.getAppId(), messageContext.getToId(), MessageCommand.MSG_P2P, messageContext);
        return clientInfos;
    }

    private void ack(MessageContent messageContext,ResponseVO resultVO){
        logger.info("msg ack ,msgId:{},checkResult:{}",messageContext.getMessageId(), resultVO.getCode());
        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContext.getMessageId(),messageContext.getMessageSequence());
        resultVO.setData(chatMessageAck);
        messageProducer.sendToUser(messageContext.getFromId() , MessageCommand.MSG_ACK , resultVO , messageContext);
    }

    public void receiveAck(MessageContent messageContext){
        MessageReceiveServerAckPack pack = new MessageReceiveServerAckPack();
        pack.setFromId(messageContext.getToId());
        pack.setToId(messageContext.getFromId());
        pack.setMessageKey(messageContext.getMessageKey());
        pack.setMessageSequence(messageContext.getMessageSequence());
        pack.setServerSend(true);
        messageProducer.sendToUser(messageContext.getFromId() , MessageCommand.MSG_RECIVE_ACK , pack , new ClientInfo(messageContext.getAppId(),messageContext.getClientType(),messageContext.getImei()));

    }

    private void syncToSender(MessageContent messageContext){
        messageProducer.sendToUserAnotherClient(messageContext.getFromId(),MessageCommand.MSG_P2P,messageContext,messageContext);
    }

    public ResponseVO serverPermissionCheck(String fromId, String toId , Integer appId){
        ResponseVO responseVO = checkSendMessageService.checkSenderForbiddenAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }
        ResponseVO friendshipCheckResponse = checkSendMessageService.checkFriendship(fromId, toId, appId);
        if(!friendshipCheckResponse.isOk()){
            return friendshipCheckResponse;
        }
        return friendshipCheckResponse;
    }

    public SendMessageResp send(SendMessageReq req){
        SendMessageResp sendMessageResp = new SendMessageResp();
        MessageContent message = new MessageContent();
        BeanUtils.copyProperties(req,message);
        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());
        messageStoreService.storeP2PMessage(message);
        syncToSender(message);
        dispatch(message);
        return sendMessageResp;
    }
}
