package io.github.nwen.freecallcenterapi.controller;

import io.github.nwen.freecallcenterapi.common.Result;
import io.github.nwen.freecallcenterapi.dto.DialRequest;
import io.github.nwen.freecallcenterapi.dto.ExtensionRequest;
import io.github.nwen.freecallcenterapi.dto.ExtensionResponse;
import io.github.nwen.freecallcenterapi.service.ExtensionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/extensions")
public class ExtensionController {

    private final ExtensionService extensionService;

    @PostMapping
    public Result<ExtensionResponse> create(@Valid @RequestBody ExtensionRequest request) {
        log.info("创建分机请求: {}", request.getExtensionNumber());
        return Result.success(extensionService.create(request));
    }

    @GetMapping
    public Result<List<ExtensionResponse>> list() {
        return Result.success(extensionService.findAll());
    }

    @GetMapping("/{id}")
    public Result<ExtensionResponse> getById(@PathVariable Long id) {
        return extensionService.findById(id)
                .map(Result::success)
                .orElse(Result.error(404, "分机不存在"));
    }

    @PutMapping("/{id}")
    public Result<ExtensionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ExtensionRequest request
    ) {
        return extensionService.update(id, request)
                .map(Result::success)
                .orElse(Result.error(404, "分机不存在"));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (extensionService.delete(id)) {
            return Result.success();
        }
        return Result.error(404, "分机不存在");
    }

    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        if (extensionService.setEnabled(id, true)) {
            return Result.success();
        }
        return Result.error(404, "分机不存在");
    }

    @PutMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        if (extensionService.setEnabled(id, false)) {
            return Result.success();
        }
        return Result.error(404, "分机不存在");
    }

    @PostMapping("/dial")
    public Result<Void> dial(@Valid @RequestBody DialRequest request) {
        try {
            extensionService.dial(request);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("呼出失败", e);
            return Result.error(500, "呼出失败: " + e.getMessage());
        }
    }
}
