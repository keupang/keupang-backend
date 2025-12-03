package com.example.keupangstock.service;

import com.example.keupangstock.client.ProductClient;
import com.example.keupangstock.client.ReviewClient;
import com.example.keupangstock.domain.SaleState;
import com.example.keupangstock.domain.Stock;
import com.example.keupangstock.domain.StockDetailImage;
import com.example.keupangstock.domain.product.Category;
import com.example.keupangstock.domain.product.Product;
import com.example.keupangstock.exception.CustomException;
import com.example.keupangstock.repository.StockRepository;
import com.example.keupangstock.response.ReviewResponse;
import com.example.keupangstock.response.StockDetailResponse;
import com.example.keupangstock.response.StockWithProductResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {
        private final StockRepository stockRepository;
        private final ProductClient productClient;
        private final S3Client s3Client;
        private final ReviewClient reviewClient;

        @Value("${aws.s3.bucket}")
        private String bucketName;
        @Value("${aws.s3.region}")
        private String region;

        public Product createProduct(MultipartFile image, String name, Integer price, Category category,
                        List<String> keywords) {

                Product product;
                try {
                        product = productClient.registerProduct(name, price, null, category, image, keywords).getBody();
                } catch (Exception ex) {
                        log.error("에러 발생 : {}", ex.getMessage());
                        throw new CustomException(
                                        HttpStatus.SERVICE_UNAVAILABLE,
                                        50301,
                                        "현재 상품 서비스를 이용할 수 없습니다.",
                                        "담당자에게 문의 후 서비스 다시 시도해주시기 바랍니다.",
                                        "SERVICE_UNAVAILABLE");
                }
                if (product == null) {
                        log.error("응답이 null 입니다.");
                        throw new RuntimeException("응답이 null 입니다.");
                }

                return product;
        }

        public Stock createStoke(Product product, Integer price, MultipartFile[] detailImages, Integer quantity)
                        throws IOException {
                Stock stock = Stock.builder()
                                .productId(product.getId())
                                .saleState(SaleState.ON_SALE)
                                .price(price)
                                .quantity(quantity)
                                .detailImages(new ArrayList<>())
                                .build();

                List<StockDetailImage> detailImageList = new ArrayList<>();

                for (MultipartFile detailImage : detailImages) {
                        String imageName = UUID.randomUUID().toString();
                        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(imageName)
                                        .acl(ObjectCannedACL.PUBLIC_READ)
                                        .contentType(detailImage.getContentType())
                                        .build();

                        s3Client.putObject(
                                        putObjectRequest,
                                        RequestBody.fromInputStream(detailImage.getInputStream(),
                                                        detailImage.getSize()));

                        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                                        bucketName, region, imageName);

                        StockDetailImage stockDetailImage = StockDetailImage.builder()
                                        .imageUrl(imageUrl)
                                        .stock(stock)
                                        .build();

                        detailImageList.add(stockDetailImage);
                }

                stock.getDetailImages().addAll(detailImageList);
                return stockRepository.save(stock);
        }

        // 상품 목록 조회
        public Page<StockWithProductResponse> getStocksWithProductInfo(
                        String search,
                        Category category,
                        Integer minPrice,
                        Integer maxPrice,
                        String sortBy,
                        int page, int size) {

                Sort sort = switch (sortBy) {
                        case "priceAsc" -> Sort.by("price").ascending();
                        case "priceDesc" -> Sort.by("price").descending();
                        case "sales" -> Sort.by("sales").descending();
                        case "name" -> Sort.by("createdAt").descending(); // Fallback for name sort
                        default -> Sort.by("createdAt").descending();
                };

                PageRequest pageRequest = PageRequest.of(page, size, sort);
                Page<Stock> stockPage;

                if ((search != null && !search.isBlank()) || category != null) {
                        List<Product> products = productClient.getProductBySearch(search, category);
                        List<Long> productIds = products.stream().map(Product::getId).toList();

                        if (productIds.isEmpty()) {
                                return new PageImpl<>(Collections.emptyList(), pageRequest, 0);
                        }

                        stockPage = stockRepository.findByProductIdInAndPriceBetween(productIds,
                                        minPrice != null ? minPrice : 0,
                                        maxPrice != null ? maxPrice : Integer.MAX_VALUE, pageRequest);
                } else {
                        stockPage = stockRepository.findByPriceBetween(minPrice != null ? minPrice : 0,
                                        maxPrice != null ? maxPrice : Integer.MAX_VALUE, pageRequest);
                }

                List<Long> productIds = stockPage.getContent().stream().map(Stock::getProductId).toList();
                List<Product> products = productClient.getProductInfoList(productIds);
                Map<Long, Product> productMap = products.stream()
                                .collect(Collectors.toMap(Product::getId, Function.identity()));

                List<StockWithProductResponse> content = stockPage.getContent().stream()
                                .map(stock -> new StockWithProductResponse(stock, productMap.get(stock.getProductId())))
                                .toList();

                return new PageImpl<>(content, pageRequest, stockPage.getTotalElements());
        }

        public StockDetailResponse getStockDetail(Long stockId) {
                Stock stock = stockRepository.findWithDetailImagesById(stockId)
                                .orElseThrow(() -> new CustomException(
                                                HttpStatus.NOT_FOUND, 40401, "해당 재고를 찾을 수 없습니다.", "재고 ID를 확인해주세요.",
                                                "STOCK_NOT_FOUND"));

                Product product = productClient.getProductInfoList(List.of(stock.getProductId()))
                                .stream()
                                .findFirst()
                                .orElseThrow(() -> new CustomException(
                                                HttpStatus.NOT_FOUND, 40402, "상품 정보를 찾을 수 없습니다.", "상품 ID를 확인해주세요.",
                                                "PRODUCT_NOT_FOUND"));

                List<StockDetailImage> images = stock.getDetailImages();

                List<ReviewResponse> reviews = Collections.emptyList();

                try {
                        reviews = reviewClient.getReviewsByProductId(product.getId());
                } catch (Exception e) {
                        log.warn("리뷰 조회 실패: productId={}, error={}", product.getId(), e.getMessage());
                }
                return StockDetailResponse.of(stock, product, images, reviews);
        }

        public Product getProductById(Long productId) {
                return productClient.getProductById(productId);
        }
}
