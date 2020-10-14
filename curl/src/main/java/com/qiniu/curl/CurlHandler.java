package com.qiniu.curl;

import java.util.HashMap;

public class CurlHandler {

    private final CurlHandlerI curlHandler;

    public CurlHandler(CurlHandlerI curlHandler) {
        this.curlHandler = curlHandler;
    }

    void receiveResponse(String url, int statusCode, String httpVersion, Object[] headers){

        if (curlHandler != null){
            HashMap<String, String> responseHeader = new HashMap<>();
            if (headers != null){
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i] instanceof String){
                        String headerField = (String) headers[i];
                        headerField = headerField.replace(" ", "");
                        headerField = headerField.replace("\r", "");
                        headerField = headerField.replace("\n", "");

                        String[] fieldArray = headerField.split(":", 2);
                        if (fieldArray.length != 2){
                            continue;
                        }
                        String key = fieldArray[0];
                        String value = fieldArray[1];
                        responseHeader.put(key.toLowerCase(), value);
                    }
                }
            }
            String mineType = responseHeader.get("content-type");
            long contentLength = 0;
            if (responseHeader.get("content-length") != null){
                contentLength = Long.parseLong(responseHeader.get("content-length"));
            }
            CurlResponse response = new CurlResponse(url, statusCode, responseHeader, mineType, contentLength);
            curlHandler.receiveResponse(response);
        }
    }
    byte[] sendData(long dataLength){
        if (curlHandler != null){
            return curlHandler.sendData(dataLength);
        } else {
            return null;
        }
    }
    void receiveData(byte[] data){
        if (curlHandler != null){
            curlHandler.receiveData(data);
        }
    }
    void completeWithError(int errorCode, String errorInfo){
        if (curlHandler != null){
            curlHandler.completeWithError(errorCode, errorInfo);
        }
    }
    void sendProgress(long bytesSent, long totalBytesSent, long totalBytesExpectedToSend){
        if (curlHandler != null){
            curlHandler.sendProgress(bytesSent, totalBytesSent, totalBytesExpectedToSend);
        }
    }
    void receiveProgress(long bytesReceive, long totalBytesReceive, long totalBytesExpectedToReceive){
        if (curlHandler != null){
            curlHandler.receiveProgress(bytesReceive, totalBytesReceive, totalBytesExpectedToReceive);
        }
    }
    void didFinishCollectingMetrics(CurlTransactionMetrics metrics){
        if (curlHandler != null){
            curlHandler.didFinishCollectingMetrics(metrics);
        }
    }
}
