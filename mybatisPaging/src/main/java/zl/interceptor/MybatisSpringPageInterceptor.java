package zl.interceptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import zl.bean.BaseBean;

@Component
@Intercepts({ @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class }) })
public class MybatisSpringPageInterceptor implements Interceptor {

	private static final Logger logger = LoggerFactory.getLogger(MybatisSpringPageInterceptor.class.getName());


	@SuppressWarnings("unused")
	public Object intercept(Invocation arg0) throws Throwable {
		MappedStatement mappedStatement = (MappedStatement) arg0.getArgs()[0];
		Object parameter = arg0.getArgs()[1];
		BoundSql boundSql = mappedStatement.getBoundSql(parameter);
		if (null == boundSql || StringUtils.isBlank(boundSql.getSql())) {
			return null;
		}
		RowBounds rowBounds = (RowBounds) arg0.getArgs()[2];
		Object parameterObject = boundSql.getParameterObject();
		BaseBean model = null;
		if (parameterObject instanceof BaseBean) {
			model = (BaseBean) parameterObject;
		} else {
			BoundSql newBoundSql = copyFromBoundSql(mappedStatement, boundSql, boundSql.getSql());
			arg0.getArgs()[0] = copyFromMappedStatement(mappedStatement, new BoundSqlSqlSource(newBoundSql));
			return arg0.proceed();
		}
		if (null == model) {
			throw new Exception("无法获取分页参数.");
		}
		if (model.getPageNo() == -1) {
			BoundSql newBoundSql = copyFromBoundSql(mappedStatement, boundSql, boundSql.getSql());
			arg0.getArgs()[0] = copyFromMappedStatement(mappedStatement, new BoundSqlSqlSource(newBoundSql));
			return arg0.proceed();
		}
		String shardSql = boundSql.getSql();
		queryTotal(mappedStatement, shardSql, parameterObject, boundSql,model);
		
		if (null == rowBounds || rowBounds == RowBounds.DEFAULT) {
			rowBounds = new RowBounds(model.getPageSize() * (model.getPageNo() - 1), model.getPageSize());
		}
		String pagesql = getLimitSql(shardSql, rowBounds.getOffset(), rowBounds.getLimit());
		arg0.getArgs()[2] = new RowBounds(RowBounds.NO_ROW_OFFSET, RowBounds.NO_ROW_LIMIT);
		BoundSql newBoundSql =  copyFromBoundSql(mappedStatement, boundSql, pagesql);
		arg0.getArgs()[0] = copyFromMappedStatement(mappedStatement, new BoundSqlSqlSource(newBoundSql));
		return arg0.proceed();
	}

	public static class BoundSqlSqlSource implements SqlSource {
		BoundSql boundSql;

		public BoundSqlSqlSource(BoundSql boundSql) {
			this.boundSql = boundSql;
		}

		public BoundSql getBoundSql(Object parameterObject) {
			return boundSql;
		}
	}
	private String getLimitSql(String sql, int start, int end) throws Exception{
		if(sql ==null){
			throw new Exception("execute sql is empty.");
		}
		StringBuffer sqlBuffer = new StringBuffer(sql.length()+300);
		sqlBuffer.append(sql);
		sqlBuffer.append(" LIMIT ").append(start).append(",").append(end);
		return sqlBuffer.toString();
	}
	private void queryTotal(MappedStatement mappedStatement, String replaceSql, Object parameterObject, BoundSql boundSql,BaseBean model) throws Exception{
		StringBuffer countSql = new StringBuffer();
		
		if(model.getTotalMappedStatementId()!=null && model.getTotalMappedStatementId().length()>0){
			MappedStatement totalMappedStatement=mappedStatement.getConfiguration().getMappedStatement(model.getTotalMappedStatementId());
			BoundSql totalBoundSql = totalMappedStatement.getBoundSql(parameterObject);
			
			countSql.append(totalBoundSql.getSql());
		}else{
			// 未指定，自动拼装
			countSql.append("SELECT COUNT(1) FROM (").append(replaceSql).append(") as total");
		}
		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = mappedStatement.getConfiguration().getEnvironment().getDataSource().getConnection();
			if (logger.isDebugEnabled()) {
				logger.debug(countSql.toString());
			}
			ps = conn.prepareStatement(countSql.toString());
			BoundSql countBS = copyFromBoundSql(mappedStatement, boundSql, countSql.toString());
			setParameters(ps, mappedStatement, countBS, parameterObject);
			rs = ps.executeQuery();
			if (rs.next()) {
				model.setTotalNum(rs.getLong(1));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new Exception(e.getMessage(), e);
		} finally {
			try {
				if (null != rs) {
					rs.close();
				}
			} catch (Exception e) {
				logger.error("rs.close() error!", e);
			}
			try {
				if (null != ps) {
					ps.close();
				}
			} catch (Exception e) {
				logger.error("ps.close() error!", e);
			}
			try {
				if (null != conn) {
					conn.close();
				}
			} catch (Exception e) {
				logger.error("conn.close() error!", e);
			}
		}
	}
	protected MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
		Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
		builder.resource(ms.getResource());
		builder.fetchSize(ms.getFetchSize());
		builder.statementType(ms.getStatementType());
		builder.keyGenerator(ms.getKeyGenerator());
		// builder.keyProperty(ms.getKeyProperties());
		builder.timeout(ms.getTimeout());
		builder.parameterMap(ms.getParameterMap());
		builder.resultMaps(ms.getResultMaps());
		builder.cache(ms.getCache());
		MappedStatement newMs = builder.build();
		return newMs;
	}

	/**
	 * 
	 * @param ps
	 * @param mappedStatement
	 * @param boundSql
	 * @param parameterObject
	 * @throws SQLException
	 */
	private void setParameters(PreparedStatement ps, MappedStatement mappedStatement, BoundSql boundSql, Object parameterObject) throws SQLException {
		ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		if (parameterMappings != null) {
			Configuration configuration = mappedStatement.getConfiguration();
			TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
			MetaObject metaObject = parameterObject == null ? null : configuration.newMetaObject(parameterObject);
			for (int i = 0; i < parameterMappings.size(); i++) {
				ParameterMapping parameterMapping = parameterMappings.get(i);
				if (parameterMapping.getMode() != ParameterMode.OUT) {
					Object value;
					String propertyName = parameterMapping.getProperty();
					if (parameterObject == null) {
						value = null;
					} else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
						value = parameterObject;
					} else if (boundSql.hasAdditionalParameter(propertyName)) {
						value = boundSql.getAdditionalParameter(propertyName);
					}  else {
						value = metaObject == null ? null : metaObject.getValue(propertyName);
					}
					TypeHandler typeHandler = parameterMapping.getTypeHandler();
					if (typeHandler == null) {
						throw new ExecutorException("There was no TypeHandler found for parameter " + propertyName + " of statement " + mappedStatement.getId());
					}
					logger.debug(i + 1 + ":" + value);
					typeHandler.setParameter(ps, i + 1, value, parameterMapping.getJdbcType());
				}
			}
		}
	}
	@Override
	public Object plugin(Object arg0) {
		return Plugin.wrap(arg0, this);
	}

	public void setProperties(Properties arg0) {

	}

	
	private BoundSql copyFromBoundSql(MappedStatement ms, BoundSql boundSql, String sql) {  
	    BoundSql newBoundSql = new BoundSql(ms.getConfiguration(),sql, boundSql.getParameterMappings(), boundSql.getParameterObject());  
	    for (ParameterMapping mapping : boundSql.getParameterMappings()) {  
	        String prop = mapping.getProperty();  
	        if (boundSql.hasAdditionalParameter(prop)) {  
	            newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));  
	        }  
	    }  
	    return newBoundSql;  
	}

}
