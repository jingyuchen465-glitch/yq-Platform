-- 种子数据使用 INSERT IGNORE 仅补齐缺失项，不覆盖后台已经修改的配置值。
CREATE TABLE IF NOT EXISTS sys_config_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置类型ID',
    type_code VARCHAR(64) NOT NULL COMMENT '配置类型编码',
    type_name VARCHAR(64) NOT NULL COMMENT '配置类型名称',
    description VARCHAR(255) NULL COMMENT '配置类型描述',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_config_type_code (type_code),
    KEY idx_sys_config_type_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置类型';

CREATE TABLE IF NOT EXISTS sys_config_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置项ID',
    type_id BIGINT NOT NULL COMMENT '配置类型ID',
    item_key VARCHAR(64) NOT NULL COMMENT '配置项键',
    item_value VARCHAR(1024) NOT NULL COMMENT '配置项值',
    value_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '值类型：STRING/INTEGER_LIST/BOOLEAN',
    description VARCHAR(255) NULL COMMENT '配置项描述',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_config_item_type_key (type_id, item_key),
    KEY idx_sys_config_item_type_status (type_id, status),
    CONSTRAINT fk_sys_config_item_type FOREIGN KEY (type_id) REFERENCES sys_config_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置项';

INSERT IGNORE INTO sys_config_type (type_code, type_name, description, status)
VALUES ('CLASS_SCHEDULE_RULE', '排课规则', '课表生成使用的动态规则', 'ACTIVE');

INSERT IGNORE INTO sys_config_item
    (type_id, item_key, item_value, value_type, description, status, sort_order)
SELECT id, 'CLASSDAYS', '1,2,3,5,6', 'INTEGER_LIST', '上课日，1=周一，7=周日', 'ACTIVE', 10
FROM sys_config_type WHERE type_code = 'CLASS_SCHEDULE_RULE';

INSERT IGNORE INTO sys_config_item
    (type_id, item_key, item_value, value_type, description, status, sort_order)
SELECT id, 'SELFSTUDYDAYS', '4', 'INTEGER_LIST', '自习日，1=周一，7=周日', 'ACTIVE', 20
FROM sys_config_type WHERE type_code = 'CLASS_SCHEDULE_RULE';

INSERT IGNORE INTO sys_config_item
    (type_id, item_key, item_value, value_type, description, status, sort_order)
SELECT id, 'RESTDAYS', '7', 'INTEGER_LIST', '休息日，1=周一，7=周日', 'ACTIVE', 30
FROM sys_config_type WHERE type_code = 'CLASS_SCHEDULE_RULE';

INSERT IGNORE INTO sys_config_item
    (type_id, item_key, item_value, value_type, description, status, sort_order)
SELECT id, 'HOLIDAYREST', 'true', 'BOOLEAN', '法定节假日是否休息', 'ACTIVE', 40
FROM sys_config_type WHERE type_code = 'CLASS_SCHEDULE_RULE';
