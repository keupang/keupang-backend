package com.example.keupangstock.response;

import com.example.keupangproduct.domain.Product;
import com.example.keupangstock.domain.Stock;
import com.example.keupangstock.domain.StockDetailImage;
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
    List<String> detailImages
) {
    public static StockDetailResponse of(Stock stock, Product product, List<StockDetailImage> images) {
        return new StockDetailResponse(
            stock.getId(),
            stock.getProductId(),
            product.getName(),
            product.getImageUrl(),
            product.getCategory().name(),
            stock.getPrice(),
            stock.getQuantity(),
            stock.getSaleState().name(),
            images.stream().map(StockDetailImage::getImageUrl).toList()
        );
    }
}
