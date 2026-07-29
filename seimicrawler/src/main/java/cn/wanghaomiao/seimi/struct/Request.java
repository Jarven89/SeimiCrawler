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
package cn.wanghaomiao.seimi.struct;


import cn.wanghaomiao.seimi.annotation.validate.NotNull;
import cn.wanghaomiao.seimi.core.SeimiCrawler;
import cn.wanghaomiao.seimi.core.SeimiDownloader;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.http.SeimiAgentContentType;
import cn.wanghaomiao.seimi.http.SeimiCookie;
import cn.wanghaomiao.seimi.http.SeimiRenderOutputType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 封装一个抓取请求的基本信息体
 * @author github.com/zhegexiaohuozi seimimaster@gmail.com
 *         Date:  14-7-7.
 */
public class Request extends CommonObject {

    @FunctionalInterface
    public static interface SeimiCallbackFunc<T,A1> extends java.io.Serializable{
        void call(T t, A1 a1);
    }

    public <T,A1> Request(String url, String callBack, SeimiCallbackFunc<T,A1> cbFunc, HttpMethod httpMethod, Map<String, String> params, Map<String, Object> meta, int maxReqCount) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.params = params;
        this.meta = meta;
        this.callBack = callBack;
        this.maxReqCount = maxReqCount;
        this.callBackFunc = cbFunc;
        if (callBackFunc!=null){
            this.lambdaCb = true;
        }
    }

    public Request(String url,String callBack){
        this.url = url;
        this.callBack = callBack;
    }

    public <T,A1> Request(String url,SeimiCallbackFunc<T,A1> callBackFunc){
        this.url = url;
        this.callBackFunc = callBackFunc;
        this.lambdaCb = true;
    }


    public static <T,A1> Request build(String url, String callBack,SeimiCallbackFunc<T,A1> callBackFunc, HttpMethod httpMethod, Map<String, String> params, Map<String, Object> meta,int maxReqcount){
        return new Request(url, callBack,callBackFunc, httpMethod, params, meta, maxReqcount);
    }

    public static Request build(String url, String callBack, HttpMethod httpMethod, Map<String, String> params, Map<String, Object> meta){
        return new Request(url, callBack,null, httpMethod, params, meta,1);
    }

    public static Request build(String url, String callBack){
        return new Request(url, callBack,null,null,null,null,1);
    }

    public static <T ,A1> Request build(String url, SeimiCallbackFunc<T,A1> callBackFunc){
        return new Request(url, null, callBackFunc,null,null,null,1);
    }

    public static Request build(String url, String callBack, int maxReqCount){
        return new Request(url, callBack,null,null,null,null, maxReqCount);
    }

    public Request(){
        super();
    }

    @NotNull
    private String crawlerName;
    /**
     * 需要请求的url
     */
    @NotNull
    private String url;
    /**
     * 要请求的方法类型 get,post,put...
     */
    private HttpMethod httpMethod;
    /**
     * 如果请求需要参数，那么将参数放在这里
     */
    private Map<String,String> params;
    /**
     * 这个主要用于存储向下级回调函数传递的一些自定义数据
     */
    private Map<String,Object> meta;
    /**
     * 回调函数方法名
     */
    @NotNull
    private String callBack;

    /**
     * 回调函数是否为Lambda表达式
     */
    private transient boolean lambdaCb = false;
    /**
     * 回调函数
     */
    private transient SeimiCallbackFunc callBackFunc;
    /**
     * 是否停止的信号，收到该信号的处理线程会退出
     */
    private boolean stop = false;
    /**
     * 最大可被重新请求次数
     */
    private int maxReqCount = 3;

    /**
     * 用来记录当前请求被执行过的次数
     */
    private int currentReqCount = 0;

    /**
     * 用来指定一个请求是否要经过去重机制
     */
    private boolean skipDuplicateFilter = false;

    /**
     * 是否启用渲染后端（SeimiRender）。底层统一字段，新老两套 API（useSeimiRender / useSeimiAgent）均读写此字段以保持同步。
     */
    private boolean useRenderBackend = false;
    /**
     * 自定义Http请求协议头
     */
    private Map<String, String> header;

    /**
     * 定义 SeimiRender 的 JS settle 等待时间（loadFinished 后等待 JS 执行的毫秒数），单位毫秒。
     * 对应 SeimiRender {@code /render} 接口的 {@code settle_ms}。{@code <=0} 表示使用 SeimiRender 默认值。
     */
    private long seimiRenderSettleMs = 0;

    /**
     * 告诉 SeimiRender 将结果渲染成何种格式返回，默认 HTML
     */
    private SeimiRenderOutputType seimiRenderOutput = SeimiRenderOutputType.HTML;

    /**
     * 用于支持在渲染后端执行指定的js脚本。<br>
     * 注意：SeimiRender 暂不支持 per-request 脚本注入，该字段仅作向后兼容保留，实际不会发送给 SeimiRender。
     */
    private String seimiAgentScript;

    /**
     * 指定提交到渲染后端的请求是否使用cookie。<br>
     * 注意：SeimiRender 通过浏览器插件或 {@code /cookies} 接口统一同步登录态，不支持 per-request cookie 控制，
     * 该字段仅作向后兼容保留，实际不会发送给 SeimiRender。
     */
    private Boolean seimiAgentUseCookie;

    /**
     * 支持添加自定义cookie
     */
    private List<SeimiCookie> seimiCookies;

    /**
     * 添加json request body支持
     */
    private String jsonBody;

    /**
     * 是否指定下载器
     */
    private Class<? extends SeimiDownloader> downloader;

    public void incrReqCount(){
        this.currentReqCount +=1;
    }

    public String getUrl() {
        return url;
    }

    public Request setUrl(String url) {
        this.url = url;
        return this;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Request setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public Request setParams(Map<String, String> params) {
        this.params = params;
        return this;
    }

    public Map<String, Object> getMeta() {
        //保证用起来时可定不为空，方便使用
        if (meta == null){
            meta = new HashMap<>();
        }
        return meta;
    }

    public Request setMeta(Map<String, Object> meta) {
        this.meta = meta;
        return this;
    }

    public String getCallBack() {
        return callBack;
    }

    public Request setCallBack(String callBack) {
        this.callBack = callBack;
        return this;
    }

    public Request setCallBack(SeimiCallbackFunc<SeimiCrawler, Response> cbFunc) {
        this.callBackFunc = cbFunc;
        this.lambdaCb = true;
        return this;
    }

    public boolean isStop() {
        return stop;
    }

    public Request setStop(boolean stop) {
        this.stop = stop;
        return this;
    }

    public int getMaxReqCount() {
        return maxReqCount;
    }

    public Request setMaxReqCount(int maxReqCount) {
        this.maxReqCount = maxReqCount;
        return this;
    }

    public int getCurrentReqCount() {
        return currentReqCount;
    }

    public Request setCurrentReqCount(int currentReqCount) {
        this.currentReqCount = currentReqCount;
        return this;
    }

    public boolean isSkipDuplicateFilter() {
        return skipDuplicateFilter;
    }

    public Request setSkipDuplicateFilter(boolean skipDuplicateFilter) {
        this.skipDuplicateFilter = skipDuplicateFilter;
        return this;
    }

    public String getCrawlerName() {
        return crawlerName;
    }

    public Request setCrawlerName(String crawlerName) {
        this.crawlerName = crawlerName;
        return this;
    }

    // ==================== SeimiRender（推荐使用）====================

    /**
     * 启用 SeimiRender 渲染后端处理该请求
     */
    public Request useSeimiRender() {
        this.useRenderBackend = true;
        return this;
    }

    public Request setUseSeimiRender(boolean useSeimiRender) {
        this.useRenderBackend = useSeimiRender;
        return this;
    }

    public boolean isUseSeimiRender() {
        return useRenderBackend;
    }

    /**
     * 设置 SeimiRender 的 JS settle 等待时间（loadFinished 后等待 JS 执行的毫秒数）。{@code <=0} 表示使用 SeimiRender 默认值。
     *
     * @param seimiRenderSettleMs settle 毫秒数（0–30000）
     */
    public Request setSeimiRenderSettleMs(long seimiRenderSettleMs) {
        this.seimiRenderSettleMs = seimiRenderSettleMs;
        return this;
    }

    public long getSeimiRenderSettleMs() {
        return seimiRenderSettleMs;
    }

    public Request setSeimiRenderOutput(SeimiRenderOutputType seimiRenderOutput) {
        this.seimiRenderOutput = seimiRenderOutput;
        return this;
    }

    public SeimiRenderOutputType getSeimiRenderOutput() {
        return seimiRenderOutput;
    }

    // ==================== SeimiAgent 兼容别名（已废弃，请改用 SeimiRender 系列）====================

    /**
     * 已废弃：请改用 {@link #useSeimiRender()}。委托到渲染后端开关。
     *
     * @deprecated 请使用 {@link #useSeimiRender()}
     */
    @Deprecated
    public Request useSeimiAgent() {
        this.useRenderBackend = true;
        return this;
    }

    /**
     * 已废弃：请改用 {@link #setUseSeimiRender(boolean)}。
     *
     * @deprecated 请使用 {@link #setUseSeimiRender(boolean)}
     */
    @Deprecated
    public Request setUseSeimiAgent(boolean useSeimiAgent) {
        this.useRenderBackend = useSeimiAgent;
        return this;
    }

    /**
     * 已废弃：请改用 {@link #isUseSeimiRender()}。
     *
     * @deprecated 请使用 {@link #isUseSeimiRender()}
     */
    @Deprecated
    public boolean isUseSeimiAgent() {
        return useRenderBackend;
    }

    /**
     * 已废弃：请改用 {@link #setSeimiRenderSettleMs(long)}。内部映射到 SeimiRender 的 {@code settle_ms}。
     *
     * @deprecated 请使用 {@link #setSeimiRenderSettleMs(long)}
     */
    @Deprecated
    public Request setSeimiAgentRenderTime(long seimiAgentRenderTime) {
        this.seimiRenderSettleMs = seimiAgentRenderTime;
        return this;
    }

    /**
     * 已废弃：请改用 {@link #getSeimiRenderSettleMs()}。
     *
     * @deprecated 请使用 {@link #getSeimiRenderSettleMs()}
     */
    @Deprecated
    public long getSeimiAgentRenderTime() {
        return seimiRenderSettleMs;
    }

    public String getSeimiAgentScript() {
        return seimiAgentScript;
    }

    /**
     * 已废弃：SeimiRender 不支持 per-request 脚本注入，本字段仅作向后兼容保留，实际不会发送给 SeimiRender。
     *
     * @deprecated SeimiRender 暂不支持，设置无效
     */
    @Deprecated
    public Request setSeimiAgentScript(String seimiAgentScript) {
        this.seimiAgentScript = seimiAgentScript;
        return this;
    }

    public Boolean isSeimiAgentUseCookie() {
        return seimiAgentUseCookie;
    }

    /**
     * 已废弃：SeimiRender 通过浏览器插件 / {@code /cookies} 接口统一同步登录态，不支持 per-request cookie 控制。
     * 本字段仅作向后兼容保留，实际不会发送给 SeimiRender。
     *
     * @deprecated SeimiRender 暂不支持 per-request cookie，设置无效
     */
    @Deprecated
    public Request setSeimiAgentUseCookie(Boolean seimiAgentUseCookie) {
        this.seimiAgentUseCookie = seimiAgentUseCookie;
        return this;
    }

    /**
     * 已废弃：请改用 {@link #setSeimiRenderOutput(SeimiRenderOutputType)}。旧枚举值会被映射到对应的 SeimiRender 输出格式。
     *
     * @deprecated 请使用 {@link #setSeimiRenderOutput(SeimiRenderOutputType)}
     */
    @Deprecated
    public Request setSeimiAgentContentType(SeimiAgentContentType seimiAgentContentType) {
        if (seimiAgentContentType == null) {
            this.seimiRenderOutput = SeimiRenderOutputType.HTML;
        } else {
            switch (seimiAgentContentType) {
                case IMG:
                    this.seimiRenderOutput = SeimiRenderOutputType.IMG;
                    break;
                case PDF:
                    this.seimiRenderOutput = SeimiRenderOutputType.PDF;
                    break;
                case HTML:
                default:
                    this.seimiRenderOutput = SeimiRenderOutputType.HTML;
                    break;
            }
        }
        return this;
    }

    /**
     * 已废弃：请改用 {@link #getSeimiRenderOutput()}。
     *
     * @deprecated 请使用 {@link #getSeimiRenderOutput()}
     */
    @Deprecated
    public SeimiAgentContentType getSeimiAgentContentType() {
        if (seimiRenderOutput == null) {
            return SeimiAgentContentType.HTML;
        }
        switch (seimiRenderOutput) {
            case IMG:
                return SeimiAgentContentType.IMG;
            case PDF:
                return SeimiAgentContentType.PDF;
            case MARKDOWN:
            case HTML:
            default:
                return SeimiAgentContentType.HTML;
        }
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public Request setHeader(Map<String, String> header) {
        this.header = header;
        return this;
    }

    public List<SeimiCookie> getSeimiCookies() {
        return seimiCookies;
    }

    public Request setSeimiCookies(List<SeimiCookie> seimiCookies) {
        this.seimiCookies = seimiCookies;
        return this;
    }

    public SeimiCallbackFunc getCallBackFunc() {
        return callBackFunc;
    }

    public boolean isLambdaCb() {
        return lambdaCb;
    }

    public String getJsonBody() {
        return jsonBody;
    }

    public void setJsonBody(String jsonBody) {
        this.jsonBody = jsonBody;
    }

    public Class<? extends SeimiDownloader> getDownloader() {
        return downloader;
    }

    public void setDownloader(Class<? extends SeimiDownloader> downloader) {
        this.downloader = downloader;
    }

    public void putParam(String name, String val){
        if (this.params == null){
            params = new HashMap<>();
        }
        params.put(name, val);
    }

    public void putMeta(String key, Object val){
        if (this.meta == null){
            meta = new HashMap<>();
        }
        meta.put(key, val);
    }
}
