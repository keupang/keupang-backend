package com.example.keupangstock.response;

import com.example.keupangstock.domain.Stock;
import com.example.keupangstock.domain.StockDetailImage;
import com.example.keupangstock.domain.product.Product;
import java.util.List;

public record StockDetailResponse(
    Long stockId,
    Long productId,
    String productName,
    String productImage,
    String category,
    Integer price,
    Integer quantity,
    String saleState,
    List<String> detailImages,
    List<ReviewResponse> reviews
) {
    public static StockDetailResponse of(Stock stock, Product product, List<StockDetailImage> images, List<ReviewResponse> reviews) {
        return new StockDetailResponse(
            stock.getId(),
            stock.getProductId(),
            product.getName(),
            product.getImageUrl(),
            product.getCategory().name(),
            stock.getPrice(),
            stock.getQuantity(),
            stock.getSaleState().name(),
            images.stream().map(StockDetailImage::getImageUrl).toList(),
            reviews
        );
    }
}
