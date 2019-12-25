package my.web.proxy;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationRuntimeException;
import my.web.proxy.Protocol.Support;

// FIXME: Why constructors are public and the factory method from exists ?
@Slf4j
public class DefaultProxy implements Proxy {
    private static final String LOCALHOST_ADDRESS = "127.0.0.1";

    private java.net.Proxy javaProxy;

    @Getter
    @Setter
    private AnonimityType anonimityType;

    @Getter
    private double medianLatency;
    private List<Double> observedLatencies;

    @Getter
    private int successfulAttemptUse;

    @Getter
    @Setter
    private String proxyProviderName;

    private Map<Protocol, Long> lastProtocolsChecks;

    private Map<Protocol, Protocol.Support> supportedProtocols;

    private ReadWriteLock proxyModificationLock;

    /**
     * 
     * Create an HTTP proxy;
     * 
     * @param host
     * @param port
     */
    public DefaultProxy(String host, int port) {
        this(Type.HTTP, host, port);
    }

    /**
     * 
     * Create a HTTP proxy
     * 
     * @param host
     * @param port
     */
    public DefaultProxy(Type type, String host, int port) {
        this(type, InetSocketAddress.createUnresolved(host, port));
    }

    public DefaultProxy(Type type, SocketAddress sa) {
        this(new java.net.Proxy(type, sa));
    }

    private DefaultProxy(java.net.Proxy proxy) {
        javaProxy = proxy;
        anonimityType = AnonimityType.UNKNOWN;
        observedLatencies = new ArrayList<>();
        medianLatency = 0;
        successfulAttemptUse = 0;
        proxyProviderName = "UNKNOWN";

        lastProtocolsChecks = new EnumMap<>(Protocol.class);
        supportedProtocols = new EnumMap<>(Protocol.class);

        for (Protocol prot : Protocol.values()) {
            lastProtocolsChecks.put(prot, 0L);
            supportedProtocols.put(prot, Support.UNKNOWN);
        }

        proxyModificationLock = new ReentrantReadWriteLock();
    }

    public void setLatency(String latencyInMs) {
        setLatency(parse(latencyInMs, "Invalid latency value"));
    }

    public void setLatency(long latency, TimeUnit timeUnit) {
        setLatency(timeUnit.toMillis(latency));
    }

    public void incrementSuccessfulAttemptUse() {
        lockWrite();
        try {
            successfulAttemptUse++;
        } finally {
            unlockWrite();
        }
    }

    public void setLatency(double latencyInMs) {
        lockWrite();
        try {
            observedLatencies.add(latencyInMs);
            Collections.sort(observedLatencies);
            int total = observedLatencies.size();

            switch (total) {
            case 0:
                throw new ApplicationRuntimeException("Strange thing happened. Observed latency is empty but it is supposed to have at least one element.");

            case 1:
                medianLatency = latencyInMs;
                break;

            case 2:
                medianLatency = mean(observedLatencies.get(0), observedLatencies.get(1));
                break;

            default:
                medianLatency = calculateMedian(total, observedLatencies);
            }
        } finally {
            unlockWrite();
        }
    }

    private double calculateMedian(int total, List<Double> values) {
        lockRead();
        try {
            double median;

            if (total % 2 == 0) {
                int tmp = total / 2;
                median = mean(values.get(tmp - 1), values.get(tmp));
            } else {
                median = values.get((total - 1) / 2);
            }

            return median;
        } finally {
            unlockRead();
        }
    }

    public static Proxy from(String host, String port) {
        return new DefaultProxy( //
                host, //
                parse(port, "Invalid provided port") //
        );
    }

    public static Proxy from(Type type, String host, String port) {
        return new DefaultProxy( //
                type, //
                host, //
                parse(port, "Invalid provided port") //
        );
    }

    private static int parse(String potentialIntegerValue, String message) {
        try {
            return Integer.parseInt(potentialIntegerValue);
        } catch (NumberFormatException nfe) {
            throw new ApplicationRuntimeException(message, nfe);
        }
    }

    private static double mean(double a, double b) {
        return (a + b) / 2d;
    }

    /**
     * 
     * Compare two proxies.
     * 
     */
    @Override
    public int compareTo(Proxy other) {
        lockRead();
        try {
            if (other == null) {
                return -1;
            }

            if (other == this) {
                return 0;
            }

            // Compare proxy type (java.net.Type.DIRECT is the highest in order,
            // java.net.Type.HTTP and java.net.Type.SOCKS are equal)
            java.net.Proxy thisJavaProxy = toJavaProxy();
            java.net.Proxy otherJavaProxy = other.toJavaProxy();
            if ((thisJavaProxy.type() == Type.DIRECT) && (otherJavaProxy.type() != Type.DIRECT)) {
                return -1;
            }

            if ((thisJavaProxy.type() != Type.DIRECT) && (otherJavaProxy.type() == Type.DIRECT)) {
                return 1;
            }

            // Compare lexicographically the inet socket adress
            InetSocketAddress thisInetSocketAdress = getInetSocketAddress();
            InetSocketAddress otherInetSocketAdress = other.getInetSocketAddress();

            Optional<InetAddress> optionalThisInetAddress = toInetAdress(thisInetSocketAdress.getHostString());
            Optional<InetAddress> optionalOtherInetAddress = toInetAdress(otherInetSocketAdress.getHostString());

            if (!optionalThisInetAddress.isPresent() && optionalOtherInetAddress.isPresent()) {
                return -1;
            }

            if (optionalThisInetAddress.isPresent() && !optionalOtherInetAddress.isPresent()) {
                return 1;
            }

            if (optionalThisInetAddress.isPresent() && optionalOtherInetAddress.isPresent()) {
                int comparisonResult = compareInetAdresses(optionalThisInetAddress.get(), optionalOtherInetAddress.get());
                if (comparisonResult != 0) {
                    return comparisonResult;
                }
            }

            // Compare ports
            return thisInetSocketAdress.getPort() - otherInetSocketAdress.getPort();
        } finally {
            unlockRead();
        }
    }

    /**
     * 
     * From https://stackoverflow.com/a/34441987/363573
     * 
     * TODO: Should we consider IP country for comparison?
     * 
     * @param a
     * @param b
     * @return
     */
    private static int compareInetAdresses(InetAddress a, InetAddress b) {
        byte[] aOctets = a.getAddress();
        byte[] bOctets = b.getAddress();
        int len = Math.max(aOctets.length, bOctets.length);

        for (int i = 0; i < len; i++) {
            byte aOctet = (i >= len - aOctets.length) ? aOctets[i - (len - aOctets.length)] : 0;
            byte bOctet = (i >= len - bOctets.length) ? bOctets[i - (len - bOctets.length)] : 0;
            if (aOctet != bOctet)
                return (0xff & aOctet) - (0xff & bOctet);
        }

        return 0;
    }

    // Inspired by https://stackoverflow.com/a/34441987/363573
    private static Optional<InetAddress> toInetAdress(String hostString) {
        try {
            return Optional.of(InetAddress.getByName(hostString));
        } catch (UnknownHostException e) {
            log.info("Unable to convert hostString into InetAdress: " + hostString, e);
            return Optional.empty();
        }
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        lockRead();
        try {
            SocketAddress sa = toJavaProxy().address();
            InetSocketAddress inetSocketAddress;

            if (sa instanceof InetSocketAddress) {
                inetSocketAddress = (InetSocketAddress) sa;
            } else {
                inetSocketAddress = InetSocketAddress.createUnresolved(LOCALHOST_ADDRESS, 0);
            }

            return inetSocketAddress;
        } finally {
            unlockRead();
        }
    }

    @Override
    public boolean equals(Object other) {
        lockRead();
        try {
            boolean ret = false;

            if (other instanceof Proxy) {
                ret = this.compareTo((Proxy) other) == 0;
            }

            return ret;
        } finally {
            unlockRead();
        }
    }

    @Override
    public int hashCode() {
        lockRead();
        try {
            return toJavaProxy().hashCode();
        } finally {
            unlockRead();
        }
    }

    @Override
    public String toString() {
        lockRead();
        try {
            return getProxyProviderName() + " / " + toJavaProxy().toString() + String.format(Locale.US, "(%s, %.3f)", getAnonimityType(), getMedianLatency());
        } finally {
            unlockRead();
        }
    }

    public void setProtocolSupport(Protocol protocol, Support support) {
        lockWrite();
        try {
            supportedProtocols.put(protocol, support);
            lastProtocolsChecks.put(protocol, ProxyClock.now());
        } finally {
            unlockWrite();
        }
    }

    public boolean supports(Protocol protocol) {
        lockRead();
        try {
            return supportedProtocols.get(protocol) == Support.OK;
        } finally {
            unlockRead();
        }
    }

    public long getLastProtocolCheckFor(Protocol protocol) {
        lockRead();
        try {
            Long lastCheck = lastProtocolsChecks.get(protocol);

            return lastCheck == null ? 0L : lastCheck;
        } finally {
            unlockRead();
        }
    }

    public java.net.Proxy toJavaProxy() {
        lockRead();
        try {
            return javaProxy;
        } finally {
            unlockRead();
        }
    }

    public boolean supportsAll(Set<Protocol> detectedProtocols) {
        lockRead();
        try {
            boolean ret = true;

            for (Protocol protocol : detectedProtocols) {
                if (!supports(protocol)) {
                    ret = false;
                    break;
                }
            }

            return ret;
        } finally {
            unlockRead();
        }
    }

    public String getHost() {
        lockRead();
        try {
            return getInetSocketAddress().getHostName();
        } finally {
            unlockRead();
        }
    }

    public int getPort() {
        lockRead();
        try {
            return getInetSocketAddress().getPort();
        } finally {
            unlockRead();
        }
    }

    private void lockRead() {
        proxyModificationLock.readLock().lock();
    }

    private void unlockRead() {
        proxyModificationLock.readLock().unlock();
    }

    private void lockWrite() {
        proxyModificationLock.writeLock().lock();
    }

    private void unlockWrite() {
        proxyModificationLock.writeLock().unlock();
    }
}
