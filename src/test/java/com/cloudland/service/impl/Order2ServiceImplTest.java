package com.cloudland.service.impl;

import com.cloudland.Interceptor.MyInterceptor;
import com.cloudland.controller.result.Code;
import com.cloudland.controller.result.Result;
import com.cloudland.mapper.CloudLandFileMapper;
import com.cloudland.mapper.LandMapper;
import com.cloudland.mapper.Order2Mapper;
import com.cloudland.mapper.ProductMapper;
import com.cloudland.pojo.Land;
import com.cloudland.pojo.Order2;
import com.cloudland.pojo.Product;
import com.cloudland.pojo.vo.LandVO;
import com.cloudland.util.DownloadUtil;
import com.cloudland.util.OrderExporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单核心不变量的单元测试：金额重算、锁地/扣库存、删单回滚、归属校验、支付映射幂等。
 *
 * <p>为什么这层必须存在：接口测试刻意跳过下单/支付写路径（不可逆副作用），
 * 这些"钱和库存"的规则此前没有任何自动化覆盖。这里用 Mockito 替换
 * Mapper/Redis/请求上下文，在无数据库环境下把规则钉死。
 *
 * <p>边界：mock 验证的是"规则与协作"，不验证真实 SQL 与事务回滚，
 * 后者仍由接口层/人工验证负责。
 */
@ExtendWith(MockitoExtension.class)
class Order2ServiceImplTest {

    @Mock
    private LandMapper landMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private Order2Mapper orderMapper;
    @Mock
    private CloudLandFileMapper cloudLandFileMapper;
    @Mock
    private OrderExporter orderExporter;
    @Mock
    private DownloadUtil downloadUtil;
    @Mock
    private HttpServletRequest request;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private Order2ServiceImpl orderService;

    /** 模拟拦截器注入的登录态 */
    private void loginAs(Integer userId, Integer power) {
        when(request.getAttribute(MyInterceptor.ATTR_USER_ID)).thenReturn(userId);
        when(request.getAttribute(MyInterceptor.ATTR_USER_POWER)).thenReturn(power);
    }

    private Order2 order(Integer id, Integer pId, Integer num, Integer uId, Integer status) {
        Order2 order = new Order2();
        order.setId(id);
        order.setPId(pId);
        order.setNum(num);
        order.setUId(uId);
        order.setStatus(status);
        return order;
    }

    private LandVO land(Integer id, Double price, Integer status) {
        LandVO land = new LandVO();
        land.setId(id);
        land.setPrice(price);
        land.setStatus(status);
        return land;
    }

    private Product product(Integer id, Double price, Double num, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setPrice(price);
        product.setNum(num);
        product.setStatus(status);
        return product;
    }

    // ==================== 金额服务端重算 ====================

    @Test
    @DisplayName("土地订单金额 = land.price，与前端传入无关")
    void landOrderAmountComesFromLandPrice() {
        when(orderMapper.selectById(1)).thenReturn(order(1, 10, -1, 5, 0));
        when(landMapper.selectById(10)).thenReturn(land(10, 100.0, 0));

        BigDecimal total = orderService.calcTotalAmount(new Integer[]{1});

        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("产品订单金额 = product.price × num")
    void productOrderAmountMultipliesPriceByNum() {
        when(orderMapper.selectById(1)).thenReturn(order(1, 20, 3, 5, 0));
        when(productMapper.selectById(20)).thenReturn(product(20, 10.5, 99.0, 1));

        BigDecimal total = orderService.calcTotalAmount(new Integer[]{1});

        assertThat(total).isEqualByComparingTo("31.50");
    }

    @Test
    @DisplayName("混合订单金额逐单累加")
    void mixedOrdersAreSummed() {
        when(orderMapper.selectById(1)).thenReturn(order(1, 10, -1, 5, 0));
        when(orderMapper.selectById(2)).thenReturn(order(2, 20, 2, 5, 0));
        when(landMapper.selectById(10)).thenReturn(land(10, 100.0, 0));
        when(productMapper.selectById(20)).thenReturn(product(20, 10.0, 99.0, 1));

        BigDecimal total = orderService.calcTotalAmount(new Integer[]{1, 2});

        assertThat(total).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("已支付订单不允许再次参与支付")
    void paidOrderIsRejected() {
        when(orderMapper.selectById(1)).thenReturn(order(1, 20, 2, 5, 1));

        assertThatThrownBy(() -> orderService.calcTotalAmount(new Integer[]{1}))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("仅未支付订单可支付");
    }

    @Test
    @DisplayName("不存在的订单直接拒绝，而不是算出 0 元")
    void missingOrderIsRejected() {
        when(orderMapper.selectById(1)).thenReturn(null);

        assertThatThrownBy(() -> orderService.calcTotalAmount(new Integer[]{1}))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    @DisplayName("空选择 / null 选择被拒绝")
    void emptySelectionIsRejected() {
        assertThatThrownBy(() -> orderService.calcTotalAmount(new Integer[0]))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> orderService.calcTotalAmount(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("土地信息缺失时拒绝支付，避免 0 元租地")
    void missingLandIsRejected() {
        when(orderMapper.selectById(1)).thenReturn(order(1, 10, -1, 5, 0));
        when(landMapper.selectById(10)).thenReturn(null);

        assertThatThrownBy(() -> orderService.calcTotalAmount(new Integer[]{1}))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("土地信息异常");
    }

    // ==================== 下单：归属 / 锁地 / 扣库存 ====================

    @Test
    @DisplayName("客户下单时 uId 被强制改为登录者，不能替他人下单")
    void customerCannotOrderForOthers() {
        loginAs(5, 0);
        when(productMapper.selectById(20)).thenReturn(product(20, 10.0, 10.0, 1));
        Order2 input = order(null, 20, 2, 999, null);

        Result result = orderService.addOrder(input);

        assertThat(result.getCode()).isEqualTo(Code.ADD_OK);
        assertThat(input.getUId()).isEqualTo(5);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(captor.capture());
        assertThat(captor.getValue().getNum()).isEqualTo(8.0);
    }

    @Test
    @DisplayName("员工/管理员可代录订单，uId 不被覆盖")
    void staffCanOrderForOthers() {
        loginAs(1, 1);
        when(productMapper.selectById(20)).thenReturn(product(20, 10.0, 10.0, 1));
        Order2 input = order(null, 20, 2, 999, null);

        orderService.addOrder(input);

        assertThat(input.getUId()).isEqualTo(999);
    }

    @Test
    @DisplayName("土地下单把 status 从 1 锁到 0，并写入未支付初始状态")
    void landOrderLocksLand() {
        loginAs(5, 0);
        when(landMapper.selectById(10)).thenReturn(land(10, 100.0, 1));
        Order2 input = order(null, 10, -1, 5, null);

        Result result = orderService.addOrder(input);

        assertThat(result.getCode()).isEqualTo(Code.ADD_OK);
        ArgumentCaptor<Land> captor = ArgumentCaptor.forClass(Land.class);
        verify(landMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(input.getStatus()).isEqualTo(0);
        assertThat(input.getDel()).isEqualTo(0);
        assertThat(input.getCreateTime()).isNotNull();
        verify(orderMapper).insert(input);
    }

    @Test
    @DisplayName("已被锁定的土地拒绝重复下单，且不写订单")
    void lockedLandIsRejected() {
        loginAs(5, 0);
        when(landMapper.selectById(10)).thenReturn(land(10, 100.0, 0));

        Result result = orderService.addOrder(order(null, 10, -1, 5, null));

        assertThat(result.getCode()).isEqualTo(Code.ADD_ERR);
        verify(landMapper, never()).updateById(any());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("库存不足拒绝下单，不扣库存不写订单")
    void insufficientStockIsRejected() {
        loginAs(5, 0);
        when(productMapper.selectById(20)).thenReturn(product(20, 10.0, 1.0, 1));

        Result result = orderService.addOrder(order(null, 20, 2, 5, null));

        assertThat(result.getCode()).isEqualTo(Code.ADD_ERR);
        verify(productMapper, never()).updateById(any());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("下架产品拒绝下单")
    void offShelfProductIsRejected() {
        loginAs(5, 0);
        when(productMapper.selectById(20)).thenReturn(product(20, 10.0, 10.0, 0));

        Result result = orderService.addOrder(order(null, 20, 2, 5, null));

        assertThat(result.getCode()).isEqualTo(Code.ADD_ERR);
        verify(orderMapper, never()).insert(any());
    }

    // ==================== 删单：归属 / 软删 / 资源回滚 ====================

    @Test
    @DisplayName("删除不存在的订单返回 DELETE_ERR")
    void deletingMissingOrderReturnsDeleteErr() {
        when(orderMapper.selectById(1)).thenReturn(null);

        Result result = orderService.deleteOrder(1);

        assertThat(result.getCode()).isEqualTo(Code.DELETE_ERR);
    }

    @Test
    @DisplayName("客户不能删他人订单（POWER_ERR），且不产生任何写操作")
    void customerCannotDeleteOthersOrder() {
        loginAs(5, 0);
        when(orderMapper.selectById(1)).thenReturn(order(1, 20, 2, 6, 0));

        Result result = orderService.deleteOrder(1);

        assertThat(result.getCode()).isEqualTo(Code.POWER_ERR);
        verify(orderMapper, never()).deleteById(any());
        verify(orderMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("已支付订单软删（del=1），保留记录不物理删除")
    void paidOrderIsSoftDeleted() {
        loginAs(5, 0);
        Order2 paid = order(1, 20, 2, 5, 1);
        paid.setPayTime(LocalDateTime.now());
        when(orderMapper.selectById(1)).thenReturn(paid);

        Result result = orderService.deleteOrder(1);

        assertThat(result.getCode()).isEqualTo(Code.DELETE_OK);
        assertThat(paid.getDel()).isEqualTo(1);
        verify(orderMapper).updateById(paid);
        verify(orderMapper, never()).deleteById(any());
    }

    @Test
    @DisplayName("未支付土地订单删除时把土地释放回 status=1")
    void unpaidLandOrderReleasesLand() {
        loginAs(5, 0);
        Order2 unpaid = order(1, 10, -1, 5, 0);
        when(orderMapper.selectById(1)).thenReturn(unpaid);

        Result result = orderService.deleteOrder(1);

        assertThat(result.getCode()).isEqualTo(Code.DELETE_OK);
        ArgumentCaptor<Land> captor = ArgumentCaptor.forClass(Land.class);
        verify(landMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        verify(orderMapper).deleteById(unpaid);
    }

    @Test
    @DisplayName("未支付产品订单删除时回补库存")
    void unpaidProductOrderRestoresStock() {
        loginAs(5, 0);
        Order2 unpaid = order(1, 20, 2, 5, 0);
        when(orderMapper.selectById(1)).thenReturn(unpaid);
        when(productMapper.selectById(20)).thenReturn(product(20, 10.0, 5.0, 1));

        Result result = orderService.deleteOrder(1);

        assertThat(result.getCode()).isEqualTo(Code.DELETE_OK);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(captor.capture());
        assertThat(captor.getValue().getNum()).isEqualTo(7.0);
        verify(orderMapper).deleteById(unpaid);
    }

    @Test
    @DisplayName("员工可删他人订单（后台管理能力）")
    void staffCanDeleteOthersOrder() {
        loginAs(1, 1);
        Order2 others = order(1, 20, 2, 6, 0);
        when(orderMapper.selectById(1)).thenReturn(others);
        when(productMapper.selectById(20)).thenReturn(product(20, 10.0, 5.0, 1));

        Result result = orderService.deleteOrder(1);

        assertThat(result.getCode()).isEqualTo(Code.DELETE_OK);
        verify(orderMapper).deleteById(others);
    }

    // ==================== 支付映射：归属 / TTL / 幂等 ====================

    @Test
    @DisplayName("客户不能为他人订单发起支付映射")
    void customerCannotPayOthersOrder() {
        loginAs(5, 0);
        when(orderMapper.selectById(1)).thenReturn(order(1, 20, 2, 6, 0));

        assertThatThrownBy(() -> orderService.saveTradeMapping("CLD1", new Integer[]{1}))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("归属校验失败");
    }

    @Test
    @DisplayName("本人订单的交易映射存 Redis，TTL 30 分钟")
    void ownOrderMappingStoredWithTtl() {
        loginAs(5, 0);
        when(orderMapper.selectById(1)).thenReturn(order(1, 20, 2, 5, 0));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        orderService.saveTradeMapping("CLD1", new Integer[]{1});

        verify(valueOperations).set(startsWith("alipay:trade:"), eq("[1]"), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("员工可代支付任意订单")
    void staffCanPayAnyOrder() {
        loginAs(1, 1);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        orderService.saveTradeMapping("CLD1", new Integer[]{1});

        verify(valueOperations).set(anyString(), anyString(), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("映射不存在时回调是空操作（幂等），不碰订单")
    void missingMappingIsNoop() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("alipay:trade:CLD1")).thenReturn(null);

        orderService.handlePaySuccess("CLD1");

        verify(orderMapper, never()).selectById(any());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("回调把订单置已支付并写 payTime，然后删除映射防重复处理")
    void callbackMarksPaidAndDeletesMapping() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("alipay:trade:CLD1")).thenReturn("[1]");
        Order2 unpaid = order(1, 20, 2, 5, 0);
        when(orderMapper.selectById(1)).thenReturn(unpaid);

        orderService.handlePaySuccess("CLD1");

        assertThat(unpaid.getStatus()).isEqualTo(1);
        assertThat(unpaid.getPayTime()).isNotNull();
        verify(orderMapper).updateById(unpaid);
        verify(stringRedisTemplate).delete("alipay:trade:CLD1");
    }
}