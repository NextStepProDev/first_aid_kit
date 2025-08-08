package com.firstaid.infrastructure.configuration;

import com.firstaid.controller.dto.DrugFormDTO;
import com.firstaid.infrastructure.database.entity.DrugEntity;
import com.firstaid.infrastructure.database.repository.DrugRepository;
import com.firstaid.infrastructure.util.DateUtils;
import com.firstaid.service.DrugFormService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!test")
@AllArgsConstructor
@jakarta.annotation.Generated("bootstrap")
@SuppressWarnings("unused")
public class BootstrapApplicationComponent implements ApplicationListener<ContextRefreshedEvent> {

    private final DrugRepository drugRepository;
    private final DrugFormService drugFormService;
    private final JdbcTemplate jdbcTemplate;

    // To jest bardzo ciekawy mechanizm. Ta klasa powoduje, że podczas wstawania spring możemy się wpiąć do niego,
    // żeby zrobił dla nas kilka rzeczy. W tym przypadku podczas uruchamiania kontekstu będą wykonywane poniższe rzeczy

    @Override
    @Transactional
    public void onApplicationEvent(final @NonNull ContextRefreshedEvent event) {


        drugRepository.deleteAll();
        drugRepository.flush();

        resetSequence();

        insertDrug("Altacet", DrugFormDTO.GEL, 2025, 4, "Lek przeciwbólowy w formie żelu.");
//        insertDrug("Centrum Junior", DrugFormDTO.PILLS, 2025, 4, "Witaminy dla dzieci");
        insertDrug("Helicid 20", DrugFormDTO.PILLS, 2025, 6, "lek zawierający omeprazol, " +
                "inhibitor pompy protonowej, który zmniejsza wydzielanie " +
                "kwasu solnego w żołądku. Stosowany jest w leczeniu choroby refluksowej przełyku, owrzodzeń " +
                "żołądka i dwunastnicy, eradykacji Helicobacter pylori");
        insertDrug("Xylometazolin", DrugFormDTO.DROPS, 2025, 7, "pełne otwarte opakowanie, " +
                "do nosa");
        insertDrug("Procto-Hemolan", DrugFormDTO.CREAM, 2025, 7, "Krem doodbytniczy");
        insertDrug("Perskindol", DrugFormDTO.GEL, 2025, 12, "Chłodząco - rozgrzewający na " +
                "bóle mięśni");
        insertDrug("Zinnat", DrugFormDTO.PILLS, 2026, 1, "stosowany w leczeniu " +
                "różnorodnych zakażeń bakteryjnych u dorosłych i dzieci. " +
                "Zakażenia górnych dróg oddechowych, takie jak zapalenie gardła, zatok i ucha środkowego. " +
                "Zakażenia dolnych dróg oddechowych, w tym zapalenie oskrzeli i płuc. Zakażenia układu moczowego.");
        insertDrug("Naproxen 500 Hasco", DrugFormDTO.PILLS, 2026, 2, "Koncówka");
        insertDrug("Ospen 1000", DrugFormDTO.PILLS, 2026, 3, "Antybiotyk zawierający " +
                "fenoksymetylopenicylinę, zakażenia górnych i dolnych dróg " +
                "oddechowych (np. angina, zapalenie migdałków, zapalenie gardła, zapalenie ucha środkowego, " +
                "zapalenie zatok);");
        insertDrug("Mugga", DrugFormDTO.LIQUID, 2026, 4, "Pełny");
        insertDrug("KickFly", DrugFormDTO.SPRAY, 2026, 4, "komry, kleszce, meszki");
        insertDrug("Proktosedon", DrugFormDTO.SUPPOSITORIES, 2026, 9, "hemoroidy");
        insertDrug("Frenadol", DrugFormDTO.SACHETS, 2026, 10, "Paracetamol, kofeina i inne");
        insertDrug("Ibuprom", DrugFormDTO.PILLS, 2026, 12, "nie trzeba przedstawiać, pełny prawie");
        insertDrug("Septanazal", DrugFormDTO.DROPS, 2027, 1, "Septanazal dla dorosłych obkurcza naczynia krwionośne nosa i zmniejsza obrzęk błony śluzowej nosa oraz ilość wydzieliny. Łagodzi uczucie zatkanego nosa.");
        insertDrug("Biofenac 100mg", DrugFormDTO.PILLS, 2027, 2, "końcówka, NLPZ stosowany w leczeniu bólu i stanów zapalnych związanych z chorobami " +
                "reumatycznymi i zwyrodnieniowymi stawów, takimi jak osteoartroza, reumatoidalne zapalenie " +
                "stawów oraz zesztywniające zapalenie stawów kręgosłupa.");
        insertDrug("Hydrocortisonum", DrugFormDTO.CREAM, 2027, 3, "Glikokortykosteroid o słabym działaniu przeciwzapalnym i przeciwświądowym.");
        insertDrug("Voltaren Sport", DrugFormDTO.GEL, 2027, 3, "Stosowany miejscowo, działa przeciwbólowo, przeciwzapalnie i przeciwobrzękowo.");
        insertDrug("Argo Tiab", DrugFormDTO.SPRAY, 2027, 4, "Z cząsteczkami srebra, na zranioną skórę");
        insertDrug("Acne-Derm", DrugFormDTO.CREAM, 2027, 4, "Do uzupełnienia, trądzik");
        insertDrug("Nimesil", DrugFormDTO.SACHETS, 2027, 5, "Połowa opakowania");
//        insertDrug("Broncho tos", DrugFormDTO.SYRUP, 2027, 7, "Kaszel suchy, mokry");
        insertDrug("Traumon", DrugFormDTO.GEL, 2027, 10, "Przeciwbólowy, przeciwzapalny");
//        insertDrug("Teraflu zatoki", DrugFormDTO.SACHETS, 2027, 10, "4 saszetki");
        insertDrug("Clatra", DrugFormDTO.PILLS, 2028, 1, "lek przeciwhistaminowy zawierający bilastynę, stosowany w leczeniu objawów " +
                "alergicznego zapalenia błony śluzowej nosa i spojówek. 1 tab na dobę");
        insertDrug("Pimafucort", DrugFormDTO.OINTMENT, 2028, 4, "zapalenie skóry, wyprysk, łuszczyca,łojotokowe zapalenie skóry");
        insertDrug("Ketonal", DrugFormDTO.PILLS, 2029, 3, "zapalenie skóry, wyprysk, łuszczyca,łojotokowe zapalenie skóry");
        insertDrug("Mugga", DrugFormDTO.LIQUID, 2029, 3, "Prawie pełny");
        insertDrug("Erdomed 300 mg", DrugFormDTO.PILLS, 2029, 4, "Erdomed to lek mukolityczny zawierający erdosteinę, stosowany w leczeniu ostrych" +
                " i przewlekłych schorzeń dróg oddechowych, takich jak zapalenie oskrzeli, którym towarzyszy" +
                " nadmierne i lepkie wydzielanie śluzu.");
    }

    private void insertDrug(String name, DrugFormDTO form, int year, int month, String description) {
        drugRepository.save(DrugEntity.builder()
                .drugName(name)
                .drugForm(drugFormService.resolve(form))
                .expirationDate(DateUtils.buildExpirationDate(year, month))
                .drugDescription(description)
                .build());
    }

    private void resetSequence() {
        String resetQuery = "ALTER SEQUENCE drugs_drug_id_seq RESTART WITH 1;";
        jdbcTemplate.execute(resetQuery);
        log.info("Sequence for drugs_drug_id_seq reset to 1.");
    }
}
