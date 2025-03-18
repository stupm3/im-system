package com.stupm.common.model.message;

import com.stupm.common.model.ClientInfo;
import lombok.Data;

@Data
public class UserStatusModifyContent extends ClientInfo {
    private String userId;

    private Integer status;
}
