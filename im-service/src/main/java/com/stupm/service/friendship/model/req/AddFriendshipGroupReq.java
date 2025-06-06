package com.stupm.service.friendship.model.req;

import com.stupm.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;


@Data
public class AddFriendshipGroupReq extends RequestBase {

    @NotBlank(message = "fromId不能为空")
    public String fromId;

    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    private List<String> toIds;

}
