package com.stupm.messageStore.model;

import com.stupm.common.model.message.MessageBody;
import com.stupm.common.model.message.MessageContent;
import com.stupm.messageStore.dao.MessageBodyEntity;
import lombok.Data;

@Data
public class StoreP2PMessageDTO {

    private MessageBodyEntity messageBody;
    private MessageContent messageContent;

}
