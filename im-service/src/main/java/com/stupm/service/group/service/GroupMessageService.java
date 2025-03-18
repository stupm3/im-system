package com.stupm.service.group.service;

import com.stupm.common.ResponseVO;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.command.GroupEventCommand;
import com.stupm.common.enums.command.MessageCommand;
import com.stupm.common.model.message.GroupChatMessageContent;
import com.stupm.common.model.message.MessageContent;
import com.stupm.common.model.message.OfflineMessageContent;
import com.stupm.message.codec.pack.message.ChatMessageAck;
import com.stupm.service.group.model.req.SendGroupMessageReq;
import com.stupm.service.message.model.resp.SendMessageResp;
import com.stupm.service.message.service.CheckSendMessageService;
import com.stupm.service.message.service.MessageStoreService;
import com.stupm.service.sequence.RedisSequence;
import com.stupm.service.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GroupMessageService {
    private static final Logger logger = LoggerFactory.getLogger(GroupMessageService.class);

    @Autowired
    private CheckSendMessageService checkSendMessageService;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private GroupMemberService groupMemberService;

    @Autowired
    private MessageStoreService messageStoreService;

    @Autowired
    private RedisSequence redisSequence;

    private final ThreadPoolExecutor threadPoolExecutor;

    private AtomicInteger atomicInteger = new AtomicInteger(0);

    {
        threadPoolExecutor = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("message-group-thread-" + atomicInteger.getAndIncrement());
                return t;
            }
        });
    }

    public void process(GroupChatMessageContent messageContext){
        Long sequence = redisSequence.getSequence(messageContext.getAppId() + ":" + Constants.SeqConstants.GroupMessage + messageContext.getGroupId());
        GroupChatMessageContent msgContent = messageStoreService.getMessageFromMessageIdCache(messageContext.getAppId(), messageContext.getMessageId(), GroupChatMessageContent.class);
        if(msgContent != null){
            threadPoolExecutor.execute(()->{
                ack(messageContext , ResponseVO.successResponse());
                syncToSender(messageContext);
                dispatch(messageContext);
            });
        }
        messageContext.setMessageSequence(sequence);
            threadPoolExecutor.execute(()->{
                redisSequence.getSequence(messageContext.getAppId() + ":" + Constants.SeqConstants.GroupMessage+ messageContext.getGroupId() );
                List<String> groupMemberId = groupMemberService.getGroupMemberId(messageContext.getGroupId(), messageContext.getAppId());
                OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
                BeanUtils.copyProperties(messageContext, offlineMessageContent);
                offlineMessageContent.setToId(messageContext.getGroupId());
                messageContext.setGroupMemberIds(groupMemberId);
                messageStoreService.storeGroupOfflineMessage(offlineMessageContent, groupMemberId);
                messageStoreService.storeGroupMessage(messageContext);
                ack(messageContext , ResponseVO.successResponse());
                syncToSender(messageContext);
                dispatch(messageContext);
                messageStoreService.setMessageFromMessageIdCache(messageContext.getAppId(), messageContext.getMessageId(),messageContext);
            });
    }

    private void dispatch(GroupChatMessageContent messageContext){
        for(String memberId : messageContext.getGroupMemberIds()){
            if(!memberId.equals(messageContext.getFromId())){
                messageProducer.sendToUser(messageContext.getAppId(),memberId, GroupEventCommand.MSG_GROUP,messageContext);
            }
        }
    }

    private void ack(GroupChatMessageContent messageContext,ResponseVO resultVO){
        logger.info("msg ack ,msgId:{},checkResult:{}",messageContext.getMessageId(), resultVO.getCode());
        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContext.getMessageId());
        resultVO.setData(chatMessageAck);
        messageProducer.sendToUser(messageContext.getFromId() , GroupEventCommand.GROUP_MSG_ACK, resultVO , messageContext);
    }

    private void syncToSender(GroupChatMessageContent messageContext){
        messageProducer.sendToUserAnotherClient(messageContext.getFromId(),GroupEventCommand.MSG_GROUP,messageContext,messageContext);
    }

    public ResponseVO serverPermissionCheck(String fromId, String groupId,Integer appId){
        return checkSendMessageService.checkGroupMessage(fromId, groupId,appId);
    }

    public SendMessageResp send(SendGroupMessageReq req) {
        SendMessageResp resp = new SendMessageResp();
        GroupChatMessageContent messageContext = new GroupChatMessageContent();
        BeanUtils.copyProperties(req, messageContext);
        resp.setMessageKey(messageContext.getMessageKey());
        resp.setMessageTime(System.currentTimeMillis());
        messageStoreService.storeGroupMessage(messageContext);
        syncToSender(messageContext);
        dispatch(messageContext);
        return resp;
    }

}
