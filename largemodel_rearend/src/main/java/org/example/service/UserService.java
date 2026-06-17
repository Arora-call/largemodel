/**
 * 模块：用户体系
 * 功能：个人信息服务，处理用户信息更新、密码修改、账户注销等业务
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.UserPasswordRequest;
import org.example.dto.request.UserUpdateRequest;
import org.example.dto.response.UserInfoResponse;
import org.example.entity.User;
import org.example.enums.UserStatus;
import org.example.exception.BusinessException;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取用户信息
     */
    public UserInfoResponse getUserInfo(Long userId) {
        User user = findActiveUser(userId);
        return UserInfoResponse.from(user);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public UserInfoResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = findActiveUser(userId);

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        user.setUpdatedBy(user.getUsername());
        user = userRepository.save(user);
        return UserInfoResponse.from(user);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, UserPasswordRequest request) {
        User user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(user.getUsername());
        userRepository.save(user);
    }

    /**
     * 注销账户（逻辑删除）
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findActiveUser(userId);
        user.setDeleted(true);
        user.setStatus(UserStatus.DISABLED.getCode());
        user.setUpdatedBy(user.getUsername());
        userRepository.save(user);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return user;
    }
}
