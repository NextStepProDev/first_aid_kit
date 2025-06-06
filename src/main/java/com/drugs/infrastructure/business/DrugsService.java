package com.drugs.infrastructure.business;

import com.drugs.controller.dto.*;
import com.drugs.controller.exception.DrugNotFoundException;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.mapper.DrugsMapper;
import com.drugs.infrastructure.database.repository.DrugsRepository;
import com.drugs.infrastructure.mail.EmailService;
import com.drugs.infrastructure.util.DateUtils;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DrugsService {

    private final DrugsRepository drugsRepository;
    private final DrugsFormService drugsFormService;
    private final DrugsMapper drugsMapper;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(DrugsService.class);

    @CacheEvict(value = {"allDrugs", "simpleDrugs", "drugById", "drugsByName", "expiredDrugs", "expiringDrugs", "sortedDrugs"}, allEntries = true)
    public void addNewDrug(DrugsRequestDTO dto) {
        logger.info("Attempting to add a new drug: {}", dto.getName());

        DrugsEntity entity = DrugsEntity.builder()
                .drugsName(dto.getName())
                .drugsForm(drugsFormService.resolve(DrugsFormDTO.valueOf(dto.getForm())))
                .expirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()))
                .drugsDescription(dto.getDescription())
                .build();

        drugsRepository.save(entity);
        logger.info("Successfully added the drug: {}", dto.getName());
    }

    @CacheEvict(value = {"allDrugs", "simpleDrugs", "drugById", "drugsByName", "expiredDrugs", "expiringDrugs", "sortedDrugs"}, allEntries = true)
    public void deleteDrug(Integer id) {
        logger.info("Attempting to delete drug with ID: {}", id);
        DrugsEntity entity = drugsRepository.findById(id)
                .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));

        drugsRepository.delete(entity);
        logger.info("Successfully deleted drug with ID: {}", id);
    }

    @CacheEvict(value = {"allDrugs", "simpleDrugs", "drugById", "drugsByName", "expiredDrugs", "expiringDrugs", "sortedDrugs"}, allEntries = true)
    public DrugsDTO updateDrug(Integer id, DrugsRequestDTO dto) {
        logger.info("Attempting to update drug with ID: {}", id);

        DrugsEntity entity = drugsRepository.findById(id)
                .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));

        entity.setDrugsName(dto.getName());
        entity.setDrugsForm(drugsFormService.resolve(DrugsFormDTO.valueOf(dto.getForm())));
        entity.setExpirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()));
        entity.setDrugsDescription(dto.getDescription());

        DrugsEntity saved = drugsRepository.save(entity);

        logger.info("Successfully updated drug with ID: {}", id);
        return drugsMapper.mapToDTO(saved);
    }

    @Cacheable(value = "drugsByName", key = "#name")
    public List<DrugsDTO> getDrugsByName(String name) {
        logger.info("Fetching drugs with name: {}", name);

        List<DrugsDTO> drugs = drugsRepository.findByDrugsNameContainingIgnoreCase(name).stream()
                .map(drugsMapper::mapToDTO)
                .collect(Collectors.toList());

        logger.info("Found {} drugs with name: {}", drugs.size(), name);
        return drugs;
    }

    @Cacheable(value = "expiringDrugs", key = "#year + '-' + #month")
    public List<DrugsDTO> getDrugsExpiringSoon(int year, int month) {
        logger.info("Fetching drugs expiring soon between year: {} and month: {}", year, month);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime until = DateUtils.buildExpirationDate(year, month);
        List<DrugsDTO> drugs = drugsRepository.findByExpirationDateBetweenOrderByExpirationDateAsc(now, until).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
        logger.info("Found {} drugs expiring soon.", drugs.size());
        return drugs;
    }

    @Cacheable("expiredDrugs")
    public List<DrugsDTO> getExpiredDrugs() {
        logger.info("Fetching expired drugs.");

        OffsetDateTime now = OffsetDateTime.now();
        List<DrugsDTO> expiredDrugs = drugsRepository.findAll().stream()
                .filter(drug -> drug.getExpirationDate() != null && drug.getExpirationDate().isBefore(now))
                .map(drugsMapper::mapToDTO)
                .sorted(Comparator.comparing(DrugsDTO::getExpirationDate)) // opcjonalne: sortowanie od najstarszych
                .collect(Collectors.toList());

        logger.info("Found {} expired drugs.", expiredDrugs.size());
        return expiredDrugs;
    }

    @Cacheable("simpleDrugs")
    public List<DrugSimpleDTO> getAllDrugsSimple() {
        logger.info("Fetching all simple drug data.");
        List<DrugSimpleDTO> drugs = drugsRepository.findAll().stream()
                .map(drugsMapper::mapToSimpleDTO)
                .toList();
        logger.info("Found {} drugs.", drugs.size());
        return drugs;
    }

    @Cacheable(value = "drugById", key = "#id")
    public DrugsDTO getDrugById(Integer id) {
        logger.info("Fetching drug with ID: {}", id);
        DrugsEntity entity = null;
        try {
            entity = drugsRepository.findById(id)
                    .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));
        } catch (ResponseStatusException e) {
            logger.error("Drug not found with ID: {}", id);
            throw e;
        }
        logger.info("Found drug with ID: {}", id);
        return drugsMapper.mapToDTO(entity);
    }

    public Page<DrugsDTO> getDrugsPaged(Pageable pageable) {
        logger.info("Fetching paged drugs with page: {}", pageable.getPageNumber());
        Page<DrugsDTO> drugs = drugsRepository.findAll(pageable)
                .map(drugsMapper::mapToDTO);
        logger.info("Found {} drugs on page: {}", drugs.getContent().size(), pageable.getPageNumber());
        return drugs;
    }

    public List<DrugsDTO> searchByDescription(String text) {
        logger.info("Searching drugs by description: {}", text);
        List<DrugsDTO> drugs = drugsRepository.findByDrugsDescriptionIgnoreCaseContaining(text).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
        logger.info("Found {} drugs with description containing: {}", drugs.size(), text);
        return drugs;
    }

    @Cacheable("allDrugs")
    public List<DrugsDTO> getAllDrugs() {
        return drugsRepository.findAll()
                .stream()
                .map(drugsMapper::mapToDTO)
                .toList();
    }

    public void sendExpiryAlertEmails(OffsetDateTime start, OffsetDateTime end) {
        logger.info("Sending expiry alert emails for drugs expiring between {} and {}", start, end);

        List<DrugsEntity> drugs = drugsRepository.findByExpirationDateBetweenAndAlertSentFalse(start, end);

        logger.info("Found {} drugs to send alerts for", drugs.size());

        for (DrugsEntity drug : drugs) {
            logger.info("Sending alert for drug: {}", drug.getDrugsName());

            if (!drug.getAlertSent()) {
                try {
                    // Wysyłanie e-maila
                    emailService.sendEmail("recipient@example.com", // W przypadku testów może to być mockowane
                            "Drug Expiry Alert",
                            "This is a reminder that the drug " + drug.getDrugsName() + " is about to expire."
                    );

                    // Markujemy lek jako powiadomiony
                    drug.setAlertSent(true);
                    drugsRepository.save(drug);
                    logger.info("Alert sent and drug marked as notified: {}", drug.getDrugsName());
                } catch (Exception e) {
                    logger.error("Failed to send alert for drug: {}", drug.getDrugsName(), e);
                }
            } else {
                logger.info("Drug already notified: {}", drug.getDrugsName());
            }
        }
    }

    public void sendDefaultExpiryAlertEmails() {
        logger.info("Sending default expiry alert emails.");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime oneMonthLater = now.plusMonths(1);
        sendExpiryAlertEmails(now, oneMonthLater);
    }

    public DrugStatisticsDTO getDrugStatistics() {
        logger.info("Fetching drug statistics.");

        long total = drugsRepository.count();
        long expired = drugsRepository.countByExpirationDateBefore(OffsetDateTime.now());
        long alertsSent = drugsRepository.countByAlertSentTrue();
        List<Object[]> rawStats = drugsRepository.countGroupedByForm();
        Map<String, Long> stats = mapGroupedByForm(rawStats);
        long activeDrugs = total - expired;

        logger.info("Statistics fetched: Total Drugs: {}, Expired Drugs: {}, Active Drugs: {}, Alerts Sent: {}",
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

    @Cacheable(value = "sortedDrugs", key = "#sortBy")
    public List<DrugsDTO> getAllSorted(String sortBy) {
        Sort sort = Sort.by(sortBy).ascending(); // można dodać opcję descending jako rozszerzenie
        return drugsRepository.findAll(sort).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
    }
}