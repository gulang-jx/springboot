package zl.mybatis.config;

/**
 * 获取数据源连接
 * @author zhouliang
 * @date 2017年9月20日
 */
public class DatabaseContextHolder {
	private static final ThreadLocal<DatabaseType> contextHolder = new ThreadLocal<DatabaseType>();
	  
	  public static void setDatabaseType(DatabaseType type){
	    contextHolder.set(type);
	  }
	  
	  public static DatabaseType getDatabaseType(){
	    return contextHolder.get();
	  }
	  public static void clearDatabaseType(){
		  contextHolder.remove();
	  }
}
