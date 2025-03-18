package com.stupm.service.group.controller;

import com.stupm.common.ResponseVO;
import com.stupm.service.group.model.req.*;
import com.stupm.service.group.service.GroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/group/member")
public class GroupMemberController {

    @Autowired
    private GroupMemberService groupMemberService;

    @RequestMapping("/import")
    public ResponseVO importGroupMember(@RequestBody @Validated ImportGroupMemberReq req, Integer appid , String operator) {
        req.setAppId(appid);
        req.setOperator(operator);
        return groupMemberService.importGroupMember(req);
    }

    @RequestMapping("/add")
    public ResponseVO addMember(@RequestBody @Validated AddGroupMemberReq req, Integer appId , String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return groupMemberService.addMember(req);
    }

    @RequestMapping("/remove")
    public ResponseVO remove(@RequestBody @Validated RemoveGroupMemberReq req, Integer appId , String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return groupMemberService.removeMember(req);
    }

    @RequestMapping("/mute")
    public ResponseVO mute(@RequestBody @Validated MuteMemberReq req , Integer appId , String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return groupMemberService.muteGroupMember(req);
    }

    @RequestMapping("/update")
    public ResponseVO update(@RequestBody @Validated UpdateGroupMemberReq req, Integer appId , String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return groupMemberService.updateGroupMember(req);
    }

}
