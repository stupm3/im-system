package com.stupm.service.friendship.controller;

import com.stupm.common.ResponseVO;
import com.stupm.service.friendship.model.req.ApproveFriendRequestReq;
import com.stupm.service.friendship.model.req.GetFriendRequestReq;
import com.stupm.service.friendship.model.req.ReadFriendshipRequestReq;
import com.stupm.service.friendship.service.FriendshipReqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/friendReq")
public class FriendReqController {
    @Autowired
    FriendshipReqService friendReqService;

    @RequestMapping("/approve")
    public ResponseVO approve(@RequestBody @Validated ApproveFriendRequestReq req , Integer appId, String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return friendReqService.approveFriendReq(req);
    }

    @RequestMapping("/read")
    public ResponseVO read(@RequestBody @Validated ReadFriendshipRequestReq req , Integer appId) {
        req.setAppId(appId);
        return friendReqService.readFriendReq(req);
    }

    @RequestMapping("/getFriendReq")
    public ResponseVO getFriendReq(@RequestBody @Validated GetFriendRequestReq req , Integer appId) {
        return friendReqService.getFriendReq(req.getFromId() , appId);
    }
}
