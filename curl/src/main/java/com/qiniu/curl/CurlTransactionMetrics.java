package com.qiniu.curl;

public class CurlTransactionMetrics {

    private long countOfRequestHeaderBytesSent;
    private long countOfRequestBodyBytesSent;

    private long countOfResponseHeaderBytesReceived;
    private long countOfResponseBodyBytesReceived;

    private String localAddress;
    private long localPort;

    private String remoteAddress;
    private long remotePort;

    private long startTimestamp;
    private long nameLookupTime;
    private long connectTime;
    private long appConnectTime;
    private long preTransferTime;
    private long startTransferTime;
    private long totalTime;
    private long redirectTime;

    public long getCountOfRequestHeaderBytesSent() {
        return countOfRequestHeaderBytesSent;
    }

    public void setCountOfRequestHeaderBytesSent(long countOfRequestHeaderBytesSent) {
        this.countOfRequestHeaderBytesSent = countOfRequestHeaderBytesSent;
    }

    public long getCountOfRequestBodyBytesSent() {
        return countOfRequestBodyBytesSent;
    }

    public void setCountOfRequestBodyBytesSent(long countOfRequestBodyBytesSent) {
        this.countOfRequestBodyBytesSent = countOfRequestBodyBytesSent;
    }

    public long getCountOfResponseHeaderBytesReceived() {
        return countOfResponseHeaderBytesReceived;
    }

    public void setCountOfResponseHeaderBytesReceived(long countOfResponseHeaderBytesReceived) {
        this.countOfResponseHeaderBytesReceived = countOfResponseHeaderBytesReceived;
    }

    public long getCountOfResponseBodyBytesReceived() {
        return countOfResponseBodyBytesReceived;
    }

    public void setCountOfResponseBodyBytesReceived(long countOfResponseBodyBytesReceived) {
        this.countOfResponseBodyBytesReceived = countOfResponseBodyBytesReceived;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public long getLocalPort() {
        return localPort;
    }

    public void setLocalPort(long localPort) {
        this.localPort = localPort;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public long getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(long remotePort) {
        this.remotePort = remotePort;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getNameLookupTime() {
        return nameLookupTime;
    }

    public void setNameLookupTime(long nameLookupTime) {
        this.nameLookupTime = nameLookupTime;
    }

    public long getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(long connectTime) {
        this.connectTime = connectTime;
    }

    public long getAppConnectTime() {
        return appConnectTime;
    }

    public void setAppConnectTime(long appConnectTime) {
        this.appConnectTime = appConnectTime;
    }

    public long getPreTransferTime() {
        return preTransferTime;
    }

    public void setPreTransferTime(long preTransferTime) {
        this.preTransferTime = preTransferTime;
    }

    public long getStartTransferTime() {
        return startTransferTime;
    }

    public void setStartTransferTime(long startTransferTime) {
        this.startTransferTime = startTransferTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getRedirectTime() {
        return redirectTime;
    }

    public void setRedirectTime(long redirectTime) {
        this.redirectTime = redirectTime;
    }
}
