package org.inventivetalent.nbt.injector;

import javassist.*;
import org.inventivetalent.reflection.minecraft.Minecraft;

import java.io.IOException;
import java.lang.reflect.Constructor;

public class ClassGenerator {

	public static Class wrapNbtClass(ClassPool classPool, Class<?> originalClass, String writeMethod, String readMethod, String extraDataKey) throws ReflectiveOperationException, NotFoundException, CannotCompileException {
		classPool.insertClassPath(new LoaderClassPath(ClassGenerator.class.getClassLoader()));

		CtClass generated = classPool.makeClass("org.inventivetalent.nbt.injector.generated." + originalClass.getSimpleName());

		CtClass wrapperInterface = classPool.get(INBTWrapper.class.getName());
		generated.setInterfaces(new CtClass[] { wrapperInterface });
		generated.setSuperclass(classPool.get(originalClass.getName()));

		classPool.importPackage("net.minecraft.server." + Minecraft.VERSION.name());
		classPool.importPackage("org.inventivetalent.nbt");
		classPool.importPackage("org.inventivetalent.nbt.injector.generated");

		generated.addField(CtField.make("public CompoundTag $extraCompound = new CompoundTag(\"" + extraDataKey + "\");", generated));
		generated.addMethod(CtMethod.make("public CompoundTag getNbtData() {\n"
				+ "  return this.$extraCompound;\n"
				+ "}", generated));
		generated.addMethod(CtMethod.make("public NBTTagCompound readExtraCompound(NBTTagCompound root) {\n"
				+ "  NBTTagCompound compound = root.getCompound(\"" + extraDataKey + "\");\n"
				+ "  this.$extraCompound = new CompoundTag().fromNMS(compound);\n"
				+ "  return root;"
				+ "}", generated));
		generated.addMethod(CtMethod.make("public NBTTagCompound writeExtraCompound(NBTTagCompound root) {\n"
				+ "  NBTBase compound = (NBTBase) this.$extraCompound.toNMS();\n"
				+ "  NBTTagCompound newRoot = new NBTTagCompound();\n"
				+ "  newRoot.set(\"" + extraDataKey + "\", compound);\n"
				+ "  root.a(newRoot);\n"
				+ "  return root;"// Merge
				+ "}", generated));

		generated.addMethod(CtMethod.make(writeMethod, generated));
		generated.addMethod(CtMethod.make(readMethod, generated));

		// Overwrite constructors
		for (Constructor constructor : originalClass.getConstructors()) {
			String paramString = "";
			String paramNameString = "";
			int c = 0;
			for (Class clazz : constructor.getParameterTypes()) {
				if (c != 0) {
					paramString += ",";
					paramNameString += ",";
				}
				paramString += clazz.getName() + " param" + c;
				paramNameString += "param" + c;
				c++;
			}
			generated.addConstructor(CtNewConstructor.make("public " + originalClass.getSimpleName() + "(" + paramString + ") {\n"
					+ "  super(" + paramNameString + ");\n"
					+ "}", generated));
		}

		try {
			generated.writeFile("nbtinjector_generated");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return generated.toClass(INBTWrapper.class.getClassLoader(), INBTWrapper.class.getProtectionDomain());
	}

	public static Class wrapEntity(ClassPool classPool, Class<?> originalClass, String extraDataKey) throws ReflectiveOperationException, NotFoundException, CannotCompileException {
		String writeReturn = Minecraft.VERSION.newerThan(Minecraft.Version.v1_10_R1) ? "NBTTagCompound" : "void";
		String writeMethod = "public " + writeReturn + " e(NBTTagCompound compound) {\n"
				+ "  compound = writeExtraCompound(compound);\n"
				+ "  " + (!"void".equals(writeReturn) ? "return " : "") + "super.e(compound);\n"
				+ "}";
		String readMethod = "public void f(NBTTagCompound compound) {\n"
				+ "  super.f(compound);\n"
				+ "  readExtraCompound(compound);\n"
				+ "}";
		return wrapNbtClass(classPool, originalClass, writeMethod, readMethod, extraDataKey);
	}

	public static Class wrapTileEntity(ClassPool classPool, Class<?> originalClass, String extraDataKey) throws ReflectiveOperationException, NotFoundException, CannotCompileException {
		String writeReturn = Minecraft.VERSION.newerThan(Minecraft.Version.v1_9_R1) ? "NBTTagCompound" : "void";
		String writeName = Minecraft.VERSION.newerThan(Minecraft.Version.v1_9_R1) ? "save" : "b";
		String writeMethod = "public " + writeReturn + " " + writeName + "(NBTTagCompound compound) {\n"
				+ "  compound = writeExtraCompound(compound);\n"
				+ "  " + (!"void".equals(writeReturn) ? "return " : "") + "super." + writeName + "(compound);\n"
				+ "}";
		String readMethod = "public void a(NBTTagCompound compound) {\n"
				+ "  super.a(compound);\n"
				+ "  readExtraCompound(compound);\n"
				+ "}";
		return wrapNbtClass(classPool, originalClass, writeMethod, readMethod, extraDataKey);
	}

}
