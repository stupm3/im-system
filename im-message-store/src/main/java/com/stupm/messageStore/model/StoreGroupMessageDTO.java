package com.stupm.messageStore.model;

import com.stupm.common.model.message.GroupChatMessageContent;
import com.stupm.common.model.message.MessageContent;
import com.stupm.messageStore.dao.MessageBodyEntity;
import lombok.Data;

@Data
public class StoreGroupMessageDTO {
    private MessageBodyEntity messageBody;
    private GroupChatMessageContent messageContent;
}
