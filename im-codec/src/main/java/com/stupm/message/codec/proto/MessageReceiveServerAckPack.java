package com.stupm.message.codec.proto;

import com.stupm.common.model.ClientInfo;
import lombok.Data;

@Data
public class MessageReceiveServerAckPack extends ClientInfo {
    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageSequence;

    private Boolean serverSend;
}
