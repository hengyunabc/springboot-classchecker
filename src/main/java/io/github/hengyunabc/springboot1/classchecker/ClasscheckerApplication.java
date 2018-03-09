package io.github.hengyunabc.springboot1.classchecker;

import java.io.File;
import java.io.IOException;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClasscheckerApplication {

	public static void main(String[] args) throws IOException {

		for (String path : args) {
			UnsupportedClassCollector collector = new UnsupportedClassCollector(path);
			System.out.println("path: " + new File(path).getName());
			for (String clazz : collector.collect()) {
				System.out.println(clazz);
			}

			System.out.println();
		}

	}
}
