package com.example.auth.dto.board;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentRequestDto {
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
}
