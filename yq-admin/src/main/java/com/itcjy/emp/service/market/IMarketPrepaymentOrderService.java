package com.itcjy.emp.service.market;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.req.market.MarketPrepaymentOrderCreateReq;
import com.itcjy.emp.pojo.req.market.MarketPrepaymentOrderPageReq;
import com.itcjy.emp.pojo.req.market.MarketPrepaymentOrderUpdateReq;
import com.itcjy.emp.pojo.res.market.MarketEducationOptionRes;
import com.itcjy.emp.pojo.res.market.MarketPrepaymentOrderCreateRes;
import com.itcjy.emp.pojo.res.market.MarketPrepaymentOrderRes;
import com.itcjy.emp.pojo.res.market.MarketProductOptionRes;
import com.itcjy.emp.pojo.res.market.MarketSalespersonOptionRes;

import java.util.List;

public interface IMarketPrepaymentOrderService extends IService<MarketPrepaymentOrder> {

    MarketPrepaymentOrderCreateRes addOrder(MarketPrepaymentOrderCreateReq req);

    void updateOrder(Long id, MarketPrepaymentOrderUpdateReq req);

    void deleteOrder(Long id);

    MarketPrepaymentOrderRes getOrderDetail(Long id);

    PageResult<MarketPrepaymentOrderRes> pageOrders(MarketPrepaymentOrderPageReq req);

    List<MarketSalespersonOptionRes> listSalespersonOptions();

    List<MarketProductOptionRes> listProductOptions();

    List<MarketEducationOptionRes> listEducationOptions();
}
