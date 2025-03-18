package com.stupm.message.codec.pack.friendship;

import lombok.Data;


@Data
public class ApproverFriendRequestPack {

    private Long id;

    //1同意 2拒绝
    private Integer status;

    private Long sequence;
}
