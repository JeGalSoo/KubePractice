package com.example.auth.service;

import com.example.auth.dto.board.CommentRequestDto;
import com.example.auth.dto.board.CommentResponseDto;
import com.example.auth.entity.Board;
import com.example.auth.entity.Comment;
import com.example.auth.entity.User;
import com.example.auth.repository.BoardRepository;
import com.example.auth.repository.CommentRepository;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * 게시글에 새로운 댓글을 추가하고 DB에 저장합니다.
     * @param boardId 게시글 ID
     * @param requestDto 댓글 생성 정보
     * @param email 작성자 이메일
     * @return 생성된 댓글 응답 DTO
     */
    @Transactional
    public CommentResponseDto createComment(Long boardId, CommentRequestDto requestDto, String email) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .content(requestDto.getContent())
                .board(board)
                .author(user)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return CommentResponseDto.from(savedComment);
    }

    /**
     * 기존 댓글의 내용을 수정합니다. 작성자 본인인지, 해당 게시글의 댓글이 맞는지 검증합니다.
     * @param boardId 게시글 ID
     * @param commentId 댓글 ID
     * @param requestDto 수정할 댓글 내용
     * @param email 수정을 요청하는 사용자의 이메일
     * @return 수정된 댓글 정보 DTO
     */
    @Transactional
    public CommentResponseDto updateComment(Long boardId, Long commentId, CommentRequestDto requestDto, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }
        if (!comment.getAuthor().getEmail().equals(email)) {
            throw new IllegalArgumentException("작성자만 수정할 수 있습니다.");
        }

        comment.update(requestDto.getContent());
        return CommentResponseDto.from(comment);
    }

    /**
     * 기존 댓글을 데이터베이스에서 삭제합니다. 작성자 본인인지, 해당 게시글의 댓글인지 검증합니다.
     * @param boardId 게시글 ID
     * @param commentId 삭제할 댓글 ID
     * @param email 삭제를 요청하는 사용자의 이메일
     */
    @Transactional
    public void deleteComment(Long boardId, Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }
        if (!comment.getAuthor().getEmail().equals(email)) {
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }
}
