package text;

import java.util.ArrayList;

public class split {
	public static void main(String[] args) 
		
	{
		String Tokens[];
		String[] loginS = {"LOGIN#"};
		String str1 = "MESSAGE#李四#张三#是的啊";
		ArrayList<String> list = new ArrayList<String>();
		list.add(str1);
		Tokens = str1.split("#");
		for(int i =0;i<Tokens.length;i++ ) {
			System.out.println(Tokens[i]+" ");
		}
		//
	}
}
