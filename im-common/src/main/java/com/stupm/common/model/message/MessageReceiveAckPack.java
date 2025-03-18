package com.stupm.common.model.message;

import com.stupm.common.model.ClientInfo;
import lombok.Data;

@Data
public class MessageReceiveAckPack extends ClientInfo {
    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageSequence;

    private Integer appId;

}
