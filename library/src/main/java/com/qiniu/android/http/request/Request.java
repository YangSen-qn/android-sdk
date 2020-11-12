package com.qiniu.android.http.request;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Request {

    public static final String HttpMethodGet = "GET";
    public static final String HttpMethodPOST = "POST";

    public final String urlString;
    public final String httpMethod;
    public final Map<String, String> allHeaders;
    public final int timeout;
    public byte[] httpBody;

    public String host;
    public String ip;

    protected IUploadServer uploadServer;

    public Request(String urlString,
                   String httpMethod,
                   Map<String, String> allHeaders,
                   byte[] httpBody,
                   int timeout) {

        this.urlString = urlString;
        this.httpMethod = (httpMethod != null) ? httpMethod : HttpMethodGet;
        this.allHeaders = (allHeaders != null) ? allHeaders : new HashMap<String, String>();
        this.httpBody = (httpBody != null) ? httpBody :  new byte[0];
        this.timeout = timeout;
    }

    public InetAddress getInetAddress(){
        if (host == null || uploadServer == null || uploadServer.getIp() == null) {
            return null;
        }

        try {
            InetAddress ipAddress = InetAddress.getByName(uploadServer.getIp());
            return InetAddress.getByAddress(host, ipAddress.getAddress());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isHttps(){
        if (this.urlString != null && this.urlString.contains("https")){
            return true;
        } else {
            return false;
        }
    }
    protected boolean isValid() {
        return this.urlString == null || httpMethod == null;
    }
}
