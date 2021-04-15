package cn.cerc.mis.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.core.ISession;
import cn.cerc.core.Record;
import cn.cerc.db.core.ISessionOwner;
import cn.cerc.mis.core.Application;
import cn.cerc.mis.core.SystemBuffer;
import cn.cerc.mis.other.MemoryBuffer;
import redis.clients.jedis.Jedis;

public class SyncDatabase implements ISessionOwner {
    private static final Logger log = LoggerFactory.getLogger(SyncDatabase.class);
    private ISession session;

    protected static void push(String tableCode, Record record, SyncOpera opera) {
        Record rs = new Record();
        rs.setField("__table", tableCode);
        rs.setField("__opera", opera.ordinal());
        rs.copyValues(record);

        String buffKey = MemoryBuffer.buildKey(SystemBuffer.Global.SyncDatabase);
        try (Jedis jedis = SyncPushRedis.getJedis()) {
            jedis.lpush(buffKey, rs.toString());
        }
    }

    protected void pull(int times) {
        String buffKey = MemoryBuffer.buildKey(SystemBuffer.Global.SyncDatabase);
        try (Jedis jedis = SyncPullRedis.getJedis()) {
            for (int i = 0; i < times; i++) {
                String data = jedis.rpop(buffKey);
                if (data == null) {
                    continue;
                }

                Record record = new Record();
                record.setJSON(data);

                String tableCode = record.getString("__table");
                int opera = record.getInt("__opera");
                int error = record.getInt("__error");
                record.delete("__table");
                record.delete("__opera");
                record.delete("__error");

                ISyncRecord sync = Application.getBean(ISyncRecord.class, "sync_" + tableCode);
                if (sync == null) {
                    sync = new SyncTableDefault().setTableCode(tableCode);
                }
                sync.setSession(session);

                boolean result;
                switch (SyncOpera.values()[opera]) {
                case Append:
                    result = sync.appendRecord(record);
                    break;
                case Delete:
                    result = sync.deleteRecord(record);
                    break;
                case Update:
                    result = sync.updateRecord(record);
                    break;
                case Reset:
                    result = sync.resetRecord(record);
                    break;
                default:
                    throw new RuntimeException("not support opera.");
                }

                if (!result) {
                    record.setField("__table", tableCode);
                    record.setField("__opera", opera);
                    record.setField("__error", error + 1);
                    if (error < 5) {
                        jedis.rpush(buffKey, record.toString());
                        log.warn("sync {}.{} fail, times {}", tableCode, opera, error);
                    } else {
                        sync.abortRecord(record, SyncOpera.values()[opera]);
                    }
                }
            }
        }
    }

    @Override
    public ISession getSession() {
        return this.session;
    }

    @Override
    public void setSession(ISession session) {
        this.session = session;
    }

}