package com.simple.book.domain.user.repository;

import com.simple.book.domain.user.entity.Authentication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthenticationRepository extends JpaRepository<Authentication, Long> {
    Authentication findByUserId(String userId);
}
