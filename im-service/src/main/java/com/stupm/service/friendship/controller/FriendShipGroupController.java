package com.stupm.service.friendship.controller;


import com.stupm.common.ResponseVO;
import com.stupm.service.friendship.model.req.AddFriendshipGroupMemberReq;
import com.stupm.service.friendship.model.req.AddFriendshipGroupReq;
import com.stupm.service.friendship.model.req.DeleteFriendshipGroupMemberReq;
import com.stupm.service.friendship.model.req.DeleteFriendshipGroupReq;
import com.stupm.service.friendship.service.FriendshipGroupMemberService;
import com.stupm.service.friendship.service.FriendshipGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Chackylee
 * @description:
 **/
@RestController
@RequestMapping("v1/friendship/group")
public class FriendShipGroupController {

    @Autowired
    FriendshipGroupService friendShipGroupService;

    @Autowired
    FriendshipGroupMemberService friendShipGroupMemberService;


    @RequestMapping("/add")
    public ResponseVO add(@RequestBody @Validated AddFriendshipGroupReq req, Integer appId)  {
        req.setAppId(appId);
        return friendShipGroupService.addGroup(req);
    }

    @RequestMapping("/del")
    public ResponseVO del(@RequestBody @Validated DeleteFriendshipGroupReq req, Integer appId)  {
        req.setAppId(appId);
        return friendShipGroupService.deleteGroup(req);
    }

    @RequestMapping("/member/add")
    public ResponseVO memberAdd(@RequestBody @Validated AddFriendshipGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return friendShipGroupMemberService.addGroupMember(req);
    }

    @RequestMapping("/member/del")
    public ResponseVO memberdel(@RequestBody @Validated DeleteFriendshipGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return friendShipGroupMemberService.delGroupMember(req);
    }

}
