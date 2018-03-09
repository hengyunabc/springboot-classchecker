package io.github.hengyunabc.springboot1.classchecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

public class UnsupportedClassCollector {

	private String path;

	public UnsupportedClassCollector(String path) {
		this.path = path;
	}

	private Set<String> collectClassFileStrings(InputStream classInputStream) throws IOException {
		Set<String> result = new HashSet<>(1024);

		ClassReader reader = new ClassReader(classInputStream);

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		reader.accept(new ClassVisitor(Opcodes.ASM6, cw) {

		}, ClassReader.EXPAND_FRAMES);

		cw.toByteArray();

		ReflectionUtils.doWithFields(ClassReader.class, new FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				if (field.getName().equals("strings")) {
					ReflectionUtils.makeAccessible(field);
					String[] strings = (String[]) ReflectionUtils.getField(field, reader);

					for (int i = 0; i < strings.length; ++i) {
						Optional<String> className = extractClassNameFromString(strings[i]);
						className.ifPresent(result::add);
					}
				}

			}

		});

		return result;
	}

	/**
	 * TODO simple process
	 *
	 * @param str
	 * @return
	 */
	private Optional<String> extractClassNameFromString(String str) {
		if (str != null && !str.equals("null")) {
			if (str.startsWith("L")) {
				try {
					str = Type.getType(str).getClassName();
				} catch (Exception e) {
				}

			}
			str = str.replace('/', '.');
			int index = str.indexOf('<');
			if (index > 0) {
				str = str.substring(0, index);
			}

			return Optional.of(str);
		}

		return Optional.empty();
	}

	private Set<String> collectFormDirectory(File dir) {
		Set<String> result = new HashSet<>(1024);
		FileUtils.listFiles(dir, new String[] { "class" }, true).stream().forEach(new Consumer<File>() {

			@Override
			public void accept(File t) {
				try (InputStream inputStream = new FileInputStream(t)) {
					result.addAll(collectClassFileStrings(inputStream));
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		});
		return result;
	}

	private Set<String> collectFromZip(File path) {
		Set<String> result = new HashSet<>(1024);

		try (ZipFile zipFile = new ZipFile(path);) {

			zipFile.stream().forEach(new Consumer<ZipEntry>() {
				@Override
				public void accept(ZipEntry entry) {
					if (entry.isDirectory()) {
						return;
					}

					String name = entry.getName();
					if (name.endsWith(".class")) {
						try (InputStream entryInputStream = zipFile.getInputStream(entry);) {
							result.addAll(collectClassFileStrings(entryInputStream));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public Set<String> collect() {
		Set<String> result = new HashSet<>(1024);
		File file = new File(path);
		if (file.isDirectory()) {
			result.addAll(collectFormDirectory(file));
		} else if (path.endsWith(".zip") || path.endsWith(".jar")) {
			result.addAll(collectFromZip(file));
		}

		// check .class file exist in current classloader
		result = result.stream().filter(s -> s.startsWith("org.springframework")).filter(new Predicate<String>() {
			@Override
			public boolean test(String className) {
				URL resource = Thread.currentThread().getContextClassLoader()
						.getResource(className.replace('.', '/') + ".class");
				return resource == null;
			}

		}).collect(Collectors.toSet());

		return result;
	}


}
