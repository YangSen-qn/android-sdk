package com.qiniu.curl;

import java.util.Map;

public interface CurlHandlerI {

    void receiveResponse(CurlResponse response);
    byte[] sendData(long dataLength);
    void receiveData(byte[] data);
    void completeWithError(int errorCode, String errorInfo);
    void sendProgress(long bytesSent, long totalBytesSent, long totalBytesExpectedToSend);
    void receiveProgress(long bytesReceive, long totalBytesReceive, long totalBytesExpectedToReceive);
    void didFinishCollectingMetrics(CurlTransactionMetrics metrics);
}