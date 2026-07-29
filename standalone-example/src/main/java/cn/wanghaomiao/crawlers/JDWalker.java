package cn.wanghaomiao.crawlers;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.SeimiRenderOutputType;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.apache.commons.lang3.StringUtils;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;
import org.seimicrawler.xpath.exception.XpathSyntaxErrorException;

import java.util.LinkedList;
import java.util.List;

/**
 * 演示通过 SeimiRender 来获取京东联盟数据。
 * <p>
 * 注意：SeimiRender 不支持 per-request 脚本注入与 cookie 控制。
 * - 登录态：请通过 SeimiRender 配套的浏览器插件或 {@code POST /cookies} 接口预先同步 cookie，之后渲染这些域名页面时 Chromium 会自动带上登录态。
 * - 代理：SeimiRender 通过服务级 {@code --proxy} 启动参数或运行时 {@code POST /proxy} 热切换统一配置。
 *
 * @author github.com/zhegexiaohuozi [seimimaster@gmail.com]
 * @since 2016/8/24.
 */
@Crawler(name = "jdWalker",httpTimeOut = 30000)
public class JDWalker extends BaseSeimiCrawler {

    @Override
    public String[] startUrls() {
        return null;
    }

    @Override
    public List<Request> startRequests() {
        List<Request> requests = new LinkedList<>();
        Request request = Request.build("https://www.jd.com/",JDWalker::start)
                .useSeimiRender()
                .setSeimiRenderSettleMs(2000)
                .setSeimiRenderOutput(SeimiRenderOutputType.HTML);
        requests.add(request);
        return requests;
    }
    @Override
    public void start(Response response) {
        JXDocument document = response.document();
        try {
            logger.info("login head name = {}", StringUtils.join(document.sel("//*[@id=\"ttbar-login-2024\"]/div[1]/a/text()"),""));
            Request request = Request.build("https://search.jd.com/Search?keyword=%E6%89%8B%E6%9C%BA",JDWalker::getProductList)
                    .useSeimiRender()
                    .setSeimiRenderSettleMs(5000)
                    .setSeimiRenderOutput(SeimiRenderOutputType.HTML);
            push(request);
        } catch (XpathSyntaxErrorException e) {
            logger.debug(e.getMessage(),e);
        }
    }
    public void getProductList(Response response){
        JXDocument jxDocument = response.document();
        try {
            List<JXNode> nodeList = jxDocument.selN("//*[@id=\"searchCenter\"]/div/div/div[3]/div[1]/div/div[7]/div/div[2]/div/div[1]/span");
            for (JXNode jxNode:nodeList){
                logger.info(jxNode.toString());
            }
        }catch (Exception e){
            logger.debug(e.getMessage(),e);
        }
    }
}
