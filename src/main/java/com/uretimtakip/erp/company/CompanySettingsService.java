package com.uretimtakip.erp.company;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.company.dto.CompanySettingsRequest;
import com.uretimtakip.erp.company.dto.CompanySettingsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * CompanySettings is mantigi (15. tur Y2a — sabit firma ayarlari).
 *
 * TEK SATIR kurali: get() satir yoksa BOS satir yaratir (ilk GET'te olusur,
 * frontend her zaman dolu bir nesne alir); update() hep o satiri gunceller.
 * PARTIAL: null alan dokunmaz, bos string temizler (logo dahil).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanySettingsService {

    /** Logo ikili boyut siniri — PDF antetine gomulecek kucuk gorsel. */
    private static final int MAX_LOGO_BYTES = 500 * 1024;

    private final CompanySettingsRepository companySettingsRepository;

    @Transactional
    public CompanySettingsResponse get() {
        return CompanySettingsResponse.fromEntity(getOrCreate());
    }

    @Transactional
    public CompanySettingsResponse update(CompanySettingsRequest request) {
        CompanySettings s = getOrCreate();
        if (request.getName() != null) s.setName(request.getName().trim());
        if (request.getAddress() != null) s.setAddress(blankToNull(request.getAddress()));
        if (request.getPhone() != null) s.setPhone(blankToNull(request.getPhone()));
        if (request.getEmail() != null) s.setEmail(blankToNull(request.getEmail()));
        if (request.getTaxOffice() != null) s.setTaxOffice(blankToNull(request.getTaxOffice()));
        if (request.getTaxNumber() != null) s.setTaxNumber(blankToNull(request.getTaxNumber()));
        if (request.getLogoBase64() != null) {
            if (request.getLogoBase64().isBlank()) {
                s.setLogo(null);
                s.setLogoContentType(null);
            } else {
                byte[] logo;
                try {
                    logo = Base64.getDecoder().decode(request.getLogoBase64());
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("Logo base64 cozulemedi.", "COMPANY_LOGO_INVALID");
                }
                if (logo.length > MAX_LOGO_BYTES) {
                    throw new BusinessException(
                            "Logo en fazla 500KB olabilir (kucuk bir antet gorseli yeterli).",
                            "COMPANY_LOGO_TOO_BIG");
                }
                s.setLogo(logo);
                if (request.getLogoContentType() != null) {
                    s.setLogoContentType(blankToNull(request.getLogoContentType()));
                }
            }
        }
        s.setUpdatedAt(LocalDateTime.now());
        CompanySettings saved = companySettingsRepository.save(s);
        log.info("CompanySettings updated: name={}, logo={}B",
                saved.getName(), saved.getLogo() == null ? 0 : saved.getLogo().length);
        return CompanySettingsResponse.fromEntity(saved);
    }

    private CompanySettings getOrCreate() {
        return companySettingsRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> companySettingsRepository.save(
                        CompanySettings.builder().name("").build()));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
