package com.example.auth.dto.board;

import com.example.auth.entity.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.util.List;
import java.util.ArrayList;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponseDto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private List<CommentResponseDto> comments = new ArrayList<>();

    public static BoardResponseDto from(Board board) {
        return BoardResponseDto.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .authorId(board.getUser().getId())
                .authorName(board.getUser().getName())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                // Assuming comments are fetched and mapped elsewhere or mapped here if EAGER/Transactional
                .build();
    }
}
