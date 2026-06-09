package org.example.motoadvisor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.dto.RecommendationDto;
import org.example.motoadvisor.dto.RiderPreferencesDto;
import org.example.motoadvisor.persistence.entity.Motorcycle;
import org.example.motoadvisor.persistence.repository.MotorcycleRepository;
import org.example.motoadvisor.service.RecommendationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Deterministic rule-based recommendation implementation.
 *
 * <p>Scoring rules (each contributing to a 0–100 score):
 * <ul>
 *   <li>+40 pts — experience level matches exactly</li>
 *   <li>+30 pts — category matches (or preference is ANY)</li>
 *   <li>+20 pts — engine size falls within requested range</li>
 *   <li>+10 pts — price is within budget (or no budget set)</li>
 * </ul>
 * Only motorcycles with score > 0 are returned.
 * Results are ordered highest score first.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final MotorcycleRepository motorcycleRepository;

    @Override
    @Transactional
    public List<RecommendationDto> recommend(RiderPreferencesDto preferences) {
        log.debug("Computing recommendations for preferences: {}", preferences);


        List<Motorcycle> all = motorcycleRepository.findAll();

        List<RecommendationDto> results = all.stream()
                .filter(m -> matchesHardConstraints(m, preferences))
                .map(m -> score(m, preferences))
                .filter(dto -> dto.getMatchScore() > 0)
                .sorted(Comparator.comparingInt(RecommendationDto::getMatchScore).reversed())
                .toList();

        log.info("Found {} recommendations for experienceLevel={}, category={}",
                results.size(),
                preferences.getExperienceLevel(),
                preferences.getPreferredCategory());

        return results;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean matchesHardConstraints(Motorcycle m, RiderPreferencesDto p) {
        if (p.getExperienceLevel() != null
                && !p.getExperienceLevel().isBlank()
                && !p.getExperienceLevel().equalsIgnoreCase(m.getExperienceLevel())) {
            return false;
        }

        int maxCc = p.getMaxEngineSizeCc() == 0 ? Integer.MAX_VALUE : p.getMaxEngineSizeCc();
        if (m.getEngineSizeCc() < p.getMinEngineSizeCc() || m.getEngineSizeCc() > maxCc) {
            return false;
        }

        if (p.getMaxBudgetEur() > 0 && m.getPriceEur() > p.getMaxBudgetEur()) {
            return false;
        }

        if (!"ANY".equalsIgnoreCase(p.getPreferredCategory())
                && !p.getPreferredCategory().equalsIgnoreCase(m.getCategory())) {
            return false;
        }

        return p.getPreferredBrand() == null
                || p.getPreferredBrand().isBlank()
                || p.getPreferredBrand().equalsIgnoreCase(m.getBrand());
    }

    private RecommendationDto score(Motorcycle m, RiderPreferencesDto p) {
        int score = 0;

        // Experience level match (+40)
        if (p.getExperienceLevel().equalsIgnoreCase(m.getExperienceLevel())) {
            score += 40;
        }

        // Category match (+30)
        if ("ANY".equalsIgnoreCase(p.getPreferredCategory())
                || p.getPreferredCategory().equalsIgnoreCase(m.getCategory())) {
            score += 30;
        }

        // Engine size range (+20)
        int maxCc = p.getMaxEngineSizeCc() == 0 ? Integer.MAX_VALUE : p.getMaxEngineSizeCc();
        if (m.getEngineSizeCc() >= p.getMinEngineSizeCc() && m.getEngineSizeCc() <= maxCc) {
            score += 20;
        }

        // Budget (+10)
        if (p.getMaxBudgetEur() == 0 || m.getPriceEur() <= p.getMaxBudgetEur()) {
            score += 10;
        }

        return RecommendationDto.builder()
                .motorcycleId(m.getId())
                .brand(m.getBrand())
                .name(m.getName())
                .category(m.getCategory())
                .engineSizeCc(m.getEngineSizeCc())
                .priceEur(m.getPriceEur())
                .seatHeightMm(m.getSeatHeightMm())
                .weightKg(m.getWeightKg())
                .experienceLevel(m.getExperienceLevel())
                .matchScore(score)
                .build();
    }
}

