package com.nl2sql.gate.auth;

import com.nl2sql.gate.auth.dto.AuthResponse;
import com.nl2sql.gate.auth.dto.LoginRequest;
import com.nl2sql.gate.auth.dto.SignupRequest;
import com.nl2sql.gate.auth.dto.UserResponse;
import com.nl2sql.gate.security.JwtService;
import com.nl2sql.gate.tenant.DefaultTenantProvisioner;
import com.nl2sql.gate.user.AppUser;
import com.nl2sql.gate.user.AppUserRepository;
import com.nl2sql.gate.user.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final DefaultTenantProvisioner tenantProvisioner;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        AppUserRepository appUserRepository,
        DefaultTenantProvisioner tenantProvisioner,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.tenantProvisioner = tenantProvisioner;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse signup(SignupRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        Role role = request.role() != null ? request.role() : Role.STAFF;
        String passwordHash = passwordEncoder.encode(request.password());
        AppUser user = appUserRepository.insert(
            tenantProvisioner.getDefaultTenantId(), request.email(), request.name(), role, passwordHash
        );
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(request.email())
            .filter(AppUser::active)
            .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return toAuthResponse(user);
    }

    public UserResponse me(String userId) {
        AppUser user = appUserRepository.findById(UUID.fromString(userId))
            .orElseThrow(InvalidCredentialsException::new);
        return new UserResponse(user.id(), user.email(), user.name(), user.role(), user.roleLevel());
    }

    private AuthResponse toAuthResponse(AppUser user) {
        String token = jwtService.generateToken(
            user.id(), user.email(), user.role(), user.roleLevel(), user.tenantId()
        );
        return new AuthResponse(token, "Bearer", jwtService.expirationMs(), user.name(), user.role());
    }
}
