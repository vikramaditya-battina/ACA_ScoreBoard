package scoreboard;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import Helpers.BinaryConversions;
import Helpers.Params;
import cache.*;
import cache.DataCache.DataCacheInfo;
import controllers.ConfigManager;
import controllers.RunManager;
import inputParsers.ConfigParser;
import inputParsers.InstructionParser;
import instructions.BEQ;
import instructions.BNE;
import instructions.DataInst;
import instructions.HLT;
import instructions.J;
import instructions.LD;
import instructions.LW;
import instructions.SD;
import instructions.SW;
import instructions.SourceVal;
import units.FunctionalUnitTypes;
import units.InstructionTypes;
public class ScoreBoard {
	FunctionalUnit fu;
	ConfigParser cparser;
	ArrayList<DataInst> read;
	ArrayList<DataInst> fetch;
	ArrayList<DataInst> issue;
	ArrayList<DataInst> writeBackPhase;
	ArrayList<DataInst> executePhase;
	InstructionCache iCache;
	DataCache dCache;
	ClockCycle clock;
	InstructionParser iParser;
	int WB_INDEX;
	int EXEC_INDEX;
	int READ_INDEX;
	int ISSUE_INDEX;
	int FETCH_INDEX;
	ConfigManager cManager;
	RunManager runManager;
	Registers registers;
	int pc;
	public ScoreBoard() throws IOException{
		clock = new ClockCycle();
		registers = new Registers();
		cparser = ConfigParser.getInstance();
		iParser = InstructionParser.getInstance();
		pc = 0;
		loadConfigFile();
		loadInstructionFile();
		cManager = ConfigManager.getInstance();
		runManager = RunManager.getInstance();
		ConfigManager cm =  ConfigManager.getInstance();
		initializecache(ConfigManager.getInstance());
		//FunctionalUnit(int multipliers, int dividers, int adders)
		fu = new FunctionalUnit(cm.getFPMULTIPLIER_Units(), cm.getFPDIVIDER_Units(), cm.getFPADDER_Units());
		executePhase = new ArrayList<DataInst>();
		writeBackPhase = new ArrayList<DataInst>();
		issue = new ArrayList<DataInst>();
		fetch = new ArrayList<DataInst>();
		read = new ArrayList<DataInst>();
		
		WB_INDEX = 4;
		EXEC_INDEX = 3;
		READ_INDEX = 2;
		ISSUE_INDEX = 1;
		FETCH_INDEX = 0;
	}
	
	public void loadConfigFile(){
		try {
			cparser.readConfigFile(Params.confFPath);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void loadInstructionFile() throws IOException{
			iParser.readInstFile(Params.insFPath);
	}
	
	public void initializecache(ConfigManager cm){
		MemoryBus bus = new MemoryBus();
		iCache = new InstructionCache(cm.getICACHE_NUM_BLOCKS(), cm.getICACHE_BLOCK_SIZE(), clock, bus);
		dCache = new DataCache(clock, bus);
	}
	
	public void startScoreBoard() throws Exception{
		boolean isDone;
		boolean isStall = false;
		boolean isFlush = false;
		int instIssueNum = 0;
		boolean makeStall = false;
		ArrayList<DataInst> fetchingNextWord = new ArrayList<DataInst>();
		ArrayList<DataInst> completedInstructions = new ArrayList<DataInst>();
		while(true){
			isDone = true;
			/*********************************************************************************************/
			ArrayList<DataInst> completeElgible = new ArrayList<DataInst>();
			ArrayList<DataInst> branchComplete = new ArrayList<DataInst>();
			ArrayList<DataInst> haltBranchComplete = new ArrayList<DataInst>();
			ArrayList<DataInst> flushedComplete = new ArrayList<DataInst>();
			if(writeBackPhase.size() != 0){
				isDone = false;
				for(int i = 0 ; i < writeBackPhase.size(); i++){
					DataInst inst = writeBackPhase.get(i);
					if(inst.endCycle[this.WB_INDEX] < clock.count() ){
						if(inst.getDestinationReg() != null && fu.isWARHazard(inst)){
							//there is a hazard so please
							inst.WARHZ = true;
						}else{
							completeElgible.add(inst);
						}
					}
				}
			}
			/*------------------------------------------------------------------------------------------------*/
			if(executePhase.size() != 0){
				isDone = false;
				ArrayList<DataInst> writeElgible = new ArrayList<DataInst>();
				for(int i = 0 ; i < executePhase.size(); i++){
					DataInst inst = executePhase.get(i);
					if(inst.endCycle[this.EXEC_INDEX] < clock.count()){
						//we dont need to check any hazards
						writeElgible.add(inst);
					}
				}
				for(int i = 0 ; i < writeElgible.size(); i++){
					DataInst inst = writeElgible.get(i);
					executePhase.remove(inst);
					inst.endCycle[this.WB_INDEX] = (int)(this.clock.count()+1-1);
					inst.endCycle[this.EXEC_INDEX] = (int)(this.clock.count()-1);
					writeBackPhase.add(inst);
				}
			}
			/*************************************************************************************************************/
			ArrayList<DataInst> executeLDSTElgible = new ArrayList<DataInst>();
			if(read.size() != 0){
				isDone = false;
				ArrayList<DataInst> excuteElgible = new ArrayList<DataInst>();
				for(int i = 0; i < read.size(); i++){
					DataInst inst = read.get(i);
					if(inst.endCycle[this.READ_INDEX] < clock.count()){
						List<SourceVal> sourceList = inst.getSourceReg();
						if(sourceList != null){
							boolean hasRAW = false;
							for(int j =0 ; j < sourceList.size(); j++){
								String register = sourceList.get(j).getSource();
								if( this.fu.isRAWHazard(inst) == true){
									hasRAW = true;
								}
							}
							if(hasRAW == true){
								inst.RAWHZ = true;
							}else{
								excuteElgible.add(inst);	
							}
						}else{
							excuteElgible.add(inst);
						}
					}
				}
				for(int i =0; i < excuteElgible.size(); i++){
					DataInst inst = excuteElgible.get(i);
					List<SourceVal> sourceReg = inst.getSourceReg();
					if(sourceReg != null){
						if(sourceReg.size() > 0){
							SourceVal reg = sourceReg.get(0);
							String regName = reg.getSource();
							reg.setValue(registers.readFrom(regName));
						}
						if(sourceReg.size() > 1){
							SourceVal reg = sourceReg.get(1);
							String regName = reg.getSource();
							reg.setValue(registers.readFrom(regName));
						}
					}
					//need to update the read flags in the functional unit status
					FunctionalUnitStatus fStatus = inst.getFunctionalUnitStatus();
					//assuming at max there would be only 2 source registers
					fStatus.source1Ready = false;
					fStatus.source2Ready = false;
					inst.endCycle[this.READ_INDEX] = (int) (this.clock.count()-1);
					//need to check whether store or not so that you know how many cycle it would compute
					
					//executing the instruction
					inst.execInst();
					if(inst.iType == InstructionTypes.ConditionalBranch){
						makeStall = false;
						if(inst instanceof BEQ){
							BEQ bq = (BEQ)inst;
							if(bq.getComparedResult()){
								isFlush = true;
								String label = bq.getBranchToLabel();
								pc = runManager.getAddressforLabel(label);
							}
						}else if(inst instanceof BNE){
							BNE bne = (BNE)inst;
							if(bne.getComparedResult()){
								isFlush = true;
								String label = bne.getBranchToLabel();
								pc = runManager.getAddressforLabel(label);
							}
						}
						branchComplete.add(inst);
					} 
					else if(inst.fType == FunctionalUnitTypes.FPAdder){
						inst.endCycle[this.EXEC_INDEX] = (int) (cManager.getFPADDER_Cycles()+clock.count()-1);
						executePhase.add(inst);
					}else if(inst.fType == FunctionalUnitTypes.FPDivider){
						inst.endCycle[this.EXEC_INDEX] = (int) (cManager.getFPDIVIDER_Cycles()+clock.count()-1);
						executePhase.add(inst);
					}else if(inst.fType == FunctionalUnitTypes.FPMultiplier){
						inst.endCycle[this.EXEC_INDEX] = (int) (cManager.getFPMULTIPLIER_Cycles()+clock.count()-1);
						executePhase.add(inst);
					}else if(inst.fType == FunctionalUnitTypes.NInteger){
						inst.endCycle[this.EXEC_INDEX] = (int) (1 + clock.count()-1);
						executePhase.add(inst);
					}else if(inst.fType == FunctionalUnitTypes.LSU){
						executeLDSTElgible.add(inst);
						executePhase.add(inst);
					}
					//
					read.remove(inst);	
				}
			} 
			/*---------------------------------------------------------------------------------------------------------*/
			if(issue.size() != 0 && isStall == false){
				isDone = false;
				ArrayList<DataInst> readElgible = new ArrayList<DataInst>();
				for(int i = 0 ; i < issue.size(); i++){
					DataInst inst = issue.get(i);
					if(isFlush == true){
						issue.remove(inst);
						inst.endCycle[this.ISSUE_INDEX] = (int) (this.clock.count()-1);
						flushedComplete.add(inst);
						continue;
					}
					if(inst instanceof HLT){
						haltBranchComplete.add(inst);
						continue;
					}
					if(inst instanceof J){
						J j = (J)inst;
						String label = j.getJumpTo();
						haltBranchComplete.add(inst);
						pc = runManager.getAddressforLabel(label);
						isFlush = true;
						continue;
					}
					//check whether its a branch instruction or not
					if(inst.entryCycle[this.ISSUE_INDEX] < clock.count()){
						if( fu.isAvailable(inst.fType) == false){
							inst.STRUCTHZ = true;
							continue;
						}
						if(inst.getDestinationReg() == null){
							readElgible.add(inst);
						}
						else if(fu.isWAWHazard(inst.getDestinationReg().getDestination()) == false){
							readElgible.add(inst);
						}
						else{
							inst.WAWHZ = true;
						}
					}
				}
				for(int i = 0 ; i < readElgible.size(); i++){
					DataInst inst = readElgible.get(i);
					if(inst.iType == InstructionTypes.ConditionalBranch){
						makeStall = true;
					}
					FunctionalUnitStatus st = fu.getFunctionalUnit(inst);
					inst.setFunctionalUnitStatus(st);
					inst.endCycle[this.READ_INDEX] = (int) (this.clock.count()+1-1);
					inst.endCycle[this.ISSUE_INDEX] = (int) (this.clock.count()-1);
					issue.remove(inst);
					read.add(inst);
				}
				for(int i = 0 ; i < haltBranchComplete.size(); i++){
					DataInst inst = haltBranchComplete.get(i);
					issue.remove(inst);
				}
			}
		/*********************************************************************************************************/
			
			if(fetch.size() == 0 && isStall == false){
				
				
				DataInst inst = runManager.getInstructioninAddress(pc);
				if(inst != null){
					int clockCycles = this.iCache.fetchInstruction(pc);
					isDone = false;
					inst.endCycle[this.FETCH_INDEX] = (int) (clock.count()+clockCycles-1);
					pc = pc  + 1;
					inst.instIssueNum = instIssueNum;
					instIssueNum = instIssueNum +1;
					fetch.add(inst);
				}
				
			}else if(isStall == false){
				//only one instruction in fetch phaze
				isDone = false;
				DataInst inst = fetch.get(0);
				if(issue.size() == 0 && inst.endCycle[this.FETCH_INDEX] < clock.count()){
					fetch.remove(inst); 
					inst.endCycle[this.FETCH_INDEX] = (int) (clock.count()-1);
					if(isFlush == false){
						inst.endCycle[this.ISSUE_INDEX] = (int) (clock.count()+1-1);
						issue.add(inst);
					}else{
						isFlush = false;
						flushedComplete.add(inst);
					}
					DataInst newinst = runManager.getInstructioninAddress(pc);
					
					if(newinst != null){
						int clockCycles = this.iCache.fetchInstruction(pc);
						isDone = false;
						newinst.endCycle[this.FETCH_INDEX] = (int) (clock.count()+clockCycles-1);
						newinst.instIssueNum = instIssueNum;
						instIssueNum = instIssueNum +1;
						fetch.add(newinst);
						pc = pc  + 1;
					}
				}
			}
		/***************************************************************************************************/
			for(int i = 0 ; i < flushedComplete.size(); i++){
				DataInst inst = flushedComplete.get(i);
				//inst.endCycle[this.FETCH_INDEX] = (int) (clock.count()-1);
				System.out.println(inst.toString());
				completedInstructions.add(inst);
			}
		/***************************************************************************************************/
			for(int i = 0 ; i < branchComplete.size(); i++){
				DataInst inst = branchComplete.get(i);
				inst.endCycle[this.READ_INDEX] = (int)(clock.count()-1);
				FunctionalUnitStatus st = inst.getFunctionalUnitStatus();
				fu.writeToFunctionalStatus(inst);
				System.out.println(inst.toString());
				completedInstructions.add(inst);
			}
		/*********************************************************************************************/
			for(int i = 0 ; i < haltBranchComplete.size(); i++){
				DataInst inst = haltBranchComplete.get(i);
				inst.endCycle[this.ISSUE_INDEX] = (int) (clock.count()-1);
				issue.remove(inst);
				System.out.println(inst.toString());
				completedInstructions.add(inst);
			} 
		/*******************************************************************************************************/
			
			fetchingNextWord = new ArrayList<DataInst>();
			for(int i = 0 ; i < executeLDSTElgible.size(); i++){
				DataInst inst = executeLDSTElgible.get(i);
				if(inst instanceof LD){
					long addressvalue = inst.addressValue;
					DataCacheInfo info = dCache.getFetchData(addressvalue);
					//DataCacheInfo info2 = dCache.getFetchData(addressvalue+4);
					//inst.endCycle[this.EXEC_INDEX] = (int) (clock.count()+info.clockCycles + info2.clockCycles -1);
					inst.endCycle[this.EXEC_INDEX] = (int) (clock.count()+info.clockCycles -1);
					int value = BinaryConversions.toInt(info.data);
					//int value =Integer.parseInt(info.data,2);
					//int value =Integer.parseInt("101001",2); 
					fetchingNextWord.add(inst);
					inst.getDestinationReg().setValue(value);
				}else if(inst instanceof SD){
					long addressvalue = inst.addressValue;
					int numCycles = dCache.updateValue(addressvalue,"10000000100000001000000010000000");
					//int numCycles2 = dCache.updateValue(addressvalue+4,"10000000100000001000000010000000");
					//inst.endCycle[this.EXEC_INDEX] = (int) (clock.count()+numCycles+ numCycles2 -1);
					inst.endCycle[this.EXEC_INDEX] = (int) (clock.count()+numCycles -1);
					fetchingNextWord.add(inst);
				}else if(inst instanceof LW){
					long addressvalue = inst.addressValue;
					DataCacheInfo info = dCache.getFetchData(addressvalue);
					inst.endCycle[this.EXEC_INDEX] = (int) (clock.count()+info.clockCycles -1);
					int value = BinaryConversions.toInt(info.data);
					//int value =Integer.parseInt("101001",2);
					inst.getDestinationReg().setValue(value);
				}else if(inst instanceof SW){
					long addressvalue = inst.addressValue;
					List<SourceVal> srcReg = inst.getSourceReg();
					String defaultValue = "10000000";
					if(srcReg != null){
						int val = srcReg.get(0).getValue();
						defaultValue = BinaryConversions.toBinary(val);
					}
					int numCycles = dCache.updateValue(addressvalue, defaultValue);
					inst.endCycle[this.EXEC_INDEX] = (int) (clock.count()+numCycles -1);
				}
			}
		/*************************************************************************************************************/
			for(int i = 0 ; i < completeElgible.size(); i++){
				DataInst inst = completeElgible.get(i);
				writeBackPhase.remove(inst);
				if(inst.getDestinationReg() != null){
					String regName = inst.getDestinationReg().getDestination();
					int val = inst.getDestinationReg().getValue();
					registers.writeTo(regName, val);
				}
				System.out.println(inst.instIssueNum);
				System.out.println(inst.toString());
				completedInstructions.add(inst);
				FunctionalUnitStatus st = inst.getFunctionalUnitStatus();
				fu.writeToFunctionalStatus(inst);
			}
			if(isDone == true){
				dCache.writeToMem();
				dCache.writeMemToFile();
				prettyPrintAllInstrcutions(completedInstructions);
				break;
			}
			if(makeStall == true){
				isStall = true;
			}
			if(makeStall == false){
				isStall = false;
			}
			clock.tick();
            for(int i = 0 ; i < fetchingNextWord.size();i++){
				DataInst inst = fetchingNextWord.get(i);
				if(inst instanceof LD){
					long addressvalue = inst.addressValue;
					DataCacheInfo info = dCache.getFetchData(addressvalue+4);
					inst.endCycle[this.EXEC_INDEX] = inst.endCycle[this.EXEC_INDEX] +info.clockCycles;
					int value = BinaryConversions.toInt(info.data);
					inst.getDestinationReg().setValue(value);
				}else if(inst instanceof SD){
					long addressvalue = inst.addressValue;
					int numCycles2 = dCache.updateValue(addressvalue+4,"10000000100000001000000010000000");
					inst.endCycle[this.EXEC_INDEX] = inst.endCycle[this.EXEC_INDEX] +numCycles2;
				}
			}
		}
	}
	public static void main(String args[]){
		try{
			Params.insFPath = args[0];
			Params.dataFPath = args[1];
			Params.confFPath = args[2];
			Params.resFPath  = args[3];
		}
		catch(Exception e){
			System.out.println("Error: ");
			System.out.println("Usage: java ScoreBoard <instfilepath> <datafilepath> <conffilepath> <resfilepath>");
		}
		ScoreBoard sb;
		try {
			sb = new ScoreBoard();
			sb.startScoreBoard();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void prettyPrintAllInstrcutions(ArrayList<DataInst> totalInstructions){
		class MyComparator implements Comparator<DataInst>{

			@Override
			public int compare(DataInst arg0, DataInst arg1) {
				// TODO Auto-generated method stub
				if(arg0.instIssueNum < arg1.instIssueNum){
					return -1;
				}
				return 1;
			}
		}
		Collections.sort(totalInstructions, new MyComparator());
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(Params.resFPath));
			String[] headerarr = new String[]{"","Instruction","Fetch","Issue","Read","Exec","Write","RAW","WAW","Struct"};
			String header = String.format(Params.outputPrintFormat, headerarr);
			bw.write(header);
			bw.write("\n");
			for(int i = 0 ; i < totalInstructions.size(); i++){
				DataInst inst = totalInstructions.get(i);
				bw.write(inst.getoutputstr());
				bw.write("\n");
			}
			bw.write(this.iCache.getStats());
			bw.write("\n");
			bw.write(this.dCache.getStats());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Got exception while Opening resultfile..");
			e.printStackTrace();
		}
	}
}
