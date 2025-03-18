package com.stupm.service.friendship.model.req;

import com.stupm.common.model.RequestBase;
import com.stupm.service.dto.ImportFriendDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ImportFriendshipReq extends RequestBase {
    @NotBlank
    private String fromId;

    private List<ImportFriendDTO> friendItem;


}
