package zl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * 项目入口，启动类
 * @author ZhouLiang
 *
 * @Date 2017年11月20日
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class})
@ServletComponentScan
@MapperScan("zl.mybatis.mapper")
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class);
	}
}
