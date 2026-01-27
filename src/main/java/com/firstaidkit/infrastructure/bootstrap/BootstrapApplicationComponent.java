package com.firstaidkit.infrastructure.bootstrap;

import com.firstaidkit.controller.dto.drug.DrugFormDTO;
import com.firstaidkit.infrastructure.database.entity.DrugEntity;
import com.firstaidkit.infrastructure.database.entity.RoleEntity;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.repository.DrugRepository;
import com.firstaidkit.infrastructure.database.repository.RoleRepository;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import com.firstaidkit.infrastructure.util.DateUtils;
import com.firstaidkit.service.DrugFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Set;

@Slf4j
@Component
@Profile({"!test", "dev"})
@RequiredArgsConstructor
@jakarta.annotation.Generated("bootstrap")
public class BootstrapApplicationComponent implements SmartApplicationListener {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return ApplicationReadyEvent.class.isAssignableFrom(eventType);
    }

    @Value("${spring.profiles.active:}")
    private String profile;

    private final DrugRepository drugRepository;
    private final DrugFormService drugFormService;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // To jest bardzo ciekawy mechanizm. Ta klasa powoduje, że podczas wstawania spring możemy się wpiąć do niego,
    // żeby zrobił dla nas kilka rzeczy. W tym przypadku podczas uruchamiania kontekstu będą wykonywane poniższe rzeczy

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        // Ensure Flyway migrations are run before seeding data

        if ("dev".equals(profile)) {
            resetUserSequences();
        }

        // 1. CZYŚCIMY (CASCADE załatwi relacje, jeśli jakieś są)
        jdbcTemplate.execute("TRUNCATE TABLE app_user_role CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE app_user CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE role CASCADE");

        // 2. RESETUJEMY LICZNIKI (żeby Mateusz miał ID=1)
        jdbcTemplate.execute("ALTER SEQUENCE app_user_user_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE role_role_id_seq RESTART WITH 1");

        // 3. TWORZYMY ROLE
        RoleEntity adminRole = new RoleEntity();
        adminRole.setRole("ADMIN");
        roleRepository.save(adminRole);

        RoleEntity userRole = new RoleEntity();
        userRole.setRole("USER");
        roleRepository.save(userRole);

        // 4. DODAJEMY TWOJEGO ADMINA (djdefkon)
        UserEntity admin = new UserEntity();
        admin.setUserName("Mateusz");
        admin.setEmail("djdefkon@gmail.com");
        // Używamy passwordEncoder, żeby było bezpiecznie i spójnie
        admin.setPassword(passwordEncoder.encode("test"));
        admin.setName("Mateusz Nawratek");
        admin.setActive(true);
        admin.setCreatedAt(OffsetDateTime.now());
        admin.setRole(Set.of(adminRole, userRole));

        userRepository.save(admin);

        // Get default owner (first user - admin)
        jdbcTemplate.queryForList("SELECT * FROM app_user").forEach(row ->
                log.info("DB row: {}", row)
        );
        UserEntity defaultOwner = userRepository.findByUserId(1)
                .orElseThrow(() -> new IllegalStateException("Default user (id=1) not found. Please ensure the database has been seeded."));

        drugRepository.deleteAll();
        drugRepository.flush();

        resetSequence();

        insertDrug("Altacet", DrugFormDTO.GEL, 2025, 4, "Lek przeciwbólowy w formie żelu.", defaultOwner);
        insertDrug("Centrum Junior", DrugFormDTO.PILLS, 2025, 4, "Witaminy dla dzieci", defaultOwner);
        insertDrug("Helicid 20", DrugFormDTO.PILLS, 2025, 6, "lek zawierający omeprazol, " +
                "inhibitor pompy protonowej, który zmniejsza wydzielanie " +
                "kwasu solnego w żołądku. Stosowany jest w leczeniu choroby refluksowej przełyku, owrzodzeń " +
                "żołądka i dwunastnicy, eradykacji Helicobacter pylori", defaultOwner);
        insertDrug("Xylometazolin", DrugFormDTO.DROPS, 2025, 7, "pełne otwarte opakowanie, " +
                "do nosa", defaultOwner);
        insertDrug("Procto-Hemolan", DrugFormDTO.CREAM, 2025, 7, "Krem doodbytniczy", defaultOwner);
        insertDrug("Perskindol", DrugFormDTO.GEL, 2025, 12, "Chłodząco - rozgrzewający na " +
                "bóle mięśni", defaultOwner);
        insertDrug("Zinnat", DrugFormDTO.PILLS, 2026, 1, "stosowany w leczeniu " +
                "różnorodnych zakażeń bakteryjnych u dorosłych i dzieci. " +
                "Zakażenia górnych dróg oddechowych, takie jak zapalenie gardła, zatok i ucha środkowego. " +
                "Zakażenia dolnych dróg oddechowych, w tym zapalenie oskrzeli i płuc. Zakażenia układu moczowego.", defaultOwner);
        insertDrug("Naproxen 500 Hasco", DrugFormDTO.PILLS, 2026, 2, "Koncówka", defaultOwner);
        insertDrug("Ospen 1000", DrugFormDTO.PILLS, 2026, 3, "Antybiotyk zawierający " +
                "fenoksymetylopenicylinę, zakażenia górnych i dolnych dróg " +
                "oddechowych (np. angina, zapalenie migdałków, zapalenie gardła, zapalenie ucha środkowego, " +
                "zapalenie zatok);", defaultOwner);
        insertDrug("Mugga", DrugFormDTO.LIQUID, 2026, 4, "Pełny", defaultOwner);
        insertDrug("KickFly", DrugFormDTO.SPRAY, 2026, 4, "komry, kleszce, meszki", defaultOwner);
        insertDrug("Proktosedon", DrugFormDTO.SUPPOSITORIES, 2026, 9, "hemoroidy", defaultOwner);
        insertDrug("Frenadol", DrugFormDTO.SACHETS, 2026, 10, "Paracetamol, kofeina i inne", defaultOwner);
        insertDrug("Ibuprom", DrugFormDTO.PILLS, 2026, 12, "nie trzeba przedstawiać, pełny prawie", defaultOwner);
        insertDrug("Septanazal", DrugFormDTO.DROPS, 2027, 1, "Septanazal dla dorosłych obkurcza naczynia krwionośne nosa i zmniejsza obrzęk błony śluzowej nosa oraz ilość wydzieliny. Łagodzi uczucie zatkanego nosa.", defaultOwner);
        insertDrug("Biofenac 100mg", DrugFormDTO.PILLS, 2027, 2, "końcówka, NLPZ stosowany w leczeniu bólu i stanów zapalnych związanych z chorobami " +
                "reumatycznymi i zwyrodnieniowymi stawów, takimi jak osteoartroza, reumatoidalne zapalenie " +
                "stawów oraz zesztywniające zapalenie stawów kręgosłupa.", defaultOwner);
        insertDrug("Hydrocortisonum", DrugFormDTO.CREAM, 2027, 3, "Glikokortykosteroid o słabym działaniu przeciwzapalnym i przeciwświądowym.", defaultOwner);
        insertDrug("Voltaren Sport", DrugFormDTO.GEL, 2027, 3, "Stosowany miejscowo, działa przeciwbólowo, przeciwzapalnie i przeciwobrzękowo.", defaultOwner);
        insertDrug("Argo Tiab", DrugFormDTO.SPRAY, 2027, 4, "Z cząsteczkami srebra, na zranioną skórę", defaultOwner);
        insertDrug("Acne-Derm", DrugFormDTO.CREAM, 2027, 4, "Do uzupełnienia, trądzik", defaultOwner);
        insertDrug("Nimesil", DrugFormDTO.SACHETS, 2027, 5, "Połowa opakowania", defaultOwner);
//        insertDrug("Broncho tos", DrugFormDTO.SYRUP, 2027, 7, "Kaszel suchy, mokry", defaultOwner);
        insertDrug("Traumon", DrugFormDTO.GEL, 2027, 10, "Przeciwbólowy, przeciwzapalny", defaultOwner);
//        insertDrug("Teraflu zatoki", DrugFormDTO.SACHETS, 2027, 10, "4 saszetki", defaultOwner);
        insertDrug("Clatra", DrugFormDTO.PILLS, 2028, 1, "lek przeciwhistaminowy zawierający bilastynę, stosowany w leczeniu objawów " +
                "alergicznego zapalenia błony śluzowej nosa i spojówek. 1 tab na dobę", defaultOwner);
        insertDrug("Pimafucort", DrugFormDTO.OINTMENT, 2028, 4, "zapalenie skóry, wyprysk, łuszczyca,łojotokowe zapalenie skóry", defaultOwner);
        insertDrug("Ketonal", DrugFormDTO.PILLS, 2029, 3, "zapalenie skóry, wyprysk, łuszczyca,łojotokowe zapalenie skóry", defaultOwner);
        insertDrug("Mugga", DrugFormDTO.LIQUID, 2029, 3, "Prawie pełny", defaultOwner);
        insertDrug("Erdomed 300 mg", DrugFormDTO.PILLS, 2029, 4, "Erdomed to lek mukolityczny zawierający erdosteinę, stosowany w leczeniu ostrych" +
                " i przewlekłych schorzeń dróg oddechowych, takich jak zapalenie oskrzeli, którym towarzyszy" +
                " nadmierne i lepkie wydzielanie śluzu.", defaultOwner);
    }

    private void insertDrug(String name, DrugFormDTO form, int year, int month, String description, UserEntity owner) {
        drugRepository.save(DrugEntity.builder()
                .drugName(name)
                .drugForm(drugFormService.resolve(form))
                .expirationDate(DateUtils.buildExpirationDate(year, month))
                .drugDescription(description)
                .owner(owner)
                .build());
    }

    private void resetSequence() {
        String resetQuery = "ALTER SEQUENCE drugs_drug_id_seq RESTART WITH 1;";
        jdbcTemplate.execute(resetQuery);
        log.info("Sequence for drugs_drug_id_seq reset to 1.");
    }

    private void resetUserSequences() {
        jdbcTemplate.execute("SELECT setval('app_user_user_id_seq', (SELECT MAX(user_id) FROM app_user))");
        jdbcTemplate.execute("SELECT setval('role_role_id_seq', (SELECT MAX(role_id) FROM role))");
        log.info("User and role sequences synchronized.");
    }
}