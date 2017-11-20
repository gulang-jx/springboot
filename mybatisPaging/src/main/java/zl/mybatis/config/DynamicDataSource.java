package zl.mybatis.config;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 抽象数据源
 * @author zhouliang
 * @date 2017年9月20日
 */
public class DynamicDataSource extends AbstractRoutingDataSource {


	@Override
	protected Object determineCurrentLookupKey() {
		// TODO Auto-generated method stub
		return DatabaseContextHolder.getDatabaseType();
	}

}
