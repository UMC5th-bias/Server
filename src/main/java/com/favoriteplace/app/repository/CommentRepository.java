package com.favoriteplace.app.repository;

import com.favoriteplace.app.domain.Member;
import com.favoriteplace.app.domain.community.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findAllByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);
    Long countByPostId(Long postId);

    //Page<Comment> findAllByMemberIdAndPostIsNotNullAndGuestBookIsNullOrderByCreatedAtDesc(Long id, Pageable pageable);

    Page<Comment> findAllByMemberIdAndPostIsNullAndGuestBookIsNotNullOrderByCreatedAtDesc(Long id, Pageable pageable);


    Page<Comment> findAllByGuestBookIdOrderByCreatedAtAsc(Long guestbookId, Pageable pageable);
    Long countByMember(Member member);
}
