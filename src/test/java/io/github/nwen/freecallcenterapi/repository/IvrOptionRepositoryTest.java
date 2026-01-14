package io.github.nwen.freecallcenterapi.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.nwen.freecallcenterapi.entity.IvrMenu;
import io.github.nwen.freecallcenterapi.entity.IvrOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IvrOptionRepositoryTest {

    @Autowired
    private IvrOptionRepository ivrOptionRepository;

    @Autowired
    private IvrMenuRepository ivrMenuRepository;

    private Long menuId;

    @BeforeEach
    void setUp() {
        ivrOptionRepository.delete(new LambdaQueryWrapper<>());
        ivrMenuRepository.delete(new LambdaQueryWrapper<>());

        IvrMenu menu = IvrMenu.builder()
                .name("Test Menu")
                .description("Test IVR Menu")
                .welcomeSound("welcome.wav")
                .timeoutSeconds(5)
                .maxAttempts(3)
                .invalidSound("invalid.wav")
                .enabled(true)
                .build();
        ivrMenuRepository.insert(menu);
        menuId = menu.getId();
    }

    @Test
    void testInsertAndFindById() {
        IvrOption option = IvrOption.builder()
                .ivrMenuId(menuId)
                .digit("1")
                .action("TRANSFER")
                .destination("1001")
                .description("Talk to sales")
                .priority(1)
                .build();

        ivrOptionRepository.insert(option);

        IvrOption found = ivrOptionRepository.selectById(option.getId());
        assertNotNull(found);
        assertEquals("1", found.getDigit());
        assertEquals("TRANSFER", found.getAction());
        assertEquals("1001", found.getDestination());
    }

    @Test
    void testFindByMenuId() {
        for (int i = 1; i <= 3; i++) {
            IvrOption option = IvrOption.builder()
                    .ivrMenuId(menuId)
                    .digit(String.valueOf(i))
                    .action("TRANSFER")
                    .destination("100" + i)
                    .description("Option " + i)
                    .priority(i)
                    .build();
            ivrOptionRepository.insert(option);
        }

        List<IvrOption> menuOptions = ivrOptionRepository.selectList(
                new LambdaQueryWrapper<IvrOption>()
                        .eq(IvrOption::getIvrMenuId, menuId)
                        .orderByAsc(IvrOption::getPriority)
        );

        assertEquals(3, menuOptions.size());
        assertEquals("1", menuOptions.get(0).getDigit());
        assertEquals("2", menuOptions.get(1).getDigit());
        assertEquals("3", menuOptions.get(2).getDigit());
    }

    @Test
    void testUpdate() {
        IvrOption option = IvrOption.builder()
                .ivrMenuId(menuId)
                .digit("9")
                .action("TRANSFER")
                .destination("1000")
                .description("Original description")
                .priority(9)
                .build();

        ivrOptionRepository.insert(option);

        option.setAction("PLAYBACK");
        option.setDestination("voicemail.wav");
        option.setDescription("Updated description");
        option.setPriority(10);
        ivrOptionRepository.updateById(option);

        IvrOption updated = ivrOptionRepository.selectById(option.getId());
        assertNotNull(updated);
        assertEquals("PLAYBACK", updated.getAction());
        assertEquals("voicemail.wav", updated.getDestination());
    }

    @Test
    void testDelete() {
        IvrOption option = IvrOption.builder()
                .ivrMenuId(menuId)
                .digit("0")
                .action("TRANSFER")
                .destination("operator")
                .description("Operator")
                .priority(0)
                .build();

        ivrOptionRepository.insert(option);
        Long id = option.getId();

        ivrOptionRepository.deleteById(id);

        IvrOption deleted = ivrOptionRepository.selectById(id);
        assertNull(deleted);
    }

    @Test
    void testFindByDigit() {
        IvrOption option = IvrOption.builder()
                .ivrMenuId(menuId)
                .digit("5")
                .action("MENU")
                .destination("submenu")
                .description("Go to submenu")
                .priority(5)
                .build();

        ivrOptionRepository.insert(option);

        IvrOption found = ivrOptionRepository.selectOne(
                new LambdaQueryWrapper<IvrOption>()
                        .eq(IvrOption::getIvrMenuId, menuId)
                        .eq(IvrOption::getDigit, "5")
        );

        assertNotNull(found);
        assertEquals("MENU", found.getAction());
    }

    @Test
    void testFindByAction() {
        IvrOption transfer = IvrOption.builder()
                .ivrMenuId(menuId)
                .digit("1")
                .action("TRANSFER")
                .destination("1001")
                .description("Transfer")
                .priority(1)
                .build();
        ivrOptionRepository.insert(transfer);

        IvrOption playback = IvrOption.builder()
                .ivrMenuId(menuId)
                .digit("2")
                .action("PLAYBACK")
                .destination("voicemail.wav")
                .description("Playback")
                .priority(2)
                .build();
        ivrOptionRepository.insert(playback);

        List<IvrOption> transferOptions = ivrOptionRepository.selectList(
                new LambdaQueryWrapper<IvrOption>()
                        .eq(IvrOption::getIvrMenuId, menuId)
                        .eq(IvrOption::getAction, "TRANSFER")
        );

        assertEquals(1, transferOptions.size());
        assertEquals("1", transferOptions.get(0).getDigit());
    }

    @Test
    void testUniqueConstraint() {
        IvrOption option1 = IvrOption.builder()
                .ivrMenuId(menuId)
                .digit("7")
                .action("TRANSFER")
                .destination("1007")
                .description("First option 7")
                .priority(7)
                .build();
        ivrOptionRepository.insert(option1);

        IvrOption option2 = IvrOption.builder()
                .ivrMenuId(menuId)
                .digit("7")
                .action("PLAYBACK")
                .destination("voicemail.wav")
                .description("Duplicate digit")
                .priority(7)
                .build();

        assertThrows(Exception.class, () -> ivrOptionRepository.insert(option2));
    }

    @Test
    void testCascadeDelete() {
        IvrOption option = IvrOption.builder()
                .ivrMenuId(menuId)
                .digit("8")
                .action("TRANSFER")
                .destination("1008")
                .description("Test cascade")
                .priority(8)
                .build();
        ivrOptionRepository.insert(option);

        ivrMenuRepository.deleteById(menuId);

        List<IvrOption> remaining = ivrOptionRepository.selectList(
                new LambdaQueryWrapper<IvrOption>()
                        .eq(IvrOption::getIvrMenuId, menuId)
        );

        assertEquals(0, remaining.size());
    }
}
