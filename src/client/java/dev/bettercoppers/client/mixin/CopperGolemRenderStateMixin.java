package dev.bettercoppers.client.mixin;

import dev.bettercoppers.client.render.GolemRenderExtras;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CopperGolemRenderState.class)
public abstract class CopperGolemRenderStateMixin implements GolemRenderExtras {
	@Unique
	private @Nullable ArmorTrim bettercoppers$trim;
	@Unique
	private int bettercoppers$eyes;
	@Unique
	private boolean bettercoppers$excited;

	@Override
	public @Nullable ArmorTrim bettercoppers$getTrim() {
		return this.bettercoppers$trim;
	}

	@Override
	public void bettercoppers$setTrim(@Nullable ArmorTrim trim) {
		this.bettercoppers$trim = trim;
	}

	@Override
	public int bettercoppers$getEyes() {
		return this.bettercoppers$eyes;
	}

	@Override
	public void bettercoppers$setEyes(int eyes) {
		this.bettercoppers$eyes = eyes;
	}

	@Override
	public boolean bettercoppers$isExcited() {
		return this.bettercoppers$excited;
	}

	@Override
	public void bettercoppers$setExcited(boolean excited) {
		this.bettercoppers$excited = excited;
	}
}
