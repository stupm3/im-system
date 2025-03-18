package com.stupm.common.model;

import lombok.Data;

@Data
public class RequestBase {
    private Integer appId;

    private String operator;

    private Integer ClientType;

    private String imei;
}
