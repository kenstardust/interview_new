import axios from 'axios'
import type { Conversation, ConversationDetail } from '@/types'

const BASE_URL = '/ai/v1'  // 通过Vite代理转发到后端8051端口

/**
 * 获取会话列表
 */
export async function getConversations(page = 1, size = 20): Promise<Conversation[]> {
  try {
    const response = await axios.get(`${BASE_URL}/conversations`, {
      params: { page, size }
    })
    return response.data.data
  } catch (error) {
    console.error('获取会话列表失败:', error)
    throw error
  }
}

/**
 * 创建新会话
 */
export async function createConversation(title?: string): Promise<Conversation> {
  try {
    const response = await axios.post(`${BASE_URL}/conversations`, { title })
    return response.data.data
  } catch (error) {
    console.error('创建会话失败:', error)
    throw error
  }
}

/**
 * 获取会话详情（包含历史消息）
 */
export async function getConversationDetail(conversationId: string): Promise<ConversationDetail> {
  try {
    const response = await axios.get(`${BASE_URL}/conversations/${conversationId}`)
    return response.data.data
  } catch (error) {
    console.error('获取会话详情失败:', error)
    throw error
  }
}

/**
 * 删除会话
 */
export async function deleteConversation(conversationId: string): Promise<void> {
  try {
    await axios.delete(`${BASE_URL}/conversations/${conversationId}`)
  } catch (error) {
    console.error('删除会话失败:', error)
    throw error
  }
}