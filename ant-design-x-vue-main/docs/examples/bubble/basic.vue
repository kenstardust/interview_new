<script setup lang="tsx">
import { Bubble } from 'ant-design-x-vue';

defineOptions({ name: 'AXBubbleBasic' });

defineRender(() => {
  return (
    <Bubble content="hello world !" />
  )
})
</script>
