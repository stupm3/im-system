package com.stupm.service.user.service;

import com.stupm.common.ResponseVO;
import com.stupm.common.model.message.UserStatusModifyContent;
import com.stupm.service.group.model.req.SubscribeUserOnlineReq;
import org.springframework.stereotype.Service;

public interface UserStatusService {
    void processUserOnlineStatusModify(UserStatusModifyContent content);


}
