package com.stupm.service.friendship.model.callback;

import com.stupm.service.dto.FriendshipDTO;
import lombok.Data;

@Data
public class AddFriendAfterCallbackDTO {
    private String fromId;

    private FriendshipDTO friendshipDTO;
}
