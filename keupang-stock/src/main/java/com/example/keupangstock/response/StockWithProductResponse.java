package com.example.keupangstock.response;

import com.example.keupangstock.domain.Stock;
import com.example.keupangstock.domain.product.Product;

public record StockWithProductResponse(Stock stock, Product product) {
}
