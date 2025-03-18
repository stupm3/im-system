package com.stupm.service.conversation.controller;

import com.stupm.common.ResponseVO;
import com.stupm.common.model.SyncReq;
import com.stupm.service.conversation.model.DeleteConversationReq;
import com.stupm.service.conversation.model.UpdateConversationReq;
import com.stupm.service.conversation.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/conversation")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @RequestMapping("/delete")
    public ResponseVO deleteConversation(@RequestBody @Validated DeleteConversationReq req , Integer appId , String op) {
        req.setAppId(appId);
        req.setOperator(op);
        return conversationService.deleteConversation(req);
    }

    @RequestMapping("/update")
    public ResponseVO updateConversation(@RequestBody @Validated UpdateConversationReq req,Integer appId , String op) {
        req.setAppId(appId);
        req.setOperator(op);
        return conversationService.updateConversation(req);
    }

    @RequestMapping("/sync")
    public ResponseVO syncConversation(@RequestBody @Validated SyncReq req , Integer appId , String operator) {
        req.setAppId(appId);
        req.setOperator(operator);
        return conversationService.syncConversation(req);
    }

}
