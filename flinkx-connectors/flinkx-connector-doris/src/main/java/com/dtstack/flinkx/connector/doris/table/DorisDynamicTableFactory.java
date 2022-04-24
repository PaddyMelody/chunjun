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

package com.dtstack.flinkx.connector.doris.table;

import com.dtstack.flinkx.connector.doris.options.DorisConf;
import com.dtstack.flinkx.connector.doris.options.DorisOptions;
import com.dtstack.flinkx.connector.doris.options.LoadConf;
import com.dtstack.flinkx.connector.doris.options.LoadConfBuilder;
import com.dtstack.flinkx.connector.doris.sink.DorisDynamicTableSink;
import com.dtstack.flinkx.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.flinkx.connector.jdbc.table.JdbcDynamicTableFactory;
import com.dtstack.flinkx.connector.mysql.dialect.MysqlDialect;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.utils.TableSchemaUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * declare doris table factory info.
 *
 * <p>Company: www.dtstack.com
 *
 * @author xuchao
 * @date 2021-11-21
 */
public class DorisDynamicTableFactory extends JdbcDynamicTableFactory
        implements DynamicTableSinkFactory {

    private static final String IDENTIFIER = "doris-x";

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        final FactoryUtil.TableFactoryHelper helper =
                FactoryUtil.createTableFactoryHelper(this, context);
        // 1.所有的requiredOptions和optionalOptions参数
        final ReadableConfig config = helper.getOptions();

        // 2.参数校验
        helper.validate();

        String url = config.get(DorisOptions.URL);
        List<String> feNodes = config.get(DorisOptions.FENODES);

        if (StringUtils.isEmpty(url) && (null == feNodes || feNodes.isEmpty())) {
            throw new IllegalArgumentException(
                    "Choose one of 'url' and 'feNodes', them can not be empty at same time.");
        }

        // 3.封装参数
        TableSchema physicalSchema =
                TableSchemaUtils.getPhysicalSchema(context.getCatalogTable().getSchema());

        DorisConf ftpConfig = getConfByOptions(config);
        return new DorisDynamicTableSink(physicalSchema, ftpConfig);
    }

    private static DorisConf getConfByOptions(ReadableConfig config) {
        DorisConf dorisConf = new DorisConf();

        dorisConf.setFeNodes(config.get(DorisOptions.FENODES));

        String schema = config.get(DorisOptions.SCHEMA);
        String tableName = config.get(DorisOptions.TABLE_NAME);
        dorisConf.setDatabase(schema);
        dorisConf.setTable(tableName);

        String url = config.get(DorisOptions.URL);
        List<String> feNodes = config.get(DorisOptions.FENODES);

        dorisConf.setUrl(url);
        dorisConf.setFeNodes(feNodes);

        if (config.get(DorisOptions.USERNAME) != null) {
            dorisConf.setUsername(config.get(DorisOptions.USERNAME));
        }

        if (config.get(DorisOptions.PASSWORD) != null) {
            dorisConf.setPassword(config.get(DorisOptions.PASSWORD));
        }

        LoadConf loadConf = getLoadConf(config);
        dorisConf.setLoadConf(loadConf);
        dorisConf.setLoadProperties(new Properties());
        dorisConf.setMaxRetries(config.get(DorisOptions.MAX_RETRIES));
        dorisConf.setWriteMode(config.get(DorisOptions.WRITE_MODE));
        dorisConf.setBatchSize(config.get(DorisOptions.BATCH_SIZE));

        return dorisConf;
    }

    private static LoadConf getLoadConf(ReadableConfig config) {
        LoadConfBuilder loadConfBuilder = new LoadConfBuilder();
        return loadConfBuilder
                .setRequestTabletSize(config.get(DorisOptions.REQUEST_TABLET_SIZE))
                .setRequestConnectTimeoutMs(config.get(DorisOptions.REQUEST_CONNECT_TIMEOUT_MS))
                .setRequestReadTimeoutMs(config.get(DorisOptions.REQUEST_READ_TIMEOUT_MS))
                .setRequestQueryTimeoutMs(config.get(DorisOptions.REQUEST_QUERY_TIMEOUT_SEC))
                .setRequestRetries(config.get(DorisOptions.REQUEST_RETRIES))
                .setRequestBatchSize(config.get(DorisOptions.REQUEST_BATCH_SIZE))
                .setExecMemLimit(config.get(DorisOptions.EXEC_MEM_LIMIT))
                .setDeserializeQueueSize(config.get(DorisOptions.DESERIALIZE_QUEUE_SIZE))
                .setDeserializeArrowAsync(config.get(DorisOptions.DESERIALIZE_ARROW_ASYNC))
                .build();
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    protected JdbcDialect getDialect() {
        return new MysqlDialect();
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        return new HashSet<>();
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> options = super.optionalOptions();
        Set<ConfigOption<?>> requiredOptions = super.requiredOptions();

        Set<ConfigOption<?>> optionalOptions =
                Stream.of(
                                DorisOptions.USERNAME,
                                DorisOptions.PASSWORD,
                                DorisOptions.REQUEST_TABLET_SIZE,
                                DorisOptions.REQUEST_CONNECT_TIMEOUT_MS,
                                DorisOptions.REQUEST_READ_TIMEOUT_MS,
                                DorisOptions.REQUEST_QUERY_TIMEOUT_SEC,
                                DorisOptions.REQUEST_RETRIES,
                                DorisOptions.REQUEST_BATCH_SIZE,
                                DorisOptions.EXEC_MEM_LIMIT,
                                DorisOptions.DESERIALIZE_QUEUE_SIZE,
                                DorisOptions.DESERIALIZE_ARROW_ASYNC,
                                DorisOptions.FIELD_DELIMITER,
                                DorisOptions.LINE_DELIMITER,
                                DorisOptions.MAX_RETRIES,
                                DorisOptions.WRITE_MODE,
                                DorisOptions.BATCH_SIZE)
                        .collect(Collectors.toSet());

        options.addAll(optionalOptions);
        options.addAll(requiredOptions);
        return options;
    }
}
