//C:\Users\russe\Documents\GitHub\Ada\Back\src\main\java\com\ada\proj\controller\TradeController.java
package com.ada.proj.controller;

import com.ada.proj.dto.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.enums.TradeCategory;
import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.TradeLog;
import com.ada.proj.service.TradeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
@Validated
@Tag(name = "거래소", description = "거래 목록/구매/로그 API")
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "거래 목록 추가",
            description = "ADMIN/TEACHER만 거래 아이템을 등록할 수 있습니다.\n\n"
                    + "요청 필드 설명:\n"
                    + "- name: 아이템 이름(상품명).\n"
                    + "- description: 아이템 설명(옵션).\n"
                    + "- price: 포인트 기준 가격(최소 1).\n"
                    + "- stock: 초기 재고 수량(0 이상).\n"
                    + "- active: 판매 활성화 여부(기본 true).\n"
                    + "- category: 카테고리(FOOD | TOOLS | ETC).\n"
                    + "- imageUrl: 대표 이미지 URL(옵션)."
    )
    public ApiResponse<TradeItemResponse> createItem(@Valid @RequestBody TradeItemCreateRequest req, Authentication auth) {
        String creatorUuid = auth != null ? auth.getName() : null;
        TradeItem item = tradeService.createItem(req, creatorUuid);
        return ApiResponse.success(TradeItemResponse.from(item));
    }

    @GetMapping("/items/detail")
    @Operation(
            summary = "아이템 상세 조회",
            description = "QueryString으로 itemUuid를 받아 아이템 상세 정보를 조회합니다.\n\n"
                    + "파라미터 설명:\n"
                    + "- itemUuid: 조회할 아이템 UUID"
    )
    public ApiResponse<TradeItemResponse> getItem(
            @RequestParam String itemUuid
    ) {
        TradeItem item = tradeService.getItemDetail(itemUuid);
        return ApiResponse.success(TradeItemResponse.from(item));
    }

    @GetMapping("/items/search")
    @Operation(
            summary = "아이템 검색/필터 조회",
            description = "QueryString으로 검색 조건을 받아 아이템을 검색합니다.\n\n"
                    + "파라미터 설명:\n"
                    + "- keyword: 검색어\n"
                    + "- category: FOOD|TOOLS|ETC\n"
                    + "- minPrice: 최소 가격\n"
                    + "- maxPrice: 최대 가격\n"
                    + "- active: 활성 여부(true|false)\n"
                    + "- page: 페이지 번호\n"
                    + "- size: 페이지 크기\n"
                    + "- sort: 정렬 기준(createdAt|price|name)\n"
                    + "- dir: 오름/내림차순(asc|desc)"
    )
    public ApiResponse<PageResponse<TradeItemResponse>> searchItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TradeCategory category,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String dir
    ) {
        var pageResult = tradeService
                .searchItems(keyword, category, minPrice, maxPrice, active, page, size, sort, dir)
                .map(TradeItemResponse::from);

        return ApiResponse.success(
                new PageResponse<>(
                        pageResult.getNumber(),
                        pageResult.getSize(),
                        pageResult.getTotalElements(),
                        pageResult.getTotalPages(),
                        pageResult.getContent()
                )
        );
    }

    @PostMapping("/purchase")
    @Operation(
            summary = "물품 거래(구매)",
            description = "로그인 사용자가 포인트로 물품을 구매합니다. 포인트 부족 시 실패합니다.\n\n"
                    + "요청 필드 설명:\n"
                    + "- itemUuid: 구매할 아이템 UUID.\n"
                    + "- quantity: 구매 수량(최소 1)."
    )
    public ApiResponse<TradePurchaseResponse> purchase(
            @Valid @RequestBody TradePurchaseRequest req,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        String userUuid = auth.getName();
        var result = tradeService.purchase(userUuid, req);
        return ApiResponse.success(
                TradePurchaseResponse.of(result.getItem(), result.getLog(), result.getPointsTx())
        );
    }

    @PostMapping("/logs")
    @Operation(
            summary = "거래 로그 저장",
            description = "거래 로그를 별도로 저장합니다(수동 기록). 포인트 트랜잭션과 연결할 수 있습니다.\n\n"
                    + "요청 필드 설명:\n"
                    + "- itemUuid: 아이템 UUID.\n"
                    + "- quantity: 수량(최소 1).\n"
                    + "- totalPoints: 총 포인트(옵션, 미전달 시 단가*수량).\n"
                    + "- itemName: 아이템 이름(옵션, 기본 DB값 사용).\n"
                    + "- pointsUuid: 연결할 포인트 트랜잭션 UUID(옵션).\n"
                    + "- metadata: 추가 메타데이터(JSON 문자열)."
    )
    public ApiResponse<String> createLog(@Valid @RequestBody TradeLogCreateRequest req, Authentication auth) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        String userUuid = auth.getName();
        TradeLog log = tradeService.createLog(userUuid, req);
        return ApiResponse.success(log.getLogUuid());
    }

    @GetMapping("/my/logs")
    @Operation(
            summary = "내 구매내역 조회",
            description = "QueryString으로 page/size를 받아 자신의 구매내역을 조회합니다.\n\n"
                    + "파라미터 설명:\n"
                    + "- page: 페이지 번호(default 0)\n"
                    + "- size: 페이지 크기(default 20)"
    )
    public ApiResponse<PageResponse<TradeLogResponse>> myLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        String userUuid = auth.getName();

        var pageResult = tradeService.getMyLogs(userUuid, page, size)
                .map(TradeLogResponse::from);

        return ApiResponse.success(
                new PageResponse<>(
                        pageResult.getNumber(),
                        pageResult.getSize(),
                        pageResult.getTotalElements(),
                        pageResult.getTotalPages(),
                        pageResult.getContent()
                )
        );
    }

    @GetMapping("/users/logs")
    @Operation(
            summary = "사용자 구매내역 조회",
            description = "QueryString으로 userUuid/page/size를 받아 특정 사용자의 구매 내역을 조회합니다.\n"
                    + "본인 혹은 ADMIN/TEACHER 권한만 접근 가능합니다.\n\n"
                    + "파라미터 설명:\n"
                    + "- userUuid: 조회 대상 사용자 UUID\n"
                    + "- page: 페이지 번호(default 0)\n"
                    + "- size: 페이지 크기(default 20)"
    )
    public ApiResponse<PageResponse<TradeLogResponse>> userLogs(
            @RequestParam String userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        ensureSelfOrAdminOrTeacher(auth, userUuid);

        var pageResult = tradeService.getMyLogs(userUuid, page, size)
                .map(TradeLogResponse::from);

        return ApiResponse.success(
                new PageResponse<>(
                        pageResult.getNumber(),
                        pageResult.getSize(),
                        pageResult.getTotalElements(),
                        pageResult.getTotalPages(),
                        pageResult.getContent()
                )
        );
    }

    private void ensureSelfOrAdminOrTeacher(Authentication auth, String userUuid) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        if (!isAdmin && !isTeacher && !auth.getName().equals(userUuid)) {
            throw new SecurityException("Forbidden");
        }
    }

    @DeleteMapping("/items/delete")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "거래 아이템 삭제",
            description = """
                    선택한 거래 아이템을 삭제(비활성화)합니다.

                    - 실제 DB 행을 제거하지 않고 active 플래그만 false 로 내려,
                      과거 거래 내역과 포인트 이력을 안전하게 보존합니다.

                    요청 필드 설명:
                    - itemUuid: 삭제할 아이템의 UUID (예: "item-uuid-12345678-90ab-cdef-1234-567890abcdef")
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<Void> deleteItem(
            @Parameter(
                    description = "삭제할 아이템 UUID",
                    example = "item-uuid-12345678-90ab-cdef-1234-567890abcdef"
            )
            @RequestParam String itemUuid
    ) {
        tradeService.deleteItem(itemUuid);
        return ApiResponse.success();
    }

    @PostMapping("/restock")
    @Operation(summary = "재고 충전", description = "ADMIN 또는 TEACHER만 재고를 충전할 수 있습니다.")
    public ApiResponse<Void> restock(
            @RequestBody RestockRequest req,
            Authentication auth
    ) {
        tradeService.restockItem(req.getItemUuid(), req.getAmount(), auth.getName());
        return ApiResponse.success();
    }
}