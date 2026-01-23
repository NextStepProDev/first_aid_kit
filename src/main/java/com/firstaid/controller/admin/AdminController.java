package com.firstaid.controller.admin;

import com.firstaid.controller.dto.admin.DeleteUserRequest;
import com.firstaid.controller.dto.admin.UserResponse;
import com.firstaid.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.web.SortDefault;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only endpoints for user management")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "List all users",
            description = "Returns a paginated list of all users. Admin only.<br><br>" +
                    "<b>Sortable fields:</b> userId, userName, email, name, active, createdAt, lastLogin<br><br>" +
                    "<b>Sort format:</b> fieldName,direction (direction: asc or desc)<br><br>" +
                    "<b>Examples:</b><br>" +
                    "• ?sort=createdAt,desc → newest users first<br>" +
                    "• ?sort=email,asc → alphabetically by email<br>" +
                    "• ?sort=lastLogin,desc&sort=email,asc → multiple sort<br>" +
                    "• ?page=0&size=10&sort=userId,desc → with pagination",
            parameters = {
                    @Parameter(name = "page", description = "Page number (0-based)", example = "0"),
                    @Parameter(name = "size", description = "Page size", example = "20"),
                    @Parameter(name = "sort", description = "Sort criteria: fieldName,direction. Available fields: userId, userName, email, name, active, createdAt, lastLogin", example = "createdAt,desc")
            })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of users returned successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public Page<UserResponse> getAllUsers(
            @Parameter(hidden = true)
            @SortDefault(sort = "userId") Pageable pageable) {
        return adminService.getAllUsers(pageable);
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user",
            description = "Deletes a user account and all associated data. Requires admin password confirmation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or trying to delete own account"),
            @ApiResponse(responseCode = "401", description = "Invalid admin password"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public void deleteUser(
            @Parameter(description = "ID of user to delete") @Positive @PathVariable Integer userId,
            @Valid @RequestBody DeleteUserRequest request) {
        adminService.deleteUser(userId, request.password());
    }
}
