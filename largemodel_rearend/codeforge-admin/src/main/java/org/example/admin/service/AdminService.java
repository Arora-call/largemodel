/**
 * 模块：系统管理
 * 功能：管理员服务，处理用户列表分页查询（动态筛选）、角色/状态管理、逻辑删除等后台业务
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.dto.response.PageResponse;
import org.example.dto.response.UserInfoResponse;
import org.example.entity.User;
import org.example.enums.UserRole;
import org.example.enums.UserStatus;
import org.example.exception.BusinessException;
import org.example.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    public PageResponse<UserInfoResponse> listUsers(int page, int size, String keyword, String role, Integer status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 逻辑删除过滤
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));

            // 关键字搜索（用户名、昵称、邮箱）
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                Predicate usernameLike = criteriaBuilder.like(root.get("username"), pattern);
                Predicate nicknameLike = criteriaBuilder.like(root.get("nickname"), pattern);
                Predicate emailLike = criteriaBuilder.like(root.get("email"), pattern);
                predicates.add(criteriaBuilder.or(usernameLike, nicknameLike, emailLike));
            }

            // 角色过滤
            if (StringUtils.hasText(role)) {
                predicates.add(criteriaBuilder.equal(root.get("role"), UserRole.valueOf(role)));
            }

            // 状态过滤
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserInfoResponse> content = userPage.getContent().stream()
                .map(UserInfoResponse::from)
                .toList();

        return PageResponse.<UserInfoResponse>builder()
                .content(content)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .currentPage(userPage.getNumber())
                .pageSize(userPage.getSize())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    /**
     * 获取用户详情
     */
    public UserInfoResponse getUserDetail(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return UserInfoResponse.from(user);
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (UserRole.ADMIN.equals(user.getRole())) {
            throw new BusinessException("不能修改管理员的账户状态");
        }

        user.setStatus(status);
        userRepository.save(user);
    }

    /**
     * 更新用户角色
     */
    @Transactional
    public void updateUserRole(Long userId, String role) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        try {
            user.setRole(UserRole.valueOf(role));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的角色: " + role);
        }

        userRepository.save(user);
    }

    /**
     * 管理员删除用户（逻辑删除）
     */
    @Transactional
    public void deleteUser(Long userId, Long adminId) {
        if (userId.equals(adminId)) {
            throw new BusinessException("不能删除自己的账户");
        }

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (UserRole.ADMIN.equals(user.getRole())) {
            throw new BusinessException("不能删除管理员账户");
        }

        user.setDeleted(true);
        user.setStatus(UserStatus.DISABLED.getCode());
        userRepository.save(user);
    }

    /**
     * 管理员重置用户密码
     */
    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 获取用户总数
     */
    public long getUserCount() {
        return userRepository.countByDeletedFalse();
    }
}
