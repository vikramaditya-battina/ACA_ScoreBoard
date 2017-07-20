package scoreboard;

import java.util.HashMap;

public class FRegisters {
	HashMap<String, Float> registers;
	public FRegisters(){
		registers = new HashMap<String, Float>();
	}
	public void writeTo(String register, Float value){
		registers.put(register, value);
	}
	public Float readFrom(String register){
		if(registers.containsKey(register)){
			return registers.get(register);
		}
		return null;
	}
}
