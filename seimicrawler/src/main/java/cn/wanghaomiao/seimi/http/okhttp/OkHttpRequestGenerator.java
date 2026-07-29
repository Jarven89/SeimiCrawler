package cn.wanghaomiao.seimi.http.okhttp;

import cn.wanghaomiao.seimi.config.SeimiConfig;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.exception.SeimiProcessExcepiton;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.http.SeimiRenderOutputType;
import cn.wanghaomiao.seimi.spring.common.CrawlerCache;
import cn.wanghaomiao.seimi.struct.CrawlerModel;
import com.alibaba.fastjson.JSON;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author SeimiMaster seimimaster@gmail.com
 * @since 2016/6/26.
 */
public class OkHttpRequestGenerator {
    public static Request.Builder getOkHttpRequesBuilder(cn.wanghaomiao.seimi.struct.Request seimiReq, CrawlerModel crawlerModel){
        BaseSeimiCrawler crawler = crawlerModel.getInstance();
        Request.Builder requestBuilder = new Request.Builder();
        if (seimiReq.isUseSeimiRender()) {
            SeimiConfig config = CrawlerCache.getConfig();
            if (config == null || StringUtils.isBlank(config.getSeimiRenderHost())) {
                throw new SeimiProcessExcepiton("SeimiRenderHost is blank.");
            }
            // SeimiRender 默认 HTTP 端口 8088；8088 非 80，故会带上端口
            String seimiRenderUrl = "http://" + config.getSeimiRenderHost() + (config.getSeimiRenderPort() != 80 ? (":" + config.getSeimiRenderPort()) : "") + "/render";
            // SeimiRender 的 /render 接收 JSON body
            Map<String, Object> renderBody = new LinkedHashMap<>();
            renderBody.put("url", seimiReq.getUrl());
            // settle_ms：loadFinished 后等待 JS 执行的毫秒数；>0 才发送，否则用 SeimiRender 默认 2000
            if (seimiReq.getSeimiRenderSettleMs() > 0) {
                renderBody.put("settle_ms", seimiReq.getSeimiRenderSettleMs());
            }
            // output：默认 html，仅当非 HTML 时显式指定
            SeimiRenderOutputType outputType = seimiReq.getSeimiRenderOutput();
            if (outputType != null && outputType.val() > SeimiRenderOutputType.HTML.val()) {
                renderBody.put("output", outputType.outputVal());
            }
            // long_poll_ms：让 HTTP 同步等待渲染结果一步到位，取爬虫配置的 HTTP 超时，且不超过 SeimiRender 上限 60000
            int longPoll = crawlerModel.getHttpTimeOut() > 0 ? Math.min(crawlerModel.getHttpTimeOut(), 60000) : 35000;
            renderBody.put("long_poll_ms", longPoll);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(renderBody));
            requestBuilder.url(seimiRenderUrl).post(requestBody).build();
        } else {
            requestBuilder.url(seimiReq.getUrl());
            requestBuilder.header("User-Agent", crawlerModel.isUseCookie() ? crawlerModel.getCurrentUA() : crawler.getUserAgent())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6");
            //自定义header
            if (!CollectionUtils.isEmpty(seimiReq.getHeader())) {
                for (Map.Entry<String,String> entry:seimiReq.getHeader().entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            if (HttpMethod.POST.equals(seimiReq.getHttpMethod())) {
                if (StringUtils.isNotBlank(seimiReq.getJsonBody())){
                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),seimiReq.getJsonBody());
                    requestBuilder.post(requestBody);
                }else {
                    FormBody.Builder formBodyBuilder = new FormBody.Builder();
                    if (seimiReq.getParams() != null) {
                        for (Map.Entry<String, String> entry : seimiReq.getParams().entrySet()) {
                            formBodyBuilder.add(entry.getKey(), entry.getValue());
                        }
                    }
                    requestBuilder.post(formBodyBuilder.build());
                }
            } else {
                String queryStr = "";
                if (seimiReq.getParams()!=null&&!seimiReq.getParams().isEmpty()){
                    queryStr += "?";
                    for (Map.Entry<String, String> entry : seimiReq.getParams().entrySet()) {
                        queryStr= queryStr+entry.getKey()+"="+entry.getValue()+"&";
                    }
                    requestBuilder.url(seimiReq.getUrl()+queryStr);
                }
                requestBuilder.get();
            }
        }
        return requestBuilder;
    }
}
