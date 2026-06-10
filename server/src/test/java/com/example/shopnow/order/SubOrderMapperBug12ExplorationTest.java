package com.example.shopnow.order;

import com.example.shopnow.order.mapper.SubOrderMapper;
import com.example.shopnow.order.models.OrderStatus;
import com.example.shopnow.order.models.SubOrder;
import com.example.shopnow.order.rest.dto.SubOrderSummaryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-12 ({@link com.example.shopnow.order.mapper.OrderMapper}
 * / {@link SubOrderMapper} narrows {@link BigDecimal} {@code totalPrice}
 * down to {@link Integer} on the {@link SubOrderSummaryDTO}, which truncates
 * the fractional part — or, for very large scaled values, throws
 * {@link ArithmeticException} {@code ("Rounding necessary")} depending on the
 * MapStruct conversion path).
 *
 * <p><b>Validates: Requirements 2.12</b> (Property 12 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed code;
 * the failure surfaces a counterexample that proves the bug exists. After
 * the fix (changing {@link SubOrderSummaryDTO#totalPrice()} — and any
 * sibling DTO field such as {@code OrderDetailResponse.totalPrice} — from
 * {@link Integer} to {@link BigDecimal} so MapStruct propagates the value
 * verbatim), this test must turn green.
 *
 * <p><b>Bug condition C(12):</b> a {@link SubOrder} whose {@code totalPrice}
 * is a {@link BigDecimal} with {@code scale &gt;= 1} (i.e. it has at least
 * one fractional digit), e.g. {@code 123.45}, {@code 0.01},
 * {@code 99999.999}, {@code 0.99}.
 *
 * <p><b>Property P(12):</b> for every input satisfying C(12),
 * {@code SubOrderMapper.toSummaryDTO(subOrder).totalPrice()} SHALL
 * <ol>
 *   <li>be an {@link BigDecimal} instance — the DTO field type itself
 *       must be {@link BigDecimal} so no narrowing happens;</li>
 *   <li>be {@linkplain BigDecimal#compareTo(BigDecimal) compareTo}-equal to
 *       the {@link BigDecimal} that was set on the source entity (value
 *       and scale roundtripped without rounding).</li>
 * </ol>
 *
 * <p><b>Buggy production behaviour</b> (see
 * {@code target/generated-sources/annotations/.../SubOrderMapperImpl.java}):
 * <pre>
 *   if ( subOrder.getTotalPrice() != null ) {
 *       totalPrice = subOrder.getTotalPrice().intValue();   // narrows BigDecimal -> int
 *   }
 * </pre>
 * Because {@link SubOrderSummaryDTO} declares {@code Integer totalPrice},
 * MapStruct emits {@code BigDecimal#intValue()} which silently drops the
 * fractional part, so any input with scale ≥ 1 fails P(12) (the result is
 * not a {@code BigDecimal} at all, and even the truncated numeric value
 * differs from the input by the discarded fraction).
 *
 * <p><b>Test design:</b> JUnit 5 parameterized test driven by
 * {@link ValueSource} with a curated set of fractional {@link BigDecimal}
 * literals that span common scales (2 decimals — currency-like, 3
 * decimals, very small magnitudes near zero, and trailing zeros). The
 * mapper is obtained through {@link Mappers#getMapper(Class)} so the test
 * runs without a Spring context — only the real generated MapStruct
 * implementation is exercised. The mapping path under test
 * ({@code toSummaryDTO}) does not use the injected
 * {@code OrderDetailMapper}, so a bare Impl with no dependencies is
 * sufficient.
 *
 * <p><b>Expected counterexample on unfixed code:</b> for every parameter,
 * {@code dto.totalPrice()} is an {@link Integer} (e.g. {@code 123} for
 * input {@code 123.45}, {@code 0} for input {@code 0.01}), so the
 * {@code instanceof BigDecimal} assertion fails. The exact counterexample
 * recorded after running the test is the smallest seed in the set:
 * {@code totalPrice=0.01} → {@code dto.totalPrice() == 0 (Integer)} ⇒
 * neither {@link BigDecimal} nor {@code compareTo}-equal to {@code 0.01}.
 */
class SubOrderMapperBug12ExplorationTest {

    private final SubOrderMapper mapper = Mappers.getMapper(SubOrderMapper.class);

    /**
     * Curated counterexample seeds for C(12). Each value has scale ≥ 1
     * and exercises a different facet of the bug:
     * <ul>
     *   <li>{@code 123.45} — currency-like, drops cents on narrowing.</li>
     *   <li>{@code 0.01} — small magnitude, narrows to {@code 0}.</li>
     *   <li>{@code 99999.999} — large with 3-decimal fraction.</li>
     *   <li>{@code 0.99} — &lt; 1, narrows to {@code 0}.</li>
     *   <li>{@code 12345.6789} — 4-decimal fraction.</li>
     *   <li>{@code 1.10} — trailing zero in scale (compareTo-equal to 1.1).</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] totalPrice={0} -> DTO.totalPrice must be BigDecimal AND compareTo(input)==0")
    @ValueSource(strings = {"123.45", "0.01", "99999.999", "0.99", "12345.6789", "1.10"})
    @DisplayName("toSummaryDTO preserves BigDecimal value and scale (no narrowing to Integer)")
    void toSummaryDTO_preservesBigDecimal(String literal) {
        // --- Arrange --------------------------------------------------------
        BigDecimal input = new BigDecimal(literal);

        SubOrder subOrder = SubOrder.builder()
                .shopId(UUID.randomUUID())
                .shopOwnerId(UUID.randomUUID())
                .status(OrderStatus.IN_PROCESS)
                .totalPrice(input)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        // BaseEntity.id is normally set by Hibernate; populate it via reflection
        // so the resulting DTO has a stable identity (not actually asserted on,
        // but keeps the source object internally consistent).
        ReflectionTestUtils.setField(subOrder, "id", UUID.randomUUID());

        // --- Act ------------------------------------------------------------
        SubOrderSummaryDTO dto = mapper.toSummaryDTO(subOrder);

        // --- Assert ---------------------------------------------------------
        // P(12).1: the DTO must carry the raw BigDecimal — no Integer
        // narrowing. Reading via the record accessor returns whatever type
        // the field declares; we widen to Object so we can perform a runtime
        // type check that surfaces the bug (the field is currently `Integer
        // totalPrice` in SubOrderSummaryDTO, so this fails on unfixed code).
        Object mappedTotalPrice = dto.totalPrice();
        assertThat(mappedTotalPrice)
                .as("toSummaryDTO must keep totalPrice as BigDecimal (input=%s); "
                        + "actual runtime type=%s, actual value=%s",
                        input,
                        mappedTotalPrice == null ? "null" : mappedTotalPrice.getClass().getName(),
                        mappedTotalPrice)
                .isInstanceOf(BigDecimal.class);

        // P(12).2: the returned BigDecimal must be compareTo-equal to the
        // input — value and scale must round-trip without rounding. Cast
        // through Object to bypass the compile-time Integer return type.
        BigDecimal mapped = (BigDecimal) mappedTotalPrice;
        assertThat(mapped.compareTo(input))
                .as("toSummaryDTO must preserve numeric value of totalPrice "
                        + "(input=%s, mapped=%s)", input, mapped)
                .isZero();
    }
}
