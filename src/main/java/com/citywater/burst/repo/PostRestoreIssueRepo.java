package com.citywater.burst.repo;

import com.citywater.burst.model.IssueStatus;
import com.citywater.burst.model.PostRestoreIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRestoreIssueRepo extends JpaRepository<PostRestoreIssue, Long> {
    List<PostRestoreIssue> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<PostRestoreIssue> findAllByOrderByCreatedAtDesc();
    long countByStatusNot(IssueStatus status);
}
