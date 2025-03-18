package com.stupm.message.codec.pack.user;

import com.stupm.common.model.UserSession;
import lombok.Data;

import java.util.List;

@Data
public class UserStatusChangeNotifyPack {

    private Integer appId;

    private String userId;

    private Integer status;

    private List<UserSession> client;

}
