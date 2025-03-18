package com.stupm.service.user.moudles.req;

import com.stupm.common.model.RequestBase;
import lombok.Data;

@Data
public class GetUserSequenceReq extends RequestBase {
    private String userId;
}
