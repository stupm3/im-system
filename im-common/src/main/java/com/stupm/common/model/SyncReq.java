package com.stupm.common.model;

import lombok.Data;

@Data
public class SyncReq extends RequestBase {
    private Long lastSeq;

    private Integer maxLimit;
}
