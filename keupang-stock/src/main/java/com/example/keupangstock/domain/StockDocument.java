package com.example.keupangstock.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "stock_index")
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockDocument {
    @Id
    private Long stockId;

    private Long productId;
    private String productName;
    private String category;
    private String imageUrl;

    private Integer price;
    private Integer quantity;
    private Integer sales;
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime createdAt;
    private List<String> keywords;

    @Builder
    public StockDocument(Long stockId, Long productId,
        String productName, String category, String imageUrl,
        Integer price, Integer quantity, Integer sales, OffsetDateTime createdAt,
        List<String> keywords){
        this.category = category;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.price = price;
        this.quantity = quantity;
        this.sales = sales;
        this.createdAt = createdAt;
        this.stockId = stockId;
        this.productId = productId;
        this.keywords = keywords;
    }

    public Stock toStock() {
        return Stock.builder()
            .id(this.stockId)
            .productId(this.productId)
            .price(this.price)
            .quantity(this.quantity)
            .sales(this.sales)
            .saleState(SaleState.ON_SALE) // 혹은 필드 추가
            .createdAt(this.createdAt.toLocalDateTime())
            .build();
    }
}
