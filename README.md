# Mini-AgentFlow

一个面向 Java 后端/Agent 方向的精简工作流项目，用于简历与面试演示。

## 当前已完成（D1-D7）

- DAG 工作流定义模型
- Kahn 拓扑排序 + 环检测
- 模板方法模式：`AbstractNodeExecutor`
- 策略模式：`NodeExecutor` + `NodeExecutorRegistry`
- 串行执行引擎：`SerialWorkflowEngine`
- 节点变量引用：`${var}` 模板解析与运行时替换
- 引用合法性校验：引用变量必须来源于输入或上游节点输出
- 图结构增强校验：唯一 START/END、重复边/自环检测、START 可达性、END 可达性
- 并行执行引擎：`ParallelWorkflowEngine`（`CompletableFuture` + `AtomicInteger`）
- TTL 上下文传递：`TransmittableThreadLocal` + `TtlRunnable`（线程池场景上下文不丢失）
- 节点超时控制：`timeoutMs`
- 节点重试机制：`retryTimes`
- 节点失败策略：`errorStrategy=INTERRUPT|CONTINUE|ERROR_BRANCH`
- 错误分支跳转：`errorNext`（仅 `ERROR_BRANCH`）
- 统一执行事件模型：`WorkflowExecutionEvent`
- 统一生命周期事件：`WORKFLOW_STARTED / NODE_STARTED / NODE_RETRYING / NODE_COMPLETED / NODE_FAILED / WORKFLOW_COMPLETED`
- 事件监听器抽象：可被内存收集器或后续 SSE 推送复用
- SSE 流式执行接口：`POST /api/workflow/stream`
- 基于 `SseEmitter` 的实时事件推送：节点执行事件会边执行边推送
- REST API：`POST /api/workflow/execute`
- 单元测试：环检测、线性链路执行、变量引用解析、非法引用拦截、并行执行验证、TTL 传递验证、失败策略验证、事件标准化验证、SSE 服务验证

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

## SSE 调试示例

```bash
curl -N -X POST http://localhost:18080/api/workflow/stream \
  -H "Content-Type: application/json" \
  -d '{
    "engineMode": "SERIAL",
    "inputs": {"input": "你好，流式输出一下"},
    "workflow": {
      "nodes": [
        {"id": "start", "type": "START", "config": {"outputKey": "raw"}},
        {"id": "llm", "type": "LLM", "config": {"prompt": "SSE:${raw}", "outputKey": "answer"}},
        {"id": "end", "type": "END", "config": {"result": "${answer}"}}
      ],
      "edges": [
        {"from": "start", "to": "llm"},
        {"from": "llm", "to": "end"}
      ]
    }
  }'
```

## 10 天冲刺路线

- D1-D2：串行执行引擎 + DAG 校验 + 模式解耦
- D3：并行引擎（`CompletableFuture` + `AtomicInteger`）
- D4：TTL 上下文传递（`TransmittableThreadLocal` + `TtlRunnable`）
- D5：超时/重试/中断/错误分支策略
- D6：SSE 前的执行事件标准化
- D7：SSE 全链路生命周期事件推送
- D8：Spring AI + Prompt 模板 + 历史窗口
- D9：`@DistributedLock` + AOP + SpEL + 简历沉淀
