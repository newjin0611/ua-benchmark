# empty (톰캣, VT 자체 성능 테스트 시 사용)
http://172.16.1.132:8801/benchmark/health

# 파싱 요청
http://172.16.1.132:8801/benchmark/parse

# 성공, 실패, 캐시, 직접파싱(캐시 미스) 횟수 확인
http://172.16.1.132:8801/benchmark/stats

- yauaa 라이브러리는 자체 캐시를 사용하는 경우 hit, miss 카운팅 기능이 없어서,
  해당 기능을 사용하길 원한다면 caffeine 같은 캐시를 끼워 넣어줘야 한다

# 로컬 PC (및 테스트 서버)
```
java -Dspring.profiles.active=browscap -jar  moa-useragent-test-1.0.0.jar
java -Dspring.profiles.active=yauaa-cache -jar  moa-useragent-test-1.0.0.jar
java -Dspring.profiles.active=yauaa-no-cache -jar  moa-useragent-test-1.0.0.jar
java -Dspring.profiles.active=ua-parser -jar  moa-useragent-test-1.0.0.jar
java -Dspring.profiles.active=browscap-cache -jar  moa-useragent-test-1.0.0.jar
```