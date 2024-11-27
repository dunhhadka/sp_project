package org.example.account.account.domain.user;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Document(collection = "store_roles")
public class StoreRole {
    @Id
    private String id;
    private int storeId;

    private String name;

    private List<String> permissions;
}
