package my.web.proxy;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import my.web.proxy.Protocol.Support;

/**
 * 
 * Implementations MUST be thread safe.
 * 
 *
 */
public interface Proxy extends Comparable<Proxy> {   
    AnonimityType getAnonimityType();

    void setAnonimityType(AnonimityType anonimityType);

    double getMedianLatency();

    int getSuccessfulAttemptUse();

    String getProxyProviderName();

    void setProxyProviderName(String proxyProviderName);

    void setLatency(String latencyInMs);

    default void setLatency(long latency, TimeUnit timeUnit) {
        setLatency(timeUnit.toMillis(latency));
    }

    void incrementSuccessfulAttemptUse();

    void setLatency(double latencyInMs);

    void setProtocolSupport(Protocol protocol, Support support);

    boolean supports(Protocol protocol);

    long getLastProtocolCheckFor(Protocol protocol);

    java.net.Proxy toJavaProxy();

    boolean supportsAll(Set<Protocol> detectedProtocols);

    String getHost();

    int getPort();

    /**
     * 
     * @return A non null (mandatory) InetSocketAddress.
     */
    InetSocketAddress getInetSocketAddress();
}
