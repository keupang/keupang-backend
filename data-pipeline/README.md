# Keupang Data Pipeline

Keupang 서비스에 넣을 상품 데이터를 정제하고, 이미지를 여러 크기로 변환한 뒤 S3 key 규칙에 맞춰 저장하기 위한 파이프라인입니다.

이 파이프라인은 특정 사이트를 무단 크롤링하는 도구가 아닙니다. 합법적으로 확보한 CSV/JSON/이미지 URL을 Keupang 백엔드와 프론트에서 사용하기 좋은 형태로 가공하는 것을 목표로 합니다.

## Pipeline Flow

```text
Amazon Reviews 2023 / source CSV
  -> normalize to Keupang CSV
  -> validate category / price / quantity
  -> download source images
  -> generate responsive image variants
  -> upload processed images to S3
  -> write manifest.json
  -> write seed.sql for product / stock / review databases
  -> write user_seed.sql for synthetic review users
```

## Input CSV

필수 컬럼:

| Column | Description |
| --- | --- |
| `source_id` | 원천 데이터 식별자. 중복 방지용 hash에 사용 |
| `name` | 상품명 |
| `category` | `FASHION`, `ELECTRONICS`, `SPORTS`, `BOOKS`, `FOOD`, `BEAUTY`, `HOME`, `HOBBY`, `AUTO` 중 하나 |
| `price` | 판매 가격 |
| `quantity` | 초기 재고 수량 |
| `main_image_url` | 대표 이미지 URL |
| `detail_image_urls` | 상세 이미지 URL. 여러 개면 `|`로 구분 |

샘플:

```bash
data-pipeline/samples/products.csv
```

## Amazon Reviews 2023 Import

Amazon Reviews 2023은 McAuley Lab에서 공개한 대규모 Amazon 상품/리뷰 데이터셋입니다. 쿠팡 사이트를 직접 크롤링하지 않고도 이커머스 상품, 이미지 URL, 리뷰 데이터를 확보할 수 있어 포트폴리오용 데이터 파이프라인 원천으로 사용하기 좋습니다.

데이터셋 페이지:

```text
https://amazon-reviews-2023.github.io/
https://github.com/hyp1231/AmazonReviews2023
```

원천 파일은 repository에 commit하지 않고 `data-pipeline/raw-datasets/` 아래에 둡니다.

예시:

```text
data-pipeline/raw-datasets/meta_Electronics.jsonl.gz
data-pipeline/raw-datasets/Electronics.jsonl.gz
```

Amazon Reviews 2023 metadata/review 파일을 Keupang 입력 CSV로 변환:

```bash
python amazon_reviews_2023_importer.py \
  --dataset raw-datasets/meta_Electronics.jsonl.gz:raw-datasets/Electronics.jsonl.gz \
  --out-dir imported/electronics \
  --max-products 10000 \
  --max-reviews-per-product 10
```

생성물:

```text
imported/electronics/products.csv
imported/electronics/reviews.csv
```

여러 Amazon 카테고리를 합쳐 Keupang 9개 카테고리를 채우려면 `--dataset`을 여러 번 넘깁니다.

```bash
python amazon_reviews_2023_importer.py \
  --dataset raw-datasets/meta_Amazon_Fashion.jsonl.gz:raw-datasets/Amazon_Fashion.jsonl.gz \
  --dataset raw-datasets/meta_Electronics.jsonl.gz:raw-datasets/Electronics.jsonl.gz \
  --dataset raw-datasets/meta_Sports_and_Outdoors.jsonl.gz:raw-datasets/Sports_and_Outdoors.jsonl.gz \
  --dataset raw-datasets/meta_Books.jsonl.gz:raw-datasets/Books.jsonl.gz \
  --dataset raw-datasets/meta_Grocery_and_Gourmet_Food.jsonl.gz:raw-datasets/Grocery_and_Gourmet_Food.jsonl.gz \
  --dataset raw-datasets/meta_All_Beauty.jsonl.gz:raw-datasets/All_Beauty.jsonl.gz \
  --dataset raw-datasets/meta_Home_and_Kitchen.jsonl.gz:raw-datasets/Home_and_Kitchen.jsonl.gz \
  --dataset raw-datasets/meta_Toys_and_Games.jsonl.gz:raw-datasets/Toys_and_Games.jsonl.gz \
  --dataset raw-datasets/meta_Automotive.jsonl.gz:raw-datasets/Automotive.jsonl.gz \
  --policy category-policy.example.json \
  --out-dir imported/catalog \
  --max-products 10000 \
  --max-reviews-per-product 10 \
  --max-review-scan 200000 \
  --language ko
```

카테고리 매핑과 가격 생성 범위는 `category-policy.example.json`에서 관리합니다.

```text
Amazon 원본 카테고리/메타데이터
  -> category-policy.example.json
  -> Keupang Category enum
```

처음부터 모든 파일을 받기보다 `All_Beauty`, `Electronics`처럼 1~2개 파일로 `--max-products 100` 테스트를 먼저 실행하는 것을 권장합니다. 리뷰 파일은 매우 클 수 있으므로 테스트에서는 `--max-review-scan`으로 각 dataset에서 훑을 review row 수를 제한합니다. 운영용 대량 적재에서는 값을 크게 잡거나 옵션을 제거할 수 있습니다.

CSV는 상품명에 comma가 들어갈 수 있으므로 `cut -d','`로 확인하면 컬럼이 깨져 보일 수 있습니다. 카테고리 분포는 CSV parser로 확인합니다.

```bash
python3 - <<'PY'
import csv
from collections import Counter

with open("data-pipeline/imported/catalog/products.csv", encoding="utf-8-sig", newline="") as f:
    print(Counter(row["category"] for row in csv.DictReader(f)))
PY
```

`--language ko`는 원문 리뷰를 직역하는 기능이 아니라, 한국어 서비스 화면에 어울리는 표시용 상품명/리뷰 문구를 생성하는 옵션입니다. 원천 데이터의 카테고리, 평점, 이미지 연결 관계는 유지합니다.

그 다음 기존 파이프라인 실행:

```bash
python pipeline.py \
  --input imported/catalog/products.csv \
  --reviews-csv imported/catalog/reviews.csv \
  --config config.prod.json \
  --out-dir out \
  --upload-s3
```

## S3 Key Rule

기본 key 구조:

```text
keupang/catalog-v1/products/{category}/{slug}-{hash}/main/{variant}.webp
keupang/catalog-v1/products/{category}/{slug}-{hash}/detail/{index}/{variant}.webp
```

예:

```text
keupang/catalog-v1/products/electronics/wireless-keyboard-a1b2c3d4e5/main/card.webp
keupang/catalog-v1/products/electronics/wireless-keyboard-a1b2c3d4e5/detail/1/lg.webp
```

이름 규칙을 이렇게 둔 이유:

- category 단위로 S3 객체를 찾기 쉽습니다.
- 상품명 slug와 source hash를 함께 써서 사람이 읽기 쉽고 충돌 가능성을 줄입니다.
- `main`, `detail`을 분리해서 프론트 표시 위치에 맞춰 이미지를 사용할 수 있습니다.
- `catalog-v1` 같은 prefix를 두면 나중에 전체 교체가 쉬워집니다.

## Image Variants

기본 설정은 `config.example.json`에 있습니다.

대표 이미지:

| Variant | Size |
| --- | --- |
| `thumb` | 240x240 |
| `card` | 600x600 |
| `detail` | 1000x1000 |

상세 이미지:

| Variant | Size |
| --- | --- |
| `md` | 720x720 |
| `lg` | 1200x1200 |

현재 백엔드 DB는 `product.imageUrl`, `stock_detail_image.imageUrl`처럼 단일 URL만 저장합니다. 그래서 파이프라인은 S3에는 여러 variant를 저장하지만 DB seed에는 대표 이미지 `card`, 상세 이미지 `lg`를 넣습니다.

## Seed Users and Seller

리뷰 데이터는 `review.user_email`을 가지고 있고, 리뷰 조회 시 user 서비스에서 해당 이메일의 이름을 조회합니다. 그래서 파이프라인은 `user_seed.sql`을 따로 만들어 리뷰 작성자용 데모 회원을 함께 넣을 수 있게 합니다.

재고 데이터는 `stock.seller_email`에 기본 판매자 이메일을 저장합니다.

기본 데모 판매자:

```text
email: catalog-seller@keupang.local
password: keupang1234!
```

`user_seed.sql`은 기존 user 테이블을 비우지 않고, 같은 이메일이 없을 때만 추가합니다. 데모 비밀번호는 `config.example.json`의 `seed_user_password_hash`에 BCrypt hash로 저장됩니다.

## Install

```bash
cd data-pipeline
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Local Dry Run

S3 업로드 없이 로컬 산출물만 생성합니다.

```bash
python pipeline.py \
  --input samples/products.csv \
  --config config.example.json \
  --out-dir out
```

생성물:

```text
out/raw
out/processed
out/manifest.json
out/seed.sql
out/user_seed.sql
```

## Upload to S3

AWS credential은 환경 변수나 AWS profile로 주입합니다.

```bash
export AWS_ACCESS_KEY_ID="..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_DEFAULT_REGION="ap-northeast-2"
```

업로드:

```bash
python pipeline.py \
  --input products.csv \
  --config config.prod.json \
  --out-dir out \
  --upload-s3
```

## Replace Existing Pipeline Data

기존 S3 전체를 지우는 대신 `config.prod.json`의 `s3_prefix` 아래 객체만 삭제합니다.

```bash
python pipeline.py \
  --input products.csv \
  --config config.prod.json \
  --out-dir out \
  --upload-s3 \
  --delete-prefix \
  --yes
```

주의:

- `--delete-prefix`는 `--yes`가 있어야만 실행됩니다.
- `s3_prefix`는 비워두지 마세요.
- bucket 전체 삭제가 아니라 pipeline 전용 prefix 삭제만 수행합니다.

## Load Seed SQL

리뷰 작성자와 재고 판매자 데모 회원을 먼저 넣습니다. `out/user_seed.sql`은 기존 user 테이블을 비우지 않고, 같은 이메일이 없을 때만 추가합니다.

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml exec -T mysql \
  mysql --default-character-set=utf8mb4 -uroot -p"$DB_PASSWORD" < data-pipeline/out/user_seed.sql
```

그 다음 상품, 재고, 리뷰 seed를 넣습니다.

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml exec -T mysql \
  mysql --default-character-set=utf8mb4 -uroot -p"$DB_PASSWORD" < data-pipeline/out/seed.sql
```

Jenkins에서 실행할 경우 `.env.deploy`의 `DB_PASSWORD`를 shell 환경으로 export하거나, Secret Text credential로 별도 주입하는 방식이 좋습니다.

## Next Step

현재는 seed SQL 방식입니다. 이후 고도화할 수 있는 방향:

- pipeline 전용 Docker image로 Jenkins stage 추가
- `product.imageUrl`을 `thumb/card/detail` 구조로 확장
- `stock_detail_image`에 variant metadata 추가
- 상품 수집 단계와 이미지 처리 단계를 분리
- 데이터 품질 리포트 생성
- 실패/성공 결과를 Slack 또는 Telegram으로 알림
