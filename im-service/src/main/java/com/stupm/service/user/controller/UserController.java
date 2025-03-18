package com.stupm.service.user.controller;

import com.stupm.common.ClientType;
import com.stupm.common.ResponseVO;
import com.stupm.common.route.RouteHandle;
import com.stupm.common.route.RouteInfo;
import com.stupm.common.utils.RouteInfoParseUtil;
import com.stupm.service.group.model.req.SubscribeUserOnlineReq;
import com.stupm.service.user.moudles.req.DeleteUserReq;
import com.stupm.service.user.moudles.req.GetUserSequenceReq;
import com.stupm.service.user.moudles.req.ImportUserReq;
import com.stupm.service.user.moudles.req.LoginReq;
import com.stupm.service.user.service.UserService;
import com.stupm.service.user.service.UserStatusService;
import com.stupm.service.utils.ZKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("v1/user")
public class UserController {

    @Autowired
    private RouteHandle routeHandle;

    @Autowired
    private ZKit zKit;


    @Autowired
    private UserService userService;

    @Autowired
    private UserStatusService userStatusService;

    @RequestMapping("/importUser")
    public ResponseVO importUser(@RequestBody @Validated ImportUserReq req , Integer appId) {
        req.setAppId(appId);
        return userService.importUser(req);
    }

    @RequestMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req , Integer appId) {
        req.setAppId(appId);
        return userService.deleteUser(req);
    }

    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req , Integer appId,String userSign) {
        req.setAppId(appId);
        req.setUserSign(userSign);
        ResponseVO login = userService.login(req);
        if(login.isOk()){
            List<String> allNode;
            if(req.getClientType() == ClientType.WEB.getCode()){
                 allNode = zKit.getAllWebNode();
            }else{
                allNode = zKit.getAllTcpNode();
            }
            String s = routeHandle.routeServer(allNode, req.getUserId());
            RouteInfo parse = RouteInfoParseUtil.parse(s);
            return ResponseVO.successResponse(parse);
        }
        return ResponseVO.errorResponse();
    }

    @RequestMapping("/getSequence")
    public ResponseVO getSequence(@RequestBody @Validated GetUserSequenceReq req , Integer appId) {
        req.setAppId(appId);
        return userService.getUserSequence(req);
    }

}
