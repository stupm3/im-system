package com.stupm.service.friendship.model.req;

import com.stupm.common.model.RequestBase;
import com.stupm.service.dto.FriendshipDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateFriendReq extends RequestBase {
    @NotBlank(message = "fromId不能为空")
    private String fromId;

    private FriendshipDTO toItem;
}
