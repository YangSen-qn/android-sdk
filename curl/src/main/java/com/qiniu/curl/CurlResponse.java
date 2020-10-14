package com.qiniu.curl;

import java.util.Map;

public class CurlResponse {

    public final String url;
    public final int statusCode;
    public final Map<String, String> allHeaderFields;
    public final String mimeType;
    public final long expectedContentLength;

    public CurlResponse(String url, int statusCode, Map<String, String> allHeaderFields, String mimeType, long expectedContentLength) {
        this.url = url;
        this.statusCode = statusCode;
        this.allHeaderFields = allHeaderFields;
        this.mimeType = mimeType;
        this.expectedContentLength = expectedContentLength;
    }
}
