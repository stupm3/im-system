package com.stupm.service.group.model.req;

import com.stupm.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
@Data
public class RemoveGroupMemberReq extends RequestBase {

    @NotBlank(message = "群id不能为空")
    private String groupId;

    private String memberId;

}
