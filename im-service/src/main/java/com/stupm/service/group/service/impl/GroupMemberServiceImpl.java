package com.stupm.service.group.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stupm.common.ResponseVO;
import com.stupm.common.config.AppConfig;
import com.stupm.common.constant.Constants;
import com.stupm.common.enums.GroupErrorCode;
import com.stupm.common.enums.GroupMemberRoleEnum;
import com.stupm.common.enums.GroupTypeEnum;
import com.stupm.common.exception.ApplicationException;
import com.stupm.service.group.dao.GroupEntity;
import com.stupm.service.group.dao.GroupMemberEntity;
import com.stupm.service.group.dao.mapper.GroupMemberMapper;
import com.stupm.service.group.model.callback.AddMemberAfterCallback;
import com.stupm.service.group.model.dto.GroupMemberDTO;
import com.stupm.service.group.model.req.*;
import com.stupm.service.group.model.resp.AddGroupMemberResp;
import com.stupm.service.group.model.resp.GetRoleResp;
import com.stupm.service.group.service.GroupMemberService;
import com.stupm.service.group.service.GroupService;
import com.stupm.service.user.dao.UserDataEntity;
import com.stupm.service.user.service.UserService;
import com.stupm.service.utils.CallbackService;
import com.stupm.service.utils.GroupMessageProducer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.acl.Owner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class GroupMemberServiceImpl implements GroupMemberService {

    private static final Logger logger = LoggerFactory.getLogger(GroupMemberServiceImpl.class);

    @Autowired
    AppConfig appConfig;

    @Autowired
    CallbackService callbackService;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupMemberService thisService;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupMessageProducer producer;


    @Override
    public ResponseVO importGroupMember(ImportGroupMemberReq req) {
        List<AddGroupMemberResp> list = new ArrayList<>();
        ResponseVO group = groupService.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        for(GroupMemberDTO dto :  req.getMembers()){

            ResponseVO responseVO = thisService.addGroupMember(req.getGroupId(), req.getAppId(), dto);
            AddGroupMemberResp resp = new AddGroupMemberResp();
            resp.setMemberId(dto.getMemberId());
            if(responseVO.isOk()){
                resp.setResult(0);

            }else if(!responseVO.isOk() && GroupErrorCode.USER_IS_JOINED_GROUP.getCode() == responseVO.getCode()){
                resp.setResult(2);
            }else{
                resp.setResult(1);
            }
            list.add(resp);
        }
        return ResponseVO.successResponse(list);
    }

    @Override
    public ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDTO dto) {
        if(dto.getRole() == GroupMemberRoleEnum.OWNER.getCode()){
            QueryWrapper<GroupMemberEntity> query = new QueryWrapper<>();
            query.eq("group_id", groupId);
            query.eq("app_id", appId);
            query.eq("role", dto.getRole());
            Integer count = groupMemberMapper.selectCount(query);
            if(count > 0){
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_HAVE_OWNER);
            }
        }
        QueryWrapper<GroupMemberEntity> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        query.eq("app_id", appId);
        query.eq("member_id", dto.getMemberId());
        GroupMemberEntity entity = groupMemberMapper.selectOne(query);
        long now = System.currentTimeMillis();
        if(entity == null){
            entity = new GroupMemberEntity();
            BeanUtils.copyProperties(dto , entity);
            entity.setGroupId(groupId);
            entity.setAppId(appId);
            entity.setJoinTime(now);
            int insert = groupMemberMapper.insert(entity);
            if(insert == 1){
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        }else if(GroupMemberRoleEnum.LEAVE.getCode() == entity.getRole()){
            entity = new GroupMemberEntity();
            BeanUtils.copyProperties(dto , entity);
            entity.setJoinTime(now);
            int update = groupMemberMapper.update(entity,query);
            if(update == 1)
                return ResponseVO.successResponse();
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        }

        return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
    }

    @Override
    public ResponseVO<GetRoleResp> getRoleInGroup(String groupId, String memberId, Integer appId) {
        GetRoleResp getRoleResp = new GetRoleResp();
        QueryWrapper<GroupMemberEntity> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        query.eq("member_id", memberId);
        query.eq("app_id", appId);
        GroupMemberEntity entity = groupMemberMapper.selectOne(query);
        if(entity == null || entity.getRole() == GroupMemberRoleEnum.LEAVE.getCode()){
            return ResponseVO.errorResponse(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
        }
        BeanUtils.copyProperties(entity , getRoleResp);
        return ResponseVO.successResponse(getRoleResp);



    }

    @Override
    public ResponseVO<List<GroupMemberDTO>> getGroupMember(String groupId, Integer appId) {
        List<GroupMemberDTO> groupMember = groupMemberMapper.getGroupMember(appId, groupId);
        return ResponseVO.successResponse(groupMember);
    }

    @Override
    public ResponseVO getMemberJoinedGroup(GetJoinedGroupReq req) {
        return ResponseVO.successResponse(groupMemberMapper.getJoinedGroupId(req.getAppId() , req.getMemberId()));
    }

    @Override
    public ResponseVO transferGroupMember(String ownerId, String groupId, Integer appId) {
        GroupMemberEntity entity = new GroupMemberEntity();
        entity.setRole(GroupMemberRoleEnum.ORDINARY.getCode());
        QueryWrapper<GroupMemberEntity> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        query.eq("role", GroupMemberRoleEnum.OWNER.getCode());
        query.eq("app_id",appId);
        groupMemberMapper.update(entity, query);

        GroupMemberEntity newOwner = new GroupMemberEntity();
        newOwner.setRole(GroupMemberRoleEnum.OWNER.getCode());
        QueryWrapper<GroupMemberEntity> query2 = new QueryWrapper<>();
        query2.eq("group_id", groupId);
        query.eq("app_id",appId);
        query.eq("member_id", ownerId);
        groupMemberMapper.update(newOwner, query2);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO addMember(AddGroupMemberReq req) {
        List<AddGroupMemberResp> result = new ArrayList<>();
        boolean isAdmin = false;

        ResponseVO<GroupEntity> group = groupService.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        List<GroupMemberDTO> memberDTOs = req.getMembers();
        if(appConfig.isAddGroupMemberBeforeCallback()){
            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(), Constants.CallbackCommand.GroupMemberAddBefore, JSONObject.toJSONString(req));
            if(!responseVO.isOk()){
                return responseVO;
            }
            try{
                memberDTOs = JSONArray.parseArray(JSONObject.toJSONString(responseVO.getData()), GroupMemberDTO.class);
            }catch (Exception e){
                e.printStackTrace();
                logger.error("GroupMemberAddBeforeCallback error:", req.getAppId());
            }
        }

        List<GroupMemberDTO> members = req.getMembers();
        GroupEntity entity = group.getData();
        if(!isAdmin && entity.getGroupType().equals(GroupTypeEnum.PUBLIC)){
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
        }
        List<String> successIds = new ArrayList<>();
        List<String> errorIds = new ArrayList<>();

        for(GroupMemberDTO member : members){
            ResponseVO responseVO = null;
            try{
                responseVO = thisService.addGroupMember(req.getGroupId() , req.getAppId() , member);

            }catch(Exception e){
                e.printStackTrace();
                responseVO = ResponseVO.errorResponse();
            }
            AddGroupMemberResp resp = new AddGroupMemberResp();
            resp.setMemberId(member.getMemberId());
            if(responseVO.isOk()){
                successIds.add(member.getMemberId());
                resp.setResult(0);
            }else if(responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()){
                resp.setResult(2);
                resp.setResultMessage(responseVO.getMsg());
                errorIds.add(member.getMemberId());
            }else{
                resp.setResult(1);
                resp.setResultMessage(responseVO.getMsg());
                errorIds.add(member.getMemberId());
            }
            result.add(resp);
        }

        if(appConfig.isAddGroupMemberAfterCallback()){
            AddMemberAfterCallback dto = new AddMemberAfterCallback();
            dto.setGroupId(req.getGroupId());
            dto.setGroupType(group.getData().getGroupType());
            dto.setMemberId(result);
            dto.setOperator(req.getOperator());
            callbackService.callback(req.getAppId(), Constants.CallbackCommand.GroupMemberAddAfter, JSONObject.toJSONString(dto));
        }

        return ResponseVO.successResponse(result);

    }

    @Override
    public ResponseVO removeMember(RemoveGroupMemberReq req) {
        boolean isAdmin = false;

        ResponseVO<GroupEntity> group = groupService.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        Integer groupType = group.getData().getGroupType();

        GroupEntity entity = group.getData();
        if(!isAdmin) {
            ResponseVO<GetRoleResp> roleInGroup = getRoleInGroup(req.getGroupId(), req.getOperator(), req.getAppId());
            if(!roleInGroup.isOk()){
                return roleInGroup;
            }
            GetRoleResp data = roleInGroup.getData();
            Integer role = data.getRole();
            boolean isOwner = role == GroupMemberRoleEnum.OWNER.getCode();
            boolean isManager = role == GroupMemberRoleEnum.MAMAGER.getCode();

            ResponseVO<GetRoleResp> memberRole = getRoleInGroup(req.getGroupId(), req.getMemberId(), req.getAppId());
            boolean memberInManager = memberRole.getData().getRole() == GroupMemberRoleEnum.MAMAGER.getCode();


            if(!isOwner && !isManager){
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
            if(!isOwner && groupType == GroupTypeEnum.PRIVATE.getCode()){
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
            if(GroupTypeEnum.PUBLIC.getCode() == groupType){
                if((isManager && Objects.equals(req.getMemberId(), group.getData().getOwnerId())) || (isManager && memberInManager)){
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }
                if(memberRole.getData().getRole() == GroupMemberRoleEnum.LEAVE.getCode()){
                    return ResponseVO.errorResponse(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
                }
            }

        }

        ResponseVO responseVO = thisService.removeGroupMember(req.getGroupId(), req.getMemberId(), req.getAppId());
        if(responseVO.isOk()){
            if(appConfig.isDeleteGroupMemberAfterCallback()){
                callbackService.callback(req.getAppId(),Constants.CallbackCommand.GroupMemberDeleteAfter ,JSONObject.toJSONString(req));
            }
        }
        return responseVO;
    }

    @Override
    public ResponseVO removeGroupMember(String groupId,  String memberId ,Integer appId ) {

        ResponseVO<UserDataEntity> singleUserInfo = userService.getSingleUserInfo(memberId, appId);
        if(!singleUserInfo.isOk()){
            return singleUserInfo;
        }

        ResponseVO<GetRoleResp> roleInGroupOne = getRoleInGroup(groupId, memberId, appId);
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }

        GetRoleResp data = roleInGroupOne.getData();
        GroupMemberEntity imGroupMemberEntity = new GroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleEnum.LEAVE.getCode());
        imGroupMemberEntity.setLeaveTime(System.currentTimeMillis());
        imGroupMemberEntity.setGroupMemberId(data.getGroupMemberId());
        groupMemberMapper.updateById(imGroupMemberEntity);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO updateGroupMember(UpdateGroupMemberReq req) {
        boolean isAdmin = false;

        ResponseVO<GroupEntity> group = groupService.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        GroupEntity groupData = group.getData();
        boolean isMeOp = req.getMemberId().equals(req.getOperator());
        if(!isAdmin){
            if(!isMeOp && StringUtils.isBlank(req.getAlias())){
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
            if(groupData.getGroupType() == GroupTypeEnum.PRIVATE.getCode() && (req.getRole().equals(GroupMemberRoleEnum.MAMAGER.getCode()) || req.getRole().equals(GroupMemberRoleEnum.OWNER.getCode())) ){
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
        }
        ResponseVO<GetRoleResp> roleInGroup = getRoleInGroup(req.getGroupId(), req.getOperator(), req.getAppId());
        if(!roleInGroup.isOk()){
            return roleInGroup;
        }
        GetRoleResp data = roleInGroup.getData();
        Integer role = data.getRole();
        boolean isOwner = role == GroupMemberRoleEnum.OWNER.getCode();
        boolean isManager = role == GroupMemberRoleEnum.MAMAGER.getCode();
        if(!isOwner &&  req.getRole() != null){
            return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
        }
        if(req.getRole() == GroupMemberRoleEnum.OWNER.getCode()){
            return  ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
        }
        GroupMemberEntity entity = new GroupMemberEntity();
        if(StringUtils.isNotBlank(req.getAlias())){
            entity.setAlias(req.getAlias());
        }

        QueryWrapper<GroupMemberEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", req.getGroupId());
        queryWrapper.eq("app_id", req.getAppId());
        queryWrapper.eq("member_id", req.getMemberId());
        groupMemberMapper.update(entity, queryWrapper);
        return ResponseVO.successResponse();


    }

    @Override
    public ResponseVO muteGroupMember(MuteMemberReq req) {
        ResponseVO<GroupEntity> group = groupService.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        boolean isAdmin = false;
        boolean isOwner = false;
        boolean isManager = false;
        GetRoleResp resp = null;
        if(!isAdmin){
            ResponseVO<GetRoleResp> roleInGroup = getRoleInGroup(req.getGroupId(), req.getOperator(), req.getAppId());
            if(!roleInGroup.isOk()){
                return roleInGroup;
            }
            GetRoleResp data = roleInGroup.getData();
            Integer opRole = data.getRole();
            isOwner = opRole == GroupMemberRoleEnum.OWNER.getCode();
            isManager = opRole == GroupMemberRoleEnum.MAMAGER.getCode();
            resp = getRoleInGroup(req.getGroupId(),req.getMemberId(),req.getAppId()).getData();
            Integer memRole = resp.getRole();

            if(!isOwner && !isManager){
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
            if(isManager && (memRole == GroupMemberRoleEnum.MAMAGER.getCode() || memRole == GroupMemberRoleEnum.OWNER.getCode())){
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
            if(memRole == GroupMemberRoleEnum.LEAVE.getCode()){
                return ResponseVO.errorResponse(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
            }

        }
        GroupMemberEntity update = new GroupMemberEntity();
        update.setGroupMemberId(resp.getGroupMemberId());
        if(req.getSpeakDate() > 0){
            update.setSpeakDate(System.currentTimeMillis() + req.getSpeakDate());
        }else{
            update.setSpeakDate(System.currentTimeMillis());
        }
        groupMemberMapper.updateById(update);
        return ResponseVO.successResponse();
    }

    @Override
    public List<String> getGroupMemberId(String groupId, Integer appId) {
        return groupMemberMapper.getGroupMemberId(appId, groupId);
    }

    public List<GroupMemberDTO> getGroupManager(String groupId, Integer appId) {
        return groupMemberMapper.getGroupManager(groupId,appId);
    }

    @Override
    public ResponseVO<Collection<String>> syncJoinedGroup(String userId, Integer appId) {
        return ResponseVO.successResponse(groupMemberMapper.syncJoinedGroupId(appId,userId,GroupMemberRoleEnum.LEAVE.getCode()));
    }


}
