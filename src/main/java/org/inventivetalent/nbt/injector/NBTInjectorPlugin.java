package org.inventivetalent.nbt.injector;

import org.bukkit.plugin.java.JavaPlugin;

public class NBTInjectorPlugin extends JavaPlugin {

	@Override
	public void onLoad() {
		NBTInjector.inject();
	}

	@Override
	public void onEnable() {
	}
}
