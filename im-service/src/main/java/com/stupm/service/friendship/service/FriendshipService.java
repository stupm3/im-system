package com.stupm.service.friendship.service;

import com.stupm.common.ResponseVO;
import com.stupm.common.model.SyncReq;
import com.stupm.service.friendship.model.req.*;

import java.util.List;

public interface FriendshipService {
    public ResponseVO importFriendship(ImportFriendshipReq req);

    public ResponseVO addFriendship(AddFriendReq req);

    public ResponseVO updateFriendship(UpdateFriendReq req);

    public ResponseVO deleteFriendship(DeleteFriendReq req);

    public ResponseVO deleteAllFriendship(DeleteAllFriendReq req);

    public ResponseVO getAllFriendShip(GetAllFriendshipReq req);

    public ResponseVO getRelationship(GetRelationshipReq req);

    public ResponseVO checkRelationShip(CheckFriendShipReq req);

    public ResponseVO addBlack(AddBlackReq req);

    public ResponseVO deleteBlack(DeleteBlackReq req);

    public ResponseVO checkBlack(CheckFriendShipReq req);

    public ResponseVO syncFriendship(SyncReq req);

    public List<String> getAllFriendsId(String userId,Integer appId)  ;
}
