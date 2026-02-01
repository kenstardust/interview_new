<script setup lang="tsx">
import { MoreOutlined } from '@ant-design/icons-vue';
import { Button, Card } from 'ant-design-vue';
import { ThoughtChain, type ThoughtChainProps } from 'ant-design-x-vue';

defineOptions({ name: 'AXThoughtChainBasic' });

const items: ThoughtChainProps['items'] = [
  {
    title: 'Thought Chain Item Title',
    description: 'description',
    extra: <Button type="text" icon={<MoreOutlined />} />,
  },
  {
    title: 'Thought Chain Item Title',
    description: 'description',
    extra: <Button type="text" icon={<MoreOutlined />} />,
  },
  {
    title: 'Thought Chain Item Title',
    description: 'description',
    extra: <Button type="text" icon={<MoreOutlined />} />,
  },

  {
    title: 'Thought Chain Item Title',
    description: 'description',
    extra: <Button type="text" icon={<MoreOutlined />} />,
  },
];

defineRender(() => {
  return (
    <Card style={{ width: '500px' }}>
      <ThoughtChain items={items} />
    </Card>
  )
});
</script>
