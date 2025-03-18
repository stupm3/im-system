package com.stupm.service.friendship.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.stupm.common.ResponseVO;
import com.stupm.common.config.AppConfig;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.AllowFriendTypeEnum;
import com.stupm.common.enums.CheckFriendShipTypeEnum;
import com.stupm.common.enums.FriendshipErrorCode;
import com.stupm.common.enums.FriendshipStatusEnum;
import com.stupm.common.enums.command.FriendshipEventCommand;
import com.stupm.common.model.ClientInfo;
import com.stupm.common.model.RequestBase;
import com.stupm.common.model.SyncReq;
import com.stupm.common.model.SyncResp;
import com.stupm.message.codec.pack.friendship.*;
import com.stupm.service.dto.FriendshipDTO;
import com.stupm.service.dto.ImportFriendDTO;
import com.stupm.service.friendship.dao.FriendshipEntity;
import com.stupm.service.friendship.dao.mapper.FriendshipMapper;
import com.stupm.service.friendship.model.callback.AddFriendAfterCallbackDTO;
import com.stupm.service.friendship.model.callback.AddFriendBlackAfterCallbackDTO;
import com.stupm.service.friendship.model.callback.DeleteFriendAfterCallbackDTO;
import com.stupm.service.friendship.model.req.*;
import com.stupm.service.friendship.model.resp.CheckFriendShipResp;
import com.stupm.service.friendship.model.resp.ImportFriendshipResp;
import com.stupm.service.friendship.service.FriendshipReqService;
import com.stupm.service.friendship.service.FriendshipService;
import com.stupm.service.sequence.RedisSequence;
import com.stupm.service.user.dao.UserDataEntity;
import com.stupm.service.user.service.UserService;
import com.stupm.service.utils.CallbackService;
import com.stupm.service.utils.MessageProducer;
import com.stupm.service.utils.WriteUserSequence;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FriendshipServiceImpl implements FriendshipService {

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private FriendshipMapper friendShipMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private FriendshipReqService friendshipReqService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CallbackService callbackService;

    @Autowired
    private WriteUserSequence  writeUserSequence;

    @Autowired
    private RedisSequence redisSequence;

    @Override
    public ResponseVO importFriendship(ImportFriendshipReq req) {
        if(req.getFriendItem().size() > 100 ){
            return ResponseVO.errorResponse(FriendshipErrorCode.IMPORT_SIZE_BEYOND);
        }

        List<String> successIds = new ArrayList<>();
        List<String> errorIds = new ArrayList<>();
        for(ImportFriendDTO dto : req.getFriendItem()){
            FriendshipEntity entity = new FriendshipEntity();
            BeanUtils.copyProperties(dto, entity);
            entity.setAppId(req.getAppId());
            entity.setFromId(req.getFromId());
            try{
                int insert = friendShipMapper.insert(entity);
                if(insert == 1){
                    successIds.add(dto.getToId());
                }else{
                    errorIds.add(dto.getToId());
                }

            }catch (Exception e){
                e.printStackTrace();
                errorIds.add(dto.getToId());
            }


        }
        ImportFriendshipResp resp = new ImportFriendshipResp();
        resp.setSuccessIds(successIds);
        resp.setErrorIds(errorIds);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO addFriendship(AddFriendReq req) {
        ResponseVO<UserDataEntity> fromInfo = userService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()){
            return fromInfo;
        }
        ResponseVO<UserDataEntity> toInfo = userService.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if(!toInfo.isOk()){
            return toInfo;
        }

        if(appConfig.isAddFriendBeforeCallback()){
            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(), Constants.CallbackCommand.AddFriendBefore, JSONObject.toJSONString(req));
            if(!responseVO.isOk()){
                return responseVO;
            }

        }

        UserDataEntity data = toInfo.getData();
        if(data.getFriendAllowType() != null && data.getFriendAllowType() == AllowFriendTypeEnum.NOT_NEED.getCode()){
            return doAddFriendship(req,req.getFromId() , req.getToItem() , req.getAppId());
        }else{
            QueryWrapper<FriendshipEntity> query = new QueryWrapper<>();
            query.eq("app_id",req.getAppId());
            query.eq("to_id",req.getToItem().getToId());
            query.eq("from_id", req.getFromId());
            FriendshipEntity friendshipEntity = friendShipMapper.selectOne(query);
            if(friendshipEntity == null || friendshipEntity.getStatus() != FriendshipStatusEnum.BLACK_STATUS_NORMAL.getCode()){
                ResponseVO responseVO = friendshipReqService.addFriendReq(req.getFromId(),req.getToItem(),req.getAppId());
                if(!responseVO.isOk()){
                    return responseVO;
                }
            }else{
                return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_YOUR_FRIEND);
            }


        }


        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO updateFriendship(UpdateFriendReq req) {
        ResponseVO<UserDataEntity> fromInfo = userService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()){
            return fromInfo;
        }
        ResponseVO<UserDataEntity> toInfo = userService.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if(!toInfo.isOk()){
            return toInfo;
        }
        if (appConfig.isModifyFriendAfterCallback()) {
            AddFriendAfterCallbackDTO callbackDto = new AddFriendAfterCallbackDTO();
            callbackDto.setFromId(req.getFromId());
            callbackDto.setFriendshipDTO(req.getToItem());
            callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.UpdateFriendAfter, JSONObject
                            .toJSONString(callbackDto));
        }
        ResponseVO responseVO = doUpdate(req.getFromId(), req.getToItem(), req.getAppId());
        if (responseVO.isOk()){
            UpdateFriendPack updateFriendPack = new UpdateFriendPack();
            updateFriendPack.setRemark(req.getToItem().getRemark());
            updateFriendPack.setToId(req.getToItem().getToId());
            messageProducer.sendToUserAnotherClient(req.getFromId(), FriendshipEventCommand.FRIEND_UPDATE , updateFriendPack,new ClientInfo(req.getAppId(), req.getClientType(),req.getImei()));
            if(appConfig.isModifyFriendAfterCallback()){
                AddFriendAfterCallbackDTO addFriendAfterCallbackDTO = new AddFriendAfterCallbackDTO();
                addFriendAfterCallbackDTO.setFromId(req.getFromId());
                addFriendAfterCallbackDTO.setFriendshipDTO(req.getToItem());
                callbackService.callback(req.getAppId(), Constants.CallbackCommand.UpdateFriendAfter, JSONObject.toJSONString(addFriendAfterCallbackDTO));
            }
        }
        return responseVO;
    }

    @Override
    public ResponseVO deleteFriendship(DeleteFriendReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
        QueryWrapper<FriendshipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id", req.getAppId());
        queryWrapper.eq("from_id", req.getFromId());
        queryWrapper.eq("to_id", req.getToId());
        FriendshipEntity entity = friendShipMapper.selectOne(queryWrapper);
        if(entity == null){
            return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }else{
            if(entity.getStatus() == FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode()){
                FriendshipEntity update = new FriendshipEntity();
                update.setFriendSequence(sequence);
                update.setStatus(FriendshipStatusEnum.FRIEND_STATUS_NO_FRIEND.getCode());
                friendShipMapper.update(update, queryWrapper);
                DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
                deleteFriendPack.setSequence(sequence);
                deleteFriendPack.setFromId(req.getFromId());
                deleteFriendPack.setToId(req.getToId());
                messageProducer.sendToUserAnotherClient(req.getFromId(),FriendshipEventCommand.FRIEND_DELETE,deleteFriendPack,new ClientInfo(req.getAppId(), req.getClientType(),req.getImei()));
                //回调
                if (appConfig.isAddFriendAfterCallback()){
                    DeleteFriendAfterCallbackDTO callbackDto = new DeleteFriendAfterCallbackDTO();
                    callbackDto.setFromId(req.getFromId());
                    callbackDto.setToId(req.getToId());
                    callbackService.beforeCallback(req.getAppId(),
                            Constants.CallbackCommand.DeleteFriendAfter, JSONObject
                                    .toJSONString(callbackDto));
                }
                writeUserSequence.writeUserSequence(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,sequence);
            }else{
                return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_DELETED);
            }
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteAllFriendship(DeleteAllFriendReq req) {
        QueryWrapper<FriendshipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        queryWrapper.eq("status",FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
        FriendshipEntity update = new FriendshipEntity();
        update.setStatus(FriendshipStatusEnum.FRIEND_STATUS_DELETE.getCode());
        friendShipMapper.update(update, queryWrapper);
        DeleteAllFriendPack deleteAllFriendPack = new DeleteAllFriendPack();
        deleteAllFriendPack.setFromId(req.getFromId());
        messageProducer.sendToUserAnotherClient(req.getFromId(),FriendshipEventCommand.FRIEND_ALL_DELETE,deleteAllFriendPack,new ClientInfo(req.getAppId(), req.getClientType(),req.getImei()));
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getAllFriendShip(GetAllFriendshipReq req) {
        QueryWrapper<FriendshipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        return ResponseVO.successResponse(friendShipMapper.selectList(queryWrapper));
    }

    @Override
    public ResponseVO getRelationship(GetRelationshipReq req) {
        QueryWrapper<FriendshipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        queryWrapper.eq("to_id",req.getToId());
        FriendshipEntity friend = friendShipMapper.selectOne(queryWrapper);
        if(friend == null){
            return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }
        return ResponseVO.successResponse(friend);
    }

    @Override
    public ResponseVO checkRelationShip(CheckFriendShipReq req) {
        Map<String , Integer> result
                = req.getToIds().stream()
                .collect(Collectors.toMap(Function.identity() , s -> 0));
        List<CheckFriendShipResp> resp;

        if(req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()){
            resp = friendShipMapper.checkFriendShipSingle(req);
        }else{
            resp = friendShipMapper.checkFriendBothWay(req);
        }
        Map<String , Integer> collect = resp.stream()
                .collect(Collectors.toMap(CheckFriendShipResp::getToId , CheckFriendShipResp::getStatus));
        for(String toId : result.keySet()){
            if(!collect.containsKey(toId)){
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setStatus(result.get(toId));
                resp.add(checkFriendShipResp);

            }
        }
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO addBlack(AddBlackReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
        ResponseVO<UserDataEntity> fromInfo = userService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()){
            return fromInfo;
        }

        ResponseVO<UserDataEntity> toInfo = userService.getSingleUserInfo(req.getToId(), req.getAppId());
        if(!toInfo.isOk()){
            return toInfo;
        }

        QueryWrapper<FriendshipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        queryWrapper.eq("to_id",req.getToId());
        FriendshipEntity entity = friendShipMapper.selectOne(queryWrapper);
        if(entity == null){
            entity = new FriendshipEntity();
            entity.setAppId(req.getAppId());
            entity.setFromId(req.getFromId());
            entity.setFriendSequence(sequence);
            entity.setToId(req.getToId());
            entity.setBlack(FriendshipStatusEnum.BLACK_STATUS_BLACKED.getCode());
            int insert = friendShipMapper.insert(entity);
            writeUserSequence.writeUserSequence(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,sequence);
            if(insert == 0){
                return ResponseVO.errorResponse(FriendshipErrorCode.ADD_BLACK_ERROR);
            }
        }else{
            if(entity.getBlack() == FriendshipStatusEnum.BLACK_STATUS_BLACKED.getCode()){
                return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_BLACK);
            }else{
                FriendshipEntity update = new FriendshipEntity();
                update.setFriendSequence(sequence);
                update.setBlack(FriendshipStatusEnum.BLACK_STATUS_BLACKED.getCode());
                int update1 = friendShipMapper.update(entity, queryWrapper);
                writeUserSequence.writeUserSequence(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,sequence);
                if(update1 == 0){
                    return ResponseVO.errorResponse(FriendshipErrorCode.ADD_BLACK_ERROR);
                }
            }
        }
        AddFriendBlackPack addFriendBlackPack = new AddFriendBlackPack();
        addFriendBlackPack.setSequence(sequence);
        addFriendBlackPack.setFromId(req.getFromId());
        addFriendBlackPack.setToId(req.getToId());
        messageProducer.sendToUserAnotherClient(req.getFromId(), FriendshipEventCommand.FRIEND_BLACK_ADD,addFriendBlackPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

        if (appConfig.isAddFriendShipBlackAfterCallback()){
            AddFriendBlackAfterCallbackDTO callbackDto = new AddFriendBlackAfterCallbackDTO();
            callbackDto.setFromId(req.getFromId());
            callbackDto.setToId(req.getToId());
            callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.AddBlackAfter, JSONObject
                            .toJSONString(callbackDto));
        }
        return ResponseVO.successResponse();

    }

    @Override
    public ResponseVO deleteBlack(DeleteBlackReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
        QueryWrapper<FriendshipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getFromId());
        queryWrapper.eq("to_id",req.getToId());
        FriendshipEntity entity = friendShipMapper.selectOne(queryWrapper);
        if(entity.getBlack() == FriendshipStatusEnum.BLACK_STATUS_BLACKED.getCode()){
            return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_NOT_YOUR_BLACK);
        }
        FriendshipEntity update = new FriendshipEntity();
        update.setFriendSequence(sequence);
        update.setBlack(FriendshipStatusEnum.BLACK_STATUS_NORMAL.getCode());
        int update1 = friendShipMapper.update(entity, queryWrapper);
        writeUserSequence.writeUserSequence(req.getAppId(),req.getFromId(),Constants.SeqConstants.Friendship,sequence);
        if(update1 == 1){
            if (appConfig.isAddFriendShipBlackAfterCallback()){
                AddFriendBlackAfterCallbackDTO callbackDto = new AddFriendBlackAfterCallbackDTO();
                callbackDto.setFromId(req.getFromId());
                callbackDto.setToId(req.getToId());
                callbackService.beforeCallback(req.getAppId(),
                        Constants.CallbackCommand.DeleteBlack, JSONObject
                                .toJSONString(callbackDto));
            }
        }
        DeleteBlackPack deleteBlackPack = new DeleteBlackPack();
        deleteBlackPack.setSequence(sequence);
        deleteBlackPack.setFromId(req.getFromId());
        deleteBlackPack.setToId(req.getToId());
        messageProducer.sendToUserAnotherClient(req.getFromId(), FriendshipEventCommand.FRIEND_BLACK_DELETE,deleteBlackPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));


        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO checkBlack(CheckFriendShipReq req) {
        Map<String , Integer> result
                = req.getToIds().stream()
                .collect(Collectors.toMap(Function.identity() , s -> 0));
        List<CheckFriendShipResp> resp;

        if(req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()){
            resp = friendShipMapper.checkBlackSingle(req);
        }else{
            resp = friendShipMapper.checkBlackBothWay(req);
        }
        Map<String , Integer> collect = resp.stream()
                .collect(Collectors.toMap(CheckFriendShipResp::getToId , CheckFriendShipResp::getStatus));
        for(String toId : result.keySet()){
            if(!collect.containsKey(toId)){
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setStatus(result.get(toId));
                resp.add(checkFriendShipResp);

            }
        }
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO syncFriendship(SyncReq req) {
        if(req.getMaxLimit() > 100){
            req.setMaxLimit(100);
        }
        SyncResp<FriendshipEntity> resp = new SyncResp();
        QueryWrapper<FriendshipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("from_id",req.getOperator());
        queryWrapper.gt("friend_sequence",req.getLastSeq());
        queryWrapper.last(" limit " + req.getMaxLimit());
        queryWrapper.orderByDesc("friend_sequence");
        List<FriendshipEntity> list = friendShipMapper.selectList(queryWrapper);
        if(!CollectionUtil.isEmpty(list)){
            FriendshipEntity friendshipEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            Long maxSeq = friendShipMapper.getMaxFriendSeq(req.getAppId(), req.getOperator());
            resp.setMaxSeq(maxSeq);
            resp.setCompleted(friendshipEntity.getFriendSequence().equals(maxSeq));
            return ResponseVO.successResponse(resp);
        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }


    @Transactional
    public ResponseVO doAddFriendship(RequestBase requestBase, String fromId , FriendshipDTO friendItem , Integer appId) {

        QueryWrapper<FriendshipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id", appId);
        queryWrapper.eq("from_id", fromId);
        queryWrapper.eq("to_id", friendItem.getToId());
        FriendshipEntity from = friendShipMapper.selectOne(queryWrapper);
        Long seq = 0L;
        if(from == null){

            from = new FriendshipEntity();
            BeanUtils.copyProperties(friendItem, from);
            seq = redisSequence.getSequence(appId+":" +Constants.SeqConstants.Friendship);
            from.setFromId(fromId);
            from.setAppId(appId);
            from.setFriendSequence(seq);
            from.setStatus(FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            from.setBlack(FriendshipStatusEnum.BLACK_STATUS_NORMAL.getCode());
            from.setCreateTime(System.currentTimeMillis());
            int insert = friendShipMapper.insert(from);
            if(insert != 1){
                return ResponseVO.errorResponse(FriendshipErrorCode.ADD_FRIEND_ERROR);
            }
        }else if(from.getStatus() == FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode()){
            return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_YOUR_FRIEND);
        }else if(from.getStatus() == FriendshipStatusEnum.FRIEND_STATUS_NO_FRIEND.getCode()){
            FriendshipEntity update = new FriendshipEntity();
            if(StringUtils.isNotBlank(friendItem.getAddSource())){
                update.setAddSource(friendItem.getAddSource()) ;
            }
            if(StringUtils.isNotBlank(friendItem.getRemark())){
                update.setAddSource(friendItem.getRemark());
            }
            if(StringUtils.isNotBlank(friendItem.getExtra())){
                update.setAddSource(friendItem.getExtra());
            }
            seq = redisSequence.getSequence(appId+":" +Constants.SeqConstants.Friendship);
            update.setFriendSequence(seq);
            update.setStatus(FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            update.setCreateTime(System.currentTimeMillis());
            int result = friendShipMapper.update(update, queryWrapper);
            if(result != 1){
                return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_YOUR_FRIEND);
            }
            writeUserSequence.writeUserSequence(appId,fromId,Constants.SeqConstants.Friendship,seq);

        }
        QueryWrapper<FriendshipEntity> toQuery = new QueryWrapper<>();
        toQuery.eq("to_id", fromId);
        toQuery.eq("from_id", friendItem.getToId());
        toQuery.eq("app_id", appId);
        FriendshipEntity to = friendShipMapper.selectOne(toQuery);
        if(to == null){
            to = new FriendshipEntity();
            BeanUtils.copyProperties(friendItem, to);
            to.setAppId(appId);
            to.setFriendSequence(seq);
            to.setFromId(friendItem.getToId());
            to.setToId(fromId);
            to.setStatus(FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            to.setCreateTime(System.currentTimeMillis());
            to.setBlack(FriendshipStatusEnum.BLACK_STATUS_NORMAL.getCode());
            int insert = friendShipMapper.insert(to);
            if(insert != 1){
                return ResponseVO.errorResponse(FriendshipErrorCode.ADD_FRIEND_ERROR);
            }
            writeUserSequence.writeUserSequence(appId,friendItem.getToId(),Constants.SeqConstants.Friendship,seq);
        }else if(FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != to.getStatus()){

                FriendshipEntity friendshipEntity = new FriendshipEntity();
                friendshipEntity.setFriendSequence(seq);
                friendshipEntity.setStatus(FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                friendShipMapper.update(friendshipEntity,toQuery);
            writeUserSequence.writeUserSequence(appId,friendItem.getToId(),Constants.SeqConstants.Friendship,seq);
        }

        AddFriendPack addFriendPack = new AddFriendPack();
        BeanUtils.copyProperties(friendItem, addFriendPack);
        addFriendPack.setSequence(seq);
        if(requestBase  != null){
            messageProducer.sendToUserAnotherClient(fromId, FriendshipEventCommand.FRIEND_ADD,addFriendPack, new ClientInfo(requestBase.getAppId(), requestBase.getClientType(),requestBase.getImei()));
        }else{
            messageProducer.sendToUser(requestBase.getAppId(),to.getToId(), FriendshipEventCommand.FRIEND_ADD,addFriendPack);
        }
        AddFriendPack addToFriendPack = new AddFriendPack();
        BeanUtils.copyProperties(to, addToFriendPack);
        messageProducer.sendToUser(requestBase.getAppId(),to.getFromId(),FriendshipEventCommand.FRIEND_ADD,addToFriendPack);


        if(appConfig.isAddFriendAfterCallback()){
            AddFriendAfterCallbackDTO addFriendAfterCallbackDTO = new AddFriendAfterCallbackDTO();
            addFriendAfterCallbackDTO.setFromId(fromId);
            addFriendAfterCallbackDTO.setFriendshipDTO(friendItem);
            ResponseVO responseVO = callbackService.beforeCallback(appId, Constants.CallbackCommand.AddFriendAfter, JSONObject.toJSONString(addFriendAfterCallbackDTO));
            if(!responseVO.isOk()){
                return responseVO;
            }

        }

        return ResponseVO.successResponse();
    }
    @Transactional
    public ResponseVO doUpdate(String fromId , FriendshipDTO friendItem , Integer appId) {
        Long sequence = redisSequence.getSequence(appId + ":" + Constants.SeqConstants.Friendship);
        UpdateWrapper<FriendshipEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(FriendshipEntity::getAddSource, friendItem.getAddSource())
                .set(FriendshipEntity::getRemark, friendItem.getRemark())
                .set(FriendshipEntity::getFriendSequence,sequence)
                .set(FriendshipEntity::getExtra, friendItem.getExtra())
                .eq(FriendshipEntity::getAppId,appId)
                .eq(FriendshipEntity::getToId,friendItem.getToId())
                .eq(FriendshipEntity::getFromId,fromId);
        int update = friendShipMapper.update(null, updateWrapper);
        if(update == 1){
            writeUserSequence.writeUserSequence(appId,fromId,Constants.SeqConstants.Friendship,sequence);
            return ResponseVO.successResponse();
        }

        return ResponseVO.errorResponse();

    }

    public List<String> getAllFriendsId(String userId, Integer appId){
        return friendShipMapper.getAllFriendsId(appId,userId);
    }


}
