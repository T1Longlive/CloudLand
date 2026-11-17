-- 测试专用种子账号（幂等）
--
-- 为什么要单独一份 SQL：
-- cloudland.sql 是业务演示数据，里面没有测试要用的固定账号。
-- 如果测试依赖"某台机器上手工建过的账号"，CI 一跑就红，而且没人知道缺了什么。
-- 把这份文件纳入版本管理，等于把"测试前置条件"变成了可执行的代码。
--
-- 约定（与 common/config.py 的 SEED_ACCOUNTS 一致）：
--   19900000001 / power=0 客户
--   19900000002 / power=1 员工
--   19900000003 / power=2 管理员
--   密码统一 Test@123456（BCrypt 哈希，不可逆，无需在测试里再加密）
--
-- 幂等策略：靠 phone 唯一索引做 ON DUPLICATE KEY UPDATE，
-- 重复执行只会把字段重置回基线（比如某次测试把账号禁用/降权了，重跑本脚本即可恢复）。
-- 注意：不能 "先 DELETE 再 INSERT"，那会撞上 land.employee_id 等外键引用。

INSERT INTO `user`
    (`username`, `password`, `age`, `phone`, `address`, `img`, `power`, `debt`,
     `detailed_address`, `status`, `mail`)
VALUES
    ('测试客户', '$2a$10$jqd/lty1BTHNh0Bc428F/.QXVvYEtBd5DD7ksAblt1Lg14.m0AyhW', 25,
     '19900000001', '510000,510100,510101', 'basic.png', 0, 0.00,
     'E2E测试专用账号-客户', 1, 'e2e-test-customer@cloudland.test'),
    ('测试员工', '$2a$10$U.tykHCve5avh3YlwaTeGOPpbIbhMoAzJ5E.TkbJnt4bCUi1FpEsG', 30,
     '19900000002', '510000,510100,510101', 'basic.png', 1, 0.00,
     'E2E测试专用账号-员工', 1, 'e2e-test-employee@cloudland.test'),
    ('测试管理员', '$2a$10$mdMgAV9Mfjq8vGrPfNkyYuNWhatO2AXkqxk9rnwV4f8RjFr3279gi', 35,
     '19900000003', '510000,510100,510101', 'basic.png', 2, 0.00,
     'E2E测试专用账号-管理员', 1, 'e2e-test-admin@cloudland.test')
ON DUPLICATE KEY UPDATE
    `username`         = VALUES(`username`),
    `password`         = VALUES(`password`),
    `age`              = VALUES(`age`),
    `address`          = VALUES(`address`),
    `img`              = VALUES(`img`),
    `power`            = VALUES(`power`),
    `debt`             = VALUES(`debt`),
    `detailed_address` = VALUES(`detailed_address`),
    `status`           = VALUES(`status`),
    `mail`             = VALUES(`mail`);
