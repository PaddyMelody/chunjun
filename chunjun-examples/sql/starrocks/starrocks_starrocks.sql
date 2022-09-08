CREATE TABLE source
(
    id            int,
    boolean_data  boolean,
    tinyint_data  tinyint,
    smallint_data smallint,
    integer_data  integer,
    bigint_data   bigint,
    float_data    float,
    double_data   double,
    decimal_data  decimal,
    string_data   string,
    date_data     date,
    datetime_data timestamp(0)
) with (
      'connector' = 'starrocks-x',
      'url' = 'jdbc:mysql://node1:9030',
      'feNodes' = 'node1:8030;node2:8030;node3:8030',
      'schema-name' = 'test',
      'table-name' = 'source',
      'username' = 'root',
      'password' = ''
      );


CREATE TABLE sink
(
    id            int,
    boolean_data  boolean,
    tinyint_data  tinyint,
    smallint_data smallint,
    integer_data  integer,
    bigint_data   bigint,
    float_data    float,
    double_data   double,
    decimal_data  decimal,
    string_data   string,
    date_data     date,
    datetime_data timestamp(0)
) with (
      'connector' = 'starrocks-x',
      'url' = 'jdbc:mysql://node1:9030',
      'feNodes' = 'node1:8030;node2:8030;node3:8030',
      'schema-name' = 'test',
      'table-name' = 'sink',
      'username' = 'root',
      'password' = ''
      );

insert into sink
select *
from source;
