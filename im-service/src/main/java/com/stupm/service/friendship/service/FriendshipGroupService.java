package com.stupm.service.friendship.service;


import com.stupm.common.ResponseVO;
import com.stupm.service.friendship.dao.FriendshipGroupEntity;
import com.stupm.service.friendship.model.req.AddFriendshipGroupReq;
import com.stupm.service.friendship.model.req.DeleteFriendshipGroupReq;

/**
 * @author: Chackylee
 * @description:
 **/
public interface FriendshipGroupService {

    public ResponseVO addGroup(AddFriendshipGroupReq req);

    public ResponseVO deleteGroup(DeleteFriendshipGroupReq req);

    public ResponseVO<FriendshipGroupEntity> getGroup(String fromId, String groupName, Integer appId);

    public Long updateSeq(String fromId, String groupName, Integer appId);
}
