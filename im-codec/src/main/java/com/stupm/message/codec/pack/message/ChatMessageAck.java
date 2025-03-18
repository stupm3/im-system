package com.stupm.message.codec.pack.message;

import lombok.Data;

@Data
public class ChatMessageAck {


    public ChatMessageAck(String messageId) {
        this.messageId = messageId;
    }

    private String messageId;

    public ChatMessageAck(String messageId, Long messageSequence) {
        this.messageId = messageId;
        this.messageSequence = messageSequence;
    }

    private Long messageSequence;

}
