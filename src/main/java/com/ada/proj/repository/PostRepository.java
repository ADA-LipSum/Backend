// src/main/java/com/ada/proj/repository/PostRepository.java
package com.ada.proj.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ada.proj.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    // 목록: 작성시간 최신순
    Page<Post> findAllByOrderByWritedAtDesc(Pageable pageable);

        // 게시글 목록 + 작성자 프로필이미지(Writer UUID로 User 조인) — 프로젝션으로 N+1 방지
        @Query("select new com.ada.proj.dto.PostSummaryResponse(p.postUuid, p.seq, p.title, p.writer, u.profileImage, p.writedAt, p.likes, p.views, p.comments, p.isDev, p.devTags, null) " +
          "from Post p left join com.ada.proj.entity.User u on u.uuid = p.writerUuid")
        Page<com.ada.proj.dto.PostSummaryResponse> findSummaryPage(Pageable pageable);

    // 조회수 +1
    @Modifying
    @Query("update Post p set p.views = p.views + 1 where p.postUuid = :uuid")
    int increaseViews(@Param("uuid") String uuid);

    // 좋아요 +1
    @Modifying
    @Query("update Post p set p.likes = p.likes + 1 where p.postUuid = :uuid")
    int increaseLikes(@Param("uuid") String uuid);

    // 좋아요 -1 (최소 0)
    @Modifying
    @Query("""
           update Post p
              set p.likes = case when p.likes > 0 then p.likes - 1 else 0 end
            where p.postUuid = :uuid
           """)
    int decreaseLikes(@Param("uuid") String uuid);

    // seq 기반 검색 지원
    java.util.Optional<Post> findBySeq(Long seq);
}