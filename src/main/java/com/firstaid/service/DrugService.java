package com.firstaid.service;

import com.firstaid.controller.dto.DrugDTO;
import com.firstaid.controller.dto.DrugFormDTO;
import com.firstaid.controller.dto.DrugRequestDTO;
import com.firstaid.controller.dto.DrugStatisticsDTO;
import com.firstaid.domain.exception.DrugNotFoundException;
import com.firstaid.domain.exception.EmailSendingException;
import com.firstaid.infrastructure.database.entity.DrugEntity;
import com.firstaid.infrastructure.database.entity.DrugFormEntity;
import com.firstaid.infrastructure.database.entity.UserEntity;
import com.firstaid.infrastructure.database.mapper.DrugMapper;
import com.firstaid.infrastructure.database.repository.DrugRepository;
import com.firstaid.infrastructure.database.repository.UserRepository;
import com.firstaid.infrastructure.email.EmailService;
import com.firstaid.infrastructure.security.CurrentUserService;
import com.firstaid.infrastructure.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

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
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} attempting to add a new drug: {}", userId, dto.getName());

        DrugFormEntity form = drugFormService.resolve(parseForm(dto.getForm()));
        UserEntity owner = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));

        DrugEntity entity = DrugEntity.builder()
                .drugName(dto.getName())
                .drugForm(form)
                .owner(owner)
                .expirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()))
                .drugDescription(dto.getDescription())
                .build();

        DrugEntity saved = drugRepository.save(entity);
        log.info("User {} successfully added the drug: {}", userId, dto.getName());

        return drugMapper.mapToDTO(saved);
    }

    @Cacheable(value = "drugById", keyGenerator = "userAwareCacheKeyGenerator")
    public DrugDTO getDrugById(Integer id) {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} fetching drug with ID: {}", userId, id);

        DrugEntity entity = drugRepository.findByDrugIdAndOwnerUserId(id, userId)
                .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));
        log.info("User {} found drug with ID: {}", userId, id);
        return drugMapper.mapToDTO(entity);
    }

    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public void deleteDrug(Integer id) {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} attempting to delete drug with ID: {}", userId, id);

        DrugEntity entity = drugRepository.findByDrugIdAndOwnerUserId(id, userId)
                .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));

        drugRepository.delete(entity);
        log.info("User {} successfully deleted drug with ID: {}", userId, id);
    }

    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public void updateDrug(Integer id, DrugRequestDTO dto) {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} attempting to update drug with ID: {}", userId, id);

        DrugEntity entity = drugRepository.findByDrugIdAndOwnerUserId(id, userId)
                .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));

        entity.setDrugName(dto.getName());
        entity.setDrugForm(drugFormService.resolve(parseForm(dto.getForm())));
        entity.setExpirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()));
        entity.setDrugDescription(dto.getDescription());

        drugRepository.save(entity);
        log.info("User {} successfully updated drug with ID: {}", userId, id);
    }

    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public void sendExpiryAlertEmailsForCurrentUser(int year, int month) {
        Integer userId = currentUserService.getCurrentUserId();
        String userEmail = currentUserService.getCurrentUserEmail();
        log.info("User {} sending expiry alert emails for drugs expiring up to {}/{}", userId, year, month);

        OffsetDateTime endInclusive = DateUtils.buildExpirationDate(year, month);
        List<DrugEntity> expiringDrugs =
                drugRepository.findByOwnerUserIdAndExpirationDateLessThanEqualAndAlertSentFalse(userId, endInclusive);

        sendAlertsForDrugs(expiringDrugs, userEmail);
    }

    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public void sendDefaultExpiryAlertEmailsForCurrentUser() {
        log.info("Sending default expiry alert emails for current user.");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime oneMonthLater = now.plusMonths(1);
        sendExpiryAlertEmailsForCurrentUser(oneMonthLater.getYear(), oneMonthLater.getMonthValue());
    }

    /**
     * Scheduled task: sends alerts to all users with drugs expiring within 1 month.
     * Runs daily at 9:00 AM.
     * This method is NOT user-scoped - it processes all users.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public void sendScheduledExpiryAlerts() {
        log.info("Scheduler: Running daily expiry alert task at 9:00 AM");
        OffsetDateTime oneMonthLater = OffsetDateTime.now().plusMonths(1);
        sendExpiryAlertEmailsForAllUsers(oneMonthLater.getYear(), oneMonthLater.getMonthValue());
    }

    @CacheEvict(value = { "drugById", "drugsSearch", "drugStatistics" }, allEntries = true)
    public void sendExpiryAlertEmailsForAllUsers(int year, int month) {
        log.info("Scheduler: Sending expiry alert emails for all users for drugs expiring up to {}/{}", year, month);

        OffsetDateTime endInclusive = DateUtils.buildExpirationDate(year, month);
        List<Integer> userIds = drugRepository.findDistinctOwnerIdsWithExpiringDrugs(endInclusive);

        log.info("Found {} users with expiring drugs", userIds.size());

        for (Integer userId : userIds) {
            UserEntity user = userRepository.findByUserId(userId).orElse(null);
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                log.warn("Skipping user {} - no valid email address", userId);
                continue;
            }

            List<DrugEntity> expiringDrugs =
                    drugRepository.findByOwnerUserIdAndExpirationDateLessThanEqualAndAlertSentFalse(userId, endInclusive);

            sendAlertsForDrugs(expiringDrugs, user.getEmail());
        }
    }

    private void sendAlertsForDrugs(List<DrugEntity> drugs, String recipientEmail) {
        log.info("Found {} drugs to send alerts for to {}", drugs.size(), recipientEmail);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("No recipient email provided. Skipping email sending.");
            return;
        }

        // Filter only drugs that haven't been alerted yet
        List<DrugEntity> drugsToAlert = drugs.stream()
                .filter(drug -> !drug.isAlertSent())
                .toList();

        if (drugsToAlert.isEmpty()) {
            log.info("All drugs already notified. Skipping email sending.");
            return;
        }

        // Build consolidated email with all drugs
        String subject = "Drug Expiry Alert - " + drugsToAlert.size() + " drug(s) expiring soon";
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Attention!\n\n");
        messageBuilder.append("The following drugs in your first aid kit are about to expire:\n\n");

        for (int i = 0; i < drugsToAlert.size(); i++) {
            DrugEntity drug = drugsToAlert.get(i);
            messageBuilder.append(String.format("%d. %s%n", i + 1, drug.getDrugName()));
            messageBuilder.append(String.format("   Expiration date: %s%n", drug.getExpirationDate().toLocalDate()));
            if (drug.getDrugDescription() != null && !drug.getDrugDescription().isBlank()) {
                messageBuilder.append(String.format("   Description: %s%n", drug.getDrugDescription()));
            }
            messageBuilder.append("\n");
        }

        messageBuilder.append("Please check these items as soon as possible!\n\n");
        messageBuilder.append("Take care of your health!\n");
        messageBuilder.append("Your First Aid Kit");

        try {
            emailService.sendEmail(recipientEmail, subject, messageBuilder.toString());
            log.info("Consolidated alert sent for {} drugs to {}", drugsToAlert.size(), recipientEmail);

            // Mark all drugs as alerted
            for (DrugEntity drug : drugsToAlert) {
                drug.setAlertSent(true);
                drugRepository.save(drug);
                log.debug("Drug marked as notified: {}", drug.getDrugName());
            }
            log.info("All {} drugs marked as notified", drugsToAlert.size());
        } catch (Exception e) {
            log.error("Failed to send consolidated alert email to {}", recipientEmail, e);
            throw new EmailSendingException("Could not send consolidated email alert", e);
        }
    }

    @Cacheable(value = "drugStatistics", keyGenerator = "userAwareCacheKeyGenerator")
    public DrugStatisticsDTO getDrugStatistics() {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} fetching drug statistics.", userId);

        long total = drugRepository.countByOwnerUserId(userId);
        long expired = drugRepository.countByOwnerUserIdAndExpirationDateBefore(userId, OffsetDateTime.now());
        long alertsSent = drugRepository.countByOwnerUserIdAndAlertSentTrue(userId);
        List<Object[]> rawStats = drugRepository.countGroupedByFormAndUserId(userId);
        Map<String, Long> stats = mapGroupedByForm(rawStats);
        long activeDrugs = total - expired;

        log.info("User {} statistics fetched: Total Drugs: {}, Expired Drugs: {}, Active Drugs: {}, Alerts Sent: {}",
                userId, total, expired, activeDrugs, alertsSent);

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
        keyGenerator = "userAwareCacheKeyGenerator",
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
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} searching drugs with filters: name={}, form={}, expired={}, expirationUntilYear={}, expirationUntilMonth={}, pageable={}",
                userId, name, form, expired, expirationUntilYear, expirationUntilMonth, pageable);

        name = (name != null && !name.isBlank()) ? name.trim() : "";

        OffsetDateTime now = OffsetDateTime.now();
        // If only year is provided, assume December (end of year)
        if (expirationUntilYear != null && expirationUntilMonth == null) {
            expirationUntilMonth = 12;
            log.debug("Only year provided ({}). Defaulting month to December (12).", expirationUntilYear);
        }
        // If only month is provided, assume current year
        if (expirationUntilYear == null && expirationUntilMonth != null) {
            expirationUntilYear = now.getYear();
            log.debug("Only month provided ({}). Defaulting year to current year ({}).", expirationUntilMonth, expirationUntilYear);
        }
        OffsetDateTime expirationUntil =
                (expirationUntilYear != null)
                        ? DateUtils.buildExpirationDate(expirationUntilYear, expirationUntilMonth)
                        : null;

        DrugFormEntity formEntity = null;
        if (form != null && !form.isBlank()) {
            DrugFormDTO formEnum = parseForm(form);
            formEntity = drugFormService.resolve(formEnum);
        }
        String nameLower = name.toLowerCase();

        // Always filter by current user (multi-tenancy)
        Specification<DrugEntity> spec = (root, query, cb) ->
                cb.equal(root.get("owner").get("userId"), userId);

        if (!nameLower.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("drugName")), "%" + nameLower + "%"));
        }
        if (formEntity != null) {
            DrugFormEntity finalFormEntity = formEntity;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("drugForm"), finalFormEntity));
        }
        if (expired != null) {
            if (expired) {
                spec = spec.and((root, query, cb) -> cb.lessThan(root.get("expirationDate"), now));
            } else {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expirationDate"), now));
            }
        }
        if (expirationUntil != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("expirationDate"), expirationUntil));
        }
        Page<DrugEntity> entityResult = drugRepository.findAll(spec, pageable);
        return entityResult.map(drugMapper::mapToDTO);
    }
}