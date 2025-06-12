package kr.hhplus.be.server.interfaces.web.point;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.common.response.ApiResponse;
import kr.hhplus.be.server.interfaces.web.point.dto.request.PointChargeRequest;
import kr.hhplus.be.server.interfaces.web.point.dto.response.PointResponse;
import kr.hhplus.be.server.domain.point.Point;
import kr.hhplus.be.server.application.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "포인트", description = "포인트 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointService pointService;

    @Operation(summary = "포인트 조회", description = "사용자의 포인트를 조회합니다.")
    @GetMapping("/{userId}")
    public ApiResponse<PointResponse> getPoints(@PathVariable Long userId) {
        Point point = pointService.getPoint(userId);

        return ApiResponse.success(PointResponse.from(point.getVolume()), "포인트 조회 성공");
    }

    @Operation(summary = "포인트 충전", description = "사용자의 포인트를 충전합니다.")
    @PostMapping("/charge")
    public ApiResponse<PointResponse> chargePoints(@RequestBody PointChargeRequest pointChargeRequest) {
        Point point = pointService.chargePoint(pointChargeRequest.getUserId(), pointChargeRequest.getAmount());

        return ApiResponse.success(PointResponse.from(point.getVolume()), "포인트 충전 성공");
    }
}
