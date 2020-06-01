package com.qiniu.android.common;

import android.os.Environment;

import com.qiniu.android.http.dns.Dns;
import com.qiniu.android.utils.ContextGetter;
import com.qiniu.android.utils.Utils;

/**
 * Created by Simon on 11/22/16.
 */
public final class Config {
    /**
     * 上传信息收集文件的地址 只保留域名部分 eg：https://uplog.qbox.me/log/3 => uplog.qbox.me
     */
    public final static String upLogURL = "uplog.qbox.me";
    /**
     * 是否记录上传状态信息。 true 表示记录，false 表示不记录。
     * <p>
     * 记录上传信息条件：
     * isRecord 为 true,
     * 记录文件大小 小于 maxRecordFileSize .
     * <p>
     * 记录文件大小 大于 maxRecordFileSize 时, 则暂停记录信息。
     */
    public static boolean isRecord = true;
    /**
     * 是否上传记录的上传状态信息。true 表示上传，false 表示不上传。
     * <p>
     * 上传条件:
     * 增加一条上传记录时触发,
     * isRecord 为 true, isUpload 为 true,
     * 且 记录文件大小 大于 uploadThreshold,
     * 且 距上次上传时间大于 minInteval .
     * <p>
     * 上传成功后，清空记录文件文件
     */
    public static boolean isUpload = true;
    /**
     * 上传信息记录文件保存的目录， 绝对路径。
     * 默认使用当前应用的缓存目录： getCacheDir()
     */
    public static String recordDir = null;
    /**
     *   是否开启dns预解析 默认开启
     */
    public static boolean isDnsOpen = true;
    /**
     *   dns预取缓存时间  单位：秒
     */
    public static int dnsCacheTime = 2 * 60;
    /**
     *   dns 预取失败后 会进行重新预取  rePreHostNum为最多尝试次数
     */
    public static int dnsRepreHostNum = 2;
    /**
     *   自定义DNS解析客户端host
     */
    public static Dns dns = null;
    /**
     *   dns解析结果本地缓存路径
     */
    public static final String dnscacheDir = Utils.sdkDirectory() + "/dnschache/";


    static {
        try {
            recordDir = ContextGetter.applicationContext().getCacheDir().getAbsolutePath();
        } catch (Throwable e) {
            e.fillInStackTrace();
        }
    }

    /**
     * 记录上传信息文件最大值，单位：字节。
     * <p>
     * 记录文件大于此值后暂停记录上传信息。
     */
    public static int maxRecordFileSize = 2 * 1024 * 1024;

    /**
     * 记录文件大于 uploadThreshold 后才可能触发上传，单位：字节。
     * <p>
     * 以 200,CwIAAF4znMnpno0U,up.qiniu.com,183.131.7.18,80,383.0,1481014578,262144 为例，
     * 100 条，约 7400Byte ；50 条，约 3700； 1024 约 13.8 条
     * <p>
     * chunkSize = 256 * 1024；putThreshold = 512 * 1024
     * 分片上传， 1M，最好情况下 5 个请求；10M，最好情况下 41 个请求
     * <p>
     * 可依据客户上传频率、文件大小做调整
     */
    public static int uploadThreshold = 4 * 1024;

    /**
     * 每次上传最小时间间隔.单位:分钟
     */
    public static int interval = 10;

    /**
     * preQuery host
     */
    public static String preQueryHost = "uc.qbox.me";

    /**
     * 当网络切换到 wifi 下，切换到此设置
     */
    public static void quick() {
        uploadThreshold = 1 * 1024;
        interval = 2;
    }

    public static void normal() {
        uploadThreshold = 4 * 1024;
        interval = 10;
    }

    /**
     * 网络走流量时，可切换到此设置
     */
    public static void slow() {
        uploadThreshold = 150 * 1024;
        interval = 300;
    }
}