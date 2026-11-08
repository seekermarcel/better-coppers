import { h } from 'vue'
import DefaultTheme from 'vitepress/theme'
import type { Theme } from 'vitepress'
import '@fontsource-variable/bitter'
import '@fontsource-variable/source-sans-3'
import './custom.css'
import Icon from './Icon.vue'
import Infobox from './Infobox.vue'
import Recipe from './Recipe.vue'

export default {
	extends: DefaultTheme,
	Layout: () => h(DefaultTheme.Layout, null, { 'doc-before': () => h(Infobox) }),
	enhanceApp({ app }) {
		app.component('Icon', Icon)
		app.component('Recipe', Recipe)
	},
} satisfies Theme
