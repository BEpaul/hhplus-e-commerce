package kr.hhplus.be.server.interfaces.web.bestseller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.bestseller.BestSellerService;
import kr.hhplus.be.server.domain.bestseller.BestSeller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BestSellerController.class)
public class BestSellerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BestSellerService bestSellerService;

    private List<BestSeller> mockBestSellers;

    @BeforeEach
    void setUp() {
        mockBestSellers = List.of(
            BestSeller.builder()
                .id(1L)
                .name("메론킥")
                .productId(1L)
                .ranking(1L)
                .topDate(LocalDateTime.now())
                .build(),
            BestSeller.builder()
                .id(2L)
                .name("엄마손")
                .productId(2L)
                .ranking(2L)
                .topDate(LocalDateTime.now())
                .build(),
            BestSeller.builder()
                .id(3L)
                .name("깡깡깡")
                .productId(3L)
                .ranking(3L)
                .topDate(LocalDateTime.now())
                .build(),
            BestSeller.builder()
                .id(4L)
                .name("스파게티")
                .productId(4L)
                .ranking(4L)
                .topDate(LocalDateTime.now())
                .build(),
            BestSeller.builder()
                .id(5L)
                .name("한우")
                .productId(5L)
                .ranking(5L)
                .topDate(LocalDateTime.now())
                .build()
        );
    }

    @Test
    void 상위_상품_목록_조회에_성공한다() throws Exception {
        // given
        given(bestSellerService.getTopProducts(any())).willReturn(mockBestSellers);

        // when & then
        mockMvc.perform(get("/api/v1/bestsellers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("상위 상품 목록 조회 성공"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].name").value("메론킥"))
            .andExpect(jsonPath("$.data[0].productId").value(1))
            .andExpect(jsonPath("$.data[0].ranking").value(1))
            .andExpect(jsonPath("$.data[1].id").value(2))
            .andExpect(jsonPath("$.data[1].name").value("엄마손"))
            .andExpect(jsonPath("$.data[1].productId").value(2))
            .andExpect(jsonPath("$.data[1].ranking").value(2))
            .andExpect(jsonPath("$.data[2].id").value(3))
            .andExpect(jsonPath("$.data[2].name").value("깡깡깡"))
            .andExpect(jsonPath("$.data[2].productId").value(3))
            .andExpect(jsonPath("$.data[2].ranking").value(3))
            .andExpect(jsonPath("$.data[3].id").value(4))
            .andExpect(jsonPath("$.data[3].name").value("스파게티"))
            .andExpect(jsonPath("$.data[3].productId").value(4))
            .andExpect(jsonPath("$.data[3].ranking").value(4))
            .andExpect(jsonPath("$.data[4].id").value(5))
            .andExpect(jsonPath("$.data[4].name").value("한우"))
            .andExpect(jsonPath("$.data[4].productId").value(5))
            .andExpect(jsonPath("$.data[4].ranking").value(5));
    }
}
