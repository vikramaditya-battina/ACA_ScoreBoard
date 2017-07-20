package scoreboard;

import java.util.ArrayList;
import java.util.List;

import instructions.DataInst;
import instructions.*;
import units.FunctionalUnitTypes;
public class FunctionalUnit {
	ArrayList<FunctionalUnitStatus> fp;
	int FPDIVIDER_Units;
	int FPADDER_Units;
	int FPMULTIPLIER_Units;
	public FunctionalUnit(int multipliers, int dividers, int adders){
		fp = new ArrayList<FunctionalUnitStatus>();
		this.FPADDER_Units = adders;
		this.FPDIVIDER_Units = dividers;
		this.FPMULTIPLIER_Units = multipliers;
		for(int i = 0 ; i < this.FPMULTIPLIER_Units; i++){
			fp.add(new FunctionalUnitStatus(FunctionalUnitTypes.FPMultiplier));
		}
		for(int i = 0; i < this.FPADDER_Units; i++){
			fp.add(new FunctionalUnitStatus(FunctionalUnitTypes.FPAdder));
		}
		for(int i = 0; i < this.FPDIVIDER_Units; i++){
			fp.add(new FunctionalUnitStatus(FunctionalUnitTypes.FPDivider));
		}
		fp.add(new FunctionalUnitStatus(FunctionalUnitTypes.NInteger));
		fp.add(new FunctionalUnitStatus(FunctionalUnitTypes.LSU));
		fp.add(new FunctionalUnitStatus(FunctionalUnitTypes.BRANCH));
	}
	
	public boolean isAvailable(FunctionalUnitTypes fType){
		for(int i = 0 ; i < fp.size(); i++){
			if(fp.get(i).fType == fType && fp.get(i).status == false){
				return true;
			}
		}
		return false;
	}
	//you need to pass destination regsiter
	public boolean isWAWHazard(String register){
		for(int i = 0 ; i < fp.size(); i++){
			if(fp.get(i).status == true && fp.get(i).destinationRegister.equals(register)){
				return true;
			}
		}
		return false;
	}
	//uyou need to pass source register
	public boolean isRAWHazard(DataInst inst){
		FunctionalUnitStatus fStatus = inst.getFunctionalUnitStatus();
		if(fStatus.source1Ready == true && fStatus.source2Ready == true){
			return false;
		}
		return true;
	}
	
	//isWaR hazard need to pass the destination()
	public boolean isWARHazard(DataInst inst){
	    if( inst.getDestinationReg() == null){
	    	return false;
	    }
	    String destReg = inst.getDestinationReg().getDestination();
		for(int i = 0 ; i < fp.size(); i++){
			FunctionalUnitStatus fStatus = inst.getFunctionalUnitStatus();
			if(fStatus.status){
				if(destReg.equals(fStatus.sourceRegister1) && fStatus.source1Ready == true){
					return true;
				}
				if(destReg.equals(fStatus.sourceRegister2) && fStatus.source2Ready == true){
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isReady(String register){
		for(int i = 0; i < fp.size(); i++){
			FunctionalUnitStatus fStatus = fp.get(i);
			if(fStatus.status == true && fStatus.destinationRegister.equals(register)){
				return false;
			}
		}
		return true;
	}
	//it will assign the functional status
	public FunctionalUnitStatus getFunctionalUnit(DataInst ist){
		
		String destRegister = "";
		String srcRegister1  = "";
		String srcRegister2 = "";
		boolean src1Ready = true;
		boolean src2Ready = true;
		if(ist.getDestinationReg() != null){
			destRegister = ist.getDestinationReg().getDestination();
		}
		List<SourceVal> srcReg = ist.getSourceReg();
		if(srcReg != null && srcReg.size() > 0){
			srcRegister1 = ist.getSourceReg().get(0).getSource();
			src1Ready = isReady(srcRegister1);
		}
		if(srcReg != null &&srcReg.size() > 1){
			srcRegister2 = ist.getSourceReg().get(1).getSource();
			src2Ready = isReady(srcRegister2);
		}
		for(int i = 0; i < fp.size(); i++){
			if(fp.get(i).status == false && ist.fType == fp.get(i).fType){
				FunctionalUnitStatus status = fp.get(i);
				status.fType = ist.fType;
				status.source1Ready = src1Ready;
				status.source2Ready = src2Ready;
				status.sourceRegister1 = srcRegister1;
				status.sourceRegister2 = srcRegister2;
				status.destinationRegister = destRegister;
				status.status = true;
				return status;
			}
		}
		return null;
	}
	
	public void writeToFunctionalStatus(DataInst inst){
		FunctionalUnitStatus fStatus = inst.getFunctionalUnitStatus();
		if(inst.getDestinationReg() == null){	
			fStatus.clear();
			return;
		}
		String destRegister = inst.getDestinationReg().getDestination();
		for(int i = 0 ; i < fp.size(); i++){
			FunctionalUnitStatus myStatus = fp.get(i);
			if(myStatus.sourceRegister1.equals(destRegister)){
				myStatus.source1Ready = true;
			}
			if(myStatus.sourceRegister2.equals(destRegister)){
				myStatus.source2Ready = true;
			}
		}
		fStatus.clear();
	}
	
}