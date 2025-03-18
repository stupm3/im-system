package com.stupm.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.stupm.common.config.AppConfig;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.ConversationTypeEnum;
import com.stupm.common.enums.DelFlagEnum;
import com.stupm.common.model.message.*;
import com.stupm.service.conversation.service.ConversationService;
import com.stupm.service.message.dao.MessageBodyEntity;
import com.stupm.service.message.dao.mapper.MessageBodyMapper;
import com.stupm.service.message.dao.mapper.MessageHistoryMapper;
import com.stupm.service.utils.SnowflakeIdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
public class MessageStoreService {
    @Autowired
    private MessageHistoryMapper messageHistoryMapper;

    @Autowired
    private MessageBodyMapper messageBodyMapper;

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;


    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private AppConfig appConfig;

    @Transactional
    public void storeP2PMessage(MessageContent messageContent) {
        MessageBodyEntity messageBody = parseMessageBody(messageContent);
        messageContent.setMessageKey(messageBody.getMessageKey());
        StoreP2PMessageDTO dto = new StoreP2PMessageDTO();
        dto.setMessageContent(messageContent);
        MessageBody messageBody1 = new MessageBody();
        BeanUtils.copyProperties(messageBody, messageBody1);
        dto.setMessageBody(messageBody1);
        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreP2PMessage,"", JSONObject.toJSONString(dto));
    }

    @Transactional
    public void storeGroupMessage(GroupChatMessageContent messageContent) {
        MessageBodyEntity messageBodyEntity = parseMessageBody(messageContent);
        StoreGroupMessageDTO dto = new StoreGroupMessageDTO();
        dto.setMessageContent(messageContent);
        MessageBody messageBody1 = new MessageBody();
        BeanUtils.copyProperties(messageBodyEntity, messageBody1);
        dto.setMessageBody(messageBody1);
        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreGroupMessage,"", JSONObject.toJSONString(dto));
        messageContent.setMessageKey(messageBodyEntity.getMessageKey());
    }







    private MessageBodyEntity parseMessageBody(MessageContent messageContent) {
        MessageBodyEntity messageBodyEntity = new MessageBodyEntity();
        messageBodyEntity.setAppId(messageContent.getAppId());
        messageBodyEntity.setMessageKey(snowflakeIdWorker.nextId());
        messageBodyEntity.setCreateTime(System.currentTimeMillis());
        messageBodyEntity.setSecurityKey("");
        messageBodyEntity.setExtra(messageContent.getExtra());
        messageBodyEntity.setDelFlag(DelFlagEnum.NORMAL.getCode());
        messageBodyEntity.setMessageTime(messageContent.getMessageTime());
        messageBodyEntity.setMessageBody(messageContent.getMessageBody());
        return messageBodyEntity;
    }

    public void setMessageFromMessageIdCache(Integer appId,String messageId,Object messageContent) {
        String key = appId + ":" + Constants.RedisConstants.cacheMessage+ ":"  + messageId;
        stringRedisTemplate.opsForValue().set(key , JSONObject.toJSONString(messageContent),300 , TimeUnit.SECONDS);
    }
    public <T> T getMessageFromMessageIdCache(Integer appId, String messageId,Class<T> clazz) {
        String key = appId + ":" + Constants.RedisConstants.cacheMessage + ":" +messageId;
        String s = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.isBlank(s))
            return null;
        return JSONObject.parseObject(s, clazz);
    }


    public void storeOfflineMessage(OfflineMessageContent offlineMessage){

        // 找到fromId的队列
        String fromKey = offlineMessage.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + offlineMessage.getFromId();
        // 找到toId的队列
        String toKey = offlineMessage.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + offlineMessage.getToId();

        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        //判断 队列中的数据是否超过设定值
        if(operations.zCard(fromKey) > appConfig.getOfflineMessageCount()){
            operations.removeRange(fromKey,0,0);
        }
        offlineMessage.setConversationId(conversationService.convertConversationId(
               offlineMessage.getFromId(),offlineMessage.getToId(), ConversationTypeEnum.P2P.getCode()
        ));
        // 插入 数据 根据messageKey 作为分值
        operations.add(fromKey,JSONObject.toJSONString(offlineMessage),
                offlineMessage.getMessageKey());

        //判断 队列中的数据是否超过设定值
        if(operations.zCard(toKey) > appConfig.getOfflineMessageCount()){
            operations.removeRange(toKey,0,0);
        }

        offlineMessage.setConversationId(conversationService.convertConversationId(
               offlineMessage.getToId(),offlineMessage.getFromId(), ConversationTypeEnum.P2P.getCode()
        ));
        // 插入 数据 根据messageKey 作为分值
        operations.add(toKey,JSONObject.toJSONString(offlineMessage),
                offlineMessage.getMessageKey());

    }

    public void storeGroupOfflineMessage(OfflineMessageContent messageContent, List<String> memberIds){

        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        messageContent.setConversationType(ConversationTypeEnum.GROUP.getCode());

        for(String memberId : memberIds){
            String toKey =  messageContent.getAppId() + ":" + Constants.SeqConstants.OfflineMessage +":"+ memberId ;
            messageContent.setConversationId(conversationService.convertConversationId(memberId,messageContent.getToId(), ConversationTypeEnum.GROUP.getCode()));
            if(zSetOperations.zCard(toKey) > appConfig.getOfflineMessageCount()){
                zSetOperations.removeRange(toKey,0,0);
            }
            zSetOperations.add(toKey,JSONObject.toJSONString(messageContent),messageContent.getMessageKey());
        }


    }
}
