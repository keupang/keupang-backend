package com.example.keupangproduct.repository;

import com.example.keupangproduct.domain.ProductKeyword;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductKeywordRepository extends JpaRepository<ProductKeyword, Long> {
    @Query("SELECT pk.keyword, COUNT(pk) as freq " +
            "FROM ProductKeyword pk " +
            "WHERE pk.keyword LIKE CONCAT(:prefix, '%') " +
            "GROUP BY pk.keyword " +
            "ORDER BY freq DESC")
    List<Object[]> findTop10ByKeywordPrefix(@Param("prefix") String prefix, Pageable pageable);

    List<ProductKeyword> findByProductIdIn(List<Long> productIds);
}
