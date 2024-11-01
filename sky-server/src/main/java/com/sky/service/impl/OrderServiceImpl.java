package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/30
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;
    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 检查地址是否为空
        AddressBook addressBook = addressBookMapper.selectById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 检查用户收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        // 检查购物车是否为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.selectList(queryWrapper);
        if (shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 设置订单
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setUserId(userId);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setOrderTime(LocalDateTime.now());

        orderMapper.insert(orders);

        // 订单详细信息
        for (ShoppingCart cart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailMapper.insert(orderDetail);
        }

        // 清空购物车
        LambdaQueryWrapper<ShoppingCart> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(ShoppingCart::getUserId, userId);
        shoppingCartMapper.delete(queryWrapper);

        // 封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, userId);
        User user = userMapper.selectOne(queryWrapper);

        //调用微信支付接口，生成预支付交易单
        /*JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }*/
        // TODO 跳过微信支付(不需要先预支付 支付后微信支付再调用回调函数调用paySuccess) 直接支付成功
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //为替代微信支付成功后的数据库订单状态更新，多定义一个方法进行修改
        Integer OrderPaidStatus = Orders.PAID; //支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单

        //发现没有将支付时间 check_out属性赋值，所以在这里更新
        LocalDateTime check_out_time = LocalDateTime.now();

        //获取订单号码
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        log.info("调用paySuccess，用于替换微信支付更新数据库状态的问题");
        // 跳过微信支付的回调函数 直接修改订单支付成功信息
        paySuccess(orderNumber);
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getNumber, outTradeNo)
                .eq(Orders::getUserId, userId);
        Orders ordersDB = orderMapper.selectOne(queryWrapper);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.updateById(orders);

        // websocket订单提醒
        Map map = new HashMap();
        map.put("type", 1);//消息类型，1表示来单提醒
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + outTradeNo);

        //通过WebSocket实现来单提醒，向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    @Override
    public PageResult pageQuery4User(int curPage, int pageSize, Integer status) {
        IPage<Orders> page = new Page<>(curPage, pageSize);
        // 分页查询当前用户要求订单状态下的所有订单 默认全查
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(status != null, Orders::getStatus, status)
                .eq(Orders::getUserId, BaseContext.getCurrentId())
                        .orderByDesc(Orders::getOrderTime);
        orderMapper.selectPage(page, queryWrapper);

        List<OrderVO> list = new ArrayList<>();
        // 向分页查询的订单中添加订单详细信息
        if (page != null && page.getTotal() > 0) {
            List<Orders> records = page.getRecords();
            for (Orders record : records) {
                Long id = record.getId();

                LambdaQueryWrapper<OrderDetail> queryWrapper1 = new LambdaQueryWrapper<>();
                queryWrapper1.eq(OrderDetail::getOrderId, id);
                List<OrderDetail> orderDetails = orderDetailMapper.selectList(queryWrapper1);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(record, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(), list);
    }

    @Override
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.selectById(id);

        // 查询该订单对应的菜品/套餐明细
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(queryWrapper);

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    @Override
    public void userCancelById(Long id) {
        Orders orders = orderMapper.selectById(id);
        // 检查订单是否存在
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 检查订单状态是否已经派送 需要联系商家退款
        if (orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 若状态未接单 直接退款
        if (orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 微信退款
            /*WeChatPayUtil.refund(orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));*/
            log.info("申请退款");
        }

        // 设置订单状态取消
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orders.setPayStatus(Orders.REFUND);

        orderMapper.updateById(orders);
    }

    @Override
    public void repetition(Long id) {
        Long userid = BaseContext.getCurrentId();
        // 得到当前订单详细信息
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(queryWrapper);
        // 重新将订单信息通过stream流转换为shoppingcart集合
        List<ShoppingCart> shoppingCarts = orderDetails.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userid);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCarts.forEach(shoppingCart -> shoppingCartMapper.insert(shoppingCart));
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        IPage<Orders> page = pageQueryOrders(ordersPageQueryDTO);
        List<Orders> orders = page.getRecords();

        List<OrderVO> orderVOS = new ArrayList<>();
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(orders)) {
            for (Orders order : orders) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                String orderDishes = getOrderDishesStr(order);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 分页查询订单信息
     * @param ordersPageQueryDTO
     * @return
     */
    private IPage<Orders> pageQueryOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        IPage<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId())
                .eq(ordersPageQueryDTO.getNumber() != null, Orders::getId, ordersPageQueryDTO.getNumber())
                .eq(ordersPageQueryDTO.getPhone() != null, Orders::getPhone, ordersPageQueryDTO.getPhone())
                .eq(ordersPageQueryDTO.getStatus() != null, Orders::getStatus, ordersPageQueryDTO.getStatus())
                .gt(ordersPageQueryDTO.getBeginTime() != null, Orders::getOrderTime, ordersPageQueryDTO.getBeginTime())
                .lt(ordersPageQueryDTO.getEndTime() != null, Orders::getOrderTime, ordersPageQueryDTO.getEndTime())
                .orderByDesc(Orders::getOrderTime);
        orderMapper.selectPage(page, queryWrapper);

        return page;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orders.getId());
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(queryWrapper);

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeconfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInPrograss = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeconfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInPrograss);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = new Orders();
        orders.setId(ordersConfirmDTO.getId());
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.updateById(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = orderMapper.selectById(ordersRejectionDTO.getId());

        if (orders == null || !(orders.getStatus().equals(Orders.TO_BE_CONFIRMED))) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Integer payStatus = orders.getPayStatus();
        if (payStatus == Orders.PAID) {
            // 微信退款
            /*WeChatPayUtil.refund(orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));*/
            orders.setPayStatus(Orders.REFUND);
            log.info("申请退款");
        }


        orders.setId(orders.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.updateById(orders);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.selectById(ordersCancelDTO.getId());

        Integer payStatus = orders.getPayStatus();
        if (payStatus == Orders.PAID) {
            // 微信退款
            /*WeChatPayUtil.refund(orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));*/
            log.info("申请退款");
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.updateById(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.selectById(id);

        if (orders == null || !(orders.getStatus().equals(Orders.CONFIRMED))) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orders.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(60));
        orderMapper.updateById(orders);
    }

    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.selectById(id);

        if (orders == null || !(orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS))) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 100000){
            //配送距离超过100km
            throw new OrderBusinessException("超出配送范围");
        }
    }

    @Override
    public void reminder(Long id) {
        // 查询订单是否存在
        Orders orders = orderMapper.selectById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //基于WebSocket实现催单
        Map map = new HashMap();
        map.put("type", 2);//2代表用户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
