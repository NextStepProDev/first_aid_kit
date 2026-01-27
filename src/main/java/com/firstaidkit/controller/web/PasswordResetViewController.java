package com.firstaidkit.controller.web;

import com.firstaidkit.controller.dto.auth.ResetPasswordRequest;
import com.firstaidkit.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PasswordResetViewController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("success", false);

        boolean isValid = passwordResetService.isTokenValid(token);

        if (!isValid) {
            model.addAttribute("error", "Invalid or expired token. Please request a new password reset.");
            model.addAttribute("tokenValid", false);
        } else {
            model.addAttribute("token", token);
            model.addAttribute("tokenValid", true);
        }

        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {

        try {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token(token)
                    .newPassword(newPassword)
                    .confirmPassword(confirmPassword)
                    .build();

            passwordResetService.resetPassword(request);
            model.addAttribute("success", true);
            model.addAttribute("tokenValid", false);
            model.addAttribute("message", "Your password has been reset successfully. You can now login with your new password.");

        } catch (Exception e) {
            log.warn("Password reset failed: {}", e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("token", token);
            model.addAttribute("tokenValid", true);
            model.addAttribute("error", e.getMessage());
        }

        return "reset-password";
    }
}
