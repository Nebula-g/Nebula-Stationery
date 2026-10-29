package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.service.GroupBuyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-buy")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyService groupBuyService;

    @GetMapping
    public Result<List<Map<String, Object>>> listGroupBuys() {
        return Result.success(groupBuyService.listGroupBuys());
    }

    @GetMapping("/sessions/{groupBuyId}")
    public Result<List<Map<String, Object>>> listSessions(@PathVariable Long groupBuyId) {
        return Result.success(groupBuyService.listSessions(groupBuyId));
    }

    @PostMapping("/sessions")
    public Result<Map<String, Object>> createSession(Authentication authentication,
                                                      @RequestParam Long groupBuyId) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(groupBuyService.createSession(userId, groupBuyId));
    }

    @PostMapping("/sessions/{sessionId}/join")
    public Result<Map<String, Object>> joinSession(Authentication authentication,
                                                    @PathVariable Long sessionId) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(groupBuyService.joinSession(userId, sessionId));
    }

    @GetMapping("/session/{sessionId}")
    public Result<Map<String, Object>> sessionDetail(@PathVariable Long sessionId) {
        return Result.success(groupBuyService.getSessionDetail(sessionId));
    }
}
