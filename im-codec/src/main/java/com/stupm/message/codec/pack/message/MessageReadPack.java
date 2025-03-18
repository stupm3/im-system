package com.stupm.message.codec.pack.message;

import lombok.Data;

@Data
public class MessageReadPack {
    private Long messageSequence;

    private String fromId;

    private String toId;

    private Integer conversationType;
}
