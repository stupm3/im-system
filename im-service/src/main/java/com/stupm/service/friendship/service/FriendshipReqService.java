package com.stupm.service.friendship.service;

import com.stupm.common.ResponseVO;
import com.stupm.service.dto.FriendshipDTO;
import com.stupm.service.friendship.model.req.ApproveFriendRequestReq;
import com.stupm.service.friendship.model.req.ReadFriendshipRequestReq;

public interface FriendshipReqService{
    public ResponseVO addFriendReq(String fromId,FriendshipDTO friendItem, Integer appId);

    public ResponseVO approveFriendReq(ApproveFriendRequestReq req);

    public ResponseVO readFriendReq(ReadFriendshipRequestReq req);

    public ResponseVO getFriendReq(String fromId,Integer appId);
}
