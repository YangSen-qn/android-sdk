//
// Created by yangsen on 2020/9/9.
//
#include "curl_native.h"
#include "curl_context.h"
#include "curl_transaction_metrics.h"
#include "curl_configuration.h"
#include "curl_jni_call_back.h"

#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include <curl/curl.h>
#include <ctime>

#ifdef ANDROID
#define TAG "CurlLibrary"
#define kCurlLogD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define kCurlLogI(...) __android_log_print(ANDROID_LOG_INFO,TAG  ,__VA_ARGS__)
#define kCurlLogW(...) __android_log_print(ANDROID_LOG_WARN,TAG  ,__VA_ARGS__)
#define kCurlLogE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)
#define kCurlLogF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__)
#else
#define kCurlLogD(...) printf(__VA_ARGS__)
#define kCurlLogI(...) printf(__VA_ARGS__)
#define kCurlLogW(...) printf(__VA_ARGS__)
#define kCurlLogE(...) printf(__VA_ARGS__)
#define kCurlLogF(...) printf(__VA_ARGS__)
#endif

#define qn_curl_easy_setopt(handle, opt, param, errorCode, errorInfo, error_desc) \
{ \
    CURLcode codeNew = curl_easy_setopt(handle,opt,param); \
    if (codeNew != CURLE_OK) { \
        *errorCode = codeNew; \
        *errorInfo = error_desc; \
        return; \
    } \
}

//-------------------------------------------- Curl filed ------------------------------------------
bool curlJavaIsCancel(struct CurlContext *curlContext){
    if (curlContext == NULL) {
        return false;
    }

    JNIEnv *env = curlContext->env;
    jobject curl = curlContext->curlObj;
    if (env == NULL || curl == NULL) {
        return false;
    }

    jclass curl_class = env->FindClass("com/qiniu/curl/Curl");
    if (curl_class == NULL) {
        return false;
    }

    jmethodID is_cancel_method = env->GetMethodID(curl_class, "isCancel", "()Z");
    jboolean isCancel = env->CallBooleanMethod(curl, is_cancel_method);
    return isCancel;
}
//--------------------------------------------- CallBack -------------------------------------------
int CurlDebugCallback(CURL *curl, curl_infotype infoType, char *info, size_t infoLen,
                      void *contextInfo) {

    const char *text;
    (void) curl; /* prevent compiler warning */
    (void) contextInfo;

    switch (infoType) {
        case CURLINFO_TEXT:
            text = "=> Text";
            fprintf(stderr, "== Info: %s", info);
            break;
        case CURLINFO_HEADER_OUT:
            text = "=> Send header";
            break;
        case CURLINFO_DATA_OUT:
            text = "=> Send data";
            break;
        case CURLINFO_SSL_DATA_OUT:
            text = "=> Send SSL data";
            break;
        case CURLINFO_HEADER_IN:
            text = "<= Recv header";
            break;
        case CURLINFO_DATA_IN:
            text = "<= Recv data";
            break;
        case CURLINFO_SSL_DATA_IN:
            text = "<= Recv SSL data";
            break;
        default: /* in case a new one is introduced to shock us */
            return 0;
    }

    kCurlLogD("%s", text);
    kCurlLogD("     %s", info);

    return 0;
}

size_t CurlReceiveHeaderCallback(char *buffer, size_t size, size_t nitems, void *userData) {
    const size_t sizeInBytes = size * nitems;
    struct CurlContext *curlContext = (struct CurlContext *) userData;
    curlContext->responseHeaderFields = curl_slist_append(curlContext->responseHeaderFields, buffer);
    kCurlLogD("====== response header:%s", buffer);
    return sizeInBytes;
}

size_t CurlReadCallback(void *ptr, size_t size, size_t nmemb, void *userData) {
    const size_t sizeInBytes = size * nmemb;
    struct CurlContext *curlContext = (struct CurlContext *) userData;
    return sendData(curlContext, (char *)ptr, sizeInBytes);;
}

size_t CurlWriteCallback(char *ptr, size_t size, size_t nmemb, void *userData) {
    const size_t sizeInBytes = size * nmemb;
    struct CurlContext *curlContext = (struct CurlContext *) userData;
    return receiveData(curlContext, ptr, sizeInBytes);
}

int CurlProgressCallback(void *client, double downloadTotal, double downloadNow, double uploadTotal, double uploadNow) {
    struct CurlContext *curlContext = (struct CurlContext *) client;

    curlContext->totalBytesExpectedToSend = uploadTotal;
    long long sendBodyLength = (long long)(uploadNow - curlContext->totalBytesSent);
    if (sendBodyLength > 0 ){
        curlContext->totalBytesSent = uploadNow;
        sendProgress(curlContext, sendBodyLength, (long long)uploadNow, (long long)uploadTotal);
    }

    curlContext->totalBytesExpectedToReceive = downloadTotal;
    long long receiveBodyLength = (long long)(downloadNow - curlContext->totalBytesReceive);
    if (receiveBodyLength > 0 ){
        curlContext->totalBytesReceive = downloadNow;
        receiveProgress(curlContext, receiveBodyLength, (long long)downloadNow, (long long)downloadTotal);
    }

    if (curlJavaIsCancel(curlContext)){
        return -999;
    } else {
        return 0;
    }
}

//--------------------------------------------- INTER ----------------------------------------------
void initCurlRequestDefaultOptions(CURL *curl, struct CurlContext *curlContext, CURLcode *errorCode,
                                   const char **errorInfo) {

    curl_easy_setopt(curl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);

    curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 10L);
    curl_easy_setopt(curl, CURLOPT_SERVER_RESPONSE_TIMEOUT, 15L);
    curl_easy_setopt(curl, CURLOPT_ACCEPTTIMEOUT_MS, 5000L);
    curl_easy_setopt(curl, CURLOPT_HAPPY_EYEBALLS_TIMEOUT_MS, 300L);
    curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 10L);

    curl_easy_setopt(curl, CURLOPT_TCP_KEEPALIVE, 1L);
    curl_easy_setopt(curl, CURLOPT_TCP_KEEPIDLE, 10L);
    curl_easy_setopt(curl, CURLOPT_TCP_KEEPINTVL, 10L);
    curl_easy_setopt(curl, CURLOPT_TCP_FASTOPEN, 1L);

    curl_easy_setopt(curl, CURLOPT_MAXCONNECTS, 0L);
    curl_easy_setopt(curl, CURLOPT_FORBID_REUSE, 1L);
    curl_easy_setopt(curl, CURLOPT_DNS_CACHE_TIMEOUT, 10L);
    curl_easy_setopt(curl, CURLOPT_HTTP_VERSION, CURL_HTTP_VERSION_2);
    //todo: CA证书配置
//  curl_easy_setopt(curl, CURLOPT_SSLVERSION, CURL_SSLVERSION_DEFAULT);
//  curl_easy_setopt(curl, CURLOPT_SSL_CIPHER_LIST, "ALL");
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);
    curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, NULL);

    curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
    curl_easy_setopt(curl, CURLOPT_DEBUGFUNCTION, CurlDebugCallback);
    curl_easy_setopt(curl, CURLOPT_DEBUGDATA, curlContext);

    qn_curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, CurlReceiveHeaderCallback, errorCode,
                        errorInfo, "header function set 0 error");
    qn_curl_easy_setopt(curl, CURLOPT_HEADERDATA, curlContext, errorCode, errorInfo,
                        "header function set 1 error");

    qn_curl_easy_setopt(curl, CURLOPT_NOPROGRESS, 0L, errorCode, errorInfo,
                        "progress function set 0 error");
    qn_curl_easy_setopt(curl, CURLOPT_PROGRESSFUNCTION, CurlProgressCallback, errorCode, errorInfo,
                        "progress function set 1 error");
    qn_curl_easy_setopt(curl, CURLOPT_PROGRESSDATA, curlContext, errorCode, errorInfo,
                        "progress function set 2 error");
}

void initCurlRequestUploadData(CURL *curl, struct CurlContext *curlContext, CURLcode *errorCode,
                               const char **errorInfo) {
    if (curlContext == NULL) {
        return;
    }
    qn_curl_easy_setopt(curl, CURLOPT_READFUNCTION, CurlReadCallback, errorCode, errorInfo,
                        "read function set 0 error");
    qn_curl_easy_setopt(curl, CURLOPT_READDATA, curlContext, errorCode, errorInfo,
                        "read function set 1 error");
}

void initCurlRequestDownloadData(CURL *curl, struct CurlContext *curlContext, CURLcode *errorCode,
                                 const char **errorInfo) {
    if (curlContext == NULL) {
        return;
    }
    qn_curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, CurlWriteCallback, errorCode, errorInfo,
                        "write function set 0 error");
    qn_curl_easy_setopt(curl, CURLOPT_WRITEDATA, curlContext, errorCode, errorInfo,
                        "write function set 1 error");
}

void initCurlRequestCustomOptions(CURL *curl, jobject configure) {
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 60);
    //todo:
    if (true) {
        curl_easy_setopt(curl, CURLOPT_PIPEWAIT, 1);
    }
}

void initCurlDnsResolver(CURL *curl, struct curl_slist *dnsResolver) {
    if (dnsResolver != NULL) {
        curl_easy_setopt(curl, CURLOPT_RESOLVE, dnsResolver);
    }
}

void initCurlRequestHeader(CURL *curl, struct curl_slist *headerList, CURLcode *errorCode,
                           const char **errorInfo) {
    if (headerList != NULL) {
        qn_curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headerList, errorCode, errorInfo,
                            "header set error");
    }
}

void initCurlRequestUrl(CURL *curl, const char *url, CURLcode *errorCode, const char **errorInfo) {
    kCurlLogD("== url:%s", url);
    if (url != NULL) {
        qn_curl_easy_setopt(curl, CURLOPT_URL, url, errorCode, errorInfo, "url set error");
    }
}

void initCurlRequestMethod(CURL *curl, long httpMethod, CURLcode *errorCode, const char **errorInfo) {
    if (httpMethod == 1) {
        qn_curl_easy_setopt(curl, CURLOPT_HTTPGET, 1L, errorCode, errorInfo,
                            "Get method set error");
    } else if (httpMethod == 2) {
        qn_curl_easy_setopt(curl, CURLOPT_POST, 1L, errorCode, errorInfo, "POST method set error");
    } else if (httpMethod == 3) {
        qn_curl_easy_setopt(curl, CURLOPT_PUT, 1L, errorCode, errorInfo, "PUT method set error");
    } else {
        *errorCode = CURLE_FAILED_INIT;
        *errorInfo = "method set error";
    }
}

void initCurlRequestProxy(CURL *curl, const char *proxy) {
    if (proxy != NULL){
        curl_easy_setopt(curl, CURLOPT_PROXY, proxy);
    }
}

void performRequest(CURL *curl, CURLcode *errorCode, const char **errorInfo) {
    char errBuffer[CURL_ERROR_SIZE];
    curl_easy_setopt(curl, CURLOPT_ERRORBUFFER, errBuffer);

    CURLcode code = curl_easy_perform(curl);
    if (code != CURLE_OK) {
        *errorInfo = "curl request perform error";
    }
    *errorCode = code;
}

void handleResponse(struct CurlContext *curlContext, CURL *curl) {
    if (curl == NULL) {
        return;
    }

    long statusCode = 0;
    long httpVersion = 0;
    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &statusCode);
    curl_easy_getinfo(curl, CURLINFO_HTTP_VERSION, &httpVersion);
    if (curlJavaIsCancel(curlContext)){
        statusCode = -999;
    }

    char const *HTTPVersion = NULL;
    if(httpVersion == CURL_HTTP_VERSION_1_0) {
        HTTPVersion = "HTTP/1.0";
    } else if(httpVersion == CURL_HTTP_VERSION_1_1) {
        HTTPVersion = "HTTP/1.1";
    } else if(httpVersion == CURL_HTTP_VERSION_2_0 ||
              httpVersion == CURL_HTTP_VERSION_2TLS ||
              httpVersion == CURL_HTTP_VERSION_2_PRIOR_KNOWLEDGE) {
        HTTPVersion = "HTTP/2";
    } else if(httpVersion == CURL_HTTP_VERSION_3) {
        HTTPVersion = "HTTP/3";
    } else {
        HTTPVersion = "";
    }
    receiveResponse(curlContext, curlContext->url, statusCode, const_cast<char *>(HTTPVersion), curlContext->responseHeaderFields);
}

void handleMetrics(struct CurlContext *curlContext, CURL *curl) {
    if (curl == NULL) {
        return;
    }

    long localPort;
    long remotePort;
    char *localIP = NULL;
    char *remoteIP = NULL;
    curl_easy_getinfo(curl, CURLINFO_LOCAL_PORT, &localPort);
    curl_easy_getinfo(curl, CURLINFO_LOCAL_IP, &localIP);
    curl_easy_getinfo(curl, CURLINFO_PRIMARY_PORT, &remotePort);
    curl_easy_getinfo(curl, CURLINFO_PRIMARY_IP, &remoteIP);

    setJavaMetricsLocalPort(curlContext, localPort);
    setJavaMetricsLocalAddress(curlContext, localIP);
    setJavaMetricsRemotePort(curlContext, remotePort);
    setJavaMetricsRemoteAddress(curlContext, remoteIP);

    curl_off_t total_time, name_lookup_time, connect_time, app_connect_time,
            pre_transfer_time, start_transfer_time, redirect_time, redirect_count;
    curl_easy_getinfo(curl, CURLINFO_TOTAL_TIME_T, &total_time);
    curl_easy_getinfo(curl, CURLINFO_NAMELOOKUP_TIME_T, &name_lookup_time);
    curl_easy_getinfo(curl, CURLINFO_CONNECT_TIME_T, &connect_time);
    curl_easy_getinfo(curl, CURLINFO_APPCONNECT_TIME_T, &app_connect_time);
    curl_easy_getinfo(curl, CURLINFO_PRETRANSFER_TIME_T, &pre_transfer_time);
    curl_easy_getinfo(curl, CURLINFO_STARTTRANSFER_TIME_T, &start_transfer_time);
    curl_easy_getinfo(curl, CURLINFO_REDIRECT_TIME_T, &redirect_time);
    curl_easy_getinfo(curl, CURLINFO_REDIRECT_COUNT, &redirect_count);

    setJavaMetricsTotalTime(curlContext, total_time);
    setJavaMetricsNameLookupTime(curlContext, name_lookup_time);
    setJavaMetricsConnectTime(curlContext, connect_time);
    setJavaMetricsAppConnectTime(curlContext, app_connect_time);
    setJavaMetricsPreTransferTime(curlContext, pre_transfer_time);
    setJavaMetricsStartTransferTime(curlContext, start_transfer_time);
    setJavaMetricsRedirectTime(curlContext, redirect_time);

    curl_off_t request_header_size, request_body_size, response_header_size, response_body_size;
    if (curlContext->requestHeaderFields != NULL) {
        struct curl_slist *next_headerField = curlContext->requestHeaderFields;
        long long size = 0;
        while (next_headerField != NULL){
            if (next_headerField->data != NULL){
                size += strlen(next_headerField->data);
            }
            next_headerField = next_headerField->next;
        }
        request_header_size = size;
    } else {
        request_header_size = 0;
    }

    curl_easy_getinfo(curl, CURLINFO_SIZE_UPLOAD_T, &request_body_size);
    curl_easy_getinfo(curl, CURLINFO_SIZE_DOWNLOAD_T, &response_body_size);
    curl_easy_getinfo(curl, CURLINFO_HEADER_SIZE, &response_header_size);

    curl_easy_getinfo(curl, CURLINFO_CONTENT_LENGTH_UPLOAD_T, &request_body_size);
    curl_easy_getinfo(curl, CURLINFO_CONTENT_LENGTH_DOWNLOAD_T, &response_body_size);

    setJavaMetricsCountOfRequestHeaderBytesSent(curlContext, request_header_size);
    setJavaMetricsCountOfRequestBodyBytesSent(curlContext, request_body_size);
    setJavaMetricsCountOfResponseHeaderBytesReceived(curlContext, response_header_size);
    setJavaMetricsCountOfResponseBodyBytesReceived(curlContext, response_body_size);

    curl_off_t protocol;
    curl_easy_getinfo(curl, CURLINFO_PROTOCOL, &protocol);

    didFinishCollectingMetrics(curlContext);
}

//---------------------------------------------- JNI -----------------------------------------------
extern "C" JNIEXPORT jlong JNICALL Java_com_qiniu_curl_Curl_globalInit(JNIEnv *env, jobject obj) {
    return curl_global_init(CURL_GLOBAL_ALL);
}


extern "C" JNIEXPORT void JNICALL Java_com_qiniu_curl_Curl_requestNative(JNIEnv *env,
                                                     jobject curlObj,
                                                     jobject curlHandler,
                                                     jobject configure,
                                                     jstring url,
                                                     jlong method,
                                                     jobjectArray header,
                                                     jbyteArray body) {

    CURLcode errorCode = CURLE_OK;
    const char *errorInfo = nullptr;
    CURLUploadInfo *uploadInfo = new CURLUploadInfo();
    if (body != NULL) {
        uploadInfo->data = *((LPBYTE *) env->GetByteArrayElements(body, 0));
        uploadInfo->bytesRead = 0;
    };

    // context
    struct CurlContext curlContext;
    curlContext.url = url;
    curlContext.env = env;
    curlContext.curlObj = curlObj;
    curlContext.curlHandler = curlHandler;
    curlContext.responseHeaderFields = NULL;
    curlContext.metrics = createJavaMetrics(&curlContext);
    struct timeval tp;
    gettimeofday(&tp, NULL);
    long int timestamp = tp.tv_sec * 1000 + tp.tv_usec / 1000;
    setJavaMetricsStartTimestamp(&curlContext, timestamp);

    //dns
    struct curl_slist *dnsResolver = getJavaCurlConfigurationDnsResolverArray(&curlContext, configure);

    //header
    struct curl_slist *headerList = NULL;
    int headSize = 0;
    if (header != NULL){
        headSize = env->GetArrayLength(header);
    }
    for (int i = 0; i < headSize; ++i) {
        jstring headerField = (jstring)env->GetObjectArrayElement(header, i);
        jboolean isCopy;
        const char *headerField_char = env->GetStringUTFChars(headerField, &isCopy);
        kCurlLogD("===== header: %s", headerField_char);
        headerList = curl_slist_append(headerList, headerField_char);
    }
    curlContext.requestHeaderFields = headerList;

    //url
    jboolean isCopy;
    const char *url_char = env->GetStringUTFChars(url, &isCopy);

    CURL *curl = curl_easy_init();
    if (curl == NULL) {
        goto curl_perform_complete;
    }

    initCurlRequestDefaultOptions(curl, &curlContext, &errorCode, reinterpret_cast<const char **>(&errorInfo));
    if (errorInfo != NULL){
        goto curl_perform_complete;
    }
    kCurlLogD("== Curl Debug: 1");
    initCurlRequestCustomOptions(curl, configure);
    initCurlRequestUploadData(curl, &curlContext, &errorCode, reinterpret_cast<const char **>(&errorInfo));
    if (errorInfo != NULL){
        goto curl_perform_complete;
    }
    kCurlLogD("== Curl Debug: 2");
    initCurlRequestDownloadData(curl, &curlContext, &errorCode, reinterpret_cast<const char **>(&errorInfo));
    if (errorInfo != NULL){
        goto curl_perform_complete;
    }
    kCurlLogD("== Curl Debug: 3");
    initCurlDnsResolver(curl, dnsResolver);

    initCurlRequestProxy(curl, getJavaCurlConfigurationProxy(&curlContext, configure));
    initCurlRequestHeader(curl, headerList, &errorCode, reinterpret_cast<const char **>(&errorInfo));
    if (errorInfo != NULL){
        goto curl_perform_complete;
    }
    kCurlLogD("== Curl Debug: 4");
    initCurlRequestUrl(curl, url_char, &errorCode, reinterpret_cast<const char **>(&errorInfo));
    if (errorInfo != NULL){
        goto curl_perform_complete;
    }
    kCurlLogD("== Curl Debug: 5");
    initCurlRequestMethod(curl, method, &errorCode, reinterpret_cast<const char **>(&errorInfo));
    if (errorInfo != NULL){
        goto curl_perform_complete;
    }
    kCurlLogD("== Curl Debug: 6");
    performRequest(curl, &errorCode, reinterpret_cast<const char **>(&errorInfo));
    if (errorInfo != NULL){
        goto curl_perform_complete;
    }
    kCurlLogD("== Curl Debug: 7");
    curl_perform_complete:
    handleResponse(&curlContext, curl);
    handleMetrics(&curlContext, curl);
    kCurlLogD("== Curl Debug: 8    error code:%d %s", errorCode, errorInfo);
    completeWithError(&curlContext, errorCode, reinterpret_cast<const char *>(&errorInfo));

    if (dnsResolver != NULL) {
        curl_slist_free_all(dnsResolver);
    }
    if (headerList != NULL) {
        curl_slist_free_all(headerList);
    }
    if (curl != NULL){
        curl_easy_cleanup(curl);
    }
}

