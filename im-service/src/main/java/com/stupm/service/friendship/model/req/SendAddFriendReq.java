package com.stupm.service.friendship.model.req;

import com.stupm.common.model.RequestBase;
import lombok.Data;

@Data
public class SendAddFriendReq extends RequestBase{

    private String fromId;

    private String toId;

    private String AddWording;

    private String remark;

    private String addSource;

    private String createTime;

    private String extra;
}
