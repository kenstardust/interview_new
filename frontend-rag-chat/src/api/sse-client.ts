// SSE客户端：用于处理流式响应
export interface SSEOptions {
  onMessage?: (event: string, data: string) => void;
  onComplete?: () => void;
  onError?: (error: Error) => void;
}

/**
 * SSE POST请求
 * 用于流式传输AI回答
 *
 * @param url API端点（如：/ai/v1/conversations/xxx/messages/stream）
 * @param body 请求体（如：{ content: "问题", fileIds: [1, 2] }）
 * @param options SSE回调选项
 */
export async function ssePost(url: string, body: any, options: SSEOptions): Promise<void> {
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (!reader) {
      throw new Error('无法获取响应流');
    }

    let buffer = '';  // 用于处理跨chunk的不完整SSE事件

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        // 解码当前chunk并添加到buffer
        buffer += decoder.decode(value, { stream: true });

        // 按SSE格式解析（每个事件以\n\n分隔）
        const events = buffer.split('\n\n');

        // 处理所有完整的事件（最后一个可能不完整）
        for (let i = 0; i < events.length - 1; i++) {
          const eventBlock = events[i];
          parseSSEEvent(eventBlock, options);
        }

        // 保留最后一个可能不完整的事件
        buffer = events[events.length - 1];
      }

      // 处理buffer中剩余的内容
      if (buffer.trim()) {
        parseSSEEvent(buffer, options);
      }

      options.onComplete?.();
    } catch (error) {
      options.onError?.(error as Error);
    }
  } catch (error) {
    options.onError?.(error as Error);
  }
}

/**
 * 解析单个SSE事件块
 * 格式：event: xxx\ndata: yyy
 */
function parseSSEEvent(eventBlock: string, options: SSEOptions) {
  const lines = eventBlock.split('\n');
  let eventType = '';
  let eventData = '';

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventType = line.substring(6).trim();
    } else if (line.startsWith('data:')) {
      eventData = line.substring(5).trim();
    }
  }

  // 触发回调
  if (eventType && eventData) {
    options.onMessage?.(eventType, eventData);
  } else if (eventData) {
    // 有些SSE实现不发送event字段，默认为message事件
    options.onMessage?.('message', eventData);
  }
}