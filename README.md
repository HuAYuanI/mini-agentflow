# Mini-AgentFlow

一个面向 Java 后端/Agent 方向的精简工作流项目，用于简历与面试演示。

## 当前已完成（D1-D3）

- DAG 工作流定义模型
- Kahn 拓扑排序 + 环检测
- 模板方法模式：`AbstractNodeExecutor`
- 策略模式：`NodeExecutor` + `NodeExecutorRegistry`
- 串行执行引擎：`SerialWorkflowEngine`
- 节点变量引用：`${var}` 模板解析与运行时替换
- 引用合法性校验：引用变量必须来源于输入或上游节点输出
- 图结构增强校验：唯一 START/END、重复边/自环检测、START 可达性、END 可达性
- 并行执行引擎：`ParallelWorkflowEngine`（`CompletableFuture` + `AtomicInteger`）
- REST API：`POST /api/workflow/execute`
- 单元测试：环检测、线性链路执行、变量引用解析、非法引用拦截、并行执行验证

## 快速启动

```bash
cd mini-agentflow
mvn spring-boot:run
```

服务端口：`18080`

## 调试请求示例

```bash
curl -X POST http://localhost:18080/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{
    "engineMode": "PARALLEL",
    "inputs": {"input": "你好，做一段简历项目介绍"},
    "workflow": {
      "nodes": [
        {"id": "start", "type": "START", "config": {"outputKey": "raw"}},
        {"id": "llmA", "type": "LLM", "config": {"prompt": "A:${raw}", "outputKey": "a"}},
        {"id": "llmB", "type": "LLM", "config": {"prompt": "B:${raw}", "outputKey": "b"}},
        {"id": "plugin", "type": "PLUGIN", "config": {"text": "${a}|${b}", "outputKey": "merged"}},
        {"id": "end", "type": "END", "config": {"result": "${merged}"}}
      ],
      "edges": [
        {"from": "start", "to": "llmA"},
        {"from": "start", "to": "llmB"},
        {"from": "llmA", "to": "plugin"},
        {"from": "llmB", "to": "plugin"},
        {"from": "plugin", "to": "end"}
      ]
    }
  }'
```

## 10 天冲刺路线

- D1-D2：串行执行引擎 + DAG 校验 + 模式解耦
- D3：并行引擎（`CompletableFuture` + `AtomicInteger`）
- D4：TTL 上下文传递（`TransmittableThreadLocal`）
- D5-D6：超时/重试/中断/错误分支策略
- D7-D8：SSE 全链路生命周期事件推送
- D9：Spring AI + Prompt 模板 + 历史窗口
- D10：`@DistributedLock` + AOP + SpEL + 简历沉淀
