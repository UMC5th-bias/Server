package com.favoriteplace.app.controller;

import com.favoriteplace.app.dto.community.TrendingPostResponseDto;
import com.favoriteplace.app.service.GuestBookService;
import com.favoriteplace.app.service.PostService;
import com.favoriteplace.app.service.TotalPostService;
import com.favoriteplace.global.util.DateTimeFormatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/posts/trending")
@RequiredArgsConstructor
public class TrendingPostController {
    private final PostService postService;
    private final GuestBookService guestBookService;
    private final TotalPostService totalPostService;

    @GetMapping("/today/free")
    public TrendingPostResponseDto.TrendingTodayPostResponseDto getTodayTrendingFreePost(){
        return TrendingPostResponseDto.TrendingTodayPostResponseDto.builder()
                .date(DateTimeFormatUtils.convertDateToString(LocalDateTime.now()))
                .rank(postService.getTodayTrendingPost())
                .build();
    }

    @GetMapping("/today/guestbooks")
    public TrendingPostResponseDto.TrendingTodayPostResponseDto getTodayTrendingGuestBook(){
        return TrendingPostResponseDto.TrendingTodayPostResponseDto.builder()
                .date(DateTimeFormatUtils.convertDateToString(LocalDateTime.now()))
                .rank(guestBookService.getTodayTrendingGuestBook())
                .build();
    }

    @GetMapping("/month")
    public List<TrendingPostResponseDto.TrendingMonthPostResponseDto> getMonthTrendingPost(){
        return totalPostService.getMonthTrendingPosts();
    }

}
