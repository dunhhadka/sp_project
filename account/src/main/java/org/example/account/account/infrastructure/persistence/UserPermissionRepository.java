package org.example.account.account.infrastructure.persistence;

import org.example.account.account.domain.user.UserPermission;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserPermissionRepository extends MongoRepository<UserPermission, String> {
}
