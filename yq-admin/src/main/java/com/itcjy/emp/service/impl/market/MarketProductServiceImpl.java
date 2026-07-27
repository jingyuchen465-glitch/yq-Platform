package com.itcjy.emp.service.impl.market;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.interceptor.AuthThreadlocal;
import com.itcjy.common.interceptor.LoginSession;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.market.MarketProductCourseMapper;
import com.itcjy.emp.mapper.market.MarketProductMapper;
import com.itcjy.emp.pojo.entity.MarketProduct;
import com.itcjy.emp.pojo.entity.MarketProductCourse;
import com.itcjy.emp.pojo.entity.SysCourse;
import com.itcjy.emp.pojo.req.market.MarketProductCreateReq;
import com.itcjy.emp.pojo.req.market.MarketProductPageReq;
import com.itcjy.emp.pojo.req.market.MarketProductUpdateReq;
import com.itcjy.emp.pojo.res.market.MarketCourseOptionRes;
import com.itcjy.emp.pojo.res.market.MarketProductRes;
import com.itcjy.emp.service.academic.ISysCourseService;
import com.itcjy.emp.service.market.IMarketProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketProductServiceImpl extends ServiceImpl<MarketProductMapper, MarketProduct>
        implements IMarketProductService {

    private static final int COURSE_NAME_SNAPSHOT_MAX_LENGTH = 1000;

    private final MarketProductCourseMapper marketProductCourseMapper;
    private final ISysCourseService sysCourseService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(MarketProductCreateReq req) {
        CourseSelection selection = validateCourseSelection(req.getCourseIds());
        MarketProduct product = new MarketProduct();
        applyProductFields(product, req.getProductName(), req.getOldPrice(), req.getNewPrice(), selection.courseName());
        product.setCreatedUserId(getCurrentUserId());
        this.save(product);
        saveCourseRelations(product.getId(), selection.courseIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, MarketProductUpdateReq req) {
        MarketProduct product = requireProduct(id);
        CourseSelection selection = validateCourseSelection(req.getCourseIds());
        applyProductFields(product, req.getProductName(), req.getOldPrice(), req.getNewPrice(), selection.courseName());
        this.updateById(product);

        marketProductCourseMapper.delete(Wrappers.<MarketProductCourse>lambdaQuery()
                .eq(MarketProductCourse::getProductId, id));
        saveCourseRelations(id, selection.courseIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        requireProduct(id);
        marketProductCourseMapper.delete(Wrappers.<MarketProductCourse>lambdaQuery()
                .eq(MarketProductCourse::getProductId, id));
        this.removeById(id);
    }

    @Override
    public MarketProductRes getProductDetail(Long id) {
        MarketProduct product = requireProduct(id);
        return MarketProductRes.from(product, listCourseIds(id));
    }

    @Override
    public PageResult<MarketProductRes> pageProducts(MarketProductPageReq req) {
        String keyword = StrUtil.trim(req.getKeyword());
        IPage<MarketProduct> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<MarketProduct>lambdaQuery()
                        .and(StrUtil.isNotBlank(keyword), query -> query
                                .like(MarketProduct::getProductName, keyword)
                                .or()
                                .like(MarketProduct::getCourseName, keyword))
                        .orderByDesc(MarketProduct::getId)
        );
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }

        Map<Long, List<Long>> courseIdsByProductId = listCourseIdsByProductIds(
                page.getRecords().stream().map(MarketProduct::getId).toList()
        );
        List<MarketProductRes> records = page.getRecords().stream()
                .map(product -> MarketProductRes.from(
                        product,
                        courseIdsByProductId.getOrDefault(product.getId(), Collections.emptyList())
                ))
                .toList();
        return new PageResult<>(page.getTotal(), records);
    }

    @Override
    public List<MarketCourseOptionRes> listCourseOptions() {
        return sysCourseService.lambdaQuery()
                .orderByAsc(SysCourse::getCourseName)
                .orderByAsc(SysCourse::getId)
                .list()
                .stream()
                .map(MarketCourseOptionRes::from)
                .toList();
    }

    private MarketProduct requireProduct(Long id) {
        MarketProduct product = this.getById(id);
        if (product == null) {
            throw BusinessException.PRODUCT_NOT_EXIST.newInstance("产品不存在");
        }
        return product;
    }

    private CourseSelection validateCourseSelection(List<Long> requestedCourseIds) {
        LinkedHashSet<Long> uniqueCourseIds = new LinkedHashSet<>(requestedCourseIds);
        if (uniqueCourseIds.size() != requestedCourseIds.size()) {
            throw BusinessException.PARAMS_ERROR.newInstance("关联课程不能重复选择");
        }

        List<Long> courseIds = uniqueCourseIds.stream().toList();
        Map<Long, SysCourse> courseMap = sysCourseService.listByIds(courseIds).stream()
                .collect(Collectors.toMap(SysCourse::getId, Function.identity()));
        if (courseMap.size() != courseIds.size()) {
            throw BusinessException.COURSE_NOT_EXIST.newInstance("部分关联课程不存在，请刷新后重试");
        }

        String courseName = courseIds.stream()
                .map(courseMap::get)
                .map(SysCourse::getCourseName)
                .collect(Collectors.joining("/"));
        if (courseName.length() > COURSE_NAME_SNAPSHOT_MAX_LENGTH) {
            throw BusinessException.PARAMS_ERROR.newInstance("所选课程名称总长度不能超过1000个字符");
        }
        return new CourseSelection(courseIds, courseName);
    }

    private void applyProductFields(
            MarketProduct product,
            String productName,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            String courseName) {
        product.setProductName(StrUtil.trim(productName));
        product.setOldPrice(oldPrice);
        product.setNewPrice(newPrice);
        product.setCourseName(courseName);
    }

    private void saveCourseRelations(Long productId, List<Long> courseIds) {
        courseIds.forEach(courseId -> {
            MarketProductCourse relation = new MarketProductCourse();
            relation.setProductId(productId);
            relation.setCourseId(courseId);
            marketProductCourseMapper.insert(relation);
        });
    }

    private List<Long> listCourseIds(Long productId) {
        return marketProductCourseMapper.selectList(Wrappers.<MarketProductCourse>lambdaQuery()
                        .eq(MarketProductCourse::getProductId, productId)
                        .orderByAsc(MarketProductCourse::getId))
                .stream()
                .map(MarketProductCourse::getCourseId)
                .toList();
    }

    private Map<Long, List<Long>> listCourseIdsByProductIds(List<Long> productIds) {
        return marketProductCourseMapper.selectList(Wrappers.<MarketProductCourse>lambdaQuery()
                        .in(MarketProductCourse::getProductId, productIds)
                        .orderByAsc(MarketProductCourse::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        MarketProductCourse::getProductId,
                        LinkedHashMap::new,
                        Collectors.mapping(MarketProductCourse::getCourseId, Collectors.toList())
                ));
    }

    private Long getCurrentUserId() {
        LoginSession loginInfo = AuthThreadlocal.getLoginInfo();
        if (loginInfo == null) {
            return null;
        }
        return loginInfo.getPrincipalId();
    }

    private record CourseSelection(List<Long> courseIds, String courseName) {
    }
}
