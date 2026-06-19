# UA Parser Benchmark - nGrinder 테스트 가이드

## 프로젝트 구조

```
ua-benchmark/
├── pom.xml
├── src/main/java/com/benchmark/ua/
│   ├── UaBenchmarkApplication.java
│   ├── controller/
│   │   ├── BenchmarkController.java   ← 9개 REST 엔드포인트
│   │   └── ParseResult.java
│   └── service/
│       ├── UaParserService.java       ← ua-parser (uap-java)
│       ├── BrowscapService.java       ← BlueConic Browscap-Java
│       └── YauaaService.java          ← Yauaa
└── ngrinder-scripts/
    ├── ua_parse_test.groovy           ← PARSE 테스트 (메인)
    ├── ua_create_test.groovy          ← CREATE 테스트
    └── ua_list.txt                    ← UA 샘플 목록
```

---

## 1. 서버 빌드 및 실행

```bash
cd ua-benchmark

# 빌드
mvn clean package -DskipTests

# 실행 (JVM 옵션: GC 로그 포함 권장)
java -Xms2g -Xmx4g \
     -Xlog:gc*:file=gc.log:time,tags \
     -jar target/ua-benchmark-1.0.0.jar
```

> **참고**: Browscap, Yauaa는 초기화에 10~30초 소요될 수 있습니다.
> 서버 시작 후 `/benchmark/health` 로 준비 상태 확인하세요.

---

## 2. API 엔드포인트

| 파서 | CREATE | CREATE_AND_PARSE | PARSE |
|------|--------|-----------------|-------|
| ua-parser | `POST /benchmark/ua-parser/create` | `POST /benchmark/ua-parser/create-and-parse` | `POST /benchmark/ua-parser/parse` |
| browscap | `POST /benchmark/browscap/create` | `POST /benchmark/browscap/create-and-parse` | `POST /benchmark/browscap/parse` |
| yauaa | `POST /benchmark/yauaa/create` | `POST /benchmark/yauaa/create-and-parse` | `POST /benchmark/yauaa/parse` |

### 요청 예시

```bash
curl -X POST http://localhost:8080/benchmark/ua-parser/parse \
  -H "Content-Type: application/json" \
  -d '{"userAgent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0"}'
```

### 응답 예시

```json
{
  "parser": "ua-parser",
  "testType": "PARSE",
  "userAgent": "Mozilla/5.0 ...",
  "browser": "Chrome",
  "browserVersion": "124",
  "os": "Windows",
  "device": "Other",
  "elapsedNanos": 45231,
  "success": true
}
```

---

## 3. nGrinder 테스트 설정

### 준비

1. nGrinder Controller 웹 UI 접속
2. `스크립트 생성` → `ua_parse_test.groovy` 업로드
3. `ua_list.txt` 를 resources 폴더에 함께 업로드
4. 스크립트 상단 `TARGET_HOST` 수정

### 테스트 시나리오별 권장 설정

#### 시나리오 1: PARSE (가장 중요 - 실서비스와 동일)
```
스크립트 : ua_parse_test.groovy
vuser    : 50~200
Duration : 10분
```

#### 시나리오 2: CREATE (초기화 비용 측정)
```
스크립트 : ua_create_test.groovy
vuser    : 1~5 (Browscap 초기화 비용이 크므로 낮게)
Duration : 5분
```

#### 시나리오 3: 100만 건 달성 설정 예시
```
vuser    : 100
Duration : 10분 → 약 TPS 1,700 이상이면 100만 건 달성
```

### nGrinder Properties 설정 (선택)

스크립트에 커스텀 속성 전달:
```properties
base_url=http://192.168.1.100:8080
test_type=parse
```

---

## 4. 측정 지표 확인

### nGrinder 리포트
- TPS (초당 처리량)
- Mean Test Time (평균 응답시간)
- Peak TPS

### 서버 메트릭 (Prometheus/Actuator)
```
# JVM 힙 사용량
http://localhost:8080/actuator/metrics/jvm.memory.used

# Prometheus 전체 메트릭
http://localhost:8080/actuator/prometheus
```

### 주목할 차이점

| 항목 | ua-parser | Browscap | Yauaa |
|------|-----------|----------|-------|
| CREATE 속도 | 중간 | **매우 느림** (수십 초) | 느림 (수 초) |
| PARSE 속도 | 빠름 | 빠름 | 중간 |
| 메모리 사용량 | 적음 | 중간 | **많음** |
| 정확도 | 중간 | 높음 | **높음** |

---

## 5. 실서비스 UA 데이터 사용 방법

100만 건 실서비스 로그가 있다면:

```bash
# Nginx 로그에서 UA 추출
awk -F'"' '{print $6}' /var/log/nginx/access.log \
  | sort -u \
  | head -10000 > ua_list.txt

# Apache 로그에서 UA 추출
awk -F'"' 'NR%2==0{print $2}' /var/log/apache2/access.log \
  | sort -u > ua_list.txt
```

추출한 `ua_list.txt` 를 nGrinder resources에 업로드하면
실제 서비스 트래픽과 동일한 환경으로 테스트 가능합니다.
