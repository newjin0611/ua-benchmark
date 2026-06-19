/**
 * nGrinder 테스트 스크립트
 * GET /benchmark/parse + User-Agent 헤더로 요청
 */
import static net.grinder.script.Grinder.grinder
import net.grinder.script.GTest
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPPluginControl
import net.grinder.scriptengine.groovy.junit.GrinderRunner  // ✅ 추가
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess  // ✅ 추가
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread   // ✅ 추가
import org.junit.Test                                                    // ✅ 추가
import org.junit.runner.RunWith                                          // ✅ 추가
import HTTPClient.NVPair
import java.util.Random

@RunWith(GrinderRunner)  // ✅ 추가
class TestRunner {
    def static baseUrl
    def static uaList = []
    def static random = new Random()
    def static gTest

    @BeforeProcess
    public static void beforeProcess() {
        baseUrl = grinder.getProperties().getProperty("base_url", "http://172.16.1.132:8083")
        gTest   = new GTest(1, "parse")
        try {
            def file = new File("resources/ua_list.txt")
            uaList = file.readLines()
                    .findAll { it?.trim() && !it.trim().startsWith("#") }
            grinder.logger.info("UA 목록 로드 완료: ${uaList.size()}건")
        } catch (Exception e) {
            uaList = [
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 Version/17.4.1 Mobile/15E148 Safari/604.1",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/124.0.6367.82 Mobile Safari/537.36",
                    "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15 Version/17.4.1 Safari/605.1.15"
            ]
            grinder.logger.warn("ua_list.txt 없음 - 기본 UA ${uaList.size()}건 사용")
        }
    }

    @BeforeThread
    public void beforeThread() {
        HTTPPluginControl.getConnectionDefaults().timeout = 10000
    }

    @Test
    public void test() {
        def ua      = uaList[random.nextInt(uaList.size())]
        def request = new HTTPRequest()
        gTest.record(request)

        def url     = "${baseUrl}/benchmark/parse"
        def headers = [new NVPair("User-Agent", ua)] as NVPair[]
        try {
            def response = request.GET(url, new NVPair[0], headers)
            if (response.statusCode != 200) {
                grinder.logger.warn("응답 오류: ${response.statusCode}")
            }
        } catch (Exception e) {
            grinder.logger.error("요청 실패: ${e.message}")
        }
    }
}