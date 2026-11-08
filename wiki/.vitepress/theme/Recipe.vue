<script setup lang="ts">
import { computed } from 'vue'
import { withBase } from 'vitepress'
import { data, type Recipe, type Slot } from './recipes.data.mts'

// id: one recipe, by its file name. uses: every recipe with this ingredient. all: every recipe.
const props = defineProps<{ id?: string, uses?: string, all?: boolean }>()

const shown = computed<Recipe[]>(() => {
	const every = Object.values(data).sort((a, b) => a.result.name.localeCompare(b.result.name))
	if (props.all) return every
	if (props.uses) {
		const used = every.filter(r => r.ingredients.some(i => i.endsWith(`:${props.uses}`)))
		if (!used.length) throw new Error(`No recipe uses ${props.uses}`)
		return used
	}
	if (!props.id || !data[props.id]) throw new Error(`Unknown recipe ${props.id}`)
	return [data[props.id]]
})

const href = (s: Slot) => s.href.startsWith('http') ? s.href : withBase(s.href)
</script>

<template>
	<div class="recipes">
		<figure v-for="r in shown" :key="r.key" class="recipe">
			<div class="recipe-grid">
				<component :is="s ? 'a' : 'span'" v-for="(s, i) in r.slots" :key="i" class="slot" :href="s ? href(s) : undefined" :title="s?.name">
					<img v-if="s" :src="withBase(`/icons/${s.icon}.png`)" :alt="s.name" width="32" height="32" loading="lazy">
				</component>
			</div>
			<span class="recipe-arrow" aria-hidden="true"></span>
			<a class="slot slot-result" :href="href(r.result)" :title="r.result.name">
				<img :src="withBase(`/icons/${r.result.icon}.png`)" :alt="r.result.name" width="32" height="32" loading="lazy">
				<b v-if="r.count > 1">{{ r.count }}</b>
			</a>
			<figcaption>{{ r.result.name }}<template v-if="r.shapeless"> (shapeless)</template></figcaption>
		</figure>
	</div>
</template>
