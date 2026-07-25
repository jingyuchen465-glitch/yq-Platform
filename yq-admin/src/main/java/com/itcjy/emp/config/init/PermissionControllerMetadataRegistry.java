package com.itcjy.emp.config.init;

import com.itcjy.common.annotations.HasPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从 Controller 注解中建立权限元数据目录。
 * <p>
 * {@link HasPermission} 提供叶子权限信息，Controller 上的 {@link Tag} 提供分组名称与说明。
 * 使用 ObjectProvider 延迟获取 HandlerMapping，避免 Controller 创建阶段产生循环依赖。
 * </p>
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class PermissionControllerMetadataRegistry implements ApplicationRunner {

    private final ObjectProvider<RequestMappingHandlerMapping> requestMappingHandlerMappingProvider;

    private volatile Map<String, PermissionControllerMetadata> metadataByCode = Map.of();

    @Override
    public void run(ApplicationArguments args) {
        RequestMappingHandlerMapping handlerMapping = requestMappingHandlerMappingProvider.getObject();
        Map<String, PermissionControllerMetadata> discoveredMetadata = new LinkedHashMap<>();
        handlerMapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) ->
                register(discoveredMetadata, mappingInfo, handlerMethod));
        metadataByCode = Map.copyOf(discoveredMetadata);
        log.info("Controller 权限目录已建立: permissions={}, controllers={}",
                metadataByCode.size(),
                metadataByCode.values().stream().map(PermissionControllerMetadata::controllerName).distinct().count());
    }

    public List<PermissionControllerMetadata> listAll() {
        return List.copyOf(metadataByCode.values());
    }

    public Optional<PermissionControllerMetadata> findByPermissionCode(String permissionCode) {
        return Optional.ofNullable(metadataByCode.get(permissionCode));
    }

    private void register(Map<String, PermissionControllerMetadata> discoveredMetadata,
                          RequestMappingInfo mappingInfo,
                          HandlerMethod handlerMethod) {
        HasPermission permission = handlerMethod.getMethodAnnotation(HasPermission.class);
        if (permission == null) {
            return;
        }

        Class<?> controllerType = handlerMethod.getBeanType();
        Tag tag = AnnotatedElementUtils.findMergedAnnotation(controllerType, Tag.class);
        String controllerName = controllerType.getSimpleName();
        String groupName = tag != null && StringUtils.hasText(tag.name()) ? tag.name() : controllerName;
        String groupDescription = tag != null ? tag.description() : "";

        PermissionControllerMetadata metadata = new PermissionControllerMetadata(
                permission.code(),
                permission.name(),
                permission.description(),
                resolveApiPath(mappingInfo),
                controllerName,
                groupName,
                groupDescription
        );
        PermissionControllerMetadata previous = discoveredMetadata.putIfAbsent(permission.code(), metadata);
        if (previous != null) {
            log.warn("发现重复权限编码，保留首次扫描结果: code={}, controller={}",
                    permission.code(), previous.controllerName());
        }
    }

    private String resolveApiPath(RequestMappingInfo mappingInfo) {
        Set<String> patternValues = mappingInfo.getPatternValues();
        if (CollectionUtils.isEmpty(patternValues)) {
            return "";
        }
        return patternValues.stream().sorted().collect(Collectors.joining(","));
    }

    public record PermissionControllerMetadata(
            String permissionCode,
            String permissionName,
            String permissionDescription,
            String apiPath,
            String controllerName,
            String groupName,
            String groupDescription
    ) {
    }
}
