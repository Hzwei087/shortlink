package com.nageoffer.shortlink.admin.test;

public class UsertableShardingTest {

    public static final String SQL = "CREATE TABLE `t_link_%d`  (\n" +
            "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',\n" +
            "  `domain` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '域名',\n" +
            "  `short_uri` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '短链接',\n" +
            "  `full_short_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '完整短链接',\n" +
            "  `origin_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '原始链接',\n" +
            "  `click_num` int(11) NULL DEFAULT 0 COMMENT '点击量',\n" +
            "  `gid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分组标识',\n" +
            "  `enable_status` tinyint(1) NULL DEFAULT NULL COMMENT '启用标识 0启用 1未启用',\n" +
            "  `created_type` tinyint(1) NULL DEFAULT NULL COMMENT '创建类型 0 接口创建 1控制台创建',\n" +
            "  `valid_date_type` tinyint(1) NULL DEFAULT NULL COMMENT '有效期类型 0 永久有效 1临时有效',\n" +
            "  `valid_date` datetime(0) NULL DEFAULT NULL COMMENT '有效期',\n" +
            "  `describe` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',\n" +
            "  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',\n" +
            "  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '修改时间',\n" +
            "  `del_flag` tinyint(1) NULL DEFAULT NULL COMMENT '删除标识 0：未删除 1:已删除',\n" +
            "  PRIMARY KEY (`id`) USING BTREE,\n" +
            "  UNIQUE INDEX `idx_unique_full_short_url`(`full_short_url`) USING BTREE\n" +
            ") ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;";
    public static final String DROPSQL = "DROP table t_link_%d;";

    public static void main(String[] args) {
        for (int i = 0; i < 16; i++) {
            System.out.println(String.format(SQL,i));

        }

    }
}
