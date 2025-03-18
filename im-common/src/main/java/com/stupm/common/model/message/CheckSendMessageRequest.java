package com.stupm.common.model.message;

import lombok.Data;

@Data
public class CheckSendMessageRequest {
    private String fromId;

    private String toId;

    private Integer appId;

    private Integer command;
}
