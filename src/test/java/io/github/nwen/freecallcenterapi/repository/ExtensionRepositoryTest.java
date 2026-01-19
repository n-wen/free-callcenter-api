package io.github.nwen.freecallcenterapi.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.nwen.freecallcenterapi.entity.Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ExtensionRepositoryTest {

    @Autowired
    private ExtensionRepository extensionRepository;

    @BeforeEach
    void setUp() {
        extensionRepository.delete(new LambdaQueryWrapper<>());
    }

    @Test
    void testInsertAndFindById() {
        Extension extension = Extension.builder()
                .extensionNumber("1001")
                .password("password123")
                .displayName("Test Extension")
                .enabled(false)
                .context("default")
                .build();

        extensionRepository.insert(extension);

        Extension found = extensionRepository.selectById(extension.getId());
        assertNotNull(found);
        assertEquals("1001", found.getExtensionNumber());
        assertEquals("password123", found.getPassword());
    }

    @Test
    void testFindByExtensionNumber() {
        Extension extension = Extension.builder()
                .extensionNumber("1002")
                .password("password456")
                .displayName("Another Extension")
                .enabled(true)
                .context("default")
                .build();

        extensionRepository.insert(extension);

        Extension found = extensionRepository.selectOne(
                new LambdaQueryWrapper<Extension>()
                        .eq(Extension::getExtensionNumber, "1002")
        );

        assertNotNull(found);
        assertEquals("1002", found.getExtensionNumber());
        assertTrue(found.getEnabled());
    }

    @Test
    void testUpdate() {
        Extension extension = Extension.builder()
                .extensionNumber("1003")
                .password("oldPassword")
                .displayName("Old Name")
                .enabled(false)
                .context("default")
                .build();

        extensionRepository.insert(extension);

        extension.setPassword("newPassword");
        extension.setDisplayName("New Name");
        extension.setEnabled(true);
        extensionRepository.updateById(extension);

        Extension updated = extensionRepository.selectById(extension.getId());
        assertNotNull(updated);
        assertEquals("newPassword", updated.getPassword());
        assertEquals("New Name", updated.getDisplayName());
        assertTrue(updated.getEnabled());
    }

    @Test
    void testDelete() {
        Extension extension = Extension.builder()
                .extensionNumber("1004")
                .password("password789")
                .displayName("To Be Deleted")
                .enabled(false)
                .context("default")
                .build();

        extensionRepository.insert(extension);
        Long id = extension.getId();

        extensionRepository.deleteById(id);

        Extension deleted = extensionRepository.selectById(id);
        assertNull(deleted);
    }

    @Test
    void testFindAll() {
        for (int i = 0; i < 5; i++) {
            Extension extension = Extension.builder()
                    .extensionNumber("200" + i)
                    .password("password" + i)
                    .displayName("Extension " + i)
                    .enabled(i % 2 == 0)
                    .context("default")
                    .build();
            extensionRepository.insert(extension);
        }

        List<Extension> all = extensionRepository.selectList(null);
        assertEquals(5, all.size());
    }

    @Test
    void testFindByEnabled() {
        Extension enabledExt = Extension.builder()
                .extensionNumber("3001")
                .password("pass1")
                .displayName("Enabled Ext")
                .enabled(true)
                .context("default")
                .build();
        extensionRepository.insert(enabledExt);

        Extension disabledExt = Extension.builder()
                .extensionNumber("3002")
                .password("pass2")
                .displayName("Disabled Ext")
                .enabled(false)
                .context("default")
                .build();
        extensionRepository.insert(disabledExt);

        List<Extension> enabledExtensions = extensionRepository.selectList(
                new LambdaQueryWrapper<Extension>()
                        .eq(Extension::getEnabled, true)
        );

        assertEquals(1, enabledExtensions.size());
        assertEquals("3001", enabledExtensions.get(0).getExtensionNumber());
    }

    @Test
    void testUniqueConstraint() {
        Extension ext1 = Extension.builder()
                .extensionNumber("4001")
                .password("password1")
                .displayName("First Extension")
                .enabled(false)
                .context("default")
                .build();
        extensionRepository.insert(ext1);

        Extension ext2 = Extension.builder()
                .extensionNumber("4001")
                .password("password2")
                .displayName("Duplicate Extension")
                .enabled(true)
                .context("default")
                .build();

        assertThrows(Exception.class, () -> extensionRepository.insert(ext2));
    }
}
