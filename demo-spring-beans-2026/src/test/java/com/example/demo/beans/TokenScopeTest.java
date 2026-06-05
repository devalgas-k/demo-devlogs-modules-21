package com.example.demo.beans;

import com.example.demo.beans.domain.Token;
import com.example.demo.beans.domain.TokenConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("fast")
class TokenScopeTest {

    @Autowired
    private TokenConsumer tokenConsumer;

    @Test
    void shouldCreateNewInstanceEachTimeWithObjectProvider() {
        Token t1 = tokenConsumer.newToken();
        Token t2 = tokenConsumer.newToken();
        
        assertThat(t1).isNotNull();
        assertThat(t2).isNotNull();
        assertThat(t1).isNotSameAs(t2);
        assertThat(t1.id()).isNotEqualTo(t2.id());
    }
}
