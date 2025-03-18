package com.stupm.service.group.service;


import com.stupm.common.ResponseVO;
import com.stupm.service.group.model.dto.GroupMemberDTO;
import com.stupm.service.group.model.req.*;
import com.stupm.service.group.model.resp.GetRoleResp;

import java.util.Collection;
import java.util.List;

public interface GroupMemberService {
    public ResponseVO importGroupMember(ImportGroupMemberReq req);

    public ResponseVO addGroupMember(String groupId , Integer appId , GroupMemberDTO dto);

    public ResponseVO<GetRoleResp> getRoleInGroup(String groupId , String memberId, Integer appId);

    public ResponseVO getGroupMember(String groupId, Integer appId);

    public ResponseVO getMemberJoinedGroup(GetJoinedGroupReq req);

    public ResponseVO transferGroupMember(String ownerId , String groupId ,Integer appId);

    public ResponseVO addMember (AddGroupMemberReq req);

    public ResponseVO removeMember(RemoveGroupMemberReq req);

    public ResponseVO removeGroupMember(String groupId , String memberId , Integer appId);

    public ResponseVO updateGroupMember(UpdateGroupMemberReq req);

    public ResponseVO muteGroupMember(MuteMemberReq req);

    public List<String> getGroupMemberId(String groupId , Integer appId);

    public List<GroupMemberDTO> getGroupManager(String groupId, Integer appId);

    public ResponseVO<Collection<String>> syncJoinedGroup(String userId,Integer appId);
}
