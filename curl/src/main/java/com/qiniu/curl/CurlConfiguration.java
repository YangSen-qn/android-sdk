package com.qiniu.curl;

public class CurlConfiguration {

    private String[] dnsResolverArray;
    private String proxy;
    private String proxyUserPwd;

    public String[] getDnsResolverArray() {
        return dnsResolverArray;
    }

    public String getProxy() {
        return proxy;
    }

    public String getProxyUserPwd() {
        return proxyUserPwd;
    }


    public CurlConfiguration(Builder builder) {
        this.dnsResolverArray = builder.dnsResolverArray;
        this.proxy = builder.proxy;
        this.proxyUserPwd = builder.proxyUserPwd;
    }

    public static class Builder{
        public String[] dnsResolverArray;
        public String proxy;
        public String proxyUserPwd;

        public CurlConfiguration build(){
            return new CurlConfiguration(this);
        }
    }
}
