package scoreboard;
import units.FunctionalUnitTypes;
public class FunctionalUnitStatus {
	FunctionalUnitTypes fType;
	String sourceRegister1;
	String sourceRegister2;
	String destinationRegister;
	boolean status;
	boolean source1Ready;
	boolean source2Ready;
	public FunctionalUnitStatus(FunctionalUnitTypes ftype){
		 this.fType = ftype;
		 this.sourceRegister1 = "";
		 this.sourceRegister2= "";
		 this.destinationRegister = "";
		 this.status = false;
		 this.source1Ready = false;
		 this.source2Ready = false;
	}
	public void clear(){
		this.sourceRegister1 = "";
		this.sourceRegister2 = "";
		this.source1Ready = false;
		this.source2Ready = false;
		this.destinationRegister = "";
		this.status = false;
	}
}
