package com.drugs.infrastructure.business;

import com.drugs.controller.dto.*;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.mapper.DrugsMapper;
import com.drugs.infrastructure.database.repository.DrugsRepository;
import com.drugs.infrastructure.mail.EmailService;
import com.drugs.infrastructure.util.DateUtils;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@AllArgsConstructor
public class DrugsService {

    private final DrugsRepository drugsRepository;
    private final DrugsFormService drugsFormService;
    private final DrugsMapper drugsMapper;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(DrugsService.class);

    public void addNewDrug(DrugsRequestDTO dto) {
        DrugsEntity entity = DrugsEntity.builder()
                .drugsName(dto.getName())
                .drugsForm(drugsFormService.resolve(DrugsFormDTO.valueOf(dto.getForm()))).expirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()))
                .drugsDescription(dto.getDescription())
                .build();

        drugsRepository.save(entity);
    }

    public void deleteDrug(Integer id) {
        DrugsEntity entity = drugsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Drug not found: " + id));
        drugsRepository.delete(entity);
    }

    public DrugsDTO updateDrug(Integer id, DrugsRequestDTO dto) {
        DrugsEntity entity = drugsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Drug not found: " + id));

        entity.setDrugsName(dto.getName());
        entity.setDrugsForm(drugsFormService.resolve(DrugsFormDTO.valueOf(dto.getForm())));
        entity.setExpirationDate(DateUtils.buildExpirationDate(dto.getExpirationYear(), dto.getExpirationMonth()));
        entity.setDrugsDescription(dto.getDescription());

        DrugsEntity saved = drugsRepository.save(entity);

        return drugsMapper.mapToDTO(saved);
    }

    public List<DrugsDTO> getDrugsByName(String name) {
        return drugsRepository.findAllByDrugsNameIgnoreCase(name).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
    }

    public List<DrugsDTO> getDrugsExpiringSoon(int year, int month) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime until = DateUtils.buildExpirationDate(year, month);
        return drugsRepository.findByExpirationDateBetweenOrderByExpirationDateAsc(now, until).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
    }

    public List<DrugsDTO> getExpiredDrugs() {
        OffsetDateTime now = OffsetDateTime.now();
        return drugsRepository.findAll().stream()
                .filter(drug -> drug.getExpirationDate() != null && drug.getExpirationDate().isBefore(now))
                .map(drugsMapper::mapToDTO)
                .sorted(Comparator.comparing(DrugsDTO::getExpirationDate)) // opcjonalne: sortowanie od najstarszych
                .toList();
    }

    public List<DrugSimpleDTO> getAllDrugsSimple() {
        return drugsRepository.findAll().stream()
                .map(drugsMapper::mapToSimpleDTO)
                .toList();
    }

//    public List<DrugsEntity> getAllDrugs() {
//        return drugsRepository.findAll().stream()
//                .sorted(Comparator.comparing(DrugsEntity::getExpirationDate, Comparator.nullsLast(Comparator.naturalOrder())))
//                .toList();
//    }

    public DrugsDTO getDrugById(Integer id) {
        DrugsEntity entity = drugsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Drug not found, id: " + id));
        return drugsMapper.mapToDTO(entity);
    }

    public Page<DrugsDTO> getDrugsPaged(Pageable pageable) {
        return drugsRepository.findAll(pageable)
                .map(drugsMapper::mapToDTO);
    }

    public List<DrugsDTO> searchByDescription(String text) {
        return drugsRepository.findByDrugsDescriptionIgnoreCaseContaining(text).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
    }

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
                    emailService.sendEmail(
                            "recipient@example.com", // W przypadku testów może to być mockowane
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
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime oneMonthLater = now.plusMonths(1);
        sendExpiryAlertEmails(now, oneMonthLater);
    }

    public DrugStatisticsDTO getDrugStatistics() {
        long total = drugsRepository.count();
        long expired = drugsRepository.countByExpirationDateBefore(OffsetDateTime.now());
        long alertsSent = drugsRepository.countByAlertSentTrue();
        List<Object[]> rawStats = drugsRepository.countGroupedByForm();
        Map<String, Long> stats = mapGroupedByForm(rawStats);
        long activeDrugs = total - expired;

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

    public List<DrugsDTO> getAllSorted(String sortBy) {
        Sort sort = Sort.by(sortBy).ascending(); // można dodać opcję descending jako rozszerzenie
        return drugsRepository.findAll(sort).stream()
                .map(drugsMapper::mapToDTO)
                .toList();
    }
}