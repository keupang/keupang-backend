package com.example.keupangstock.service;

import com.example.keupangproduct.domain.Category;
import com.example.keupangproduct.domain.Product;
import com.example.keupangproduct.exception.CustomException;
import com.example.keupangstock.client.ProductClient;
import com.example.keupangstock.domain.SaleState;
import com.example.keupangstock.domain.Stock;
import com.example.keupangstock.domain.StockDetailImage;
import com.example.keupangstock.repository.StockRepository;
import com.example.keupangstock.response.StockDetailResponse;
import com.example.keupangstock.response.StockWithProductResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Value("${aws.s3.bucket}")
    private String bucketName;
    @Value("${aws.s3.region}")
    private String region;

    public Long createProduct(MultipartFile image, String name, Category category){

        ResponseEntity<Map<String,Object>> response = null;
        try {
            response = productClient.registerProduct(name, category, image);
        } catch (Exception ex) {
            log.error("에러 발생 : {}", ex.getMessage());
            throw new CustomException(
                HttpStatus.SERVICE_UNAVAILABLE,
                50301,
                "현재 상품 서비스를 이용할 수 없습니다.",
                "담당자에게 문의 후 서비스 다시 시도해주시기 바랍니다.",
                "SERVICE_UNAVAILABLE");
        }
        Map<String, Object> body = response.getBody();

        if(body == null){
            log.error("응답이 null 입니다.");
            throw new RuntimeException("응답이 null 입니다.");
        }

        Object dataObj = body.get("data");
        if(dataObj instanceof Map<?,?> data){
            Object productObj = data.get("product");
            if(productObj instanceof Map<?,?> productMap){
                Object idObj = productMap.get("id");

                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();  // 안전한 변환
                } else {
                    log.error("id 필드가 숫자가 아님");
                    throw new RuntimeException("id 필드가 숫자가 아님");
                }
            } else {
                log.error("product 필드가 Map 형식이 아님");
                throw new RuntimeException("product 필드가 Map 형식이 아님");
            }
        } else {
            log.error("data 필드가 Map 형식이 아님");
            throw new RuntimeException("data 필드가 Map 형식이 아님");
        }
    }

    public Stock createStoke(Long productId, Integer price, MultipartFile[] detailImages, Integer quantity)
        throws IOException {
        Stock stock = Stock.builder()
            .productId(productId)
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

        return stockRepository.save(stock);
    }

    public Page<StockWithProductResponse> getStocksWithProductInfo(
        String search,
        Category category,
        Integer minPrice,
        Integer maxPrice,
        String sortBy,
        int page, int size
    ){
        List<Product> products = productClient.getProductBySearch(search, category);
        products.forEach(p -> log.warn("Fetched product: id={}, name={}", p.getId(), p.getName()));

        List<Long> productIds = products.stream().map(Product::getId).toList();

        if(productIds.isEmpty()){
            return Page.empty(); //검색 결과 없음
        }
        Pageable pageable = PageRequest.of(page, size, getSort(sortBy));

        Page<Stock> stockPage = stockRepository.findByProductIdInAndPriceBetween(
            productIds,
            minPrice != null ? minPrice : 0,
            maxPrice != null ? maxPrice : Integer.MAX_VALUE,
            pageable
        );

        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<StockWithProductResponse> content = stockPage.getContent().stream()
            .map(stock -> new StockWithProductResponse(stock, productMap.get(stock.getProductId())))
            .sorted((a, b) -> {
                if (sortBy.equals("name")) {
                    return a.product().getName().compareToIgnoreCase(b.product().getName());
                }
                return 0;
            })
            .toList();

        return new PageImpl<>(content, pageable, stockPage.getTotalElements());
    }

    private Sort getSort(String sortBy) {
        return switch (sortBy) {
            case "priceAsc" -> Sort.by("price").ascending(); //가격 낮은순
            case "priceDesc" -> Sort.by("price").descending(); //가격 높은순
            case "new" -> Sort.by("createdAt").descending();  // 최신순
            case "sales" -> Sort.by("sales").descending();    // 판매량순
            default -> Sort.unsorted();
        };
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

        return StockDetailResponse.of(stock, product, images);
    }
}
