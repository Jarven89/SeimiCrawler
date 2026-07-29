package cn.wanghaomiao.seimi.spring.boot;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * @author: github.com/zhegexiaohuozi seimimaster@gmail.com
 * @since 2018/5/8.
 */
@ConfigurationProperties(prefix="seimi.crawler")
public class CrawlerProperties implements Serializable {
    private boolean enabled;
    private String names;
    private boolean enableRedissonQueue;
    private long bloomFilterExpectedInsertions;
    private double bloomFilterFalseProbability;
    /**
     * SeimiRender host address, such as seimi.wanghaomiao.cn or 10.10.121.211
     */
    private String seimiRenderHost;

    /**
     * SeimiRender HTTP listening port (default 8088)
     */
    private int seimiRenderPort;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public boolean isEnableRedissonQueue() {
        return enableRedissonQueue;
    }

    public void setEnableRedissonQueue(boolean enableRedissonQueue) {
        this.enableRedissonQueue = enableRedissonQueue;
    }

    public long getBloomFilterExpectedInsertions() {
        return bloomFilterExpectedInsertions;
    }

    public void setBloomFilterExpectedInsertions(long bloomFilterExpectedInsertions) {
        this.bloomFilterExpectedInsertions = bloomFilterExpectedInsertions;
    }

    public double getBloomFilterFalseProbability() {
        return bloomFilterFalseProbability;
    }

    public void setBloomFilterFalseProbability(double bloomFilterFalseProbability) {
        this.bloomFilterFalseProbability = bloomFilterFalseProbability;
    }

    // ==================== SeimiRender（推荐使用）====================

    public String getSeimiRenderHost() {
        return seimiRenderHost;
    }

    public void setSeimiRenderHost(String seimiRenderHost) {
        this.seimiRenderHost = seimiRenderHost;
    }

    public int getSeimiRenderPort() {
        return seimiRenderPort;
    }

    public void setSeimiRenderPort(int seimiRenderPort) {
        this.seimiRenderPort = seimiRenderPort;
    }

    // ==================== SeimiAgent 兼容别名（已废弃，请改用 SeimiRender 系列）====================

    /**
     * 已废弃：请改用 {@link #getSeimiRenderHost()}。
     *
     * @deprecated 请使用 {@link #getSeimiRenderHost()}
     */
    @Deprecated
    public String getSeimiAgentHost() {
        return seimiRenderHost;
    }

    /**
     * 已废弃：请改用 {@link #setSeimiRenderHost(String)}。
     *
     * @deprecated 请使用 {@link #setSeimiRenderHost(String)}
     */
    @Deprecated
    public void setSeimiAgentHost(String seimiAgentHost) {
        this.seimiRenderHost = seimiAgentHost;
    }

    /**
     * 已废弃：请改用 {@link #getSeimiRenderPort()}。
     *
     * @deprecated 请使用 {@link #getSeimiRenderPort()}
     */
    @Deprecated
    public int getSeimiAgentPort() {
        return seimiRenderPort;
    }

    /**
     * 已废弃：请改用 {@link #setSeimiRenderPort(int)}。
     *
     * @deprecated 请使用 {@link #setSeimiRenderPort(int)}
     */
    @Deprecated
    public void setSeimiAgentPort(int seimiAgentPort) {
        this.seimiRenderPort = seimiAgentPort;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("enabled", enabled)
                .append("names", names)
                .append("enableRedissonQueue", enableRedissonQueue)
                .append("bloomFilterExpectedInsertions", bloomFilterExpectedInsertions)
                .append("bloomFilterFalseProbability", bloomFilterFalseProbability)
                .toString();
    }
}
