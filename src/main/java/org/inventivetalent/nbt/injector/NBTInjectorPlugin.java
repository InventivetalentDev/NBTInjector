package org.inventivetalent.nbt.injector;

import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.apihelper.APIManager;

public class NBTInjectorPlugin extends JavaPlugin {

	@Override
	public void onLoad() {
		APIManager.registerAPI(new NBTInjector(), this);
	}

	@Override
	public void onEnable() {
	}
}
