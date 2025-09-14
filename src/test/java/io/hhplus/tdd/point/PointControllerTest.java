package io.hhplus.tdd.point;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

@WebMvcTest(PointController.class)
public class PointControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserPointTable userPointTable;

    @MockBean
    private PointHistoryTable pointHistoryTable;

    @Nested
    @DisplayName("포인트 조회 API: [GET] /point/{id}")
    class Test_GetPoint {
        @Test
        @DisplayName("신규 사용자를 조회하면 0 포인트를 반환한다.")
        void success_getNewUserPoint() throws Exception {
            long id = 0L;

            given(userPointTable.selectById(id)).willReturn(new UserPoint(id, 0, anyLong()));

            mockMvc.perform(get("/point/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.point").value(0));

            verify(userPointTable, times(1)).selectById(id);
        }

        @Test
        @DisplayName("현재 사용자의 포인트를 반환한다.")
        void success_getUserPoint() throws Exception {
            long id = 1L;
            long point = 1000L;

            given(userPointTable.selectById(id)).willReturn(new UserPoint(id, point, anyLong()));

            mockMvc.perform(get("/point/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.point").value(point));

            verify(userPointTable, times(1)).selectById(id);
        }
    }

    @Nested
    @DisplayName("포인트 내역 조회 API: [GET] /point/{id}/histories")
    class Test_GetPointHistory {
        @Test
        @DisplayName("사용자의 포인트 충전/사용 내역이 없는 경우 빈 list를 반환한다.")
        void success_noHistoryEmptyList() throws Exception {
            long id = 2L;

            given(pointHistoryTable.selectAllByUserId(id)).willReturn(Collections.emptyList());

            mockMvc.perform(get("/point/{id}/histories", id))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(pointHistoryTable, times(1)).selectAllByUserId(id);
        }

        @Test
        @DisplayName("사용자의 포인트 충전/사용 내역을 반환한다.")
        void success_getUserPointHistory() throws Exception {
            long id = 3L;
            List<PointHistory> histories = List.of(
                new PointHistory(1L, id, 1000, TransactionType.CHARGE, anyLong()),
                new PointHistory(2L, id, -500, TransactionType.USE, anyLong())
            );

            given(pointHistoryTable.selectAllByUserId(id)).willReturn(histories);

            mockMvc.perform(get("/point/{id}/histories", id))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));

            verify(pointHistoryTable, times(1)).selectAllByUserId(id);
        }
    }

@Nested
    @DisplayName("포인트 충전 API: [PATCH] /point/{id}/charge")
    class Test_ChargePoint {
        @Test
        @DisplayName("포인트 충전에 성공한다.")
        void success_chargePoint() throws Exception {
            long id = 4L;
            long currentPoint = 1000L;
            long chargePoint = 1000L;
            long updatedPoint = currentPoint + chargePoint;

            given(userPointTable.selectById(id)).willReturn(new UserPoint(id, currentPoint, System.currentTimeMillis()));
            given(userPointTable.insertOrUpdate(id, updatedPoint)).willReturn(new UserPoint(id, updatedPoint, System.currentTimeMillis()));

            mockMvc.perform(patch("/point/{id}/charge", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(chargePoint))
                    ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(updatedPoint));
            
            InOrder inOrder = inOrder(userPointTable, pointHistoryTable);
            inOrder.verify(userPointTable).selectById(id);
            inOrder.verify(userPointTable).insertOrUpdate(id, updatedPoint);
            inOrder.verify(pointHistoryTable).insert(eq(id), eq(chargePoint), eq(TransactionType.CHARGE), anyLong());
            verifyNoMoreInteractions(userPointTable, pointHistoryTable);
        }

        @Test
        @DisplayName("음수 금액을 충전 시 400에러를 발생시킨다.")
        void fail_negative_chargePoint() throws Exception {
            long id = 5L;
            long negativePoint = -500L;

            mockMvc.perform(patch("/point/{id}/charge", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(negativePoint))
                    ).andExpect(status().isBadRequest());
            
            verifyNoInteractions(userPointTable, pointHistoryTable);
        }
    }

    @Nested
    @DisplayName("포인트 사용 API: [PATCH] /point/{id}/use")
    class Test_UsePoint { 
        @DisplayName("포인트 사용에 성공한다.")
        void success_usePoint() throws Exception {
            long id = 6L;
            long currentPoint = 1000L;
            long usePoint = 500L;
            long updatedPoint = currentPoint - usePoint;

            given(userPointTable.selectById(id)).willReturn(new UserPoint(id, currentPoint, System.currentTimeMillis()));
            given(userPointTable.insertOrUpdate(id, updatedPoint)).willReturn(new UserPoint(id, updatedPoint, System.currentTimeMillis()));

            mockMvc.perform(patch("/point/{id}/use", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usePoint))
                    ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(updatedPoint));
            
            InOrder inOrder = inOrder(userPointTable, pointHistoryTable);
            inOrder.verify(userPointTable).selectById(id);
            inOrder.verify(userPointTable).insertOrUpdate(id, updatedPoint);
            inOrder.verify(pointHistoryTable).insert(eq(id), eq(-usePoint), eq(TransactionType.USE), anyLong());
            verifyNoMoreInteractions(userPointTable, pointHistoryTable);
        }

        @Test
        @DisplayName("음수 금액을 사용 시 400에러를 발생시킨다.")
        void fail_negative_usePoint() throws Exception {
            long id = 6L;
            long negativePoint = -500L;

            mockMvc.perform(patch("/point/{id}/use", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(negativePoint))
                    ).andExpect(status().isBadRequest());
            
            verifyNoInteractions(userPointTable, pointHistoryTable);
        }

        @Test
        @DisplayName("사용금액이 포인트 금액을 초과할 경우 409에러를 발생시킨다.")
        void fail_usePoint_bigger_currentPoint() throws Exception {
            long id = 7L;
            long currentPoint = 1000L;
            long usePoint = 2000L;

            given(userPointTable.selectById(id)).willReturn(new UserPoint(id, currentPoint, System.currentTimeMillis()));

            mockMvc.perform(patch("/point/{id}/use", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(usePoint))
                    ).andExpect(status().isConflict()); 
    }
}
