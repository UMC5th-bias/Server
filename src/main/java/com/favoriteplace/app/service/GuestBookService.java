package com.favoriteplace.app.service;

import com.favoriteplace.app.converter.GuestBookConverter;
import com.favoriteplace.app.domain.Image;
import com.favoriteplace.app.domain.Member;
import com.favoriteplace.app.domain.community.GuestBook;
import com.favoriteplace.app.domain.community.HashTag;
import com.favoriteplace.app.domain.community.Post;
import com.favoriteplace.app.dto.community.GuestBookResponseDto;
import com.favoriteplace.app.dto.community.TrendingPostResponseDto;
import com.favoriteplace.app.repository.GuestBookRepository;
import com.favoriteplace.app.repository.HashtagRepository;
import com.favoriteplace.app.repository.ImageRepository;
import com.favoriteplace.app.repository.LikedPostRepository;
import com.favoriteplace.global.exception.ErrorCode;
import com.favoriteplace.global.exception.RestApiException;
import com.favoriteplace.global.gcpImage.ConvertUuidToUrl;
import com.favoriteplace.global.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestBookService {
    private final GuestBookRepository guestBookRepository;
    private final LikedPostRepository likedPostRepository;
    private final ImageRepository imageRepository;
    private final HashtagRepository hashtagRepository;
    private final CountComments countComments;
    private final SecurityUtil securityUtil;

    public List<TrendingPostResponseDto.TrendingTodayPostResponseDto.TrendingPostRank> getTodayTrendingGuestBook() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        List<GuestBook> guestBooks = guestBookRepository.findByCreatedAtBetweenOrderByLikeCountDesc(startOfDay, now);
        if(guestBooks.isEmpty()){
            throw new RestApiException(ErrorCode.GUESTBOOK_NOT_FOUND);
        }
        guestBooks.subList(0, Math.min(5, guestBooks.size()));

        List<TrendingPostResponseDto.TrendingTodayPostResponseDto.TrendingPostRank> trendingPostsRank = new ArrayList<>();
        for(int i = 0; (i < guestBooks.size()) && (i < 5); i++){
            trendingPostsRank.add(TrendingPostResponseDto.TrendingTodayPostResponseDto.TrendingPostRank.of(guestBooks.get(i)));
        }
        return trendingPostsRank;
    }

    public Page<GuestBookResponseDto.GuestBook> getMyGuestBooks(int page, int size) {
        Member member = securityUtil.getUser();
        Pageable pageable = PageRequest.of(page-1, size);
        Page<GuestBook> myGuestBooks = guestBookRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId(), pageable);
        if(myGuestBooks.isEmpty()){return Page.empty();}
        return myGuestBooks.map(guestBook -> GuestBookConverter.toGuestBook(guestBook, member.getNickname(), countComments.countGuestBookComments(guestBook.getId())));
    }

    public GuestBookResponseDto.GuestBookInfo getDetailGuestBookInfo(Long guestBookId, HttpServletRequest request) {
        Member member = securityUtil.getUserFromHeader(request);
        Optional<GuestBook> optionalGuestBook = guestBookRepository.findById(guestBookId);
        if(optionalGuestBook.isEmpty()){throw new RestApiException(ErrorCode.GUESTBOOK_NOT_FOUND);}
        //GuestBook
        GuestBook guestBook = optionalGuestBook.get();
        //Image
        List<Image> images = imageRepository.findAllByGuestBookId(guestBook.getId());
        List<String> imagesUrl = images.stream().map(Image::getUrl).map(ConvertUuidToUrl::convertUuidToUrl).toList();
        //HashTag
        List<HashTag> hashTags = hashtagRepository.findAllByGuestBookId(guestBook.getId());
        List<String> hashTagsString = hashTags.stream().map(HashTag::getTagName).toList();
        if(!securityUtil.isTokenExists(request)){
            return GuestBookConverter.toGuestBookInfo(guestBook, false, false, imagesUrl, hashTagsString);
        }
        return GuestBookConverter.toGuestBookInfo(guestBook, isLiked(guestBook.getId(), member.getId()), isWriter(guestBook.getId(), member.getId()), imagesUrl, hashTagsString);
    }

    private Boolean isLiked(Long guestBookId, Long memberId){
        return likedPostRepository.existsByGuestBookIdAndMemberId(guestBookId, memberId);
    }

    private Boolean isWriter(Long guestBookId, Long memberId){
        Optional<GuestBook> optionalGuestBook = guestBookRepository.findById(guestBookId);
        if(optionalGuestBook.isEmpty()){
            throw new RestApiException(ErrorCode.GUESTBOOK_NOT_FOUND);
        }
        return optionalGuestBook.get().getMember().getId().equals(memberId);
    }

    public void increaseGuestBookView(Long guestBookId) {
        Optional<GuestBook> optionalGuestBook = guestBookRepository.findById(guestBookId);
        if(optionalGuestBook.isEmpty()){throw new RestApiException(ErrorCode.GUESTBOOK_NOT_FOUND);}
        GuestBook guestBook = optionalGuestBook.get();
        guestBook.increaseView();
        guestBookRepository.save(guestBook);
    }
}
