package com.stupm.service.group.model.req;

import com.stupm.common.model.RequestBase;
import com.stupm.service.group.model.dto.GroupMemberDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ImportGroupMemberReq extends RequestBase {
    @NotBlank(message = "群Id不能为空")
    private String groupId;

    private List<GroupMemberDTO> members;
}
