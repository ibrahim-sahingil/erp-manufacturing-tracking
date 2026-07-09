// (5. denetim turu) Pasife alinan kullanicinin token'i ANINDA gecersiz olmali.
//
// Bulunan hata: JwtService.isTokenValid yalnizca kullanici adi + sure kontrolu
// yapiyordu; hesap aktifligine BAKMIYORDU. JwtAuthFilter kimligi dogrudan
// kurdugu icin Spring'in hesap-durumu denetimi de devreye girmiyordu. Sonuc:
// "pasife alma" bir erisim iptali DEGILDI — kullanici elindeki token'la token
// suresi dolana kadar (24 saat) tum API'yi kullanmaya devam ediyordu.
//
// Kullanim: node scripts/verify-disabled-user.js   (sunucu 8080'de calismali)
const BASE = 'http://localhost:8080/api/';
let fail = 0;
const chk = (ad, ok, ek = '') => {
  console.log((ok ? '  ✅ ' : '  ❌ FAIL ') + ad + (ek ? '  — ' + ek : ''));
  if (!ok) fail++;
};

async function call(tok, method, path, body) {
  const r = await fetch(BASE + path.replace(/^\//, ''), {
    method,
    headers: { Authorization: 'Bearer ' + tok, 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  let j = null; try { j = await r.json(); } catch (e) {}
  return { status: r.status, ok: r.ok && !(j && j.success === false), msg: (j && j.message) || '' };
}
async function login(u, p) {
  const r = await fetch(BASE + 'auth/login', { method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: u, password: p }) });
  const j = await r.json().catch(() => null);
  return { token: j && j.data && (j.data.token || j.data.access_token), status: r.status };
}

(async () => {
  const dev = (await login('testdev', 'test1234')).token;
  if (!dev) throw new Error('testdev login basarisiz — sunucu ayakta mi?');

  const sfx = Date.now().toString(36);
  const uname = 'disabled.test.' + sfx;
  const c = await call(dev, 'POST', '/users', { username: uname, password: 'disabled12345',
    name: 'DISABLED Test', role: 'user', permissions: ['dashboard'] });
  if (!c.ok) throw new Error('test kullanicisi olusmadi: ' + c.msg);
  const users = await fetch(BASE + 'users', { headers: { Authorization: 'Bearer ' + dev } })
    .then(r => r.json());
  const uid = users.data.find(u => u.username === uname).id;

  try {
    const lr = await login(uname, 'disabled12345');
    chk('aktif kullanici login olabildi', !!lr.token);
    const TOK = lr.token;

    const before = await call(TOK, 'GET', '/bom-products');
    chk('aktifken token calisiyor (GET 200)', before.ok, 'HTTP ' + before.status);

    // Pasife al (dev token ile)
    const off = await call(dev, 'PUT', '/users/' + uid, { is_active: false });
    chk('kullanici pasife alindi', off.ok, off.msg || ('HTTP ' + off.status));

    // ESKI token artik gecersiz olmali
    const after = await call(TOK, 'GET', '/bom-products');
    chk('PASIF kullanicinin ESKI token\'i REDDEDILDI', after.status === 401 || after.status === 403,
        'HTTP ' + after.status);

    // Yeni login de reddedilmeli
    const relog = await login(uname, 'disabled12345');
    chk('pasif kullanici yeniden login OLAMADI', !relog.token, 'HTTP ' + relog.status);

    // Geri aktive edilince tekrar calismali (kirilma kontrolu)
    await call(dev, 'PUT', '/users/' + uid, { is_active: true });
    const relog2 = await login(uname, 'disabled12345');
    chk('tekrar aktif edilince login calisiyor', !!relog2.token);
    if (relog2.token) {
      const back = await call(relog2.token, 'GET', '/bom-products');
      chk('tekrar aktif kullanici API kullanabiliyor', back.ok, 'HTTP ' + back.status);
    }
  } finally {
    await call(dev, 'DELETE', '/users/' + uid);
  }

  console.log('\n' + '─'.repeat(60));
  if (fail) { console.log(`❌ ${fail} kontrol BASARISIZ.`); process.exit(1); }
  console.log('✅ PASIF KULLANICI KONTROLU SAGLAM — token aninda gecersiz oluyor.');
})().catch(e => { console.error('HATA:', e.message); process.exit(1); });
