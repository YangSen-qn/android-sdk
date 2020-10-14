//
// Created by yangsen on 2020/9/18.
//

#ifndef CURLDEMO_CURL_CONFIGURATION_H
#define CURLDEMO_CURL_CONFIGURATION_H

#include <curl/curl.h>

struct curl_slist * getJavaCurlConfigurationDnsResolverArray(CurlContext *curlContext, jobject curlConfiguration);

char * getJavaCurlConfigurationProxy(CurlContext *curlContext, jobject curlConfiguration);

#endif //CURLDEMO_CURL_CONFIGURATION_H
