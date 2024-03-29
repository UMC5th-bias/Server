package com.favoriteplace.app.domain.travel;

import com.favoriteplace.app.domain.Image;
import com.favoriteplace.app.domain.item.Item;
import com.favoriteplace.app.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
public class Rally extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rally_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Column(nullable = false)
    private String name;  //애니메이션 이름

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Long achieveNumber;  //달성한 사람 수

    @Column(nullable = false)
    private Long pilgrimageNumber; //해당 랠리의 성지 순례 갯수

    @OneToMany(mappedBy = "rally")
    private List<Pilgrimage> pilgrimages;

    public void addPilgrimage(){
        this.pilgrimageNumber += 1;
    }
    public void addAchieveNumber() { this.achieveNumber += 1; }
}
