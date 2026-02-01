<script setup lang="tsx">
import { MoreOutlined } from '@ant-design/icons-vue';
import { Button, Card, Radio, type ConfigProviderProps } from 'ant-design-vue';
import { ThoughtChain, type ThoughtChainProps } from 'ant-design-x-vue';
import { ref } from 'vue';

defineOptions({ name: 'AXThoughtChainSize' });

type SizeType = ConfigProviderProps['componentSize'];

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

// default size is 'middle'
const size = ref<SizeType>('middle');

defineRender(() => {
  return (
    <Card style={{ width: '500px' }}>
      <Radio.Group
        value={size}
        onChange={(e) => {
          size.value = e.target.value;
        }}
        style={{ marginBottom: '16px' }}
      >
        <Radio.Button value="large">Large</Radio.Button>
        <Radio.Button value="middle">Default</Radio.Button>
        <Radio.Button value="small">Small</Radio.Button>
      </Radio.Group>
      <ThoughtChain items={items} size={size.value} />
    </Card>
  )
});
</script>
