package com.stupm.service.friendship.model.req;

import com.stupm.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DeleteAllFriendReq extends RequestBase {
    @NotBlank
    private String fromId;
}
