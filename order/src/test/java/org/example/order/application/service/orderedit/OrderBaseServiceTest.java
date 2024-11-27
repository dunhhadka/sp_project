package org.example.order.application.service.orderedit;

import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

@BootstrapWith(DefaultTestContextBootstrapper.class)
@SpringJUnitConfig(initializers = ConfigDataApplicationContextInitializer.class,
        classes = {OrderBaseServiceTest.BaseConfiguration.class})
@TestPropertySource(properties = {
        """
                spring.jpa.hibernate.ddl-auto=update
                spring.jpa.properties.hibernate.show_sql=true
                spring.jpa.properties.hibernate.format_sql=true
                        """
})
abstract class OrderBaseServiceTest {


    @ImportAutoConfiguration({
            HibernateJpaAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            TransactionAutoConfiguration.class
    })
    @Configuration
    @EnableJpaRepositories(basePackages = "org.example.order.order.infrastructure.persistence")
    @EntityScan(basePackageClasses = OrderEdit.class)
    static class BaseConfiguration {

    }
}
