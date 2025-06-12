package kr.hhplus.be.server.interfaces.web.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.interfaces.web.coupon.dto.request.CouponIssueRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @Test
    void 쿠폰을_발급한다() throws Exception {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        Long userCouponId = 1L;
        CouponIssueRequest request = new CouponIssueRequest(userId, couponId);

        UserCoupon userCoupon = UserCoupon.builder()
                .id(userCouponId)
                .build();

        given(couponService.issueCoupon(anyLong(), anyLong())).willReturn(userCoupon);

        // when & then
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.couponId").value(userCouponId))
                .andExpect(jsonPath("$.message").value("쿠폰 발급 성공"));
    }
}
