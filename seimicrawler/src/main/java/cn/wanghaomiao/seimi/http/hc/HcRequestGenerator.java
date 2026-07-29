/*
   Copyright 2015 Wang Haomiao<seimimaster@gmail.com>

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package cn.wanghaomiao.seimi.http.hc;

import cn.wanghaomiao.seimi.config.SeimiConfig;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.exception.SeimiProcessExcepiton;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.http.SeimiRenderOutputType;
import cn.wanghaomiao.seimi.spring.common.CrawlerCache;
import cn.wanghaomiao.seimi.struct.CrawlerModel;
import cn.wanghaomiao.seimi.struct.Request;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.util.CollectionUtils;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author SeimiMaster seimimaster@gmail.com
 * @since 2016/4/14.
 */
public class HcRequestGenerator {
    public static RequestBuilder getHttpRequestBuilder(Request request, CrawlerModel crawlerModel) {
        RequestBuilder requestBuilder;
        BaseSeimiCrawler crawler = crawlerModel.getInstance();
        if (request.isUseSeimiRender()) {
            SeimiConfig config = CrawlerCache.getConfig();
            if (config == null || StringUtils.isBlank(config.getSeimiRenderHost())) {
                throw new SeimiProcessExcepiton("SeimiRenderHost is blank.");
            }
            // SeimiRender 默认 HTTP 端口 8088；8088 非 80，故会带上端口
            String seimiRenderUrl = "http://" + config.getSeimiRenderHost() + (config.getSeimiRenderPort() != 80 ? (":" + config.getSeimiRenderPort()) : "") + "/render";
            requestBuilder = RequestBuilder.post().setUri(seimiRenderUrl);
            // SeimiRender 的 /render 接收 JSON body
            Map<String, Object> renderBody = new LinkedHashMap<>();
            renderBody.put("url", request.getUrl());
            // settle_ms：loadFinished 后等待 JS 执行的毫秒数；>0 才发送，否则用 SeimiRender 默认 2000
            if (request.getSeimiRenderSettleMs() > 0) {
                renderBody.put("settle_ms", request.getSeimiRenderSettleMs());
            }
            // output：默认 html，仅当非 HTML 时显式指定
            SeimiRenderOutputType outputType = request.getSeimiRenderOutput();
            if (outputType != null && outputType.val() > SeimiRenderOutputType.HTML.val()) {
                renderBody.put("output", outputType.outputVal());
            }
            // long_poll_ms：让 HTTP 同步等待渲染结果一步到位，取爬虫配置的 HTTP 超时，且不超过 SeimiRender 上限 60000
            int longPoll = crawlerModel.getHttpTimeOut() > 0 ? Math.min(crawlerModel.getHttpTimeOut(), 60000) : 35000;
            renderBody.put("long_poll_ms", longPoll);
            requestBuilder.addHeader("Content-type", "application/json; charset=utf-8");
            requestBuilder.setEntity(new StringEntity(JSON.toJSONString(renderBody), Charset.forName("UTF-8")));
        } else {
            if (HttpMethod.POST.equals(request.getHttpMethod())) {
                requestBuilder = RequestBuilder.post().setUri(request.getUrl());
                if (StringUtils.isNotBlank(request.getJsonBody())){
                    requestBuilder.addHeader("Content-type","application/json; charset=utf-8");
                    requestBuilder.setEntity(new StringEntity(request.getJsonBody(), Charset.forName("UTF-8")));
                }else if (request.getParams() != null) {
                    List<NameValuePair> nameValuePairList = new LinkedList<>();
                    for (Map.Entry<String, String> entry : request.getParams().entrySet()) {
                        nameValuePairList.add(new BasicNameValuePair(entry.getKey(),entry.getValue()));
                    }
                    requestBuilder.setEntity(new UrlEncodedFormEntity(nameValuePairList, Charset.forName("utf8")));
                }
            } else {
                requestBuilder = RequestBuilder.get().setUri(request.getUrl());
                if (request.getParams() != null) {
                    for (Map.Entry<String, String> entry : request.getParams().entrySet()) {
                        requestBuilder.addParameter(entry.getKey(), entry.getValue());
                    }
                }
            }
            RequestConfig config = RequestConfig.custom().setProxy(crawlerModel.getProxy()).setCircularRedirectsAllowed(true).build();


            requestBuilder.setConfig(config).setHeader("User-Agent", crawlerModel.isUseCookie() ? crawlerModel.getCurrentUA() : crawler.getUserAgent());
            requestBuilder.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            requestBuilder.setHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6");
        }
        if (!CollectionUtils.isEmpty(request.getHeader())) {
            for (Map.Entry<String, String> entry : request.getHeader().entrySet()) {
                requestBuilder.setHeader(entry.getKey(), entry.getValue());
            }
        }
        return requestBuilder;
    }
}
