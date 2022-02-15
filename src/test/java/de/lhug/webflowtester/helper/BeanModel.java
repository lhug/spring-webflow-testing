package de.lhug.webflowtester.helper;

import java.util.List;

import lombok.Data;

@Data
public class BeanModel {

	private String name;
	private int amount;
	private List<String> entries;
}
