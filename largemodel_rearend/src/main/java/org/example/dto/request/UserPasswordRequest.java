package org.example.dto.request;

import lombok.Data;

@Data
public class UserPasswordRequest {
    private String oldPassword;
    private String newPassword;
}
