package com.banking.controller;

import com.banking.model.dto.request.CreateAccountRequest;
import com.banking.model.dto.response.AccountResponse;
import com.banking.model.dto.response.ApiResponse;
import com.banking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse account = accountService.createAccount(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", account));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getMyAccounts(userDetails.getUsername())));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.getAccount(accountNumber, userDetails.getUsername())));
    }

    // Admin only — enforced via SecurityConfig + @PreAuthorize in service
    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@PathVariable String accountNumber) {
        accountService.deactivateAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated", null));
    }
}
