package org.example.account.account.infrastructure.persistence;

import org.example.account.account.domain.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    User findByStoreIdAndEmail(int storeId, String email);

    User findByStoreIdAndPhoneNumber(int storeId, String phoneNumber);

    List<User> findByStoreId(int storeId);
}
