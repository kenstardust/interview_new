import axios from 'axios'
import type { FileUploadResult, FileStatus } from '@/types'

const BASE_URL = '/ai/v1'  // 通过Vite代理转发到后端8051端口

/**
 * 上传文件
 *
 * @param file 文件对象（PDF、Word、TXT等）
 * @returns 文件上传结果（包含fileId和初始状态）
 */
export async function uploadFile(file: File): Promise<FileUploadResult> {
  try {
    const formData = new FormData()
    formData.append('file', file)

    const response = await axios.post(`${BASE_URL}/files/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    return response.data.data
  } catch (error) {
    console.error('文件上传失败:', error)
    throw error
  }
}

/**
 * 查询文件处理状态
 *
 * @param fileId 文件ID
 * @returns 文件状态（PENDING, PROCESSING, COMPLETED, FAILED）
 */
export async function getFileStatus(fileId: number): Promise<FileStatus> {
  try {
    const response = await axios.get(`${BASE_URL}/files/${fileId}/status`)
    return response.data.data
  } catch (error) {
    console.error('查询文件状态失败:', error)
    throw error
  }
}

/**
 * 删除文件
 *
 * @param fileId 文件ID
 */
export async function deleteFile(fileId: number): Promise<void> {
  try {
    await axios.delete(`${BASE_URL}/files/${fileId}`)
  } catch (error) {
    console.error('删除文件失败:', error)
    throw error
  }
}