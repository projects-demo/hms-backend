package com.hms.tenancy.master.web;

import com.hms.common.dto.ApiResponse;
import com.hms.identity.dto.LoginResponse;
import com.hms.tenancy.master.dto.PlatformLoginRequest;
import com.hms.tenancy.master.service.PlatformAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/auth")
@RequiredArgsConstructor
@Tag(name = "Platform Auth", description = "Login for platform-level staff who manage hospital onboarding")
public class PlatformAuthController {

    private final PlatformAuthService platformAuthService;

    @PostMapping("/login")
    @Operation(summary = "Platform admin login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody PlatformLoginRequest request) {
        return ApiResponse.ok(platformAuthService.login(request.username(), request.password()));
    }
}
