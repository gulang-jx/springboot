package zl.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import zl.bean.UserBean;
import zl.common.Common;
import zl.service.UserService;

@RestController
public class UserController {
	@Autowired UserService userService;
	
	@RequestMapping("/demo")
	public String demo(String pageNo,String pageSize,String name){
		UserBean bean = new UserBean();
		bean.setPageNo(Integer.parseInt(pageNo));
		bean.setPageSize(Integer.parseInt(pageSize));
		
		if(name != null){
			bean.setName(name);
		}
		List<UserBean> list = userService.getAllUser(bean);
		
		return list.size()+"===="+bean.getTotalNum();
	}
	@RequestMapping("/demo2")
	public String demo2(String pageNo,String pageSize,String name){
		UserBean bean = new UserBean();
		bean.setPageNo(Integer.parseInt(pageNo));
		bean.setPageSize(Integer.parseInt(pageSize));
		bean.setShardValue(Common.DB_1);
		if(name != null){
			bean.setName(name);
		}
		List<UserBean> list = userService.getAllUser(bean);
		
		return list.size()+"===="+bean.getTotalNum();
	}
}
