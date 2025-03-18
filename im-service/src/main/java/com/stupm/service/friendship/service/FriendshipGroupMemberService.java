package com.stupm.service.friendship.service;


import com.stupm.common.ResponseVO;
import com.stupm.service.friendship.model.req.AddFriendshipGroupMemberReq;
import com.stupm.service.friendship.model.req.DeleteFriendshipGroupMemberReq;

/**
 * @author: Chackylee
 * @description:
 **/
public interface FriendshipGroupMemberService {

    public ResponseVO addGroupMember(AddFriendshipGroupMemberReq req);

    public ResponseVO delGroupMember(DeleteFriendshipGroupMemberReq req);

    public int doAddGroupMember(Long groupId, String toId);

    public int clearGroupMember(Long groupId);
}
