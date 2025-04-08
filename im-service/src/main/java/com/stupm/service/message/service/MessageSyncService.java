package com.stupm.service.message.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.stupm.common.ResponseVO;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.command.Command;
import com.stupm.common.enums.command.GroupEventCommand;
import com.stupm.common.enums.command.MessageCommand;
import com.stupm.common.model.ClientInfo;
import com.stupm.common.model.SyncReq;
import com.stupm.common.model.SyncResp;
import com.stupm.common.model.message.MessageContent;
import com.stupm.common.model.message.MessageReadContent;
import com.stupm.common.model.message.MessageReceiveAckPack;
import com.stupm.common.model.message.OfflineMessageContent;
import com.stupm.message.codec.pack.message.MessageReadPack;
import com.stupm.service.conversation.model.DeleteConversationReq;
import com.stupm.service.conversation.service.ConversationService;
import com.stupm.service.utils.MessageProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.jws.Oneway;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class MessageSyncService {
    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private RedisTemplate redisTemplate;

    public void receiveMark(MessageReceiveAckPack messageReceiveAckPack) {
        messageProducer.sendToUser(messageReceiveAckPack.getAppId(),messageReceiveAckPack.getToId(), MessageCommand.MSG_RECIVE_ACK,messageReceiveAckPack);
    }

    public void readMark(MessageReadContent messageContent) {
        conversationService.messageMarkRead(messageContent);
        MessageReadPack messageReadPack = new MessageReadPack();
        BeanUtils.copyProperties(messageContent,messageReadPack);
        syncToSender(messageReadPack, messageContent,MessageCommand.MSG_READED_NOTIFY);
        messageProducer.sendToUser(messageContent.getAppId(),messageContent.getToId(),MessageCommand.MSG_READED_RECEIPT,messageContent);
    }

    private void syncToSender(MessageReadPack pack, ClientInfo clientInfo, Command command) {
        messageProducer.sendToUserAnotherClient(pack.getToId(),command,pack,clientInfo);
    }

    public void groupReadMark(MessageReadContent messageContent) {
        conversationService.messageMarkRead(messageContent);
        MessageReadPack messageReadPack = new MessageReadPack();
        BeanUtils.copyProperties(messageContent,messageReadPack);
        syncToSender(messageReadPack,messageContent, GroupEventCommand.MSG_GROUP_READED_NOTIFY);
        if(!messageContent.getFromId().equals(messageReadPack.getToId())){
            messageProducer.sendToUser(messageContent.getAppId(),messageContent.getToId(),GroupEventCommand.MSG_GROUP_READED_RECEIPT,messageContent);
        }
    }


    public ResponseVO syncOfflineMessage(SyncReq req) {
        SyncResp<OfflineMessageContent> resp = new SyncResp<>();
        String key = req.getAppId() + ":" + Constants.SeqConstants.OfflineMessage+":" + req.getOperator() ;
        Long maxSeq = 0L;
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        Set set = zSetOperations.reverseRangeWithScores(key, 0, 0);
        if(!CollectionUtil.isEmpty(set)){
            List list = new ArrayList<>();
            DefaultTypedTuple o = (DefaultTypedTuple) list.get(0);
            maxSeq = o.getScore().longValue();
        }
        List<OfflineMessageContent> respList = new ArrayList<>();
        resp.setMaxSeq(maxSeq);
        Set<ZSetOperations.TypedTuple> querySet = zSetOperations.rangeByScoreWithScores(key, req.getLastSeq(), maxSeq, 0, req.getMaxLimit());
        for (ZSetOperations.TypedTuple<String> typedTuple : querySet) {
            String value = typedTuple.getValue();
            OfflineMessageContent offlineMessageContent = JSONObject.parseObject(value, OfflineMessageContent.class);
            respList.add(offlineMessageContent);
        }
        resp.setDataList(respList);
        if(!CollectionUtil.isEmpty(respList)){
            OfflineMessageContent offlineMessageContent = respList.get(respList.size() - 1);
            resp.setCompleted(maxSeq.equals(offlineMessageContent.getMessageSequence()));
        }
        return ResponseVO.successResponse(resp);
    }
}
