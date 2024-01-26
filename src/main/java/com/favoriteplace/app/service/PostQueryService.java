package com.favoriteplace.app.service;

import com.favoriteplace.app.converter.PostConverter;
import com.favoriteplace.app.domain.Image;
import com.favoriteplace.app.domain.Member;
import com.favoriteplace.app.domain.community.Post;
import com.favoriteplace.app.dto.community.PostRequestDto;
import com.favoriteplace.app.dto.community.PostResponseDto;
import com.favoriteplace.app.dto.community.TrendingPostResponseDto;
import com.favoriteplace.app.repository.CommentRepository;
import com.favoriteplace.app.repository.ImageRepository;
import com.favoriteplace.app.repository.LikedPostRepository;
import com.favoriteplace.app.repository.PostRepository;
import com.favoriteplace.app.service.sortStrategy.SortStrategy;
import com.favoriteplace.global.exception.ErrorCode;
import com.favoriteplace.global.exception.RestApiException;
import com.favoriteplace.global.gcpImage.UploadImage;
import com.favoriteplace.global.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final LikedPostRepository likedPostRepository;
    private final CommentRepository commentRepository;
    private final SortStrategy<Post> sortPostByLatestStrategy;
    private final SortStrategy<Post> sortPostByLikedStrategy;
    private final UploadImage uploadImage;
    private final SecurityUtil securityUtil;

//    @Value("${spring.cloud.gcp.storage.bucket}")
//    private String bucketName;

    public List<TrendingPostResponseDto.TrendingTodayPostResponseDto.TrendingPostRank> getTodayTrendingPost() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        List<Post> posts = postRepository.findByCreatedAtBetweenOrderByLikeCountDesc(startOfDay, now);
        if(posts.isEmpty()){
            throw new RestApiException(ErrorCode.POST_NOT_FOUND);
        }
        posts.subList(0, Math.min(5, posts.size()));

        List<TrendingPostResponseDto.TrendingTodayPostResponseDto.TrendingPostRank> trendingPostRanks = new ArrayList<>();
        for(int i =0; i<posts.size() && i<5; i++){
            trendingPostRanks.add(TrendingPostResponseDto.TrendingTodayPostResponseDto.TrendingPostRank.of(posts.get(i)));
        }
        return trendingPostRanks;
    }

    public PostResponseDto.PostInfo getPostDetail(Long postId, HttpServletRequest request) {
        Optional<Post> post = postRepository.findById(postId);
        if(post.isEmpty()){
            throw new RestApiException(ErrorCode.POST_NOT_FOUND);
        }
        List<String> imageUrls = getImageUrlsByPostId(postId);
        if(!securityUtil.isTokenExists(request)){
            return PostResponseDto.PostInfo.of(post.get(), false, false, imageUrls);
        }
        Long memberId = securityUtil.getUserFromHeader(request).getId();
        return PostResponseDto.PostInfo.of(post.get(), isLiked(postId, memberId), isWriter(postId, memberId), imageUrls);
    }

    private Boolean isWriter(Long postId, Long memberId){
        Optional<Post> optionalPost = postRepository.findById(postId);
        if(optionalPost.isEmpty()){
            throw new RestApiException(ErrorCode.POST_NOT_FOUND);
        }
        return optionalPost.get().getMember().getId().equals(memberId);
    }

    private Boolean isLiked(Long postId, Long memberId){
        return likedPostRepository.existsByPostIdAndMemberId(postId, memberId);
    }

    private List<String> getImageUrlsByPostId(Long postId) {
        return Optional.ofNullable(imageRepository.findByPostId(postId))
                .map(images -> images.stream().map(Image::getUrl).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public List<PostResponseDto.MyPost> getMyPosts(int page, int size) {
        Member member = securityUtil.getUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId(), pageable);
        if(postPage.isEmpty()){
            return Collections.emptyList();
        }
        List<PostResponseDto.MyPost> myPosts = new ArrayList<>();
        for(Post post : postPage.getContent()){
            Long comments = commentRepository.countByPostId(post.getId()) != null ? commentRepository.countByPostId(post.getId()) : 0L;
            myPosts.add(PostConverter.toMyPost(post, member, comments));
        }
        return myPosts;
    }

    public List<PostResponseDto.MyPost> getTotalPostBySort(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size);
        SortStrategy<Post> sortStrategy;
        if("latest".equals(sort)){
            sortStrategy = sortPostByLatestStrategy;
        }else if("liked".equals(sort)) {
            sortStrategy = sortPostByLikedStrategy;
        }else{
            throw new RestApiException(ErrorCode.SORT_KEYWORD_NOT_ALLOWED);
        }
        Page<Post> sortedPosts = sortStrategy.sort(pageable);
        if(sortedPosts.isEmpty()){
            return Collections.emptyList();
        }
        List<PostResponseDto.MyPost> totalPosts = new ArrayList<>();
        for(Post post : sortedPosts.getContent()){
            Member member = post.getMember();
            Long comments = commentRepository.countByPostId(post.getId()) != null ? commentRepository.countByPostId(post.getId()) : 0L;
            totalPosts.add(PostConverter.toMyPost(post, member, comments));
        }
        return totalPosts;
    }

    /**
     * 게시글의 조회수를 증가하는 함수
     * @param postId
     */
    public void increasePostView(Long postId) {
        Optional<Post> postOptional = postRepository.findById(postId);
        if(postOptional.isEmpty()){throw new RestApiException(ErrorCode.POST_NOT_FOUND);}
        Post post = postOptional.get();
        post.increaseView();
        postRepository.save(post);
    }
}