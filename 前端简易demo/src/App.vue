<template>
  <n-config-provider>
    <n-message-provider>
      <SeckillPage />
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { NConfigProvider, NMessageProvider } from 'naive-ui'
import SeckillPage from './views/SeckillPage.vue'
</script>
