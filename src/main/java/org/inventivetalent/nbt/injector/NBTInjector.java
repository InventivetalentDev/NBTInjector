package org.inventivetalent.nbt.injector;

import javassist.ClassPool;
import org.bukkit.plugin.Plugin;
import org.inventivetalent.apihelper.API;
import org.inventivetalent.nbt.CompoundTag;
import org.inventivetalent.reflection.minecraft.Minecraft;
import org.inventivetalent.reflection.resolver.minecraft.NMSClassResolver;
import org.inventivetalent.reflection.resolver.minecraft.OBCClassResolver;
import org.inventivetalent.reflection.util.AccessUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

public class NBTInjector implements API {

	static Logger           logger           = Logger.getLogger("NBTInjector");
	static NMSClassResolver nmsClassResolver = new NMSClassResolver();
	static OBCClassResolver obcClassResolver = new OBCClassResolver();

	public static void inject() {
		try {
			ClassPool classPool = ClassPool.getDefault();
			logger.info("Injecting Entity classes...");
			for (Map.Entry<String, Class<?>> entry : new HashSet<>(Entity.getCMap().entrySet())) {
				try {
					if (INBTWrapper.class.isAssignableFrom(entry.getValue())) { continue; }//Already injected
					int entityId = Entity.getFMap().get(entry.getValue());

					Class wrapped = ClassGenerator.wrapEntity(classPool, entry.getValue(), "__extraData");
					Entity.getCMap().put(entry.getKey(), wrapped);
					Entity.getDMap().put(wrapped, entry.getKey());

					Entity.getEMap().put(entityId, wrapped);
					Entity.getFMap().put(wrapped, entityId);
				} catch (Exception e) {
					throw new RuntimeException("Exception while injecting " + entry.getKey(), e);
				}
			}

			logger.info("Injecting Tile Entity classes...");
			for (Map.Entry<String, Class<?>> entry : new HashSet<>(TileEntity.getFMap().entrySet())) {
				try {
					if (INBTWrapper.class.isAssignableFrom(entry.getValue())) { continue; }//Already injected

					Class wrapped = ClassGenerator.wrapTileEntity(classPool, entry.getValue(), "__extraData");
					TileEntity.getFMap().put(entry.getKey(), wrapped);
					TileEntity.getGMap().put(wrapped, entry.getKey());
				} catch (Exception e) {
					throw new RuntimeException("Exception while injecting " + entry.getKey(), e);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static CompoundTag getNbtData(Object object) {
		if (object instanceof INBTWrapper) {
			return ((INBTWrapper) object).getNbtData();
		}
		return null;
	}

	public static CompoundTag getNbtData(org.bukkit.entity.Entity entity) {
		if (entity == null) { return null; }
		try {
			return getNbtData(Minecraft.getHandle(entity));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static CompoundTag getNbtData(org.bukkit.block.BlockState blockState) {
		if (blockState == null) { return null; }
		try {
			Object tileEntity = obcClassResolver.resolve("block.CraftBlockState").getMethod("getTileEntity").invoke(blockState);
			return getNbtData(tileEntity);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void load() {
		inject();
	}

	@Override
	public void init(Plugin plugin) {
	}

	@Override
	public void disable(Plugin plugin) {
	}

	static class Entity {
		static Map<String, Class<?>> getCMap() throws ReflectiveOperationException {
			return (Map<String, Class<?>>) AccessUtil.setAccessible(nmsClassResolver.resolve("EntityTypes").getDeclaredField("c")).get(null);
		}

		static Map<Class<?>, String> getDMap() throws ReflectiveOperationException {
			return (Map<Class<?>, String>) AccessUtil.setAccessible(nmsClassResolver.resolve("EntityTypes").getDeclaredField("d")).get(null);
		}

		static Map<Integer, Class<?>> getEMap() throws ReflectiveOperationException {
			return (Map<Integer, Class<?>>) AccessUtil.setAccessible(nmsClassResolver.resolve("EntityTypes").getDeclaredField("e")).get(null);
		}

		static Map<Class<?>, Integer> getFMap() throws ReflectiveOperationException {
			return (Map<Class<?>, Integer>) AccessUtil.setAccessible(nmsClassResolver.resolve("EntityTypes").getDeclaredField("f")).get(null);
		}
	}

	static class TileEntity {
		static Map<String, Class<?>> getFMap() throws ReflectiveOperationException {
			return (Map<String, Class<?>>) AccessUtil.setAccessible(nmsClassResolver.resolve("TileEntity").getDeclaredField("f")).get(null);
		}

		static Map<Class<?>, String> getGMap() throws ReflectiveOperationException {
			return (Map<Class<?>, String>) AccessUtil.setAccessible(nmsClassResolver.resolve("TileEntity").getDeclaredField("g")).get(null);
		}
	}
}
