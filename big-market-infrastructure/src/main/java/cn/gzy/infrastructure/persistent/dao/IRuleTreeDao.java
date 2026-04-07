package cn.gzy.infrastructure.persistent.dao;


import cn.gzy.infrastructure.persistent.po.RuleTree;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IRuleTreeDao {

    public RuleTree queryRuleTreeByTreeId(String treeId);
}
