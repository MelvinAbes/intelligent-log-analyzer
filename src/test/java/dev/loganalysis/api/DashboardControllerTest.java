package dev.loganalysis.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import(RequestCorrelationFilter.class)
class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void rendersDashboardWithRestrictiveBrowserHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Log Analysis")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(
                        header().string(
                                        "Content-Security-Policy",
                                        org.hamcrest.Matchers.containsString(
                                                "frame-ancestors 'none'")));
    }
}
