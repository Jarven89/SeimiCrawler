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
package cn.wanghaomiao.seimi.core;

import cn.wanghaomiao.seimi.annotation.Interceptor;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.SeimiHttpType;
import cn.wanghaomiao.seimi.http.hc.HcDownloader;
import cn.wanghaomiao.seimi.http.okhttp.OkHttpDownloader;
import cn.wanghaomiao.seimi.struct.BodyType;
import cn.wanghaomiao.seimi.struct.CrawlerModel;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import cn.wanghaomiao.seimi.utils.ClazzUtils;
import cn.wanghaomiao.seimi.utils.StructValidator;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2015/8/21.
 */
public class SeimiProcessor implements Runnable {
    private SeimiQueue queue;
    private List<SeimiInterceptor> interceptors;
    private CrawlerModel crawlerModel;
    private BaseSeimiCrawler crawler;
    private Logger logger = LoggerFactory.getLogger(getClass());

    public SeimiProcessor(List<SeimiInterceptor> interceptors, CrawlerModel crawlerModel) {
        this.queue = crawlerModel.getQueueInstance();
        this.interceptors = interceptors;
        this.crawlerModel = crawlerModel;
        this.crawler = crawlerModel.getInstance();
    }

    private Pattern metaRefresh = Pattern.compile("<(?:META|meta|Meta)\\s+(?:HTTP-EQUIV|http-equiv)\\s*=\\s*\"refresh\".*(?:url|URL)=(\\S*)\".*/?>");

    @Override
    public void run() {
        while (true) {
            Request request = null;
            try {
                request = queue.bPop(crawlerModel.getCrawlerName());
                if (request == null) {
                    continue;
                }
                if (crawlerModel == null) {
                    logger.error("No such crawler name:'{}'", request.getCrawlerName());
                    continue;
                }
                if (request.isStop()) {
                    logger.info("SeimiProcessor[{}] will stop!", Thread.currentThread().getName());
                    break;
                }
                //对请求开始校验
                if (!StructValidator.validateAnno(request)) {
                    logger.warn("Request={} is illegal", JSON.toJSONString(request));
                    continue;
                }
                if (!StructValidator.validateAllowRules(crawler.allowRules(), request.getUrl())) {
                    logger.warn("Request={} will be dropped by allowRules=[{}]", JSON.toJSONString(request), StringUtils.join(crawler.allowRules(), ","));
                    continue;
                }
                if (StructValidator.validateDenyRules(crawler.denyRules(), request.getUrl())) {
                    logger.warn("Request={} will be dropped by denyRules=[{}]", JSON.toJSONString(request), StringUtils.join(crawler.denyRules(), ","));
                    continue;
                }
                //异常请求重试次数超过最大重试次数三次后，直接放弃处理
                if (request.getCurrentReqCount() >= request.getMaxReqCount()+3) {
                    continue;
                }

                SeimiDownloader downloader;
                if (SeimiHttpType.APACHE_HC.val() == crawlerModel.getSeimiHttpType().val()) {
                    downloader = new HcDownloader(crawlerModel);
                } else {
                    downloader = new OkHttpDownloader(crawlerModel);
                }
                //支持针对单个请求指定下载器
                if (request.getDownloader()!=null){
                    ClazzUtils.setCurrentCModel(crawlerModel);
                    downloader = ClazzUtils.getInstance(request.getDownloader());
                }

                Response seimiResponse = downloader.process(request);
                // SeimiRender 的 /render 返回 JSON（{state,html,markdown,...}），需将渲染产物从 JSON 中解出，
                // 否则后续基于 XPath 的回调会拿到 JSON 而非真实页面内容。
                if (request.isUseSeimiRender() && StringUtils.isNotBlank(seimiResponse.getContent()) && BodyType.TEXT.equals(seimiResponse.getBodyType())) {
                    try {
                        JSONObject renderResult = JSON.parseObject(seimiResponse.getContent());
                        if (renderResult != null) {
                            String state = renderResult.getString("state");
                            if ("failed".equalsIgnoreCase(state)) {
                                // 渲染失败：保留原始 JSON 以便排错，并记录 error 信息
                                logger.error("SeimiRender render failed, url={}, task_id={}, error={}",
                                        request.getUrl(), renderResult.getString("task_id"), renderResult.getString("error"));
                            } else {
                                // succeeded / running：优先取 html，其次 markdown；都没有则保持原样
                                String html = renderResult.getString("html");
                                if (StringUtils.isBlank(html)) {
                                    html = renderResult.getString("markdown");
                                }
                                if (StringUtils.isNotBlank(html)) {
                                    seimiResponse.setContent(html);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 非 JSON 响应（兼容某些异常场景）时保持原内容不变，仅记录 debug
                        logger.debug("SeimiRender response is not valid JSON, keep raw content, url={}", request.getUrl());
                    }
                }
                if (StringUtils.isNotBlank(seimiResponse.getContent()) && BodyType.TEXT.equals(seimiResponse.getBodyType())) {
                    Matcher mm = metaRefresh.matcher(seimiResponse.getContent());
                    int refreshCount = 0;
                    while (!request.isUseSeimiRender() && mm.find() && refreshCount < 3) {
                        String nextUrl = mm.group(1).replaceAll("'", "");
                        seimiResponse = downloader.metaRefresh(nextUrl);
                        mm = metaRefresh.matcher(seimiResponse.getContent());
                        refreshCount += 1;
                    }
                }
                //处理回调函数
                if (!request.isLambdaCb()){
                    doCallback(request, seimiResponse);
                }else {
                    doLambdaCallback(request, seimiResponse);
                }
                logger.debug("Crawler[{}] ,url={} ,responseStatus={}", crawlerModel.getCrawlerName(), request.getUrl(), downloader.statusCode());
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                if (request == null) {
                    continue;
                }
                if (request.getCurrentReqCount() < request.getMaxReqCount()) {
                    request.incrReqCount();
                    queue.push(request);
                    logger.info("Request process error,req will go into queue again,url={},maxReqCount={},currentReqCount={}", request.getUrl(), request.getMaxReqCount(), request.getCurrentReqCount());
                } else if (request.getCurrentReqCount() >= request.getMaxReqCount() && request.getMaxReqCount() > 0) {
                    crawler.handleErrorRequest(request);
                }

            }
        }
    }

    private String resolveLambdaCallbackName(Request request, Request.SeimiCallbackFunc<?, ?> callback) {
        // 优先通过 SerializedLambda 解析，可获取完整的 类::方法 格式
        try {
            Method writeReplace = callback.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(callback);
            String implMethodName = serializedLambda.getImplMethodName();
            // getImplClass() 返回内部斜杠格式，某些 JDK 版本对方法引用可能为空，此时回退到 capturingClass
            String implClass = serializedLambda.getImplClass();
            if (implClass == null || implClass.isEmpty()) {
                implClass = serializedLambda.getCapturingClass().replace('/', '.');
            } else {
                implClass = implClass.replace('/', '.');
            }
            return implClass + "::" + implMethodName;
        } catch (Exception e) {
            logger.debug("Failed to resolve lambda callback name via SerializedLambda: {}", e.getMessage());
        }
        // 降级：尝试使用 request 中保存的回调方法名
        String methodName = request.getCallBack();
        if (methodName != null && !methodName.isEmpty()) {
            return methodName;
        }
        return callback.toString();
    }

    private void doCallback(Request request, Response seimiResponse) throws Exception {

        Method requestCallback = crawlerModel.getMemberMethods().get(request.getCallBack());
        if (requestCallback == null) {
            logger.info("can not find callback function");
            return;
        }
        for (SeimiInterceptor interceptor : interceptors) {
            Interceptor interAnno = interceptor.getClass().getAnnotation(Interceptor.class);
            if (interAnno.everyMethod() || requestCallback.isAnnotationPresent(interceptor.getTargetAnnotationClass()) || crawlerModel.getClazz().isAnnotationPresent(interceptor.getTargetAnnotationClass())) {
                interceptor.before(requestCallback, seimiResponse);
            }
        }
        if (crawlerModel.getDelay() > 0) {
            TimeUnit.SECONDS.sleep(crawlerModel.getDelay());
        }
        requestCallback.invoke(crawlerModel.getInstance(), seimiResponse);

        for (SeimiInterceptor interceptor : interceptors) {
            Interceptor interAnno = interceptor.getClass().getAnnotation(Interceptor.class);
            if (interAnno.everyMethod() || requestCallback.isAnnotationPresent(interceptor.getTargetAnnotationClass()) || crawlerModel.getClazz().isAnnotationPresent(interceptor.getTargetAnnotationClass())) {
                interceptor.after(requestCallback, seimiResponse);
            }
        }
    }

    private void doLambdaCallback(Request request, Response seimiResponse) throws Exception {
        Request.SeimiCallbackFunc<SeimiCrawler,Response> requestCallback = request.getCallBackFunc();
        if (requestCallback == null) {
            logger.info("can not find callback function");
            return;
        }
        logger.debug("Request URL: {}, Callback: {}", request.getUrl(), resolveLambdaCallbackName(request, requestCallback));
        for (SeimiInterceptor interceptor : interceptors) {
            Interceptor interAnno = interceptor.getClass().getAnnotation(Interceptor.class);
            if (interAnno.everyMethod()) {
                interceptor.before(null, seimiResponse);
            }
        }
        if (crawlerModel.getDelay() > 0) {
            TimeUnit.SECONDS.sleep(crawlerModel.getDelay());
        }
        requestCallback.call(crawler,seimiResponse);
        for (SeimiInterceptor interceptor : interceptors) {
            Interceptor interAnno = interceptor.getClass().getAnnotation(Interceptor.class);
            if (interAnno.everyMethod() ) {
                interceptor.after(null, seimiResponse);
            }
        }
    }
}
