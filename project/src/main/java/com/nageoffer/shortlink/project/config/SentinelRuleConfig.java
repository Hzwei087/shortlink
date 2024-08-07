package com.nageoffer.shortlink.project.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 初始化限流配置
 */

@Component
public class SentinelRuleConfig implements InitializingBean {
    //spring启动就会调用这个方法
    @Override
    public void afterPropertiesSet() throws Exception {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule createOrderRule = new FlowRule();
        createOrderRule.setResource("create_short-link");
        //创建一个规则
        createOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        //两种方式，超过一个线程访问和QPS超过1，这里按照QPS做限制
        createOrderRule.setCount(1);
        //访问上限，每秒最多访问一次
        rules.add(createOrderRule);
        FlowRuleManager.loadRules(rules);
        //加载规则
    }
}
