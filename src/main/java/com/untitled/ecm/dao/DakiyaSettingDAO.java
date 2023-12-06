package com.untitled.ecm.dao;

import com.untitled.ecm.dao.mappers.DakiyaSettingMapper;
import com.untitled.ecm.dtos.DakiyaSetting;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.Iterator;
import java.util.List;

@RegisterMapper(DakiyaSettingMapper.class)
public interface DakiyaSettingDAO {

    // #pg95
    @SqlBatch("INSERT INTO dakiya_settings (key, value) VALUES (:key, :value) " +
            "ON CONFLICT (key) Do UPDATE SET value = EXCLUDED.value ")
    int[] saveMultipleSettings(@BindBean Iterator<DakiyaSetting> dakiyaSettingIterator);

    @SqlQuery("SELECT * FROM dakiya_settings")
    List<DakiyaSetting> getAllSettings();

    @SqlQuery("SELECT value from dakiya_settings where key = :key ")
    String getSettingValueByKey(@Bind("key") String key);

    // #pg95
    @SqlUpdate(("INSERT INTO dakiya_settings (key, value) VALUES (:key, :value)\n" +
            "ON CONFLICT (key) Do UPDATE SET value = EXCLUDED.value\n"))
    int saveSettingByKey(@Bind("key") String key, @Bind("value") String value);

    void close();
}
