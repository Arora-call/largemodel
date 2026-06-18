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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

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

    /**
     * 上传用户头像
     */
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = findActiveUser(userId);

        // 校验文件非空
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只允许上传图片文件");
        }

        // 校验文件大小（2MB）
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过2MB");
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        // 确保目录存在
        Path uploadPath = Path.of(avatarDir);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new BusinessException("创建上传目录失败");
        }

        // 保存文件
        Path filePath = uploadPath.resolve(filename);
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }

        // 构建可访问的 URL
        String avatarUrl = "/uploads/avatars/" + filename;

        // 更新用户头像字段
        user.setAvatar(avatarUrl);
        user.setUpdatedBy(user.getUsername());
        userRepository.save(user);

        return avatarUrl;
    }
}
