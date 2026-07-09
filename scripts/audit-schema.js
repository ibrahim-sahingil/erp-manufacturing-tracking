// ŞEMA ↔ ENTITY UYUM BEKÇİSİ (5. denetim turu, Bölüm 3).
//
// spring.jpa.hibernate.ddl-auto=none — şemayı Hibernate yönetmiyor, elle
// (DBeaver) yönetiliyor. Bu yüzden entity'de olup DB'de OLMAYAN bir kolon
// derleme anında değil, o entity ilk sorgulandığında ÇALIŞMA ANINDA patlar.
// Bu script db/schema.sql ile @Entity sınıflarını karşılaştırır.
//
// Kapsam: @Table(name=...) + @Column(name=...) eşlemeleri. @Transient,
// @OneToMany gibi DB kolonu olmayan alanlar atlanır.
// Kullanım: node scripts/audit-schema.js    (sunucu/DB GEREKMEZ)
const fs = require('fs');
const path = require('path');

const kok = path.join(__dirname, '..');
const schemaSql = fs.readFileSync(path.join(kok, 'db', 'schema.sql'), 'utf8');

// ── 1. schema.sql'den tablo → kolon kümesi ──────────────────────────────────
const tablolar = new Map();
const reCreate = /CREATE TABLE (?:public\.)?"?([a-zA-Z0-9_]+)"?\s*\(([\s\S]*?)\n\);/g;
for (const m of schemaSql.matchAll(reCreate)) {
  const tablo = m[1].toLowerCase();
  const kolonlar = new Set();
  for (const satir of m[2].split('\n')) {
    const s = satir.trim();
    if (!s || /^(CONSTRAINT|PRIMARY KEY|FOREIGN KEY|UNIQUE|CHECK)/i.test(s)) continue;
    const km = s.match(/^"?([a-zA-Z0-9_]+)"?\s+/);
    if (km) kolonlar.add(km[1].toLowerCase());
  }
  tablolar.set(tablo, kolonlar);
}

// ── 2. Entity dosyalarını topla ─────────────────────────────────────────────
function javaDosyalari(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) javaDosyalari(p, out);
    else if (e.name.endsWith('.java')) out.push(p);
  }
  return out;
}
const javaKok = path.join(kok, 'src', 'main', 'java');
const entityler = javaDosyalari(javaKok).filter(f => /@Entity/.test(fs.readFileSync(f, 'utf8')));

let sorun = 0;
const bilinmeyenTablo = [];
const eksikKolon = [];

for (const dosya of entityler) {
  const src = fs.readFileSync(dosya, 'utf8');
  const ad = path.basename(dosya, '.java');
  const tm = src.match(/@Table\s*\(\s*name\s*=\s*"([^"]+)"/);
  if (!tm) continue; // @Table yoksa varsayılan adlandırma — kapsam dışı
  const tablo = tm[1].toLowerCase();

  if (!tablolar.has(tablo)) {
    bilinmeyenTablo.push(`${ad} → @Table("${tablo}") ama schema.sql'de böyle bir tablo YOK`);
    sorun++;
    continue;
  }
  const dbKolonlar = tablolar.get(tablo);

  // @Column(name="x") geçen her alan; @Transient işaretli alanları atla
  for (const cm of src.matchAll(/@Column\s*\(([^)]*)\)/g)) {
    const nm = cm[1].match(/name\s*=\s*"([^"]+)"/);
    if (!nm) continue;
    const kolon = nm[1].toLowerCase();
    const oncesi = src.slice(Math.max(0, cm.index - 300), cm.index);
    if (/@Transient/.test(oncesi)) continue;

    // @ElementCollection alanlarında @Column, ana tabloya DEĞİL
    // @CollectionTable(name=...) ile verilen yan tabloya aittir.
    const ct = oncesi.match(/@CollectionTable\s*\(\s*name\s*=\s*"([^"]+)"/);
    const hedefTablo = ct ? ct[1].toLowerCase() : tablo;
    if (!tablolar.has(hedefTablo)) {
      bilinmeyenTablo.push(`${ad} → koleksiyon tablosu "${hedefTablo}" schema.sql'de YOK`);
      sorun++;
      continue;
    }
    if (!tablolar.get(hedefTablo).has(kolon)) {
      eksikKolon.push(`${ad} (${hedefTablo}) → "${kolon}" kolonu schema.sql'de YOK`);
      sorun++;
    }
  }
}

console.log(`schema.sql: ${tablolar.size} tablo · Entity: ${entityler.length} sınıf\n`);

console.log('── Entity\'nin işaret ettiği tablo şemada var mı? ──');
console.log(bilinmeyenTablo.length ? bilinmeyenTablo.map(x => '  ❌ ' + x).join('\n') : '  ✅ hepsi mevcut');

console.log('\n── Entity kolonları şemada var mı? ──');
console.log(eksikKolon.length ? eksikKolon.map(x => '  ❌ ' + x).join('\n') : '  ✅ hepsi mevcut');

// ── 3. Bilgi: şemada olup hiçbir entity'nin kullanmadığı tablolar ───────────
const kullanilan = new Set();
for (const dosya of entityler) {
  const tm = fs.readFileSync(dosya, 'utf8').match(/@Table\s*\(\s*name\s*=\s*"([^"]+)"/);
  if (tm) kullanilan.add(tm[1].toLowerCase());
}
// @CollectionTable ile eşlenenler de "kullanılıyor" sayılır (ör. bom_document_parts)
for (const dosya of entityler)
  for (const cm of fs.readFileSync(dosya, 'utf8').matchAll(/@CollectionTable\s*\(\s*name\s*=\s*"([^"]+)"/g))
    kullanilan.add(cm[1].toLowerCase());
const kullanilmayan = [...tablolar.keys()].filter(t => !kullanilan.has(t));
console.log('\n── Bilgi: şemada olup entity\'si olmayan tablolar ──');
console.log(kullanilmayan.length ? '  • ' + kullanilmayan.join(', ') : '  (yok)');

console.log('\n' + '─'.repeat(60));
if (sorun) { console.log(`❌ ${sorun} şema/entity uyumsuzluğu.`); process.exit(1); }
console.log('✅ Şema ↔ entity uyumlu.');
