<script setup lang="ts">
import { withBase } from 'vitepress'

defineProps<{ id: string }>()
</script>

<template>
	<img class="icon" :src="withBase(`/icons/${id}.png`)" alt="" width="20" height="20" loading="lazy">
</template>
