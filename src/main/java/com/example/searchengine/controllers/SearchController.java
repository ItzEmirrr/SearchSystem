package com.example.searchengine.controllers;

import com.example.searchengine.dto.SearchResultDto;
import com.example.searchengine.services.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/advanced")
    public List<SearchResultDto> advanced(
            @RequestParam String phrase,
            @RequestParam(defaultValue = "OR") String operator,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {
        return searchService.search(searchService.buildRankedQuery(phrase, operator), page, size);
    }
}
