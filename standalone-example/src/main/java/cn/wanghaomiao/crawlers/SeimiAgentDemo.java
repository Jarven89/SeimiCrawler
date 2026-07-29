package cn.wanghaomiao.crawlers;

import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.apache.commons.lang3.StringUtils;
import org.seimicrawler.xpath.JXDocument;

/**
 * 这个例子演示如何使用 SeimiRender 进行复杂动态页面信息抓取。
 * SeimiRender 是 SeimiAgent 的现代化升级版，基于 Chromium（QtWebEngine），支持 JS 渲染、SPA、动态内容，
 * 返回 html / markdown / pdf / 截图 / 搜索结构化结果，并支持 MCP 协议接入各类 AI Agent。
 * <p>
 * 独立启动需通过 {@link cn.wanghaomiao.seimi.config.SeimiConfig} 配置，spring boot 通过标准的 application.properties 来进行配置，
 * #seimi.crawler.seimi-render-host=
 * #seimi.crawler.seimi-render-port=8088
 *
 * @author 汪浩淼 et.tw@163.com
 * @since 2016/4/14.
 */
@Crawler(name = "seimiagent")
public class SeimiAgentDemo extends BaseSeimiCrawler{

    @Override
    public String[] startUrls() {
        return new String[]{"https://www.baidu.com"};
    }

    @Override
    public void start(Response response) {
        Request seimiRenderReq = Request.build("https://weibo.com/newlogin?tabtype=weibo&gid=102803&openLoginLayer=0&url=https://weibo.com/",SeimiAgentDemo::getFirstFeedNews)
                .useSeimiRender()
                // 设置 loadFinished 后给 SeimiRender 多少时间用于执行 JS 并渲染页面（settle_ms），单位为毫秒
                .setSeimiRenderSettleMs(5000);
        push(seimiRenderReq);
    }

    /**
     * 获取搜易贷首页总成交额
     * @param response
     */
    public void getFirstFeedNews(Response response){
//        logger.info(response.getContent());
        JXDocument doc = response.document();
        try {
            String trans = StringUtils.join(doc.selN("//div[1]/div[1]/div[2]/div[2]/main/div[1]/div/div[2]/div/div/div/div/div/div/div[2]/div[1]/div/article/div/div/div[1]/div[1]/html()"),"");
            logger.info("Final Res:{}",trans);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
