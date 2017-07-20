package scoreboard;

import java.util.HashMap;

public class Registers {
	HashMap<String, Integer> registers;
	public Registers(){
		registers = new HashMap<String, Integer>();
		for(int i = 0 ; i < 32; i++){
			registers.put("F"+i, 0);
		}
	}
	public void writeTo(String register, Integer value){
		registers.put(register, value);
	}
	
	public Integer readFrom(String register){
		if(registers.containsKey(register)){
			return registers.get(register);
		}
		return null;
	}
}
