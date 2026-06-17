package br.com.stella.api.controller;

import br.com.stella.api.dto.ChangePasswordDTO;
import br.com.stella.api.dto.MyProfileResponseDTO;
import br.com.stella.api.dto.MyProfileUpdateDTO;
import br.com.stella.api.dto.UserCreateDTO;
import br.com.stella.api.dto.UserResponseDTO;
import br.com.stella.api.dto.UserUpdateDTO;
import br.com.stella.api.service.KeycloakUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing users via Keycloak.
 *
 * <p>Exposes the {@code /api/v0/users} resource with administrative operations
 * (restricted to the {@code admin} role) and operations by the current user ({@code /me}).</p>
 *
 * <p>All changes are propagated directly to Keycloak via the Admin API,
 * without location persistence.</p>
 */
@RestController
@RequestMapping("/api/v0/users")
public class UserController {

    private final KeycloakUserService service;

    /**
     * Constructs the controller injecting the Keycloak user service.
     *
     * @param service service responsible for operations on the Keycloak Admin API
     */
    public UserController(KeycloakUserService service) {
        this.service = service;
    }

    /**
     * Lists all users registered in Keycloak. Requires the {@code admin} role.
     *
     * @return {@code 200 OK} with the list of users
     */
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<UserResponseDTO>> list() {
        return ResponseEntity.ok(service.list());
    }

    /**
     * Finds a user by their ID in Keycloak. Requires the {@code admin} role.
     *
     * @param id user ID in Keycloak (UUID as string)
     * @return {@code 200 OK} with the user data
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Creates a new user in Keycloak. Requires the {@code admin} role.
     *
     * @param dto new user data validated by Bean Validation
     * @return {@code 201 Created} with the created user data
     */
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UserResponseDTO> create(@RequestBody @Valid UserCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    /**
     * Updates a user's data in Keycloak. Requires the {@code admin} role.
     *
     * @param id  user ID in Keycloak
     * @param dto update data validated by Bean Validation
     * @return {@code 200 OK} with the updated user data
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UserResponseDTO> update(@PathVariable String id, @RequestBody @Valid UserUpdateDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * Activates or deactivates a user in Keycloak. Requires the {@code admin} role.
     *
     * @param id   user ID in Keycloak
     * @param body map with the key {@code "enabled"} and a boolean value
     * @return {@code 204 In Content} on success
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> changeStatus(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        service.changeStatus(id, Boolean.TRUE.equals(body.get("enabled")));
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the authenticated user's profile, including name, e-mail and roles.
     *
     * @param jwt JWT token of the authenticated user
     * @return {@code 200 OK} with the profile data
     */
    @GetMapping("/me")
    public ResponseEntity<MyProfileResponseDTO> myProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.myProfile(jwt));
    }

    /**
     * Updates the name and e-mail of the authenticated user in Keycloak.
     *
     * @param jwt JWT token of the authenticated user
     * @param dto profile update data validated by Bean Validation
     * @return {@code 200 OK} with the updated profile
     */
    @PutMapping("/me")
    public ResponseEntity<MyProfileResponseDTO> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid MyProfileUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.updateMyProfile(jwt, dto));
    }

    /**
     * Changes the password of the authenticated user in Keycloak.
     *
     * @param jwt JWT token of the authenticated user
     * @param dto DTO with the current and new passwords validated by Bean Validation
     * @return {@code 204 In Content} on success
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ChangePasswordDTO dto
    ) {
        service.changeMyPassword(jwt, dto);
        return ResponseEntity.noContent().build();
    }
}
