# Tagged-template Kalan İş — Devir Notu (2026-07-08)

Bu, `h\`\`` tagged-template geçişinin KALAN küçük render'larını bitirmek için
devir notudur. Mekanizma, politika, güvenlik-hassas büyük ekranlar ve otomatik
güvence zaten kurulu; bu yalnızca kategori-1 (temiz map'li) render'ların
kalanını dönüştürme işidir.

## Durum
- Dal: **`tagged-template-kalan`** (master'dan ayrı; bitince master'a merge + push).
- Mekanizma index.html ~2050'de: `esc` / `raw` / `_hval` / `h`. Ayrıca `ea`
  (JS-in-attribute kaçırma). Bu üçü **e2e-test.js ve verify-h-render.js'te de
  elle kopya** — değişirse üçünü de senkron tut.
- Güvence: `node scripts/verify-h-render.js` (sunucu gerekmez, DOM-shim XSS,
  58 kontrol). Dönüştürülen HER yeni fonksiyona buraya senaryo ekle.
- Tam doğrulama (sunucu 8080'de çalışırken): `node scripts/e2e-test.js <token>`
  + `node scripts/verify-h-render.js` + `npx playwright test`.
- Syntax: `node C:\Temp\...\scratchpad\check-html-js.js` yoksa
  `node --check` benzeri; pratikte: index.html tek `<script>` bloğunu
  `new Function()` ile parse eden küçük script (git geçmişindeki komutlara bak)
  ya da sadece `node -e "require('fs')..."`. Basitçe: her commit öncesi bir
  syntax kontrolü + verify-h-render koş.

## Dönüşüm deseni (dönüştürülen 14 fonksiyonla AYNI)
- `` `…${esc(x)}…` `` → `` h`…${x}…` `` (h otomatik esc'ler).
- İç içe `arr.map(...).join('')` → `${arr.map(x=>h`…`)}` — **`.join('')` KALDIRILIR**
  (h array'i kendi ham-birleştirir; unutulursa `[object,object]` virgüllü bozulur).
- Koşullu ham parça / statik HTML → `${cond?raw('<html>'):''}` veya `${cond?h`…`:''}`.
- Çağıran tarafça üretilen HTML dize argümanı (actions vb.) → `${raw(htmlStr)}`.
- Alt-fonksiyon HTML döndürüyorsa (kindBadge, statusBadge, dimLabel-HTML) → `${raw(fn(...))}`.
- `onclick="fn('${x}')"` JS-arg: sabit/UUID id → `${raw(id)}`; KULLANICI VERİSİ →
  `${raw(ea(x))}` (örn. `openWsMemberModal('${raw(ea(w.name))}')`). h'in HTML-esc'i
  JS string'ini bozar, o yüzden JS bağlamı DAİMA raw.
- meta dizileri: `esc`'leri KALDIR (ham kur), `${meta}` tek seferde h esc'ler.

## Bitti (dönüşen fonksiyonlar)
Faz 4 büyük ekranlar: renderOrders, renderReceiving, renderPurchaseList,
renderPurchaseOrders (alt-parçalar), renderWorkOrders (alt-parçalar), renderDnList,
renderDnDetail(itemRows), whRow, renderWarehouse(dropdown), + önceki QR/yönetim
(renderUsers, renderDeptList, renderSuppliersModalList, renderScan, renderReceive).
Bu dal (küçükler): renderBreadcrumb, renderOpDefsList, renderWorkspaces,
renderWsCurrentMembers, renderBomList, renderWhMovements, renderWhStock.

## KALAN — kategori 1 (temiz map'li, DÖNÜŞTÜR)
renderPdList (~3920), renderAppUsers (~8916), viewPublishedBom (~11374),
renderBomTreeSvg (~9915), pbomeRenderList (~10368), pbomeShowTree (~10754),
renderPbomCustomParts (~11066), renderProjectBomList (~10100),
renderProjectCards (~2492), renderParts / renderPartsFiltered (~2859/2898),
renderStatsView / renderDetailTable (~3283/3461), renderPlanning (~3783),
renderPdForm, woLoadParts (~4360), deptWoStripHTML (~2231),
xlsImportRenderList, showLog, whvProjectHTML/whvWarehouseHTML (string-concat —
map+h``'ye çevir, dikkatli). (Satır no'ları kaymış olabilir; `grep -n` ile bul.)

## KALAN — kategori 2/3 (DÖNÜŞTÜRME, esc-güvenli bırak)
- Inline-JS onclick içerenler: **renderMrp, renderMrpParams** (`onclick="...forEach(c=>c.checked=true)"`,
  `onclick="purTogglePlanning(...).then(()=>renderMrp())"` — `=>`/`>` h'i bozar).
- Modal form (value="${esc()}" attribute, güvenli): supEdit, editPurchaseItem,
  editBomPart, openWoReviseModal, pbome*Edit/Op, whmEditWarehouse, rcvReceiveModal,
  openPurImport, openPbomCustomEdit.
- PDF/print (window.open + document.write, ayrı doküman): dnPrint, printWorkOrders,
  printGoodsReceiptPDF, whvProjectPDF.
Bunlar zaten `esc()` ile XSS-güvenli; CLAUDE.md politikası gereği fırsatçı kalır.

## Bitince
- Her ekranı verify-h-render.js'e ekle, `esc(` sayısı düşerken kontrol et.
- `docs/denetim-eksik-korkuluklar.md` "kalan tagged-template" notunu güncelle.
- Bu handoff dosyasını sil, dalı master'a merge + push.
