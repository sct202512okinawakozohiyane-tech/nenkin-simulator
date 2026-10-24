package com.example.pension;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerTest {

    @Autowired
    private MockMvc mvc;

    private static String body(String lifeExpectancy, String specialExpenses) {
        return """
            {"startAge":60,"lifeExpectancy":%s,"retirementAge":65,"publicPensionStartAge":65,
             "basicLivingCost":200000,"leisureCost":100000,"salaryAfter60":300000,"publicPension":250000,
             "dcBalance":10000000,"dcStartAge":60,"dcLumpSumAmount":0,"dcAnnuityYears":20,
             "nisaBalance":5000000,"otherBalance":5000000,
             "inflationRate":0.02,"dcReturnRate":0.05,"returnVolatility":0.15,
             "withdrawalOrder":["NASDAQ","OTHER"],"specialExpenses":%s,
             "salaryNetRate":0.8,"pensionNetRate":0.9,"dcLumpSumNetRate":0.95,"dcAnnuityNetRate":0.85}
            """.formatted(lifeExpectancy, specialExpenses);
    }

    private static String specialExpenses(int n) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"category\":\"その他\",\"label\":\"x\",\"fromAge\":60,\"toAge\":70,\"annualAmount\":100000}");
        }
        return sb.append(']').toString();
    }

    @Test
    void validRequestSucceeds() throws Exception {
        mvc.perform(post("/api/simulate").contentType(MediaType.APPLICATION_JSON).content(body("90", "[]")))
            .andExpect(status().isOk());
    }

    @Test
    void hugeLifeExpectancyIsRejected() throws Exception {
        mvc.perform(post("/api/simulate").contentType(MediaType.APPLICATION_JSON).content(body("2147483647", "[]")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void startAgeBelow60IsRejected() throws Exception {
        String req = body("90", "[]").replace("\"startAge\":60", "\"startAge\":59");
        mvc.perform(post("/api/simulate").contentType(MediaType.APPLICATION_JSON).content(req))
            .andExpect(status().isBadRequest());
    }

    @Test
    void tooManySpecialExpensesAreRejected() throws Exception {
        mvc.perform(post("/api/simulate").contentType(MediaType.APPLICATION_JSON).content(body("90", specialExpenses(11))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Security-Policy"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
