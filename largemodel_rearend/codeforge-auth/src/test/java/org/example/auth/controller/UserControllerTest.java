/**
 * 模块：测试
 * 功能：UserController 集成测试，覆盖获取/更新用户信息、修改密码
 * 作者：yx
 * 创建时间：2026-06-18
 */
package org.example.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.JwtUtil;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.UserPasswordRequest;
import org.example.dto.request.UserUpdateRequest;
import org.example.entity.User;
import org.example.enums.UserRole;
import org.example.enums.UserStatus;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String testToken;
    private static final String TEST_USERNAME = "testuser_uc";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        // Clean up leftover from previous runs
        userRepository.findByUsernameAndDeletedFalse(TEST_USERNAME)
                .ifPresent(u -> userRepository.delete(u));

        // Create test user
        testUser = User.builder()
                .username(TEST_USERNAME)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .email("test_uc@example.com")
                .nickname("TestUC")
                .phone("13800001111")
                .role(UserRole.USER)
                .status(UserStatus.ENABLED.getCode())
                .deleted(false)
                .build();
        testUser.setCreatedBy("test");
        testUser = userRepository.save(testUser);

        // Generate JWT token
        testToken = jwtUtil.generateToken(testUser.getId(), testUser.getUsername(), testUser.getRole().name());
    }

    @AfterEach
    void tearDown() {
        userRepository.findById(testUser.getId())
                .ifPresent(u -> userRepository.delete(u));
    }

    // ==================== Get User Info Tests ====================

    @Test
    void getUserInfo_authenticated() throws Exception {
        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.email").value("test_uc@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("TestUC"))
                .andExpect(jsonPath("$.data.phone").value("13800001111"));
    }

    @Test
    void getUserInfo_noToken() throws Exception {
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isForbidden());
    }

    // ==================== Update User Info Tests ====================

    @Test
    void updateUserInfo_success() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("UpdatedNick");
        request.setEmail("updated@example.com");
        request.setPhone("13900002222");

        mockMvc.perform(put("/api/user/info")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.nickname").value("UpdatedNick"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                .andExpect(jsonPath("$.data.phone").value("13900002222"));
    }

    @Test
    void updateUserInfo_partialUpdate() throws Exception {
        // Only update nickname, email and phone should remain unchanged
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("PartialNick");

        mockMvc.perform(put("/api/user/info")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("PartialNick"))
                .andExpect(jsonPath("$.data.email").value("test_uc@example.com"))
                .andExpect(jsonPath("$.data.phone").value("13800001111"));
    }

    // ==================== Change Password Tests ====================

    @Test
    void changePassword_success() throws Exception {
        UserPasswordRequest request = new UserPasswordRequest();
        request.setOldPassword(TEST_PASSWORD);
        request.setNewPassword("newSecure456");

        mockMvc.perform(put("/api/user/password")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("密码修改成功"));

        // Verify login with new password works
        LoginRequest loginWithNew = new LoginRequest();
        loginWithNew.setUsername(TEST_USERNAME);
        loginWithNew.setPassword("newSecure456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginWithNew)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void changePassword_wrongOldPassword() throws Exception {
        UserPasswordRequest request = new UserPasswordRequest();
        request.setOldPassword("wrongOldPassword");
        request.setNewPassword("newSecure456");

        mockMvc.perform(put("/api/user/password")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("原密码错误")));
    }
}
