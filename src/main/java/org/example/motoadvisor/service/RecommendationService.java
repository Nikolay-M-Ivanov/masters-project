package org.example.motoadvisor.service;

import org.example.motoadvisor.dto.RecommendationDto;
import org.example.motoadvisor.dto.RiderPreferencesDto;

import java.util.List;

/**
 * Core advisor service.
 * Takes rider preferences and returns an ordered list of motorcycle recommendations,
 * highest match score first.
 */
public interface RecommendationService {

    /**
     * Produce a ranked list of motorcycle recommendations for the given preferences.
     *
     * @param preferences validated rider preferences from the web form
     * @return ordered list (best match first); empty list when no matches found
     */
    List<RecommendationDto> recommend(RiderPreferencesDto preferences);
}

