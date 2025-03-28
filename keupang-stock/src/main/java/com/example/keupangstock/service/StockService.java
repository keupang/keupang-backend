package com.example.keupangstock.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.keupangproduct.domain.Category;
import com.example.keupangproduct.domain.Product;
import com.example.keupangproduct.exception.CustomException;
import com.example.keupangstock.client.ProductClient;
import com.example.keupangstock.domain.SaleState;
import com.example.keupangstock.domain.Stock;
import com.example.keupangstock.repository.StockRepository;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {
    private final StockRepository stockRepository;
    private final ProductClient productClient;
    private final AmazonS3 amazonS3;
    @Value("${aws.s3.bucket}")
    private String bucketName;

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

    public Stock createStoke(Long productId, Integer price, MultipartFile detailImage, Integer quantity)
        throws IOException {
        String imageName = UUID.randomUUID() + "_" + detailImage.getOriginalFilename();
        amazonS3.putObject(new PutObjectRequest(bucketName, imageName, detailImage.getInputStream(), null)
            .withCannedAcl(CannedAccessControlList.PublicRead));
        String imageUrl = amazonS3.getUrl(bucketName, imageName).toString();
        log.info("imageUrl = {}", imageUrl);
        Stock stock = Stock.builder()
            .productId(productId)
            .saleState(SaleState.ON_SALE)
            .price(price)
            .quantity(quantity)
            .detailImage(imageUrl)
            .build();
        return stockRepository.save(stock);
    }
}
