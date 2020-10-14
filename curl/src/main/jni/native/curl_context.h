//
// Created by yangsen on 2020/9/18.
//

#include <jni.h>
#include "curl/curl.h"
#ifndef CURLDEMO_CURL_CONTEXT_H
#define CURLDEMO_CURL_CONTEXT_H

struct CurlContext {

    JNIEnv *env;
    jobject curlObj;
    jstring url;
    jobject curlHandler;

    double totalBytesSent;
    double totalBytesExpectedToSend;
    double totalBytesReceive;
    double totalBytesExpectedToReceive;

    struct curl_slist *requestHeaderFields;
    struct curl_slist *responseHeaderFields;

    jobject metrics;
};

#endif //CURLDEMO_CURL_CONTEXT_H
