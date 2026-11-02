package com.homework.online.web;

import com.homework.online.common.CurrentUser;
import com.homework.online.dto.StatsDtos;
import com.homework.online.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public StatsDtos.OverviewResponse overview() {
        return statsService.overview(CurrentUser.get());
    }
}
