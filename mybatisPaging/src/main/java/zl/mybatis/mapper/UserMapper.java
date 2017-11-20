package zl.mybatis.mapper;

import java.util.List;

import zl.bean.UserBean;

public interface UserMapper {
	public List<UserBean> getAllUser(UserBean bean);
}
