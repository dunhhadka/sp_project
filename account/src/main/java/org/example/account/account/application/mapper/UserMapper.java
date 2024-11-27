package org.example.account.account.application.mapper;

import org.example.account.account.application.model.user.request.UserAddRequest;
import org.example.account.account.application.model.user.response.UserResponse;
import org.example.account.account.domain.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class UserMapper {
    public abstract User fromRequestToModel(UserAddRequest request);

    public abstract UserResponse fromEntityToResponse(User user);
}
