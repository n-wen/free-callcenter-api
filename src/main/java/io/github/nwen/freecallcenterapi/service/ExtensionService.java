package io.github.nwen.freecallcenterapi.service;

import io.github.nwen.freecallcenterapi.dto.DialRequest;
import io.github.nwen.freecallcenterapi.dto.ExtensionRequest;
import io.github.nwen.freecallcenterapi.dto.ExtensionResponse;
import io.github.nwen.freecallcenterapi.entity.Extension;
import io.github.nwen.freecallcenterapi.repository.ExtensionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtensionService {

    private final ExtensionRepository extensionRepository;
    private final EslService eslService;

    public ExtensionResponse create(ExtensionRequest request) {
        if (extensionRepository.findByExtensionNumber(request.getExtensionNumber()).isPresent()) {
            throw new IllegalArgumentException("分机号 " + request.getExtensionNumber() + " 已存在");
        }

        Extension extension = Extension.builder()
                .extensionNumber(request.getExtensionNumber())
                .password(request.getPassword())
                .displayName(request.getDisplayName())
                .context(request.getContext())
                .enabled(true)
                .build();

        extensionRepository.insert(extension);
        log.info("创建分机成功: {}", extension.getExtensionNumber());
        return toResponse(extension);
    }

    public List<ExtensionResponse> findAll() {
        return extensionRepository.selectList(null).stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<ExtensionResponse> findById(Long id) {
        return Optional.ofNullable(extensionRepository.selectById(id)).map(this::toResponse);
    }

    public Optional<ExtensionResponse> update(Long id, ExtensionRequest request) {
        return Optional.ofNullable(extensionRepository.selectById(id)).map(extension -> {
            extension.setPassword(request.getPassword());
            extension.setDisplayName(request.getDisplayName());
            extension.setContext(request.getContext());
            extensionRepository.updateById(extension);
            log.info("更新分机成功: {}", extension.getExtensionNumber());
            return toResponse(extension);
        });
    }

    public boolean delete(Long id) {
        if (extensionRepository.selectById(id) == null) {
            return false;
        }
        extensionRepository.deleteById(id);
        log.info("删除分机成功, id: {}", id);
        return true;
    }

    public boolean setEnabled(Long id, boolean enabled) {
        Extension extension = extensionRepository.selectById(id);
        if (extension == null) {
            return false;
        }
        extension.setEnabled(enabled);
        extensionRepository.updateById(extension);
        log.info("分机 {} 状态: {}", extension.getExtensionNumber(), enabled ? "启用" : "禁用");
        return true;
    }

    public boolean dial(DialRequest request) {
        String source = request.getSource();
        Extension extension = extensionRepository.findByExtensionNumber(source)
                .orElseThrow(() -> new IllegalArgumentException("分机不存在: " + source));

        if (!Boolean.TRUE.equals(extension.getEnabled())) {
            throw new IllegalStateException("分机 " + extension.getExtensionNumber() + " 当前已被禁用，无法呼出");
        }

        String destination = request.getDestination();
        String callerIdNumber = request.getCallerIdNumber() != null
                ? request.getCallerIdNumber()
                : extension.getExtensionNumber();
        String callerIdName = request.getCallerIdName() != null
                ? request.getCallerIdName()
                : extension.getDisplayName();

        String command = String.format(
                "originate {origination_caller_id_number=%s,origination_caller_id_name=%s}user/%s %s",
                callerIdNumber,
                callerIdName,
                destination,
                extension.getExtensionNumber()
        );

        try {
            String result = eslService.sendCommand(command);
            log.info("分机 {} 发起外呼到 {}, 结果: {}", extension.getExtensionNumber(), destination, result);
            return true;
        } catch (Exception e) {
            log.error("分机 {} 呼出失败: {}", extension.getExtensionNumber(), e.getMessage());
            throw new RuntimeException("呼出失败: " + e.getMessage());
        }
    }

    private ExtensionResponse toResponse(Extension extension) {
        return ExtensionResponse.builder()
                .id(extension.getId())
                .extensionNumber(extension.getExtensionNumber())
                .displayName(extension.getDisplayName())
                .enabled(extension.getEnabled())
                .context(extension.getContext())
                .createdAt(extension.getCreatedAt())
                .updatedAt(extension.getUpdatedAt())
                .build();
    }
}
