-- E2E 测试专用种子账号（199 号段，与业务测试数据隔离）
-- 密码统一为 Test@123456（BCrypt 哈希由后端注册流程生成，可重新生成见文末说明）
-- 用途：vue/tests 下的 Playwright 用例依赖这 3 个账号；重复导入安全（INSERT IGNORE）
-- 导入（Windows PowerShell）：
--   Get-Content vue/tests/fixtures/seed.sql -Encoding UTF8 | mysql -uroot -p cloudland
-- 导入（Linux/macOS）：
--   mysql -uroot -p cloudland < vue/tests/fixtures/seed.sql
--
-- 哈希失效时重新生成：删除 3 个账号后，用 /user/register 真实注册流程重建，
-- 再从 user 表导出新哈希回填本文件（保证与后端 BCryptPasswordEncoder 完全一致）。

INSERT IGNORE INTO `user`
  (`username`, `password`, `age`, `phone`, `address`, `img`, `power`, `debt`, `detailed_address`, `status`, `mail`)
VALUES
  ('测试客户', '$2a$10$jqd/lty1BTHNh0Bc428F/.QXVvYEtBd5DD7ksAblt1Lg14.m0AyhW', 25, '19900000001', '510000,510100,510101', 'basic.png', 0, 0.00, 'E2E测试专用账号-客户', 1, 'e2e-test-customer@cloudland.test'),
  ('测试员工', '$2a$10$U.tykHCve5avh3YlwaTeGOPpbIbhMoAzJ5E.TkbJnt4bCUi1FpEsG', 30, '19900000002', '510000,510100,510101', 'basic.png', 1, 0.00, 'E2E测试专用账号-员工', 1, 'e2e-test-employee@cloudland.test'),
  ('测试管理员', '$2a$10$mdMgAV9Mfjq8vGrPfNkyYuNWhatO2AXkqxk9rnwV4f8RjFr3279gi', 35, '19900000003', '510000,510100,510101', 'basic.png', 2, 0.00, 'E2E测试专用账号-管理员', 1, 'e2e-test-admin@cloudland.test');
