const workflowTextarea = document.getElementById("workflow-json");
const engineModeSelect = document.getElementById("engine-mode");
const workflowInput = document.getElementById("workflow-input");
const lockKeyInput = document.getElementById("lock-key");
const lockTypeSelect = document.getElementById("lock-type");
const lockWaitInput = document.getElementById("lock-wait");
const lockLeaseInput = document.getElementById("lock-lease");
const workflowResultOutput = document.getElementById("workflow-result-output");
const workflowStatusValue = document.getElementById("workflow-status-value");
const workflowNodeCount = document.getElementById("workflow-node-count");
const workflowEventCount = document.getElementById("workflow-event-count");
const workflowLockState = document.getElementById("workflow-lock-state");
const workflowStatusChip = document.getElementById("workflow-status-chip");
const eventTimeline = document.getElementById("event-timeline");
const copyResultButton = document.getElementById("copy-result-btn");

const chatConversationId = document.getElementById("chat-conversation-id");
const chatProjectName = document.getElementById("chat-project-name");
const chatSystemPrompt = document.getElementById("chat-system-prompt");
const chatPromptTemplate = document.getElementById("chat-prompt-template");
const chatUserInput = document.getElementById("chat-user-input");
const chatProvider = document.getElementById("chat-provider");
const chatMockState = document.getElementById("chat-mock-state");
const chatMemorySize = document.getElementById("chat-memory-size");
const chatConversationValue = document.getElementById("chat-conversation-value");
const chatContentOutput = document.getElementById("chat-content-output");
const chatStatusChip = document.getElementById("chat-status-chip");

const defaultPresets = {
    parallel: {
        engineMode: "PARALLEL",
        input: "请帮我生成一段适合简历介绍的 Agent 项目亮点",
        lockKey: "",
        lockType: "REENTRANT",
        lockWaitTimeMs: "",
        lockLeaseTimeMs: "",
        workflow: {
            nodes: [
                { id: "start", type: "START", config: { outputKey: "raw" } },
                { id: "llmA", type: "LLM", config: { prompt: "请从架构设计角度总结：${raw}", outputKey: "a" } },
                { id: "llmB", type: "LLM", config: { prompt: "请从技术亮点角度总结：${raw}", outputKey: "b" } },
                { id: "plugin", type: "PLUGIN", config: { text: "${a}\n---\n${b}", outputKey: "merged" } },
                { id: "end", type: "END", config: { result: "${merged}" } }
            ],
            edges: [
                { from: "start", to: "llmA" },
                { from: "start", to: "llmB" },
                { from: "llmA", to: "plugin" },
                { from: "llmB", to: "plugin" },
                { from: "plugin", to: "end" }
            ]
        }
    },
    stream: {
        engineMode: "SERIAL",
        input: "请把这个 Agent 项目的执行过程拆成可讲解步骤",
        lockKey: "",
        lockType: "REENTRANT",
        lockWaitTimeMs: "",
        lockLeaseTimeMs: "",
        workflow: {
            nodes: [
                { id: "start", type: "START", config: { outputKey: "raw" } },
                { id: "llm", type: "LLM", config: { prompt: "请按步骤解释：${raw}", outputKey: "answer", delayMs: 220 } },
                { id: "plugin", type: "PLUGIN", config: { text: "SSE_EVENT:${answer}", outputKey: "streamed" } },
                { id: "end", type: "END", config: { result: "${streamed}" } }
            ],
            edges: [
                { from: "start", to: "llm" },
                { from: "llm", to: "plugin" },
                { from: "plugin", to: "end" }
            ]
        }
    },
    lock: {
        engineMode: "PARALLEL",
        input: "请生成一段带锁控执行说明的项目总结",
        lockKey: "resume-workflow-1",
        lockType: "FAIR",
        lockWaitTimeMs: 120,
        lockLeaseTimeMs: 30000,
        workflow: {
            nodes: [
                { id: "start", type: "START", config: { outputKey: "raw" } },
                { id: "llm", type: "LLM", config: { prompt: "请结合锁控执行场景总结：${raw}", outputKey: "answer" } },
                { id: "end", type: "END", config: { result: "${answer}" } }
            ],
            edges: [
                { from: "start", to: "llm" },
                { from: "llm", to: "end" }
            ]
        }
    }
};

const defaultChat = {
    conversationId: "resume-demo-1",
    project: "mini-agentflow",
    systemPrompt: "你是一个资深 Java 面试官，请用简历项目亮点的口吻回答。",
    promptTemplate: "请围绕 {project} 回答下面这个问题：{input}",
    userInput: "这个项目最值得讲的三点是什么？"
};

let latestWorkflowResult = null;

document.querySelectorAll("[data-scroll-target]").forEach((button) => {
    button.addEventListener("click", () => {
        document.getElementById(button.dataset.scrollTarget)?.scrollIntoView({ behavior: "smooth" });
    });
});

document.querySelectorAll("[data-preset]").forEach((button) => {
    button.addEventListener("click", () => loadPreset(button.dataset.preset));
});

document.getElementById("reset-workflow-btn").addEventListener("click", () => loadPreset("parallel"));
document.getElementById("reset-chat-btn").addEventListener("click", resetChatForm);
document.getElementById("run-workflow-btn").addEventListener("click", runWorkflow);
document.getElementById("stream-workflow-btn").addEventListener("click", streamWorkflow);
document.getElementById("run-chat-btn").addEventListener("click", runChat);
copyResultButton.addEventListener("click", copyLatestResult);

loadPreset("parallel");
resetChatForm();
setTimelineEmpty();

async function runWorkflow() {
    clearTimeline();
    setWorkflowPending("同步执行中");
    workflowResultOutput.textContent = "请求发送中...";
    const payload = buildWorkflowPayload();

    try {
        const response = await fetch("/api/workflow/execute", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });
        const body = await parseJsonSafely(response);
        if (!response.ok) {
            throw new Error(body?.message || "工作流执行失败");
        }

        latestWorkflowResult = body;
        updateWorkflowResult(body);
        renderTimelineFromEvents(body.events || []);
        setWorkflowStatusChip(body.status || "SUCCESS", "success");
    } catch (error) {
        latestWorkflowResult = null;
        workflowResultOutput.textContent = formatError(error);
        workflowStatusValue.textContent = "FAILED";
        workflowNodeCount.textContent = "0";
        workflowEventCount.textContent = "0";
        setWorkflowStatusChip("执行失败", "error");
    }
}

async function streamWorkflow() {
    clearTimeline();
    setWorkflowPending("流式执行中");
    workflowResultOutput.textContent = "正在等待流式事件...";
    const payload = buildWorkflowPayload();

    try {
        const response = await fetch("/api/workflow/stream", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/event-stream"
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok || !response.body) {
            const body = await parseJsonSafely(response);
            throw new Error(body?.message || "无法建立流式连接");
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        let buffer = "";

        while (true) {
            const { done, value } = await reader.read();
            if (done) {
                break;
            }
            buffer += decoder.decode(value, { stream: true });
            const chunks = buffer.split("\n\n");
            buffer = chunks.pop() || "";
            chunks
                .map(parseSseEvent)
                .filter(Boolean)
                .forEach(handleStreamEvent);
        }

        if (buffer.trim()) {
            const finalEvent = parseSseEvent(buffer);
            if (finalEvent) {
                handleStreamEvent(finalEvent);
            }
        }
    } catch (error) {
        appendTimelineItem({
            badge: "WORKFLOW_ERROR",
            title: "流式执行失败",
            meta: formatError(error),
            json: null,
            tone: "error"
        });
        setWorkflowStatusChip("流式失败", "error");
    }
}

async function runChat() {
    setChatPending("发送中");
    chatContentOutput.textContent = "模型思考中...";

    try {
        const response = await fetch("/api/chat/complete", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                conversationId: normalizeOptional(chatConversationId.value),
                systemPrompt: chatSystemPrompt.value,
                promptTemplate: chatPromptTemplate.value,
                userInput: chatUserInput.value,
                templateVariables: {
                    project: chatProjectName.value || "mini-agentflow"
                }
            })
        });
        const body = await parseJsonSafely(response);
        if (!response.ok) {
            throw new Error(body?.message || "Chat 请求失败");
        }

        chatProvider.textContent = body.provider || "-";
        chatMockState.textContent = body.mock ? "true" : "false";
        chatMemorySize.textContent = String(body.memorySize ?? 0);
        chatConversationValue.textContent = body.conversationId || "-";
        chatContentOutput.textContent = body.content || "";
        setChatStatus(body.mock ? "Mock 返回" : "模型返回", "success");
    } catch (error) {
        chatProvider.textContent = "-";
        chatMockState.textContent = "-";
        chatMemorySize.textContent = "0";
        chatConversationValue.textContent = "-";
        chatContentOutput.textContent = formatError(error);
        setChatStatus("请求失败", "error");
    }
}

function buildWorkflowPayload() {
    const workflow = JSON.parse(workflowTextarea.value);
    const payload = {
        engineMode: engineModeSelect.value,
        inputs: {
            input: workflowInput.value
        },
        workflow
    };

    const lockKey = normalizeOptional(lockKeyInput.value);
    if (lockKey) {
        payload.lockKey = lockKey;
        payload.lockType = lockTypeSelect.value;
    }
    const waitTime = normalizeNumber(lockWaitInput.value);
    if (waitTime !== null) {
        payload.lockWaitTimeMs = waitTime;
    }
    const leaseTime = normalizeNumber(lockLeaseInput.value);
    if (leaseTime !== null) {
        payload.lockLeaseTimeMs = leaseTime;
    }
    return payload;
}

function updateWorkflowResult(result) {
    workflowResultOutput.textContent = JSON.stringify(result, null, 2);
    workflowStatusValue.textContent = result.status || "-";
    workflowNodeCount.textContent = String((result.nodeResults || []).length);
    workflowEventCount.textContent = String((result.events || []).length);
    workflowLockState.textContent = normalizeOptional(lockKeyInput.value) ? lockTypeSelect.value : "未启用";
}

function loadPreset(name) {
    const preset = defaultPresets[name];
    if (!preset) {
        return;
    }
    engineModeSelect.value = preset.engineMode;
    workflowInput.value = preset.input;
    lockKeyInput.value = preset.lockKey;
    lockTypeSelect.value = preset.lockType;
    lockWaitInput.value = preset.lockWaitTimeMs;
    lockLeaseInput.value = preset.lockLeaseTimeMs;
    workflowTextarea.value = JSON.stringify(preset.workflow, null, 2);
    workflowResultOutput.textContent = "等待请求...";
    workflowStatusValue.textContent = "-";
    workflowNodeCount.textContent = "0";
    workflowEventCount.textContent = "0";
    workflowLockState.textContent = preset.lockKey ? preset.lockType : "未启用";
    setWorkflowStatusChip("等待执行", "");
    setTimelineEmpty();
}

function resetChatForm() {
    chatConversationId.value = defaultChat.conversationId;
    chatProjectName.value = defaultChat.project;
    chatSystemPrompt.value = defaultChat.systemPrompt;
    chatPromptTemplate.value = defaultChat.promptTemplate;
    chatUserInput.value = defaultChat.userInput;
    chatProvider.textContent = "-";
    chatMockState.textContent = "-";
    chatMemorySize.textContent = "0";
    chatConversationValue.textContent = "-";
    chatContentOutput.textContent = "等待请求...";
    setChatStatus("未发送", "");
}

function setTimelineEmpty() {
    eventTimeline.innerHTML = '<div class="timeline-empty">点击“流式执行”后，这里会实时出现工作流事件。</div>';
}

function clearTimeline() {
    eventTimeline.innerHTML = "";
}

function renderTimelineFromEvents(events) {
    if (!events.length) {
        setTimelineEmpty();
        return;
    }
    clearTimeline();
    events.forEach((event) => appendWorkflowEvent(event));
}

function appendWorkflowEvent(event) {
    appendTimelineItem({
        badge: event.type,
        title: event.message || event.type || "工作流事件",
        meta: buildEventMeta(event),
        json: event.data && Object.keys(event.data).length ? event.data : null,
        tone: mapEventTone(event.type)
    });
}

function appendTimelineItem({ badge, title, meta, json, tone }) {
    const item = document.createElement("article");
    item.className = "timeline-item";
    item.innerHTML = `
        <div class="timeline-badge">${escapeHtml(badge || "EVENT")}</div>
        <div class="timeline-content">
            <div class="timeline-title">${escapeHtml(title || "未命名事件")}</div>
            <div class="timeline-meta">${escapeHtml(meta || "")}</div>
            ${json ? `<pre class="timeline-json">${escapeHtml(JSON.stringify(json, null, 2))}</pre>` : ""}
        </div>
    `;
    if (tone === "error") {
        item.querySelector(".timeline-badge").style.background = "rgba(183, 74, 47, 0.12)";
        item.querySelector(".timeline-badge").style.color = "#b74a2f";
    } else if (tone === "success") {
        item.querySelector(".timeline-badge").style.background = "rgba(29, 122, 72, 0.12)";
        item.querySelector(".timeline-badge").style.color = "#1d7a48";
    } else if (tone === "warm") {
        item.querySelector(".timeline-badge").style.background = "rgba(220, 107, 67, 0.12)";
        item.querySelector(".timeline-badge").style.color = "#dc6b43";
    }
    eventTimeline.prepend(item);
}

function handleStreamEvent(event) {
    if (event.event === "WORKFLOW_RESULT") {
        latestWorkflowResult = event.data;
        updateWorkflowResult(event.data);
        setWorkflowStatusChip(event.data?.status || "完成", "success");
        return;
    }

    if (event.event === "WORKFLOW_ERROR") {
        appendTimelineItem({
            badge: event.event,
            title: "工作流错误",
            meta: event.data?.message || "未知错误",
            json: null,
            tone: "error"
        });
        setWorkflowStatusChip("流式失败", "error");
        return;
    }

    appendWorkflowEvent({
        type: event.event,
        ...event.data
    });
}

function parseSseEvent(chunk) {
    const lines = chunk.split(/\r?\n/);
    let eventName = "message";
    const dataLines = [];

    lines.forEach((line) => {
        if (line.startsWith("event:")) {
            eventName = line.slice(6).trim();
        } else if (line.startsWith("data:")) {
            dataLines.push(line.slice(5).trim());
        }
    });

    if (!dataLines.length) {
        return null;
    }

    const rawData = dataLines.join("\n");
    let parsedData = rawData;
    try {
        parsedData = JSON.parse(rawData);
    } catch (error) {
        parsedData = rawData;
    }

    return {
        event: eventName,
        data: parsedData
    };
}

function buildEventMeta(event) {
    const pieces = [];
    if (event.nodeId) {
        pieces.push(`node=${event.nodeId}`);
    }
    if (event.engineMode) {
        pieces.push(`mode=${event.engineMode}`);
    }
    if (event.workflowStatus) {
        pieces.push(`status=${event.workflowStatus}`);
    }
    if (event.attempt) {
        pieces.push(`attempt=${event.attempt}`);
    }
    if (event.executionId) {
        pieces.push(`executionId=${event.executionId}`);
    }
    return pieces.join(" · ");
}

function mapEventTone(type) {
    if (!type) {
        return "";
    }
    if (String(type).includes("FAILED") || String(type).includes("ERROR")) {
        return "error";
    }
    if (String(type).includes("COMPLETED")) {
        return "success";
    }
    if (String(type).includes("RETRY")) {
        return "warm";
    }
    return "";
}

function setWorkflowPending(label) {
    workflowStatusValue.textContent = "RUNNING";
    workflowNodeCount.textContent = "0";
    workflowEventCount.textContent = "0";
    workflowLockState.textContent = normalizeOptional(lockKeyInput.value) ? lockTypeSelect.value : "未启用";
    setWorkflowStatusChip(label, "pending");
}

function setWorkflowStatusChip(label, tone) {
    workflowStatusChip.textContent = label;
    workflowStatusChip.className = "status-chip";
    if (tone) {
        workflowStatusChip.classList.add(tone);
    }
}

function setChatStatus(label, tone) {
    chatStatusChip.textContent = label;
    chatStatusChip.className = "status-chip";
    if (tone) {
        chatStatusChip.classList.add(tone);
    }
}

function setChatPending(label) {
    setChatStatus(label, "pending");
}

async function copyLatestResult() {
    if (!latestWorkflowResult) {
        workflowResultOutput.textContent = "还没有可复制的结果，请先执行一次工作流。";
        return;
    }
    try {
        await navigator.clipboard.writeText(JSON.stringify(latestWorkflowResult, null, 2));
        setWorkflowStatusChip("结果已复制", "success");
    } catch (error) {
        setWorkflowStatusChip("复制失败", "error");
    }
}

async function parseJsonSafely(response) {
    const text = await response.text();
    if (!text) {
        return null;
    }
    try {
        return JSON.parse(text);
    } catch (error) {
        return { message: text };
    }
}

function normalizeOptional(value) {
    const trimmed = String(value ?? "").trim();
    return trimmed ? trimmed : null;
}

function normalizeNumber(value) {
    if (String(value ?? "").trim() === "") {
        return null;
    }
    return Number(value);
}

function formatError(error) {
    return error instanceof Error ? error.message : String(error);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
}
