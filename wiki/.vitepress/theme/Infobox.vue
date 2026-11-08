<script setup lang="ts">
import { useData, withBase } from 'vitepress'

// Filled from the page's frontmatter: infobox: { icon, image, caption, rows: { Label: value } }
const { frontmatter, page } = useData()
</script>

<template>
	<aside v-if="frontmatter.infobox" class="infobox">
		<h2 class="infobox-title">{{ frontmatter.title ?? page.title }}</h2>
		<figure class="infobox-picture">
			<img v-if="frontmatter.infobox.image" :src="withBase(frontmatter.infobox.image)" :alt="frontmatter.infobox.caption ?? ''">
			<span v-else class="slot slot-large"><img :src="withBase(`/icons/${frontmatter.infobox.icon}.png`)" alt="" width="64" height="64"></span>
			<figcaption v-if="frontmatter.infobox.caption">{{ frontmatter.infobox.caption }}</figcaption>
		</figure>
		<dl>
			<template v-for="(value, label) in frontmatter.infobox.rows" :key="label">
				<dt>{{ label }}</dt>
				<dd>{{ value }}</dd>
			</template>
		</dl>
	</aside>
</template>
