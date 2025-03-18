package com.stupm.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stupm.common.ResponseVO;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.command.FriendshipEventCommand;
import com.stupm.common.model.ClientInfo;
import com.stupm.message.codec.pack.friendship.AddFriendGroupMemberPack;
import com.stupm.message.codec.pack.friendship.DeleteFriendGroupMemberPack;
import com.stupm.service.friendship.dao.FriendShipGroupMemberEntity;
import com.stupm.service.friendship.dao.FriendshipGroupEntity;
import com.stupm.service.friendship.dao.mapper.FriendShipGroupMemberMapper;
import com.stupm.service.friendship.model.req.AddFriendshipGroupMemberReq;
import com.stupm.service.friendship.model.req.DeleteFriendshipGroupMemberReq;
import com.stupm.service.friendship.model.resp.UpdateGroupMemberResp;
import com.stupm.service.friendship.service.FriendshipGroupMemberService;
import com.stupm.service.friendship.service.FriendshipGroupService;
import com.stupm.service.sequence.RedisSequence;
import com.stupm.service.user.dao.UserDataEntity;
import com.stupm.service.user.service.UserService;
import com.stupm.service.utils.MessageProducer;
import com.stupm.service.utils.WriteUserSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendshipGroupMemberServiceImpl implements FriendshipGroupMemberService {

    @Autowired
    FriendshipGroupService friendshipGroupService;

    @Autowired
    FriendShipGroupMemberMapper friendShipGroupMemberMapper;

    @Autowired
    UserService userService;

    @Autowired
    FriendshipGroupMemberService thisService;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    WriteUserSequence writeUserSequence;

    @Autowired
    RedisSequence redisSequence;


    @Override
    public ResponseVO addGroupMember(AddFriendshipGroupMemberReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
        ResponseVO<FriendshipGroupEntity> group = friendshipGroupService.getGroup(req.getFromId(), req.getGroupName(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        List<String> successIds = new ArrayList<>();
        List<String> errorIds = new ArrayList<>();

        for(String toId : req.getToIds()){
            ResponseVO<UserDataEntity> singleUserInfo = userService.getSingleUserInfo(toId, req.getAppId());
            if(singleUserInfo.isOk()){
                int i = thisService.doAddGroupMember(group.getData().getGroupId(), toId);
                if(i == 1){
                    successIds.add(toId);
                }else {
                    errorIds.add(toId);
                }
            }

        }
        UpdateGroupMemberResp resp = new UpdateGroupMemberResp();
        resp.setSuccessIds(successIds);
        resp.setErrorIds(errorIds);
        writeUserSequence.writeUserSequence(req.getAppId() , req.getFromId() , Constants.SeqConstants.FriendshipGroup , sequence);


        AddFriendGroupMemberPack addFriendGroupMemberPack = new AddFriendGroupMemberPack();
        addFriendGroupMemberPack.setSequence(sequence);
        addFriendGroupMemberPack.setFromId(req.getFromId());
        addFriendGroupMemberPack.setGroupName(req.getGroupName());
        addFriendGroupMemberPack.setToIds(successIds);
        messageProducer.sendToUserAnotherClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_MEMBER_ADD,addFriendGroupMemberPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));


        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO delGroupMember(DeleteFriendshipGroupMemberReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
        ResponseVO<FriendshipGroupEntity> group = friendshipGroupService.getGroup(req.getFromId(), req.getGroupName(), req.getAppId());
        if(!group.isOk()){
            return group;
        }

        List<String> successIds = new ArrayList<>();
        List<String> errorIds = new ArrayList<>();

        for(String toId : req.getToIds()){
            ResponseVO<UserDataEntity> singleUserInfo = userService.getSingleUserInfo(toId, req.getAppId());
            if(!singleUserInfo.isOk()){
                int i = deleteGroupMember(group.getData().getGroupId(), toId);
                if(i == 0) {
                    successIds.add(toId);
                }else{
                    errorIds.add(toId);
                }
            }
        }
        writeUserSequence.writeUserSequence(req.getAppId() , req.getFromId() , Constants.SeqConstants.FriendshipGroup , sequence);
        UpdateGroupMemberResp resp = new UpdateGroupMemberResp();
        resp.setSuccessIds(successIds);
        resp.setErrorIds(errorIds);

        DeleteFriendGroupMemberPack deleteFriendGroupMemberPack = new DeleteFriendGroupMemberPack();
        deleteFriendGroupMemberPack.setSequence(sequence);
        deleteFriendGroupMemberPack.setFromId(req.getFromId());
        deleteFriendGroupMemberPack.setGroupName(req.getGroupName());
        deleteFriendGroupMemberPack.setToIds(successIds);
        messageProducer.sendToUserAnotherClient(req.getFromId(),FriendshipEventCommand.FRIEND_GROUP_MEMBER_DELETE,deleteFriendGroupMemberPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

        return ResponseVO.successResponse(resp);
    }


    public int deleteGroupMember(Long groupId , String toId){
        QueryWrapper<FriendShipGroupMemberEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", groupId);
        queryWrapper.eq("to_id", toId);

        try{
            return friendShipGroupMemberMapper.delete(queryWrapper);
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }

    }


    @Override
    @Transactional
    public int doAddGroupMember(Long groupId, String toId) {
        FriendShipGroupMemberEntity entity = new FriendShipGroupMemberEntity();
        entity.setGroupId(groupId);
        entity.setToId(toId);
        try{
            int insert = friendShipGroupMemberMapper.insert(entity);
            return insert;
        }catch(Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int clearGroupMember(Long groupId) {
        QueryWrapper<FriendShipGroupMemberEntity> query = new QueryWrapper<>();
        query.eq("group_id",groupId);
        int delete = friendShipGroupMemberMapper.delete(query);
        return delete;
    }
}
