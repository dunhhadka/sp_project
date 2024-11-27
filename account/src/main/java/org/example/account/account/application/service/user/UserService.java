package org.example.account.account.application.service.user;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.account.account.application.Constant;
import org.example.account.account.application.exception.ConstrainViolationException;
import org.example.account.account.application.mapper.UserMapper;
import org.example.account.account.application.mapper.UserPermissionMapper;
import org.example.account.account.application.model.user.request.UserAddRequest;
import org.example.account.account.application.model.user.request.UserPermissionRequest;
import org.example.account.account.application.model.user.response.UserResponse;
import org.example.account.account.domain.user.StoreRole;
import org.example.account.account.infrastructure.persistence.StoreRoleRepository;
import org.example.account.account.infrastructure.persistence.UserPermissionRepository;
import org.example.account.account.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StoreRoleRepository storeRoleRepository;
    private final UserPermissionRepository userPermissionRepository;

    private final UserMapper userMapper;
    private final UserPermissionMapper userPermissionMapper;

    @Transactional
    public UserResponse create(Integer storeId, UserAddRequest request) {
        //TODO: find store here
        var store = findStoreById(storeId);

        validateCreateUserRequest(store, request);

        // get store role
        List<StoreRole> storeRoles = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getUserPermissions())) {
            var storeRoleIds = request.getUserPermissions().stream()
                    .map(UserPermissionRequest::getStoreRoleId)
                    .filter(StringUtils::isNotBlank)
                    .distinct().toList();
            if (!CollectionUtils.isEmpty(storeRoleIds)) {
                storeRoles = storeRoleRepository.findByIdIn(storeRoleIds);
            }
        }

        // validate locations
        validateAndGetLocationWhenCreateUser(storeId, request);

        // set permission request
        if (!CollectionUtils.isEmpty(request.getUserPermissions())) {
            request.setPermissions(null);
            for (var userPermission : request.getUserPermissions()) {
                StoreRole storeRole = null;
                if (StringUtils.isNotBlank(userPermission.getStoreRoleId())) {
                    storeRole = storeRoles.stream()
                            .filter(role -> Objects.equals(role.getId(), userPermission.getStoreRoleId()))
                            .findFirst().
                            orElseThrow(() -> new ConstrainViolationException("store_role_id", "store_role not found"));
                }
                var permissionRequests = validateAndSetPermissionRequest(userPermission.getPermissions(), List.of("application"), storeRole, true);
                userPermission.setPermissions(permissionRequests);
            }
        } else {
            request.setPermissions(validateAndSetPermissionRequest(request.getPermissions(), List.of(), null, false));
        }


        var aggregatePermissions = aggregatePermissionsWithUserPermission(request.getPermissions(), request.getUserPermissions());
        var user = userMapper.fromRequestToModel(request);
        user.setCreatedOn(Instant.now());
        user.setPasswordSalt(RandomStringUtils.randomAlphanumeric(10));
        user.setPassword(user.getPassword() + user.getPasswordSalt());
        user.setLastLogin(null);
        user.setStoreId(storeId);
        user.setPermissions(request.getPermissions());
        user.setSso(store.isSso());

        userRepository.save(user);

        var userPermissions = userPermissionMapper.fromRequestToEntities(request.getUserPermissions());
        userPermissions.forEach(userPermission -> {
            userPermission.setUserId(user.getId());
            userPermission.setStoreId(storeId);
        });
        userPermissionRepository.saveAll(userPermissions);

        var response = userMapper.fromEntityToResponse(user);
        return response;
    }

    private List<String> aggregatePermissionsWithUserPermission(List<String> permissions, List<UserPermissionRequest> userPermissions) {
        List<String> result = new ArrayList<>();
        if (!CollectionUtils.isEmpty(permissions)) {
            result.addAll(permissions);
        }
        if (!CollectionUtils.isEmpty(userPermissions)) {
            for (var userPermission : userPermissions) {
                if (CollectionUtils.isEmpty(userPermission.getPermissions())) continue;
                result.addAll(userPermission.getPermissions());
            }
        }
        return result.stream().distinct().sorted().toList();
    }

    private List<String> validateAndSetPermissionRequest(
            List<String> inputPermissions,
            List<String> excludePermissions,
            StoreRole storeRole,
            boolean fromUserPermission
    ) {
        List<String> permissionRequests = new ArrayList<>();
        List<String> notAllowPermission = new ArrayList<>();
        List<String> storeRolePermission = storeRole != null ? storeRole.getPermissions() : new ArrayList<>();
        boolean isFullPermission = false;
        if (inputPermissions != null
                && inputPermissions.size() == 1
                && Objects.equals(inputPermissions.get(0), "full")
                && !fromUserPermission) {
            isFullPermission = true;
            permissionRequests.add("full");
        } else if (inputPermissions != null) {
            permissionRequests.addAll(inputPermissions.stream().distinct().sorted().toList());
        }

        if (!isFullPermission) {
            if (CollectionUtils.isEmpty(inputPermissions)) {
                return new ArrayList<>();
            }
            for (var permission : inputPermissions) {
                if (!Constant.DETAIL_PERMISSIONS.contains(permission)) {
                    notAllowPermission.add(permission);
                }
                if (storeRole != null) {
                    permissionRequests.removeIf(storeRolePermission::contains);
                }
            }
            if (!CollectionUtils.isEmpty(permissionRequests) && !CollectionUtils.isEmpty(excludePermissions)) {
                notAllowPermission.addAll(
                        permissionRequests.stream()
                                .filter(excludePermissions::contains)
                                .toList()
                );
            }
            if (!notAllowPermission.isEmpty()) {
                throw new ConstrainViolationException("permission_not_allow", StringUtils.join(notAllowPermission, ","));
            }
        }

        return permissionRequests;
    }

    private List<Location> validateAndGetLocationWhenCreateUser(Integer storeId, UserAddRequest request) {
        List<Location> locations = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getUserPermissions())) {
            var locationIds = request.getUserPermissions().stream()
                    .map(UserPermissionRequest::getLocationId)
                    .toList();
            Set<Long> uniqueLocationIds = new HashSet<>();
            for (var locationId : locationIds) {
                if (locationId == null || !uniqueLocationIds.add(locationId)) {
                    throw new ConstrainViolationException("location_id", "is duplicate or null");
                }
            }

            if (!CollectionUtils.isEmpty(locationIds)) {
                // tìm location ở service khác
            }
        }

        return locations;
    }

    private void validateCreateUserRequest(Store store, UserAddRequest request) {
        request.setPassword(validatePassword(store.isSso(), request.getPassword()));
        validateExistedByEmail(store.getId(), request.getEmail());
        request.setPhoneNumber(validateExistedPhoneNumber(store.getId(), request.getPhoneNumber()));
        validateMaxUser(store);
    }

    private void validateMaxUser(Store store) {
        var maxUser = store.getMaxUser();
        if (maxUser == null) {
            return;
        }
        var allUser = userRepository.findByStoreId(store.getId());
        var count = allUser.size();
        if (count >= maxUser) {
            throw new ConstrainViolationException("max_user", "maximum user is " + maxUser);
        }
    }

    private String validateExistedPhoneNumber(int storeId, String phoneNumber) {
        if (StringUtils.isBlank(phoneNumber)) {
            return phoneNumber;
        }
        var user = userRepository.findByStoreIdAndPhoneNumber(storeId, phoneNumber);
        if (user != null) {
            throw new ConstrainViolationException("phone_number", "Phone number already existed");
        }
        return phoneNumber;
    }

    private void validateExistedByEmail(int storeId, String email) {
        var user = userRepository.findByStoreIdAndEmail(storeId, email);
        if (user != null) {
            throw new ConstrainViolationException("email", "Email already existed");
        }
    }

    private String validatePassword(boolean sso, String password) {
        if (!sso && StringUtils.isBlank(password)) {
            throw new ConstrainViolationException("password", "password is required");
        }
        if (sso && StringUtils.isBlank(password)) {
            password = RandomStringUtils.randomAlphanumeric(50);
        }
        return password;
    }

    private Store findStoreById(Integer storeId) {
        return Store.builder()
                .id(storeId)
                .alias("hadung")
                .name("Store Dung Ha")
                .build();
    }

    @Getter
    @Builder
    public static class Store {
        private int id;
        private String alias;
        private String name;
        private boolean sso;
        @Builder.Default
        private Integer maxUser = 100;
    }

    @Getter
    @Setter
    public static class Location {
        private long id;
        private int storeId;
        private String code;
        private String name;
        private String email;
        private String phone;
        private String country;
        private String countryCode;
        private String province;
        private String provinceCode;
        private String district;
        private String districtCode;
    }
}
