package org.example.account.account.application.model.user.request;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@JsonRootName("user")
public class UserAddRequest {
    @NotBlank
    @Size(max = 128)
    private String email;

    @NotBlank
    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 255)
    private String description;

    private boolean receivedAnnouncements;

    private String password;

    private List<String> permissions;

    private List<@Valid UserPermissionRequest> userPermissions;
}
