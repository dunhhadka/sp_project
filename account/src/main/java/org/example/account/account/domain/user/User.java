package org.example.account.account.domain.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @NotNull
    @Email
    private String email;

    @NotBlank
    @Size(max = 64, min = 3)
    private String password;

    @NotBlank
    @Size(max = 10)
    private String passwordSalt;

    @NotBlank
    @Size(max = 250)
    private String firstName;

    @Size(max = 250)
    private String lastName;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 255)
    private String description;

    private Instant lastLogin;

    private boolean accountOwner;

    private boolean receivedAnnouncements;

    private boolean sso;

    @NotNull
    private Instant createdOn;
    private Instant modifiedOn;

    private int storeId;

    private List<String> permissions;
}
