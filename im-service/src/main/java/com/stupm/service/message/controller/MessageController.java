package com.stupm.service.message.controller;

import com.stupm.common.ResponseVO;
import com.stupm.common.enums.command.MessageCommand;
import com.stupm.common.model.SyncReq;
import com.stupm.common.model.message.CheckSendMessageRequest;
import com.stupm.service.group.service.GroupMessageService;
import com.stupm.service.message.model.req.SendMessageReq;
import com.stupm.service.message.service.MessageSyncService;
import com.stupm.service.message.service.P2PMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/message")
public class MessageController {
    @Autowired
    P2PMessageService p2PMessageService;

    @Autowired
    GroupMessageService groupMessageService;

    @Autowired
    MessageSyncService messageSyncService;

    @RequestMapping("/send")
    public ResponseVO send(@RequestBody @Validated SendMessageReq req, Integer appId){
        req.setAppId(appId);
        return ResponseVO.successResponse(p2PMessageService.send(req));
    }

    @RequestMapping("/checkSend")
    public ResponseVO checkSend(@RequestBody @Validated CheckSendMessageRequest req){
        if (req.getCommand().equals(MessageCommand.MSG_P2P.getCommand())) {
            return p2PMessageService.serverPermissionCheck(req.getFromId(),req.getToId(),req.getAppId());
        }
        return groupMessageService.serverPermissionCheck(req.getFromId(),req.getToId(),req.getAppId());
    }

    @RequestMapping("/SyncOfflineMessage")
    public ResponseVO SyncOfflineMessage(@RequestBody @Validated SyncReq req,Integer appId,String operator){
        req.setAppId(appId);
        req.setOperator(operator);
        return messageSyncService.syncOfflineMessage(req);
    }

}
