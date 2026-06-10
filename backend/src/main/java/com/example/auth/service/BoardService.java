package com.example.auth.service;

import com.example.auth.dto.board.BoardRequestDto;
import com.example.auth.dto.board.BoardResponseDto;
import com.example.auth.entity.Board;
import com.example.auth.entity.User;
import com.example.auth.repository.BoardRepository;
import com.example.auth.repository.CommentRepository;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final BoardSearchService boardSearchService;

    /**
     * 새로운 게시글을 생성하고 Elasticsearch에 색인합니다.
     * @param requestDto 게시글 제목 및 내용
     * @param email 작성자 이메일
     * @return 생성된 게시글 응답 DTO
     */
    @Transactional
    @CacheEvict(value = "boards", allEntries = true)
    public BoardResponseDto createBoard(BoardRequestDto requestDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board board = Board.builder()
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .user(user)
                .build();

        Board savedBoard = boardRepository.save(board);
        
        // Elasticsearch 동기화
        boardSearchService.indexBoard(savedBoard);
        
        return BoardResponseDto.from(savedBoard);
    }

    /**
     * 캐시를 사용하여 전체 게시글 목록을 페이징하여 조회합니다.
     * @param pageable 페이징 인자
     * @return 페이징된 게시글 응답 DTO
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "boards", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<BoardResponseDto> getAllBoards(Pageable pageable) {
        return boardRepository.findAll(pageable).map(BoardResponseDto::from);
    }

    /**
     * 특정 게시글의 상세 내용을 조회하고 캐싱합니다. 관련된 댓글 목록도 함께 로드합니다.
     * @param id 조회할 게시글 ID
     * @return 게시글 상세 정보 (댓글 포함)
     */
    @Cacheable(value = "board", key = "#id")
    public BoardResponseDto getBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        BoardResponseDto response = BoardResponseDto.from(board);
        
        java.util.List<com.example.auth.dto.board.CommentResponseDto> comments = commentRepository.findByBoardIdOrderByCreatedAtAsc(id)
                .stream().map(com.example.auth.dto.board.CommentResponseDto::from).toList();
        
        return BoardResponseDto.builder()
                .id(response.getId())
                .title(response.getTitle())
                .content(response.getContent())
                .authorId(response.getAuthorId())
                .authorName(response.getAuthorName())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .comments(comments)
                .build();
    }

    /**
     * 특정 게시글을 수정하고, Elasticsearch 색인 및 관련 캐시를 갱신합니다.
     * @param id 게시글 ID
     * @param requestDto 수정할 제목/내용
     * @param email 요청자 이메일
     * @return 수정된 게시글 정보
     */
    @Transactional
    @CacheEvict(value = {"boards", "board"}, allEntries = true)
    public BoardResponseDto updateBoard(Long id, BoardRequestDto requestDto, String email) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!board.getUser().getEmail().equals(email) && user.getRole() != com.example.auth.entity.Role.ADMIN) {
             throw new IllegalArgumentException("작성자 또는 관리자만 수정할 수 있습니다.");
        }

        board.setTitle(requestDto.getTitle());
        board.setContent(requestDto.getContent());

        // Elasticsearch 동기화
        boardSearchService.indexBoard(board);

        return BoardResponseDto.from(board);
    }

    /**
     * 특정 게시글을 데이터베이스와 Elasticsearch에서 삭제하고, 관련 캐시를 무효화합니다.
     * @param id 삭제할 게시글 ID
     * @param email 요청자 이메일
     */
    @Transactional
    @CacheEvict(value = {"boards", "board"}, allEntries = true)
    public void deleteBoard(Long id, String email) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!board.getUser().getEmail().equals(email) && user.getRole() != com.example.auth.entity.Role.ADMIN) {
            throw new IllegalArgumentException("작성자 또는 관리자만 삭제할 수 있습니다.");
        }

        boardRepository.delete(board);
        // Elasticsearch 동기화
        boardSearchService.deleteBoard(id);
    }
}
