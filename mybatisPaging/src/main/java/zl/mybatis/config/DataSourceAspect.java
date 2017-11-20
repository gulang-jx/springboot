package zl.mybatis.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import zl.bean.BaseBean;
import zl.common.Common;

@Aspect
@Component
public class DataSourceAspect {
	Logger logger = LoggerFactory.getLogger(DataSourceAspect.class);

	@Before("execution(* zl.mybatis.mapper.*.*(..))")
	public void setDataSourcePgKey(JoinPoint point) {
		Object args[] = point.getArgs();
		for(Object obj:args){
			if(obj instanceof BaseBean){
				BaseBean bean = (BaseBean) obj;
				if(Common.DB_0==bean.getShardValue()){
					logger.info("===========================使用数据源DB_route=======================");
					DatabaseContextHolder.setDatabaseType(DatabaseType.routeDS);
				}else{
					logger.info("===========================使用数据源DB_operate=======================");
					DatabaseContextHolder.setDatabaseType(DatabaseType.operateDS);
				}
				break;
			}
		}
	}

}
