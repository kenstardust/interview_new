<script setup lang="tsx">
import { Select } from 'ant-design-vue';
import { Suggestion } from 'ant-design-x-vue';
import { ref } from 'vue';

defineOptions({ name: 'AXSuggestionTrigger' });

const uuid = ref(0);
const tags = ref<string[]>([]);
const value = ref('');

defineRender(() => {
  return (
    <Suggestion<string>
      items={(info) => [{ label: `Trigger by '${info}'`, value: String(info) }]}
      onSelect={(info) => {
        uuid.value += 1;
        tags.value = [...tags.value, `Cell_${uuid.value}`];
        value.value = value.value.replace(info, '');
      }}
      children={({ onTrigger, onKeyDown }) => {
        return (
          <Select
            value={tags.value}
            style={{ width: '100%' }}
            mode="tags"
            open={false}
            searchValue={value.value}
            onChange={(nextTags) => {
              if ((nextTags as string[]).length < tags.value.length) {
                tags.value = nextTags as string[];
              }
            }}
            onSearch={(nextVal) => {
              value.value = nextVal;
            }}
            onKeydown={(e) => {
              if (e.key === '/' || e.key === '#') {
                onTrigger(e.key);
              }
              onKeyDown(e);
            }}
            placeholder="可任意输入 / 与 # 多次获取建议"
          />
        );
      }}
    />
  )
});
</script>
