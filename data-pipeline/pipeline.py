#!/usr/bin/env python3
import argparse
import csv
import hashlib
import json
import mimetypes
import os
import re
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import urlparse
from urllib.request import Request, urlopen

from PIL import Image, ImageOps


CATEGORIES = ["FASHION", "ELECTRONICS", "SPORTS", "BOOKS", "FOOD", "BEAUTY", "HOME", "HOBBY", "AUTO"]
SALE_STATES = ["ON_SALE", "OUT_OF_STOCK", "DISCONTINUED", "DELETED"]


@dataclass
class ProductRow:
    source_id: str
    name: str
    category: str
    price: int
    quantity: int
    main_image_url: str
    detail_image_urls: list[str]


@dataclass
class ReviewRow:
    source_id: str
    rating: int
    content: str
    user_email: str


@dataclass
class SeedUserRow:
    user_email: str
    user_name: str
    user_phone: str


def slugify(value: str) -> str:
    value = value.strip().lower()
    value = re.sub(r"[^a-z0-9]+", "-", value)
    value = re.sub(r"-+", "-", value).strip("-")
    return value or "item"


def short_hash(value: str) -> str:
    return hashlib.sha1(value.encode("utf-8")).hexdigest()[:10]


def load_config(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def read_rows(path: Path) -> list[ProductRow]:
    rows: list[ProductRow] = []
    with path.open("r", encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        required = {"source_id", "name", "category", "price", "quantity", "main_image_url"}
        missing = required - set(reader.fieldnames or [])
        if missing:
            raise ValueError(f"Missing required CSV columns: {', '.join(sorted(missing))}")

        for line_no, raw in enumerate(reader, start=2):
            category = (raw.get("category") or "").strip().upper()
            if category not in CATEGORIES:
                raise ValueError(f"Invalid category at line {line_no}: {category}")

            detail_urls = [
                item.strip()
                for item in (raw.get("detail_image_urls") or "").split("|")
                if item.strip()
            ]

            rows.append(
                ProductRow(
                    source_id=(raw.get("source_id") or "").strip(),
                    name=(raw.get("name") or "").strip(),
                    category=category,
                    price=int(raw.get("price") or "0"),
                    quantity=int(raw.get("quantity") or "0"),
                    main_image_url=(raw.get("main_image_url") or "").strip(),
                    detail_image_urls=detail_urls,
                )
            )

    return rows


def read_review_rows(path: Path | None) -> dict[str, list[ReviewRow]]:
    if path is None:
        return {}

    reviews: dict[str, list[ReviewRow]] = {}
    with path.open("r", encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        required = {"source_id", "rating", "content", "user_email"}
        missing = required - set(reader.fieldnames or [])
        if missing:
            raise ValueError(f"Missing required review CSV columns: {', '.join(sorted(missing))}")

        for line_no, raw in enumerate(reader, start=2):
            source_id = (raw.get("source_id") or "").strip()
            rating = int(raw.get("rating") or "0")
            if rating < 1 or rating > 5:
                raise ValueError(f"Invalid review rating at line {line_no}: {rating}")
            content = (raw.get("content") or "").strip()
            if not source_id or not content:
                continue
            reviews.setdefault(source_id, []).append(
                ReviewRow(
                    source_id=source_id,
                    rating=rating,
                    content=content,
                    user_email=(raw.get("user_email") or "anonymous@keupang.local").strip(),
                )
            )

    return reviews


def extension_from_url(url: str, content_type: str | None = None) -> str:
    if content_type:
        guessed = mimetypes.guess_extension(content_type.split(";")[0].strip())
        if guessed:
            return guessed
    suffix = Path(urlparse(url).path).suffix
    return suffix if suffix else ".bin"


def download_image(url: str, target_dir: Path, base_name: str) -> Path:
    target_dir.mkdir(parents=True, exist_ok=True)
    request = Request(url, headers={"User-Agent": "keupang-data-pipeline/1.0"})
    with urlopen(request, timeout=30) as response:
        content = response.read()
        content_type = response.headers.get("content-type")
    extension = extension_from_url(url, content_type)
    path = target_dir / f"{base_name}{extension}"
    path.write_bytes(content)
    return path


def resize_image(source: Path, target: Path, width: int, height: int, image_format: str, quality: int) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    with Image.open(source) as image:
        image = ImageOps.exif_transpose(image)
        image.thumbnail((width, height), Image.Resampling.LANCZOS)
        if image.mode not in ("RGB", "RGBA"):
            image = image.convert("RGB")
        if image_format.upper() == "WEBP":
            image.save(target, "WEBP", quality=quality, method=6)
        else:
            image.save(target, image_format.upper(), quality=quality)


def s3_url(config: dict[str, Any], key: str) -> str:
    base_url = config.get("public_base_url")
    if base_url:
        return f"{base_url.rstrip('/')}/{key}"
    return f"https://{config['bucket']}.s3.{config['region']}.amazonaws.com/{key}"


def upload_file(s3_client: Any, bucket: str, key: str, path: Path, content_type: str) -> None:
    s3_client.upload_file(
        str(path),
        bucket,
        key,
        ExtraArgs={
            "ContentType": content_type,
            "CacheControl": "public, max-age=31536000, immutable",
        },
    )


def write_failed_row(out_dir: Path, row: ProductRow, reason: str) -> None:
    path = out_dir / "failed-products.csv"
    exists = path.exists()
    with path.open("a", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=["source_id", "name", "category", "reason"])
        if not exists:
            writer.writeheader()
        writer.writerow(
            {
                "source_id": row.source_id,
                "name": row.name,
                "category": row.category,
                "reason": reason[:500],
            }
        )


def delete_prefix(s3_client: Any, bucket: str, prefix: str) -> None:
    prefix = prefix.strip("/")
    if not prefix or len(prefix) < 8:
        raise ValueError("Refusing to delete an empty or too-short S3 prefix.")

    paginator = s3_client.get_paginator("list_objects_v2")
    batch: list[dict[str, str]] = []
    deleted = 0
    for page in paginator.paginate(Bucket=bucket, Prefix=f"{prefix}/"):
        for item in page.get("Contents", []):
            batch.append({"Key": item["Key"]})
            if len(batch) == 1000:
                s3_client.delete_objects(Bucket=bucket, Delete={"Objects": batch})
                deleted += len(batch)
                batch = []
    if batch:
        s3_client.delete_objects(Bucket=bucket, Delete={"Objects": batch})
        deleted += len(batch)
    print(f"Deleted {deleted} objects under s3://{bucket}/{prefix}/")


def create_s3_client(region: str) -> Any:
    try:
        import boto3
    except ImportError as exc:
        raise RuntimeError("boto3 is required for S3 upload. Run: pip install -r data-pipeline/requirements.txt") from exc
    return boto3.client("s3", region_name=region)


def sql_quote(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def seed_user_name(email: str) -> str:
    local_part = email.split("@", 1)[0]
    tokens = [token for token in re.split(r"[^a-zA-Z0-9]+", local_part) if token]
    if not tokens:
        return "규팡 리뷰어"
    name = " ".join(token.capitalize() for token in tokens[:2])
    return f"{name} 리뷰어"


def seed_user_phone(email: str) -> str:
    digest = hashlib.sha1(email.encode("utf-8")).hexdigest()
    middle = int(digest[:4], 16) % 10000
    last = int(digest[4:8], 16) % 10000
    return f"010-{middle:04d}-{last:04d}"


def collect_seed_users(reviews_by_source_id: dict[str, list[ReviewRow]]) -> list[SeedUserRow]:
    users: list[SeedUserRow] = []
    seen: set[str] = set()
    for reviews in reviews_by_source_id.values():
        for review in reviews:
            email = review.user_email.strip().lower()
            if not email or email in seen:
                continue
            seen.add(email)
            users.append(
                SeedUserRow(
                    user_email=email,
                    user_name=seed_user_name(email),
                    user_phone=seed_user_phone(email),
                )
            )
    return users


def build_user_seed_sql(users: list[SeedUserRow], config: dict[str, Any]) -> str:
    user_db = config["db"].get("user_database")
    if not user_db:
        return ""

    seller_email = config["db"].get("seed_seller_email", "catalog-seller@keupang.local").strip().lower()
    seller_name = config["db"].get("seed_seller_name", "규팡 카탈로그 셀러")
    all_users = [SeedUserRow(seller_email, seller_name, seed_user_phone(seller_email)), *users]
    deduped_users: list[SeedUserRow] = []
    seen: set[str] = set()
    for user in all_users:
        if user.user_email in seen:
            continue
        seen.add(user.user_email)
        deduped_users.append(user)

    dummy_password = config["db"].get(
        "seed_user_password_hash",
        "$2y$10$9SH3PF.gcplcuLcADZlVwO9lXY2W/KoM/j3sumxbnwvkUC77fJ3Gu",
    )
    lines: list[str] = [
        "-- Generated by data-pipeline/pipeline.py",
        "-- Inserts synthetic users for imported review emails without deleting existing users.",
        "-- Default demo password: keupang1234!",
        "SET NAMES utf8mb4;",
        "SET CHARACTER SET utf8mb4;",
        f"USE `{user_db}`;",
    ]

    for user in deduped_users:
        email = sql_quote(user.user_email)
        lines.append(
            "INSERT INTO `user` (`user_email`, `user_password`, `user_name`, `user_phone`, `role`) "
            f"SELECT {email}, {sql_quote(dummy_password)}, {sql_quote(user.user_name)}, {sql_quote(user.user_phone)}, 0 "
            f"WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `user_email` = {email});"
        )

    return "\n".join(lines) + "\n"


def build_seed_sql(
    records: list[dict[str, Any]],
    config: dict[str, Any],
    reviews_by_source_id: dict[str, list[ReviewRow]] | None = None,
) -> str:
    product_db = config["db"]["product_database"]
    stock_db = config["db"]["stock_database"]
    review_db = config["db"].get("review_database")
    seller_email = config["db"].get("seed_seller_email", "catalog-seller@keupang.local")
    truncate = bool(config["db"].get("truncate_before_insert", True))
    reviews_by_source_id = reviews_by_source_id or {}
    lines: list[str] = []
    lines.append("-- Generated by data-pipeline/pipeline.py")
    lines.append("SET NAMES utf8mb4;")
    lines.append("SET CHARACTER SET utf8mb4;")
    lines.append("SET FOREIGN_KEY_CHECKS = 0;")

    if truncate:
        lines.extend(
            [
                f"USE `{stock_db}`;",
                "TRUNCATE TABLE `stock_detail_image`;",
                "TRUNCATE TABLE `stock`;",
                f"USE `{product_db}`;",
                "TRUNCATE TABLE `product`;",
            ]
        )
        if review_db:
            lines.extend(
                [
                    f"USE `{review_db}`;",
                    "TRUNCATE TABLE `review`;",
                ]
            )

    lines.append(f"USE `{product_db}`;")
    for index, record in enumerate(records, start=1):
        product = record["product"]
        category_ordinal = CATEGORIES.index(product["category"])
        lines.append(
            "INSERT INTO `product` (`id`, `category`, `image_url`, `name`) VALUES "
            f"({index}, {category_ordinal}, {sql_quote(product['image_url'])}, {sql_quote(product['name'])});"
        )

    lines.append(f"USE `{stock_db}`;")
    for index, record in enumerate(records, start=1):
        stock = record["stock"]
        sale_state_ordinal = SALE_STATES.index("ON_SALE")
        lines.append(
            "INSERT INTO `stock` (`id`, `created_at`, `price`, `product_id`, `quantity`, `sale_state`, `sales`, `seller_email`) VALUES "
            f"({index}, NOW(), {stock['price']}, {index}, {stock['quantity']}, {sale_state_ordinal}, 0, {sql_quote(seller_email)});"
        )
        for image_url in stock["detail_image_urls"]:
            lines.append(
                "INSERT INTO `stock_detail_image` (`image_url`, `stock_id`) VALUES "
                f"({sql_quote(image_url)}, {index});"
            )

    if review_db:
        lines.append(f"USE `{review_db}`;")
        for product_id, record in enumerate(records, start=1):
            for review in reviews_by_source_id.get(record["source_id"], []):
                lines.append(
                    "INSERT INTO `review` (`content`, `created_at`, `likes`, `product_id`, `rating`, `user_email`) VALUES "
                    f"({sql_quote(review.content)}, NOW(), 0, {product_id}, {review.rating}, {sql_quote(review.user_email)});"
                )

    lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    return "\n".join(lines) + "\n"


def process(config: dict[str, Any], input_path: Path, out_dir: Path, upload_s3: bool) -> list[dict[str, Any]]:
    rows = read_rows(input_path)
    image_format = config["image"].get("format", "WEBP").upper()
    quality = int(config["image"].get("quality", 82))
    extension = "webp" if image_format == "WEBP" else image_format.lower()
    bucket = config["bucket"]
    prefix = config["s3_prefix"].strip("/")
    s3_client = create_s3_client(config["region"]) if upload_s3 else None
    records: list[dict[str, Any]] = []

    raw_dir = out_dir / "raw"
    processed_dir = out_dir / "processed"

    for index, row in enumerate(rows, start=1):
        item_id = f"{slugify(row.name)}-{short_hash(row.source_id or row.name)}"
        item_prefix = f"{prefix}/products/{row.category.lower()}/{item_id}"
        local_item_dir = processed_dir / row.category.lower() / item_id
        raw_item_dir = raw_dir / row.category.lower() / item_id

        try:
            main_source = download_image(row.main_image_url, raw_item_dir / "main", "original")
            main_urls: dict[str, str] = {}
            for variant_name, size in config["image"]["variants"]["main"].items():
                target = local_item_dir / "main" / f"{variant_name}.{extension}"
                resize_image(main_source, target, int(size["width"]), int(size["height"]), image_format, quality)
                key = f"{item_prefix}/main/{variant_name}.{extension}"
                if upload_s3 and s3_client:
                    upload_file(s3_client, bucket, key, target, f"image/{extension}")
                main_urls[variant_name] = s3_url(config, key)

            detail_urls_for_db: list[str] = []
            detail_manifest: list[dict[str, Any]] = []
            for detail_index, detail_url in enumerate(row.detail_image_urls, start=1):
                try:
                    detail_source = download_image(detail_url, raw_item_dir / "detail" / str(detail_index), "original")
                    variant_urls: dict[str, str] = {}
                    for variant_name, size in config["image"]["variants"]["detail"].items():
                        target = local_item_dir / "detail" / str(detail_index) / f"{variant_name}.{extension}"
                        resize_image(detail_source, target, int(size["width"]), int(size["height"]), image_format, quality)
                        key = f"{item_prefix}/detail/{detail_index}/{variant_name}.{extension}"
                        if upload_s3 and s3_client:
                            upload_file(s3_client, bucket, key, target, f"image/{extension}")
                        variant_urls[variant_name] = s3_url(config, key)

                    db_variant = "lg" if "lg" in variant_urls else next(iter(variant_urls))
                    detail_urls_for_db.append(variant_urls[db_variant])
                    detail_manifest.append({"source_url": detail_url, "variants": variant_urls})
                except Exception as exc:
                    print(f"Skipped detail image for source_id={row.source_id}: {exc}", file=sys.stderr)

            db_main_variant = "card" if "card" in main_urls else next(iter(main_urls))
            records.append(
                {
                    "source_id": row.source_id,
                    "s3_prefix": item_prefix,
                    "product": {
                        "name": row.name,
                        "category": row.category,
                        "image_url": main_urls[db_main_variant],
                        "image_variants": main_urls,
                    },
                    "stock": {
                        "price": row.price,
                        "quantity": row.quantity,
                        "detail_image_urls": detail_urls_for_db,
                        "detail_image_variants": detail_manifest,
                    },
                }
            )
            if index % 100 == 0:
                print(f"Processed {index}/{len(rows)} rows; kept {len(records)} products")
        except Exception as exc:
            write_failed_row(out_dir, row, str(exc))
            print(f"Skipped product source_id={row.source_id}: {exc}", file=sys.stderr)

    return records


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build Keupang product data assets and seed SQL.")
    parser.add_argument("--input", required=True, type=Path, help="Input CSV path.")
    parser.add_argument("--config", required=True, type=Path, help="Pipeline JSON config path.")
    parser.add_argument("--out-dir", default=Path("data-pipeline/out"), type=Path, help="Output directory.")
    parser.add_argument("--reviews-csv", type=Path, help="Optional review CSV path.")
    parser.add_argument("--upload-s3", action="store_true", help="Upload processed image variants to S3.")
    parser.add_argument("--delete-prefix", action="store_true", help="Delete configured S3 prefix before upload.")
    parser.add_argument("--yes", action="store_true", help="Required with --delete-prefix.")
    parser.add_argument("--clean", action="store_true", help="Delete output directory before running.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    config = load_config(args.config)

    if args.clean and args.out_dir.exists():
        shutil.rmtree(args.out_dir)
    args.out_dir.mkdir(parents=True, exist_ok=True)

    if args.delete_prefix:
        if not args.upload_s3:
            raise ValueError("--delete-prefix requires --upload-s3")
        if not args.yes:
            raise ValueError("--delete-prefix requires --yes")
        s3_client = create_s3_client(config["region"])
        delete_prefix(s3_client, config["bucket"], config["s3_prefix"])

    records = process(config, args.input, args.out_dir, args.upload_s3)
    reviews_by_source_id = read_review_rows(args.reviews_csv)

    manifest_path = args.out_dir / "manifest.json"
    manifest_path.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")

    sql_path = args.out_dir / "seed.sql"
    sql_path.write_text(build_seed_sql(records, config, reviews_by_source_id), encoding="utf-8")
    users = collect_seed_users(reviews_by_source_id)
    user_seed_sql = build_user_seed_sql(users, config)
    if user_seed_sql:
        user_sql_path = args.out_dir / "user_seed.sql"
        user_sql_path.write_text(user_seed_sql, encoding="utf-8")
        seller_email = config["db"].get("seed_seller_email", "catalog-seller@keupang.local").strip().lower()
        user_count = len({user.user_email for user in users} | {seller_email})
        print(f"Wrote {user_sql_path} with {user_count} synthetic users")

    print(f"Wrote {manifest_path}")
    print(f"Wrote {sql_path}")
    print(f"Processed {len(records)} products")
    return 0


if __name__ == "__main__":
    sys.exit(main())
