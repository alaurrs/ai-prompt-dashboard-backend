package com.sallyvnge.aipromptbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngestService {
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public void ingest(String id, String text) {
        Document document = Document.builder().id(id).text(text).build();
        vectorStore.add(List.of(document));
    }

}
