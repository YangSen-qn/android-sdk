package com.qiniu.android.http.request.httpclient;

import android.util.Log;

import com.qiniu.android.common.Constants;
import com.qiniu.android.http.ProxyConfiguration;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.http.request.IRequestClient;
import com.qiniu.android.http.request.Request;
import com.qiniu.android.utils.AsyncRun;
import com.qiniu.android.utils.StringUtils;

import com.qiniu.curl.Curl;
import com.qiniu.curl.CurlConfiguration;
import com.qiniu.curl.CurlHandlerI;
import com.qiniu.curl.CurlRequest;
import com.qiniu.curl.CurlResponse;
import com.qiniu.curl.CurlTransactionMetrics;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LibcurlHttpClient implements IRequestClient {

    public static final String JsonMime = "application/json";

    private Curl httpClient;
    private long dataHasSent;
    private CurlResponse response;
    private byte[] responseBody;
    private UploadSingleRequestMetrics metrics;

    @Override
    public void request(final Request request,
                        final boolean isAsync,
                        final ProxyConfiguration connectionProxy,
                        final RequestClientProgress progress,
                        final RequestClientCompleteHandler complete) {

        initData();

        final CurlConfiguration.Builder configurationBuilder = new CurlConfiguration.Builder();

        Log.d("", "== CURL request url:" + request.urlString + " ip:" + request.ip);

        // dns
        if (request.ip != null && request.host != null){
                CurlConfiguration.DnsResolver dnsResolver = new CurlConfiguration.DnsResolver(request.host, request.ip, request.isHttps() ? 443 : 80);
                configurationBuilder.setDnsResolverArray(new CurlConfiguration.DnsResolver[]{dnsResolver});
        }

        // proxy
        if (connectionProxy != null){
            if (connectionProxy.hostAddress != null){
                String proxy = connectionProxy.hostAddress + ":" + connectionProxy.port;
                configurationBuilder.setProxy(proxy);
            }
            if (connectionProxy.user != null && connectionProxy.password != null){
                String proxyUserPwd = connectionProxy.user + ":" + connectionProxy.password;
                configurationBuilder.setProxyUserPwd(proxyUserPwd);
            }
        }

        if (isAsync){
            AsyncRun.runInBack(new Runnable() {
                @Override
                public void run() {
                    request(configurationBuilder.build(), request, progress, complete);
                }
            });
        } else {
            request(configurationBuilder.build(), request, progress, complete);
        }
    }

    @Override
    public void cancel() {
        if (httpClient != null){
            httpClient.cancel();
        }
    }

    private void initData(){
        dataHasSent = 0;
        metrics = new UploadSingleRequestMetrics();
    }

    private static boolean hasCurlGlobalInit = false;
    private synchronized void curlGlobalInit(Curl curl){
        if (!hasCurlGlobalInit){
            hasCurlGlobalInit = true;
            curl.globalInit();
        }
    }

    private void request(CurlConfiguration curlConfiguration,
                         final Request request,
                         final RequestClientProgress progress,
                         final RequestClientCompleteHandler complete){

        String url = request.urlString;
        int method = 1;
        if (request.httpMethod.equals("Get")){
            method = 1;
        } else if (request.httpMethod.equals("POST")){
            method = 2;
        } else if (request.httpMethod.equals("PUT")){
            method = 3;
        }
        Map<String, String> header = request.allHeaders;
        byte[] body = request.httpBody;

        CurlRequest curlRequest = new CurlRequest(url, method, header, body, request.timeout);
        httpClient = new Curl();
        curlGlobalInit(httpClient);

        httpClient.request(curlRequest, curlConfiguration, new CurlHandlerI() {
            @Override
            public void receiveResponse(CurlResponse curlResponse) {
                response = curlResponse;
            }

            @Override
            public byte[] sendData(long dataLength) {
                if (request.httpBody != null){
                    long lastLength = request.httpBody.length - dataHasSent;
                    if (lastLength <= 0){
                        return new byte[0];
                    }

                    int sendLength = (int)Math.min(lastLength, dataLength);
                    byte[] sendData = Arrays.copyOfRange(request.httpBody, (int)dataHasSent, (int)dataHasSent + sendLength);
                    dataHasSent += sendLength;
                    return sendData;
                } else {
                    return new byte[0];
                }
            }

            @Override
            public void receiveData(byte[] data) {
                if (responseBody == null){
                    responseBody = data;
                } else {
                    byte[] newData = new byte[responseBody.length + data.length];
                    System.arraycopy(responseBody, 0, newData, 0, responseBody.length);
                    System.arraycopy(data, 0, newData, responseBody.length, data.length);
                    responseBody = newData;
                }
            }

            @Override
            public void completeWithError(final int errorCode, final String errorInfo) {

                AsyncRun.runInMain(new Runnable() {
                    @Override
                    public void run() {
                        if (errorCode == 0){
                            handleResponse(request, response, responseBody, complete);
                        } else {
                            handleError(request, errorCode, errorInfo, complete);
                        }
                    }
                });
            }

            @Override
            public void sendProgress(long bytesSent, final long totalBytesSent, final long totalBytesExpectedToSend) {
                AsyncRun.runInMain(new Runnable() {
                    @Override
                    public void run() {
                        if (progress != null){
                            progress.progress(totalBytesSent, totalBytesExpectedToSend);
                        }
                    }
                });
            }

            @Override
            public void receiveProgress(long bytesReceive, long totalBytesReceive, long totalBytesExpectedToReceive) {

            }

            @Override
            public void didFinishCollectingMetrics(CurlTransactionMetrics curlMetrics) {

            }

        });
    }

    private synchronized void handleError(Request request,
                                          int responseCode,
                                          String errorMsg,
                                          RequestClientCompleteHandler complete){
        if (metrics == null || metrics.response != null) {
            return;
        }

        ResponseInfo info = ResponseInfo.create(request, responseCode, null,null, errorMsg);
        metrics.response = info;

        complete.complete(info, metrics, info.response);

        releaseResource();
    }

    private synchronized void handleResponse(Request request,
                                             CurlResponse response,
                                             byte[] responseBody,
                                             RequestClientCompleteHandler complete){
        if (metrics == null || metrics.response != null) {
            return;
        }

        int statusCode = response.statusCode;

        HashMap<String, String> responseHeader = new HashMap<String, String>();
        if (response.allHeaderFields != null){
            for (String key : response.allHeaderFields.keySet()) {
                String name = key.toLowerCase();
                String value = response.allHeaderFields.get(key);
                responseHeader.put(name, value);
            }
        }

        JSONObject responseJson = null;
        String errorMessage = null;

        if (responseBody == null){
            errorMessage = "no response body";
        } else if (response.mimeType != null || !response.mimeType.equals(JsonMime)){
            String responseString = new String(responseBody);
            if (responseString.length() > 0){
                try {
                    responseJson = new JSONObject(responseString);
                } catch (Exception ignored) {}
            }
        } else {
            try {
                responseJson = buildJsonResp(responseBody);
            } catch (Exception e) {
                statusCode = ResponseInfo.PasrseError;
                errorMessage = e.getMessage();
            }
        }


        final ResponseInfo info = ResponseInfo.create(request, statusCode, responseHeader, responseJson, errorMessage);
        metrics.response = info;
        complete.complete(info, metrics, info.response);

        releaseResource();
    }

    private void releaseResource(){
        this.httpClient = null;
    }

    private static JSONObject buildJsonResp(byte[] body) throws Exception {
        String str = new String(body, Constants.UTF_8);
        // 允许 空 字符串
        if (StringUtils.isNullOrEmpty(str)) {
            return new JSONObject();
        }
        return new JSONObject(str);
    }
}
