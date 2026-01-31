package com.firstaidkit.service;

import com.firstaidkit.controller.dto.drug.DrugCreateRequest;
import com.firstaidkit.controller.dto.drug.DrugFormDTO;
import com.firstaidkit.controller.dto.drug.DrugResponse;
import com.firstaidkit.controller.dto.drug.DrugStatistics;
import com.firstaidkit.domain.exception.DrugNotFoundException;
import com.firstaidkit.domain.exception.EmailSendingException;
import com.firstaidkit.domain.exception.InvalidPasswordException;
import com.firstaidkit.infrastructure.database.entity.DrugEntity;
import com.firstaidkit.infrastructure.database.entity.DrugFormEntity;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.mapper.DrugMapper;
import com.firstaidkit.infrastructure.database.repository.DrugRepository;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import com.firstaidkit.infrastructure.email.EmailService;
import com.firstaidkit.infrastructure.security.CurrentUserService;
import com.firstaidkit.infrastructure.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.OffsetDateTime.now;

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
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;


    @Caching(evict = {@CacheEvict(value = {"drugsSearch", "drugStatistics"}, allEntries = true), @CacheEvict(value = "drugById", keyGenerator = "userAwareCacheKeyGenerator")})
    public DrugResponse addNewDrug(DrugCreateRequest dto) {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} attempting to add a new drug: {}", userId, dto.getName());

        DrugFormEntity form = drugFormService.resolve(DrugFormDTO.fromString(dto.getForm()));
        UserEntity owner = userService.getUserOrThrow(userId);

        DrugEntity entity = DrugEntity.builder().drugName(dto.getName()).drugForm(form).owner(owner).expirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth())).drugDescription(dto.getDescription()).build();

        DrugEntity saved = drugRepository.save(entity);
        log.info("User {} successfully added the drug: {}", userId, dto.getName());

        return drugMapper.mapToDTO(saved);
    }

    @Cacheable(value = "drugById", keyGenerator = "userAwareCacheKeyGenerator")
    public DrugResponse getDrugById(Integer id) {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} fetching drug with ID: {}", userId, id);

        DrugEntity entity = drugRepository.findByDrugIdAndOwnerUserId(id, userId).orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));
        log.info("User {} found drug with ID: {}", userId, id);
        return drugMapper.mapToDTO(entity);
    }

    @Caching(evict = {@CacheEvict(value = {"drugsSearch", "drugStatistics"}, allEntries = true), @CacheEvict(value = "drugById", keyGenerator = "userAwareCacheKeyGenerator")})
    public void deleteDrug(Integer id) {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} attempting to delete drug with ID: {}", userId, id);

        DrugEntity entity = drugRepository.findByDrugIdAndOwnerUserId(id, userId).orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));

        drugRepository.delete(entity);
        log.info("User {} successfully deleted drug with ID: {}", userId, id);
    }

    @Transactional
    @CacheEvict(value = {"drugById", "drugsSearch", "drugStatistics"}, allEntries = true)
    public long deleteAllDrugs(String password) {
        Integer userId = currentUserService.getCurrentUserId();
        String userEmail = currentUserService.getCurrentUserEmail();
        log.info("User {} attempting to delete all drugs", userEmail);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Invalid password provided by user {} for delete all drugs operation", userEmail);
            throw new InvalidPasswordException("Invalid password");
        }

        long count = drugRepository.countByOwnerUserId(userId);
        drugRepository.deleteAllByOwnerUserId(userId);
        log.info("User {} successfully deleted all {} drugs", userEmail, count);

        return count;
    }

    @Caching(evict = {@CacheEvict(value = {"drugsSearch", "drugStatistics"}, allEntries = true), @CacheEvict(value = "drugById", keyGenerator = "userAwareCacheKeyGenerator")})
    public void updateDrug(Integer id, DrugCreateRequest dto) {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} attempting to update drug with ID: {}", userId, id);

        DrugEntity entity = drugRepository.findByDrugIdAndOwnerUserId(id, userId).orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));

        entity.setDrugName(dto.getName());
        entity.setDrugForm(drugFormService.resolve(DrugFormDTO.fromString(dto.getForm())));
        entity.setExpirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()));
        entity.setDrugDescription(dto.getDescription());

        drugRepository.save(entity);
        log.info("User {} successfully updated drug with ID: {}", userId, id);
    }

    /**
     * Scheduled task: sends alerts to all users with drugs expiring within 1 month.
     * Runs daily at 9:00 AM.
     * This method is NOT user-scoped - it processes all users.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendScheduledExpiryAlerts() {
        log.info("Scheduler: Running daily expiry alert task at 9:00 AM");
        OffsetDateTime now = now();
        sendExpiryAlertEmailsForAllUsers(now.getYear(), now.getMonthValue());
    }

    @CacheEvict(value = {"drugsSearch", "drugStatistics", "drugById"}, allEntries = true)
    public void sendExpiryAlertEmailsForAllUsers(int year, int month) {
        log.info("Scheduler: Sending expiry alert emails for all users for drugs expiring up to {}/{}", year, month);

        OffsetDateTime endInclusive = DateUtils.buildExpirationDate(year, month);
        List<Integer> userIds = drugRepository.findDistinctOwnerIdsWithExpiringDrugs(endInclusive);

        log.info("Found {} users with expiring drugs", userIds.size());

        for (Integer userId : userIds) {
            UserEntity user = userRepository.findByUserId(userId).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getAlertsEnabled()) || user.getEmail() == null || user.getEmail().isBlank()) {
                log.warn("Skipping user {} - alerts disabled or no valid email address", userId);
                continue;
            }

            List<DrugEntity> expiringDrugs = drugRepository.findByOwnerUserIdAndExpirationDateLessThanEqualAndAlertSentFalse(userId, endInclusive);

            try {
                sendAlertsForDrugs(expiringDrugs, user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send alert for user {}: {}", userId, e.getMessage());
            }
        }
    }

    private int sendAlertsForDrugs(List<DrugEntity> drugs, String recipientEmail) {
        log.info("Found {} drugs to send alerts for to {}", drugs.size(), recipientEmail);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("No recipient email provided. Skipping email sending.");
            return 0;
        }

        List<DrugEntity> drugsToAlert = drugs.stream().filter(drug -> !drug.isAlertSent()).toList();

        if (drugsToAlert.isEmpty()) {
            log.info("All drugs already notified. Skipping email sending.");
            return 0;
        }

        String subject = "\uD83D\uDC8A Drug Expiry Alert - " + drugsToAlert.size() + " drug(s) expiring soon";
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("⚠️ Attention!\n\n");
        messageBuilder.append("The following drugs in your first aid kit are about to expire! ⏳\n");
        messageBuilder.append("Please check them as soon as possible before it's too late! ❌\n\n");

        for (int i = 0; i < drugsToAlert.size(); i++) {
            DrugEntity drug = drugsToAlert.get(i);
            messageBuilder.append(String.format("%d. %s%n", i + 1, drug.getDrugName()));
            messageBuilder.append(String.format("   \uD83D\uDCC5 Expiration date: %s%n", drug.getExpirationDate().toLocalDate()));
            if (drug.getDrugDescription() != null && !drug.getDrugDescription().isBlank()) {
                messageBuilder.append(String.format("   \uD83D\uDCDD Description: %s%n", drug.getDrugDescription()));
            }
            messageBuilder.append("\n");
        }

        messageBuilder.append("✅ Please check these items and replace expired drugs!\n\n");
        messageBuilder.append("\uD83D\uDC9A Take care of your health!\n");
        messageBuilder.append("Your First Aid Kit \uD83C\uDFE5");

        try {
            emailService.sendEmail(recipientEmail, subject, messageBuilder.toString());
            log.info("Consolidated alert sent for {} drugs to {}", drugsToAlert.size(), recipientEmail);

            for (DrugEntity drug : drugsToAlert) {
                drug.setAlertSent(true);
                drugRepository.save(drug);
            }
            return drugsToAlert.size();
        } catch (Exception e) {
            log.error("Failed to send consolidated alert email to {}", recipientEmail, e);
            throw new EmailSendingException("Could not send consolidated email alert", e);
        }
    }

    @Cacheable(value = "drugStatistics", keyGenerator = "userAwareCacheKeyGenerator")
    public DrugStatistics getDrugStatistics() {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} fetching drug statistics.", userId);

        long total = drugRepository.countByOwnerUserId(userId);
        long expired = drugRepository.countByOwnerUserIdAndExpirationDateBefore(userId, DateUtils.startOfToday());
        long alertsSent = drugRepository.countByOwnerUserIdAndAlertSentTrue(userId);
        List<Object[]> rawStats = drugRepository.countGroupedByFormAndUserId(userId);
        Map<String, Long> stats = mapGroupedByForm(rawStats);
        long activeDrugs = total - expired;

        log.info("User {} statistics fetched: Total Drugs: {}, Expired Drugs: {}, Active Drugs: {}, Alerts Sent: {}", userId, total, expired, activeDrugs, alertsSent);

        return DrugStatistics.builder().totalDrugs(total).expiredDrugs(expired).activeDrugs(activeDrugs).alertSentCount(alertsSent).drugsByForm(stats).build();
    }

    private Map<String, Long> mapGroupedByForm(List<Object[]> rawData) {
        return rawData.stream().filter(arr -> arr[0] != null && arr[1] != null).collect(Collectors.toMap(arr -> (String) arr[0], arr -> ((Number) arr[1]).longValue()));
    }

    @Cacheable(value = "drugsSearch", keyGenerator = "userAwareCacheKeyGenerator", condition = "(#name != null && #name.trim().length() > 0) || (#form != null && #form.trim().length() > 0) || (#expired != null) || (#expiringSoon != null) || (#expirationUntilYear != null) || (#expirationUntilMonth != null)", unless = "#result == null || #result.isEmpty()")
    public Page<DrugResponse> searchDrugs(String name, String form, Boolean expired, Boolean expiringSoon, Integer expirationUntilYear, Integer expirationUntilMonth, Pageable pageable) {
        Integer userId = currentUserService.getCurrentUserId();
        log.info("User {} searching drugs with filters: name={}, form={}, expired={}, expiringSoon={}, expirationUntilYear={}, expirationUntilMonth={}, pageable={}", userId, name, form, expired, expiringSoon, expirationUntilYear, expirationUntilMonth, pageable);

        name = (name != null && !name.isBlank()) ? name.trim() : "";

        OffsetDateTime now = DateUtils.startOfToday();
        if (expirationUntilYear != null && expirationUntilMonth == null) {
            expirationUntilMonth = 12;
            log.debug("Only year provided ({}). Defaulting month to December (12).", expirationUntilYear);
        }
        if (expirationUntilYear == null && expirationUntilMonth != null) {
            expirationUntilYear = now.getYear();
            log.debug("Only month provided ({}). Defaulting year to current year ({}).", expirationUntilMonth, expirationUntilYear);
        }
        OffsetDateTime expirationUntil = (expirationUntilYear != null) ? DateUtils.buildExpirationDate(expirationUntilYear, expirationUntilMonth) : null;

        DrugFormEntity formEntity = null;
        if (form != null && !form.isBlank()) {
            DrugFormDTO formEnum = DrugFormDTO.fromString(form);
            formEntity = drugFormService.resolve(formEnum);
        }
        String nameLower = name.toLowerCase();

        // Always filter by current user (multi-tenancy)
        Specification<DrugEntity> spec = (root, query, cb) -> cb.equal(root.get("owner").get("userId"), userId);

        if (!nameLower.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                String pattern = "%" + nameLower + "%";
                return cb.or(cb.like(cb.lower(root.get("drugName")), pattern), cb.like(cb.lower(root.get("drugDescription")), pattern));
            });
        }
        if (formEntity != null) {
            DrugFormEntity finalFormEntity = formEntity;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("drugForm"), finalFormEntity));
        }
        if (Boolean.TRUE.equals(expiringSoon)) {
            OffsetDateTime thirtyDaysFromNow = now.plusDays(30);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expirationDate"), now));
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("expirationDate"), thirtyDaysFromNow));
        } else if (expired != null) {
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