package com.favoriteplace.app.converter;

import com.favoriteplace.app.domain.community.Comment;
import com.favoriteplace.app.domain.community.GuestBook;
import com.favoriteplace.app.dto.UserInfoResponseDto;
import com.favoriteplace.app.dto.community.CommentResponseDto;
import com.favoriteplace.app.dto.community.GuestBookResponseDto;
import com.favoriteplace.app.dto.community.PostResponseDto;
import com.favoriteplace.global.util.DateTimeFormatUtils;

public class CommentConverter {

    public static GuestBookResponseDto.MyGuestBookComment toMyGuestBookComment(Comment comment) {
        GuestBook guestBook = comment.getGuestBook();
        return GuestBookResponseDto.MyGuestBookComment.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .passedTime(DateTimeFormatUtils.getPassDateTime(comment.getCreatedAt()))
                .myGuestBookInfo(GuestBookResponseDto.MyGuestBookInfo.builder()
                        .id(guestBook.getId())
                        .title(guestBook.getTitle())
                        .nickname(guestBook.getMember().getNickname())
                        .views(guestBook.getView())
                        .likes(guestBook.getLikeCount())
                        .comments((long) guestBook.getComments().size())
                        .passedTime(DateTimeFormatUtils.getPassDateTime(guestBook.getCreatedAt()))
                        .build())
                .build();
    }

    public static CommentResponseDto.PostComment toPostComment(Comment comment, Boolean isWrite){
        return CommentResponseDto.PostComment.builder()
                .userInfo(UserInfoResponseDto.of(comment.getMember()))
                .id(comment.getId())
                .content(comment.getContent())
                .passedTime(DateTimeFormatUtils.getPassDateTime(comment.getCreatedAt()))
                .isWrite(isWrite)
                .build();
    }

    public static PostResponseDto.MyComment toMyPostComment(Comment comment){
        return PostResponseDto.MyComment.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .passedTime(DateTimeFormatUtils.getPassDateTime(comment.getCreatedAt()))
                .post(PostConverter.toMyPost(comment.getPost()))
                .build();
    }
}
