package org.example.account.account.application.model.user.request;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import java.util.List;

@Getter
@Setter
@JsonRootName("user_permission")
public class UserPermissionRequest {
    private String storeRoleId;
    @Min(1)
    private Long locationId;
    private List<String> permissions;
}
