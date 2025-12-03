package com.example.keupangstock.component;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StockQueryBuilder {
    public Query buildQuery(String search, String category, Integer minPrice, Integer maxPrice) {
        List<Query> must = new ArrayList<>();

        // 검색어가 있을 경우 productName OR keywords
        if (search != null && !search.isBlank()) {
            List<Query> should = new ArrayList<>();
            should.add(MatchQuery.of(m -> m.field("productName").query(search))._toQuery());
            should.add(MatchQuery.of(m -> m.field("keywords").query(search))._toQuery());

            must.add(BoolQuery.of(b -> b
                .should(should)
                .minimumShouldMatch("1")
            )._toQuery());
        }

        if (category != null) {
            must.add(TermQuery.of(t -> t
                .field("category")
                .value(category)
            )._toQuery());
        }

        if (minPrice != null || maxPrice != null) {
            RangeQuery.Builder range = new RangeQuery.Builder().field("price");
            if (minPrice != null) range.gte(JsonData.of(minPrice));
            if (maxPrice != null) range.lte(JsonData.of(maxPrice));
            must.add(range.build()._toQuery());
        }

        return BoolQuery.of(b -> b.must(must))._toQuery();
    }
}
