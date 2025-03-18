package com.stupm.message.tcp.feign;

import com.stupm.common.ResponseVO;
import com.stupm.common.model.message.CheckSendMessageRequest;
import feign.Headers;
import feign.RequestLine;

public interface FeignMessageService {

    @Headers({"Content-Type: application/json" , "Accept: application/json"})
    @RequestLine("POST /message/checkSend")
    public ResponseVO checkSendMessage(CheckSendMessageRequest req);
}
