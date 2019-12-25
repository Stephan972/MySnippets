package my.web.proxy;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import my.web.proxy.Protocol.Support;

public final class NoProxy implements Proxy {

    public static final Proxy INSTANCE = new NoProxy();

    private AtomicInteger successfulAttemptUse;

    private NoProxy() {
        successfulAttemptUse = new AtomicInteger(0);
    }

    @Override
    public int compareTo(Proxy o) {
        if (o != this) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public AnonimityType getAnonimityType() {
        return AnonimityType.UNKNOWN;
    }

    @Override
    public void setAnonimityType(AnonimityType anonimityType) {
        // Do nothing...
    }

    @Override
    public double getMedianLatency() {
        return 0;
    }

    @Override
    public int getSuccessfulAttemptUse() {
        return successfulAttemptUse.get();
    }

    @Override
    public String getProxyProviderName() {
        return "UNKNOWN";
    }

    @Override
    public void setProxyProviderName(String proxyProviderName) {
        // Do nothing...
    }

    @Override
    public void setLatency(String latencyInMs) {
        // Do nothing...
    }

    @Override
    public void incrementSuccessfulAttemptUse() {
        successfulAttemptUse.incrementAndGet();
    }

    @Override
    public void setLatency(double latencyInMs) {
        // Do nothing...
    }

    @Override
    public void setProtocolSupport(Protocol protocol, Support support) {
        // Do nothing...
    }

    @Override
    public boolean supports(Protocol protocol) {
        return false;
    }

    @Override
    public long getLastProtocolCheckFor(Protocol protocol) {
        return 0;
    }

    @Override
    public java.net.Proxy toJavaProxy() {
        return java.net.Proxy.NO_PROXY;
    }

    @Override
    public boolean supportsAll(Set<Protocol> detectedProtocols) {
        return false;
    }

    @Override
    public String getHost() {
        return "127.0.0.1";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        return InetSocketAddress.createUnresolved(getHost(), 0);
    }
}
