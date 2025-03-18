package com.stupm.service.user.moudles.req;

import com.stupm.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotNull;


@Data
public class LoginReq extends RequestBase {

    @NotNull(message = "用户id不能位空")
    private String userId;

    private String userSign;


}
