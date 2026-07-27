package com.itcjy.emp.service.impl.market;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.interceptor.AuthThreadlocal;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.market.MarketPrepaymentOrderMapper;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.entity.MarketProduct;
import com.itcjy.emp.pojo.entity.SysRole;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.entity.SysUserRole;
import com.itcjy.emp.pojo.req.market.MarketPrepaymentOrderCreateReq;
import com.itcjy.emp.pojo.req.market.MarketPrepaymentOrderPageReq;
import com.itcjy.emp.pojo.req.market.MarketPrepaymentOrderUpdateReq;
import com.itcjy.emp.pojo.res.market.MarketEducationOptionRes;
import com.itcjy.emp.pojo.res.market.MarketPrepaymentOrderCreateRes;
import com.itcjy.emp.pojo.res.market.MarketPrepaymentOrderRes;
import com.itcjy.emp.pojo.res.market.MarketProductOptionRes;
import com.itcjy.emp.pojo.res.market.MarketSalespersonOptionRes;
import com.itcjy.emp.pojo.res.system.SysConfigItemRes;
import com.itcjy.emp.pojo.res.system.SysConfigTypeRes;
import com.itcjy.emp.pojo.res.system.LoginInfo;
import com.itcjy.emp.service.market.IMarketPrepaymentOrderService;
import com.itcjy.emp.service.market.IMarketProductService;
import com.itcjy.emp.service.market.IStudentService;
import com.itcjy.emp.service.system.ISysConfigService;
import com.itcjy.emp.service.system.ISysRoleService;
import com.itcjy.emp.service.system.ISysUserRoleService;
import com.itcjy.emp.service.system.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketPrepaymentOrderServiceImpl
        extends ServiceImpl<MarketPrepaymentOrderMapper, MarketPrepaymentOrder>
        implements IMarketPrepaymentOrderService {

    private static final String SALES_ROLE_CODE = "SALES";
    private static final String EDUCATION_RULE_TYPE_CODE = "Educational_Background_Rule";

    private final IMarketProductService marketProductService;
    private final IStudentService studentService;
    private final ISysUserService sysUserService;
    private final ISysRoleService sysRoleService;
    private final ISysUserRoleService sysUserRoleService;
    private final ISysConfigService sysConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketPrepaymentOrderCreateRes addOrder(MarketPrepaymentOrderCreateReq req) {
        MarketProduct product = requireProduct(req.getProductId());
        SysUser salesperson = requireSalesperson(req.getSalespersonUserId());
        validateEducation(req.getEducation(), null);
        Long createdUserId = getCurrentUserId();

        IStudentService.StudentCreationResult studentCreation = studentService.createIfAbsent(
                req.getName(),
                req.getPhone(),
                req.getEmail(),
                createdUserId
        );

        MarketPrepaymentOrder order = new MarketPrepaymentOrder();
        applyOrderFields(
                order,
                req.getName(),
                req.getPhone(),
                req.getEmail(),
                req.getEducation(),
                req.getGraduateSchool(),
                req.getHomeAddress(),
                req.getBirthday(),
                product,
                salesperson
        );
        order.setCreatedUserId(createdUserId);
        this.save(order);
        return new MarketPrepaymentOrderCreateRes(
                order.getId(),
                studentCreation.created(),
                StrUtil.trim(req.getPhone()),
                studentCreation.initialPassword()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, MarketPrepaymentOrderUpdateReq req) {
        MarketPrepaymentOrder order = requireOrder(id);
        MarketProduct product = requireProduct(req.getProductId());
        SysUser salesperson = requireSalesperson(req.getSalespersonUserId());
        validateEducation(req.getEducation(), order.getEducation());
        applyOrderFields(
                order,
                req.getName(),
                req.getPhone(),
                req.getEmail(),
                req.getEducation(),
                req.getGraduateSchool(),
                req.getHomeAddress(),
                req.getBirthday(),
                product,
                salesperson
        );
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        requireOrder(id);
        this.removeById(id);
    }

    @Override
    public MarketPrepaymentOrderRes getOrderDetail(Long id) {
        return MarketPrepaymentOrderRes.from(requireOrder(id));
    }

    @Override
    public PageResult<MarketPrepaymentOrderRes> pageOrders(MarketPrepaymentOrderPageReq req) {
        String keyword = StrUtil.trim(req.getKeyword());
        IPage<MarketPrepaymentOrder> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<MarketPrepaymentOrder>lambdaQuery()
                        .and(StrUtil.isNotBlank(keyword), query -> query
                                .like(MarketPrepaymentOrder::getName, keyword)
                                .or().like(MarketPrepaymentOrder::getPhone, keyword)
                                .or().like(MarketPrepaymentOrder::getEmail, keyword)
                                .or().like(MarketPrepaymentOrder::getGraduateSchool, keyword)
                                .or().like(MarketPrepaymentOrder::getProductName, keyword)
                                .or().like(MarketPrepaymentOrder::getSalespersonName, keyword))
                        .orderByDesc(MarketPrepaymentOrder::getId)
        );
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }
        return new PageResult<>(
                page.getTotal(),
                page.getRecords().stream().map(MarketPrepaymentOrderRes::from).toList()
        );
    }

    @Override
    public List<MarketSalespersonOptionRes> listSalespersonOptions() {
        SysRole salesRole = findActiveSalesRole();
        if (salesRole == null) {
            return Collections.emptyList();
        }
        List<Long> userIds = sysUserRoleService.lambdaQuery()
                .eq(SysUserRole::getRoleId, salesRole.getId())
                .list()
                .stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysUserService.listByIds(userIds).stream()
                .filter(user -> ActiveEnum.ACTIVE.name().equals(user.getStatus()))
                .sorted(Comparator.comparing(this::getUserDisplayName))
                .map(user -> new MarketSalespersonOptionRes(
                        user.getId(),
                        getUserDisplayName(user),
                        user.getPhone()
                ))
                .toList();
    }

    @Override
    public List<MarketProductOptionRes> listProductOptions() {
        return marketProductService.lambdaQuery()
                .orderByAsc(MarketProduct::getProductName)
                .orderByAsc(MarketProduct::getId)
                .list()
                .stream()
                .map(MarketProductOptionRes::from)
                .toList();
    }

    @Override
    public List<MarketEducationOptionRes> listEducationOptions() {
        SysConfigTypeRes educationType = sysConfigService.listTypes().stream()
                .filter(type -> EDUCATION_RULE_TYPE_CODE.equalsIgnoreCase(type.typeCode()))
                .filter(type -> ActiveEnum.ACTIVE.name().equals(type.status()))
                .findFirst()
                .orElse(null);
        if (educationType == null) {
            return Collections.emptyList();
        }
        return sysConfigService.listItems(educationType.id()).stream()
                .filter(item -> ActiveEnum.ACTIVE.name().equals(item.status()))
                .map(this::toEducationOption)
                .toList();
    }

    private MarketPrepaymentOrder requireOrder(Long id) {
        MarketPrepaymentOrder order = this.getById(id);
        if (order == null) {
            throw BusinessException.PREPAY_ORDER_NOT_EXIST.newInstance("预订单不存在");
        }
        return order;
    }

    private MarketProduct requireProduct(Long productId) {
        MarketProduct product = marketProductService.getById(productId);
        if (product == null) {
            throw BusinessException.PRODUCT_NOT_EXIST.newInstance("意向产品不存在");
        }
        return product;
    }

    private SysUser requireSalesperson(Long userId) {
        SysUser salesperson = sysUserService.getById(userId);
        if (salesperson == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("销售人员不存在");
        }
        if (!ActiveEnum.ACTIVE.name().equals(salesperson.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("所选销售人员已禁用");
        }
        SysRole salesRole = findActiveSalesRole();
        if (salesRole == null) {
            throw BusinessException.ROLE_NOT_EXIST.newInstance("SALES角色不存在或已禁用");
        }
        boolean hasSalesRole = sysUserRoleService.lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, salesRole.getId())
                .exists();
        if (!hasSalesRole) {
            throw BusinessException.DATA_ERROR.newInstance("所选用户不属于SALES角色");
        }
        return salesperson;
    }

    private SysRole findActiveSalesRole() {
        return sysRoleService.lambdaQuery()
                .eq(SysRole::getStatus, ActiveEnum.ACTIVE.name())
                .list()
                .stream()
                .filter(role -> SALES_ROLE_CODE.equalsIgnoreCase(role.getRoleCode()))
                .findFirst()
                .orElse(null);
    }

    private void validateEducation(String education, String currentEducation) {
        String normalizedEducation = normalizeNullable(education);
        if (normalizedEducation == null
                || normalizedEducation.equals(normalizeNullable(currentEducation))) {
            return;
        }
        boolean valid = listEducationOptions().stream()
                .anyMatch(option -> option.value().equals(normalizedEducation));
        if (!valid) {
            throw BusinessException.CONFIG_ERROR.newInstance("所选学历不在当前启用的学历规则中");
        }
    }

    private MarketEducationOptionRes toEducationOption(SysConfigItemRes item) {
        String label = StrUtil.isNotBlank(item.description()) ? item.description() : item.itemValue();
        return new MarketEducationOptionRes(item.itemValue(), label);
    }

    private void applyOrderFields(
            MarketPrepaymentOrder order,
            String name,
            String phone,
            String email,
            String education,
            String graduateSchool,
            String homeAddress,
            LocalDate birthday,
            MarketProduct product,
            SysUser salesperson) {
        order.setName(StrUtil.trim(name));
        order.setPhone(StrUtil.trim(phone));
        order.setEmail(normalizeNullable(email));
        order.setEducation(normalizeNullable(education));
        order.setGraduateSchool(normalizeNullable(graduateSchool));
        order.setHomeAddress(normalizeNullable(homeAddress));
        order.setBirthday(birthday);
        order.setProductId(product.getId());
        order.setProductName(product.getProductName());
        order.setProductPrice(normalizePrice(product.getNewPrice()));
        order.setSalespersonUserId(salesperson.getId());
        order.setSalespersonName(getUserDisplayName(salesperson));
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }

    private String normalizeNullable(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private String getUserDisplayName(SysUser user) {
        if (StrUtil.isNotBlank(user.getRealName())) {
            return user.getRealName();
        }
        if (StrUtil.isNotBlank(user.getNickname())) {
            return user.getNickname();
        }
        return StrUtil.blankToDefault(user.getUsername(), "未命名销售");
    }

    private Long getCurrentUserId() {
        LoginInfo loginInfo = AuthThreadlocal.getLoginInfo();
        if (loginInfo == null || loginInfo.getUserDetailRes() == null) {
            throw BusinessException.USER_NO_TOKEN;
        }
        return loginInfo.getUserDetailRes().getId();
    }
}
