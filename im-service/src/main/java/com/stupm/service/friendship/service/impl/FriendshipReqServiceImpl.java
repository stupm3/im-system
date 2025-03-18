package com.stupm.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stupm.common.ResponseVO;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.ApproveFriendRequestStatusEnum;
import com.stupm.common.enums.FriendshipErrorCode;
import com.stupm.common.enums.command.FriendshipEventCommand;
import com.stupm.message.codec.pack.friendship.ApproverFriendRequestPack;
import com.stupm.message.codec.pack.friendship.ReadAllFriendRequestPack;
import com.stupm.service.dto.FriendshipDTO;
import com.stupm.service.friendship.dao.FriendshipRequestEntity;
import com.stupm.service.friendship.dao.mapper.FriendShipRequestMapper;
import com.stupm.service.friendship.model.req.ApproveFriendRequestReq;
import com.stupm.service.friendship.model.req.ReadFriendshipRequestReq;
import com.stupm.service.friendship.service.FriendshipReqService;
import com.stupm.service.sequence.RedisSequence;
import com.stupm.service.utils.MessageProducer;
import com.stupm.service.utils.WriteUserSequence;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FriendshipReqServiceImpl implements FriendshipReqService {

    @Autowired
    FriendShipRequestMapper friendShipRequestMapper;

    @Autowired
    FriendshipServiceImpl friendshipService;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    RedisSequence redisSequence;

    @Autowired
    WriteUserSequence writeUserSequence;

    @Override
    public ResponseVO addFriendReq(String fromId,FriendshipDTO friendItem, Integer appId) {
        Long sequence = redisSequence.getSequence(appId + ":" + Constants.SeqConstants.FriendshipRequest);
        QueryWrapper<FriendshipRequestEntity> queryWrapper = new QueryWrapper();
        queryWrapper.eq("app_id", appId);
        queryWrapper.eq("from_id",fromId);
        queryWrapper.eq("to_id",friendItem.getToId());
        FriendshipRequestEntity request = friendShipRequestMapper.selectOne(queryWrapper);
        FriendshipRequestEntity requestEntity = new FriendshipRequestEntity();
        requestEntity.setReadStatus(0);
        BeanUtils.copyProperties(friendItem, requestEntity);
        requestEntity.setAppId(appId);
        requestEntity.setToId(fromId);
        requestEntity.setSequence(sequence);
        if(request != null){
            requestEntity.setUpdateTime(System.currentTimeMillis());
            friendShipRequestMapper.update(requestEntity,queryWrapper);
        }else{
            if(StringUtils.isNotBlank(friendItem.getRemark())){
                requestEntity.setRemark(friendItem.getRemark());
            }
            if(StringUtils.isNotBlank(friendItem.getAddSource())){
                requestEntity.setAddSource(requestEntity.getAddSource());
            }
            if(StringUtils.isNotBlank(friendItem.getAddWording())){
                requestEntity.setAddWording(friendItem.getAddWording());
            }
            requestEntity.setApproveStatus(0);
            requestEntity.setReadStatus(0);
            requestEntity.setCreateTime(System.currentTimeMillis());
            friendShipRequestMapper.insert(requestEntity);
        }
        writeUserSequence.writeUserSequence(appId, friendItem.getToId(), Constants.SeqConstants.FriendshipRequest,sequence);
        messageProducer.sendToUser(request.getAppId(), request.getToId(), FriendshipEventCommand.FRIEND_REQUEST,request);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO approveFriendReq(ApproveFriendRequestReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.FriendshipRequest);
        FriendshipRequestEntity requestEntity = friendShipRequestMapper.selectById(req.getId());
        if(requestEntity == null){
            return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
        }
        if(!req.getOperator().equals(requestEntity.getToId())){
            return ResponseVO.errorResponse(FriendshipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
        }
        FriendshipRequestEntity entity = new FriendshipRequestEntity();
        entity.setSequence(sequence);
        entity.setApproveStatus(req.getStatus());
        entity.setUpdateTime(System.currentTimeMillis());
        entity.setId(requestEntity.getId());
        friendShipRequestMapper.updateById(entity);
        writeUserSequence.writeUserSequence(req.getAppId(), req.getOperator(), Constants.SeqConstants.FriendshipRequest,sequence);
        if(req.getStatus() == ApproveFriendRequestStatusEnum.AGREE.getCode()){
            FriendshipDTO friend = new FriendshipDTO();
            friend.setAddSource(requestEntity.getAddSource());
            friend.setRemark(requestEntity.getRemark());
            friend.setAddWording(requestEntity.getAddWording());
            friend.setToId(entity.getToId());
            deleteReq(req);
            ResponseVO responseVO = friendshipService.doAddFriendship(req,entity.getFromId(), friend, req.getAppId());
            if(!responseVO.isOk())
                return responseVO;
        }
        ApproverFriendRequestPack approverFriendRequestPack = new ApproverFriendRequestPack();
        approverFriendRequestPack.setSequence(sequence);
        approverFriendRequestPack.setId(req.getId());
        approverFriendRequestPack.setStatus(req.getStatus());
        messageProducer.sendToUser(req.getAppId(),requestEntity.getToId(),FriendshipEventCommand.FRIEND_REQUEST_APPROVER,sequence);

        return ResponseVO.successResponse();
    }

    private void deleteReq(ApproveFriendRequestReq req) {
        QueryWrapper<FriendshipRequestEntity> query = new QueryWrapper<>();
        query.eq("app_id",req.getAppId());
        query.eq("from_id",req.getId());
        query.eq("to_id",req.getOperator());
        friendShipRequestMapper.delete(query);
    }

    @Override
    public ResponseVO readFriendReq(ReadFriendshipRequestReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.FriendshipRequest);
        QueryWrapper<FriendshipRequestEntity> queryWrapper = new QueryWrapper();
        queryWrapper.eq("app_id", req.getAppId());
        queryWrapper.eq("to_id", req.getFromId());
        FriendshipRequestEntity update = new FriendshipRequestEntity();
        update.setReadStatus(1);
        update.setSequence(sequence);
        update.setUpdateTime(System.currentTimeMillis());
        friendShipRequestMapper.update(update,queryWrapper);
        messageProducer.sendToUser(req.getAppId(),req.getOperator(),FriendshipEventCommand.FRIEND_REQUEST_APPROVER,sequence);
        ReadAllFriendRequestPack readAllFriendRequestPack = new ReadAllFriendRequestPack();
        readAllFriendRequestPack.setFromId(req.getFromId());
        readAllFriendRequestPack.setSequence(sequence);
        messageProducer.sendToUser(req.getAppId(),req.getFromId(),FriendshipEventCommand.FRIEND_REQUEST_READ,readAllFriendRequestPack);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getFriendReq(String fromId, Integer appId) {
        QueryWrapper<FriendshipRequestEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id", appId);
        queryWrapper.eq("to_id", fromId);
        return ResponseVO.successResponse(friendShipRequestMapper.selectList(queryWrapper));


    }


}
