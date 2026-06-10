package com.example.auth.dto.board;

import com.example.auth.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String content;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
            .id(comment.getId())
            .content(comment.getContent())
            .authorId(comment.getAuthor().getId())
            .authorName(comment.getAuthor().getName())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }
}
