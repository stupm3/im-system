package com.stupm.service.friendship.model.req;

import com.stupm.common.model.RequestBase;
import lombok.Data;

@Data
public class GetFriendRequestReq extends RequestBase {
    private String fromId;
}
