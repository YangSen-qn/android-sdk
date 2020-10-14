//
// Created by yangsen on 2020/9/18.
//
#include <jni.h>
#include "curl_jni_call_back.h"
#include "curl_context.h"

void receiveResponse(CurlContext *curlContext, jstring url, int statusCode, char *httPVersion,
                     struct curl_slist *headerFields) {
    if (curlContext == NULL) {
        return;
    }

    jobject curlHandler = curlContext->curlHandler;
    JNIEnv *env = curlContext->env;
    if (env == NULL || curlHandler == NULL) {
        return;
    }

    jclass handler_class = env->FindClass("com/qiniu/curl/CurlHandler");
    if (handler_class == NULL) {
        return;
    }

    jmethodID receiveResponse_method = env->GetMethodID(handler_class,
                                                        "receiveResponse",
                                                        "(Ljava/lang/String;ILjava/lang/String;[Ljava/lang/Object;)V");
    if (receiveResponse_method == NULL) {
        return;
    }

    jstring httPVersion_string = NULL;
    jobjectArray headerFieldArray = NULL;

    if (httPVersion != NULL){
        httPVersion_string = env->NewStringUTF(httPVersion);
    }
    if (headerFields != NULL && headerFields->data != NULL){
        jsize  size = 0;
        struct curl_slist *next_field = headerFields;
        while (next_field != NULL){
            size += 1;
            next_field = next_field->next;
        }

        jstring first = env->NewStringUTF(headerFields->data);
        headerFieldArray = env->NewObjectArray(size, env->FindClass("java/lang/String"), first);

        int index = 0;
        struct curl_slist *headerField = headerFields;
        while (headerField != NULL && index < size){

            jstring value = env->NewStringUTF(headerField->data);
            env->SetObjectArrayElement(headerFieldArray, index, value);
            index ++;
            headerField = headerField->next;
        }
    }

    env->CallVoidMethod(curlHandler, receiveResponse_method, url, statusCode, httPVersion_string, headerFieldArray);
}

size_t sendData(struct CurlContext *curlContext, char *buffer, long long dataLength) {
    if (curlContext == NULL) {
        return static_cast<size_t>(-1);
    }

    jobject curlHandler = curlContext->curlHandler;
    JNIEnv *env = curlContext->env;
    if (env == NULL || curlHandler == NULL) {
        return static_cast<size_t>(-1);
    }

    jclass handler_class = env->FindClass("com/qiniu/curl/CurlHandler");
    if (handler_class == NULL) {
        return static_cast<size_t>(-1);
    }

    jmethodID sendData_method = env->GetMethodID(handler_class,
                                                 "sendData",
                                                 "(J)[B");
    if (sendData_method == NULL) {
        return static_cast<size_t>(-1);
    }

    jbyteArray data = static_cast<jbyteArray>(env->CallObjectMethod(curlHandler, sendData_method, dataLength));
    if (data != NULL){
        long length = static_cast<size_t>(env->GetArrayLength(data));
        env->GetByteArrayRegion(data, 0, length, reinterpret_cast<jbyte *>(buffer));
        return length;
    } else {
        return 0;
    }
}

size_t receiveData(struct CurlContext *curlContext, char *buffer, size_t size) {
    if (curlContext == NULL) {
        return static_cast<size_t>(-1);
    }

    jobject curlHandler = curlContext->curlHandler;
    JNIEnv *env = curlContext->env;
    if (env == NULL || curlHandler == NULL) {
        return static_cast<size_t>(-1);
    }

    jclass handler_class = env->FindClass("com/qiniu/curl/CurlHandler");
    if (handler_class == NULL) {
        return static_cast<size_t>(-1);
    }

    jmethodID receiveData_method = env->GetMethodID(handler_class,
                                                    "receiveData",
                                                    "([B)V");
    if (receiveData_method == NULL) {
        return static_cast<size_t>(-1);
    }

    jbyteArray data = env->NewByteArray(size);
    env->SetByteArrayRegion(data, 0, size, reinterpret_cast<const jbyte*>(buffer));

    env->CallVoidMethod(curlHandler, receiveData_method, data);
    return size;
}

void completeWithError(struct CurlContext *curlContext, int errorCode, const char *errorInfo) {
    if (curlContext == NULL) {
        return;
    }

    jobject curlHandler = curlContext->curlHandler;
    JNIEnv *env = curlContext->env;
    if (env == NULL || curlHandler == NULL) {
        return;
    }

    jclass handler_class = env->FindClass("com/qiniu/curl/CurlHandler");
    if (handler_class == NULL) {
        return;
    }

    jmethodID completeWithError_method = env->GetMethodID(handler_class,
                                                          "completeWithError",
                                                          "(ILjava/lang/String;)V");
    if (completeWithError_method == NULL) {
        return;
    }

    jstring errorInfo_string = NULL;
    if (errorInfo == NULL){
        errorInfo_string = env->NewStringUTF(errorInfo);
    }
    env->CallVoidMethod(curlHandler, completeWithError_method, errorCode, errorInfo_string);
}

void sendProgress(struct CurlContext *curlContext, long long bytesSent, long long totalBytesSent, long long totalBytesExpectedToSend) {
    if (curlContext == NULL) {
        return;
    }

    jobject curlHandler = curlContext->curlHandler;
    JNIEnv *env = curlContext->env;
    if (env == NULL || curlHandler == NULL) {
        return;
    }

    jclass handler_class = env->FindClass("com/qiniu/curl/CurlHandler");
    if (handler_class == NULL) {
        return;
    }

    jmethodID sendProgress_method = env->GetMethodID(handler_class,
                                                     "sendProgress",
                                                     "(JJJ)V");
    if (sendProgress_method == NULL) {
        return;
    }

    env->CallVoidMethod(curlHandler, sendProgress_method, bytesSent, totalBytesSent, totalBytesExpectedToSend);
}

void receiveProgress(struct CurlContext *curlContext, long long bytesReceive, long long totalBytesReceive, long long totalBytesExpectedToReceive) {
    if (curlContext == NULL) {
        return;
    }

    jobject curlHandler = curlContext->curlHandler;
    JNIEnv *env = curlContext->env;
    if (env == NULL || curlHandler == NULL) {
        return;
    }

    jclass handler_class = env->FindClass("com/qiniu/curl/CurlHandler");
    if (handler_class == NULL) {
        return;
    }

    jmethodID receiveProgress_method = env->GetMethodID(handler_class,
                                                        "receiveProgress",
                                                        "(JJJ)V");
    if (receiveProgress_method == NULL) {
        return;
    }

    env->CallVoidMethod(curlHandler, receiveProgress_method, bytesReceive, totalBytesReceive, totalBytesExpectedToReceive);
}

void didFinishCollectingMetrics(struct CurlContext *curlContext){
    if (curlContext == NULL) {
        return;
    }

    jobject curlHandler = curlContext->curlHandler;
    JNIEnv *env = curlContext->env;
    if (env == NULL || curlHandler == NULL) {
        return;
    }

    jclass handler_class = env->FindClass("com/qiniu/curl/CurlHandler");
    if (handler_class == NULL) {
        return;
    }

    jmethodID didFinishCollectingMetrics_method = env->GetMethodID(handler_class,
                                                                   "didFinishCollectingMetrics",
                                                                   "(Lcom/qiniu/curl/CurlTransactionMetrics;)V");
    if (didFinishCollectingMetrics_method == NULL) {
        return;
    }

    env->CallVoidMethod(curlHandler, didFinishCollectingMetrics_method, curlContext->metrics);
}