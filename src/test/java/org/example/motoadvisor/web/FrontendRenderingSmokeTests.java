package org.example.motoadvisor.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.jade.enabled=false")
@AutoConfigureMockMvc
class FrontendRenderingSmokeTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageRendersWithSharedStyling() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/app.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Moto Advisor")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Find recommendations")));
    }

    @Test
    void adminPagesRenderWithUpdatedLayout() throws Exception {
        mockMvc.perform(get("/admin/import"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Operations dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload CSV")));

        mockMvc.perform(get("/admin/agent-logs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Agent communication logs")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Latest log entries")));
    }

    @Test
    void searchResultsPageRendersRecommendationsView() throws Exception {
        mockMvc.perform(post("/search")
                        .param("criteria.experienceLevel", "BEGINNER")
                        .param("criteria.preferredCategory", "ANY")
                        .param("criteria.minEngineSizeCc", "0")
                        .param("criteria.maxEngineSizeCc", "0")
                        .param("criteria.maxBudgetEur", "0")
                        .param("criteria.preferredBrand", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Recommended motorcycles")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("resultsTable")));
    }
}

