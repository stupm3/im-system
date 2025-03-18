package com.stupm.common.model.message;

import lombok.Data;

@Data
public class StoreGroupMessageDTO {
    private GroupChatMessageContent messageContent;

    private MessageBody messageBody;
}
