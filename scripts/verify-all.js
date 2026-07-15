// DOĞRULAMA KAPISI (tasarım 2026) — tüm bekçileri TEK komutla koşar.
// "Bitti" demeden önce bu geçmeli. Kullanım: node scripts/verify-all.js [--hizli]
//   --hizli: UI taramasını 2 temayla sınırlar, playwright'ı atlar (ara kontroller için).
// Sunucu 8080'de GÜNCEL yapıyla çalışıyor olmalı (taskkill + mvnw spring-boot:run).
const { execSync, spawnSync } = require('child_process');

const hizli = process.argv.includes('--hizli');
const sonuc = [];
const kos = (ad, cmd, args = []) => {
  process.stdout.write(`▶ ${ad} ... `);
  const t0 = Date.now();
  const r = spawnSync(cmd, args, { encoding: 'utf8', shell: true, timeout: 10 * 60 * 1000 });
  const ok = r.status === 0;
  const sn = Math.round((Date.now() - t0) / 1000);
  console.log((ok ? 'GEÇTİ ✅' : 'BAŞARISIZ ❌') + ` (${sn}s)`);
  if (!ok) {
    const out = (r.stdout || '') + (r.stderr || '');
    console.log(out.split('\n').slice(-15).join('\n'));
  }
  sonuc.push([ad, ok]);
  return ok;
};

(async () => {
  // 0) Sunucu güncel mi (frontend imzası target'takiyle aynı mı) — eski-yapı tuzağı
  try {
    const canli = await (await fetch('http://localhost:8080/', { signal: AbortSignal.timeout(5000) })).text();
    const disk = require('fs').readFileSync('src/main/resources/static/index.html', 'utf8');
    const imza = s => s.length + '|' + s.slice(0, 400);
    if (imza(canli) !== imza(disk)) {
      console.log('❌ SUNUCU ESKİ YAPIYI SERVİS EDİYOR — taskkill /F /IM java.exe + ./mvnw spring-boot:run gerekli');
      process.exit(1);
    }
    console.log('▶ sunucu güncel yapıyı servis ediyor ✅');
  } catch (e) {
    console.log('❌ Sunucuya erişilemiyor (8080): ' + e.message);
    process.exit(1);
  }

  // 1) Sunucusuz bekçiler
  // NOT: mvnw compile BURADA KOŞMAZ — sunucu açıkken Windows target kilidi
  // yüzünden düşer. Derleme, yeniden-başlatma ritüelinin parçasıdır
  // (taskkill → compile → run); 0. adımdaki imza kontrolü güncelliği doğrular.
  kos('mükerrer fonksiyon', 'node', ['scripts/verify-no-dup-fn.js']);
  kos('referans bütünlüğü', 'node', ['scripts/audit-refs.js']);
  kos('XSS/render + SVG sızıntı', 'node', ['scripts/verify-h-render.js']);
  kos('şema ↔ entity', 'node', ['scripts/audit-schema.js']);
  kos('depo aktarım çekirdeği', 'node', ['scripts/verify-whxfer.js']);
  kos('MİP hesap çekirdeği', 'node', ['scripts/verify-mip.js']);

  // 2) Canlı bekçiler
  kos('KONSEPT PARİTESİ', 'node', ['scripts/audit-concept-parity.js']);
  kos('UI tam taraması', 'node', ['scripts/audit-ui-sweep.js', ...(hizli ? ['--hizli'] : [])]);

  // 3) İş kuralları (e2e) — token içeride alınır
  const tokenRes = await (await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'testdev', password: 'test1234' })
  })).json();
  kos('e2e iş kuralları', 'node', ['scripts/e2e-test.js', tokenRes.data.token]);
  kos('yetki kilidi', 'node', ['scripts/verify-authz.js']);
  kos('pasif kullanıcı', 'node', ['scripts/verify-disabled-user.js']);
  kos('test artığı', 'node', ['scripts/audit-test-leftovers.js']);
  kos('sahipsiz kayıt', 'node', ['scripts/audit-orphans.js']);

  // 4) Tarayıcı paketi (flaky'e karşı 1 retry)
  if (!hizli) kos('playwright (23 test)', 'npx', ['playwright', 'test', '--retries=1']);

  const kirik = sonuc.filter(([, ok]) => !ok);
  console.log('\n════════ DOĞRULAMA KAPISI ÖZETİ ════════');
  sonuc.forEach(([ad, ok]) => console.log(`  ${ok ? '✅' : '❌'} ${ad}`));
  console.log(kirik.length
    ? `\n${kirik.length} bekçi BAŞARISIZ — iş bitmiş sayılmaz.`
    : '\nHEPSİ GEÇTİ — kapı açık ✅');
  process.exit(kirik.length ? 1 : 0);
})();
