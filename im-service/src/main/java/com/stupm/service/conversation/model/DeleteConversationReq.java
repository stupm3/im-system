package com.stupm.service.conversation.model;

import com.stupm.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DeleteConversationReq extends RequestBase {
    @NotBlank
    private String conversationId;

    @NotBlank
    private String formId;

}
