package kr.hhplus.be.server.interfaces.web.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.infrastructure.persistence.point.PointRepository;
import kr.hhplus.be.server.interfaces.web.point.dto.request.PointChargeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PointControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PointRepository pointRepository;

    private Point savedPoint;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        Point point = Point.create(1L, 10000L);
        savedPoint = pointRepository.save(point);
    }

    @Test
    void 사용자의_포인트를_조회한다() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/points/{userId}", savedPoint.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.point").value(savedPoint.getVolume()))
                .andExpect(jsonPath("$.message").value("포인트 조회 성공"));
    }

    @Test
    void 사용자의_포인트를_충전한다() throws Exception {
        // given
        Long chargeAmount = 5000L;
        PointChargeRequest request = new PointChargeRequest(savedPoint.getUserId(), chargeAmount);

        // when & then
        mockMvc.perform(post("/api/v1/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.point").value(15000L))
                .andExpect(jsonPath("$.message").value("포인트 충전 성공"));
    }
}
