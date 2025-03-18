package com.stupm.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserClientDTO {
    private Integer appId;

    private String userId;

    private Integer clientType;

    private String imei;
}
