package in.coderarmy.gatekeeper.management.config;

import in.coderarmy.gatekeeper.management.entity.ManagementUser;
import in.coderarmy.gatekeeper.management.entity.Role;
import in.coderarmy.gatekeeper.management.repository.ManagementUserRepository;
import in.coderarmy.gatekeeper.management.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Creates the platform admin and the gateway service account on first start,
 * using passwords from configuration instead of credentials committed in a migration.
 * An account that already exists is never changed.
 */
@Component
public class PlatformAccountBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAccountBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final ManagementUserRepository managementUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String platformAdminUsername;
    private final String platformAdminPassword;
    private final String gatewayUsername;
    private final String gatewayPassword;

    public PlatformAccountBootstrap(
            ManagementUserRepository managementUserRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${gatekeeper.bootstrap.platform-admin-username}") String platformAdminUsername,
            @Value("${gatekeeper.bootstrap.platform-admin-password}") String platformAdminPassword,
            @Value("${gatekeeper.bootstrap.gateway-username}") String gatewayUsername,
            @Value("${gatekeeper.bootstrap.gateway-password}") String gatewayPassword) {
        this.managementUserRepository = managementUserRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.platformAdminUsername = platformAdminUsername;
        this.platformAdminPassword = platformAdminPassword;
        this.gatewayUsername = gatewayUsername;
        this.gatewayPassword = gatewayPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing(platformAdminUsername, platformAdminPassword, "PLATFORM_ADMIN");
        createIfMissing(gatewayUsername, gatewayPassword, "GATEWAY_SERVICE");
    }

    private void createIfMissing(String username, String password, String roleName) {
        if (managementUserRepository.existsByUsername(username)) {
            return;
        }

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Account '{}' was not created: set a password of at least {} characters in the environment",
                    username, MIN_PASSWORD_LENGTH);
            return;
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role " + roleName + " is missing"));

        ManagementUser user = new ManagementUser();
        user.setRole(role);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEnabled(true);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        managementUserRepository.save(user);

        log.info("Created account '{}' with role {}", username, roleName);
    }
}
