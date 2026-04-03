// 会话类型定义
export interface Conversation {
  conversationId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  status: number;  // 1=ACTIVE, 2=ARCHIVED, 3=DELETED
}

// 消息类型定义
export interface Message {
  id: number;
  role: 'user' | 'assistant' | 'system';
  content: string;
  createdAt?: string;
  tokenCount?: number;
  metadata?: string;  // JSON字符串
}

// 文件上传结果
export interface FileUploadResult {
  fileId: number;
  storageKey: string;
  taskstatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
}

// 文件状态查询结果
export interface FileStatus {
  fileId: number;
  taskstatus: string;
  analyzeError?: string;
  chunkCount?: number;
}

// 会话详情（包含消息列表）
export interface ConversationDetail {
  conversationId: string;
  title: string;
  messages: Message[];
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

// 文件信息（用于前端展示）
export interface FileInfo {
  fileId: number;
  name: string;
  status: string;
}

// API响应包装类型
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}