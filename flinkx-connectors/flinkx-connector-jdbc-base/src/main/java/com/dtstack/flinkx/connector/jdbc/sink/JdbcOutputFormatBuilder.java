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
package com.dtstack.flinkx.connector.jdbc.sink;

import com.dtstack.flinkx.connector.jdbc.conf.JdbcConf;
import com.dtstack.flinkx.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.flinkx.converter.AbstractRowConverter;
import com.dtstack.flinkx.sink.format.BaseRichOutputFormatBuilder;

import org.apache.commons.lang.StringUtils;

/** @author sishu.yss @Company: www.dtstack.com */
public class JdbcOutputFormatBuilder extends BaseRichOutputFormatBuilder {

    protected JdbcOutputFormat format;

    public JdbcOutputFormatBuilder(JdbcOutputFormat format) {
        super.format = this.format = format;
    }

    public void setJdbcConf(JdbcConf jdbcConf) {
        super.setConfig(jdbcConf);
        format.setJdbcConf(jdbcConf);
    }

    public void setJdbcDialect(JdbcDialect JdbcDialect) {
        format.setJdbcDialect(JdbcDialect);
    }

    @Override
    public void setRowConverter(AbstractRowConverter rowConverter) {
        format.setRowConverter(rowConverter);
    }

    @Override
    protected void checkFormat() {
        JdbcConf jdbcConf = format.getJdbcConf();
        StringBuilder sb = new StringBuilder(256);
        if (StringUtils.isBlank(jdbcConf.getUsername())) {
            sb.append("No username supplied;\n");
        }

        if (StringUtils.isBlank(jdbcConf.getJdbcUrl())) {
            sb.append("No jdbc url supplied;\n");
        }
        if (sb.length() > 0) {
            throw new IllegalArgumentException(sb.toString());
        }
    }
}
