package com.favoriteplace.app.service;

import com.favoriteplace.app.converter.CommonConverter;
import com.favoriteplace.app.converter.PointHistoryConverter;
import com.favoriteplace.app.domain.Member;
import com.favoriteplace.app.domain.enums.PointType;
import com.favoriteplace.app.domain.enums.RallyVersion;
import com.favoriteplace.app.domain.item.AcquiredItem;
import com.favoriteplace.app.domain.item.PointHistory;
import com.favoriteplace.app.domain.travel.*;
import com.favoriteplace.app.dto.CommonResponseDto;
import com.favoriteplace.app.dto.travel.PilgrimageDto;
import com.favoriteplace.app.repository.*;
import com.favoriteplace.global.exception.ErrorCode;
import com.favoriteplace.global.exception.RestApiException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PilgrimageCommandService {
    private final RallyRepository rallyRepository;
    private final PilgrimageRepository pilgrimageRepository;
    private final LikedRallyRepository likedRallyRepository;
    private final VisitedPilgrimageRepository visitedPilgrimageRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final CompleteRallyRepository completeRallyRepository;
    private final AcquiredItemRepository acquiredItemRepository;
    private final EntityManager em;

    /***
     * 랠리 찜하기
     * @param rallyId 랠리 아이디
     * @param member 찜한 사용자
     * @return
     */
    public CommonResponseDto.PostResponseDto likeToRally(Long rallyId, Member member) {
        Rally rally = rallyRepository.findById(rallyId).orElseThrow(
                ()->new RestApiException(ErrorCode.RALLY_NOT_FOUND));
        LikedRally likedRally = likedRallyRepository.findByRallyAndMember(rally, member);

        if (likedRally == null) {
            LikedRally newLikedRally = LikedRally.builder().rally(rally).member(member).build();
            likedRallyRepository.save(newLikedRally);
            return CommonConverter.toPostResponseDto(true, "찜 목록에 추가됐습니다.");
        } else {
            likedRallyRepository.delete(likedRally);
            return CommonConverter.toPostResponseDto(true, "찜을 취소했습니다.");
        }
    }

    /***
     * 성지순례 방문 인증하기
     * @param pilgrimageId 성지순례 아이디
     * @param member 인증한 사용자
     * @return
     */
    public CommonResponseDto.RallyResponseDto certifyToPilgrimage(Long pilgrimageId,
                                                                 Member member,
                                                                 PilgrimageDto.PilgrimageCertifyRequestDto form) {
        Pilgrimage pilgrimage = pilgrimageRepository.findById(pilgrimageId).orElseThrow(
                () -> new RestApiException(ErrorCode.PILGRIMAGE_NOT_FOUND));

        List<VisitedPilgrimage> visitedPilgrimages = visitedPilgrimageRepository
                .findByPilgrimageAndMemberOrderByCreatedAtDesc(pilgrimage, member);

        ZoneId serverZoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime nowInServerTimeZone = ZonedDateTime.now(serverZoneId);

        log.info("now="+LocalDateTime.now());
        if (!visitedPilgrimages.isEmpty()) {
            log.info("pilgrimage="+visitedPilgrimages.get(0).getPilgrimage().getCreatedAt().atZone(serverZoneId));
            log.info("application pilgrimage="+visitedPilgrimages.get(0).getPilgrimage().getCreatedAt());
        }

        if (visitedPilgrimages.isEmpty()
                || (!visitedPilgrimages.isEmpty()
                && visitedPilgrimages.get(0).getPilgrimage().getCreatedAt().plusHours(24L).isBefore(LocalDateTime.now()))) {
            // 현재 좌표가 성지순례 장소 좌표 기준 +-0.00135 이내인지 확인
            if (checkCoordinate(form, pilgrimage)){
                throw new RestApiException(ErrorCode.PILGRIMAGE_CAN_NOT_CERTIFIED);
            }
            // 성공 시 포인트 지급 -> 15p & visitedPilgrimage 추가
            successVisitedAndPointProcess(member, pilgrimage);

            Long completeCount = visitedPilgrimageRepository.findByDistinctCount(member.getId(), pilgrimage.getRally().getId());
            log.info("completeCount="+completeCount);
//            // 랠리를 완료했는지 확인
            if (checkCompleteRally(member, pilgrimage, completeCount))
                return CommonConverter.toRallyResponseDto(true, true,"<"+pilgrimage.getRally().getItem().getName()+"> 칭호를 얻었습니다!");
        } else
            throw new RestApiException(ErrorCode.PILGRIMAGE_ALREADY_CERTIFIED);
        return CommonConverter.toRallyResponseDto(true, false,"성지순례 인증하기 15P를 얻으셨습니다!");
    }

    private boolean checkCompleteRally(Member member, Pilgrimage pilgrimage, Long completeCount) {
        if (completeCount == pilgrimage.getRally().getPilgrimageNumber()) {
            List<CompleteRally> completeRally = completeRallyRepository.findByMemberAndRally(member, pilgrimage.getRally());
            log.info("size="+completeRally.size());
            // 이전에 이미 랠리를 완료한 상태인지 확인
            if (completeRally.isEmpty()) {
                // 최초 완료에 한해 칭호 획득
                if (completeRally == null)
                    acquiredItemRepository.save(AcquiredItem.builder().item(pilgrimage.getRally().getItem()).member(member).build());
                // 포인트 획득
                pointHistoryRepository.save(PointHistoryConverter.toPointHistory(member, 100L, PointType.ACQUIRE));
                // 완료 랠리 추가
                completeRallyRepository.save(CompleteRally.builder().rally(pilgrimage.getRally()).member(member).version(RallyVersion.v1).build());
                // 랠리 완료자 목록에 추가
                pilgrimage.getRally().addAchieveNumber();
                rallyRepository.save(pilgrimage.getRally());
                return true;
            }
        }
        return false;
    }

    private boolean checkCoordinate(PilgrimageDto.PilgrimageCertifyRequestDto form, Pilgrimage pilgrimage) {
        return pilgrimage.getLatitude() + 0.00135 < form.getLatitude() || pilgrimage.getLatitude() - 0.00135 > form.getLatitude()
                || pilgrimage.getLongitude() + 0.00135 < form.getLongitude() || pilgrimage.getLongitude() - 0.00135 > form.getLongitude();
    }

    private void successVisitedAndPointProcess(Member member, Pilgrimage pilgrimage) {
        VisitedPilgrimage newVisited = VisitedPilgrimage.builder().pilgrimage(pilgrimage).member(member).build();
        log.info("visited="+newVisited.getId());
        visitedPilgrimageRepository.save(newVisited);
        pointHistoryRepository.save(PointHistoryConverter.toPointHistory(member, 15L, PointType.ACQUIRE));
        member.updatePoint(15L);
        log.info("clear");
    }
}
