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
package cn.wanghaomiao.seimi.http;

/**
 * 用于指定 SeimiRender 处理请求返回内容的输出格式，对应 SeimiRender {@code /render} 接口的 {@code output} 参数。
 * <p>
 * SeimiRender 是 SeimiAgent 的现代化升级版，基于 Chromium（QtWebEngine），支持 html、markdown、pdf、截图等多种渲染输出。
 *
 * @author github.com/zhegexiaohuozi et.tw@163.com
 * @since 2.1.5
 */
public enum SeimiRenderOutputType {
    /**
     * 请求 SeimiRender 渲染返回完整 HTML（执行 JS 后的 DOM）
     */
    HTML(1, "html"),
    /**
     * 请求 SeimiRender 返回页面截图（png/jpg 二进制）
     */
    IMG(2, "screenshot"),
    /**
     * 请求 SeimiRender 返回 PDF
     */
    PDF(3, "pdf"),
    /**
     * 请求 SeimiRender 返回页面正文的 Markdown（readability/conservative 提取）
     */
    MARKDOWN(4, "markdown");

    private int val;
    private String seimiRenderOutput;

    SeimiRenderOutputType(int val, String outputStr) {
        this.val = val;
        this.seimiRenderOutput = outputStr;
    }

    public int val() {
        return this.val;
    }

    /**
     * 对应 SeimiRender {@code /render} 接口 {@code output} 参数的取值
     */
    public String outputVal() {
        return seimiRenderOutput;
    }
}
