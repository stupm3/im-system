package com.stupm.service.group.model.req;

import com.stupm.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateGroupReq extends RequestBase {

    @NotBlank(message = "群id不能为空")
    private String groupId;

    private String groupName;

    private Integer mute; //全体禁言 0 no 1 yes

    private Integer applyJoinType; // 0 所有人可加入 1 群成员拉人 2 群管理拉人

    private String groupIntroduction;

    private String notification;

    private String photo;

    private String extra;

    private Integer MaxMemberCount;
}
