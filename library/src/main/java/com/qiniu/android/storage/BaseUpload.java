package com.qiniu.android.storage;

import com.qiniu.android.common.Zone;
import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.common.ZonesInfo;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.request.UploadRegion;
import com.qiniu.android.http.metrics.UploadRegionRequestMetrics;
import com.qiniu.android.http.metrics.UploadTaskMetrics;
import com.qiniu.android.http.serverRegion.UploadDomainRegion;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public abstract class BaseUpload implements Runnable {
    public final String key;
    public final String fileName;
    public final byte[] data;
    public final File file;
    public final UpToken token;
    public final UploadOptions option;
    public final Configuration config;
    public final Recorder recorder;
    public final String recorderKey;
    public final UpTaskCompletionHandler completionHandler;


    private UploadRegionRequestMetrics currentRegionRequestMetrics;
    private UploadTaskMetrics metrics = new UploadTaskMetrics(null);

    private int currentRegionIndex;
    private ArrayList<UploadRegion> regions;

    private BaseUpload(File file,
                       byte[] data,
                       String fileName,
                       String key,
                       UpToken token,
                       UploadOptions option,
                       Configuration config,
                       Recorder recorder,
                       String recorderKey,
                       UpTaskCompletionHandler completionHandler) {
        this.file = file;
        this.data = data;
        this.fileName = fileName != null ? fileName : "?";
        this.key = key;
        this.token = token;
        this.option = option != null ? option : UploadOptions.defaultOptions();
        this.config = config;
        this.recorder = recorder;
        this.recorderKey = recorderKey;
        this.completionHandler = completionHandler;

        this.initData();
    }

    protected BaseUpload(File file,
                        String key,
                        UpToken token,
                        UploadOptions option,
                        Configuration config,
                        Recorder recorder,
                        String recorderKey,
                        UpTaskCompletionHandler completionHandler) {
        this(file, null, file.getName(), key, token, option, config, recorder, recorderKey, completionHandler);
    }

    protected BaseUpload(byte[] data,
                         String key,
                         String fileName,
                         UpToken token,
                         UploadOptions option,
                         Configuration config,
                         UpTaskCompletionHandler completionHandler) {
        this(null, data, fileName, key, token, option, config, null, null, completionHandler);
    }

    private void initData(){
        currentRegionIndex = 0;
    }


    @Override
    public void run(){
        config.zone.preQuery(token, new Zone.QueryHandler() {
            @Override
            public void complete(int code, ResponseInfo responseInfo, UploadRegionRequestMetrics requestMetrics) {
                metrics.addMetrics(requestMetrics);

                if (code == 0){
                    int prepareCode = prepareToUpload();
                    if (prepareCode == 0){
                        startToUpload();
                    } else {
                        ResponseInfo responseInfoP = ResponseInfo.errorInfo(prepareCode, null);
                        completeAction(responseInfoP, null);
                    }
                } else {
                    completeAction(responseInfo, responseInfo.response);
                }
            }
        });
    }

    protected int prepareToUpload(){
        setupRegions();
        return 0;
    }

    protected abstract void startToUpload();

    protected boolean switchRegionAndUpload(){
        if (currentRegionRequestMetrics != null){
            metrics.addMetrics(currentRegionRequestMetrics);
            currentRegionRequestMetrics = null;
        }
        boolean isSwitched = switchRegion();
        if (isSwitched){
            startToUpload();
        }
        return isSwitched;
    }

    protected void completeAction(ResponseInfo responseInfo,
                               JSONObject response){
        if (currentRegionRequestMetrics != null && metrics != null){
            metrics.addMetrics(currentRegionRequestMetrics);
        }
        if (completionHandler != null){
            completionHandler.complete(responseInfo, key, metrics, response);
        }


    }

    private void setupRegions(){
        if (config == null || config.zone == null){
            return;
        }
        ZonesInfo zonesInfo = config.zone.getZonesInfo(token);
        if (zonesInfo == null || zonesInfo.zonesInfo == null || zonesInfo.zonesInfo.size() == 0){
            return;
        }
        ArrayList<ZoneInfo> zoneInfos = zonesInfo.zonesInfo;

        ArrayList<UploadRegion> defaultRegions = new ArrayList<>();
        for (ZoneInfo zoneInfo : zoneInfos) {
            UploadDomainRegion region = new UploadDomainRegion();
            region.setupRegionData(zoneInfo);
            defaultRegions.add(region);
        }
        regions = defaultRegions;
        metrics.regions = defaultRegions;
    }

    protected void insertRegionAtFirstByZoneInfo(ZoneInfo zoneInfo){
        if (zoneInfo == null){
            return;
        }
        UploadDomainRegion region = new UploadDomainRegion();
        region.setupRegionData(zoneInfo);
        insertRegionAtFirst(region);
    }

    private void insertRegionAtFirst(UploadRegion region){
        regions.add(0, region);
    }

    protected boolean switchRegion(){
        if (regions == null) {
            return false;
        }
        boolean ret = false;
        synchronized (this){
            int regionIndex = currentRegionIndex + 1;
            if (regionIndex < regions.size()){
                currentRegionIndex = regionIndex;
                ret = true;
            }
        }
        return ret;
    }

    protected UploadRegion getTargetRegion(){
        if (regions == null || regions.size() == 0){
            return null;
        } else {
            return regions.get(0);
        }
    }

    protected UploadRegion getCurrentRegion(){
        if (regions == null){
            return null;
        }
        UploadRegion region = null;
        synchronized (this){
            if (currentRegionIndex < regions.size()){
                region = regions.get(currentRegionIndex);
            }
        }
        return region;
    }


    protected UploadRegionRequestMetrics getCurrentRegionRequestMetrics() {
        return currentRegionRequestMetrics;
    }

    // 一个上传流程可能会发起多个上传操作（如：上传多个分片），每个上传操作均是以一个Region的host做重试操作
    protected void addRegionRequestMetricsOfOneFlow(UploadRegionRequestMetrics metrics){
        if (metrics == null){
            return;
        }
        if (this.currentRegionRequestMetrics == null){
            this.currentRegionRequestMetrics = metrics;
        } else {
            this.currentRegionRequestMetrics.addMetrics(metrics);
        }
    }

    protected interface UpTaskCompletionHandler {
        void complete(ResponseInfo responseInfo,
                      String key,
                      UploadTaskMetrics requestMetrics,
                      JSONObject response);
    }
}