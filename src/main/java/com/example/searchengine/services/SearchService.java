package com.example.searchengine.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.searchengine.dto.SearchResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchClient client;

    public List<SearchResultDto> search(Query query, int page, int size) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index("books")
                .from(page * size)
                .size(size)
                .query(query)
        );

        SearchResponse<Map> response = client.search(request, Map.class);

        return response.hits().hits().stream()
                .map(Hit::source)
                .map(source -> new SearchResultDto(
                        (String) source.get("title"),
                        (String) source.get("file_path"),
                        (String) source.get("sentence")
                ))
                .toList();
    }

    public Query buildExactPhraseQuery(String phrase) {
        return MatchPhraseQuery.of(m -> m.field("sentence").query(phrase))._toQuery();
    }

    public Query buildRankedQuery(String userQuery, String operator) {

        Query exactPhraseQuery = MatchPhraseQuery.of(m -> m
                .field("sentence")
                .query(userQuery)
                .boost(3.0f)
        )._toQuery();

        MultiMatchQuery looseQuery = MultiMatchQuery.of(m -> m
                .fields("sentence")
                .query(userQuery)
                .operator(operator.equalsIgnoreCase("AND") ? Operator.And : Operator.Or)
                .type(TextQueryType.BestFields)
                .minimumShouldMatch("2")
                .boost(1.0f)
        );

        MultiMatchQuery fuzzyQuery = MultiMatchQuery.of(m -> m
                .fields("sentence")
                .query(userQuery)
                .operator(operator.equalsIgnoreCase("AND") ? Operator.And : Operator.Or)
                .type(TextQueryType.BestFields)
                .fuzziness("AUTO")
                .minimumShouldMatch("2")
                .boost(0.5f)
        );

        BoolQuery boolQuery = BoolQuery.of(b -> b
                .should(exactPhraseQuery)
                .should(looseQuery._toQuery())
                .should(fuzzyQuery._toQuery())
        );

        return Query.of(q -> q.bool(boolQuery));
    }
}
