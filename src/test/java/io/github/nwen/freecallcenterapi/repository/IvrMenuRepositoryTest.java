package io.github.nwen.freecallcenterapi.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.nwen.freecallcenterapi.entity.IvrMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IvrMenuRepositoryTest {

    @Autowired
    private IvrMenuRepository ivrMenuRepository;

    @BeforeEach
    void setUp() {
        ivrMenuRepository.delete(new LambdaQueryWrapper<>());
    }

    @Test
    void testInsertAndFindById() {
        IvrMenu menu = IvrMenu.builder()
                .name("Main Menu")
                .description("Main IVR menu for incoming calls")
                .welcomeSound("welcome.wav")
                .timeoutSeconds(5)
                .maxAttempts(3)
                .invalidSound("invalid.wav")
                .enabled(true)
                .build();

        ivrMenuRepository.insert(menu);

        IvrMenu found = ivrMenuRepository.selectById(menu.getId());
        assertNotNull(found);
        assertEquals("Main Menu", found.getName());
        assertEquals("welcome.wav", found.getWelcomeSound());
    }

    @Test
    void testFindByName() {
        IvrMenu menu = IvrMenu.builder()
                .name("Sales Menu")
                .description("Sales department menu")
                .welcomeSound("sales.wav")
                .timeoutSeconds(10)
                .maxAttempts(2)
                .invalidSound("invalid.wav")
                .enabled(true)
                .build();

        ivrMenuRepository.insert(menu);

        IvrMenu found = ivrMenuRepository.selectOne(
                new LambdaQueryWrapper<IvrMenu>()
                        .eq(IvrMenu::getName, "Sales Menu")
        );

        assertNotNull(found);
        assertEquals("Sales Menu", found.getName());
    }

    @Test
    void testUpdate() {
        IvrMenu menu = IvrMenu.builder()
                .name("Support Menu")
                .description("Support department")
                .welcomeSound("support.wav")
                .timeoutSeconds(5)
                .maxAttempts(3)
                .invalidSound("invalid.wav")
                .enabled(true)
                .build();

        ivrMenuRepository.insert(menu);

        menu.setName("Updated Support Menu");
        menu.setTimeoutSeconds(15);
        menu.setEnabled(false);
        ivrMenuRepository.updateById(menu);

        IvrMenu updated = ivrMenuRepository.selectById(menu.getId());
        assertNotNull(updated);
        assertEquals("Updated Support Menu", updated.getName());
        assertEquals(15, updated.getTimeoutSeconds());
        assertFalse(updated.getEnabled());
    }

    @Test
    void testDelete() {
        IvrMenu menu = IvrMenu.builder()
                .name("To Be Deleted")
                .description("Delete test")
                .welcomeSound("delete.wav")
                .timeoutSeconds(5)
                .maxAttempts(3)
                .invalidSound("invalid.wav")
                .enabled(true)
                .build();

        ivrMenuRepository.insert(menu);
        Long id = menu.getId();

        ivrMenuRepository.deleteById(id);

        IvrMenu deleted = ivrMenuRepository.selectById(id);
        assertNull(deleted);
    }

    @Test
    void testFindAll() {
        for (int i = 0; i < 3; i++) {
            IvrMenu menu = IvrMenu.builder()
                    .name("Menu " + i)
                    .description("Description " + i)
                    .welcomeSound("welcome" + i + ".wav")
                    .timeoutSeconds(5 + i)
                    .maxAttempts(3)
                    .invalidSound("invalid.wav")
                    .enabled(true)
                    .build();
            ivrMenuRepository.insert(menu);
        }

        List<IvrMenu> all = ivrMenuRepository.selectList(null);
        assertEquals(3, all.size());
    }

    @Test
    void testFindEnabledMenus() {
        IvrMenu enabled = IvrMenu.builder()
                .name("Enabled Menu")
                .description("This menu is enabled")
                .welcomeSound("welcome.wav")
                .timeoutSeconds(5)
                .maxAttempts(3)
                .invalidSound("invalid.wav")
                .enabled(true)
                .build();
        ivrMenuRepository.insert(enabled);

        IvrMenu disabled = IvrMenu.builder()
                .name("Disabled Menu")
                .description("This menu is disabled")
                .welcomeSound("welcome.wav")
                .timeoutSeconds(5)
                .maxAttempts(3)
                .invalidSound("invalid.wav")
                .enabled(false)
                .build();
        ivrMenuRepository.insert(disabled);

        List<IvrMenu> enabledMenus = ivrMenuRepository.selectList(
                new LambdaQueryWrapper<IvrMenu>()
                        .eq(IvrMenu::getEnabled, true)
        );

        assertEquals(1, enabledMenus.size());
        assertEquals("Enabled Menu", enabledMenus.get(0).getName());
    }

    @Test
    void testFindByTimeoutRange() {
        IvrMenu shortTimeout = IvrMenu.builder()
                .name("Short Timeout")
                .description("Quick response")
                .welcomeSound("welcome.wav")
                .timeoutSeconds(3)
                .maxAttempts(3)
                .invalidSound("invalid.wav")
                .enabled(true)
                .build();
        ivrMenuRepository.insert(shortTimeout);

        IvrMenu longTimeout = IvrMenu.builder()
                .name("Long Timeout")
                .description("Slow response")
                .welcomeSound("welcome.wav")
                .timeoutSeconds(30)
                .maxAttempts(3)
                .invalidSound("invalid.wav")
                .enabled(true)
                .build();
        ivrMenuRepository.insert(longTimeout);

        List<IvrMenu> shortTimeoutMenus = ivrMenuRepository.selectList(
                new LambdaQueryWrapper<IvrMenu>()
                        .le(IvrMenu::getTimeoutSeconds, 5)
        );

        assertEquals(1, shortTimeoutMenus.size());
        assertEquals("Short Timeout", shortTimeoutMenus.get(0).getName());
    }
}
