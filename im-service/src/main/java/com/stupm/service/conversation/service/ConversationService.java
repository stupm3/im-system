package com.stupm.service.conversation.service;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stupm.common.ResponseVO;
import com.stupm.common.config.AppConfig;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.ConversationErrorCode;
import com.stupm.common.enums.command.ConversationEventCommand;
import com.stupm.common.model.ClientInfo;
import com.stupm.common.model.SyncReq;
import com.stupm.common.model.SyncResp;
import com.stupm.common.model.message.MessageReadContent;
import com.stupm.message.codec.pack.conversation.DeleteConversationPack;
import com.stupm.message.codec.pack.conversation.UpdateConversationPack;
import com.stupm.service.conversation.dao.ConversationSetEntity;
import com.stupm.service.conversation.dao.mapper.ConversationSetMapper;
import com.stupm.service.conversation.model.DeleteConversationReq;
import com.stupm.service.conversation.model.UpdateConversationReq;
import com.stupm.service.friendship.dao.FriendshipEntity;
import com.stupm.service.sequence.RedisSequence;
import com.stupm.service.utils.MessageProducer;
import com.stupm.service.utils.WriteUserSequence;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {
    @Autowired
    private ConversationSetMapper conversationSetMapper;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private RedisSequence redisSequence;

    @Autowired
    private WriteUserSequence writeUserSequence;

    public String convertConversationId(String fromId,String toId,Integer type){
        return type + "_"  + fromId + "_" + toId;
    }

    public void messageMarkRead(MessageReadContent messageReadContent) {
        Long sequence = redisSequence.getSequence(messageReadContent.getAppId() + ":" + Constants.SeqConstants.Conversation);
        String id = convertConversationId(messageReadContent.getFromId(), messageReadContent.getToId(), messageReadContent.getConversationType());
        QueryWrapper<ConversationSetEntity> objectQueryWrapper = new QueryWrapper<>();
        objectQueryWrapper.eq("conversation_id", id);
        objectQueryWrapper.eq("app_id", messageReadContent.getAppId());
        ConversationSetEntity conversationSetEntity = conversationSetMapper.selectOne(objectQueryWrapper);
        if(conversationSetEntity==null){
            conversationSetEntity = new ConversationSetEntity();
            conversationSetEntity.setConversationId(id);
            BeanUtils.copyProperties(messageReadContent, conversationSetEntity);
            conversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
            conversationSetEntity.setSequence(sequence);
            conversationSetEntity.setToId(messageReadContent.getToId());
            conversationSetMapper.insert(conversationSetEntity);
        }
        else{
            conversationSetEntity.setSequence(sequence);
            conversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
            conversationSetMapper.readMark(conversationSetEntity);
        }
        writeUserSequence.writeUserSequence(messageReadContent.getAppId(),messageReadContent.getFromId(),Constants.SeqConstants.Conversation,sequence);

    }
    public ResponseVO deleteConversation(DeleteConversationReq req){
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.FriendshipRequest);
        QueryWrapper<ConversationSetEntity> query = new QueryWrapper<>();
        query.eq("conversation_id", req.getConversationId());
        query.eq("app_id", req.getAppId());
        ConversationSetEntity conversationSetEntity = conversationSetMapper.selectOne(query);
        if(conversationSetEntity!=null){
            conversationSetEntity.setSequence(sequence);
            conversationSetEntity.setIsMute(0);
            conversationSetEntity.setIsTop(0);
            conversationSetMapper.update(conversationSetEntity,query);
            writeUserSequence.writeUserSequence(req.getAppId(),req.getFormId(),Constants.SeqConstants.Conversation,sequence);
        }
        if(appConfig.getDeleteConversationSyncMode() == 1){
            DeleteConversationPack pack = new DeleteConversationPack();
            pack.setConversationId(req.getConversationId());
            messageProducer.sendToUserAnotherClient(req.getFormId(), ConversationEventCommand.CONVERSATION_DELETE,pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
        }
        return ResponseVO.successResponse();
    }

    public ResponseVO updateConversation(UpdateConversationReq req){
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.FriendshipRequest);
        if(req.getIsMute() == null && req.getIsMute() == null){
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_UPDATE_PARAM_ERROR);
        }
        QueryWrapper<ConversationSetEntity> query = new QueryWrapper<>();
        query.eq("conversation_id", req.getConversationId());
        query.eq("app_id", req.getAppId());
        ConversationSetEntity conversationSetEntity = conversationSetMapper.selectOne(query);
        if(conversationSetEntity == null){
            if(req.getIsMute() != null){
                conversationSetEntity.setIsMute(req.getIsMute());
            }
            if(req.getIsTop() != null){
                conversationSetEntity.setIsTop(req.getIsTop());
            }
            conversationSetEntity.setSequence(sequence);
            conversationSetMapper.update(conversationSetEntity,query);
            writeUserSequence.writeUserSequence(req.getAppId(),req.getFromId(),Constants.SeqConstants.Conversation,sequence);
            UpdateConversationPack pack = new UpdateConversationPack();
            pack.setSequence(sequence);
            pack.setConversationId(req.getConversationId());
            pack.setIsMute(req.getIsMute());
            pack.setIsTop(req.getIsTop());
            pack.setConversationType(conversationSetEntity.getConversationType());
            messageProducer.sendToUserAnotherClient(req.getFromId(), ConversationEventCommand.CONVERSATION_DELETE,pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
        }
        return ResponseVO.successResponse();
    }

    public ResponseVO syncConversation(SyncReq req) {
        if(req.getMaxLimit() > 100){
            req.setMaxLimit(100);
        }
        SyncResp<ConversationSetEntity> resp = new SyncResp();
        QueryWrapper<ConversationSetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getOperator());
        queryWrapper.gt("sequence",req.getLastSeq());
        queryWrapper.last(" limit " + req.getMaxLimit());
        queryWrapper.orderByDesc("sequence");
        List<ConversationSetEntity> list = conversationSetMapper.selectList(queryWrapper);
        if(!CollectionUtil.isEmpty(list)){
            ConversationSetEntity friendshipEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            Long maxSeq = conversationSetMapper.getConversationSetMaxSeq(req.getAppId(), req.getOperator());
            resp.setMaxSeq(maxSeq);
            resp.setCompleted(friendshipEntity.getSequence().equals(maxSeq));
            return ResponseVO.successResponse(resp);
        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }
}
