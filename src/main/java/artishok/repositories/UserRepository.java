package artishok.repositories;

import artishok.entities.User;
import artishok.entities.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Поиск по email (уникальный)
    Optional<User> findByEmail(String email);
    
    // Поиск по роли
    List<User> findByRole(UserRole role);
    
    // Поиск по роли с пагинацией
    Page<User> findByRole(UserRole role, Pageable pageable);
    
    // Поиск активных пользователей
    List<User> findByIsActiveTrue();
    
    // Поиск неактивных пользователей
    List<User> findByIsActiveFalse();
    
    // Поиск по роли и активности
    List<User> findByRoleAndIsActive(UserRole role, Boolean isActive);
    
    // Поиск пользователей по дате регистрации (после указанной даты)
    List<User> findByRegistrationDateAfter(LocalDateTime date);
    
    // Поиск по имени (частичное совпадение)
    List<User> findByFullNameContainingIgnoreCase(String name);
    boolean existsByPhoneNumber(String phoneNumber);
    
    // Проверка существования email
    boolean existsByEmail(String email);
    
    // Подсчет пользователей по роли
    long countByRole(UserRole role);
    long countByIsActiveTrue();
    
    // Получение последних зарегистрированных пользователей
    List<User> findTop10ByOrderByRegistrationDateDesc();
    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> findUsersByFilters(@Param("role") UserRole role,
                                  @Param("search") String search);
}