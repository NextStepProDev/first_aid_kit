package com.firstaid.service;

import com.firstaid.controller.dto.DrugDTO;
import com.firstaid.controller.dto.DrugFormDTO;
import com.firstaid.controller.dto.DrugRequestDTO;
import com.firstaid.controller.dto.DrugStatisticsDTO;
import com.firstaid.controller.exception.DrugNotFoundException;
import com.firstaid.controller.exception.EmailSendingException;
import com.firstaid.infrastructure.database.entity.DrugEntity;
import com.firstaid.infrastructure.database.entity.DrugFormEntity;
import com.firstaid.infrastructure.database.mapper.DrugMapper;
import com.firstaid.infrastructure.database.repository.DrugRepository;
import com.firstaid.infrastructure.email.EmailService;
import com.firstaid.infrastructure.util.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugService {

    private final DrugRepository drugRepository;
    private final DrugFormService drugFormService;
    private final DrugMapper drugMapper;
    private final EmailService emailService;

    @Value("${app.alert.recipientEmail:}")
    private String alertRecipientEmail;

    private DrugFormDTO parseForm(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Drug form cannot be null");
        }
        return Arrays.stream(DrugFormDTO.values())
                .filter(e -> e.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid drug form: " + value + ". Allowed: " +
                        Arrays.toString(DrugFormDTO.values())));
    }

    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public DrugDTO addNewDrug(DrugRequestDTO dto) {
        log.info("Attempting to add a new drug: {}", dto.getName());

        DrugFormEntity form = drugFormService.resolve(parseForm(dto.getForm()));

        DrugEntity entity = DrugEntity.builder()
                .drugName(dto.getName())
                .drugForm(form)
                .expirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()))
                .drugDescription(dto.getDescription())
                .build();

        DrugEntity saved = drugRepository.save(entity);
        log.info("Successfully added the drug: {}", dto.getName());

        return drugMapper.mapToDTO(saved);
    }

    @Cacheable(value = "drugById", key = "#id")
    public DrugDTO getDrugById(Integer id) {
        log.info("Fetching drug with ID: {}", id);
        DrugEntity entity;

        entity = drugRepository.findById(id)
                .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));
        log.info("Found drug with ID: {}", id);
        return drugMapper.mapToDTO(entity);
    }

    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public void deleteDrug(Integer id) {
        log.info("Attempting to delete drug with ID: {}", id);
        DrugEntity entity = drugRepository.findById(id)
                .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));

        drugRepository.delete(entity);
        log.info("Successfully deleted drug with ID: {}", id);
    }

    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public DrugDTO updateDrug(Integer id, DrugRequestDTO dto) {
        log.info("Attempting to update drug with ID: {}", id);
        DrugEntity entity;
        entity = drugRepository.findById(id)
                .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));
        entity.setDrugName(dto.getName());
        entity.setDrugForm(drugFormService.resolve(parseForm(dto.getForm())));
        entity.setExpirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()));
        entity.setDrugDescription(dto.getDescription());

        DrugEntity saved = drugRepository.save(entity);

        log.info("Successfully updated drug with ID: {}", id);
        return drugMapper.mapToDTO(saved);
    }

    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public void sendExpiryAlertEmails(int year, int month) {
        log.info("Sending expiry alert emails for drugs expiring up to {}/{}", year, month);

        OffsetDateTime endInclusive = DateUtils.buildExpirationDate(year, month);
        List<DrugEntity> expiringDrugs =
                drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(endInclusive);

        log.info("Found {} drugs to send alerts for", expiringDrugs.size());

        if (alertRecipientEmail == null || alertRecipientEmail.isBlank()) {
            log.warn("No alert recipient configured (app.alert.recipientEmail). Skipping email sending.");
            return;
        }

        for (DrugEntity drug : expiringDrugs) {
            log.info("Sending alert for drug: {}", drug.getDrugName());

            if (!drug.isAlertSent()) {
                String subject = "💊 Drug Expiry Alert";
                String message = """
                        ⚠️ Attention!
                        
                        The drug *%s* in your first aid kit is about to expire! ⏳
                        Please check it as soon as possible before it's too late! ❌
                        
                        🗓️ Expiration date: %s
                        📝 Description: %s
                        
                        Take care of your health! ❤️
                        """.formatted(drug.getDrugName(), drug.getExpirationDate().toLocalDate(),
                        drug.getDrugDescription());
                try {
                    emailService.sendEmail(alertRecipientEmail, subject, message);
                    drug.setAlertSent(true);
                    drugRepository.save(drug);
                    log.info("Alert sent and drug marked as notified: {}", drug.getDrugName());
                } catch (Exception e) {
                    log.error("Failed to send alert for drug: {}", drug.getDrugName(), e);
                    throw new EmailSendingException("Could not send email alert for drug: " + drug.getDrugName(), e);
                }
            } else {
                log.info("Drug already notified: {}", drug.getDrugName());
            }
        }
    }

    public void sendDefaultExpiryAlertEmails() {
        log.info("Sending default expiry alert emails.");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime oneMonthLater = now.plusMonths(1);
        sendExpiryAlertEmails(oneMonthLater.getYear(), oneMonthLater.getMonthValue());
    }

    @Cacheable("drugStatistics")
    public DrugStatisticsDTO getDrugStatistics() {
        log.info("Fetching drug statistics.");

        long total = drugRepository.count();
        long expired = drugRepository.countByExpirationDateBefore(OffsetDateTime.now());
        long alertsSent = drugRepository.countByAlertSentTrue();
        List<Object[]> rawStats = drugRepository.countGroupedByForm();
        Map<String, Long> stats = mapGroupedByForm(rawStats);
        long activeDrugs = total - expired;

        log.info("Statistics fetched: Total Drugs: {}, Expired Drugs: {}, Active Drugs: {}, Alerts Sent: {}",
                total, expired, activeDrugs, alertsSent);

        return DrugStatisticsDTO.builder()
                .totalDrugs(total)
                .expiredDrugs(expired)
                .activeDrugs(activeDrugs)
                .alertSentCount(alertsSent)
                .drugsByForm(stats)
                .build();
    }

    private Map<String, Long> mapGroupedByForm(List<Object[]> rawData) {
        return rawData.stream()
                .filter(arr -> arr[0] != null && arr[1] != null)
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> ((Number) arr[1]).longValue()
                ));
    }

    @Cacheable(
        value = "drugsSearch",
        key = "{#name, #form, #expired, #expirationUntilYear, #expirationUntilMonth, #pageable}",
        condition = "(#name != null && #name.trim().length() > 0) || (#form != null && #form.trim().length() > 0) || (#expired != null) || (#expirationUntilYear != null) || (#expirationUntilMonth != null)",
        unless = "#result == null || #result.isEmpty()"
    )
    public Page<DrugDTO> searchDrugs(
            String name,
            String form,
            Boolean expired,
            Integer expirationUntilYear,
            Integer expirationUntilMonth,
            Pageable pageable
    ) {
        log.info("Searching drugs with filters: name={}, form={}, expired={}, expirationUntilYear={}, expirationUntilMonth={}, pageable={}",
                name, form, expired, expirationUntilYear, expirationUntilMonth, pageable);

        name = (name != null && !name.isBlank()) ? name.trim() : "";

        OffsetDateTime now = OffsetDateTime.now();
        if (expirationUntilYear != null && expirationUntilMonth == null) {
            expirationUntilMonth = 12;
        }
        OffsetDateTime expirationUntil = (expirationUntilYear != null && expirationUntilMonth != null)
                ? DateUtils.buildExpirationDate(expirationUntilYear, expirationUntilMonth)
                : null;

        DrugFormEntity formEntity = null;
        if (form != null && !form.isBlank()) {
            DrugFormDTO formEnum = parseForm(form);
            formEntity = drugFormService.resolve(formEnum);
        }
        String nameLower = name.toLowerCase();
        Specification<DrugEntity> spec = (root, query, cb) -> cb.conjunction();
        if (!nameLower.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("drugName")), "%" + nameLower + "%"));
        }
        if (formEntity != null) {
            DrugFormEntity finalFormEntity = formEntity;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("drugForm"), finalFormEntity));
        }
        if (expired != null) {
            OffsetDateTime finalNow = now;
            if (Boolean.TRUE.equals(expired)) {
                spec = spec.and((root, query, cb) -> cb.lessThan(root.get("expirationDate"), finalNow));
            } else {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expirationDate"), finalNow));
            }
        }
        if (expirationUntil != null) {
            OffsetDateTime finalUntil = expirationUntil;
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("expirationDate"), finalUntil));
        }
        Page<DrugEntity> entityResult = drugRepository.findAll(spec, pageable);
        Page<DrugDTO> result = entityResult.map(drugMapper::mapToDTO);
        return result;
    }
}