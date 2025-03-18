package com.stupm.service.friendship.controller;

import com.stupm.common.ResponseVO;
import com.stupm.common.model.SyncReq;
import com.stupm.service.friendship.model.req.*;
import com.stupm.service.friendship.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("v1/friendship")
public class FriendshipController {
    @Autowired
    FriendshipService friendshipService;

    @RequestMapping("/importFriendship")
    public ResponseVO importFriendship(@RequestBody @Validated ImportFriendshipReq req , Integer appId) {
        req.setAppId(appId);
        return friendshipService.importFriendship(req);
    }

    @RequestMapping("/addFriend")
    public ResponseVO addFriend(@RequestBody @Validated AddFriendReq req , Integer appId) {
        req.setAppId(appId);
        return friendshipService.addFriendship(req);
    }

    @RequestMapping("/updateFriend")
    public ResponseVO updateFriend(@RequestBody @Validated UpdateFriendReq req , Integer appId) {
        req.setAppId(appId);
        return friendshipService.updateFriendship(req);
    }

    @RequestMapping("/deleteFriend")
    public ResponseVO deleteFriend(@RequestBody @Validated DeleteFriendReq req , Integer appId) {
        req.setAppId(appId);
        return friendshipService.deleteFriendship(req);
    }

    @RequestMapping("/deleteAllFriend")
    public ResponseVO deleteAllFriend(@RequestBody @Validated DeleteAllFriendReq req , Integer appId) {
        req.setAppId(appId);
        return friendshipService.deleteAllFriendship(req);
    }

    @RequestMapping("/getAllFriendship")
    public ResponseVO getAllFriendship(@RequestBody @Validated GetAllFriendshipReq req , Integer appId) {
        req.setAppId(appId);
        return friendshipService.getAllFriendShip(req);
    }

    @RequestMapping("/getRelationship")
    public ResponseVO getRelationship(@RequestBody @Validated GetRelationshipReq req , Integer appId) {
        req.setAppId(appId);
        return friendshipService.getRelationship(req);
    }
    
    @RequestMapping("/checkRelationship")
    public ResponseVO checkRelationship(@RequestBody @Valid CheckFriendShipReq req , Integer appId) {
        req.setAppId(appId);
        return friendshipService.checkRelationShip(req);
    }

    @RequestMapping("/addBlack")
    public ResponseVO addBlack(@RequestBody @Valid AddBlackReq req , Integer appId){
        req.setAppId(appId);
        return friendshipService.addBlack(req);
    }

    @RequestMapping("/checkBlack")
    public ResponseVO checkBlack(@RequestBody @Valid CheckFriendShipReq req , Integer appId){
        req.setAppId(appId);
        return friendshipService.checkBlack(req);
    }

    @RequestMapping("/deleteBlack")
    public ResponseVO deleteBlack(@RequestBody @Valid DeleteBlackReq req , Integer appId){
        req.setAppId(appId);
        return friendshipService.deleteBlack(req);
    }

    @RequestMapping("/syncFriendship")
    public ResponseVO syncFriendship(@RequestBody @Validated SyncReq req , Integer appId,String operator){
        req.setAppId(appId);
        req.setOperator(operator);
        return friendshipService.syncFriendship(req);
    }


}
