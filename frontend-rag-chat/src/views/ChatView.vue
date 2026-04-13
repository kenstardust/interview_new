<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Conversations, Bubble, Sender } from 'ant-design-x-vue'
import { UserOutlined, PaperClipOutlined } from '@ant-design/icons-vue'
import { getConversations, createConversation, getConversationDetail } from '@/api/chat'
import { uploadFile, getFileStatus } from '@/api/file'
import { ssePost } from '@/api/sse-client'
import type { Conversation, Message, FileInfo } from '@/types'

// 会话列表
const conversations = ref<Conversation[]>([])
const activeConversationId = ref<string>()

// 消息列表
const messages = ref<Message[]>([])

// 文件上传
const files = ref<FileInfo[]>([])
const fileStatuses = ref<Map<number, string>>(new Map())
const fileInputRef = ref<HTMLInputElement>()

// 发送状态
const loading = ref(false)

// 角色配置（消息样式）
const roles = {
  assistant: {
    placement: 'start',
    avatar: { icon: UserOutlined, style: { background: '#fde3cf' } },
    typing: { step: 5, interval: 20 },  // 打字机效果
  },
  user: {
    placement: 'end',
    avatar: { icon: UserOutlined, style: { background: '#87d068' } },
  }
}

// 初始化：加载会话列表
onMounted(async () => {
  try {
    conversations.value = await getConversations()
    if (conversations.value.length > 0) {
      activeConversationId.value = conversations.value[0].conversationId
      // 加载第一个会话的历史消息
      await loadConversationMessages(activeConversationId.value)
    } else {
      // 创建默认会话
      const newConv = await createConversation('工业软件问答')
      conversations.value.push(newConv)
      activeConversationId.value = newConv.conversationId
    }
  } catch (error) {
    console.error('初始化失败:', error)
  }
})

// 加载会话历史消息
async function loadConversationMessages(conversationId: string) {
  try {
    const detail = await getConversationDetail(conversationId)
    messages.value = detail.messages || []
  } catch (error) {
    console.error('加载历史消息失败:', error)
  }
}

// 切换会话
async function handleConversationChange(key: string) {
  activeConversationId.value = key
  await loadConversationMessages(key)
}

// 发送消息（流式）
async function handleSend(content: string) {
  if (!activeConversationId.value || !content.trim()) return

  // 添加用户消息
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: content.trim()
  })

  loading.value = true

  // 获取已上传文件ID（只使用处理完成的文件）
  const fileIds = files.value
    .filter(f => fileStatuses.get(f.fileId) === 'COMPLETED')
    .map(f => f.fileId)

  // 添加空的assistant消息（用于流式更新）
  const assistantMessageId = Date.now() + 1
  messages.value.push({
    id: assistantMessageId,
    role: 'assistant',
    content: ''
  })

  let assistantContent = ''

  try {
    // SSE流式请求
    await ssePost(`/ai/v1/conversations/${activeConversationId.value}/messages/stream`, {
      content: content.trim(),
      fileIds: fileIds.length > 0 ? fileIds : undefined
    }, {
      onMessage: (event, data) => {
        if (event === 'message') {
          assistantContent += data
          // 更新最后一条消息的content
          const lastMessage = messages.value.find(m => m.id === assistantMessageId)
          if (lastMessage) {
            lastMessage.content = assistantContent
          }
        } else if (event === 'complete') {
          // 流式传输完成
          loading.value = false
          files.value = []  // 清空已发送文件
        } else if (event === 'error') {
          // 错误事件
          console.error('SSE Error:', data)
          loading.value = false
          const lastMessage = messages.value.find(m => m.id === assistantMessageId)
          if (lastMessage) {
            lastMessage.content = `错误：${data}`
          }
        }
      },
      onComplete: () => {
        loading.value = false
      },
      onError: (error) => {
        console.error('SSE Error:', error)
        loading.value = false
        const lastMessage = messages.value.find(m => m.id === assistantMessageId)
        if (lastMessage) {
          lastMessage.content = `请求失败：${error.message}`
        }
      }
    })
  } catch (error) {
    console.error('发送消息失败:', error)
    loading.value = false
  }
}

// 上传文件按钮点击
function handleUploadClick() {
  fileInputRef.value?.click()
}

// 文件选择处理
async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) return

  try {
    // 上传文件
    const result = await uploadFile(file)

    // 添加到文件列表
    files.value.push({
      fileId: result.fileId,
      name: file.name,
      status: result.taskstatus
    })

    // 初始化状态
    fileStatuses.set(result.fileId, result.taskstatus)

    // 轮询文件状态（每2秒查询一次）
    pollFileStatus(result.fileId)

  } catch (error) {
    console.error('文件上传失败:', error)
  } finally {
    // 清空input，允许重复选择同一文件
    input.value = ''
  }
}

// 轮询文件状态
async function pollFileStatus(fileId: number) {
  const intervalId = setInterval(async () => {
    try {
      const status = await getFileStatus(fileId)
      fileStatuses.set(fileId, status.taskstatus)

      // 更新files数组中的状态
      const file = files.value.find(f => f.fileId === fileId)
      if (file) {
        file.status = status.taskstatus
      }

      // 处理完成或失败时停止轮询
      if (status.taskstatus === 'COMPLETED' || status.taskstatus === 'FAILED') {
        clearInterval(intervalId)

        if (status.taskstatus === 'FAILED') {
          console.error(`文件处理失败: ${status.analyzeError}`)
        }
      }
    } catch (error) {
      console.error('查询文件状态失败:', error)
      clearInterval(intervalId)
    }
  }, 2000)  // 每2秒查询一次
}

// 创建新会话
async function handleCreateConversation() {
  try {
    const newConv = await createConversation('新会话')
    conversations.value.unshift(newConv)  // 添加到列表顶部
    activeConversationId.value = newConv.conversationId
    messages.value = []  // 清空消息列表
  } catch (error) {
    console.error('创建会话失败:', error)
  }
}

// 格式化会话列表项（适配Conversations组件）
function formatConversationItems() {
  return conversations.value.map(c => ({
    key: c.conversationId,
    label: c.title || '未命名会话',
    timestamp: new Date(c.updatedAt).toLocaleString('zh-CN')
  }))
}
</script>

<template>
  <div class="chat-container">
    <!-- 左侧：会话列表 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <a-button type="primary" block @click="handleCreateConversation">
          新建会话
        </a-button>
      </div>

      <div class="sidebar-content">
        <Conversations
          :items="formatConversationItems()"
          :activeKey="activeConversationId"
          @activeChange="handleConversationChange"
        />
      </div>
    </aside>

    <!-- 右侧：聊天区域 -->
    <main class="chat-main">
      <!-- 标题 -->
      <header class="chat-header">
        <h2 style="margin: 0;">工业软件智能问答系统</h2>
      </header>

      <!-- 消息列表 -->
      <section class="messages-container">
        <Bubble.List
          :roles="roles"
          :items="messages.map(m => ({
            key: m.id,
            role: m.role,
            content: m.content,
            loading: m.role === 'assistant' && m.content === '' && loading
          }))"
        />
      </section>

      <!-- 文件状态提示 -->
      <section v-if="files.length > 0" class="files-status">
        <div class="files-status-content">
          <span>已上传文件：</span>
          <div v-for="file in files" :key="file.fileId" class="file-item">
            <a-tag :color="fileStatuses.get(file.fileId) === 'COMPLETED' ? 'success' : fileStatuses.get(file.fileId) === 'FAILED' ? 'error' : 'processing'">
              {{ file.name }}
            </a-tag>
            <span class="file-status-text">
              {{ fileStatuses.get(file.fileId) === 'PENDING' ? '等待处理' : fileStatuses.get(file.fileId) === 'PROCESSING' ? '处理中...' : fileStatuses.get(file.fileId) === 'COMPLETED' ? '就绪' : '失败' }}
            </span>
          </div>
        </div>
      </section>

      <!-- 输入区域 -->
      <footer class="input-area">
        <Sender
          :loading="loading"
          @submit="handleSend"
          submit-type="enter"
        >
          <template #prefix>
            <a-button @click="handleUploadClick" :disabled="loading">
              <PaperClipOutlined />
              上传文件
            </a-button>
          </template>
        </Sender>

        <!-- 隐藏的文件上传input -->
        <input
          ref="fileInputRef"
          type="file"
          @change="handleFileSelect"
          style="display: none;"
          accept=".pdf,.doc,.docx,.txt"
        />
      </footer>
    </main>
  </div>
</template>

<style scoped>
/* 主容器：响应式布局 */
.chat-container {
  display: flex;
  height: 100vh;
  min-height: 600px;  /* 最小高度防止布局崩溃 */
  background: #f5f5f5;
  overflow: hidden;  /* 防止超出屏幕 */
}

/* 左侧会话列表：响应式宽度 */
.sidebar {
  width: 20%;  /* 默认宽度20% */
  min-width: 200px;  /* 最小宽度200px */
  max-width: 280px;  /* 最大宽度280px */
  background: #fff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;  /* 宽度变化时平滑过渡 */
}

/* 小屏幕时左侧宽度固定 */
@media (max-width: 768px) {
  .sidebar {
    width: 200px;
    min-width: 200px;
    max-width: 200px;
  }
}

.sidebar-header {
  padding: 16px;
  flex-shrink: 0;  /* 固定高度不压缩 */
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

/* 右侧聊天区域：自适应宽度 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  min-width: 0;  /* 允许flex子元素缩小 */
  overflow: hidden;
}

/* 标题区域：固定高度 */
.chat-header {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
  flex-shrink: 0;  /* 固定高度不压缩 */
}

/* 消息列表：自适应高度 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  min-height: 300px;  /* 最小高度保证可读性 */
}

/* 文件状态提示：动态显示 */
.files-status {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  flex-shrink: 0;  /* 固定高度不压缩 */
}

.files-status-content {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;  /* 文件过多时自动换行 */
}

.file-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.file-status-text {
  font-size: 12px;
  color: #999;
}

/* 输入区域：固定高度 */
.input-area {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  flex-shrink: 0;  /* 固定高度不压缩 */
  min-height: 100px;  /* 保证输入区域可用 */
}

/* 消息列表滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
}

::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}

/* 响应式调整：超大屏幕 */
@media (min-width: 1920px) {
  .sidebar {
    width: 25%;
    max-width: 320px;
  }

  .chat-main {
    max-width: 1400px;  /* 超大屏幕限制聊天区域最大宽度 */
  }
}
</style>