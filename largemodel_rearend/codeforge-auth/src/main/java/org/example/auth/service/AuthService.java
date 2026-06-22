/**
 * 模块：用户体系
 * 功能：认证服务，处理用户注册（用户名查重/BCrypt加密）、登录（密码校验/JWT生成）、获取当前用户等核心业务
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.config.JwtUtil;
import org.example.dto.request.ForgetPasswordRequest;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.LoginResponse;
import org.example.dto.response.UserInfoResponse;
import org.example.entity.User;
import org.example.enums.UserRole;
import org.example.enums.UserStatus;
import org.example.exception.BusinessException;
import org.example.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @Transactional
    public UserInfoResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new BusinessException("用户名已被注册");
        }

        // 创建新用户
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(UserRole.USER)
                .status(UserStatus.ENABLED.getCode())
                .deleted(false)
                .build();

        user.setCreatedBy(request.getUsername());

        user = userRepository.save(user);
        return UserInfoResponse.from(user);
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));

        if (!user.isEnabled()) {
            throw new BusinessException("账户已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        UserInfoResponse userInfo = UserInfoResponse.from(user);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration())
                .user(userInfo)
                .build();
    }

    /**
     * 获取当前登录用户信息
     */
    public UserInfoResponse getCurrentUser(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return UserInfoResponse.from(user);
    }

    /**
     * 找回密码：通过用户名+邮箱验证身份，验证通过后重置密码
     */
    @Transactional
    public void forgetPassword(ForgetPasswordRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名不存在"));

        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(request.getEmail().trim())) {
            throw new BusinessException("邮箱与注册时填写的邮箱不一致");
        }

        if (!user.isEnabled()) {
            throw new BusinessException("账户已被禁用，无法重置密码");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(user.getUsername());
        userRepository.save(user);
    }
}
