package cn.gzy.infrastructure.persistent.dao;


import cn.gzy.infrastructure.persistent.po.RuleTreeNode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IRuleTreeNodeDao {

    List<RuleTreeNode> queryRuleTreeNodeListByTreeId(String treeId);
}
