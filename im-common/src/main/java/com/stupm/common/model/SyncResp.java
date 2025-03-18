package com.stupm.common.model;

import lombok.Data;

import java.util.List;

@Data
public class  SyncResp<T> {
    private Long maxSeq;

    private boolean isCompleted;

    private List<T> dataList;
}
