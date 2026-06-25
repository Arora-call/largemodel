/**
 * 模块：系统管理 — 应用管理
 * 功能：管理员查看/修改/删除所有用户的应用
 * 作者：yx
 * 创建时间：2026-06-25
 */
package org.example.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Application;
import org.example.entity.User;
import org.example.exception.BusinessException;
import org.example.repository.ApplicationRepository;
import org.example.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminApplicationService {

    private final ApplicationRepository appRepo;
    private final UserRepository userRepo;

    /** 分页查询所有用户的应用 */
    public Page<Application> listAll(int page, int size, String keyword, String type) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasType = StringUtils.hasText(type);

        if (hasKeyword && hasType) {
            return appRepo.findByStatusNotAndNameContainingAndType(0, keyword, type, pageable);
        } else if (hasKeyword) {
            return appRepo.findByStatusNotAndNameContaining(0, keyword, pageable);
        } else if (hasType) {
            return appRepo.findByStatusNotAndType(0, type, pageable);
        }
        return appRepo.findByStatusNot(0, pageable);
    }

    /** 批量查询用户名，返回 userId → userName 映射 */
    public Map<Long, String> getUserNameMap(Set<Long> userIds) {
        return userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getNickname() != null ? u.getNickname() : u.getUsername()));
    }

    /** 管理员更新应用 */
    @Transactional
    public Application update(Long id, String name, String description, String type, String language) {
        Application app = appRepo.findById(id)
                .orElseThrow(() -> new BusinessException("应用不存在"));
        if (StringUtils.hasText(name)) app.setName(name);
        if (description != null) app.setDescription(description);
        if (StringUtils.hasText(type)) app.setType(type);
        if (StringUtils.hasText(language)) app.setLanguage(language);
        app.setUpdatedAt(LocalDateTime.now());
        return appRepo.save(app);
    }

    /** 管理员删除应用（逻辑删除） */
    @Transactional
    public void delete(Long id) {
        Application app = appRepo.findById(id)
                .orElseThrow(() -> new BusinessException("应用不存在"));
        app.setStatus(0);
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.save(app);
    }
}
