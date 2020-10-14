package com.qiniu.android.http.request;


import com.qiniu.android.collect.ReportItem;
import com.qiniu.android.collect.UploadInfoReporter;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.DnsPrefetcher;
//import com.qiniu.android.http.request.httpclient.LibcurlHttpClient;
import com.qiniu.android.http.request.httpclient.SystemHttpClient;
import com.qiniu.android.http.request.handler.CheckCancelHandler;
import com.qiniu.android.http.request.handler.RequestProgressHandler;
import com.qiniu.android.http.request.handler.RequestShouldRetryHandler;
import com.qiniu.android.http.metrics.UploadSingleRequestMetrics;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.util.ArrayList;


class HttpSingleRequest {

    private int currentRetryTime;
    private final Configuration config;
    private final UploadOptions uploadOption;
    private final UpToken token;
    private final UploadRequestInfo requestInfo;
    private final UploadRequestState requestState;

    private ArrayList <UploadSingleRequestMetrics> requestMetricsList;

    private IRequestClient client;

    HttpSingleRequest(Configuration config,
                      UploadOptions uploadOption,
                      UpToken token,
                      UploadRequestInfo requestInfo,
                      UploadRequestState requestState) {
        this.config = config;
        this.uploadOption = uploadOption;
        this.token = token;
        this.requestInfo = requestInfo;
        this.requestState = requestState;
        this.currentRetryTime = 1;
    }

    void request(Request request,
                 boolean isAsync,
                 boolean toSkipDns,
                 RequestShouldRetryHandler shouldRetryHandler,
                 RequestProgressHandler progressHandler,
                 RequestCompleteHandler completeHandler){
        currentRetryTime = 1;
        requestMetricsList = new ArrayList<>();
        retryRequest(request, isAsync, toSkipDns, shouldRetryHandler, progressHandler, completeHandler);
    }

    private void retryRequest(final Request request,
                              final boolean isAsync,
                              final boolean toSkipDns,
                              final RequestShouldRetryHandler shouldRetryHandler,
                              final RequestProgressHandler progressHandler,
                              final RequestCompleteHandler completeHandler){

        if (toSkipDns){
            client = new SystemHttpClient();
        } else {
            client = new SystemHttpClient();
        }

//        client = new LibcurlHttpClient();

        final CheckCancelHandler checkCancelHandler = new CheckCancelHandler() {
            @Override
            public boolean checkCancel() {
                boolean isCancelled = requestState.isUserCancel();
                if (! isCancelled && uploadOption.cancellationSignal != null) {
                    isCancelled = uploadOption.cancellationSignal.isCancelled();
                }
                return isCancelled;
            }
        };

        client.request(request, isAsync, config.proxy, new IRequestClient.RequestClientProgress() {
            @Override
            public void progress(long totalBytesWritten, long totalBytesExpectedToWrite) {
                if (checkCancelHandler.checkCancel()) {
                    requestState.setUserCancel(true);
                    if (client != null){
                        client.cancel();
                    }
                } else if (progressHandler != null){
                    progressHandler.progress(totalBytesWritten, totalBytesExpectedToWrite);
                }
            }
        }, new IRequestClient.RequestClientCompleteHandler() {
            @Override
            public void complete(ResponseInfo responseInfo, UploadSingleRequestMetrics metrics, JSONObject response) {
                if (metrics != null){
                    requestMetricsList.add(metrics);
                }
                if (shouldRetryHandler != null && shouldRetryHandler.shouldRetry(responseInfo, response)
                    && currentRetryTime < config.retryMax
                    && responseInfo.couldHostRetry()){
                    currentRetryTime += 1;

                    try {
                        Thread.sleep(config.retryInterval);
                    } catch (InterruptedException ignored) {
                    }
                    retryRequest(request, isAsync, toSkipDns, shouldRetryHandler, progressHandler, completeHandler);
                } else {
                    completeAction(request, responseInfo, response, metrics, completeHandler);
                }
            }
        });

    }


    private synchronized void completeAction(Request request,
                                             ResponseInfo responseInfo,
                                             JSONObject response,
                                             UploadSingleRequestMetrics requestMetrics,
                                             RequestCompleteHandler completeHandler) {

        if (client == null){
            return;
        }
        client = null;

        if (completeHandler != null){
            completeHandler.complete(responseInfo, requestMetricsList, response);
        }
        reportRequest(responseInfo, request, requestMetrics);
    }

    private void reportRequest(ResponseInfo responseInfo,
                               Request request,
                               UploadSingleRequestMetrics requestMetrics){

        if (!requestInfo.shouldReportRequestLog() || requestMetrics == null){
            return;
        }

        long currentTimestamp = Utils.currentTimestamp();
        ReportItem item = new ReportItem();
        item.setReport(ReportItem.LogTypeRequest, ReportItem.RequestKeyLogType);
        item.setReport((currentTimestamp/1000), ReportItem.RequestKeyUpTime);
        item.setReport(ReportItem.requestReportStatusCode(responseInfo), ReportItem.RequestKeyStatusCode);
        item.setReport(responseInfo != null ? responseInfo.reqId : null, ReportItem.RequestKeyRequestId);
        item.setReport(request != null ? request.host : null, ReportItem.RequestKeyHost);
        item.setReport(requestMetrics.remoteAddress, ReportItem.RequestKeyRemoteIp);
        item.setReport(requestMetrics.remotePort, ReportItem.RequestKeyPort);
        item.setReport(requestInfo.bucket, ReportItem.RequestKeyTargetBucket);
        item.setReport(requestInfo.key, ReportItem.RequestKeyTargetKey);
        item.setReport(requestMetrics.totalElapsedTime(), ReportItem.RequestKeyTotalElapsedTime);
        item.setReport(requestMetrics.totalDnsTime(), ReportItem.RequestKeyDnsElapsedTime);
        item.setReport(requestMetrics.totalConnectTime(), ReportItem.RequestKeyConnectElapsedTime);
        item.setReport(requestMetrics.totalSecureConnectTime(), ReportItem.RequestKeyTLSConnectElapsedTime);
        item.setReport(requestMetrics.totalRequestTime(), ReportItem.RequestKeyRequestElapsedTime);
        item.setReport(requestMetrics.totalWaitTime(), ReportItem.RequestKeyWaitElapsedTime);
        item.setReport(requestMetrics.totalWaitTime(), ReportItem.RequestKeyResponseElapsedTime);
        item.setReport(requestMetrics.totalResponseTime(), ReportItem.RequestKeyResponseElapsedTime);
        item.setReport(requestInfo.fileOffset, ReportItem.RequestKeyFileOffset);
        item.setReport(requestMetrics.bytesSend(), ReportItem.RequestKeyBytesSent);
        item.setReport(requestMetrics.totalBytes(), ReportItem.RequestKeyBytesTotal);
        item.setReport(Utils.getCurrentProcessID(), ReportItem.RequestKeyPid);
        item.setReport(Utils.getCurrentThreadID(), ReportItem.RequestKeyTid);
        item.setReport(requestInfo.targetRegionId, ReportItem.RequestKeyTargetRegionId);
        item.setReport(requestInfo.currentRegionId, ReportItem.RequestKeyCurrentRegionId);
        String errorType = ReportItem.requestReportErrorType(responseInfo);
        item.setReport(errorType, ReportItem.RequestKeyErrorType);
        String errorDesc = null;
        if (responseInfo != null && errorType != null){
            errorDesc = responseInfo.error != null ? responseInfo.error : responseInfo.message;
        }
        item.setReport(errorDesc, ReportItem.RequestKeyErrorDescription);
        item.setReport(requestInfo.requestType, ReportItem.RequestKeyUpType);
        item.setReport(Utils.systemName(), ReportItem.RequestKeyOsName);
        item.setReport(Utils.systemVersion(), ReportItem.RequestKeyOsVersion);
        item.setReport(Utils.sdkLanguage(), ReportItem.RequestKeySDKName);
        item.setReport(Utils.sdkVerion(), ReportItem.RequestKeySDKVersion);
        item.setReport(currentTimestamp, ReportItem.RequestKeyClientTime);
        item.setReport(Utils.getCurrentNetworkType(), ReportItem.RequestKeyNetworkType);
        item.setReport(Utils.getCurrentSignalStrength(), ReportItem.RequestKeySignalStrength);

        item.setReport(request.uploadServer.getSource(), ReportItem.RequestKeyPrefetchedDnsSource);
        if (request.uploadServer.getIpPrefetchedTime() != null){
            Long prefetchTime = request.uploadServer.getIpPrefetchedTime() - currentTimestamp;
            item.setReport(prefetchTime, ReportItem.RequestKeyPrefetchedBefore);
        }
        item.setReport(DnsPrefetcher.getInstance().lastPrefetchErrorMessage, ReportItem.RequestKeyPrefetchedErrorMessage);

        UploadInfoReporter.getInstance().report(item, token.token);
    }

    interface RequestCompleteHandler {
        void complete(ResponseInfo responseInfo,
                      ArrayList<UploadSingleRequestMetrics> requestMetricsList,
                      JSONObject response);
    }
}

