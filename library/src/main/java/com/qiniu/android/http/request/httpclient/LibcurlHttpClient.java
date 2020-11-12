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

import com.qiniu.library.CurlAPI.CurlAPI;
import com.qiniu.library.CurlAPI.ICurl;
import com.qiniu.library.CurlAPI.ICurlConfiguration;
import com.qiniu.library.CurlAPI.ICurlHandler;
import com.qiniu.library.CurlAPI.ICurlRequest;
import com.qiniu.library.CurlAPI.ICurlResponse;
import com.qiniu.library.CurlAPI.ICurlTransactionMetrics;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LibcurlHttpClient implements IRequestClient {

    public static final String JsonMime = "application/json";

    private ICurl httpClient;
    private long dataHasSent;
    private ICurlResponse response;
    private byte[] responseBody;
    private UploadSingleRequestMetrics metrics;

    @Override
    public void request(final Request request,
                        final boolean isAsync,
                        final ProxyConfiguration connectionProxy,
                        final RequestClientProgress progress,
                        final RequestClientCompleteHandler complete) {

        initData();

        final ICurlConfiguration.IBuilder configurationBuilder = CurlAPI.getCurlConfigurationBuilderObject();

        Log.d("", "== CURL request url:" + request.urlString + " ip:" + request.ip);

        // dns
        if (request.ip != null && request.host != null){
                ICurlConfiguration.IDnsResolver dnsResolver = CurlAPI.getCurlConfigurationDnsResolverObject();
                dnsResolver.init(request.host, request.ip, request.isHttps() ? 443 : 80);
                configurationBuilder.setDnsResolverArray(new ICurlConfiguration.IDnsResolver[]{dnsResolver});
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
    private synchronized void curlGlobalInit(ICurl curl){
        if (!hasCurlGlobalInit){
            hasCurlGlobalInit = true;
            curl.globalInit();
        }
    }

    private void request(ICurlConfiguration curlConfiguration,
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

        ICurlRequest curlRequest = CurlAPI.getCurlRequestObject();
        curlRequest.init(url, method, header, body, request.timeout);

        httpClient = CurlAPI.getCurlObject();
        curlGlobalInit(httpClient);

        httpClient.request(curlRequest, curlConfiguration, new ICurlHandler() {
            @Override
            public void receiveResponse(ICurlResponse curlResponse) {
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
            public void didFinishCollectingMetrics(ICurlTransactionMetrics curlMetrics) {

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
                                             ICurlResponse response,
                                             byte[] responseBody,
                                             RequestClientCompleteHandler complete){
        if (metrics == null || metrics.response != null) {
            return;
        }

        int statusCode = response.getStatusCode();

        HashMap<String, String> responseHeader = new HashMap<String, String>();
        if (response.getAllHeaderFields() != null){
            for (String key : response.getAllHeaderFields().keySet()) {
                String name = key.toLowerCase();
                String value = response.getAllHeaderFields().get(key);
                responseHeader.put(name, value);
            }
        }

        JSONObject responseJson = null;
        String errorMessage = null;

        if (responseBody == null){
            errorMessage = "no response body";
        } else if (response.getMimeType() != null || !response.getMimeType().equals(JsonMime)){
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
