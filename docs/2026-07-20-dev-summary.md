# 2026-07-20 项目开发交接记录

> 项目路径：`D:\Developer\IDEAProject\yq`  
> 后端模块：`yq-admin`  
> 主要技术栈：Spring Boot 3.2、Java 17、MyBatis-Plus、MySQL、Springdoc Swagger、JSR303、AOP、Logback

本文用于给后续接手的大模型或开发者快速理解今天已经完成的代码内容、核心入口和后续注意点。

## 1. 今日主要完成内容

### 1.1 基础三张表实体生成

根据数据库 SQL，补充了用户相关实体，主要在：

- `com.itcjy.emp.pojo.entity.SysUser`
- `com.itcjy.emp.pojo.entity.SysRole`
- `com.itcjy.emp.pojo.entity.SysPermission`

后续 RBAC 相关又补充了中间表实体：

- `com.itcjy.emp.pojo.entity.SysUserRole`
- `com.itcjy.emp.pojo.entity.SysRolePermission`

对应 Mapper 位于：

- `com.itcjy.emp.mapper.SysUserMapper`
- `com.itcjy.emp.mapper.SysRoleMapper`
- `com.itcjy.emp.mapper.SysPermissionMapper`
- `com.itcjy.emp.mapper.SysUserRoleMapper`
- `com.itcjy.emp.mapper.SysRolePermissionMapper`

### 1.2 异常包整理

修正并完善了公共异常处理相关代码：

- `com.itcjy.common.exception.BusinessException`
- `com.itcjy.common.exception.GlobalExceptionHandler`

目标是让业务异常、参数校验异常等统一返回 `ApiResponse` 格式。

### 1.3 Swagger 注解

项目已引入 Springdoc 依赖：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.1.0</version>
</dependency>
```

相关接口、请求对象、实体类添加了 Swagger 注解，主要使用：

- `@Tag`
- `@Operation`
- `@Parameter`
- `@ParameterObject`
- `@Schema`

Swagger 配置在 `application.yml`：

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
  packages-to-scan: com.itcjy.emp.controller
  paths-to-match: /emp/**
```

### 1.4 JSR303 参数校验和状态自定义校验

已添加 Spring Validation 依赖，并对请求参数增加校验注解。

公共状态枚举和自定义校验位于：

- `com.itcjy.common.myEnum.ActiveEnum`
- `com.itcjy.common.annotations.StatusEnum`
- `com.itcjy.common.validator.EnumValueValidator`

典型使用方式：

```java
@StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
private String status;
```

用户分页请求已抽取为请求类，避免 Controller 参数过长：

- `com.itcjy.emp.pojo.req.SysUserPageReq`

### 1.5 Controller AOP 日志

已使用 AOP 对 Controller 接口统一打印日志。

核心文件：

- `com.itcjy.common.aspect.ControllerLogAspect`

日志内容包括：

- 请求 URL
- 请求方式
- 入参
- 出参
- 异常信息
- requestId

### 1.6 MDC + requestId

为了解决高并发下日志不好区分的问题，已使用 MDC 给每个请求增加唯一 `requestId`。

- `MDC.put()` 把 requestId 绑定到**当前线程**

核心文件：

- `com.itcjy.common.filter.RequestIdFilter`

日志格式中包含：

```xml
%X{requestId}
```

这样同一个请求的日志可以通过同一个 `requestId` 分组查看。

### 1.7 日志输出到文件和滚动策略

日志配置文件：

- `yq-admin/src/main/resources/logback-spring.xml`

自定义文件 Appender：

- `com.itcjy.common.logging.RandomSizeFileAppender`

当前策略：

- 日志同时输出到控制台和文件
- 日志目录通过 `app.logging.path` 配置
- 单个日志文件超过 `2MB` 后生成新文件
- 文件名使用“日期_随机数”的形式

配置入口：

```yaml
app:
  logging:
    path: ${APP_LOG_PATH:logs}
```

### 1.8 .gitignore

项目根目录已添加 `.gitignore`，忽略内容包括：

- Maven / Java 构建产物：`target/`、`*.class`、`*.jar` 等
- IDE 文件：`.idea/`、`*.iml`
- 日志：`logs/`、`*.log`
- 前端目录产物：`node_modules/`、`dist/`
- 本地环境文件：`.env`、`.env.*`

## 2. RBAC 权限体系完成情况

当前 RBAC 使用 5 张表：

- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`

### 2.1 权限注解

自定义注解：

- `com.itcjy.common.annotations.HasPermission`

定义：

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HasPermission {
    String code();
    String name();
    String description() default "";
}
```

Controller 方法通过该注解声明权限，例如：

```java
@HasPermission(code = "sys:user:update", name = "修改用户", description = "根据ID修改系统用户")
```

### 2.2 启动同步接口权限到权限表

核心类：

- `com.itcjy.emp.config.PermissionSyncRunner`

作用：

- 应用启动后扫描 Spring MVC 接口方法
- 找到标记了 `@HasPermission` 的接口
- 将权限编码、权限名称、接口路径、权限描述同步到 `sys_permission`
- 数据库不存在则新增，已存在则更新

配置：

```yaml
app:
  permission-sync:
    enabled: true
```

### 2.3 固定系统角色启动初始化

核心类：

- `com.itcjy.emp.config.SystemRoleInitRunner`

默认初始化角色：

- `ADMIN`：超级管理员
- `LECTURER`：讲师
- `STUDENT`：学生
- `OPERATOR`：运营人员

配置：

```yaml
app:
  role-init:
    enabled: true
```

初始化逻辑只插入缺失角色，不覆盖数据库已有角色。

### 2.4 ADMIN 启动自动拥有全部权限

核心类：

- `com.itcjy.emp.config.AdminPermissionInitRunner`

启动顺序：

1. `SystemRoleInitRunner`，`@Order(0)`，先初始化固定角色
2. `PermissionSyncRunner`，`@Order(1)`，再同步接口权限到 `sys_permission`
3. `AdminPermissionInitRunner`，`@Order(2)`，最后给 ADMIN 补齐全部权限

配置：

```yaml
app:
  admin-permission-init:
    enabled: true
    role-code: ${APP_ADMIN_ROLE_CODE:ADMIN}
```

行为说明：

- 查询 `role_code = ADMIN` 的角色
- 查询 `sys_permission` 全部权限
- 查询 ADMIN 已拥有的权限
- 只插入缺失的 `sys_role_permission`
- 不会删除已有授权
- 如果手动删除 ADMIN 的某个权限，下次启动会自动补回来

### 2.5 角色授权接口

Controller：

- `com.itcjy.emp.controller.SysRolePermissionController`

主要能力：

- 查询角色已拥有权限
- 给角色分配权限

Service：

- `com.itcjy.emp.service.ISysRolePermissionService`
- `com.itcjy.emp.service.impl.SysRolePermissionServiceImpl`

请求对象：

- `com.itcjy.emp.pojo.req.RolePermissionReq`

当前角色授权采用“全量覆盖”思路：传入某角色的权限 ID 列表后，重新维护该角色的权限关系。

### 2.6 用户角色分配接口

Controller：

- `com.itcjy.emp.controller.SysUserRoleController`

主要能力：

- 查询用户已拥有角色
- 给用户手动分配角色

Service：

- `com.itcjy.emp.service.ISysUserRoleService`
- `com.itcjy.emp.service.impl.SysUserRoleServiceImpl`

请求对象：

- `com.itcjy.emp.pojo.req.UserRoleReq`

### 2.7 默认用户角色

新增用户时默认分配角色，默认角色编码来自 yml 配置，不写死在业务代码里。

配置：

```yaml
app:
  user:
    default-role-code: ${APP_DEFAULT_USER_ROLE_CODE:LECTURER}
```

核心逻辑：

- `SysUserServiceImpl.addUser`
- `SysUserRoleServiceImpl.assignDefaultRole`

当前默认是新增用户分配 `LECTURER` 角色。

## 3. 用户管理接口完成情况

Controller：

- `com.itcjy.emp.controller.SysUserController`

已完成接口：

- `POST /emp/sysUser/add`：新增用户
- `DELETE /emp/sysUser/delete/{id}`：删除用户，并清理 `sys_user_role`
- `DELETE /emp/sysUser/batchDelete`：批量删除用户（RequestBody 传 `List<Long>` ID 列表）
- `PUT /emp/sysUser/update/{id}`：修改用户
- `GET /emp/sysUser/get/{id}`：查询用户详情
- `GET /emp/sysUser/page`：分页查询用户

删除用户规则：

- 只有 `status = INACTIVE`（禁用）的用户才允许删除，启用状态（`ACTIVE`）的用户不能删除，需先禁用
- 批量删除时如果存在启用状态的用户，会返回具体哪些用户不能删除
- 删除用户时同步清理 `sys_user_role` 关联关系

分页查询支持模糊查询字段：

- `username`
- `nickname`
- `realName`
- `real_name` 兼容参数

分页使用 MyBatis-Plus 分页能力，分页插件配置在：

- `com.itcjy.emp.config.MybatisPlusConfig`

### 3.1 用户编辑请求对象已拆分

新增用户仍使用：

- `com.itcjy.emp.pojo.req.SysUserReq`

修改用户使用：

- `com.itcjy.emp.pojo.req.SysUserUpdateReq`

原因：

- 新增用户需要 `password`
- 编辑用户不应该携带 `password`
- 避免修改接口误改密码
- 避免 Swagger 暴露编辑时不该填写的密码字段

### 3.2 用户响应对象

新增响应视图对象：

- `com.itcjy.emp.pojo.res.SysUserRes`

该对象继承 `SysUser`，额外携带角色列表：

```java
private List<SysRole> roles;
```

用于用户详情或列表需要展示用户角色时使用。

## 4. 角色管理接口完成情况

Controller：

- `com.itcjy.emp.controller.SysRoleController`

已完成接口：

- `POST /emp/sysRole/add`：新增角色
- `DELETE /emp/sysRole/delete/{id}`：删除角色
- `DELETE /emp/sysRole/batchDelete`：批量删除角色（RequestBody 传 `List<Long>` ID 列表）
- `PUT /emp/sysRole/update/{id}`：修改角色
- `GET /emp/sysRole/get/{id}`：查询角色详情
- `GET /emp/sysRole/page`：分页查询角色

删除角色规则：

- 只有 `status = INACTIVE`（禁用）的角色才允许删除，启用状态（`ACTIVE`）的角色不能删除，需先禁用
- 如果该角色已经被用户使用，也就是 `sys_user_role` 中存在该角色 ID，则抛出业务异常，禁止删除
- 批量删除时会检查所有选中角色，如果有启用状态或被用户使用的角色，会返回具体哪些角色不能删除
- 如果未被用户使用，删除角色前会清理 `sys_role_permission`

Service：

- `com.itcjy.emp.service.ISysRoleService`
- `com.itcjy.emp.service.impl.SysRoleServiceImpl`

请求对象：

- `com.itcjy.emp.pojo.req.SysRoleReq`
- `com.itcjy.emp.pojo.req.SysRolePageReq`

## 5. 权限查询接口完成情况

Controller：

- `com.itcjy.emp.controller.SysPermissionController`

已完成接口：

- 查询权限列表
- 分页查询权限
- 根据 ID 查询权限详情

请求对象：

- `com.itcjy.emp.pojo.req.SysPermissionPageReq`

支持筛选字段：

- `permissionCode`
- `permissionName`
- `apiPath`
- `status`

注意：权限不建议手动新增、修改、删除，权限来源应该以 Controller 上的 `@HasPermission` 注解和启动同步为准。

## 6. 当前 application.yml 关键配置

```yaml
app:
  logging:
    path: ${APP_LOG_PATH:logs}
  permission-sync:
    enabled: true
  role-init:
    enabled: true
  admin-permission-init:
    enabled: true
    role-code: ${APP_ADMIN_ROLE_CODE:ADMIN}
  user:
    default-role-code: ${APP_DEFAULT_USER_ROLE_CODE:LECTURER}
```

配置说明：

- `app.logging.path`：日志文件目录
- `app.permission-sync.enabled`：是否启动时同步接口权限
- `app.role-init.enabled`：是否启动时初始化固定系统角色
- `app.admin-permission-init.enabled`：是否启动时给 ADMIN 补齐全部权限
- `app.admin-permission-init.role-code`：超级管理员角色编码
- `app.user.default-role-code`：新增用户默认角色编码

## 7. 前端管理台（yq-admin-web）

技术栈：Vue 2.7 + Element UI + Axios，开发服务器端口 5173，代理 `/yq-admin` 到 `localhost:8080`。

核心文件：

- `yq-admin-web/src/api/user.js`：用户管理 API（含 `batchDeleteUsers`）
- `yq-admin-web/src/api/role.js`：角色管理 API（含 `batchDeleteRoles`）
- `yq-admin-web/src/api/permission.js`：权限管理 API
- `yq-admin-web/src/views/user/UserManage.vue`：用户管理页面
- `yq-admin-web/src/views/role/RoleManage.vue`：角色管理页面
- `yq-admin-web/src/views/permission/PermissionManage.vue`：权限管理页面

前端批量删除交互：

- 用户/角色表格均增加了多选列（`type="selection"`）
- 表格上方增加「批量删除」按钮，未选中时禁用
- 选中后显示「已选 N 项」提示
- 点击批量删除弹出确认框，确认后调用 `DELETE /emp/sysUser/batchDelete` 或 `DELETE /emp/sysRole/batchDelete`
- 请求体为纯 JSON 数组 `[1, 2, 3]`

## 8. 当前需要注意的问题

1. 目前权限同步只负责把 `@HasPermission` 保存进 `sys_permission`，真正的接口鉴权拦截还没有明确完成。后续如果要让权限真正生效，需要增加认证登录和权限校验逻辑。
2. ADMIN 自动授权是启动补齐模式，如果手动删除 ADMIN 权限，下次启动会补回来，这是预期行为。
3. 普通角色的权限不自动分配，需要通过角色授权接口维护。
4. 新增用户默认角色依赖 `app.user.default-role-code`，并且数据库里必须存在对应 `role_code`。当前通过角色启动初始化可以保证 `LECTURER` 存在。
5. 删除用户/角色时已经清理关联表（`sys_user_role`、`sys_role_permission`），删除角色时如果角色被用户使用会禁止删除。
6. **删除前置校验**：用户和角色只有处于 `INACTIVE`（禁用）状态才允许删除（单个删除和批量删除均生效）。启用状态需先禁用再删除。
7. 当前终端显示部分中文可能是编码显示问题，源文件实际按 UTF-8 保存。若 IDE 显示乱码，需要检查 IDEA 文件编码是否为 UTF-8。
8. `git status` 中存在未跟踪目录 `yq-admin-web/`，需要确认是否要一起提交。

## 9. 建议提交信息

如果提交今天这批 RBAC 和基础增强代码，建议使用：

```bash
git commit -m "feat: 完善 RBAC 权限管理和初始化"
```

如果只提交最近一次“用户编辑不带密码”的修复，建议使用：

```bash
git commit -m "fix: 修改用户接口移除密码字段"
```

如果把今天所有内容统一作为一次较大的功能提交，可以写：

```bash
git commit -m "feat: 完善后台用户和 RBAC 权限体系"
```

## 10. 验证记录

最近已执行并通过：

```bash
mvn -q -DskipTests compile
```

说明后端 Java 编译通过。

## 11. 后续建议

建议下一步优先做两件事：

1. 增加真正的权限校验拦截逻辑，让 `@HasPermission` 不只是同步权限表，也参与请求鉴权。
2. 对 `SysUserController` 的详情和分页返回对象做脱敏，尤其注意不要返回密码字段。