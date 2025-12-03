package com.example.keupangstock.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.keupangstock.client.ProductClient;
import com.example.keupangstock.client.ReviewClient;
import com.example.keupangstock.component.StockQueryBuilder;
import com.example.keupangstock.domain.SaleState;
import com.example.keupangstock.domain.Stock;
import com.example.keupangstock.domain.StockDetailImage;
import com.example.keupangstock.domain.StockDocument;
import com.example.keupangstock.domain.product.Category;
import com.example.keupangstock.domain.product.Product;
import com.example.keupangstock.exception.CustomException;
import com.example.keupangstock.repository.StockRepository;
import com.example.keupangstock.response.ReviewResponse;
import com.example.keupangstock.response.StockDetailResponse;
import com.example.keupangstock.response.StockWithProductResponse;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
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
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ElasticsearchClient elasticsearchClient;
    private final StockQueryBuilder stockQueryBuilder;

    @Value("${aws.s3.bucket}")
    private String bucketName;
    @Value("${aws.s3.region}")
    private String region;

    public void saveStockDocument(Stock stock, Product product, List<String> keywords) {
        StockDocument doc = StockDocument.builder()
            .stockId(stock.getId())
            .productId(product.getId())
            .productName(product.getName())
            .category(product.getCategory().name())
            .imageUrl(product.getImageUrl())
            .price(stock.getPrice())
            .quantity(stock.getQuantity())
            .sales(stock.getSales())
            .createdAt(stock.getCreatedAt().atOffset(ZoneOffset.UTC))
            .keywords(keywords)
            .build();

        elasticsearchTemplate.save(doc);
    }

    public Product createProduct(MultipartFile image, String name, Category category, List<String> keywords){

        Product product;
        try {
            product = productClient.registerProduct(name, category, image, keywords).getBody();
        } catch (Exception ex) {
            log.error("에러 발생 : {}", ex.getMessage());
            throw new CustomException(
                HttpStatus.SERVICE_UNAVAILABLE,
                50301,
                "현재 상품 서비스를 이용할 수 없습니다.",
                "담당자에게 문의 후 서비스 다시 시도해주시기 바랍니다.",
                "SERVICE_UNAVAILABLE");
        }
        if(product == null){
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

        for(MultipartFile detailImage : detailImages){
            String imageName = UUID.randomUUID().toString();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(imageName)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .contentType(detailImage.getContentType())
                .build();

            s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(detailImage.getInputStream(), detailImage.getSize())
            );

            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, imageName);

            StockDetailImage stockDetailImage = StockDetailImage.builder()
                .imageUrl(imageUrl)
                .stock(stock)
                .build();

            detailImageList.add(stockDetailImage);
        }

        stock.getDetailImages().addAll(detailImageList);
        Stock save = stockRepository.save(stock);

        List<String> keywords = productClient.getProductKeywords(List.of(product.getId())).getOrDefault(product.getId(), List.of());
        saveStockDocument(stock, product, keywords);
        return save;
    }

    // 상품 목록 조회
    public Page<StockWithProductResponse> getStocksWithProductInfo(
        String search,
        Category category,
        Integer minPrice,
        Integer maxPrice,
        String sortBy,
        int page, int size
    ) {
        try {
            // 1. 쿼리 생성
            Query query = stockQueryBuilder.buildQuery(
                search,
                category != null ? category.name() : null,
                minPrice,
                maxPrice
            );

            // 2. 정렬 기준 생성
            SortOptions sortOption = switch (sortBy) {
                case "priceAsc" -> SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Asc)));
                case "priceDesc" -> SortOptions.of(s -> s.field(f -> f.field("price").order(SortOrder.Desc)));
                case "sales" -> SortOptions.of(s -> s.field(f -> f.field("sales").order(SortOrder.Desc)));
                case "name" -> SortOptions.of(s -> s.field(f -> f.field("productName.keyword").order(SortOrder.Asc)));
                default -> SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc)));
            };

            // 3. 검색 요청 생성
            SearchRequest request = SearchRequest.of(s -> s
                .index("stock_index")
                .query(query)
                .collapse(c -> c.field("stockId"))
                .sort(List.of(sortOption)) // sortOption 하나만 넣을 경우에도 List.of로 감싸야 함
                .from(page * size)
                .size(size)
            );

            // 4. 검색 실행
            SearchResponse<StockDocument> response = elasticsearchClient.search(request, StockDocument.class);

            // 5. 결과 매핑
            List<StockDocument> docs = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();

            // 6. 연관된 product 정보 조회
            List<Product> products = productClient.getProductBySearch(search, category);
            Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

            List<StockWithProductResponse> content = docs.stream()
                .map(doc -> new StockWithProductResponse(doc.toStock(), productMap.get(doc.getProductId())))
                .toList();

            long total = response.hits().total() != null ? response.hits().total().value() : content.size();

            return new PageImpl<>(content, PageRequest.of(page, size), total);

        } catch (IOException e) {
            log.error("Elasticsearch 검색 중 오류 발생", e);
            throw new CustomException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                50001,
                "상품 목록 검색 중 오류가 발생했습니다.",
                "나중에 다시 시도해주세요.",
                "ES_SEARCH_FAILED"
            );
        }
    }

    public StockDetailResponse getStockDetail(Long stockId) {
        Stock stock = stockRepository.findWithDetailImagesById(stockId)
            .orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND, 40401, "해당 재고를 찾을 수 없습니다.", "재고 ID를 확인해주세요.", "STOCK_NOT_FOUND"
            ));

        Product product = productClient.getProductInfoList(List.of(stock.getProductId()))
            .stream()
            .findFirst()
            .orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND, 40402, "상품 정보를 찾을 수 없습니다.", "상품 ID를 확인해주세요.", "PRODUCT_NOT_FOUND"
            ));

        List<StockDetailImage> images = stock.getDetailImages(); // 이미 FetchType.LAZY면 자동 조회됨

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

    //elastic data migration
    public void migrateExistingStocksToES() {
        List<Stock> stocks = stockRepository.findAll(); // 또는 필요한 조건

        // productId -> product 조회
        List<Long> productIds = stocks.stream()
            .map(Stock::getProductId)
            .distinct()
            .toList();

        // 상품 정보 조회
        List<Product> products = productClient.getProductInfoList(productIds);
        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 상품 키워드 정보 조회
        Map<Long, List<String>> keywordMap = productClient.getProductKeywords(productIds);

        for (Stock stock : stocks) {
            Product product = productMap.get(stock.getProductId());
            if (product != null) {
                List<String> keywords = keywordMap.getOrDefault(product.getId(), List.of());
                saveStockDocument(stock, product, keywords);
            }
        }
    }

}
