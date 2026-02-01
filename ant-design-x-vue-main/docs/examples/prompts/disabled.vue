<script setup lang="tsx">
import { CheckCircleOutlined, CoffeeOutlined } from '@ant-design/icons-vue';
import { Prompts, type PromptsProps } from 'ant-design-x-vue';

defineOptions({ name: 'AXPromptsDisabled' });

const items: PromptsProps['items'] = [
  {
    key: '5',
    icon: <CheckCircleOutlined style={{ color: '#52C41A' }} />,
    label: 'Task Completion Secrets',
    description: 'What are some tricks for getting tasks done?',
    disabled: true,
  },
  {
    key: '6',
    icon: <CoffeeOutlined style={{ color: '#964B00' }} />,
    label: 'Time for a Coffee Break',
    description: 'How to rest effectively after long hours of work?',
  },
];

defineRender(() => {
  return (
    <Prompts title="☕️ It's time to relax!" items={items} />
  )
});
</script>
