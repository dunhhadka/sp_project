package org.example.account.account.domain.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document(collection = "user_permissions")
public class UserPermission {
    @Id
    private String id;
    private int storeId;
    private String userId;
    private int locationId;
    private int storeRoleId;

    private List<String> permissions; // không custom được như sql
}
