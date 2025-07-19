package kr.hhplus.be.server.interfaces.web.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.application.coupon.CouponService;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.DiscountType;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.interfaces.web.coupon.dto.request.CouponIssueRequest;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponListResponse;
import kr.hhplus.be.server.interfaces.web.coupon.dto.response.CouponResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void 쿠폰을_발급_요청한다() throws Exception {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        CouponIssueRequest request = new CouponIssueRequest(userId, couponId);

        willDoNothing().given(couponService).issueCoupon(anyLong(), anyLong());

        // when & then
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("쿠폰 발급 요청 성공"));
    }

    @Test
    void 사용자의_쿠폰_목록을_조회한다() throws Exception {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();
        
        CouponResponse coupon1 = new CouponResponse(
            1L,
            "3000원 할인 쿠폰",
            DiscountType.AMOUNT,
            3000L,
            now.plusDays(30)
        );
        
        CouponResponse coupon2 = new CouponResponse(
            2L,
            "10프로 할인 쿠폰",
            DiscountType.PERCENT,
            10L,
            now.plusDays(30)
        );

        List<CouponResponse> coupons = Arrays.asList(coupon1, coupon2);
        CouponListResponse response = CouponListResponse.of(coupons);

        given(couponService.getUserCoupons(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/coupons", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("쿠폰 목록 조회 성공"))
                .andExpect(jsonPath("$.data.coupons").isArray())
                .andExpect(jsonPath("$.data.coupons.length()").value(2))
                
                // 첫 번째 쿠폰 검증
                .andExpect(jsonPath("$.data.coupons[0].id").value(1))
                .andExpect(jsonPath("$.data.coupons[0].title").value("3000원 할인 쿠폰"))
                .andExpect(jsonPath("$.data.coupons[0].discountType").value("AMOUNT"))
                .andExpect(jsonPath("$.data.coupons[0].discountValue").value(3000))
                .andExpect(jsonPath("$.data.coupons[0].expiredAt").exists())
                
                // 두 번째 쿠폰 검증
                .andExpect(jsonPath("$.data.coupons[1].id").value(2))
                .andExpect(jsonPath("$.data.coupons[1].title").value("10프로 할인 쿠폰"))
                .andExpect(jsonPath("$.data.coupons[1].discountType").value("PERCENT"))
                .andExpect(jsonPath("$.data.coupons[1].discountValue").value(10))
                .andExpect(jsonPath("$.data.coupons[1].expiredAt").exists());
    }

    @Test
    void 사용자의_쿠폰_목록이_없는_경우_빈_배열을_반환한다() throws Exception {
        // given
        Long userId = 1L;
        CouponListResponse emptyResponse = CouponListResponse.of(List.of());
        
        given(couponService.getUserCoupons(userId)).willReturn(emptyResponse);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/coupons", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("쿠폰 목록 조회 성공"))
                .andExpect(jsonPath("$.data.coupons").isArray())
                .andExpect(jsonPath("$.data.coupons.length()").value(0));
    }
}
