package com.ada.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.Comment;
import com.ada.proj.entity.Post;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"children", "author"})
    List<Comment> findByPostAndParentIsNullOrderByCreatedAtAsc(Post post);
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);
    
    List<Comment> findByParentOrderByCreatedAtAsc(Comment parent);
    long countByPost(Post post);
}