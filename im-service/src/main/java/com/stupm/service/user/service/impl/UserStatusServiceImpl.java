package com.stupm.service.user.service.impl;

import com.stupm.common.ResponseVO;
import com.stupm.common.enums.command.UserEventCommand;
import com.stupm.common.model.ClientInfo;
import com.stupm.common.model.UserSession;
import com.stupm.common.model.message.UserStatusModifyContent;
import com.stupm.message.codec.pack.user.UserStatusChangeNotifyPack;
import com.stupm.service.friendship.service.FriendshipService;
import com.stupm.service.group.model.req.SubscribeUserOnlineReq;
import com.stupm.service.user.service.UserStatusService;
import com.stupm.service.utils.MessageProducer;
import com.stupm.service.utils.UserSessionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserStatusServiceImpl implements UserStatusService {

    @Autowired
    UserSessionUtils userSessionUtils;

    @Autowired
    MessageProducer producer;

    @Autowired
    FriendshipService friendshipService;
    @Autowired
    private MessageProducer messageProducer;

    @Override
    public void processUserOnlineStatusModify(UserStatusModifyContent content) {
        List<UserSession> userSession = userSessionUtils.getUserSession(content.getAppId(), content.getUserId());
        UserStatusChangeNotifyPack pack = new UserStatusChangeNotifyPack();
        BeanUtils.copyProperties(content, pack);
        pack.setClient(userSession);
        sync(pack,content.getUserId(),content);
        dispatch(pack,content.getUserId(),content.getAppId());

    }
    

    private void sync(Object pack , String userId , ClientInfo clientInfo){
        producer.sendToUserAnotherClient(userId, UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC,pack,clientInfo);
    }

    private void dispatch(Object pack , String userId,Integer appId ){
        List<String> allFriendsId = friendshipService.getAllFriendsId(userId, appId);
        for (String friendId : allFriendsId) {
            messageProducer.sendToUser(appId,friendId,UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY,pack);
        }
    }
}
