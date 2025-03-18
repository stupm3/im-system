package com.stupm.service.friendship.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stupm.common.ResponseVO;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.DelFlagEnum;
import com.stupm.common.enums.FriendshipErrorCode;
import com.stupm.common.enums.command.FriendshipEventCommand;
import com.stupm.common.model.ClientInfo;
import com.stupm.message.codec.pack.friendship.AddFriendGroupPack;
import com.stupm.message.codec.pack.friendship.DeleteFriendGroupPack;
import com.stupm.service.friendship.dao.FriendshipGroupEntity;
import com.stupm.service.friendship.dao.mapper.FriendShipGroupMapper;
import com.stupm.service.friendship.model.req.AddFriendshipGroupMemberReq;
import com.stupm.service.friendship.model.req.AddFriendshipGroupReq;
import com.stupm.service.friendship.model.req.DeleteFriendshipGroupReq;
import com.stupm.service.friendship.service.FriendshipGroupMemberService;
import com.stupm.service.friendship.service.FriendshipGroupService;
import com.stupm.service.sequence.RedisSequence;
import com.stupm.service.utils.MessageProducer;
import com.stupm.service.utils.WriteUserSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FriendshipGroupServiceImpl implements FriendshipGroupService {

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    FriendShipGroupMapper friendShipGroupMapper;

    @Autowired
    FriendshipGroupMemberService friendshipGroupMemberService;

    @Autowired
    RedisSequence redisSequence;

    @Autowired
    WriteUserSequence writeUserSequence;

    @Override
    public ResponseVO addGroup(AddFriendshipGroupReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);

        QueryWrapper<FriendshipGroupEntity> query = new QueryWrapper<>();
        query.eq("group_name", req.getGroupName());
        query.eq("app_id", req.getAppId());
        query.eq("from_id",req.getFromId());
        FriendshipGroupEntity entity = friendShipGroupMapper.selectOne(query);
        if(entity != null) {
            if(entity.getDelFlag() == DelFlagEnum.DELETE.getCode()){
                FriendshipGroupEntity update = new FriendshipGroupEntity();
                update.setDelFlag(DelFlagEnum.NORMAL.getCode());
                update.setFromId(req.getFromId());
                update.setGroupId(entity.getGroupId());
                update.setSequence(sequence);
                friendShipGroupMapper.updateById(update);
                writeUserSequence.writeUserSequence(req.getAppId() , req.getFromId() , Constants.SeqConstants.FriendshipGroup , sequence);
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);

        }
        FriendshipGroupEntity insert = new FriendshipGroupEntity();
        insert.setAppId(req.getAppId());
        insert.setGroupName(req.getGroupName());
        insert.setCreateTime(System.currentTimeMillis());
        insert.setUpdateTime(System.currentTimeMillis());
        insert.setDelFlag(DelFlagEnum.NORMAL.getCode());
        insert.setFromId(req.getFromId());
        insert.setSequence(sequence);
        try{
            int number = friendShipGroupMapper.insert(insert);
            writeUserSequence.writeUserSequence(req.getAppId() , req.getFromId() , Constants.SeqConstants.FriendshipGroup , sequence);
            if(number != 1) {
                return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_SHIP_GROUP_CREATE_ERROR);
            }
            if(CollectionUtil.isNotEmpty(req.getToIds())){
                AddFriendshipGroupMemberReq addFriendshipGroupMemberReq = new AddFriendshipGroupMemberReq();
                addFriendshipGroupMemberReq.setFromId(req.getFromId());
                addFriendshipGroupMemberReq.setToIds(req.getToIds());
                addFriendshipGroupMemberReq.setGroupName(req.getGroupName());
                addFriendshipGroupMemberReq.setAppId(req.getAppId());
                friendshipGroupMemberService.addGroupMember(addFriendshipGroupMemberReq);
                return ResponseVO.successResponse();

            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        AddFriendGroupPack addFriendGroupPack = new AddFriendGroupPack();
        addFriendGroupPack.setSequence(sequence);
        addFriendGroupPack.setFromId(req.getFromId());
        addFriendGroupPack.setGroupName(req.getGroupName());
        messageProducer.sendToUserAnotherClient(req.fromId, FriendshipEventCommand.FRIEND_GROUP_ADD,addFriendGroupPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getFromId()));

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteGroup(DeleteFriendshipGroupReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
        for(String groupName : req.getGroupName()){
            QueryWrapper<FriendshipGroupEntity> query = new QueryWrapper<>();
            query.eq("group_name", groupName);
            query.eq("del_flag" , DelFlagEnum.NORMAL);
            query.eq("from_id",req.getFromId());
            query.eq("app_id",req.getAppId());
            FriendshipGroupEntity entity = friendShipGroupMapper.selectOne(query);
            if(entity != null) {
                FriendshipGroupEntity update = new FriendshipGroupEntity();
                update.setSequence(sequence);
                update.setDelFlag(DelFlagEnum.DELETE.getCode());
                update.setGroupId(entity.getGroupId());
                update.setUpdateTime(System.currentTimeMillis());
                friendShipGroupMapper.updateById(update);
                friendshipGroupMemberService.clearGroupMember(entity.getGroupId());
                writeUserSequence.writeUserSequence(req.getAppId() , req.getFromId() , Constants.SeqConstants.FriendshipGroup , sequence);
                DeleteFriendGroupPack deleteFriendGroupPack = new DeleteFriendGroupPack();
                deleteFriendGroupPack.setSequence(sequence);
                deleteFriendGroupPack.setFromId(req.getFromId());
                deleteFriendGroupPack.setGroupName(groupName);
                messageProducer.sendToUserAnotherClient(req.getFromId(),FriendshipEventCommand.FRIEND_GROUP_DELETE,deleteFriendGroupPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getFromId()));
            }
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO<FriendshipGroupEntity> getGroup(String fromId, String groupName, Integer appId) {
        QueryWrapper<FriendshipGroupEntity> friendshipGroupEntityQueryWrapper = new QueryWrapper<>();
        friendshipGroupEntityQueryWrapper.eq("from_id", fromId);
        friendshipGroupEntityQueryWrapper.eq("group_name", groupName);
        friendshipGroupEntityQueryWrapper.eq("app_id", appId);
        FriendshipGroupEntity entity = friendShipGroupMapper.selectOne(friendshipGroupEntityQueryWrapper);
        return ResponseVO.successResponse(entity);
    }

    @Override
    public Long updateSeq(String fromId, String groupName, Integer appId) {
        return 0L;
    }
}
