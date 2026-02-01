<script setup lang="tsx">
import { UserOutlined } from '@ant-design/icons-vue';
import { Bubble } from 'ant-design-x-vue';
import { Button, Flex } from 'ant-design-vue';
import { ref } from 'vue';

defineOptions({ name: 'BubbleTyping' });

const text = ref('Ant Design X love you! ');

const repeat = ref(1);

defineRender(() => {
  return (
    <Flex vertical gap="small">
      <Bubble
        content={text.value.repeat(repeat.value)}
        typing={{ step: 2, interval: 50 }}
        avatar={{ icon: <UserOutlined /> }}
      />
      <Bubble
        content={text.value.repeat(repeat.value)}
        typing={{ step: 2, interval: 50, suffix: <>💗</> }}
        avatar={{ icon: <UserOutlined /> }}
      />

      <Button
        style={{ alignSelf: 'flex-end' }}
        onClick={() => {
          repeat.value = (repeat.value < 5 ? repeat.value + 1 : 1);
        }}
      >
        Repeat {repeat.value} Times
      </Button>
    </Flex>
  )
});
</script>
