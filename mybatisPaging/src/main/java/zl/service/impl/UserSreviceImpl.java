package zl.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import zl.bean.UserBean;
import zl.mybatis.mapper.UserMapper;
import zl.service.UserService;

@Component
public class UserSreviceImpl implements UserService{
	@Autowired UserMapper userMapper;
	public List<UserBean> getAllUser(UserBean bean) {
		// TODO Auto-generated method stub
		return userMapper.getAllUser(bean);
	}

}
