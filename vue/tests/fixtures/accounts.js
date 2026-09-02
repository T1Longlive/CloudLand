// E2E 测试账号集中定义（与 seed.sql 保持同步）
// seed.sql 中三个 199 段账号由 INSERT IGNORE 保证幂等，密码统一 Test@123456
export const TEST_ACCOUNTS = {
  customer: { phone: '19900000001', password: 'Test@123456', email: 'e2e-test-customer@cloudland.test', power: 0 },
  employee: { phone: '19900000002', password: 'Test@123456', email: 'e2e-test-employee@cloudland.test', power: 1 },
  admin:    { phone: '19900000003', password: 'Test@123456', email: 'e2e-test-admin@cloudland.test', power: 2 },
};

// 后端 API 地址支持环境变量覆盖
export const API_BASE_URL = process.env.TEST_API_BASE_URL || 'http://localhost:9090/api';
