package org.fade.demo.springframework.beans.parse.lookupmethod;

/**
 * @author fade
 * @date 2021/11/16
 */
public class Student extends User {

	@Override
	public void showMe() {
		System.out.println("I am student");
	}

}
