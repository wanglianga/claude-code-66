package com.citywater.burst.web;

import com.citywater.burst.dto.Requests.IssueCreateReq;
import com.citywater.burst.dto.Requests.IssueStatusReq;
import com.citywater.burst.model.PostRestoreIssue;
import com.citywater.burst.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @GetMapping
    public List<PostRestoreIssue> list(@RequestParam(required = false) Long orderId) {
        return issueService.list(orderId);
    }

    /** 复供后问题登记（回到原抢修单） */
    @PostMapping
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public PostRestoreIssue create(@Valid @RequestBody IssueCreateReq req) {
        return issueService.create(req);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CS','DISPATCHER','ADMIN')")
    public PostRestoreIssue updateStatus(@PathVariable long id, @Valid @RequestBody IssueStatusReq req) {
        return issueService.updateStatus(id, req);
    }
}
