package com.example.auth.repository.search;

import com.example.auth.document.BoardDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardSearchRepository extends ElasticsearchRepository<BoardDocument, String> {
    Page<BoardDocument> findByTitleContainingOrContentContaining(String title, String content, Pageable pageable);
}
