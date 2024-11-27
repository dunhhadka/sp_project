package org.example.account.account.infrastructure.persistence;


import org.example.account.account.domain.user.StoreRole;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StoreRoleRepository extends MongoRepository<StoreRole, String> {
    List<StoreRole> findByIdIn(List<String> ids);
}
