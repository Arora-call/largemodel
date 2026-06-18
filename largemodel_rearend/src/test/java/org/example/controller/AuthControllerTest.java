/**
 * 模块：测试
 * 功能：AuthController 集成测试，覆盖注册/登录/获取当前用户/找回密码
 * 作者：yx
 * 创建时间：2026-06-18
 */
package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.JwtUtil;
import org.example.config.TestRedisConfig;
import org.example.dto.request.ForgetPasswordRequest;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestRedisConfig.class)
class AuthControllerTest {

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
    private static final String TEST_USERNAME = "testuser_auth";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_EMAIL = "test_auth@example.com";

    @BeforeEach
    void setUp() {
        // Clean up leftover from previous runs
        userRepository.findByUsernameAndDeletedFalse(TEST_USERNAME)
                .ifPresent(u -> userRepository.delete(u));

        // Create test user
        testUser = User.builder()
                .username(TEST_USERNAME)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .email(TEST_EMAIL)
                .nickname("TestAuth")
                .role(UserRole.USER)
                .status(UserStatus.ENABLED.getCode())
                .deleted(false)
                .build();
        testUser.setCreatedBy("test");
        testUser = userRepository.save(testUser);

        // Generate JWT token for test user
        testToken = jwtUtil.generateToken(testUser.getId(), testUser.getUsername(), testUser.getRole().name());
    }

    @AfterEach
    void tearDown() {
        // Clean up test user
        userRepository.findById(testUser.getId())
                .ifPresent(u -> userRepository.delete(u));
    }

    // ==================== Register Tests ====================

    @Test
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser_register");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");
        request.setNickname("NewUser");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.username").value("newuser_register"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.role").value("USER"));

        // Cleanup the newly registered user
        userRepository.findByUsernameAndDeletedFalse("newuser_register")
                .ifPresent(u -> userRepository.delete(u));
    }

    @Test
    void register_duplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(TEST_USERNAME);  // already exists
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(not(200)))
                .andExpect(jsonPath("$.message").value(containsString("已被注册")));
    }

    @Test
    void register_missingField() throws Exception {
        RegisterRequest request = new RegisterRequest();
        // Missing username (has @NotBlank)
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void register_shortUsername() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab");  // less than 3 chars
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("3-50")));
    }

    // ==================== Login Tests ====================

    @Test
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andExpect(jsonPath("$.data.user.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    void login_badCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(TEST_USERNAME);
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void login_nonexistentUser() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent_user_999");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    // ==================== Get Current User Tests ====================

    @Test
    void getCurrentUser_authenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void getCurrentUser_noToken() throws Exception {
        // No Authorization header → Spring Security blocks → AccessDeniedException → 403
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCurrentUser_invalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isForbidden());
    }

    // ==================== Forgot Password Tests ====================

    @Test
    void forgotPassword_success() throws Exception {
        ForgetPasswordRequest request = new ForgetPasswordRequest();
        request.setUsername(TEST_USERNAME);
        request.setEmail(TEST_EMAIL);
        request.setNewPassword("newpassword456");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("密码重置成功，请使用新密码登录"));

        // Verify old password no longer works
        LoginRequest loginWithOld = new LoginRequest();
        loginWithOld.setUsername(TEST_USERNAME);
        loginWithOld.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginWithOld)))
                .andExpect(status().isUnauthorized());

        // Verify new password works
        LoginRequest loginWithNew = new LoginRequest();
        loginWithNew.setUsername(TEST_USERNAME);
        loginWithNew.setPassword("newpassword456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginWithNew)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void forgotPassword_wrongEmail() throws Exception {
        ForgetPasswordRequest request = new ForgetPasswordRequest();
        request.setUsername(TEST_USERNAME);
        request.setEmail("wrong_email@example.com");
        request.setNewPassword("newpassword456");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("邮箱")));
    }

    @Test
    void forgotPassword_nonexistentUser() throws Exception {
        ForgetPasswordRequest request = new ForgetPasswordRequest();
        request.setUsername("nonexistent_user_888");
        request.setEmail("any@example.com");
        request.setNewPassword("newpassword456");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("用户名不存在")));
    }
}
