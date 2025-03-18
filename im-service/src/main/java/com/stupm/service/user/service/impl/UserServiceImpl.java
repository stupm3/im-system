package com.stupm.service.user.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stupm.common.BaseErrorCode;
import com.stupm.common.ResponseVO;
import com.stupm.common.config.AppConfig;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.DelFlagEnum;
import com.stupm.common.enums.UserErrorCode;
import com.stupm.common.enums.command.UserEventCommand;
import com.stupm.message.codec.pack.user.UserModifyPack;
import com.stupm.service.group.model.req.SubscribeUserOnlineReq;
import com.stupm.service.group.service.GroupService;
import com.stupm.service.user.dao.UserDataEntity;
import com.stupm.service.user.dao.mapper.UserDataMapper;
import com.stupm.service.user.moudles.req.*;
import com.stupm.service.user.moudles.resp.GetUserInfoResp;
import com.stupm.service.user.moudles.resp.ImportUserResp;
import com.stupm.service.user.service.UserService;
import com.stupm.service.utils.CallbackService;
import com.stupm.service.utils.MessageProducer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDataMapper userDataMapper;

    @Autowired
    AppConfig appConfig;

    @Autowired
    CallbackService callBackService;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    GroupService groupService;


    @Override
    public ResponseVO importUser(ImportUserReq req) {
        if(req.getUserData().size() > 100){
            //TODO 返回用户数量过多
        }

        List<String> successIds = new ArrayList<>();
        List<String> errorIds = new ArrayList<>();


        req.getUserData().forEach(user -> {
            try{
                user.setAppId(req.getAppId());
                int insert = userDataMapper.insert(user);
                if(insert == 1){
                    successIds.add(user.getUserId());
                }

            }
            catch(Exception e){
                errorIds.add(user.getUserId());
                e.printStackTrace();
            }
        });
        ImportUserResp resp = new ImportUserResp();
        resp.setSuccessIds(successIds);
        resp.setErrorIds(errorIds);

        return ResponseVO.successResponse(resp);

    }
    @Override
    public ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req) {
        QueryWrapper<UserDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.in("user_id",req.getUserIds());
        queryWrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        List<UserDataEntity> userDataEntities = userDataMapper.selectList(queryWrapper);
        HashMap<String, UserDataEntity> map = new HashMap<>();

        for (UserDataEntity data:
                userDataEntities) {
            map.put(data.getUserId(),data);
        }

        List<String> failUser = new ArrayList<>();
        for (String uid:
                req.getUserIds()) {
            if(!map.containsKey(uid)){
                failUser.add(uid);
            }
        }

        GetUserInfoResp resp = new GetUserInfoResp();
        resp.setUserDataItem(userDataEntities);
        resp.setFailUser(failUser);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<UserDataEntity> getSingleUserInfo(String userId, Integer appId) {
        QueryWrapper objectQueryWrapper = new QueryWrapper<>();
        objectQueryWrapper.eq("app_id",appId);
        objectQueryWrapper.eq("user_id",userId);
        objectQueryWrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        UserDataEntity ImUserDataEntity = userDataMapper.selectOne(objectQueryWrapper);
        if(ImUserDataEntity == null){
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }

        return ResponseVO.successResponse(ImUserDataEntity);
    }

    @Override
    public ResponseVO deleteUser(DeleteUserReq req) {
        UserDataEntity entity = new UserDataEntity();
        entity.setDelFlag(DelFlagEnum.DELETE.getCode());

        List<String> errorIds = new ArrayList();
        List<String> successIds = new ArrayList();

        for (String userId:
                req.getUserId()) {
            QueryWrapper wrapper = new QueryWrapper();
            wrapper.eq("app_id",req.getAppId());
            wrapper.eq("user_id",userId);
            wrapper.eq("del_flag",DelFlagEnum.NORMAL.getCode());
            int update;

            try {
                update =  userDataMapper.update(entity, wrapper);
                if(update > 0){
                    successIds.add(userId);
                }else{
                    errorIds.add(userId);
                }
            }catch (Exception e){
                errorIds.add(userId);
            }
        }

        ImportUserResp resp = new ImportUserResp();
        resp.setSuccessIds(successIds);
        resp.setErrorIds(errorIds);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO modifyUserInfo(ModifyUserInfoReq req) {
        QueryWrapper<UserDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.eq("user_id",req.getUserId());
        queryWrapper.eq("del_flag",DelFlagEnum.NORMAL.getCode());
        UserDataEntity user = userDataMapper.selectOne(queryWrapper);
        if(user == null){
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }
        BeanUtils.copyProperties(req,user);
         int update = userDataMapper.update(user, queryWrapper);
        if(update == 1){
            UserModifyPack userModifyPack = new UserModifyPack();
            BeanUtils.copyProperties(req,userModifyPack);
            messageProducer.sendToUser(req.getUserId(),req.getClientType(),req.getImei() , UserEventCommand.USER_MODIFY , userModifyPack , req.getAppId());

            if(appConfig.isModifyUserAfterCallback()){
                callBackService.callback(req.getAppId() , Constants.CallbackCommand.ModifyUserAfter, JSONObject.toJSONString(req));
            }
        }
        return ResponseVO.errorResponse(UserErrorCode.MODIFY_USER_ERROR);
    }

    @Override
    public ResponseVO login(LoginReq req) {
        String cacheUserSig = stringRedisTemplate.opsForValue().get(req.getAppId() + Constants.RedisConstants.UserSign + ":" + req.getUserId() + req.getUserSign());

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getUserSequence(GetUserSequenceReq req) {
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(req.getAppId() + ":" + Constants.RedisConstants.SeqPrefix + ":" + req.getUserId());
        Long maxGroupSeq = groupService.getMaxGroupSeq(req.getUserId(), req.getAppId());
        map.put(Constants.SeqConstants.Group , maxGroupSeq);
        return ResponseVO.successResponse(map);
    }

}
