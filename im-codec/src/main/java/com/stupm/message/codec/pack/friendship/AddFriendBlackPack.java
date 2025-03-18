package com.stupm.message.codec.pack.friendship;

import lombok.Data;


@Data
public class AddFriendBlackPack {
    private String fromId;

    private String toId;

    private Long sequence;
}
