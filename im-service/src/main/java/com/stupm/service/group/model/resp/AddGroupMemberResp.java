package com.stupm.service.group.model.resp;

import lombok.Data;

@Data
public class AddGroupMemberResp {
    private String memberId;

    // 0为成功 1为失败 2为已是群成员
    private Integer result;

    private String resultMessage;
}
