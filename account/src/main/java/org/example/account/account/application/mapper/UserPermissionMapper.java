package org.example.account.account.application.mapper;

import org.example.account.account.application.model.user.request.UserPermissionRequest;
import org.example.account.account.domain.user.UserPermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class UserPermissionMapper {

    @Mapping(target = "permissions", ignore = true)
    public abstract UserPermission fromRequestToEntity(UserPermissionRequest userPermissionRequest);

    public List<UserPermission> fromRequestToEntities(List<UserPermissionRequest> userPermissionRequests) {
        var userPermissions = new ArrayList<UserPermission>();
        if (userPermissionRequests == null) {
            return userPermissions;
        }
        for (var userPermissionRequest : userPermissionRequests) {
            var userPermission = fromRequestToEntity(userPermissionRequest);
            userPermission.setPermissions(userPermissionRequest.getPermissions());
            userPermissions.add(userPermission);
        }
        return userPermissions;
    }
}
