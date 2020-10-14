//
// Created by yangsen on 2020/9/18.
//
#include <curl/curl.h>
#include "curl_context.h"

struct curl_slist * getJavaCurlConfigurationDnsResolverArray(CurlContext *curlContext, jobject curlConfiguration){
    if (curlContext == NULL || curlConfiguration == NULL) {
        return NULL;
    }

    JNIEnv *env = curlContext->env;
    if (env == NULL) {
        return NULL;
    }

    jclass config_class = env->FindClass("com/qiniu/curl/CurlConfiguration");
    if (config_class == NULL) {
        return NULL;
    }

    jmethodID getDnsResolverArray_method = env->GetMethodID(config_class,
                                                            "getDnsResolverArray",
                                                            "()[Ljava/lang/String;");
    if (getDnsResolverArray_method == NULL) {
        return NULL;
    }
    jobjectArray dnsResolverArray = (jobjectArray)env->CallObjectMethod(curlConfiguration, getDnsResolverArray_method);

    struct curl_slist *dnsResolverList = NULL;
    int dnsResolverListSize = 0;
    if (dnsResolverArray != NULL){
        dnsResolverListSize = env->GetArrayLength(dnsResolverArray);
    }
    for (int i = 0; i < dnsResolverListSize; ++i) {
        jstring dnsResolver = (jstring)env->GetObjectArrayElement(dnsResolverArray, i);
        jboolean isCopy;
        const char *headerField_char = env->GetStringUTFChars(dnsResolver, &isCopy);
        curl_slist_append(dnsResolverList, headerField_char);
    }

    return dnsResolverList;
}

char * getJavaCurlConfigurationProxy(CurlContext *curlContext, jobject curlConfiguration){
    if (curlContext == NULL || curlConfiguration == NULL) {
        return NULL;
    }

    JNIEnv *env = curlContext->env;
    if (env == NULL) {
        return NULL;
    }

    jclass config_class = env->FindClass("com/qiniu/curl/CurlConfiguration");
    if (config_class == NULL) {
        return NULL;
    }

    jmethodID getProxy_method = env->GetMethodID(config_class,
                                                 "getProxy",
                                                 "()Ljava/lang/String;");
    if (getProxy_method == NULL) {
        return NULL;
    }
    jstring proxy = (jstring)env->CallObjectMethod(curlConfiguration, getProxy_method);
    if (proxy != NULL){
        jboolean isCopy;
        const char *proxy_char = env->GetStringUTFChars(proxy, &isCopy);
        return const_cast<char *>(proxy_char);
    } else {
        return NULL;
    }
}