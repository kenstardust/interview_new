<script setup lang="tsx">
import { CoffeeOutlined, FireOutlined, SmileOutlined, UserOutlined } from '@ant-design/icons-vue';
import { Bubble, Prompts, type PromptsProps } from 'ant-design-x-vue';
import { Flex } from 'ant-design-vue';

defineOptions({ name: 'BubbleVariant' });

const items: PromptsProps['items'] = [
  {
    key: '6',
    icon: <CoffeeOutlined style={{ color: '#964B00' }} />,
    description: 'How to rest effectively after long hours of work?',
  },
  {
    key: '7',
    icon: <SmileOutlined style={{ color: '#FAAD14' }} />,
    description: 'What are the secrets to maintaining a positive mindset?',
  },
  {
    key: '8',
    icon: <FireOutlined style={{ color: '#FF4D4F' }} />,
    description: 'How to stay calm under immense pressure?',
  },
];

defineRender(() => {
  return (
    <Flex vertical gap="middle">
      <Bubble variant="filled" avatar={{ icon: <UserOutlined /> }} content="variant: filled" />
      <Bubble variant="outlined" avatar={{ icon: <UserOutlined /> }} content="variant: outlined" />
      <Bubble variant="shadow" avatar={{ icon: <UserOutlined /> }} content="variant: shadow" />
      <Bubble
        variant="borderless"
        avatar={{ icon: <UserOutlined /> }}
        content={<Prompts title="variant: borderless to customize" items={items} vertical />}
      />
    </Flex>
  )
});
</script>
