import fs from 'node:fs'
import { defineLoader } from 'vitepress'

export interface Slot { name: string, icon: string, href: string }
export interface Recipe { key: string, shapeless: boolean, slots: (Slot | null)[], result: Slot, count: number, ingredients: string[] }

declare const data: Record<string, Recipe>
export { data }

// Tags in recipes. Keep the icons in step with TAGS in tools/gen_wiki_icons.py.
const TAGS: Record<string, Slot> = {
	'#minecraft:planks': { name: 'Any Planks', icon: 'oak_planks', href: 'https://minecraft.wiki/w/Planks' },
	'#minecraft:copper_chests': { name: 'Any Copper Chest', icon: 'copper_chest', href: 'https://minecraft.wiki/w/Copper_Chest' },
	'#c:chests/wooden': { name: 'Any Wooden Chest', icon: 'chest', href: 'https://minecraft.wiki/w/Chest' },
}

const titleCase = (s: string) => s.split('_').map(w => w[0].toUpperCase() + w.slice(1)).join(' ')

export default defineLoader({
	watch: [
		'../../../src/main/resources/data/*/recipe/*.json',
		'../../../automatisation/src/main/resources/data/*/recipe/*.json',
		'../../../src/main/resources/assets/*/lang/en_us.json',
		'../../../automatisation/src/main/resources/assets/*/lang/en_us.json',
	],
	load(files: string[]) {
		const read = (f: string) => JSON.parse(fs.readFileSync(f, 'utf8'))
		const lang: Record<string, string> = Object.assign({}, ...files.filter(f => f.endsWith('en_us.json')).map(read))

		const slot = (id: string): Slot => {
			if (id.startsWith('#')) {
				if (!TAGS[id]) throw new Error(`No wiki label for recipe tag ${id}`)
				return TAGS[id]
			}
			const [ns, name] = id.split(':')
			if (ns === 'minecraft') {
				const title = titleCase(name)
				return { name: title, icon: name, href: `https://minecraft.wiki/w/${title.replaceAll(' ', '_')}` }
			}
			const label = lang[`item.${ns}.${name}`] ?? lang[`block.${ns}.${name}`]
			if (!label) throw new Error(`No English name for ${id}`)
			return { name: label, icon: name, href: `/items/${name}` }
		}

		const recipes: Record<string, Recipe> = {}
		for (const file of files.filter(f => f.includes('/recipe/'))) {
			const r = read(file)
			const key = file.split('/').pop()!.replace('.json', '')
			const slots: (Slot | null)[] = Array(9).fill(null)
			let ingredients: string[]
			if (r.pattern) {
				ingredients = Object.values(r.key)
				r.pattern.forEach((row: string, y: number) => [...row].forEach((c, x) => {
					if (c !== ' ') slots[y * 3 + x] = slot(r.key[c])
				}))
			} else {
				ingredients = r.ingredients
				r.ingredients.forEach((id: string, i: number) => { slots[i] = slot(id) })
			}
			recipes[key] = {
				key, slots, ingredients, shapeless: !r.pattern,
				result: slot(r.result.id), count: r.result.count ?? 1,
			}
		}
		return recipes
	},
})
