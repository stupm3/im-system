package com.stupm.messageStore.service;


import com.stupm.common.model.message.GroupChatMessageContent;
import com.stupm.common.model.message.MessageContent;
import com.stupm.messageStore.dao.GroupMessageHistoryEntity;
import com.stupm.messageStore.dao.MessageBodyEntity;
import com.stupm.messageStore.dao.MessageHistoryEntity;
import com.stupm.messageStore.dao.mapper.GroupMessageHistoryMapper;
import com.stupm.messageStore.dao.mapper.MessageBodyMapper;
import com.stupm.messageStore.dao.mapper.MessageHistoryMapper;
import com.stupm.messageStore.model.StoreGroupMessageDTO;
import com.stupm.messageStore.model.StoreP2PMessageDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class  StoreMessageService {
    @Autowired
    MessageHistoryMapper messageHistoryMapper;

    @Autowired
    MessageBodyMapper messageBodyMapper;

    @Autowired
    GroupMessageHistoryMapper groupMessageHistoryMapper;

    @Transactional
    public void doStoreP2PMessage(StoreP2PMessageDTO dto) {
        messageBodyMapper.insert(dto.getMessageBody());
        List<MessageHistoryEntity> messageHistoryEntities = parseMessageHistory(dto.getMessageContent(), dto.getMessageBody());
        messageHistoryMapper.insertBatchSomeColumn(messageHistoryEntities);

    }
    private List<MessageHistoryEntity> parseMessageHistory(MessageContent messageContent, MessageBodyEntity messageBody) {
        List<MessageHistoryEntity> list = new ArrayList<>();
        MessageHistoryEntity fromHistory = new MessageHistoryEntity();
        MessageHistoryEntity toHistory = new MessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, fromHistory);
        BeanUtils.copyProperties(messageContent, toHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        fromHistory.setMessageKey(messageBody.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());
        fromHistory.setSequence(messageContent.getMessageSequence());

        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(messageBody.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());
        toHistory.setSequence(messageContent.getMessageSequence());
        list.add(fromHistory);
        list.add(toHistory);
        return list;
    }

    @Transactional
    public void doStoreGroupMessage(StoreGroupMessageDTO dto) {
        messageBodyMapper.insert(dto.getMessageBody());
        GroupMessageHistoryEntity groupMessageHistoryEntity = parseMessageHistory(dto.getMessageContent(), dto.getMessageBody());
        groupMessageHistoryMapper.insert(groupMessageHistoryEntity);
    }
    private GroupMessageHistoryEntity parseMessageHistory(GroupChatMessageContent messageContent, MessageBodyEntity messageBody) {
        messageContent.setMessageKey(messageBody.getMessageKey());
        GroupMessageHistoryEntity result = new GroupMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, result);
        result.setMessageKey(messageBody.getMessageKey());
        result.setCreateTime(System.currentTimeMillis());

        return result;
    }
}
