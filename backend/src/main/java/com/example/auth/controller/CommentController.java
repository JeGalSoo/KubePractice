package com.example.auth.controller;

import com.example.auth.dto.board.CommentRequestDto;
import com.example.auth.dto.board.CommentResponseDto;
import com.example.auth.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boards/{boardId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 특정 게시글에 새로운 댓글을 작성합니다.
     * @param boardId 대상 게시글 ID
     * @param requestDto 댓글 내용
     * @param userDetails 작성자 인증 정보
     * @return 작성된 댓글 정보
     */
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long boardId,
            @Valid @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentResponseDto responseDto = commentService.createComment(boardId, requestDto, userDetails.getUsername());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 지정된 게시글의 특정 댓글 내용을 수정합니다 (작성자만 가능).
     * @param boardId 게시글 ID
     * @param commentId 수정할 댓글 ID
     * @param requestDto 변경할 댓글 내용
     * @param userDetails 요청자 인증 정보
     * @return 수정된 댓글 정보
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentResponseDto responseDto = commentService.updateComment(boardId, commentId, requestDto, userDetails.getUsername());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 지정된 게시글의 특정 댓글을 삭제합니다 (작성자만 가능).
     * @param boardId 게시글 ID
     * @param commentId 삭제할 댓글 ID
     * @param userDetails 요청자 인증 정보
     * @return 상태코드 204 No Content
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(boardId, commentId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
