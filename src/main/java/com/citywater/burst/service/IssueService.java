package com.citywater.burst.service;

import com.citywater.burst.config.CurrentUser;
import com.citywater.burst.dto.Requests.IssueCreateReq;
import com.citywater.burst.dto.Requests.IssueStatusReq;
import com.citywater.burst.model.*;
import com.citywater.burst.repo.PostRestoreIssueRepo;
import com.citywater.burst.repo.RepairOrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 复供后问题：黄水投诉、二次漏水、道路沉降、赔付申请，回到原抢修单闭环处理。
 */
@Service
@RequiredArgsConstructor
public class IssueService {

    private final PostRestoreIssueRepo issueRepo;
    private final RepairOrderRepo orderRepo;
    private final CurrentUser currentUser;

    public List<PostRestoreIssue> list(Long orderId) {
        return orderId == null
                ? issueRepo.findAllByOrderByCreatedAtDesc()
                : issueRepo.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Transactional
    public PostRestoreIssue create(IssueCreateReq req) {
        RepairOrder o = orderRepo.findById(req.orderId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "工单不存在: " + req.orderId()));
        EventStatus es = o.getEvent().getStatus();
        if (es != EventStatus.RESTORED && es != EventStatus.CLOSED) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "复供后问题须在事件复供后登记，当前事件状态: " + es.getLabel());
        }
        PostRestoreIssue i = new PostRestoreIssue();
        i.setOrder(o);
        i.setType(req.type());
        i.setDescription(req.description());
        i.setContactName(req.contactName());
        i.setContactPhone(req.contactPhone());
        i.setStatus(IssueStatus.OPEN);
        return issueRepo.save(i);
    }

    @Transactional
    public PostRestoreIssue updateStatus(long id, IssueStatusReq req) {
        PostRestoreIssue i = issueRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "问题不存在: " + id));
        i.setStatus(req.status());
        if (req.handleNote() != null) {
            i.setHandleNote(req.handleNote());
        }
        i.setHandler(currentUser.displayName());
        if (req.status() == IssueStatus.RESOLVED) {
            i.setResolvedAt(LocalDateTime.now());
        }
        return issueRepo.save(i);
    }
}
