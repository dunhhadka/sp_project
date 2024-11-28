package org.example.order.application.service.orderedit;

import org.example.order.SapoClient;
import org.example.order.order.application.service.orderedit.OrderEditContextService;
import org.example.order.order.application.service.orderedit.OrderEditWriteService;
import org.example.order.order.application.utils.TaxHelper;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.orderedit.model.OrderEdit;
import org.example.order.order.infrastructure.data.dao.ProductDao;
import org.example.order.order.infrastructure.persistence.JpaOrderEditRepository;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import javax.sql.DataSource;

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
            TransactionAutoConfiguration.class,
            MockConfiguration.class
    })
    @Configuration
    @EnableJpaRepositories(basePackages = "org.example.order.order.infrastructure.persistence")
    @EntityScan(basePackageClasses = OrderEdit.class)
    @Import({OrderEditWriteService.class, JpaOrderEditRepository.class, OrderEditContextService.class, MockConfiguration.class})
    static class BaseConfiguration {

        @Bean
        @ConditionalOnMissingBean
        DataSource dataSource() {
            return DataSourceBuilder.create()
                    .driverClassName("org.h2.Driver")
                    .url("jdbc:h2:mem:testdb;MODE=MSSQLServer;CASE_INSENSITIVE_IDENTIFIERS=TRUE;IGNORECASE=TRUE")
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean
        OrderRepository orderRepository() {
            return new TestOrderRepository();
        }
    }

    @Configuration
    static class MockConfiguration {
        @Bean
        @ConditionalOnMissingBean
        ProductDao productDao() {
            return mock(ProductDao.class);
        }

        @Bean
        @ConditionalOnMissingBean
        SapoClient sapoClient() {
            return mock(SapoClient.class);
        }

        @Bean
        @ConditionalOnMissingBean
        TaxHelper taxHelper() {
            return mock(TaxHelper.class);
        }
    }

    static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz);
    }
}
