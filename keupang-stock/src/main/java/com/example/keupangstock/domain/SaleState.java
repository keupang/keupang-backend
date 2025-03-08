package com.example.keupangstock.domain;

public enum SaleState {
    ON_SALE,        // 판매 중 (재고와 무관)
    OUT_OF_STOCK,   // 품절 (재고 0이지만 판매 유지)
    DISCONTINUED,   // 판매 중지 (관리자가 수동으로 중단)
    DELETED         // 일정 기간(예: 1주일) 추가 재고 없으면 삭제 대상
}
