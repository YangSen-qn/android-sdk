package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;

/**
 * Created by bailong on 14/10/12.
 */
public final class TestConfig {
    // TODO: 2020-05-09 bad token for testPutBytesWithFixedZoneUseBackupDomains
// 华东上传凭证
    public static final String bucket_z0 = "kodo-phone-zone0-space";
    public static final String token_z0 = "dxVQk8gyk3WswArbNhdKIwmwibJ9nFsQhMNUmtIM:VSYUpM0l-xwbNtSH0jYzqjqJI5A=:eyJzY29wZSI6ImtvZG8tcGhvbmUtem9uZTAtc3BhY2UiLCJkZWFkbGluZSI6MTY1NzM1Mzc5MSwgInJldHVybkJvZHkiOiJ7XCJmb29cIjokKHg6Zm9vKSwgXCJiYXJcIjokKHg6YmFyKSwgXCJtaW1lVHlwZVwiOiQobWltZVR5cGUpLCBcImhhc2hcIjokKGV0YWcpLCBcImtleVwiOiQoa2V5KSwgXCJmbmFtZVwiOiQoZm5hbWUpfSJ9";
    // 华北上传凭证
    public static final String bucket_z1 = "kodo-phone-zone1-space";
    public static final String token_z1 = "dxVQk8gyk3WswArbNhdKIwmwibJ9nFsQhMNUmtIM:wMAHQCA_pTuG4atQqWl6vjqBjQ4=:eyJzY29wZSI6ImtvZG8tcGhvbmUtem9uZTEtc3BhY2UiLCJkZWFkbGluZSI6MTY1NzM1Mzc5MSwgInJldHVybkJvZHkiOiJ7XCJmb29cIjokKHg6Zm9vKSwgXCJiYXJcIjokKHg6YmFyKSwgXCJtaW1lVHlwZVwiOiQobWltZVR5cGUpLCBcImhhc2hcIjokKGV0YWcpLCBcImtleVwiOiQoa2V5KSwgXCJmbmFtZVwiOiQoZm5hbWUpfSJ9";
    // 华南上传凭证
    public static final String bucket_z2 = "kodo-phone-zone2-space";
    public static final String token_z2 = "dxVQk8gyk3WswArbNhdKIwmwibJ9nFsQhMNUmtIM:uTnRnSynN9vGVzo8qnYT-QXjKIw=:eyJzY29wZSI6ImtvZG8tcGhvbmUtem9uZTItc3BhY2UiLCJkZWFkbGluZSI6MTY1NzM1Mzc5MSwgInJldHVybkJvZHkiOiJ7XCJmb29cIjokKHg6Zm9vKSwgXCJiYXJcIjokKHg6YmFyKSwgXCJtaW1lVHlwZVwiOiQobWltZVR5cGUpLCBcImhhc2hcIjokKGV0YWcpLCBcImtleVwiOiQoa2V5KSwgXCJmbmFtZVwiOiQoZm5hbWUpfSJ9";
    // 北美上传凭证
    public static final String bucket_na0 = "kodo-phone-zone-na0-space";
    public static final String token_na0 = "dxVQk8gyk3WswArbNhdKIwmwibJ9nFsQhMNUmtIM:E_6g6R_SNaik9oepZElktanK8Pc=:eyJzY29wZSI6ImtvZG8tcGhvbmUtem9uZS1uYTAtc3BhY2UiLCJkZWFkbGluZSI6MTY1NzM1Mzc5MSwgInJldHVybkJvZHkiOiJ7XCJmb29cIjokKHg6Zm9vKSwgXCJiYXJcIjokKHg6YmFyKSwgXCJtaW1lVHlwZVwiOiQobWltZVR5cGUpLCBcImhhc2hcIjokKGV0YWcpLCBcImtleVwiOiQoa2V5KSwgXCJmbmFtZVwiOiQoZm5hbWUpfSJ9";
    // 东南亚上传凭证
    public static final String bucket_as0 = "kodo-phone-zone-as0-space";
    public static final String token_as0 = "dxVQk8gyk3WswArbNhdKIwmwibJ9nFsQhMNUmtIM:7O_yi3Gju89b_GLN8Ffd5NOvXYQ=:eyJzY29wZSI6ImtvZG8tcGhvbmUtem9uZS1hczAtc3BhY2UiLCJkZWFkbGluZSI6MTY1NzM1Mzc5MSwgInJldHVybkJvZHkiOiJ7XCJmb29cIjokKHg6Zm9vKSwgXCJiYXJcIjokKHg6YmFyKSwgXCJtaW1lVHlwZVwiOiQobWltZVR5cGUpLCBcImhhc2hcIjokKGV0YWcpLCBcImtleVwiOiQoa2V5KSwgXCJmbmFtZVwiOiQoZm5hbWUpfSJ9";
    public static final String invalidBucketToken = "dxVQk8gyk3WswArbNhdKIwmwibJ9nFsQhMNUmtIM:MxUCcdbRDl2V67pA6g_whNXejEk=:eyJzY29wZSI6InpvbmVfaW52YWxpZCIsImRlYWRsaW5lIjoxNjU3MzUzNzkxLCAicmV0dXJuQm9keSI6IntcImZvb1wiOiQoeDpmb28pLCBcImJhclwiOiQoeDpiYXIpLCBcIm1pbWVUeXBlXCI6JChtaW1lVHlwZSksIFwiaGFzaFwiOiQoZXRhZyksIFwia2V5XCI6JChrZXkpLCBcImZuYW1lXCI6JChmbmFtZSl9In0=";
    
    // -----------
    public static final String ak = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW";


    //测试通用的token
    public static final String commonToken = token_na0;
    //dns prefetch token
    public static final String uptoken_prefetch = "MP_Ebql_lSsUrDr7WrXn_5vKocQDLvTPCNEFeVmp:3KJpXCGMqm6EAYU71RF1HDmQrcE=:eyJzY29wZSI6ImFuZHJvaWR0ZXN0IiwiZGVhZGxpbmUiOjE1Njc0OTAxODF9";

    /**
     * 华东机房
     */
    public static final Zone mock_bucket_zone0 = new FixedZone(new String[]{
            "mock.upload.qiniup.com", "mock.upload-nb.qiniup.com",
            "mock.upload-xs.qiniup.com", "mock.up.qiniup.com",
            "mock.up-nb.qiniup.com", "mock.up-xs.qiniup.com",
            "mock.upload.qbox.me", "up.qbox.me"
    });

    /**
     * 华北机房
     */
    public static final Zone mock_bucket_zone1 = new FixedZone(new String[]{
            "mock.upload-z1.qiniup.com", "mock.up-z1.qiniup.com",
            "mock.upload-z1.qbox.me", "up-z1.qbox.me"
    });

    /**
     * 华南机房
     */
    public static final Zone mock_bucket_zone2 = new FixedZone(new String[]{
            "mock.upload-z2.qiniup.com", "mock.upload-gz.qiniup.com",
            "mock.upload-fs.qiniup.com", "mock.up-z2.qiniup.com",
            "mock.up-gz.qiniup.com", "mock.up-fs.qiniup.com",
            "mock.upload-z2.qbox.me", "up-z2.qbox.me"
    });

    /**
     * 北美机房
     */
    public static final Zone mock_bucket_zoneNa0 = new FixedZone(new String[]{
            "mock.upload-na0.qiniu.com", "mock.up-na0.qiniup.com",
            "mock.upload-na0.qbox.me", "up-na0.qbox.me"
    });

}
