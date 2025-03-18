package com.stupm.service.message.service;

import com.stupm.common.ResponseVO;
import com.stupm.common.config.AppConfig;
import com.stupm.common.enums.*;
import com.stupm.service.friendship.dao.FriendshipEntity;
import com.stupm.service.friendship.model.req.GetRelationshipReq;
import com.stupm.service.friendship.service.FriendshipService;
import com.stupm.service.group.dao.GroupEntity;
import com.stupm.service.group.model.resp.GetRoleResp;
import com.stupm.service.group.service.GroupMemberService;
import com.stupm.service.group.service.GroupService;
import com.stupm.service.user.dao.UserDataEntity;
import com.stupm.service.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckSendMessageService {
    @Autowired
    private UserService userService;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupMemberService groupMemberService;

    public ResponseVO checkSenderForbiddenAndMute(String fromId,Integer appId){

        ResponseVO<UserDataEntity> singleUserInfo = userService.getSingleUserInfo(fromId, appId);
        if(!singleUserInfo.isOk()){
            return singleUserInfo;
        }
        UserDataEntity data = singleUserInfo.getData();
        if(data.getForbiddenFlag() == UserForbiddenFlagEnum.FORBIBBEN.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_FORBIDDEN);
        }
        else if(data.getSilentFlag() == UserSilentFlagEnum.MUTE.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_MUTE);
        }
        return ResponseVO.successResponse();
    }

    public ResponseVO checkFriendship(String fromId,String toId,Integer appId){
        if(appConfig.isSendMessageCheckFriend()){
            GetRelationshipReq fromReq = new GetRelationshipReq();
            fromReq.setFromId(fromId);
            fromReq.setToId(toId);
            fromReq.setAppId(appId);
            ResponseVO<FriendshipEntity> relationship = friendshipService.getRelationship(fromReq);
            if(!relationship.isOk()){
                return relationship;
            }
            GetRelationshipReq toReq = new GetRelationshipReq();
            toReq.setFromId(toId);
            toReq.setToId(fromId);
            toReq.setAppId(appId);
            ResponseVO<FriendshipEntity> toRelationship = friendshipService.getRelationship(toReq);
            if(FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != relationship.getData().getStatus() ){
                return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_DELETED);
            }
            if(FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != toRelationship.getData().getStatus() ){
                return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_NOT_YOUR_FRIEND);
            }
            if(appConfig.isSendMessageCheckBlack()){
                if(FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != toRelationship.getData().getBlack() ){
                    return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_BLACK);
                }
                if(FriendshipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != relationship.getData().getBlack() ){
                    return ResponseVO.errorResponse(FriendshipErrorCode.TARGET_IS_BLACK_YOU);
                }
            }
        }
        return ResponseVO.successResponse();
    }

    public ResponseVO checkGroupMessage(String fromId, String groupId,Integer appId){
        ResponseVO responseVO = checkSenderForbiddenAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }
        ResponseVO<GroupEntity> group = groupService.getGroup(groupId, appId);
        if(!group.isOk()){
            return responseVO;
        }
        ResponseVO<GetRoleResp> roleInGroup = groupMemberService.getRoleInGroup(groupId, fromId, appId);
        if(!roleInGroup.isOk()){
            return roleInGroup;
        }
        GroupEntity entity = group.getData();
        GetRoleResp data = roleInGroup.getData();
        if(entity.getMute() == GroupMuteTypeEnum.MUTE.getCode() &&( data.getRole() != GroupMemberRoleEnum.MAMAGER.getCode() && roleInGroup.getData().getRole() != GroupMemberRoleEnum.OWNER.getCode())){
            return ResponseVO.errorResponse(GroupErrorCode.THIS_GROUP_IS_MUTE);
        }
        if(data.getSpeakDate() != null && data.getSpeakDate() >System.currentTimeMillis() ){
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_MEMBER_IS_SPEAK);
        }


        return ResponseVO.successResponse();
    }
}
