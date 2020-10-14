package com.qiniu.curl;


import java.util.ArrayList;
import java.util.Map;

public class Curl {

    private boolean isCancel = false;

    static {
        System.loadLibrary("qn-curl");
    }

    public boolean isCancel() {
        return isCancel;
    }

    public native long globalInit();

    public void request(CurlHandlerI curlHandler,
                        CurlConfiguration curlConfiguration,
                        String url,
                        long method,
                        Map<String, String> headers,
                        byte[] body){

        String[] headerArray = new String[]{};
        if (headers != null){
            ArrayList<String> headerList = new ArrayList<>();
            for (String headerKey : headers.keySet()) {
                String headerValue = headers.get(headerKey);
                String header = headerKey + ": " + headerValue;
                headerList.add(header);
            }
            headerArray = headerList.toArray(new String[]{});
        }
        CurlHandler curlHandlerReal = new CurlHandler(curlHandler);
        requestNative(curlHandlerReal, curlConfiguration, url, method, headerArray, body);
    }

    public native void requestNative(CurlHandler curlHandler,
                                     CurlConfiguration curlConfiguration,
                                     String url,
                                     long method,
                                     Object[] header,
                                     byte[] body);

    public void cancel(){
        isCancel = true;
    }

}
