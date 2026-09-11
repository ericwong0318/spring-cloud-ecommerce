package org.example.authserver.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthorizationServerConfigTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void registeredClientRepositoryBean_createsInstance() {
        RegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        assertThat(repository).isNotNull();
    }

    @Test
    void registeredClientRepository_isInstanceOfJdbcRegisteredClientRepository() {
        RegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        assertThat(repository).isInstanceOf(JdbcRegisteredClientRepository.class);
    }
}
