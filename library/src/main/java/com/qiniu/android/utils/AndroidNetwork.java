package com.qiniu.android.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Process;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by bailong on 16/9/7.
 *
 * @hidden
 */
public final class AndroidNetwork {

    private AndroidNetwork() {
    }

    /**
     * 网络是否正常连接
     *
     * @return 网络是否连接
     */
    public static boolean isNetWorkReady() {
        Context c = ContextGetter.applicationContext();
        if (c == null) {
            return true;
        }
        ConnectivityManager connMgr = (ConnectivityManager)
                c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) {
            return true;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities caps = connMgr.getNetworkCapabilities(connMgr.getActiveNetwork());
                return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }

            NetworkInfo info = connMgr.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 获取ip地址
     * 使用DNS解析某地址时，可能会同时返回IPv4和IPv6的地址。
     * 如果同时拥有IPv4和IPv6的地址，是会默认优先上报IPv6的地址
     *
     * @return IP
     */
    public static String getHostIP() {
        String hostIp = null;
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            if (nis == null) {
                return null;
            }

            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress()) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

    /**
     * 网络类型
     * {@link  Constants#NETWORK_CLASS_UNKNOWN}
     * {@link  Constants#NETWORK_WIFI}
     * {@link  Constants#NETWORK_CLASS_2_G}
     * {@link  Constants#NETWORK_CLASS_3_G}
     * {@link  Constants#NETWORK_CLASS_4_G}
     * {@link  Constants#NETWORK_CLASS_5_G}
     * {@link  Constants#NETWORK_CLASS_MOBILE}
     *
     * @param context context
     * @return 网络类型
     */
    public static String networkType(Context context) {
        try {
            return getNetWorkClass(context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getNetWorkClass(Context context) {
        if (context == null) {
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        // API 29+ getNetworkType() 已废弃，直接用 NetworkCapabilities
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getNetworkClassByConnectivity(context);
        }

        // API 1-28 优先用 TelephonyManager，结果为 unknown 时降级
        String networkType = getNetworkTypeByTelephony(context);
        if (networkType != null && !Constants.NETWORK_CLASS_UNKNOWN.equals(networkType)) {
            return networkType;
        }

        // 此接口需要 23+，但返回信息不详细，所以上面优先使用 getNetworkTypeByTelephony
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getNetworkClassByConnectivity(context);
        }

        return Constants.NETWORK_CLASS_UNKNOWN;
    }

    /**
     * 通过 TelephonyManager 获取网络类型（2G/3G/4G/5G）
     * API < 23 时无需权限，API 23-28 需要 READ_PHONE_STATE 权限
     *
     * @param context context
     * @return 网络类型
     */
    @SuppressLint("MissingPermission")
    private static String getNetworkTypeByTelephony(Context context) {
        if (context.checkPermission(Manifest.permission.READ_PHONE_STATE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        switch (telephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return Constants.NETWORK_CLASS_2_G;

            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return Constants.NETWORK_CLASS_3_G;

            case TelephonyManager.NETWORK_TYPE_LTE:
                return Constants.NETWORK_CLASS_4_G;

            case TelephonyManager.NETWORK_TYPE_NR:
                return Constants.NETWORK_CLASS_5_G;

            default:
                return Constants.NETWORK_CLASS_UNKNOWN;
        }
    }

    /**
     * 使用 NetworkCapabilities 获取网络类型（无需 READ_PHONE_STATE 权限）
     * API 23+
     *
     * @param context context
     * @return 网络类型
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private static String getNetworkClassByConnectivity(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (caps == null) {
            return Constants.NETWORK_CLASS_UNKNOWN;
        }

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return Constants.NETWORK_WIFI;
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // 无法细分 2G/3G/4G/5G，统一返回 mobile
            return Constants.NETWORK_CLASS_MOBILE;
        }
        return Constants.NETWORK_CLASS_UNKNOWN;
    }
}
