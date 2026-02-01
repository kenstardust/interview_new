<script setup lang="tsx">
import { UserOutlined } from '@ant-design/icons-vue';
import { Flex, Switch } from 'ant-design-vue';
import { Bubble } from 'ant-design-x-vue';
import { ref } from 'vue';

defineOptions({ name: 'BubbleLoading' });

const loading = ref(true);

defineRender(() => {
  return (
    <Flex gap="large" vertical>
      <Bubble loading={loading.value} content="hello world !" avatar={{ icon: <UserOutlined /> }} />
      <Flex gap="large" wrap="wrap">
        Loading state:
        <Switch v-model:checked={loading.value} />
      </Flex>
    </Flex>
  )
});
</script>
