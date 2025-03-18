package com.stupm.service.group.model.req;

import com.stupm.common.model.RequestBase;
import lombok.Data;

import java.util.List;

@Data
public class SubscribeUserOnlineReq extends RequestBase {
    private List<String> subId;

    private Long subTime;
}
