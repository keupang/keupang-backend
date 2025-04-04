package com.example.keupangstock.response;

import com.example.keupangproduct.domain.Product;
import com.example.keupangstock.domain.Stock;

public record StockWithProductResponse(Stock stock, Product product) {

}
