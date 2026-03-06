package com.ada.proj.service;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.entity.Post;
import com.ada.proj.entity.PostLike;
import com.ada.proj.entity.User;
import com.ada.proj.repository.PostLikeRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserBanService userBanService;

    private @NonNull
    String requirePostUuid(Post post) {
        return Objects.requireNonNull(post.getPostUuid(), "postUuid is required");
    }

    private Post getPostByUuidOrThrow(@NonNull String uuid) {
        return postRepository.findById(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + uuid));
    }

    private boolean hasAdminRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga != null && "ROLE_ADMIN".equalsIgnoreCase(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private void ensureWriterOrAdmin(Post post, Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        String requesterUuid = auth.getName();
        boolean isAdmin = hasAdminRole(auth);
        String writerUuid = post.getWriterUuid();

        if (isAdmin) {
            return;
        }
        if (writerUuid == null || writerUuid.isBlank() || !writerUuid.equals(requesterUuid)) {
            throw new AccessDeniedException("작성자 또는 관리자만 수정/삭제할 수 있습니다.");
        }
    }

    // 생성
    @Transactional
    public String create(@NonNull PostCreateRequest req) {

        String writerUuid = req.getWriterUuid();
        if (writerUuid == null || writerUuid.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                writerUuid = auth.getName();
            }
        }

        String writerName = null;
        if (writerUuid != null && !writerUuid.isBlank()) {
            User writerUser = userRepository.findByUuid(writerUuid).orElse(null);
            if (writerUser != null) {
                userBanService.checkUserBanned(writerUser);
                writerName = writerUser.isUseNickname()
                        ? writerUser.getUserNickname()
                        : writerUser.getUserRealname();
            }
        }

        Post p = Post.builder()
                .writerUuid(writerUuid)
                .title(req.getTitle())
                .images(req.getImages())
                .videos(req.getVideos())
                .writer(writerName)
                .isDev(Boolean.TRUE.equals(req.getIsDev()))
                .devTags(req.getDevTags())
                .build();

        if (req.getContent() != null) {
            p.setContent(req.getContent());
        }

        return postRepository.save(Objects.requireNonNull(p)).getPostUuid();
    }

    // 목록
    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> list(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "writedAt"));

        Page<PostSummaryResponse> result = postRepository.findSummaryPage(pageable)
                .map(dto -> {
                    // tag 계산
                    String tag = (Boolean.TRUE.equals(dto.getIsDev()))
                            ? (dto.getDevTags() != null && !dto.getDevTags().isBlank() ? "개발(" + dto.getDevTags() + ")" : "개발")
                            : "일반";
                    dto.setTag(tag);
                    return dto;
                });

        return new PageResponse<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent()
        );
    }

    // 상세(+조회수)
    @Transactional
    public PostDetailResponse detail(@NonNull String uuid) {

        postRepository.increaseViews(uuid);

        Post p = getPostByUuidOrThrow(uuid);

        User u = userRepository.findByUuid(p.getWriterUuid()).orElse(null);

        return PostDetailResponse.builder()
                .postUuid(p.getPostUuid())
                .seq(p.getSeq())
                .writerUuid(p.getWriterUuid())
                .writer(p.getWriter())
                .writerProfileImage(u != null ? u.getProfileImage() : null)
                .title(p.getTitle())
                .content(p.getContent())
                .images(p.getImages())
                .videos(p.getVideos())
                .writedAt(p.getWritedAt())
                .updatedAt(p.getUpdatedAt())
                .likes(p.getLikes())
                .views(p.getViews())
                .comments(p.getComments())
                .isDev(p.getIsDev())
                .devTags(p.getDevTags())
                .build();
    }

    // 수정
    @Transactional
    public void update(@NonNull String uuid, @NonNull PostUpdateRequest req, Authentication auth) {
        Post p = getPostByUuidOrThrow(uuid);
        ensureWriterOrAdmin(p, auth);

        if (req.getTitle() != null) {
            p.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            p.setContent(req.getContent());
        }
        if (req.getImages() != null) {
            p.setImages(req.getImages());
        }
        if (req.getVideos() != null) {
            p.setVideos(req.getVideos());
        }
        if (req.getIsDev() != null) {
            p.setIsDev(req.getIsDev());
        }
        if (req.getDevTags() != null) {
            p.setDevTags(req.getDevTags());
        }
    }

    // 삭제
    @Transactional
    public void delete(@NonNull String uuid, Authentication auth) {
        if (!postRepository.existsById(uuid)) {
            throw new EntityNotFoundException("Post not found: " + uuid);
        }
        Post p = getPostByUuidOrThrow(uuid);
        ensureWriterOrAdmin(p, auth);
        postRepository.deleteById(uuid);
    }

    @Transactional
    public boolean toggleLike(@NonNull String userUuid, @NonNull String postUuid) {
        Post post = getPostByUuidOrThrow(postUuid);

        boolean alreadyLiked = postLikeRepository.existsByUserUuidAndPostUuid(userUuid, postUuid);

        if (alreadyLiked) {
            // 좋아요 취소
            postLikeRepository.deleteByUserUuidAndPostUuid(userUuid, postUuid);
            post.setLikes(Math.max(0, post.getLikes() - 1));
            return false; // 좋아요 취소됨
        } else {
            // 좋아요 추가
            PostLike like = PostLike.builder()
                    .userUuid(userUuid)
                    .postUuid(postUuid)
                    .build();
            postLikeRepository.save(Objects.requireNonNull(like));

            post.setLikes(post.getLikes() + 1);
            return true; // 좋아요 눌림
        }

    }

    // 좋아요 id로 취소(또는 검사) 처리
    @Transactional
    public void deleteLikeById(@NonNull Long likeId, @NonNull String userUuid) {
        PostLike like = postLikeRepository.findById(likeId)
                .orElseThrow(() -> new IllegalArgumentException("Like not found: " + likeId));

        if (!like.getUserUuid().equals(userUuid)) {
            throw new AccessDeniedException("본인의 좋아요만 취소할 수 있습니다.");
        }

        String postUuid = Objects.requireNonNull(like.getPostUuid(), "postUuid is required");
        Post post = postRepository.findById(postUuid)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + like.getPostUuid()));

        // 좋아요 삭제
        postLikeRepository.deleteById(likeId);

        // 카운트 보정
        post.setLikes(Math.max(0, post.getLikes() - 1));
    }

}
