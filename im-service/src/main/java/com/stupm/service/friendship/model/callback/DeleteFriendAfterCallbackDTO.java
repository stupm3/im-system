package com.stupm.service.friendship.model.callback;

import lombok.Data;

@Data
public class DeleteFriendAfterCallbackDTO {

    private String fromId;

    private String toId;
}
