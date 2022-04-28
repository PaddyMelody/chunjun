/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flinkx.connector.hbase2.table.lookup;

import com.dtstack.flinkx.connector.hbase2.common.HBaseTableSchema;
import com.dtstack.flinkx.connector.hbase2.conf.HBaseConf;
import com.dtstack.flinkx.connector.hbase2.converter.HBaseSerde;
import com.dtstack.flinkx.lookup.AbstractAllTableFunction;
import com.dtstack.flinkx.lookup.conf.LookupConf;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.functions.FunctionContext;

import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

public class HBaseAllTableFunction extends AbstractAllTableFunction {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(HBaseAllTableFunction.class);
    private Connection conn;
    private Table table;
    private ResultScanner resultScanner;

    private final HBaseTableSchema hbaseTableSchema;
    private transient HBaseSerde serde;
    private final String nullStringLiteral;
    private final HBaseConf hBaseConf;

    public HBaseAllTableFunction(
            LookupConf lookupConf, HBaseTableSchema hbaseTableSchema, HBaseConf hBaseConf) {
        super(null, null, lookupConf, null);
        this.hbaseTableSchema = hbaseTableSchema;
        this.hBaseConf = hBaseConf;
        this.nullStringLiteral = hBaseConf.getNullMode();
    }

    @Override
    public void open(FunctionContext context) throws Exception {
        this.serde = new HBaseSerde(hbaseTableSchema, nullStringLiteral);
        super.open(context);
    }

    @Override
    protected void loadData(Object cacheRef) {
        Configuration hbaseDomainConf = new Configuration();
        for (Map.Entry<String, Object> entry : hBaseConf.getHbaseConfig().entrySet()) {
            hbaseDomainConf.set(entry.getKey(), entry.getValue().toString());
        }
        int loadDataCount = 0;
        try {
            conn = ConnectionFactory.createConnection(hbaseDomainConf);
            table = conn.getTable(TableName.valueOf(hbaseTableSchema.getTableName()));
            resultScanner = table.getScanner(new Scan());
            Map<Object, RowData> tmpCache = (Map<Object, RowData>) cacheRef;
            for (Result r : resultScanner) {
                tmpCache.put(serde.getRowKey(r.getRow()), serde.convertToNewRow(r));
                loadDataCount++;
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            LOG.info("load Data count: {}", loadDataCount);
            try {
                if (null != conn) {
                    conn.close();
                }

                if (null != table) {
                    table.close();
                }

                if (null != resultScanner) {
                    resultScanner.close();
                }
            } catch (IOException e) {
                LOG.error("", e);
            }
        }
    }

    @Override
    protected void initCache() {
        Map<Object, RowData> newCache = Maps.newConcurrentMap();
        cacheRef.set(newCache);
        loadData(newCache);
    }

    /** 定时加载数据库中数据 */
    @Override
    protected void reloadCache() {
        // reload cacheRef and replace to old cacheRef
        Map<Object, RowData> newCache = Maps.newConcurrentMap();
        loadData(newCache);
        cacheRef.set(newCache);
        LOG.info(
                "----- " + lookupConf.getTableName() + ": all cacheRef reload end:{}",
                LocalDateTime.now());
    }

    /**
     * The invoke entry point of lookup function.
     *
     * @param keys the lookup key. Currently only support single rowkey.
     */
    @Override
    public void eval(Object... keys) {
        Map<Object, RowData> cache = (Map<Object, RowData>) cacheRef.get();
        RowData rowData = cache.get(keys[0]);
        if (rowData == null) {
            return;
        }
        collect(rowData);
    }
}
