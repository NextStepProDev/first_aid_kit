package com.drugs.infrastructure.business;

import com.drugs.controller.dto.*;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.mapper.DrugsMapper;
import com.drugs.infrastructure.database.repository.DrugsRepository;
import com.drugs.infrastructure.mail.EmailService;
import com.drugs.infrastructure.util.DateUtils;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public void sendExpiryAlertEmails() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime oneMonthLater = now.plusMonths(1);

        List<DrugsEntity> expiringDrugs = drugsRepository.findByExpirationDateBetween(now, oneMonthLater);

        for (DrugsEntity drug : expiringDrugs) {
            String subject = "📢 Lek bliski terminu ważności";
            String body = String.format("""
                Nazwa leku: %s
                Termin ważności: %s
                Opis: %s
                """, drug.getDrugsName(), drug.getExpirationDate().toLocalDate(), drug.getDrugsDescription());

            // Wyślij na obie skrzynki
            emailService.sendEmail("djdefkon@gmail.com", subject, body);
            emailService.sendEmail("paula.konarska@gmail.com", subject, body);
        }
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
}