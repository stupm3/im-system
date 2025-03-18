package com.stupm.service.group.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stupm.common.ResponseVO;
import com.stupm.common.config.AppConfig;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.GroupErrorCode;
import com.stupm.common.enums.GroupMemberRoleEnum;
import com.stupm.common.enums.GroupStatusEnum;
import com.stupm.common.enums.GroupTypeEnum;
import com.stupm.common.enums.command.GroupEventCommand;
import com.stupm.common.exception.ApplicationException;
import com.stupm.common.model.ClientInfo;
import com.stupm.common.model.SyncReq;
import com.stupm.common.model.SyncResp;
import com.stupm.message.codec.pack.group.*;
import com.stupm.service.conversation.dao.ConversationSetEntity;
import com.stupm.service.group.dao.GroupEntity;
import com.stupm.service.group.dao.mapper.GroupMapper;
import com.stupm.service.group.dao.mapper.GroupMemberMapper;
import com.stupm.service.group.model.callback.DestroyGroupCallbackDTO;
import com.stupm.service.group.model.dto.GroupMemberDTO;
import com.stupm.service.group.model.req.*;
import com.stupm.service.group.model.resp.GetGroupResp;
import com.stupm.service.group.model.resp.GetRoleResp;
import com.stupm.service.group.service.GroupMemberService;
import com.stupm.service.group.service.GroupService;
import com.stupm.service.sequence.RedisSequence;
import com.stupm.service.utils.CallbackService;
import com.stupm.service.utils.GroupMessageProducer;
import com.stupm.service.utils.WriteUserSequence;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    AppConfig appConfig;

    @Autowired
    CallbackService callbackService;

    @Autowired
    GroupMapper groupMapper;

    @Autowired
    GroupMemberService groupMemberService;

    @Autowired
    GroupMessageProducer groupMessageProducer;

    @Autowired
    RedisSequence redisSequence;


    @Override
    public ResponseVO importGroup( ImportGroupReq req) {

        if(StringUtils.isNotBlank(req.getGroupId())){
            QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("group_id", req.getGroupId());
            queryWrapper.eq("app_id", req.getAppId());
            Integer count = groupMapper.selectCount(queryWrapper);
            if(count != 0){
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_EXIST);
            }
        }else{
            req.setGroupId(UUID.randomUUID().toString().replaceAll("-", ""));
        }
        GroupEntity entity = new GroupEntity();
        BeanUtils.copyProperties(req, entity);
        if(req.getCreateTime() == null)
            entity.setCreateTime(System.currentTimeMillis());
        if(req.getStatus() == null)
            entity.setStatus(GroupStatusEnum.NORMAL.getCode());
        int insert = groupMapper.insert(entity);
        if(insert != 1){
            throw new ApplicationException(GroupErrorCode.IMPORT_GROUP_ERROR);
        }
        return ResponseVO.successResponse(entity);
    }

    @Override
    public ResponseVO<GroupEntity> getGroup(String groupId, Integer appId) {

        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", groupId);
        queryWrapper.eq("app_id", appId);
        GroupEntity entity = groupMapper.selectOne(queryWrapper);
        if(entity == null){
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    @Override
    public ResponseVO updateGroupInfo(UpdateGroupReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.Group);
        ResponseVO<GroupEntity> group = this.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        boolean isAdmin = false;
        if(!isAdmin){
            ResponseVO<GetRoleResp> roleInGroup = groupMemberService.getRoleInGroup(req.getGroupId(), req.getOperator(), req.getAppId());
            if(!roleInGroup.isOk()){
                return roleInGroup;
            }
            GetRoleResp data = roleInGroup.getData();
            Integer role = data.getRole();
            boolean isManager = GroupMemberRoleEnum.MAMAGER.getCode() == role;
            boolean isOwner = GroupMemberRoleEnum.OWNER.getCode() == role;
            if(GroupTypeEnum.PUBLIC.getCode() == group.getData().getGroupType()){
                if(!isManager && !isOwner){
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

            }
        }
        GroupEntity entity = new GroupEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setSequence(sequence);
        QueryWrapper<GroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", req.getGroupId());
        queryWrapper.eq("app_id", req.getAppId());
        int update = groupMapper.update(entity, queryWrapper);
        UpdateGroupInfoPack updateGroupInfoPack = new UpdateGroupInfoPack();
        updateGroupInfoPack.setSequence(sequence);
        BeanUtils.copyProperties(req, updateGroupInfoPack);
        groupMessageProducer.producer(req.getOperator(),GroupEventCommand.UPDATED_GROUP,updateGroupInfoPack,new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        if(update != 1){
            throw new ApplicationException(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
        }
        if(appConfig.isModifyGroupAfterCallback()){
            callbackService.callback(req.getAppId() , Constants.CallbackCommand.UpdateGroupAfter , JSONObject.toJSONString( groupMapper.selectOne(queryWrapper) ));
        }
        return ResponseVO.successResponse();

    }

    public ResponseVO createGroup(CreateGroupReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.Group);

        boolean isAdmin = false;

        if (!isAdmin) {
            req.setOwnerId(req.getOperator());
        }

        QueryWrapper<GroupEntity> query = new QueryWrapper<>();

        if (StringUtils.isEmpty(req.getGroupId())) {
            req.setGroupId(UUID.randomUUID().toString().replace("-", ""));
        } else {
            query.eq("group_id", req.getGroupId());
            query.eq("app_id", req.getAppId());
            Integer integer = groupMapper.selectCount(query);
            if (integer > 0) {
                throw new ApplicationException(GroupErrorCode.GROUP_IS_EXIST);
            }
        }

        if (req.getGroupType() == GroupTypeEnum.PUBLIC.getCode() && StringUtils.isBlank(req.getOwnerId())) {
            throw new ApplicationException(GroupErrorCode.PUBLIC_GROUP_MUST_HAVE_OWNER);
        }

        GroupEntity entity = new GroupEntity();
        entity.setSequence(sequence);
        entity.setCreateTime(System.currentTimeMillis());
        entity.setStatus(GroupStatusEnum.NORMAL.getCode());
        BeanUtils.copyProperties(req, entity);
        groupMapper.insert(entity);

        GroupMemberDTO groupMemberDto = new GroupMemberDTO();
        groupMemberDto.setMemberId(req.getOwnerId());
        groupMemberDto.setRole(GroupMemberRoleEnum.OWNER.getCode());
        groupMemberDto.setJoinTime(System.currentTimeMillis());
        groupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), groupMemberDto);
        if(CollectionUtil.isNotEmpty(req.getMember())){
            for (GroupMemberDTO dto : req.getMember()) {
                groupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), dto);
            }
        }

        if(appConfig.isCreateGroupAfterCallback()){
            callbackService.callback(req.getAppId() , Constants.CallbackCommand.CreateGroupAfter , JSONObject.toJSONString(entity) );
        }

        CreateGroupPack createGroupPack = new CreateGroupPack();
        BeanUtils.copyProperties(entity,createGroupPack);
        groupMessageProducer.producer(req.getOwnerId(), GroupEventCommand.CREATED_GROUP,createGroupPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));



        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getGroupInfo(GetGroupInfoReq req) {
        ResponseVO<GroupEntity> group = this.getGroup(req.getGroupId(), req.getAppId());

        if(!group.isOk()){
            return group;
        }

        GetGroupResp getGroupResp = new GetGroupResp();
        BeanUtils.copyProperties(group.getData(), getGroupResp);
        try {
            ResponseVO<List<GroupMemberDTO>> groupMember = groupMemberService.getGroupMember(req.getGroupId(), req.getAppId());
            if (groupMember.isOk()) {
                getGroupResp.setMemberList(groupMember.getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseVO.successResponse(getGroupResp);
    }

    @Override
    public ResponseVO getJoinedGroup(GetJoinedGroupReq req) {
        ResponseVO<List<String>> memberJoinedGroup = groupMemberService.getMemberJoinedGroup(req);
        List<GroupEntity> list = new ArrayList<>();
        QueryWrapper<GroupEntity> query = new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        if(CollectionUtil.isNotEmpty(req.getGroupType())){
            query.in("group_type", req.getGroupType());
        }
        query.in("group_id",memberJoinedGroup.getData());
        list = groupMapper.selectList(query);
        return ResponseVO.successResponse(list);
    }

    @Override
    public ResponseVO destroyGroup(DestroyGroupReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.Group);
        boolean isAdmin = false;
        QueryWrapper<GroupEntity> query = new QueryWrapper<>();
        query.eq("group_id", req.getGroupId());
        query.eq("app_id", req.getAppId());
        GroupEntity entity = groupMapper.selectOne(query);
        if(entity == null){
            throw new ApplicationException(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }
        if(!isAdmin){
            if(entity.getGroupType() == GroupTypeEnum.PUBLIC.getCode() && !req.getOperator().equals(entity.getOwnerId())){
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
            if(entity.getGroupType() == GroupTypeEnum.PRIVATE.getCode() ){
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
        }
        GroupEntity update = new GroupEntity();
        update.setStatus(GroupStatusEnum.DESTROY.getCode());
        update.setSequence(sequence);
        int update1 = groupMapper.update(update, query);
        if(update1 != 1){
            throw new ApplicationException(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
        }
        if(appConfig.isModifyGroupAfterCallback()){
            DestroyGroupCallbackDTO dto = new DestroyGroupCallbackDTO();
            dto.setGroupId(req.getGroupId());
            callbackService.callback(req.getAppId() , Constants.CallbackCommand.DestroyGroupAfter , JSONObject.toJSONString(dto));
        }

        DestroyGroupPack destroyGroupPack = new DestroyGroupPack();
        destroyGroupPack.setSequence(sequence);
        destroyGroupPack.setGroupId(req.getGroupId());
        groupMessageProducer.producer(req.getOperator(),GroupEventCommand.DESTROY_GROUP,destroyGroupPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
        return ResponseVO.successResponse();
    }


    @Override
    @Transactional
    public ResponseVO transferGroup(TransferGroupReq req) {
        Long sequence = redisSequence.getSequence(req.getAppId() + ":" + Constants.SeqConstants.Group);
        ResponseVO<GetRoleResp> roleInGroup = groupMemberService.getRoleInGroup(req.getGroupId(), req.getOperator(), req.getAppId());
        if(!roleInGroup.isOk()){
            return roleInGroup;
        }
        if(roleInGroup.getData().getRole() != GroupMemberRoleEnum.OWNER.getCode()){
            return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
        }
        ResponseVO<GetRoleResp> newOwner = groupMemberService.getRoleInGroup(req.getGroupId(), req.getOwnerId(), req.getAppId());
        if(!newOwner.isOk()){
            return newOwner;
        }
        GroupEntity entity = new GroupEntity();
        entity.setSequence(sequence);
        entity.setOwnerId(req.getOwnerId());
        QueryWrapper<GroupEntity> query = new QueryWrapper<>();
        query.eq("group_id", req.getGroupId());
        query.eq("app_id", req.getAppId());
        groupMapper.update(entity, query);
        TransferGroupPack transferGroupPack = new TransferGroupPack();
        transferGroupPack.setGroupId(req.getGroupId());
        transferGroupPack.setOwnerId(req.getOwnerId());
        groupMessageProducer.producer(req.getOperator(),GroupEventCommand.TRANSFER_GROUP,transferGroupPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

        return groupMemberService.transferGroupMember(req.getOwnerId(), req.getGroupId(), req.getAppId());
    }

    @Override
    public ResponseVO muteGroup(MuteGroupReq req) {

        ResponseVO<GroupEntity> group = getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        boolean isAdmin = false;
        if(!isAdmin){
            ResponseVO<GetRoleResp> roleInGroup = groupMemberService.getRoleInGroup(req.getGroupId(), req.getOperator(), req.getAppId());
            if(!roleInGroup.isOk()){
                return roleInGroup;
            }
            GetRoleResp data = roleInGroup.getData();
            Integer role = data.getRole();
            boolean isManager = role == GroupMemberRoleEnum.OWNER.getCode() || role == GroupMemberRoleEnum.MAMAGER.getCode();
            if(!isManager){
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
        }
        GroupEntity entity = new GroupEntity();
        entity.setMute(req.getMute());

        QueryWrapper<GroupEntity> query = new QueryWrapper<>();
        query.eq("group_id", req.getGroupId());
        query.eq("app_id", req.getAppId());
        groupMapper.update(entity, query);
        MuteGroupPack muteGroupPack = new MuteGroupPack();
        muteGroupPack.setGroupId(req.getGroupId());
        groupMessageProducer.producer(req.getOperator(),GroupEventCommand.MUTE_GROUP,muteGroupPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO syncJoinedGroup(SyncReq req) {
        if(req.getMaxLimit() > 100){
            req.setMaxLimit(100);
        }
        SyncResp<GroupEntity> resp = new SyncResp<>();

        ResponseVO<Collection<String>> joinedGroup = groupMemberService.syncJoinedGroup(req.getOperator(), req.getAppId());
        if(joinedGroup.isOk()){
            Collection<String> group = joinedGroup.getData();
            QueryWrapper<GroupEntity> query = new QueryWrapper<>();
            query.eq("app_id", req.getAppId());
            query.in("group_id",group);
            query.gt("sequence",req.getLastSeq());
            query.last(" limit " + req.getMaxLimit());
            query.orderByDesc("sequence");
            List<GroupEntity> list = groupMapper.selectList(query);

            if(!CollectionUtil.isEmpty(list)){
                GroupEntity entity = list.get(list.size() - 1);
                resp.setDataList(list);
                Long maxSeq = groupMapper.getGroupMaxSeq( group,req.getAppId());
                resp.setMaxSeq(maxSeq);
                resp.setCompleted(entity.getSequence().equals(maxSeq));
                return ResponseVO.successResponse(resp);
            }
            resp.setCompleted(true);
            return ResponseVO.successResponse(resp);
        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public Long getMaxGroupSeq(String userId, Integer appId) {
        ResponseVO<Collection<String>> joinedGroup = groupMemberService.syncJoinedGroup(userId, appId);
        if(!joinedGroup.isOk()){
            throw new ApplicationException(500,"");
        }
        Collection<String> data = joinedGroup.getData();
        return groupMapper.getGroupMaxSeq(data,appId);
    }
}

