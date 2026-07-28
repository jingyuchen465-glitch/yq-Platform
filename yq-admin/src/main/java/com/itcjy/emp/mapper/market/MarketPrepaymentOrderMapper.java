package com.itcjy.emp.mapper.market;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MarketPrepaymentOrderMapper extends BaseMapper<MarketPrepaymentOrder> {

    @Select("SELECT * FROM market_prepayment_order WHERE id = #{id} FOR UPDATE")
    MarketPrepaymentOrder selectByIdForUpdate(Long id);
}
