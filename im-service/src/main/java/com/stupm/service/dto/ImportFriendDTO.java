package com.stupm.service.dto;

import com.stupm.common.enums.FriendshipStatusEnum;
import lombok.Data;

@Data
public class ImportFriendDTO {
    private String toId;

    private String remark;

    private String addSource;

    private Integer status = FriendshipStatusEnum.FRIEND_STATUS_NO_FRIEND.getCode();

    private Integer black = FriendshipStatusEnum.BLACK_STATUS_NORMAL.getCode();
}
