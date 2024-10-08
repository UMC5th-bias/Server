package com.favoriteplace.app.domain;

import com.favoriteplace.app.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
public class Notification extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false)
    private String title;
    private String content;
    private Long postId;
    private Long guestBookId;
    private Long rallyId;
    private Boolean isRead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    private Notification(String type, String title, String content, Long postId, Long guestBookId, Long rallyId, Member member){
        this.type = type;
        this.title = title;
        this.content = content;
        this.postId = postId;
        this.guestBookId = guestBookId;
        this.rallyId = rallyId;
        this.isRead = false;
        this.member = member;
    }

    public void readNotification(){this.isRead = true;}
}
