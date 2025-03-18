package com.stupm.service.group.controller;

import com.stupm.common.ResponseVO;
import com.stupm.common.model.SyncReq;
import com.stupm.service.group.model.req.*;
import com.stupm.service.group.service.GroupMessageService;
import com.stupm.service.group.service.GroupService;
import com.stupm.service.message.model.req.SendMessageReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupMessageService messageService;

    @RequestMapping("/import")
    public ResponseVO importGroup(@RequestBody @Validated ImportGroupReq importGroupReq , Integer appId , String operator) {
        importGroupReq.setAppId(appId);
        importGroupReq.setOperator(operator);
        return groupService.importGroup(importGroupReq);
    }

    @RequestMapping("/updateGroupInfo")
    public ResponseVO updateGroupInfo(@RequestBody @Validated  UpdateGroupReq req, Integer appid , String operator) {
        req.setAppId(appid);
        req.setOperator(operator);
        return groupService.updateGroupInfo(req);
    }

    @RequestMapping("/create")
    public ResponseVO createGroup(@RequestBody @Validated  CreateGroupReq req , Integer appId , String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return groupService.createGroup(req);
    }

    @RequestMapping("/getJoinedGroup")
    public ResponseVO getJoinedGroup(@RequestBody @Validated  GetJoinedGroupReq req , Integer appid , String operator) {
        req.setAppId(appid);
        req.setOperator(operator);
        return groupService.getJoinedGroup(req);
    }


    @RequestMapping("/getInfo")
    public ResponseVO getGroupInfo(@RequestBody @Validated  GetGroupInfoReq req , Integer appid , String operator) {
        req.setAppId(appid);
        req.setOperator(operator);
        return groupService.getGroupInfo(req);
    }

    @RequestMapping("/destroy")
    public ResponseVO destroyGroup(@RequestBody @Validated DestroyGroupReq req , Integer appid , String operator) {
        req.setAppId(appid);
        req.setOperator(operator);
        return groupService.destroyGroup(req);
    }

    @RequestMapping("/transfer")
    public ResponseVO transferGroup(@RequestBody @Validated  TransferGroupReq req , Integer appid , String operator) {
        req.setAppId(appid);
        req.setOperator(operator);
        return groupService.transferGroup(req);
    }

    @RequestMapping("/mute")
    public ResponseVO muteGroup(@RequestBody @Validated  MuteGroupReq req , Integer appid , String operator) {
        req.setAppId(appid);
        req.setOperator(operator);
        return groupService.muteGroup(req);
    }

    @RequestMapping("/sendMessage")
    public ResponseVO sendMessage(@RequestBody @Validated SendGroupMessageReq req , Integer appid , String operator) {
        req.setAppId(appid);
        req.setOperator(operator);
        return ResponseVO.successResponse(messageService.send(req));
    }

    @RequestMapping("/sync")
    public ResponseVO syncJoinedGroup(@RequestBody @Validated SyncReq req,Integer appId , String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return groupService.syncJoinedGroup(req);
    }
}
