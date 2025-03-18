package com.stupm.service.user.controller;

import com.stupm.common.ResponseVO;
import com.stupm.service.user.moudles.req.GetUserInfoReq;
import com.stupm.service.user.moudles.req.ModifyUserInfoReq;
import com.stupm.service.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/user/data")
public class UserDataController {
    @Autowired
    UserService userService;


    @RequestMapping("/getStringUserInfo")
    public ResponseVO getStringUserInfo(@RequestBody @Validated String userId , Integer appId) {
        return userService.getSingleUserInfo(userId , appId);
    }

    @RequestMapping("/getUserInfo")
    public ResponseVO getUserInfo(@RequestBody @Validated  GetUserInfoReq req , Integer appId) {
        req.setAppId(appId);
        return userService.getUserInfo(req);
    }

    @RequestMapping("/modifyUserInfo")
    public ResponseVO modifyUserInfo(@RequestBody @Validated  ModifyUserInfoReq req , Integer appId) {
        req.setAppId(appId);
        return userService.modifyUserInfo(req);
    }

}
;