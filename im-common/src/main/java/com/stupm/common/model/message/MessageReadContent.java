package com.stupm.common.model.message;

import com.stupm.common.model.ClientInfo;
import lombok.Data;

@Data
public class MessageReadContent extends ClientInfo {
    private Long messageSequence;

    private String fromId;

    private String toId;

    private Integer conversationType;
}
