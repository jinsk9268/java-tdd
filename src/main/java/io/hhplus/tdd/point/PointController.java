package io.hhplus.tdd.point;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

import io.hhplus.tdd.ErrorResponse;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

import java.util.List;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointController(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("/{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        return this.userPointTable.selectById(id);
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("/{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return this.pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("/{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        if(amount < 0) throw new IllegalArgumentException( "충전하려는 포인트 금액은 음수가 될 수 없습니다.");
        
        UserPoint currUserPoint = this.userPointTable.selectById(id);
        long newPoint = currUserPoint.point() + amount;

        UserPoint chargePoint = this.userPointTable.insertOrUpdate(id, newPoint);
        this.pointHistoryTable.insert(id, amount, TransactionType.CHARGE, chargePoint.updateMillis());
        return chargePoint;
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("/{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        if(amount < 0) throw new IllegalArgumentException("사용하려는 포인트 금액은 음수가 될 수 없습니다.");
        
        UserPoint currUserPoint = this.userPointTable.selectById(id);
        if(currUserPoint.point() < amount) throw new IllegalStateException("포인트 잔액이 부족합니다.");
        long newPoint = currUserPoint.point() - amount;

        UserPoint usePoint = this.userPointTable.insertOrUpdate(id, newPoint);
        this.pointHistoryTable.insert(id, -amount, TransactionType.USE, usePoint.updateMillis());
        return usePoint;
    }
}
