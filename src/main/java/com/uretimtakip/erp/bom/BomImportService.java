package com.uretimtakip.erp.bom;

import com.uretimtakip.erp.bom.dto.BomImportParseResponse;
import com.uretimtakip.erp.bom.dto.BomImportRowResponse;
import com.uretimtakip.erp.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Excel'den urun agaci (BOM) iceri aktarma - PARSE katmani.
 *
 * Beklenen kolon duzeni (ilk sayfa, 1. satir baslik):
 *   A: Basamak        (1, 1.1, 1.1.2 ... hiyerarsiyi belirler - ZORUNLU)
 *   B: Parca Adi      (ZORUNLU)
 *   C: Kod            (ZORUNLU)
 *   D: Malzeme
 *   E: Adet           (bos = 1)
 *   F: En (mm)        (opsiyonel sac/profil olcusu)
 *   G: Boy (mm)
 *   H: Kalinlik (mm)
 *
 * Dosya KAYDEDILMEZ; sadece parse edilir. Olusturma islemini frontend,
 * onizlemede secilen satirlarla mevcut bom-parts / project-bom-parts
 * endpoint'leri uzerinden yapar (mukerrer kod kontrolleri orada zaten var).
 *
 * Basamak kolonu icin DataFormatter kullanilir: hucre sayisal bile olsa
 * ekranda gorunen metin alinir ("1.1" gibi). Yine de kullaniciya sablonda
 * bu kolon METIN bicimli verilir (1.10 ile 1.1 karismasin).
 */
@Slf4j
@Service
public class BomImportService {

    private static final Pattern LEVEL_NO = Pattern.compile("^\\d+(\\.\\d+)*$");
    private static final int MAX_ROWS = 2000;

    public BomImportParseResponse parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Dosya bos.", "BOM_IMPORT_EMPTY_FILE");
        }
        String fname = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";
        if (!fname.endsWith(".xlsx") && !fname.endsWith(".xlsm") && !fname.endsWith(".xls")) {
            throw new BusinessException(
                    "Sadece .xlsx / .xlsm / .xls uzantili Excel dosyasi destekleniyor.",
                    "BOM_IMPORT_BAD_TYPE");
        }

        List<BomImportRowResponse> rows = new ArrayList<>();
        List<String> fileErrors = new ArrayList<>();
        DataFormatter fmt = new DataFormatter();

        // WorkbookFactory icerigi koklayarak hem .xls (HSSF) hem .xlsx (XSSF) acar
        try (InputStream in = file.getInputStream();
             Workbook wb = WorkbookFactory.create(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 0) {
                throw new BusinessException("Excel'in ilk sayfasi bos.",
                        "BOM_IMPORT_EMPTY_SHEET");
            }
            if (sheet.getLastRowNum() > MAX_ROWS) {
                throw new BusinessException(
                        "Dosya cok buyuk: en fazla " + MAX_ROWS + " satir desteklenir.",
                        "BOM_IMPORT_TOO_MANY_ROWS");
            }

            int firstDataRow = detectFirstDataRow(sheet, fmt);
            Set<String> seenLevelNos = new HashSet<>();
            Map<String, Integer> codeFirstRow = new HashMap<>();

            for (int r = firstDataRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row, fmt)) continue;

                String levelNo   = fmt.formatCellValue(row.getCell(0)).trim();
                String name      = fmt.formatCellValue(row.getCell(1)).trim();
                String code      = fmt.formatCellValue(row.getCell(2)).trim();
                String material  = fmt.formatCellValue(row.getCell(3)).trim();
                BigDecimal qty       = readNumber(row.getCell(4), fmt);
                BigDecimal widthMm   = readNumber(row.getCell(5), fmt);
                BigDecimal heightMm  = readNumber(row.getCell(6), fmt);
                BigDecimal thickMm   = readNumber(row.getCell(7), fmt);

                String error = null;
                String parentLevelNo = null;
                int level = 0;

                if (!LEVEL_NO.matcher(levelNo).matches()) {
                    error = "Basamak numarasi gecersiz: \"" + levelNo
                            + "\" (beklenen: 1, 1.1, 1.1.2 ...)";
                } else {
                    level = levelNo.split("\\.").length - 1;
                    int lastDot = levelNo.lastIndexOf('.');
                    parentLevelNo = lastDot > 0 ? levelNo.substring(0, lastDot) : null;

                    if (!seenLevelNos.add(levelNo)) {
                        error = "Ayni basamak numarasi dosyada iki kez var: " + levelNo;
                    } else if (parentLevelNo != null && !seenLevelNos.contains(parentLevelNo)) {
                        error = "Ust basamak (" + parentLevelNo
                                + ") dosyada bu satirdan once gelmiyor.";
                    }
                }
                if (error == null && name.isBlank()) error = "Parca adi bos.";
                if (error == null && code.isBlank()) error = "Kod bos.";
                if (error == null) {
                    Integer prev = codeFirstRow.putIfAbsent(code.toLowerCase(), r + 1);
                    if (prev != null) {
                        error = "Ayni kod dosyada " + prev + ". satirda da var: " + code;
                    }
                }

                rows.add(BomImportRowResponse.builder()
                        .rowNum(r + 1)
                        .levelNo(levelNo)
                        .parentLevelNo(parentLevelNo)
                        .level(level)
                        .name(name)
                        .code(code)
                        .material(material.isBlank() ? null : material)
                        .quantity(qty != null ? qty : BigDecimal.ONE)
                        .widthMm(widthMm)
                        .heightMm(heightMm)
                        .thicknessMm(thickMm)
                        .error(error)
                        .build());
            }
        } catch (BusinessException be) {
            throw be;
        } catch (IOException | RuntimeException e) {
            log.warn("BOM import parse hatasi: {}", e.getMessage());
            throw new BusinessException(
                    "Excel dosyasi okunamadi: " + e.getMessage(),
                    "BOM_IMPORT_PARSE_ERROR");
        }

        if (rows.isEmpty()) {
            fileErrors.add("Dosyada veri satiri bulunamadi (baslik haric).");
        }
        long errCount = rows.stream().filter(x -> x.getError() != null).count();

        return BomImportParseResponse.builder()
                .rows(rows)
                .fileErrors(fileErrors)
                .okCount((int) (rows.size() - errCount))
                .errorCount((int) errCount)
                .build();
    }

    /** Ilk hucre basamak desenine uymuyorsa o satiri baslik sayar. */
    private int detectFirstDataRow(Sheet sheet, DataFormatter fmt) {
        Row first = sheet.getRow(sheet.getFirstRowNum());
        if (first == null) return sheet.getFirstRowNum() + 1;
        String c0 = fmt.formatCellValue(first.getCell(0)).trim();
        return LEVEL_NO.matcher(c0).matches()
                ? sheet.getFirstRowNum()
                : sheet.getFirstRowNum() + 1;
    }

    private boolean isRowBlank(Row row, DataFormatter fmt) {
        for (int c = 0; c < 8; c++) {
            if (!fmt.formatCellValue(row.getCell(c)).trim().isEmpty()) return false;
        }
        return true;
    }

    /** Sayisal hucre okur; metin girilmisse virgullu Turkce degeri de tolere eder. */
    private BigDecimal readNumber(Cell cell, DataFormatter fmt) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            return d < 0 ? null : BigDecimal.valueOf(d);
        }
        String s = fmt.formatCellValue(cell).trim();
        if (s.isEmpty()) return null;
        s = s.replace(" ", "").replace(",", ".");
        try {
            BigDecimal v = new BigDecimal(s);
            return v.signum() < 0 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Doldurulabilir ornek sablon (.xlsx) uretir. */
    public byte[] buildTemplate() {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Urun Agaci");
            String[] headers = {"Basamak", "Parça Adı", "Kod", "Malzeme",
                    "Adet", "En (mm)", "Boy (mm)", "Kalınlık (mm)"};

            CellStyle headStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headStyle.setFont(bold);

            // Basamak kolonu METIN bicimli: 1.10 sayiya donusup 1.1 olmasin
            CellStyle textStyle = wb.createCellStyle();
            textStyle.setDataFormat(wb.createDataFormat().getFormat("@"));

            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = head.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle);
            }

            String[][] examples = {
                    {"1",     "Ana Gövde",             "GVD-001", "S235",  "1", "",     "",     ""},
                    {"1.1",   "Yan Sac",               "YS-001",  "St-37", "2", "300",  "500",  "3"},
                    {"1.1.1", "Yan Sac Destek Profili","YSD-001", "S235",  "4", "",     "1200", ""},
                    {"1.2",   "Kapak Sacı",            "KPK-001", "St-37", "1", "250",  "250",  "2"},
                    {"2",     "Şase",                  "SSE-001", "S355",  "1", "",     "",     ""}
            };
            for (int r = 0; r < examples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < examples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(examples[r][c]);
                    if (c == 0) cell.setCellStyle(textStyle);
                }
            }
            sheet.setDefaultColumnStyle(0, textStyle);
            for (int i = 0; i < headers.length; i++) sheet.setColumnWidth(i, 4400);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Sablon olusturulamadi: " + e.getMessage(),
                    "BOM_IMPORT_TEMPLATE_ERROR");
        }
    }
}
