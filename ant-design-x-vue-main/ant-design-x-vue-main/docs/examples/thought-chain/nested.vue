<script setup lang="tsx">
import { MoreOutlined } from '@ant-design/icons-vue';
import { Button, Card } from 'ant-design-vue';
import { ThoughtChain, type ThoughtChainProps } from 'ant-design-x-vue';

defineOptions({ name: 'AXThoughtChainNested' });

const items: ThoughtChainProps['items'] = [
  {
    title: '1 - Thought Chain Item',
    description: 'description',
    extra: <Button type="text" icon={<MoreOutlined />} />,
    footer: <Button>1 - Thought Chain Item Footer</Button>,
    content: (
      <ThoughtChain
        items={[
          {
            title: '1-1 Thought Chain Item',
            description: 'description',
            extra: <Button type="text" icon={<MoreOutlined />} />,
          },
          {
            title: '1-2 Thought Chain Item',
            description: 'description',
            extra: <Button type="text" icon={<MoreOutlined />} />,
          },
        ]}
      />
    ),
  },
  {
    title: '2 - Thought Chain Item',
    description: 'description',
    extra: <Button type="text" icon={<MoreOutlined />} />,
    footer: <Button>2 - Thought Chain Item Footer</Button>,
    content: (
      <ThoughtChain
        items={[
          {
            title: '2-1 Thought Chain Item',
            description: 'description',
            extra: <Button type="text" icon={<MoreOutlined />} />,
          },
          {
            title: '2-2 Thought Chain Item',
            description: 'description',
            extra: <Button type="text" icon={<MoreOutlined />} />,
          },
        ]}
      />
    ),
  },
];

defineRender(() => {
  return (
    <Card style={{ width: '500px' }}>
      <ThoughtChain items={items} collapsible />
    </Card>
  )
});
</script>
