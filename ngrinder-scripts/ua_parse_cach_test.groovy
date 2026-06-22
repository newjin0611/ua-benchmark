/**
 * nGrinder 테스트 스크립트
 * GET /benchmark/parse + User-Agent 헤더로 요청
 */
import static net.grinder.script.Grinder.grinder
import net.grinder.script.GTest
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPPluginControl
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Test
import org.junit.runner.RunWith
import HTTPClient.NVPair
import java.util.Random
import java.util.UUID

@RunWith(GrinderRunner)
class TestRunner {
    def static baseUrl
    def static uaList = []
    def static random = new Random()
    def static gTest
    def static request   // ✅ 공유 필드로 선언

    @BeforeProcess
    public static void beforeProcess() {
        baseUrl = grinder.getProperties().getProperty("base_url", "http://172.16.1.132:8801")
        gTest   = new GTest(1, "parse")
        request = new HTTPRequest()   // ✅ 프로세스 스레드에서 생성 → <clinit> 정상 초기화
        gTest.record(request)         // ✅ record도 여기서 한 번만

        try {
            def file = new File("resources/ua_maid_list.txt")
            uaList = file.readLines()
                    .findAll { it?.trim() && !it.trim().startsWith("#") }
            grinder.logger.info("UA 목록 로드 완료: ${uaList.size()}건")
        } catch (Exception e) {
            grinder.logger.warn("ua_list.txt 없음 - 기본 UA ${uaList.size()}건 사용")
        }
    }

    @BeforeThread
    public void beforeThread() {
        HTTPPluginControl.getConnectionDefaults().timeout = 10000
    }

    @Test
    public void test() {
        def ua = uaList[random.nextInt(uaList.size())]
        // ✅ 요청마다 madid가 매번 다른 값이 되도록: {MADID} → 새 UUID 치환
        if (ua.contains("{MADID}")) {
            ua = ua.replace("{MADID}", UUID.randomUUID().toString())
        }

        def url     = "${baseUrl}/benchmark/parse"
        def headers = [new NVPair("User-Agent", ua)] as NVPair[]
        try {
            // ✅ 공유 request 재사용 (new HTTPRequest() 호출 안 함)
            def response = request.GET(url, new NVPair[0], headers)
            if (response.statusCode != 200) {
                grinder.logger.warn("응답 오류: ${response.statusCode}")
            }
        } catch (Exception e) {
            grinder.logger.error("요청 실패: ${e.message}")
        }
    }
}