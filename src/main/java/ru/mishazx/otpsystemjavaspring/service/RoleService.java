package ru.mishazx.otpsystemjavaspring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import ru.mishazx.otpsystemjavaspring.exception.DefaultRoleNotFound;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.model.role.RoleUser;
import ru.mishazx.otpsystemjavaspring.repository.RoleRepository;

import jakarta.annotation.PostConstruct;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;


//Отвечает за работу с ролями пользователей
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    //Ищет роль по названию. Если не найдена — выбрасывает исключение.
    public RoleUser findRole(String nameRole){
        String roleName = nameRole.startsWith("ROLE_") ? nameRole : "ROLE_" + nameRole;

        try {
            return roleRepository.findByNameRole(roleName)
                    .orElseThrow(() -> new DefaultRoleNotFound("Роль " + roleName + " не найдена"));
        } catch (DefaultRoleNotFound ex) {
            return roleRepository.findByNameRole(nameRole)
                    .orElseThrow(() -> new DefaultRoleNotFound("Роль " + nameRole + " не найдена"));
        }
    }

    //Проверяет наличие роли, если нет — создаёт новую.
    public void checkRoleOrCreate(String arg){
        String roleName = arg.startsWith("ROLE_") ? arg : "ROLE_" + arg;

        try {
            RoleUser role = roleRepository.findByNameRole(roleName)
                    .orElseThrow(() -> new DefaultRoleNotFound("Не найдена"));

            log.info("Предустановленная роль: {} уже существует", role.getNameRole());
        } catch (DefaultRoleNotFound ex1) {
            try {
                RoleUser role = roleRepository.findByNameRole(arg)
                        .orElseThrow(() -> new DefaultRoleNotFound("Не найдена"));

                log.info("Предустановленная роль без префикса: {} существует, изменение не требуется",
                        role.getNameRole());
            } catch (DefaultRoleNotFound ex2) {
                log.warn("Отсутствует предустановленная роль {}, создаём...", roleName);
                roleRepository.save(new RoleUser(roleName));
            }
        }
    }

    //Инициализация стандартных ролей приложения.
    @PostConstruct
    public void initDefaultRoles() {
        log.info("Инициализация ролей пользователей");
        checkRoleOrCreate("USER");
        checkRoleOrCreate("ADMIN");
    }

    //Определяет и устанавливает роли для нового пользователя
    public boolean assignRolesForNewUser(User user) {
        String username = user.getUsername();
        Set<RoleUser> roles = new HashSet<>();

        // Проверяем, является ли логин "admin" и нет ли других администраторов
        if ("admin".equalsIgnoreCase(username) && !hasAdminUsers()) {
            RoleUser adminRole = findRole("ADMIN");
            roles.add(adminRole);
            log.info("Создание ПЕРВОГО администратора: {}", username);
            user.setRoleUsers(roles);
            return true;
        } else {
            // Для всех остальных пользователей или если админ уже есть
            RoleUser userRole = findRole("USER");
            roles.add(userRole);
            log.info("Создание обычного пользователя: {}", username);
            user.setRoleUsers(roles);
            return false;
        }
    }

    //Проверяет, есть ли уже пользователи с правами администратора
    public boolean hasAdminUsers() {
        return userRepository.findAll().stream()
                .anyMatch(user -> user.getRoleUsers().stream()
                        .anyMatch(role ->
                                role.getNameRole().equals("ROLE_ADMIN") ||
                                        role.getNameRole().equals("ADMIN")));
    }
}