package com.qiniu.android.transaction;

import com.qiniu.android.BaseTest;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.android.utils.Utils;

import java.util.Date;

/**
 * Created by yangsen on 2020/6/9
 */
public class TransactionManagerTest extends BaseTest {

    public void testTransaction(){

        TransactionManager.Transaction normal = new TransactionManager.Transaction("1", 0, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("1");
            }
        });
        assertNotNull(normal);


        TransactionManager.Transaction time = new TransactionManager.Transaction("1", 0, 1, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("2");
            }
        });
        assertNotNull(time);
    }

    public void testTransactionManagerAddAndRemove(){

        final boolean[] executedTransaction = {false};
        String normalName = "testNormalTransaction";
        TransactionManager.Transaction normal = new TransactionManager.Transaction(normalName, 0, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("1: thread:" + Thread.currentThread().getId() + new Date().toString());
                executedTransaction[0] = true;
            }
        });

        String timeName = "testTimeTransaction";
        TransactionManager.Transaction time = new TransactionManager.Transaction(timeName, 3, 2, new Runnable() {
            @Override
            public void run() {
                LogUtil.d("2: thread:" + Thread.currentThread().getId() + new Date().toString());
            }
        });

        TransactionManager manager = TransactionManager.getInstance();
        manager.addTransaction(normal);
        manager.addTransaction(time);


        wait(new WaitConditional() {
            @Override
            public boolean shouldWait() {
                return !executedTransaction[0];
            }
        }, 60);
        
        wait(null, 6);

        long timestamp = Utils.currentSecondTimestamp();
        assertTrue("timestamp:: manager action count:" + manager.actionCount + " normal nextExecutionTime:" + normal.nextExecutionTime + " now:" + timestamp, normal.nextExecutionTime < timestamp);
        assertTrue("maybeCompleted:: manager action count:" + manager.actionCount + " normal nextExecutionTime:" + normal.nextExecutionTime + " now:" + timestamp, normal.maybeCompleted());
        assertEquals("executedCount:: manager action count:" + manager.actionCount + " normal.executedCount:" + normal.executedCount, 1, normal.executedCount);

        boolean exist = manager.existTransactionsForName(normalName);
        assertFalse(exist);
        exist = manager.existTransactionsForName(timeName);
        assertTrue(exist);
    }

}
