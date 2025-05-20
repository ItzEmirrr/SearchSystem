package com.example.searchengine.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.searchengine.model.PagedSearchResults;
import com.example.searchengine.model.SearchResultItem;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final ElasticsearchClient esClient;

    @Autowired
    public SearchController(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @GetMapping("/advanced")
    public ResponseEntity<PagedSearchResults> advancedSearch(
            @RequestParam String phrase,
            @RequestParam(defaultValue = "OR") String operator,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (phrase == null || phrase.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        logger.info("Advanced search request: phrase='{}', operator='{}', page={}, size={}", phrase, operator, page, size);

        String queryOperator = operator.equalsIgnoreCase("AND") ? "AND" : "OR";

        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                .index("books")
                .from(page * size)
                .size(size)
                .query(q -> q
                    .match(m -> m
                            .field("sentence")
                            .query(phrase)
                                    .operator(queryOperator.equalsIgnoreCase("and") ? Operator.And : Operator.Or)
                    )
                );
        try {
            SearchResponse<JsonNode> response = esClient.search(searchRequestBuilder.build(), JsonNode.class);

            List<SearchResultItem> results = new ArrayList<>();
            if (response.hits().hits() != null) {
                for (Hit<JsonNode> hit : response.hits().hits()) {
                    JsonNode source = hit.source();
                    if (source != null) {
                        String title = source.has("title") ? source.get("title").asText("Без названия") : "Без названия";
                        String filePath = source.has("file_path") ? source.get("file_path").asText("Не указан") : "Не указан";
                        String sentence = source.has("sentence") ? source.get("sentence").asText("") : "";
                        results.add(new SearchResultItem(title, filePath, sentence, hit.score() != null ? hit.score().floatValue() : 0f));

                    }
                }
            }
            long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
            PagedSearchResults pagedResults = new PagedSearchResults(results, page, size, totalHits);

            return ResponseEntity.ok(pagedResults);

        } catch (IOException e) {
            logger.error("Error during Elasticsearch search", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }
}