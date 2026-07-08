// Playwright yapılandırması (Faz 5) — kritik akışların gerçek tarayıcı testi.
// Uygulama PostgreSQL + secrets.properties gerektirdiğinden Playwright'ın
// sunucuyu başlatması yerine ZATEN ÇALIŞAN 8080 sunucusuna bağlanılır
// (./mvnw spring-boot:run önceden ayakta olmalı). Test kullanıcısı: testdev.
const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',
  timeout: 30000,
  expect: { timeout: 7000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:8080',
    headless: true,
    actionTimeout: 10000,
    ignoreHTTPSErrors: true
  }
});
