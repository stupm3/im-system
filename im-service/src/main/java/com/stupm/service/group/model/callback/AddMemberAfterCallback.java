package com.stupm.service.group.model.callback;

import com.stupm.service.group.model.resp.AddGroupMemberResp;
import lombok.Data;

import java.util.List;
@Data
public class AddMemberAfterCallback {
    private String groupId;
    private Integer groupType;
    private String operator;
    private List<AddGroupMemberResp> memberId;
}
