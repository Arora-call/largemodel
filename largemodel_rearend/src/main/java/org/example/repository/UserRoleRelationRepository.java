package org.example.repository;

import org.example.entity.UserRoleRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRelationRepository extends JpaRepository<UserRoleRelation, Long> {

    List<UserRoleRelation> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
