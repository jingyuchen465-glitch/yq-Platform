# 燕雀平台笔记

# 一、Swagger 注解

**1.引入 Springdoc 依赖**

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.1.0</version>
</dependency>
```

**2.Swagger 配置在 `application.yml`**

```yml
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

**3.相关接口、请求对象、实体类添加了 Swagger 注解，主要使用：**

```java
@Tag
@Operation
@Parameter
@ParameterObject
@Schema
```

# 二、JSR303 参数校验和状态自定义校验

## 2.1、普通校验

**1.添加 Spring Validation 依赖**

```xml
<!-- Spring Boot Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**2.典型使用方式：**

**dto层：**

```java
@Data
@Schema(description = "员工用户请求参数")
public class SysUserReq {

    @Schema(description = "登录用户名", example = "zhangsan")
    @NotBlank(message = "登录用户名不能为空")
    @Size(max = 64, message = "登录用户名长度不能超过64个字符")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 255, message = "登录密码长度必须在6到255个字符之间")
    private String password;

    @Schema(description = "用户昵称", example = "张三")
    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 64, message = "用户昵称长度不能超过64个字符")
    private String nickname;

    @Schema(description = "真实姓名", example = "张三")
    @Size(max = 64, message = "真实姓名长度不能超过64个字符")
    private String realName;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Schema(description = "飞书 union_id", example = "on_123456789")
    @Size(max = 128, message = "飞书 union_id 长度不能超过128个字符")
    private String unionId;

    @Schema(description = "状态", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @NotBlank(message = "状态不能为空")
    @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
    private String status;
}

```

**controller层**

```java
    @Operation(summary = "员工登录", description = "根据用户名和密码登录，返回token")
    @PostMapping("/login")
    public ApiResponse<LoginRes> empLogin(@Valid @RequestBody LoginReq req) {
        return ApiResponse.success(sysUserService.empLogin(req));
    }



    @Operation(summary = "修改员工", description = "根据ID修改员工信息")
    @PutMapping("/update/{id}")
    @HasPermission(code = "sys:user:update", name = "修改用户", description = "根据ID修改系统用户")
    public ApiResponse<Void> updateSysUser(@Parameter(description = "用户ID", required = true)
                                           @PathVariable @NotNull(message = "用户ID不能为空") Long id,
                                           @Valid @RequestBody SysUserUpdateReq req) {
        sysUserService.updateUser(id, req);
        return ApiResponse.success("修改成功");
    }
```

## 2.2、自定义校验

**使用方式：**

```java
// 用在字段上，限制该字段的值必须是 StatusEnum 枚举中的某个值
@StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
private String status;
```

**配置方法：**

**1、编写枚举类**：表示这个字段哪些是合法的

```java
@AllArgsConstructor
@NoArgsConstructor
public enum ActiveEnum {

    ACTIVE("生效"),

    INACTIVE("失效");

    @Getter
    private String desc;
}
```

**2、自定义注解StatusEnum**

```java
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
public @interface StatusEnum  {
    // 默认错误消息提示.
    String message() default "字段值不符合要求";

    //指定要校验的枚举类,比如在字段上写@EnumValue(enumClass = StatusEnum.class)
    Class<? extends Enum<?>> enumClass();


    //分组校验
    Class<?>[] groups() default {};

    //自定义负载信息
    Class<? extends Payload>[] payload() default {};
}
```

**3.编写自定义校验器**

与相关自定义注解`StatusEnum`绑定，获取注解标记的字段的值拿来和枚举类里面的合法值进行对比

```java
public class EnumValueValidator implements ConstraintValidator<StatusEnum, String> {

    private Class<? extends Enum<?>> enumClass;

    // 初始化动作, 课获取注解上属性参数.
    @Override
    public void initialize(StatusEnum constraintAnnotation) {
        enumClass = constraintAnnotation.enumClass();
    }

    // 核心校验逻辑,返回true= 校验通过, false = 校验失败.
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isEmpty(value)) {
            return true;
        }
        Enum<?>[] enumConstants = enumClass.getEnumConstants();
        for (Enum<?> enumConstant : enumConstants) {
            if (enumConstant.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
```

# 三、Controller AOP 日志

## 3.1、使用 AOP 对 Controller 接口统一打印日志。

配置一个`ControllerLogAspect`类就行

```java
/**
 * Controller 层统一日志切面
 * <p>
 * 拦截所有 Controller 方法，记录请求入参、响应结果及耗时，
 * 并对敏感字段（密码、令牌等）进行脱敏处理，避免敏感信息泄露到日志中。
 * </p>
 *
 * @author itcjy
 */
@Slf4j
@Aspect
@Component
public class ControllerLogAspect {

    /** 需要脱敏的敏感字段名集合（忽略大小写匹配） */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "oldPassword",
            "newPassword",
            "confirmPassword",
            "token",
            "accessToken",
            "refreshToken",
            "authorization"
    );

    /**
     * 切入点：匹配 com.itcjy 包及子包下所有 controller 类的 public 方法
     */
    @Pointcut("execution(public * com.itcjy..controller..*(..))")
    public void controllerMethods() {
    }

    /**
     * 环绕通知：在 Controller 方法执行前后记录日志
     * <ul>
     *     <li>执行前：记录 requestId、URL、HTTP 方法、处理方法、请求参数</li>
     *     <li>执行后：记录响应结果及耗时</li>
     *     <li>异常时：记录异常信息及耗时</li>
     * </ul>
     *
     * @param joinPoint 连接点
     * @return Controller 方法的返回值
     * @throws Throwable 方法执行过程中抛出的异常
     */
    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录开始时间，用于计算耗时
        long startTime = System.currentTimeMillis();
        // 获取当前 HTTP 请求对象
        HttpServletRequest request = getRequest();
        // 获取方法签名信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取请求追踪 ID（由 RequestIdFilter 写入 MDC）
        String requestId = getRequestId();
        // 拼接处理方法全限定名：类名.方法名
        String handler = signature.getDeclaringTypeName() + "." + signature.getName();
        // 构建完整请求 URL（含查询参数）
        String url = buildUrl(request);
        // 获取 HTTP 请求方法（GET/POST/PUT/DELETE 等）
        String httpMethod = request == null ? "UNKNOWN" : request.getMethod();
        // 将方法入参序列化为 JSON（已过滤不可序列化参数并脱敏）
        String params = toJson(buildParamMap(signature.getParameterNames(), joinPoint.getArgs()));

        // 打印请求日志
        log.info("Controller request: requestId={}, url={}, method={}, handler={}, params={}",
                requestId, url, httpMethod, handler, params);
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - startTime;
            // 打印正常响应日志
            log.info("Controller response: requestId={}, url={}, method={}, handler={}, cost={}ms, result={}",
                    requestId, url, httpMethod, handler, cost, toJson(result));
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - startTime;
            // 打印异常日志
            log.error("Controller exception: requestId={}, url={}, method={}, handler={}, cost={}ms, params={}",
                    requestId, url, httpMethod, handler, cost, params, ex);
            throw ex;
        }
    }

    /**
     * 从 MDC 中获取请求追踪 ID
     *
     * @return requestId，若不存在则返回 "UNKNOWN"
     */
    private String getRequestId() {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        return requestId == null || requestId.isBlank() ? "UNKNOWN" : requestId;
    }

    /**
     * 从 Spring 上下文中获取当前 HttpServletRequest 对象
     *
     * @return 当前请求对象，非 Web 环境下返回 null
     */
    private HttpServletRequest getRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 构建完整请求 URL（URI + 查询字符串）
     *
     * @param request HTTP 请求对象
     * @return 完整 URL 字符串，request 为 null 时返回 "UNKNOWN"
     */
    private String buildUrl(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }

    /**
     * 构建方法参数映射（参数名 -> 参数值），过滤不可日志化的参数
     *
     * @param parameterNames 方法参数名数组
     * @param args           方法参数值数组
     * @return 可日志化的参数键值对（保持插入顺序）
     */
    private Map<String, Object> buildParamMap(String[] parameterNames, Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return params;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            // 跳过不可序列化的参数（如 Request、Response、文件等）
            if (!isLoggable(arg)) {
                continue;
            }
            // 若参数名不可用则使用 arg0、arg1 等占位
            String paramName = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            params.put(paramName, arg);
        }
        return params;
    }

    /**
     * 判断参数是否可日志化（排除 Servlet 对象、文件上传、绑定结果等）
     *
     * @param arg 方法参数
     * @return true 表示可以记录到日志中
     */
    private boolean isLoggable(Object arg) {
        return !(arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile
                || arg instanceof BindingResult);
    }

    /**
     * 将对象序列化为 JSON 字符串，并对敏感字段进行脱敏
     *
     * @param value 待序列化的对象
     * @return 脱敏后的 JSON 字符串；序列化失败时返回 toString 结果
     */
    private String toJson(Object value) {
        try {
            Object jsonValue = JSON.toJSON(value);
            // 递归脱敏敏感字段
            maskSensitiveValue(jsonValue);
            return JSON.toJSONString(jsonValue);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    /**
     * 递归遍历 JSON 结构，将敏感字段的值替换为 "******"
     *
     * @param value JSON 对象或数组
     */
    private void maskSensitiveValue(Object value) {
        if (value instanceof JSONObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                Object item = jsonObject.get(key);
                if (isSensitiveKey(key)) {
                    // 敏感字段直接替换为掩码
                    jsonObject.put(key, "******");
                } else {
                    // 非敏感字段继续递归检查嵌套结构
                    maskSensitiveValue(item);
                }
            }
            return;
        }
        if (value instanceof JSONArray jsonArray) {
            for (Object item : jsonArray) {
                maskSensitiveValue(item);
            }
        }
    }

    /**
     * 判断字段名是否为敏感字段（忽略大小写）
     *
     * @param key 字段名
     * @return true 表示是敏感字段，需要脱敏
     */
    private boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.stream().anyMatch(sensitiveKey -> sensitiveKey.equalsIgnoreCase(key));
    }
}
```

## 3.2、MDC + requestId

**为了解决高并发下日志不好区分的问题，已使用 MDC 给每个请求增加唯一 `requestId`。**

```java
/**
 * 请求ID过滤器：为每个HTTP请求生成或透传唯一的 requestId
 * 将 requestId 写入 MDC，供全链路日志追踪使用，并在响应头中回传给调用方
 * 优先级设为最高，确保后续所有Filter/Interceptor/Controller都能获取到 requestId
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** 请求/响应头中携带 requestId 的 Header 名称 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    /** MDC 中存储 requestId 的 key，日志格式中可通过 %X{requestId} 引用 */
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    /**
     * 核心过滤逻辑：
     * 1. 获取或生成 requestId
     * 2. 放入 MDC 供日志输出
     * 3. 设置到响应头供调用方追踪
     * 4. 请求结束后清理 MDC，防止线程池复用导致数据串扰
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 获取或生成唯一请求ID
        String requestId = getOrCreateRequestId(request);
        // 写入MDC，后续日志自动携带该requestId，MDC.put() 把 requestId 绑定到当前线程
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        // 在响应头中回传requestId，方便调用方进行链路追踪
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            // 继续执行过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 无论请求是否异常，都必须清理MDC，避免线程复用时数据污染
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }

    /**
     * 获取或创建 requestId：
     * 优先从请求头 X-Request-Id 中获取（支持上游服务透传），
     * 若不存在则生成格式为 "req-" + 16位随机字符 的唯一ID
     */
    private String getOrCreateRequestId(HttpServletRequest request) {
        // 尝试从请求头获取上游传递的 requestId
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        // 请求头中无 requestId，生成新的：req- + UUID前16位（去掉横杠）
        return "req-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
```

```
日志格式中包含：

​```xml
%X{requestId}
​```

这样同一个请求的日志可以通过同一个 `requestId` 分组查看。
```

## 3.3、日志输出到文件和滚动策略

**自定义Logback文件Appender：按随机大小滚动日志文件**
**当日志文件达到配置的最大大小时，自动关闭当前文件并创建新文件**
**新文件名格式：yyyy-MM-dd_随机6位数.log，避免文件名冲突**

**1、自定义文件 Appender：**

```java
/**
 * 自定义Logback文件Appender：按随机大小滚动日志文件
 * 当日志文件达到配置的最大大小时，自动关闭当前文件并创建新文件
 * 新文件名格式：yyyy-MM-dd_随机6位数.log，避免文件名冲突
 */
public class RandomSizeFileAppender extends AppenderBase<ILoggingEvent> {

    /** 日期格式化器，用于生成日志文件名中的日期部分 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 日志事件编码器，负责将日志事件转为字节数组 */
    private Encoder<ILoggingEvent> encoder;
    /** 日志文件存放目录，默认为 "logs" */
    private String logPath = "logs";
    /** 单个日志文件最大大小（字符串配置，如 "2MB"） */
    private String maxFileSize = "2MB";
    /** 单个日志文件最大大小（字节数，由 maxFileSize 解析而来） */
    private long maxFileSizeBytes = FileSize.valueOf("2MB").getSize();
    /** 当前日志文件的输出流 */
    private OutputStream outputStream;
    /** 当前日志文件已写入的字节数 */
    private long currentSize;

    /** 设置日志编码器（由logback配置文件注入） */
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    /** 设置日志文件存放路径（由logback配置文件注入） */
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    /** 设置单个日志文件最大大小，如 "2MB"、"500KB"（由logback配置文件注入） */
    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    /** Appender启动：解析文件大小配置并打开第一个日志文件 */
    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for RandomSizeFileAppender");
            return;
        }
        try {
            // 将字符串大小配置解析为字节数
            this.maxFileSizeBytes = FileSize.valueOf(maxFileSize).getSize();
            // 创建并打开第一个日志文件
            openNewFile();
            super.start();
        } catch (Exception ex) {
            addError("Failed to start RandomSizeFileAppender", ex);
        }
    }

    /** 核心写入逻辑：将日志事件编码后写入文件，超过大小限制时触发滚动 */
    @Override
    protected synchronized void append(ILoggingEvent eventObject) {
        if (!isStarted()) {
            return;
        }
        try {
            // 将日志事件编码为字节数组
            byte[] bytes = encoder.encode(eventObject);
            // 判断写入后是否超过文件大小限制，超过则滚动到新文件
            if (currentSize > 0 && currentSize + bytes.length > maxFileSizeBytes) {
                rollover();
            }
            // 写入日志数据并刷新缓冲区
            outputStream.write(bytes);
            outputStream.flush();
            // 累加当前文件已写入字节数
            currentSize += bytes.length;
        } catch (IOException ex) {
            addError("Failed to write log event", ex);
        }
    }

    /** Appender停止：关闭当前日志文件，释放资源 */
    @Override
    public synchronized void stop() {
        if (!isStarted()) {
            return;
        }
        closeCurrentFile();
        super.stop();
    }

    /** 日志滚动：关闭当前文件并打开新文件 */
    private void rollover() throws IOException {
        closeCurrentFile();
        openNewFile();
    }

    /** 打开新的日志文件：创建目录、生成唯一文件名、写入文件头 */
    private void openNewFile() throws IOException {
        // 确保日志目录存在
        Path directory = Path.of(logPath);
        Files.createDirectories(directory);
        // 生成不重复的文件路径
        Path file = createUniqueFilePath(directory);
        // 以 CREATE_NEW 模式打开，确保不会覆盖已有文件
        outputStream = new BufferedOutputStream(Files.newOutputStream(
                file,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        ));
        currentSize = 0;

        // 写入编码器定义的日志文件头（如有）
        byte[] header = encoder.headerBytes();
        if (header != null && header.length > 0) {
            outputStream.write(header);
            currentSize += header.length;
        }
    }

    /** 生成唯一文件路径：日期_随机6位数.log，最多尝试100次避免冲突 */
    private Path createUniqueFilePath(Path directory) throws IOException {
        for (int i = 0; i < 100; i++) {
            String date = LocalDate.now().format(DATE_FORMATTER);
            // 生成 100000~999999 的随机数作为文件名后缀
            int random = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
            Path file = directory.resolve(date + "_" + random + ".log");
            // 文件不存在则可用
            if (Files.notExists(file)) {
                return file;
            }
        }
        throw new IOException("Failed to create unique log file name");
    }

    /** 关闭当前日志文件：写入文件尾（如有），关闭流并重置状态 */
    private void closeCurrentFile() {
        if (outputStream == null) {
            return;
        }
        try {
            // 写入编码器定义的日志文件尾（如有）
            byte[] footer = encoder.footerBytes();
            if (footer != null && footer.length > 0) {
                outputStream.write(footer);
            }
            outputStream.close();
        } catch (IOException ex) {
            addError("Failed to close log file", ex);
        } finally {
            // 无论成功与否都重置流和大小计数
            outputStream = null;
            currentSize = 0;
        }
    }
}
```

**2、日志配置文件：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="yq-admin"/>
    <springProperty scope="context" name="LOG_PATH" source="app.logging.path" defaultValue="logs"/>

    <property name="LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId}] %-5level %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <appender name="FILE" class="com.itcjy.common.logging.RandomSizeFileAppender">
        <logPath>${LOG_PATH}</logPath>
        <maxFileSize>2MB</maxFileSize>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <logger name="com.itcjy" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

```
当前策略：
- 日志同时输出到控制台和文件
- 日志目录通过 `app.logging.path` 配置
- 单个日志文件超过 `2MB` 后生成新文件
- 文件名使用“日期_随机数”的形式

配置入口：
​```yaml
app:
  logging:
    path: ${APP_LOG_PATH:logs}
​```
```

# 四、RBAC 权限体系

```
当前 RBAC 使用 5 张表：

- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`
```

## 4.1、权限注解自动同步数据库

**使用方式：**

```java
@PutMapping("/update/{id}")
@HasPermission(code = "sys:user:update", name = "修改用户", description = "根据ID修改系统用户")
public ApiResponse<Void> updateSysUser(@Parameter(description = "用户ID", required = true)
                                       @PathVariable @NotNull(message = "用户ID不能为空") Long id,
                                       @Valid @RequestBody SysUserUpdateReq req) {
    sysUserService.updateUser(id, req);
    return ApiResponse.success("修改成功");
}
```

**1、自定义注解：**

在注解上添加当前的权限code，把注解标记在方法上表示这个方法要求什么权限。

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HasPermission {

    // 权限码
    String code();

    // 权限名称
    String name();

    // 权限描述
    String description() default "";
}
```

**2、启动同步接口权限到权限表**

当`SpringBoot`一启动就自动扫描controller类上如果标记了这个注解就保存到数据库里面

![image-20260721190707546](img/image-20260721190707546.png)

```java
package com.itcjy.emp.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.myEnum.ActiveEnum;
import com.itcjy.emp.mapper.SysPermissionMapper;
import com.itcjy.emp.pojo.entity.SysPermission;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限同步Runner
 * <p>
 * 应用启动后自动扫描所有标注了 {@link HasPermission} 注解的接口方法，
 * 将权限信息同步到 sys_permission 表：数据库中不存在的权限则新增，已存在的则更新。
 * 可通过配置 app.permission-sync.enabled=false 关闭该功能。
 * </p>
 */
@Slf4j
@Component
@Order(1)//就是排队拿号，数字越小越靠前执行。
@ConditionalOnProperty(prefix = "app.permission-sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PermissionSyncRunner implements ApplicationRunner {

    /**
     * Spring MVC 请求映射处理器，用于获取所有接口方法及其路由信息
     */
    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * 权限表 Mapper，用于权限数据的增删改查
     */
    @Resource
    private SysPermissionMapper sysPermissionMapper;

    /**
     * 应用启动完成后执行：遍历所有接口方法，逐个同步权限
     *
     * @param args 启动参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        requestMappingHandlerMapping.getHandlerMethods().forEach(this::syncPermission);
    }

    /**
     * 同步单个接口的权限信息
     * <p>未标注 {@link HasPermission} 注解的接口直接跳过；根据权限编码判断是新增还是更新。</p>
     *
     * @param mappingInfo   接口的路由映射信息
     * @param handlerMethod 接口的处理方法
     */
    private void syncPermission(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        // 获取方法上的权限注解，未标注则无需同步
        HasPermission hasPermission = handlerMethod.getMethodAnnotation(HasPermission.class);
        if (hasPermission == null) {
            return;
        }

        String permissionCode = hasPermission.code();
        String apiPath = resolveApiPath(mappingInfo);
        // 根据权限编码查询数据库中是否已存在该权限
        SysPermission dbPermission = sysPermissionMapper.selectOne(
                Wrappers.<SysPermission>lambdaQuery()
                        .eq(SysPermission::getPermissionCode, permissionCode)
        );

        // 不存在则新增，存在则更新
        if (dbPermission == null) {
            insertPermission(hasPermission, apiPath);
            return;
        }

        updatePermission(dbPermission, hasPermission, apiPath);
    }

    /**
     * 新增权限记录
     *
     * @param hasPermission 权限注解信息
     * @param apiPath       接口路径
     */
    private void insertPermission(HasPermission hasPermission, String apiPath) {
        LocalDateTime now = LocalDateTime.now();
        SysPermission permission = new SysPermission();
        permission.setPermissionCode(hasPermission.code());
        permission.setPermissionName(hasPermission.name());
        permission.setApiPath(apiPath);
        permission.setDescription(hasPermission.description());
        permission.setStatus(ActiveEnum.ACTIVE.name());
        permission.setCreatedAt(now);
        permission.setUpdatedAt(now);
        sysPermissionMapper.insert(permission);
        log.info("同步新增权限: code={}, path={}", hasPermission.code(), apiPath);
    }

    /**
     * 更新已存在的权限记录
     *
     * @param permission    数据库中已存在的权限实体
     * @param hasPermission 权限注解信息
     * @param apiPath       接口路径
     */
    private void updatePermission(SysPermission permission, HasPermission hasPermission, String apiPath) {
        permission.setPermissionName(hasPermission.name());
        permission.setApiPath(apiPath);
        permission.setDescription(hasPermission.description());
        permission.setUpdatedAt(LocalDateTime.now());
        sysPermissionMapper.updateById(permission);
        log.info("同步更新权限: code={}, path={}", hasPermission.code(), apiPath);
    }

    /**
     * 解析接口的访问路径
     * <p>一个接口可能配置了多个路径，按字典序排序后用逗号拼接。</p>
     *
     * @param mappingInfo 接口的路由映射信息
     * @return 接口路径字符串，无路径时返回空字符串
     */
    private String resolveApiPath(RequestMappingInfo mappingInfo) {
        Set<String> patternValues = mappingInfo.getPatternValues();
        if (CollectionUtils.isEmpty(patternValues)) {
            return "";
        }
        return patternValues.stream().sorted().collect(Collectors.joining(","));
    }
}
```

```
作用：

- 应用启动后扫描 Spring MVC 接口方法
- 找到标记了 `@HasPermission` 的接口
- 将权限编码、权限名称、接口路径、权限描述同步到 `sys_permission`
- 数据库不存在则新增，已存在则更新

配置：

​```yaml
app:
  permission-sync:
    enabled: true
​```
```

## 4.2、固定系统角色启动初始化

~~~yml
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
~~~

## 4.3、ADMIN 启动自动拥有全部权限

~~~yml
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
~~~

## 4.4、对各表进行维护

完成`用户角色分配接口`、`用户管理接口完成情况`等表



# 五、用户登录与鉴权功能实现

```
JwtAuthInterceptor
    ↓
SignInterceptor
    ↓
PermissionInterceptor
    ↓
Controller
```

## 5.1、登录功能

**主要流程：**

1. 根据用户名查询用户。
2. 校验用户是否存在。
3. 校验用户状态是否为 `ACTIVE`。
4. 校验密码。
5. 查询用户拥有的角色编码。
6. 查询用户拥有的权限编码。
7. 生成 JWT Token。
8. 生成 `signSecret`，用于后续请求签名。
9. 把 `LoginInfo` 保存到 Redis，key 为：

```
user_jwt_key_ + userId
```

​	10.返回登录结果：

```
LoginRes {
    token,
    signSecret,
    userDetailRes
}
```

**代码实现：**

```java
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
```

## 5.2、第一个拦截器：JwtAuthInterceptor

作用：**校验用户是否登录，并把当前登录用户信息放入 ThreadLocal。**

请求头要求：`Authorization: Bearer xxxxxx`

核心流程：

1. 从请求头获取 `Authorization`。
2. 判断是否存在，并且是否以 `Bearer ` 开头。
3. 截取 `JWT token`。
4. 使用 `JwtUtil` 校验是否可以解析成功token。
5. 从 `JWT claims` 中解析 `userId`。
6. 根据 `userId` 查询 Redis 登录态。
7. `Redis` 中存在 `LoginInfo` 才认为登录有效。
8. 把从Redis里面获取的`LoginInfo` 放入`AuthThreadlocal`：

```
AuthThreadlocal.setLoginInfo(loginInfo);
```

1. 请求结束后在 `afterCompletion` 中清理 ThreadLocal：

```
AuthThreadlocal.remove();
```

这样可以避免 Tomcat 线程复用导致用户信息串号。

**核心代码：**

```
com\itcjy\common\interceptor\JwtAuthInterceptor.java
```

## 5.3、第二个拦截器：SignInterceptor

```
com\itcjy\common\interceptor\SignInterceptor.java
```

作用：**校验请求签名，防止请求被篡改、过期请求或同一个请求重复提交。**

前端每次请求需要携带：

```
X-Timestamp: 时间戳
X-Nonce: 随机字符串
X-Sign: 签名
```

**核心流程：**

1. `OPTIONS` 预检请求直接放行。
2. 校验 `X-Timestamp`、`X-Nonce`、`X-Sign` 是否齐全。
3. 校验时间戳是否过期，目前有效期是 5 分钟：

```
SIGN_REQUEST_EXPIRE_DURATION = Duration.ofMinutes(5)
```

1. 使用 Redis 的 `setIfAbsent` 保存 nonce：

```
sign_nonce:{nonce}
```

同一个 nonce 在有效期内只能使用一次，防止重放请求。

1. 从 `AuthThreadlocal` 获取当前登录用户的 `signSecret`。
2. 后端重新构建签名原文：

```
method
uri
query
timestamp
nonce
```

1. 使用 `HMAC-SHA256` 计算服务端签名。
2. 比对服务端签名和前端传来的 `X-Sign`。
3. 不一致则抛出签名错误。

**核心流程：**

1.前端携带签名、时间戳和随机字符串到后端

2.后端先校验这个请求有没有过期，通过对时间戳+预设的过期时间与当前时间进行相比，如果小于就是没已经过期了

3.如果请求没有过期就要检验这个请求是否被篡改，先从登入用户的线程AuthThreadlocal里面获取密钥（一开始登录拦截器从redis保存 到AuthThreadlocal里面）然后重新生成签名与前端生成的签名再进行对比，合法再进行下一步

4.下一步就是防重放，前端传过来的随机字符串通过setnx保存到redis，这样子就可以防止同一个请求重复消费。

## **5.4、第三个拦截器：PermissionInterceptor**

```
com\itcjy\common\interceptor\PermissionInterceptor.java
```

作用：**让之前的 `@HasPermission` 真正参与接口鉴权。**

核心流程：

1. 判断当前请求是否是 Controller 方法。
2. 获取 Controller 方法上的：

```
@HasPermission
```

1. 如果接口没有标注 `@HasPermission`，直接放行。
2. 如果有标注，则从 `AuthThreadlocal` 获取当前登录用户信息。
3. 读取登录时保存的权限编码列表：

```
loginInfo.getPermissions()
```

1. 判断是否包含接口要求的权限编码：

```
hasPermission.code()
```

1. 不包含则抛出：

```
new BusinessException(403, "没有权限访问该接口")
```

这样原先只用于同步权限表的 `@HasPermission`，现在已经具备实际拦截能力。

**核心流程**：

通过对`controller`层上面标记了注权限解的通过获取出来然后再从`AuthThreadlocal`获取自己拥有权限再通过对比判断有没有权限。

## 5.5、拦截器注册配置

注册顺序是：

```
registry.addInterceptor(jwtAuthInterceptor)
registry.addInterceptor(signInterceptor)
registry.addInterceptor(permissionInterceptor)
```

也就是：

```
先认证登录态
再校验请求签名
最后校验接口权限
```

拦截范围：

```
/emp/**
```

排除接口：

```
/emp/sysUser/login
/v3/api-docs/**
/swagger-ui.html
/swagger-ui/**
```

# 六、课程详情 Excel 导入

Excel 导入解决的是“课程详情数量多时不适合一条一条手动录入”的问题。

线下课程通常会有几十天课程内容。如果全部让运营人员在页面上逐条新增，效率低，也容易录错。更合理的方式是先整理一份 Excel，然后由系统解析 Excel，校验每一行数据，最后批量写入课程详情表。

使用 EasyExcel 实现 Excel 解析。EasyExcel 是阿里开源的 Excel 读写工具，常用于 Java 项目中的 Excel 导入和导出。课堂上采用监听器方式读取 Excel，这样可以清楚看到“读取一行、校验一行、缓存一批、批量入库”的完整过程。

## 6.1、实现思路

编写一个`controller`接口接收课程的`Id`和`Excel`文件，然后在实现类里面调用一下一个`CourseDetailImportListener`监听器，这个监听器里面的`invoke()`可以读取Excel里面的数据，每读取大于50行（可以自己设置）就会调用`flush()`这个方法里面的`batchSaver.accept(new ArrayList<>(batch));`使用了`SysCourseDetailServiceImpl`里面mp的批量保存回调函数用于批量保存。

```
Excel 文件（150 行数据）
    │
    ▼
EasyExcel 逐行读取
    │
    ├── 第 1 行 ──▶ invoke() ──▶ batch.add(第1条)
    ├── 第 2 行 ──▶ invoke() ──▶ batch.add(第2条)
    ├── ...
    ├── 第 50 行 ──▶ invoke() ──▶ batch.add(第50条) ──▶ 满了！──▶ flush()
    │
    ├── 第 51 行 ──▶ invoke() ──▶ batch.add(第51条)
    ├── ...
    ├── 第 100 行 ──▶ invoke() ──▶ batch.add(第100条) ──▶ 又满了！──▶ flush()
    │
    ├── 第 101 行 ──▶ invoke() ──▶ batch.add(第101条)
    ├── ...
    ├── 第 150 行 ──▶ invoke() ──▶ batch.add(第150条)
    │
    ▼
读完了 ──▶ doAfterAllAnalysed() ──▶ flush()（最后 50 条）
```

```
┌──────────────────────────────┐
│   SysCourseDetailServiceImpl │  ← this 指向这里
│   ─────────────────────────  │
│                              │
│   importCourseDetail() {     │  ← 请求先到这里
│       // ...                 │
│       listener = new CourseDetailImportListener(
│           courseId,          │
│           this::saveBatch    │  ← "把我的 saveBatch 方法地址给监听器"
│       );                     │
│       // ...                 │
│   }                          │
│                              │
│   saveBatch(List list) {     │  ← 这个方法被"存"进了监听器
│       // MyBatis-Plus 批量插入 │
│   }                          │
│                              │
└──────────────────────────────┘
              │
              │ this::saveBatch 作为 Consumer 传入
              ▼
┌──────────────────────────────┐
│  CourseDetailImportListener  │
│  ─────────────────────────   │
│                              │
│   batchSaver = this::saveBatch│ ← 存着"回调地址"
│                              │
│   flush() {                  │
│       batchSaver.accept(     │  ← 攒够 50 条时调用
│           new ArrayList<>(batch)  ← 创建 list 传入
│       );                     │
│   }                          │
│                              │
└──────────────────────────────┘
```

## **6.2、代码实现**

**1、编写课程详情 Excel 导入数据模型实体类**

```java
/**
 * 课程详情 Excel 导入数据模型
 * Excel 列顺序：阶段 | 第几天 | 上课内容
 */
@Data
public class CourseDetailExcelData {

    @ExcelProperty(index = 0)
    private String stageName;

    @ExcelProperty(index = 1)
    private Integer dayNumber;

    @ExcelProperty(index = 2)
    private String classContent;
}
```

**2、编写实现类**

```java
@Override
public int importCourseDetail(Long courseId, MultipartFile file) {
    // 校验课程是否存在
    SysCourse course = sysCourseService.getById(courseId);
    if (course == null) {
        throw BusinessException.COURSE_NOT_EXIST.newInstance("课程不存在");
    }
    // 流式读取 + 分批入库，避免大文件 OOM
    CourseDetailImportListener listener = new CourseDetailImportListener(courseId, this::saveBatch);
    try {
        EasyExcel.read(file.getInputStream(), CourseDetailExcelData.class, listener)
                .sheet()
                .doRead();
    } catch (IOException e) {
        throw BusinessException.PARAMS_ERROR.newInstance("读取 Excel 文件失败");
    }
    if (listener.getTotalCount() == 0) {
        throw BusinessException.PARAMS_ERROR.newInstance("Excel 文件内容为空");
    }
    return listener.getTotalCount();
}
```

**3、编写监听器**

```java
/**
 * 课程详情 Excel 导入流式监听器
 * <p>
 * 每积累 BATCH_SIZE 条记录执行一次批量入库，内存占用恒定，防止大文件 OOM。
 */
public class CourseDetailImportListener implements ReadListener<CourseDetailExcelData> {

    private static final int BATCH_SIZE = 50;

    private final Long courseId;
    private final Consumer<List<SysCourseDetail>> batchSaver;
    private final List<SysCourseDetail> batch = new ArrayList<>(BATCH_SIZE);
    private final AtomicInteger totalCount = new AtomicInteger(0);

    /**
     * @param courseId   所属课程ID
     * @param batchSaver 批量保存回调（通常传入 service::saveBatch）
     */
    public CourseDetailImportListener(Long courseId, Consumer<List<SysCourseDetail>> batchSaver) {
        this.courseId = courseId;
        this.batchSaver = batchSaver;
    }

    @Override
    public void invoke(CourseDetailExcelData data, AnalysisContext context) {
        SysCourseDetail detail = new SysCourseDetail();
        detail.setCourseId(courseId);
        detail.setStageName(data.getStageName());
        detail.setDayNumber(data.getDayNumber());
        detail.setClassContent(data.getClassContent());
        batch.add(detail);
        if (batch.size() >= BATCH_SIZE) {
            flush();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        flush();
    }

    private void flush() {
        if (!batch.isEmpty()) {
            batchSaver.accept(new ArrayList<>(batch));
            totalCount.addAndGet(batch.size());
            batch.clear();
        }
    }

    /**
     * 获取本次导入的总记录数
     */
    public int getTotalCount() {
        return totalCount.get();
    }
}
```

# 七、课程表生成（根据课程来生成）

争对不同的班级选择开课时间就可根据这个班级选择的课程进行生成具体的课程表。

## 7.1、业务难点

课程安排的时间要避免节假日和固定自习日（周四）、开课第一天要判断是不是class，如果是class就不影响进行创建。

## 7.2、业务流程

选择日期进行生成课程表，首页要判断当前日期是不是节假日等，如果是就不允许创建，如果是就通过循环遍历提前创建好的是否是可以上课的规则对每天的日期进行校验，校验完可以添加才添加进去，否则就空着直到下一个class日。

并且准备好了ClassSchedualConstants（日期合理校验规则）、HolidayInfo（日期信息，用于接收是否是节假日的时间）、HolidayUtil（发送一个请求判断是不是节假日）

## 7.3、代码实现

**1.请求体（ai写的可能有屎山）**

```java
@Schema(description = "生成班级课程表请求参数")
public record SysClassScheduleGenerateReq(
        @Schema(description = "班级ID", example = "1")
        @NotNull(message = "班级ID不能为空")
        @Positive(message = "班级ID必须大于0")
        Long classId,

        @Schema(description = "第一天上课日期", example = "2026-07-27")
        @NotNull(message = "开始日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate
) {
}
```

**2.校验规则（ai写的可能有屎山）**

```
public class ClassScheduleConstants {

    private ClassScheduleConstants() {
    }

    /**
     * 默认排课规则：周一、二、三、五、六上课，周四自习，周日休息，法定节假日休息。
     */
    public static final String RULE_CONFIG = """
            {
              "classDays": [1, 2, 3, 5, 6],
              "selfStudyDays": [4],
              "restDays": [7],
              "holidayRest": true
            }
            """;

    /**
     * 课表中每天可能的类型
     */
    public static final class DayType {
        private DayType() {
        }

        /** 正常上课，绑定课程详情 */
        public static final String CLASS = "CLASS";
        /** 自习日，不消耗课程详情 */
        public static final String SELF_STUDY = "SELF_STUDY";
        /** 固定休息日，不消耗课程详情 */
        public static final String REST = "REST";
        /** 法定节假日，不消耗课程详情 */
        public static final String HOLIDAY = "HOLIDAY";
    }

    /**
     * 课表规则配置 JSON 字段名
     */
    public static final class RuleField {
        private RuleField() {
        }

        /** 上课日（星期几集合，1=周一 ... 7=周日） */
        public static final String CLASS_DAYS = "classDays";
        /** 自习日（星期几集合） */
        public static final String SELF_STUDY_DAYS = "selfStudyDays";
        /** 固定休息日（星期几集合） */
        public static final String REST_DAYS = "restDays";
        /** 法定节假日是否自动休息 */
        public static final String HOLIDAY_REST = "holidayRest";
    }
}
```

**3.接收节假日信息类**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayInfo {

    private Boolean holiday;

    private String name;
}
```

**4.校验工具，可以向一个专门的获得节假日的api发送，保存到节假日信息类**

```java
@Component
public class HolidayUtil {

    private static final String HOLIDAY_YEAR_URL = "https://timor.tech/api/holiday/year/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final Cache<Integer, Map<String, HolidayInfo>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(20)
            .build();

    public HolidayInfo getHolidayInfo(LocalDate date) {
        int year = date.getYear();
        try {
            Map<String, HolidayInfo> yearHolidayMap = cache.get(year, () -> loadYearHoliday(year));
            return yearHolidayMap.get(date.toString());
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }
            throw BusinessException.REMOTE_ERROR.newInstance("节假日信息获取失败");
        }
    }

    private Map<String, HolidayInfo> loadYearHoliday(int year) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HOLIDAY_YEAR_URL + year))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw BusinessException.REMOTE_ERROR.newInstance(
                        "节假日API响应异常，HTTP " + response.statusCode());
            }
            JSONObject body = JSON.parseObject(response.body());
            if (body == null || body.getIntValue("code") != 0) {
                throw BusinessException.REMOTE_ERROR.newInstance("节假日信息获取失败");
            }

            JSONObject holiday = body.getJSONObject("holiday");
            Map<String, HolidayInfo> result = new HashMap<>();
            if (holiday == null) {
                return result;
            }

            for (String key : holiday.keySet()) {
                JSONObject item = holiday.getJSONObject(key);
                if (item == null) {
                    continue;
                }
                String dateText = item.getString("date");
                String date = dateText == null ? year + "-" + key : dateText;
                result.put(date, new HolidayInfo(item.getBoolean("holiday"), item.getString("name")));
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.REMOTE_ERROR.newInstance("节假日信息获取失败");
        }
    }

}
```

5.相关核心代码

对从用户选择的每一天都进行校验，循环遍历校验规则，是class就加入，否则就跳过，要严格按照`课程这个实体表`来生成每一天。

# 八、为班级的不同阶段分配教师（按阶段来）

## 8.1、业务难点

**一个老师不能同时出现在两个地方**

## 8.2、解决思路

前面生成的课程表还没开始分配讲师，所以在现在要分配讲师，分配讲师我们首先应该要通过查找选择的这一阶段的这个课程id去查询这些课程的上课时间，然后通过上课时间去查询当前有哪些教师是被安排了课程的，对安排了课程的教师不能进行分配选择，最后再批量更新教师。

# 九、新增临时课程

## 9.1、业务难点

核心在于**"插入一个点，牵动一条线"**——加一节课不只是改一天，而是可能引发后续一连串的连锁反应。

## 9.2、解决思路

我想着可以在前端接收课程id、教师id、加课日期和班级id，后端接收到处理一下参数校验嘛，然后判断加课那天的日期是不是class，如果不是class就直接加，如果是class就要获取那天后面所有的课程，然后重新排列，当然前面的分配教师也要判断那天有没有上课，上次对话我不是说明了在分配教师的时候进行校验不能重复授课



# 十、在数据库进行动态配置

动态配置用于把系统运行过程中可能变化的参数放到数据库中维护，而不是全部写死在配置文件里。

配置文件适合保存固定的基础环境信息，例如数据库地址、Redis 地址、 访问密钥。

动态配置适合保存运营过程中可能调整的参数，排课规则、订单过期时间。

| 配置方式       | 适合内容                       | 修改后是否需要重新发版 |
| :------------- | :----------------------------- | :--------------------- |
| 配置文件       | 环境地址、服务端口             | 通常需要               |
| 环境变量       | 不同环境的敏感配置 密钥        | 通常需要重启           |
| 数据库动态配置 | 业务规则、过期时间、开关类配置 | 不需要发版             |

## 10.1、使用方法：

创建SysConfigType 表，表示有哪些配置（相当于大key）

```java
@Data
public class SysConfigType {

    private Long id;
    private String typeName;
    private String typeCode;//业务中的唯一标识
    private String desc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
```

创建SysConfigItem ，表示某个配置的具体规则

```Java
@Data
public class SysConfigItem {

    private Long id;
    private Long typeId;//关联配置类型
    private String key;
    private String value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
```

![image-20260724142648453](img/image-20260724142648453.png)



# 十一、通过阿里云OSS预签名上传文件

## 11.1、完整时序图

![](img/20260724084904-104a4de4-351dd399.jpg)

| 参数         | 含义     | 示例                                                         |
| ------------ | -------- | ------------------------------------------------------------ |
| `dir`        | 业务目录 | `homework_standard`（作业标准）、`homework/content`（作业内容）、`homework/submission`（学生提交） |
| `fileSuffix` | 文件后缀 | `md`、`png`                                                  |

![](img/下载.jpg)

# 十二、订单管理

本模块涉及多张表，包括预定单表、订单表和学生表等。

**核心流程：**

销售人员与客户沟通好后确认意向后先生成预定单，预定单绑定课程相关，生成预定单后就在stu表里面插入一个学生数据，状态要设置为临时学员，然后学生通过这个账号去学生端进行登录，登录后去订单模块进行支付，此时就涉及到支付相关功能，支付功能本项目就是仅对接了支付宝支付。

支付流程：支付宝支付采用了RSA2的加密算法，在生成一对公私钥后互换公钥进行验签，在用户点击支付后发送请求到服务端，服务端把前端带来的订单号、金额等发送给支付宝端，支付宝生成支付流水号后发送html文件给前端进行登录支付，支付成功后发送回调消息给后端预设的URL，服务端收到支付成功的信息就更新学生状态为在校，并且把订单表的状态也更新为已支付。

支付完成后就会跳转到预设的跳转地址到用户端首页。

# 十三、考试管理

## 13.1.题库管理

题库管理设涉及

`exam_question`题库题目表

![image-20260729105600334](img/image-20260729105600334.png)

`exam_question_option`题目选项表

![image-20260729105720804](img/image-20260729105720804.png)

`exam_question_course`题目关联课程表

![image-20260729105842614](img/image-20260729105842614.png)

`exam_question`题库题目表保存题目的题干和正确选项难易程度和相关状态通过关联`exam_question_option`题目选项表和`exam_question_course`题目关联课程表完成题库相关模块的crud。

## 13.2、试卷管理

本模块涉及`exam_paper`试卷表

![image-20260729110610983](img/image-20260729110610983.png)

`exam_paper_question`试卷题目关联表

![image-20260729110718094](img/image-20260729110718094.png)

在生成试卷后通过`exam_paper_question`试卷题目关联表绑定`exam_question`题库题目表生成完整的试卷

## 13.3.考试管理

本模块涉及4张表

`exam`考试安排表（通过此表关联`exam_paper`试卷表）

![image-20260729111045609](img/image-20260729111045609.png)

和学生端的`student_exam_record`学生考试记录表

![image-20260729111619615](img/image-20260729111619615.png)

和`student_exam_answer`学生考试答案表

![image-20260729111958853](img/image-20260729111958853.png)

## 后端接口与业务实现

遵循现有 Controller → Service → Mapper 分层，继续使用项目已有的 `ApiResponse`、`PageResult`、Long ID、MyBatis-Plus分页、`@HasPermission` 和学生 JWT 身份。

### 题库管理

- `GET /emp/exam/questions`：组合分页查询，支持题干关键字、课程、阶段、题型、难度和状态。
- `POST /emp/exam/questions`：新增题目、选项和阶段关联。
- `GET /emp/exam/questions/{id}`：完整详情。
- `PUT /emp/exam/questions/{id}`：事务内替换基本信息、选项和阶段关联。
- `PATCH /emp/exam/questions/{id}/status`：启用或停用。

分页列表展示：ID、题干摘要、题型、课程/阶段标签、难度、状态、选项数量、更新时间。展开行或详情抽屉展示完整题干、选项、标准答案和解析。

组合查询避免一对多连接导致分页重复：先分页得到题目ID，再批量加载课程阶段和选项，禁止逐行查询产生 N+1。

### 试卷管理

- `GET /emp/exam/papers`：分页查询试卷名称、课程、阶段、状态等。
- `POST /emp/exam/papers`：一次事务创建试卷、阶段、题目快照和选项快照。
- `GET /emp/exam/papers/{id}`：试卷详情和完整题目。
- `PUT /emp/exam/papers/{id}`：仅允许修改草稿试卷。
- 候选题复用题库分页接口，只返回 `ENABLED` 且属于所选课程阶段的题目。

首版采用手动组卷：按阶段、难度、题型筛选，查看题目详情后加入试卷，设置每题分值和顺序；总分由后端求和，不接收前端自行计算结果。

### 考试发布与管理

- `POST /emp/exams`：提交 `paperId、classIds、startTime、entryDeadlineTime、durationMinutes、invigilatorUserId`，批量发布。
- `GET /emp/exams`：分页展示试卷、班级、开放时间、最晚入场、关闭时间以及各状态人数。
- `GET /emp/exams/{id}`：考试详情。
- `GET /emp/exams/{id}/records`：分页查询考生状态和批改状态。
- `GET /emp/exam-records/{recordId}/grading`：仅返回填空、简答题及其学生答案。
- `PUT /emp/exam-records/{recordId}/grading`：批量提交主观题得分和评语，校验每题得分不超过题目分值，重新计算总分。
- `PATCH /emp/exams/{id}/answer-visibility`：仅在 `close_time` 之后允许开关成绩和答卷详情。

客观题在主动或超时提交时立即判分；含主观题的记录进入 `PENDING`，全部人工评分后变为 `COMPLETED`。纯客观题提交后直接完成批改。

### 学生答题

- `GET /stu/exams`：查询当前学生发布快照中的考试，不根据后续转班情况改变范围。
- `POST /stu/exams/{examId}/start`：幂等开始考试，原子地把 `NOT_STARTED` 改为 `IN_PROGRESS`，返回试卷、服务器时间和个人截止时间。
- `GET /stu/exam-records/{recordId}`：刷新或断线后恢复题目及已保存答案。
- `PUT /stu/exam-records/{recordId}/answers/{paperQuestionId}`：逐题防抖自动保存。
- `POST /stu/exam-records/{recordId}/submit`：主动封卷和判分。
- `GET /stu/exam-records/{recordId}/result`：仅当考试已关闭、公布开关开启且当前记录批改完成时返回标准答案、解析、逐题得分和教师评语。

所有学生接口只使用 JWT 中的当前学生ID，不接受前端传入 `studentId`；开始答题前绝不返回标准答案和解析。

### 自动交卷

- 学生开始考试时写入超时 Outbox，并投递包含 `recordId、deadlineTime` 的 RocketMQ 延时消息。
- 当前 RocketMQ 4.x 固定延时档位采用分段延时：消息提前到达时重新计算剩余时间并再次延迟投递。
- 消费者只在记录仍为 `IN_PROGRESS` 且已经到期时执行自动提交；重复消息直接幂等返回。
- 增加数据库定时补偿任务，扫描已到期的 `IN_PROGRESS` 记录以及发送失败的 Outbox。
- 主动提交和自动提交使用条件更新或乐观锁竞争，保证只能有一个流程成功封卷。
