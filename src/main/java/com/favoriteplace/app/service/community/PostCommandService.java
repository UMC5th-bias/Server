package com.favoriteplace.app.service.community;

import com.favoriteplace.app.domain.Image;
import com.favoriteplace.app.domain.Member;
import com.favoriteplace.app.domain.community.Post;
import com.favoriteplace.app.dto.community.PostRequestDto;
import com.favoriteplace.app.repository.ImageRepository;
import com.favoriteplace.app.repository.LikedPostRepository;
import com.favoriteplace.app.repository.PostRepository;
import com.favoriteplace.global.exception.ErrorCode;
import com.favoriteplace.global.exception.RestApiException;
import com.favoriteplace.global.gcpImage.ConvertUuidToUrl;
import com.favoriteplace.global.gcpImage.UploadImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostCommandService {
    private final UploadImage uploadImage;
    private final PostRepository postRepository;
    private final LikedPostRepository likedPostRepository;
    private final ImageRepository imageRepository;

    /**
     * 자유게시글 작성
     * @param data
     * @param images
     * @throws IOException
     */
    @Transactional
    public Long createPost(PostRequestDto data, List<MultipartFile> images, Member member) throws IOException {
        Post newPost = Post.builder()
                .member(member).title(data.getTitle())
                .images(new ArrayList<>())
                .content(data.getContent()).likeCount(0L).view(0L)
                .build();
        if(images != null && !images.isEmpty()){
            newPost.getImages().addAll(setImageList(newPost, images));
        }
        Post post = postRepository.save(newPost);
        return post.getId();
    }

    /**
     * 이미지가 여러개일 때, 이미지 처리하는 로직
     * @param images
     * @throws IOException
     */
    @Transactional
    public List<Image> setImageList(Post post, List<MultipartFile> images) throws IOException {
        //이미지 업로드 관련
        List<CompletableFuture<Image>> futures = new ArrayList<>();

        for(MultipartFile image: images){
            if(image != null && !image.isEmpty()){
                /*
                String uuid = uploadImage.uploadImageToCloud(image);
                Image newImage = Image.builder().url(ConvertUuidToUrl.convertUuidToUrl(uuid)).post(post).build();
                imagesForPost.add(newImage);*/
                CompletableFuture<Image> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        String uuid = uploadImage.uploadImageToCloud(image);
                        return Image.builder().url(ConvertUuidToUrl.convertUuidToUrl(uuid)).post(post).build();
                    } catch (IOException e) {
                        throw new RestApiException(ErrorCode.IMAGE_CANNOT_UPLOAD);
                    }
                });
                futures.add(future);
            }
        }

        // 모든 작업이 완료될 때까지 기다림
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * 자유게사글 삭제
     * @param postId
     */
    @Transactional
    public void deletePost(long postId, Member member) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RestApiException(ErrorCode.POST_NOT_FOUND));
        checkAuthOfGuestBook(member, post);
        likedPostRepository.deleteByPostIdAndMemberId(postId, member.getId());
        postRepository.delete(post);
    }

    /**
     * 자유게시글 수정
     * @param postId
     * @param data
     * @param images
     */
    @Transactional
    public void modifyPost(long postId, PostRequestDto data, List<MultipartFile> images, Member member) throws IOException {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RestApiException(ErrorCode.POST_NOT_FOUND));
        checkAuthOfGuestBook(member, post);
        Optional.ofNullable(data.getTitle()).ifPresent(post::setTitle);
        Optional.ofNullable(data.getContent()).ifPresent(post::setContent);
        //기존의 이미지 삭제 필요
        post.getImages().clear();
        imageRepository.deleteByPostId(post.getId());
        if(images != null && !images.isEmpty()){
            post.getImages().addAll(setImageList(post, images));
        }
        postRepository.save(post);
    }

    /**
     * post의 작성자가 맞는지 확인하는 로직
     * @param member
     * @param post
     */
    private void checkAuthOfGuestBook(Member member, Post post){
        if(!member.getId().equals(post.getMember().getId())){
            throw new RestApiException(ErrorCode.USER_NOT_AUTHOR);
        }
    }

    /**
     * 게시글의 조회수를 증가하는 함수
     * @param postId
     */
    @Transactional
    public void increasePostView(Long postId) {
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isEmpty()) {
            throw new RestApiException(ErrorCode.POST_NOT_FOUND);
        }
        Post post = postOptional.get();
        post.increaseView();
        postRepository.save(post);
    }
}
