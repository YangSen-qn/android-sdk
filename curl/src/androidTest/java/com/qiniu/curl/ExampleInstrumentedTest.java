package com.qiniu.curl;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.qiniu.curl.test", appContext.getPackageName());
    }

    @Test
    public void testCurl(){

        CurlConfiguration.Builder builder = new CurlConfiguration.Builder();
        builder.dnsResolverArray = new String[]{"www.baidu.com:442:61.135.169.121"};

        CurlConfiguration curlConfiguration = builder.build();

        Curl curl = new Curl();
        long code = curl.globalInit();

        curl.request(new CurlHandlerI() {

            @Override
            public void receiveResponse(CurlResponse response) {
                Log.i("Curl","====== Response: url:" + response.url + " statusCode:" + response.statusCode + " headerInfo:" + response.allHeaderFields);
            }

            @Override
            public byte[] sendData(long dataLength) {
                Log.i("Curl","====== sendData:");
                return new byte[0];
            }

            @Override
            public void receiveData(byte[] data) {
                String info = new String(data);
                Log.i("Curl","====== receiveData:" + info);
            }

            @Override
            public void completeWithError(int errorCode, String errorInfo) {
                Log.i("Curl","====== completeWithError errorCode:" + errorCode + " errorInfo:" + errorInfo);
            }

            @Override
            public void sendProgress(long bytesSent, long totalBytesSent, long totalBytesExpectedToSend) {
                Log.i("Curl","====== sendProgress bytesSent:" + bytesSent + " totalBytesSent:" + totalBytesSent + " totalBytesExpectedToSend:" + totalBytesExpectedToSend);
            }

            @Override
            public void receiveProgress(long bytesReceive, long totalBytesReceive, long totalBytesExpectedToReceive) {
                Log.i("Curl","====== receiveProgress bytesReceive:" + bytesReceive + " totalBytesReceive:" + totalBytesReceive + " totalBytesExpectedToReceive:" + totalBytesExpectedToReceive);
            }

            @Override
            public void didFinishCollectingMetrics(CurlTransactionMetrics metrics) {
                Log.i("Curl","====== didFinishCollectingMetrics metrics:" + metrics);
            }
        }, curlConfiguration, "https://www.baidu.com", 1, null, null);


    }

    @After
    public void cleanData(){

    }
}
