package zl.mybatis.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import zl.interceptor.MybatisSpringPageInterceptor;

import com.alibaba.druid.pool.DruidDataSourceFactory;
//import com.github.pagehelper.PageHelper;
//import com.xbsafe.common.interceptors.PaginationInterceptor.PaginationInterceptor;

/**
 * 多数据源配置
 * @author zhouliang
 * @date 2017年9月20日
 */
@Configuration
@PropertySource("classpath:multiple-datasource.properties")
public class MyBatisConfig {
	@Autowired
	private Environment env;
	@Autowired MybatisSpringPageInterceptor inteceptor;

	/**
	 * 创建数据源(数据源的名称：方法名可以取为XXXDataSource(),XXX为数据库名称,该名称也就是数据源的名称)
	 */
	@Bean
	public DataSource operateDataSource() throws Exception {
		Properties props = new Properties();
		props.put("driverClassName", env.getProperty("operate.datasource.driver-class-name"));
		props.put("url", env.getProperty("operate.datasource.url"));
		props.put("username", env.getProperty("operate.datasource.username"));
		props.put("password", env.getProperty("operate.datasource.password"));
		return DruidDataSourceFactory.createDataSource(props);
	}
	@Bean
	public DataSource routeDataSource() throws Exception {
		Properties props = new Properties();
		props.put("driverClassName", env.getProperty("route.datasource.driver-class-name"));
		props.put("url", env.getProperty("route.datasource.url"));
		props.put("username", env.getProperty("route.datasource.username"));
		props.put("password", env.getProperty("route.datasource.password"));
		return DruidDataSourceFactory.createDataSource(props);
	}

	/**
	 * @Primary 该注解表示在同一个接口有多个实现类可以注入的时候，默认选择哪一个，而不是让@autowire注解报错
	 * @Qualifier 根据名称进行注入，通常是在具有相同的多个类型的实例的一个注入（例如有多个DataSource类型的实例）
	 */
	@Bean
	@Primary
	public DynamicDataSource dataSource(
			@Qualifier("routeDataSource") DataSource routeDataSource,
			@Qualifier("operateDataSource") DataSource operateDataSource) {
		Map<Object, Object> targetDataSources = new HashMap<Object, Object>();
		targetDataSources.put(DatabaseType.routeDS, routeDataSource);
		targetDataSources.put(DatabaseType.operateDS, operateDataSource);

		DynamicDataSource dataSource = new DynamicDataSource();
		dataSource.setTargetDataSources(targetDataSources);// 该方法是AbstractRoutingDataSource的方法
		return dataSource;
	}

	/**
	 * 根据数据源创建SqlSessionFactory
	 */
	@Bean
	public SqlSessionFactory sqlSessionFactory(DynamicDataSource ds)
			throws Exception {
		SqlSessionFactoryBean fb = new SqlSessionFactoryBean();
		fb.setDataSource(ds);// 指定数据源(这个必须有，否则报错)
		// 下边两句仅仅用于*.xml文件，如果整个持久层操作不需要使用到xml文件的话（只用注解就可以搞定），则不加
		fb.setTypeAliasesPackage(env.getProperty("mybatis.typeAliasesPackage"));// 指定基包
		fb.setMapperLocations(new PathMatchingResourcePatternResolver()
				.getResources(env.getProperty("mybatis.mapperLocations")));//
		fb.setPlugins(new Interceptor[]{inteceptor});
		return fb.getObject();
	}

}
