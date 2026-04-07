package cn.gzy.infrastructure.persistent.dao;


import cn.gzy.infrastructure.persistent.po.RuleTreeNodeLine;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IRuleTreeNodeLineDao {

    List<RuleTreeNodeLine> queryRuleTreeNodeLineListByTreeId(String treeId);
}
