package com.memes.api.config;

import com.memes.api.mappers.custom.MemeSearchMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test that verifies MyBatis can parse all XML mappers and bind their
 * namespace interfaces without duplicate-statement collisions.
 *
 * This catches regressions such as a mapper method defined both with @Select
 * and in the XML file for the same namespace, which causes:
 *
 *   Mapped Statements collection already contains key ... please check
 *   class path resource [mappers/custom/MemeSearchMapper.xml]
 *   and com/memes/api/mappers/custom/MemeSearchMapper.java
 */
class MapperLoadSmokeTest {

    @Test
    void sqlSessionFactory_loadsMappersWithoutDuplicateStatements() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(noOpDataSource());
        factoryBean.setTypeAliasesPackage("com.memes.api.models");
        factoryBean.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mappers/**/*.xml"));

        SqlSessionFactory factory = factoryBean.getObject();
        assertThat(factory).isNotNull();

        var configuration = factory.getConfiguration();
        assertThat(configuration.hasMapper(MemeSearchMapper.class)).isTrue();

        var statementNames = configuration.getMappedStatementNames();
        assertThat(statementNames)
            .contains(
                "com.memes.api.mappers.custom.MemeSearchMapper.selectMemesFlat",
                "com.memes.api.mappers.custom.MemeSearchMapper.countMemesFlat",
                "com.memes.api.mappers.custom.MemeSearchMapper.searchMemes",
                "com.memes.api.mappers.custom.MemeSearchMapper.selectMemeDetail",
                "com.memes.api.mappers.custom.MemeSearchMapper.selectMemeDetailsBatch");
    }

    private DataSource noOpDataSource() {
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                throw new SQLException("Intentionally no-op datasource");
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                throw new SQLException("Intentionally no-op datasource");
            }

            @Override
            public java.io.PrintWriter getLogWriter() {
                return new java.io.PrintWriter(System.out);
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) {
                // no-op
            }

            @Override
            public void setLoginTimeout(int seconds) {
                // no-op
            }

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                return java.util.logging.Logger.getLogger("com.memes.api");
            }

            @Override
            public <T> T unwrap(Class<T> iface) {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }
        };
    }
}
