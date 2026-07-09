// index.html TEK DOSYA olduğundan aynı ada sahip iki üst-seviye fonksiyon
// sessizce birbirini EZER (sonraki kazanır) — kod okurken görünmez, testler
// bile yanıltır (grab() ilk tanımı alır, tarayıcı sonuncuyu çalıştırır).
// 5. denetim turunda gerçek örneği çıktı: plaka kataloğunun ssRenderList'i
// searchSelect'in ssRenderList'ini eziyor, tüm aranabilir kutuların listesi
// hiç açılmıyordu. Bu bekçi o sınıfı hatayı bir daha geçirmez.
// Kullanım: node scripts/verify-no-dup-fn.js   (sunucu GEREKMEZ)
const fs = require('fs');
const path = require('path');

const file = path.join(__dirname, '..', 'src', 'main', 'resources', 'static', 'index.html');
const lines = fs.readFileSync(file, 'utf8').split('\n');

// Sadece SÜTUN 0'daki tanımlar üst seviyedir; girintili olanlar başka bir
// fonksiyonun içinde (kapsamı yerel) ve çakışma yaratmaz.
const TOP_LEVEL_FN = /^(?:async\s+)?function\s+([A-Za-z0-9_$]+)\s*\(/;

const seen = new Map();
lines.forEach((line, i) => {
  const m = line.match(TOP_LEVEL_FN);
  if (m) {
    if (!seen.has(m[1])) seen.set(m[1], []);
    seen.get(m[1]).push(i + 1);
  }
});

const dups = [...seen.entries()].filter(([, ls]) => ls.length > 1);

console.log(`Üst seviye fonksiyon sayısı: ${seen.size}`);
if (dups.length) {
  console.log('\n❌ MÜKERRER ÜST SEVİYE FONKSİYON ADI — sonraki tanım öncekini EZER:\n');
  for (const [name, ls] of dups) console.log(`  • ${name}()  → satır ${ls.join(', ')}`);
  console.log('\nBirini yeniden adlandırın (çağrılarını da güncelleyin).');
  process.exit(1);
}
console.log('✅ Mükerrer üst seviye fonksiyon adı yok.');
