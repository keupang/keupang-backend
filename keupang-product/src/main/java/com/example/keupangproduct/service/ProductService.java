package com.example.keupangproduct.service;

import com.example.keupangproduct.domain.Category;
import com.example.keupangproduct.domain.Product;
import com.example.keupangproduct.repository.ProductRepository;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final S3Client s3Client;
    @Value("${aws.s3.bucket}")
    private String bucketName;
    @Value("${aws.s3.region}")
    private String region;

    public Page<Product> getProducts(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByNameContainingIgnoreCase(search, pageable);
    }

    public Product createProduct(String name, Category category, MultipartFile image)
        throws IOException {
        String imageName = UUID.randomUUID().toString();
        log.info("image type : {}", image.getContentType());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                    .key(imageName)
                        .acl(ObjectCannedACL.PUBLIC_READ)
                            .contentType(image.getContentType())
                                .build();

        s3Client.putObject(
            putObjectRequest,
            RequestBody.fromInputStream(image.getInputStream(), image.getSize())
        );
        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
            bucketName, region,imageName);

        Product product = Product.builder()
            .name(name)
            .category(category)
            .imageUrl(imageUrl)
            .build();

//        s3Client.putObject(new PutObjectRequest(bucketName, imageName, image.getInputStream(), null)
//            .withCannedAcl(CannedAccessControlList.PublicRead));
//        String imageUrl = s3Client.getUrl(bucketName, imageName).toString();
//

        return productRepository.save(product);
    }

}
