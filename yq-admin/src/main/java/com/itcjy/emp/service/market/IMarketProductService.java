package com.itcjy.emp.service.market;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.MarketProduct;
import com.itcjy.emp.pojo.req.market.MarketProductCreateReq;
import com.itcjy.emp.pojo.req.market.MarketProductPageReq;
import com.itcjy.emp.pojo.req.market.MarketProductUpdateReq;
import com.itcjy.emp.pojo.res.market.MarketCourseOptionRes;
import com.itcjy.emp.pojo.res.market.MarketProductRes;

import java.util.List;

public interface IMarketProductService extends IService<MarketProduct> {

    void addProduct(MarketProductCreateReq req);

    void updateProduct(Long id, MarketProductUpdateReq req);

    void deleteProduct(Long id);

    MarketProductRes getProductDetail(Long id);

    PageResult<MarketProductRes> pageProducts(MarketProductPageReq req);

    List<MarketCourseOptionRes> listCourseOptions();
}
