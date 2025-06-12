package kr.hhplus.be.server.interfaces.web.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.point.PointService;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.interfaces.web.point.dto.request.PointChargeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PointService pointService;

    @Test
    void 사용자의_포인트를_조회한다() throws Exception {
        // given
        Long userId = 1L;
        Long pointVolume = 10000L;
        Point point = Point.create(userId, pointVolume);
        given(pointService.getPoint(userId)).willReturn(point);

        // when & then
        mockMvc.perform(get("/api/v1/points/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.point").value(pointVolume))
                .andExpect(jsonPath("$.message").value("포인트 조회 성공"));
    }

    @Test
    void 사용자의_포인트를_충전한다() throws Exception {
        // given
        Long userId = 1L;
        Long chargeAmount = 5000L;
        Long totalPoint = 15000L;
        Point point = Point.create(userId, totalPoint);
        PointChargeRequest request = new PointChargeRequest(userId, chargeAmount);

        given(pointService.chargePoint(anyLong(), any())).willReturn(point);

        // when & then
        mockMvc.perform(post("/api/v1/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.point").value(totalPoint))
                .andExpect(jsonPath("$.message").value("포인트 충전 성공"));
    }
} 