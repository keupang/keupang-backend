#!/usr/bin/env python3
import argparse
import csv
import gzip
import json
import random
import re
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


@dataclass
class DatasetPair:
    metadata: Path
    reviews: Path | None


def load_policy(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def open_jsonl(path: Path) -> Iterable[dict[str, Any]]:
    opener = gzip.open if path.suffix == ".gz" else open
    with opener(path, "rt", encoding="utf-8") as file:
        for line in file:
            line = line.strip()
            if line:
                yield json.loads(line)


def clean_text(value: Any, max_len: int = 1000) -> str:
    if value is None:
        return ""
    if isinstance(value, list):
        value = " ".join(str(item) for item in value if item)
    value = re.sub(r"\s+", " ", str(value)).strip()
    return value[:max_len]


def compact_title(value: str, max_len: int = 80) -> str:
    value = re.sub(r"\([^)]*\)", "", value)
    value = re.sub(r"\[[^]]*]", "", value)
    value = re.split(r"\s+-\s+|\s+for\s+|,\s+Pack|,\s+\d+\s*Pack", value, maxsplit=1, flags=re.IGNORECASE)[0]
    value = clean_text(value, max_len)
    return value.strip(" ,-") or "프리미엄 상품"


def korean_product_name(title: str, category: str) -> str:
    base = compact_title(title)
    suffix = {
        "FASHION": "패션 아이템",
        "ELECTRONICS": "전자제품",
        "SPORTS": "스포츠 용품",
        "BOOKS": "도서",
        "FOOD": "식품",
        "BEAUTY": "뷰티 케어",
        "HOME": "생활용품",
        "HOBBY": "취미용품",
        "AUTO": "자동차용품",
    }.get(category, "상품")
    return f"{base} {suffix}"


def korean_review_content(raw: dict[str, Any], category: str) -> str:
    rating = rating_for(raw)
    positive = {
        "FASHION": "착용감과 마감이 기대보다 좋아요.",
        "ELECTRONICS": "작동이 안정적이고 사용하기 편합니다.",
        "SPORTS": "운동할 때 쓰기 좋고 내구성도 괜찮아요.",
        "BOOKS": "내용 구성이 좋아서 편하게 읽었습니다.",
        "FOOD": "맛과 구성 모두 만족스럽습니다.",
        "BEAUTY": "사용감이 부드럽고 데일리로 쓰기 좋아요.",
        "HOME": "집에서 쓰기 편하고 품질도 무난합니다.",
        "HOBBY": "취미용으로 쓰기에 만족스럽습니다.",
        "AUTO": "차량 관리용으로 실용적입니다.",
    }.get(category, "사용해보니 전반적으로 만족스럽습니다.")
    neutral = "가격 대비 무난하고 기본기는 충분한 상품입니다."
    negative = "기대했던 것보다는 아쉬운 부분이 있지만 사용은 가능합니다."
    if rating >= 4:
        return positive
    if rating == 3:
        return neutral
    return negative


def map_category(item: dict[str, Any], policy: dict[str, Any]) -> str:
    candidates = [
        item.get("main_category"),
        item.get("category"),
        item.get("categories"),
    ]
    flattened: list[str] = []
    for candidate in candidates:
        if isinstance(candidate, list):
            flattened.extend(str(value) for value in candidate)
        elif candidate:
            flattened.append(str(candidate))

    joined = " ".join(flattened).lower()
    for category, keywords in policy["category_keywords"].items():
        if any(str(keyword).lower() in joined for keyword in keywords):
            return category
    return policy.get("default_category", "HOBBY")


def extract_images(item: dict[str, Any]) -> list[str]:
    images = item.get("images") or item.get("image") or item.get("images_url")
    urls: list[str] = []

    def add(value: Any) -> None:
        if isinstance(value, str) and value.startswith(("http://", "https://")):
            urls.append(value)

    if isinstance(images, dict):
        for key in ("large", "hi_res", "hiRes", "main", "variant", "medium", "small"):
            value = images.get(key)
            if isinstance(value, list):
                for item_url in value:
                    add(item_url)
            else:
                add(value)
    elif isinstance(images, list):
        for value in images:
            if isinstance(value, dict):
                for nested in value.values():
                    if isinstance(nested, list):
                        for item_url in nested:
                            add(item_url)
                    else:
                        add(nested)
            else:
                add(value)
    else:
        add(images)

    deduped: list[str] = []
    seen: set[str] = set()
    for url in urls:
        if url not in seen:
            seen.add(url)
            deduped.append(url)
    return deduped


def price_for(source_id: str, raw_price: Any, category: str, policy: dict[str, Any]) -> int:
    if raw_price not in (None, "", "None"):
        numeric = re.sub(r"[^0-9.]", "", str(raw_price))
        if numeric:
            value = int(float(numeric) * 1300)
            return max(1000, min(value, 2_000_000))

    seed = int(re.sub(r"[^0-9a-f]", "", source_id.lower())[:6] or "123456", 16)
    rng = random.Random(seed)
    price_range = policy["price_ranges"].get(category, {"min": 5_900, "max": 299_900, "step": 100})
    return rng.randrange(int(price_range["min"]), int(price_range["max"]), int(price_range.get("step", 100)))


def quantity_for(source_id: str) -> int:
    seed = int(re.sub(r"[^0-9a-f]", "", source_id.lower())[-6:] or "654321", 16)
    rng = random.Random(seed)
    return rng.randint(20, 500)


def rating_for(raw: dict[str, Any]) -> int:
    value = raw.get("rating") or raw.get("overall") or raw.get("score") or 5
    try:
        return max(1, min(5, round(float(value))))
    except ValueError:
        return 5


def user_email_for(raw: dict[str, Any]) -> str:
    user_id = clean_text(raw.get("user_id") or raw.get("reviewerID") or "anonymous", 80)
    safe = re.sub(r"[^a-zA-Z0-9]+", "-", user_id).strip("-").lower() or "anonymous"
    return f"{safe}@reviews.keupang.local"


def iter_valid_products(dataset: DatasetPair, policy: dict[str, Any], language: str) -> Iterable[dict[str, Any]]:
    for item in open_jsonl(dataset.metadata):
        source_id = clean_text(item.get("parent_asin") or item.get("asin"), 80)
        title = clean_text(item.get("title"), 180)
        image_urls = extract_images(item)
        if not source_id or not title or not image_urls:
            continue

        category = map_category(item, policy)
        name = korean_product_name(title, category) if language == "ko" else title
        yield {
            "source_id": source_id,
            "name": name,
            "category": category,
            "price": price_for(source_id, item.get("price"), category, policy),
            "quantity": quantity_for(source_id),
            "main_image_url": image_urls[0],
            "detail_image_urls": "|".join(image_urls[1:4]),
        }


def write_products(
    datasets: list[DatasetPair],
    output_path: Path,
    max_products: int,
    policy: dict[str, Any],
    language: str,
) -> list[str]:
    source_ids: list[str] = []
    seen_source_ids: set[str] = set()
    with output_path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=[
                "source_id",
                "name",
                "category",
                "price",
                "quantity",
                "main_image_url",
                "detail_image_urls",
            ],
        )
        writer.writeheader()

        iterators = [iter(iter_valid_products(dataset, policy, language)) for dataset in datasets]
        active = [True] * len(iterators)
        while len(source_ids) < max_products and any(active):
            for index, iterator in enumerate(iterators):
                if not active[index]:
                    continue
                try:
                    row = next(iterator)
                except StopIteration:
                    active[index] = False
                    continue
                source_id = row["source_id"]
                if source_id in seen_source_ids:
                    continue
                writer.writerow(row)
                seen_source_ids.add(source_id)
                source_ids.append(source_id)
                if len(source_ids) >= max_products:
                    return source_ids

    return source_ids


def write_reviews(
    datasets: list[DatasetPair],
    output_path: Path,
    source_ids: list[str],
    max_reviews_per_product: int,
    product_categories: dict[str, str],
    language: str,
    max_review_scan: int | None,
) -> int:
    allowed = set(source_ids)
    counts: dict[str, int] = defaultdict(int)
    written = 0
    with output_path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=["source_id", "rating", "content", "user_email"])
        writer.writeheader()

        for dataset in datasets:
            if not dataset.reviews:
                continue
            scanned = 0
            for raw in open_jsonl(dataset.reviews):
                scanned += 1
                if max_review_scan and scanned > max_review_scan:
                    break
                source_id = clean_text(raw.get("parent_asin") or raw.get("asin"), 80)
                if source_id not in allowed or counts[source_id] >= max_reviews_per_product:
                    continue

                if language == "ko":
                    content = korean_review_content(raw, product_categories.get(source_id, "HOBBY"))
                else:
                    title = clean_text(raw.get("title"), 180)
                    text = clean_text(raw.get("text") or raw.get("reviewText"), 900)
                    content = f"{title}. {text}".strip(". ")
                if not content:
                    continue

                writer.writerow(
                    {
                        "source_id": source_id,
                        "rating": rating_for(raw),
                        "content": content,
                        "user_email": user_email_for(raw),
                    }
                )
                counts[source_id] += 1
                written += 1

                if len(counts) == len(allowed) and all(counts[source_id] >= max_reviews_per_product for source_id in allowed):
                    return written

    return written


def parse_dataset_pair(value: str) -> DatasetPair:
    parts = value.split(":", 1)
    if len(parts) == 1:
        return DatasetPair(metadata=Path(parts[0]), reviews=None)
    return DatasetPair(metadata=Path(parts[0]), reviews=Path(parts[1]))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Convert Amazon Reviews 2023 JSONL files to Keupang pipeline CSV files.")
    parser.add_argument("--dataset", action="append", help="Dataset pair as metadata_path[:reviews_path]. Can be repeated.")
    parser.add_argument("--metadata", type=Path, help="Amazon metadata JSONL or JSONL.GZ path. Deprecated; use --dataset.")
    parser.add_argument("--reviews", type=Path, help="Amazon reviews JSONL or JSONL.GZ path. Deprecated; use --dataset.")
    parser.add_argument("--policy", default=Path("data-pipeline/category-policy.example.json"), type=Path)
    parser.add_argument("--out-dir", default=Path("data-pipeline/imported"), type=Path)
    parser.add_argument("--max-products", default=10_000, type=int)
    parser.add_argument("--max-reviews-per-product", default=10, type=int)
    parser.add_argument("--max-review-scan", type=int, help="Maximum review rows to scan per dataset during import.")
    parser.add_argument("--language", choices=["source", "ko"], default="source")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    args.out_dir.mkdir(parents=True, exist_ok=True)
    policy = load_policy(args.policy)
    datasets = [parse_dataset_pair(value) for value in args.dataset or []]
    if args.metadata:
        datasets.append(DatasetPair(metadata=args.metadata, reviews=args.reviews))
    if not datasets:
        raise ValueError("At least one --dataset or --metadata is required.")

    products_path = args.out_dir / "products.csv"
    reviews_path = args.out_dir / "reviews.csv"

    source_ids = write_products(datasets, products_path, args.max_products, policy, args.language)
    product_categories: dict[str, str] = {}
    with products_path.open("r", encoding="utf-8-sig", newline="") as file:
        for row in csv.DictReader(file):
            product_categories[row["source_id"]] = row["category"]
    review_count = 0
    if any(dataset.reviews for dataset in datasets):
        review_count = write_reviews(
            datasets,
            reviews_path,
            source_ids,
            args.max_reviews_per_product,
            product_categories,
            args.language,
            args.max_review_scan,
        )

    print(f"Wrote {products_path} with {len(source_ids)} products")
    if any(dataset.reviews for dataset in datasets):
        print(f"Wrote {reviews_path} with {review_count} reviews")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
