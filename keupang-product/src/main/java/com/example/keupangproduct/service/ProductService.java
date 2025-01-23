package com.example.keupangproduct.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.keupangproduct.domain.Category;
import com.example.keupangproduct.domain.Product;
import com.example.keupangproduct.repository.ProductRepository;
import com.example.keupangproduct.request.ProductRequest;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final AmazonS3 amazonS3;
    @Value("${aws.s3.bucket}")
    private String bucketName;

    public Page<Product> getProducts(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByNameContainingIgnoreCase(search, pageable);
    }

    public Product saveProduct(ProductRequest productRequest)
        throws IOException, IOException {
        String imageName = UUID.randomUUID() + "_" + productRequest.imageUrl().getOriginalFilename();
        amazonS3.putObject(new PutObjectRequest(bucketName, imageName, productRequest.imageUrl().getInputStream(), null)
            .withCannedAcl(CannedAccessControlList.PublicRead));
        String imageUrl = amazonS3.getUrl(bucketName, imageName).toString();

        Product product = Product.builder()
            .name(productRequest.name())
            .price(productRequest.price())
            .category(productRequest.category())
            .imageUrl(imageUrl)
            .build();

        return productRepository.save(product);
    }

}
