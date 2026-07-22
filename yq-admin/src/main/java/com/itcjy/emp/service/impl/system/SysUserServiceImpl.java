package com.itcjy.emp.service.impl.system;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.constants.TokenConstants;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.common.properties.JwtProperties;
import com.itcjy.common.utils.JwtUtil;
import com.itcjy.emp.mapper.system.SysUserMapper;
import com.itcjy.emp.pojo.entity.*;
import com.itcjy.emp.pojo.req.system.LoginReq;
import com.itcjy.emp.pojo.req.system.SysUserPageReq;
import com.itcjy.emp.pojo.req.system.SysUserReq;
import com.itcjy.emp.pojo.req.system.SysUserUpdateReq;
import com.itcjy.emp.pojo.res.system.LoginInfo;
import com.itcjy.emp.pojo.res.system.LoginRes;
import com.itcjy.emp.pojo.res.system.SysUserRes;
import com.itcjy.emp.pojo.res.system.UserDetailRes;
import com.itcjy.emp.service.system.*;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ISysUserRoleService sysUserRoleService;
    @Resource
    private ISysRolePermissionService sysRolePermissionService;
    @Resource
    private ISysPermissionService sysPermissionService;
    @Resource
    private ISysRoleService sysRoleService;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private JwtProperties jwtProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(SysUserReq req) {
        SysUser sysUser = BeanUtil.copyProperties(req, SysUser.class);
        this.save(sysUser);
        sysUserRoleService.assignDefaultRole(sysUser.getId());
    }

    @Override
    public boolean existsById(Long id) {
        return this.lambdaQuery().eq(SysUser::getId, id).exists();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
        if ("ACTIVE".equals(user.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("启用状态的用户不能删除，请先禁用");
        }
        sysUserRoleService.remove(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, id));
        this.removeById(id);
    }

    @Override
    public PageResult<SysUserRes> pageUsers(SysUserPageReq req) {
        List<Long> roleUserIds = null;
        if (req.getRoleId() != null) {
            roleUserIds = sysUserRoleService.list(Wrappers.<SysUserRole>lambdaQuery()
                            .eq(SysUserRole::getRoleId, req.getRoleId()))
                    .stream()
                    .map(SysUserRole::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            if (roleUserIds.isEmpty()) {
                return new PageResult<>(0L, Collections.emptyList());
            }
        }

        IPage<SysUser> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<SysUser>lambdaQuery()
                        .in(roleUserIds != null, SysUser::getId, roleUserIds)
                        .like(StrUtil.isNotBlank(req.getUsername()), SysUser::getUsername, req.getUsername())
                        .like(StrUtil.isNotBlank(req.getNickname()), SysUser::getNickname, req.getNickname())
                        .like(StrUtil.isNotBlank(req.getRealName()), SysUser::getRealName, req.getRealName())
                        .orderByAsc(SysUser::getId)
        );
        List<SysUser> users = page.getRecords();
        if (users.isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }

        // 批量查询当前页用户的角色关系，再批量查角色，避免 N+1
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        List<SysUserRole> userRoles = sysUserRoleService.list(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, userIds));

        Map<Long, SysRole> roleMap = Collections.emptyMap();
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).distinct().collect(Collectors.toList());
            roleMap = sysRoleService.listByIds(roleIds).stream()
                    .collect(Collectors.toMap(SysRole::getId, Function.identity()));
        }

        // 按 userId 分组角色列表
        Map<Long, SysRole> finalRoleMap = roleMap;
        Map<Long, List<SysRole>> userRoleMap = userRoles.stream()
                .map(ur -> {
                    SysRole role = finalRoleMap.get(ur.getRoleId());
                    return role != null ? Map.entry(ur.getUserId(), role) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        List<SysUserRes> records = users.stream().map(user -> {
            SysUserRes res = BeanUtil.copyProperties(user, SysUserRes.class);
            res.setPassword(null);
            res.setRoles(userRoleMap.getOrDefault(user.getId(), Collections.emptyList()));
            return res;
        }).collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), records);
    }

    @Override
    public void updateUser(Long id, SysUserUpdateReq req) {
        if (!existsById(id)) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
        SysUser sysUser = BeanUtil.copyProperties(req, SysUser.class);
        sysUser.setId(id);
        this.updateById(sysUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.PARAMS_ERROR.newInstance("删除ID列表不能为空");
        }
        List<SysUser> users = this.listByIds(ids);
        if (users.size() != ids.size()) {
            throw BusinessException.USER_NOT_EXIST.newInstance("部分用户不存在");
        }
        List<String> activeNames = users.stream()
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .collect(Collectors.toList());
        if (!activeNames.isEmpty()) {
            throw BusinessException.DATA_ERROR.newInstance(
                    "用户「" + String.join("、", activeNames) + "」处于启用状态，不能删除，请先禁用");
        }
        sysUserRoleService.remove(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, ids));
        this.removeByIds(ids);
    }

    /**
     * 员工登录
     * @param req
     * @return
     */
    @Override
    public LoginRes empLogin(LoginReq req) {
        //1.解析请求参数
        String username = req.getUsername();
        String password = req.getPassword();
        //2.校验用户是否存在
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username);
        SysUser user = sysUserMapper.selectOne(queryWrapper);
        if (user == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("用户不存在");
        }
        //3.校验用户状态
        if (!ActiveEnum.ACTIVE.name().equals(user.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("用户已禁用");
        }
        //4.校验密码
        if (!password.equals(user.getPassword())) {
            throw BusinessException.PASSWORD_ERROR.newInstance("密码错误");
        }
        //获取用户相关信息
        //5.获取用户角色code（roleIds 只计算一次，后续复用）
        List<SysUserRole> userRoles = sysUserRoleService.list(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, user.getId()));
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        List<String> roleCodes = Collections.emptyList();
        if (!roleIds.isEmpty()) {
            roleCodes = sysRoleService.listByIds(roleIds).stream()
                    .map(SysRole::getRoleCode)
                    .collect(Collectors.toList());
        }
        // 6. 获取用户权限 code
        Set<String> loginPermissions = new HashSet<>();
        if (!roleIds.isEmpty()) {
            List<SysRolePermission> rolePermissions = sysRolePermissionService.list(
                    new LambdaQueryWrapper<SysRolePermission>()
                            .in(SysRolePermission::getRoleId, roleIds)
            );
            List<Long> permissionIds = rolePermissions.stream()
                    .map(SysRolePermission::getPermissionId)
                    .distinct()
                    .collect(Collectors.toList());
            loginPermissions = sysPermissionService.listByIds(permissionIds).stream()
                    .map(SysPermission::getPermissionCode)
                    .collect(Collectors.toSet());
        }
        List<String> loginPermissionList = new ArrayList<>(loginPermissions);
        //7. 构建保存到redis里面的信息
        UserDetailRes userDetailRes = buildUserDetail(user);
        LoginInfo loginInfo = new LoginInfo();
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCodes);
        loginInfo.setToken(token);
        loginInfo.setSignSecret(createSignSecret());
        loginInfo.setUserDetailRes(userDetailRes);
        loginInfo.setRoles(roleCodes);
        loginInfo.setPermissions(loginPermissionList);
        //7.2 将登录信息保存到 redis（TTL 与 JWT 有效期保持一致）
        redisTemplate.opsForValue().set(
                TokenConstants.USER_JWT_KEY_PREFIX + user.getId(),
                loginInfo,
                jwtProperties.getExpireMillis(),
                TimeUnit.MILLISECONDS
        );
        //8. 返回登录信息
        LoginRes loginRes = new LoginRes();
        loginRes.setToken(token);
        loginRes.setSignSecret(loginInfo.getSignSecret());
        loginRes.setUserDetailRes(userDetailRes);
        return loginRes;

    }

    @Override
    public void logout(String authorization) {
        String token = jwtUtil.resolveToken(authorization);
        if (StrUtil.isBlank(token)) {
            throw BusinessException.USER_NO_TOKEN;
        }

        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw BusinessException.JWT_ERROR;
        }
        if (userId == null) {
            throw BusinessException.JWT_ERROR;
        }

        redisTemplate.delete(TokenConstants.USER_JWT_KEY_PREFIX + userId);
    }

    private UserDetailRes buildUserDetail(SysUser user) {
        UserDetailRes userDetailRes = BeanUtil.copyProperties(user, UserDetailRes.class);
        if (user.getCreatedAt() != null) {
            userDetailRes.setCreatedAt(Date.from(user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
        }
        if (user.getUpdatedAt() != null) {
            userDetailRes.setUpdatedAt(Date.from(user.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()));
        }
        userDetailRes.setStatusDesc(ActiveEnum.ACTIVE.name().equals(user.getStatus()) ? "启用" : "禁用");
        return userDetailRes;
    }
    /*
     * 创建签名密钥
     */
    private String createSignSecret() {
        // 1. 创建一个 32 字节（256 位）的字节数组
        byte[] bytes = new byte[32];

        // 2. 使用密码学安全的随机数生成器填充这 32 字节
        //    SECURE_RANDOM 通常是 SecureRandom 实例，比普通 Random 更安全
        TokenConstants.SECURE_RANDOM.nextBytes(bytes);

        // 3. 将字节数组编码为 Base64 URL 安全字符串（无填充 '='）
        //    例如输出: "x7G_2kP9mN4qR8sT1uW3yZ5aB6cD0eF"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
