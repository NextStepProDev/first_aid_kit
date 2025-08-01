package com.drugs.service;

import com.drugs.controller.dto.*;
import com.drugs.controller.exception.DrugNotFoundException;
import com.drugs.controller.exception.EmailSendingException;
import com.drugs.controller.exception.InvalidSortFieldException;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.entity.DrugsFormEntity;
import com.drugs.infrastructure.database.mapper.DrugsMapper;
import com.drugs.infrastructure.database.repository.DrugsRepository;
import com.drugs.infrastructure.email.EmailService;
import com.drugs.infrastructure.util.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class DrugsService {

    private final DrugsRepository drugsRepository;
    private final DrugsFormService drugsFormService;
    private final DrugsMapper drugsMapper;
    private final EmailService emailService;

    /**
     * Adds a new drug to the database and clears relevant caches.
     *
     * @param dto the drug data transfer object containing drug details
     */

    @CacheEvict(value = {"allDrugs", "simpleDrugs", "drugById", "drugsByName", "expiredDrugs", "expiringDrugs",
            "sortedDrugs"}, allEntries = true)
    public DrugsDTO addNewDrug(DrugsRequestDTO dto) {
        log.info("Attempting to add a new drug: {}", dto.getName());

        DrugsFormEntity form = drugsFormService.resolve(DrugsFormDTO.valueOf(dto.getForm()));

        DrugsEntity entity = DrugsEntity.builder()
                .drugsName(dto.getName())
                .drugsForm(form)
                .expirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()))
                .drugsDescription(dto.getDescription())
                .build();

        DrugsEntity saved = drugsRepository.save(entity);
        log.info("Successfully added the drug: {}", dto.getName());

        return drugsMapper.mapToDTO(saved);
    }

    /**
     * Retrieves a drug by its ID.
     *
     * @param id the ID of the drug to retrieve
     * @return the drug data transfer object
     * @throws DrugNotFoundException if the drug with the given ID does not exist
     */
    @Cacheable(value = "drugById", key = "#id")
    public DrugsDTO getDrugById(Integer id) {
        log.info("Fetching drug with ID: {}", id);
        DrugsEntity entity;
        try {
            entity = drugsRepository.findById(id)
                    .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));
        } catch (ResponseStatusException e) {
            log.error("Drug not found with ID: {}", id);
            throw e;
        }
        log.info("Found drug with ID: {}", id);
        return drugsMapper.mapToDTO(entity);
    }

    /**
     * Deletes a drug by its ID and clears relevant caches.
     *
     * @param id the ID of the drug to delete
     */
    @CacheEvict(value = {"allDrugs", "simpleDrugs", "drugById", "drugsByName", "expiredDrugs", "expiringDrugs", "sortedDrugs"}, allEntries = true)
    public void deleteDrug(Integer id) {
        log.info("Attempting to delete drug with ID: {}", id);
        DrugsEntity entity = drugsRepository.findById(id)
                .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));

        drugsRepository.delete(entity);
        log.info("Successfully deleted drug with ID: {}", id);
    }

    /**
     * Updates an existing drug by its ID and clears relevant caches.
     *
     * @param id  the ID of the drug to update
     * @param dto the drug data transfer object containing updated details
     * @return the updated drug data transfer object
     */
    @CacheEvict(value = {"allDrugs", "simpleDrugs", "drugById", "drugsByName", "expiredDrugs", "expiringDrugs",
            "sortedDrugs"}, allEntries = true)
    public DrugsDTO updateDrug(Integer id, DrugsRequestDTO dto) {
        log.info("Attempting to update drug with ID: {}", id);
        DrugsEntity entity;
        try {
            entity = drugsRepository.findById(id)
                    .orElseThrow(() -> new DrugNotFoundException("Drug not found with ID: " + id));

        } catch (ResponseStatusException e) {
            log.error("Drug not found with ID: {}", id);
            throw e;
        }
        entity.setDrugsName(dto.getName());
        entity.setDrugsForm(drugsFormService.resolve(DrugsFormDTO.valueOf(dto.getForm())));
        entity.setExpirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()));
        entity.setDrugsDescription(dto.getDescription());

        DrugsEntity saved = drugsRepository.save(entity);

        log.info("Successfully updated drug with ID: {}", id);
        return drugsMapper.mapToDTO(saved);
    }

    /**
     * Retrieves a list of drugs by their name, case-insensitive.
     *
     * @param name the name to search for
     * @return a list of drugs matching the name
     */
    @Cacheable(value = "drugsByName", key = "#name")
    public List<DrugsDTO> getDrugsByName(String name) {
        log.info("Fetching drugs with name: {}", name);

        List<DrugsDTO> drugs = drugsRepository.findByDrugsNameContainingIgnoreCase(name).stream()
                .map(drugsMapper::mapToDTO)
                .collect(Collectors.toList());

        log.info("Found {} drugs with name: {}", drugs.size(), name);
        return drugs;
    }

    /**
     * Retrieves a list of drugs that are expiring soon, based on the provided year and month.
     *
     * @param year  the year to check for expiring drugs
     * @param month the month to check for expiring drugs
     * @return a list of drugs expiring soon
     */
    @Cacheable(value = "expiringDrugs", key = "#year + '-' + #month")
    public List<DrugsDTO> getDrugsExpiringSoon(int year, int month) {
        log.info("Fetching drugs expiring soon up to year: {} and month: {}", year, month);
        OffsetDateTime until = DateUtils.buildExpirationDate(year, month);
        List<DrugsDTO> drugs = drugsRepository.findByExpirationDateLessThanEqualOrderByExpirationDateAsc(until).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
        log.info("Found {} drugs expiring soon.", drugs.size());
        return drugs;
    }

    /**
     * Retrieves a list of expired drugs.
     *
     * @return a list of expired drugs
     */
    @Cacheable("expiredDrugs")
    public List<DrugsDTO> getExpiredDrugs() {
        log.info("Fetching expired drugs.");

        OffsetDateTime now = OffsetDateTime.now();
        List<DrugsDTO> expiredDrugs = drugsRepository.findAll().stream()
                .filter(drug -> drug.getExpirationDate() != null && drug.getExpirationDate().isBefore(now))
                .map(drugsMapper::mapToDTO)
                .sorted(Comparator.comparing(DrugsDTO::getExpirationDate))
                .collect(Collectors.toList());

        log.info("Found {} expired drugs.", expiredDrugs.size());
        return expiredDrugs;
    }

    /**
     * Retrieves a simplified list of drugs containing only ID, name, form, and expiration date.
     *
     * @return a list of simplified drug data transfer objects
     */
    @Cacheable("simpleDrugs")
    public List<DrugSimpleDTO> getAllDrugsSimple() {
        log.info("Fetching all simple drug data.");
        List<DrugSimpleDTO> drugs = drugsRepository.findAll().stream()
                .map(drugsMapper::mapToSimpleDTO)
                .toList();
        log.info("Found {} drugs total.", drugs.size());
        return drugs;
    }

    /**
     * Retrieves a paginated list of drugs.
     *
     * @param pageable the pagination information
     * @return a page of drug data transfer objects
     */
    public Page<DrugsDTO> getDrugsPaged(Pageable pageable) {
        log.info("Fetching paged drugs with page: {}", pageable.getPageNumber());
        Page<DrugsDTO> drugs = drugsRepository.findAll(pageable)
                .map(drugsMapper::mapToDTO);
        log.info("Found {} drugs on page: {}", drugs.getContent().size(), pageable.getPageNumber());
        return drugs;
    }

    /**
     * Searches for drugs by their description, case-insensitive.
     *
     * @param text the text to search for in drug descriptions
     * @return a list of drugs whose descriptions contain the given text
     */
    @Cacheable(value = "drugsByDescription", key = "#text")
    public List<DrugsDTO> searchByDescription(String text) {
        log.info("Searching drugs by description: {}", text);
        List<DrugsDTO> drugs = drugsRepository.findByDrugsDescriptionIgnoreCaseContaining(text).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
        log.info("Found {} drugs with description containing: {}", drugs.size(), text);
        return drugs;
    }

    /**
     * Retrieves all drugs from the database.
     *
     * @return a list of all drug data transfer objects
     */
    @Cacheable("allDrugs")
    public List<DrugsDTO> getAllDrugs() {
        return drugsRepository.findAll()
                .stream()
                .map(drugsMapper::mapToDTO)
                .toList();
    }

    /**
     * Sends expiry alert emails for drugs expiring up to the specified year and month.
     *
     * @param year  the year to check for expiring drugs
     * @param month the month to check for expiring drugs
     */
    public void sendExpiryAlertEmails(int year, int month) {
        log.info("Sending expiry alert emails for drugs expiring up to {}/{}", year, month);

        OffsetDateTime endInclusive = DateUtils.buildExpirationDate(year, month);
        List<DrugsEntity> expiringDrugs =
                drugsRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(endInclusive);

        log.info("Found {} drugs to send alerts for", expiringDrugs.size());

        for (DrugsEntity drug : expiringDrugs) {
            log.info("Sending alert for drug: {}", drug.getDrugsName());

            if (!drug.getAlertSent()) {
                String subject = "💊 Drug Expiry Alert";
                String message = """
                        ⚠️ Attention!
                        
                        The drug *%s* in your first aid kit is about to expire! ⏳
                        Please check it as soon as possible before it's too late! ❌
                        
                        🗓️ Expiration date: %s
                        📝 Description: %s
                        
                        Take care of your health! ❤️
                        """.formatted(drug.getDrugsName(), drug.getExpirationDate().toLocalDate(),
                        drug.getDrugsDescription());
                try {
                    emailService.sendEmail("djdefkon@gmail.com", subject, message);
                    emailService.sendEmail("paula.konarska@gmail.com", subject, message);

                    drug.setAlertSent(true);
                    drugsRepository.save(drug);
                    log.info("Alert sent and drug marked as notified: {}", drug.getDrugsName());
                } catch (Exception e) {
                    log.error("Failed to send alert for drug: {}", drug.getDrugsName(), e);
                    throw new EmailSendingException("Could not send email alert for drug: " + drug.getDrugsName(), e);
                }
            } else {
                log.info("Drug already notified: {}", drug.getDrugsName());
            }
        }
    }

    /**
     * Sends default expiry alert emails for drugs expiring in the next month.
     * This method clears relevant caches after sending the emails.
     */
    @CacheEvict(value = {"allDrugs", "simpleDrugs", "drugById", "drugsByName", "expiredDrugs", "expiringDrugs",
            "sortedDrugs"}, allEntries = true)
    public void sendDefaultExpiryAlertEmails() {
        log.info("Sending default expiry alert emails.");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime oneMonthLater = now.plusMonths(1);
        sendExpiryAlertEmails(oneMonthLater.getYear(), oneMonthLater.getMonthValue());
    }

    /**
     * Retrieves statistics about drugs, including total count, expired count, active count, alerts sent, and drugs
     * grouped by form.
     *
     * @return a data transfer object containing drug statistics
     */
    @Cacheable("drugStatistics")
    public DrugStatisticsDTO getDrugStatistics() {
        log.info("Fetching drug statistics.");

        long total = drugsRepository.count();
        long expired = drugsRepository.countByExpirationDateBefore(OffsetDateTime.now());
        long alertsSent = drugsRepository.countByAlertSentTrue();
        List<Object[]> rawStats = drugsRepository.countGroupedByForm();
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

    /**
     * Retrieves a list of drugs by their form.
     *
     * @param form the form of the drugs to retrieve
     * @return a list of drug data transfer objects matching the specified form
     */
    @Cacheable("drugsByForm")
    public List<DrugsDTO> getDrugsByForm(String form) {
        DrugsFormDTO formEnum = Arrays.stream(DrugsFormDTO.values())
                .filter(e -> e.name().equalsIgnoreCase(form))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid drug form: " + form));

        DrugsFormEntity formEntity = drugsFormService.resolve(formEnum);
        List<DrugsEntity> entities = drugsRepository.findByDrugsForm(formEntity);
        return entities.stream()
                .map(drugsMapper::mapToDTO)
                .toList();
    }

    /**
     * Maps raw data grouped by drug form into a map with form names as keys and counts as values.
     *
     * @param rawData the raw data containing form names and counts
     * @return a map with form names as keys and counts as values
     */
    @Cacheable("drugsByForm")
    private Map<String, Long> mapGroupedByForm(List<Object[]> rawData) {
        return rawData.stream()
                .filter(arr -> arr[0] != null && arr[1] != null)
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> ((Number) arr[1]).longValue()
                ));
    }

    /**
     * Retrieves all drugs sorted by the specified field.
     *
     * @param sortBy the field to sort by
     * @return a list of sorted drug data transfer objects
     */
    public List<DrugsDTO> getAllSorted(String sortBy) {
        String field = resolveSortField(sortBy);
        Sort sort = Sort.by(field).ascending(); // można dodać opcję descending jako rozszerzenie
        return drugsRepository.findAll(sort).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
    }

    private String resolveSortField(String sortBy) {
        return switch (sortBy) {
            case "drugsId" -> "drugsId";
            case "drugsName" -> "drugsName";
            case "expirationDate" -> "expirationDate";
            case "drugsForm" -> "drugsForm.name";
            case "drugsDescription" -> "drugsDescription";
            default -> throw new InvalidSortFieldException(sortBy);
        };
    }
}