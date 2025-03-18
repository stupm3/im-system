package com.stupm.common.model.message;

import lombok.Data;

@Data
public class StoreP2PMessageDTO {
    private MessageContent messageContent;

    private MessageBody messageBody;
}
