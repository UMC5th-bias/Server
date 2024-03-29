package com.favoriteplace.app.service.community;

import com.favoriteplace.app.domain.Member;
import com.favoriteplace.app.domain.community.Comment;
import com.favoriteplace.app.domain.community.GuestBook;
import com.favoriteplace.app.domain.community.Post;
import com.favoriteplace.app.dto.community.GuestBookRequestDto;
import com.favoriteplace.app.repository.CommentRepository;
import com.favoriteplace.app.repository.GuestBookRepository;
import com.favoriteplace.app.repository.PostRepository;
import com.favoriteplace.global.exception.ErrorCode;
import com.favoriteplace.global.exception.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentCommandService {
    private final PostRepository postRepository;
    private final GuestBookRepository guestBookRepository;
    private final CommentRepository commentRepository;

    /**
     * 자유게시글 새로운 댓글 작성
     * @param member
     * @param postId
     * @param content
     */
    @Transactional
    public void createPostComment(Member member, long postId, String content) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RestApiException(ErrorCode.POST_NOT_FOUND));
        Comment comment = Comment.builder()
                .member(member)
                .post(post)
                .content(content)
                .build();
        post.getComments().add(comment);
        postRepository.save(post);
    }

    /**
     * 자유게시글 댓글 수정
     * @param member
     * @param commentId
     * @param content
     */
    @Transactional
    public void modifyPostComment(Member member, long commentId, String content) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RestApiException(ErrorCode.COMMENT_NOT_FOUND));
        checkAuthOfComment(member, comment);
        Optional.ofNullable(content).ifPresent(comment::setContent);
        commentRepository.save(comment);
    }

    /**
     * 자유게시글 댓글 삭제
     * @param member
     * @param commendId
     */
    @Transactional
    public void deletePostComment(Member member, long commendId){
        Comment comment = commentRepository.findById(commendId).orElseThrow(() -> new RestApiException(ErrorCode.COMMENT_NOT_FOUND));
        checkAuthOfComment(member, comment);
        commentRepository.delete(comment);
    }

    /**
     * 성지순례 인증글에 댓글 추가
     * @param member
     * @param guestbookId
     */
    @Transactional
    public void createGuestBookComment(Member member, Long guestbookId, GuestBookRequestDto.GuestBookCommentDto comment) {
        GuestBook guestBook = guestBookRepository.findById(guestbookId).orElseThrow(() -> new RestApiException(ErrorCode.GUESTBOOK_NOT_FOUND));
        Comment newComment = Comment.builder().member(member).guestBook(guestBook).content(comment.getContent()).build();
        guestBook.addComment(newComment);
        guestBookRepository.save(guestBook);
    }

    /**
     * 성지순례 인증글에 댓글 수정
     * @param member
     * @param commentId
     * @param content
     */
    @Transactional
    public void modifyGuestBookComment(Member member, Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RestApiException(ErrorCode.COMMENT_NOT_FOUND));
        checkAuthOfComment(member, comment);
        Optional.ofNullable(content).ifPresent(comment::setContent);
        commentRepository.save(comment);
    }

    /**
     * 성지순례 인증글에 댓글 삭제
     * @param member
     * @param commentId
     */
    @Transactional
    public void deleteGuestBookComment(Member member, Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RestApiException(ErrorCode.COMMENT_NOT_FOUND));
        checkAuthOfComment(member, comment);
        commentRepository.delete(comment);
    }

    /**
     * 댓글의 작성자가 맞는지 확인하는 함수 (만약 작성자가 아니라면 에러 출력)
     * @param member
     * @param comment
     */
    private void checkAuthOfComment(Member member, Comment comment){
        if(!member.getId().equals(comment.getMember().getId())){
            throw new RestApiException(ErrorCode.USER_NOT_AUTHOR);
        }
    }


}
