package com.example.auth.service;

import com.example.auth.document.BoardDocument;
import com.example.auth.entity.Board;
import com.example.auth.repository.search.BoardSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardSearchService {

    private final BoardSearchRepository boardSearchRepository;

    public void indexBoard(Board board) {
        BoardDocument document = BoardDocument.builder()
                .id(board.getId().toString())
                .title(board.getTitle())
                .content(board.getContent())
                .authorName(board.getUser().getName())
                .createdAt(board.getCreatedAt())
                .build();
        boardSearchRepository.save(document);
    }

    public void deleteBoard(Long boardId) {
        boardSearchRepository.deleteById(boardId.toString());
    }



    public Page<BoardDocument> search(String keyword, Pageable pageable) {
        return boardSearchRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
    }
}
