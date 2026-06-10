package com.example.auth.controller;

import com.example.auth.dto.board.BoardRequestDto;
import com.example.auth.dto.board.BoardResponseDto;
import com.example.auth.service.BoardSearchService;
import com.example.auth.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final BoardSearchService boardSearchService;

    /**
     * 새로운 게시글을 생성합니다.
     * @param requestDto 생성할 게시글 제목 및 내용
     * @param userDetails 작성자의 인증 정보
     * @return 생성된 게시글 응답 DTO
     */
    @PostMapping
    public ResponseEntity<BoardResponseDto> createBoard(
            @Valid @RequestBody BoardRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        BoardResponseDto responseDto = boardService.createBoard(requestDto, userDetails.getUsername());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 전체 게시글 목록을 페이징하여 조회합니다.
     * @param pageable 페이징 인자
     * @return 페이징된 게시글 목록
     */
    @GetMapping
    public ResponseEntity<Page<BoardResponseDto>> getAllBoards(
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(boardService.getAllBoards(pageable));
    }

    /**
     * 특정 게시글의 상세 내용을 조회합니다 (댓글 포함).
     * @param id 게시글 ID
     * @return 게시글 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponseDto> getBoard(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getBoard(id));
    }

    /**
     * 특정 게시글을 수정합니다 (작성자 또는 관리자 권한 필요).
     * @param id 수정할 게시글 ID
     * @param requestDto 수정될 제목 및 내용
     * @param userDetails 요청자의 인증 정보
     * @return 수정된 게시글 응답 DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponseDto> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody BoardRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        BoardResponseDto responseDto = boardService.updateBoard(id, requestDto, userDetails.getUsername());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 특정 게시글을 삭제합니다 (작성자 또는 관리자 권한 필요).
     * @param id 삭제할 게시글 ID
     * @param userDetails 요청자의 인증 정보
     * @return 상태코드 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        boardService.deleteBoard(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Elasticsearch를 이용하여 게시글을 전문 검색합니다.
     * @param keyword 검색어
     * @param pageable 페이징 인자
     * @return 검색된 결과가 포함된 페이징 객체
     */
    @GetMapping("/search")
    public ResponseEntity<org.springframework.data.domain.Page<com.example.auth.document.BoardDocument>> searchBoards(
            @RequestParam("q") String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(boardSearchService.search(keyword, pageable));
    }
}
