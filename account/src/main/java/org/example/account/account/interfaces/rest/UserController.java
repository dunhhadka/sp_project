package org.example.account.account.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.example.account.account.application.model.user.request.UserAddRequest;
import org.example.account.account.application.model.user.response.UserResponse;
import org.example.account.account.application.service.user.UserService;
import org.example.account.account.infrastructure.configuration.bind.StoreId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public UserResponse create(@StoreId Integer storeId, @RequestBody @Valid UserAddRequest request) {
        return userService.create(storeId, request);
    }
}
