package io.github.nwen.freecallcenterapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nwen.freecallcenterapi.dto.DialRequest;
import io.github.nwen.freecallcenterapi.dto.ExtensionRequest;
import io.github.nwen.freecallcenterapi.entity.Extension;
import io.github.nwen.freecallcenterapi.repository.ExtensionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExtensionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExtensionRepository extensionRepository;

    @BeforeEach
    void setUp() {
        extensionRepository.delete(new LambdaQueryWrapper<>());
    }

    @Test
    void create_Success() throws Exception {
        ExtensionRequest request = ExtensionRequest.builder()
                .extensionNumber("1001")
                .password("password123")
                .displayName("Test Extension")
                .context("default")
                .build();

        MvcResult result = mockMvc.perform(post("/extensions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.extensionNumber").value("1001"))
                .andExpect(jsonPath("$.data.displayName").value("Test Extension"))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("\"id\":"));

        Extension saved = extensionRepository.selectOne(
                new LambdaQueryWrapper<Extension>().eq(Extension::getExtensionNumber, "1001")
        );
        assertNotNull(saved);
        assertEquals("1001", saved.getExtensionNumber());
        assertEquals("password123", saved.getPassword());
    }

    @Test
    void create_ValidationError_EmptyExtensionNumber() throws Exception {
        ExtensionRequest request = ExtensionRequest.builder()
                .extensionNumber("")
                .password("password123")
                .displayName("Test Extension")
                .context("default")
                .build();

        mockMvc.perform(post("/extensions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_ValidationError_InvalidExtensionNumber() throws Exception {
        ExtensionRequest request = ExtensionRequest.builder()
                .extensionNumber("12") // Less than 3 digits
                .password("password123")
                .displayName("Test Extension")
                .context("default")
                .build();

        mockMvc.perform(post("/extensions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_ValidationError_ShortPassword() throws Exception {
        ExtensionRequest request = ExtensionRequest.builder()
                .extensionNumber("1001")
                .password("123") // Less than 4 characters
                .displayName("Test Extension")
                .context("default")
                .build();

        mockMvc.perform(post("/extensions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_DuplicateExtensionNumber() throws Exception {
        Extension existing = Extension.builder()
                .extensionNumber("1001")
                .password("password123")
                .displayName("Existing Extension")
                .enabled(false)
                .context("default")
                .build();
        extensionRepository.insert(existing);

        ExtensionRequest request = ExtensionRequest.builder()
                .extensionNumber("1001")
                .password("newpassword")
                .displayName("New Extension")
                .context("default")
                .build();

        mockMvc.perform(post("/extensions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("分机号 1001 已存在"));
    }

    @Test
    void list_Success() throws Exception {
        for (int i = 0; i < 3; i++) {
            Extension extension = Extension.builder()
                    .extensionNumber("200" + i)
                    .password("password" + i)
                    .displayName("Extension " + i)
                    .enabled(i % 2 == 0)
                    .context("default")
                    .build();
            extensionRepository.insert(extension);
        }

        mockMvc.perform(get("/extensions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void list_Empty() throws Exception {
        mockMvc.perform(get("/extensions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getById_Found() throws Exception {
        Extension extension = Extension.builder()
                .extensionNumber("1001")
                .password("password123")
                .displayName("Test Extension")
                .enabled(true)
                .context("default")
                .build();
        extensionRepository.insert(extension);

        mockMvc.perform(get("/extensions/" + extension.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(extension.getId()))
                .andExpect(jsonPath("$.data.extensionNumber").value("1001"));
    }

    @Test
    void getById_NotFound() throws Exception {
        mockMvc.perform(get("/extensions/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("分机不存在"));
    }

    @Test
    void update_Success() throws Exception {
        Extension extension = Extension.builder()
                .extensionNumber("1001")
                .password("oldPassword")
                .displayName("Old Name")
                .enabled(false)
                .context("default")
                .build();
        extensionRepository.insert(extension);

        ExtensionRequest request = ExtensionRequest.builder()
                .extensionNumber("1001")
                .password("newPassword")
                .displayName("Updated Name")
                .context("default")
                .build();

        mockMvc.perform(put("/extensions/" + extension.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayName").value("Updated Name"));

        Extension updated = extensionRepository.selectById(extension.getId());
        assertNotNull(updated);
        assertEquals("newPassword", updated.getPassword());
        assertEquals("Updated Name", updated.getDisplayName());
    }

    @Test
    void update_NotFound() throws Exception {
        ExtensionRequest request = ExtensionRequest.builder()
                .extensionNumber("9999")
                .password("password")
                .displayName("Test")
                .context("default")
                .build();

        mockMvc.perform(put("/extensions/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void delete_Success() throws Exception {
        Extension extension = Extension.builder()
                .extensionNumber("1001")
                .password("password123")
                .displayName("To Be Deleted")
                .enabled(false)
                .context("default")
                .build();
        extensionRepository.insert(extension);

        mockMvc.perform(delete("/extensions/" + extension.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Extension deleted = extensionRepository.selectById(extension.getId());
        assertNull(deleted);
    }

    @Test
    void delete_NotFound() throws Exception {
        mockMvc.perform(delete("/extensions/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void dial_Disabled() throws Exception {
        Extension extension = Extension.builder()
                .extensionNumber("1001")
                .password("password123")
                .displayName("Test Extension")
                .enabled(false)
                .context("default")
                .build();
        extensionRepository.insert(extension);

        DialRequest request = DialRequest.builder()
                .destination("1002")
                .build();

        mockMvc.perform(post("/extensions/" + extension.getId() + "/dial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
