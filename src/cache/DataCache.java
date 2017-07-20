package cache;

public class DataCache {
	int validbits[];
	int lrucounter[];
	int tag[];
	int cacheSize;
	int blockSize;
	int nwayass ;
	int dirtybit[];
	ClockCycle c;
	MemoryBus bus;
	int hits;
	int requests;
	String cache[][];
	MainMemory mem;
	public class DataCacheInfo{
		public String data;
		public int clockCycles;
		DataCacheInfo(String data, int clockCycles){
			this.data = data;
			this.clockCycles = clockCycles;
		}
	}
	public DataCache(ClockCycle c, MemoryBus bus){
		nwayass = 2;
		cacheSize = 4;
		blockSize = 4;
		validbits = new int[cacheSize];
		lrucounter = new int[cacheSize];
		dirtybit = new int[cacheSize];
		tag = new int[cacheSize];
		this.c = c;
		this.bus = bus;
		cache = new String[cacheSize][];
		for(int i =0 ; i < cacheSize; i++){
		  cache[i] = new String[blockSize];
		}
		mem = new MainMemory();
		requests = 0;
		hits = 0;
	}
	public DataCacheInfo getFetchData(long address){
		long oldaddress = address;
		System.out.println("address are:-->>"+oldaddress);
		address = address >> 2;
		//hard coding as it is fixed BlockSize and fixed number of blocks;
		long blockOffsetMask = 3;
		int blockOffset = (int)(address & blockOffsetMask);
		long addr = address >> 2;
		int setNum = (int)(addr %2);
		this.requests++;
		//checking the hit or not
		int index = setNum * this.nwayass;
		int templrucounter[] = new int[4];
		templrucounter[index] = this.lrucounter[index];
		templrucounter[index+1] = this.lrucounter[index+1];
		this.lrucounter[index] = 0;
		this.lrucounter[index+1] = 0;
		if(this.validbits[index] == 1 && this.tag[index] == addr){
			this.lrucounter[index] = 1;
			this.hits++;
			return new DataCacheInfo(cache[index][blockOffset],1);
		}
		if(this.validbits[index+1] == 1 && this.tag[index+1] == addr){
			this.lrucounter[index+1] = 1;
			this.hits++;
			return new DataCacheInfo(cache[index+1][blockOffset],1);
		}
		//missed not available in the cache so fetch from the mainMemory
		//lot of redundant code will try to fix it.
		//fetched data from the mem address
		String data[] = mem.fetchData(oldaddress);
		
		if(this.validbits[index] == 0){
			this.validbits[index] = 1;
			this.lrucounter[index] = 1;
			this.dirtybit[index] = 0;
			this.tag[index] = (int)addr;
			for(int i = 0 ; i < data.length; i++){
				cache[index][i] = data[i];
			}
			return new DataCacheInfo(cache[index][blockOffset],numClockCyclesNeed(12)+1);
		}
		if(this.validbits[index+1] == 0){
			this.validbits[index+1] = 1;
			this.dirtybit[index+1] = 0;
			this.lrucounter[index+1] = 1;
			this.tag[index+1] = (int)addr;
			for(int i = 0 ; i < data.length; i++){
				cache[index+1][i] = data[i];
			}
			return new DataCacheInfo(cache[index+1][blockOffset],numClockCyclesNeed(12)+1);
		}
		
		//not available place in the cache, so please use LRU replacement startegy to find which block to replace
		if(templrucounter[index] == 0){
			this.validbits[index] = 1;
			this.lrucounter[index] = 1;
			int extraCycles = 0;
			if(this.dirtybit[index] == 1){
				extraCycles = 12;
				mem.updateData(oldaddress, cache[index]);
			}
			this.dirtybit[index] = 0;
			this.tag[index] = (int)addr;
			for(int i = 0 ; i < data.length; i++){
				cache[index][i] = data[i];
			}
			return new DataCacheInfo(cache[index][blockOffset],numClockCyclesNeed(12+extraCycles)+1);
		}
		if(templrucounter[index+1] == 0){
			this.validbits[index+1] = 1;
			this.lrucounter[index+1] = 1;
			this.tag[index+1] = (int)addr;
			int extraCycles = 0;
			if(this.dirtybit[index+1] == 1){
				extraCycles = 12;
				mem.updateData(oldaddress, cache[index]);
			}
			this.dirtybit[index+1] = 0;
			for(int i = 0 ; i < data.length; i++){
				cache[index+1][i] = data[i];
			}
			return new DataCacheInfo(cache[index+1][blockOffset],numClockCyclesNeed(12+extraCycles)+1);
		}
		return null;
	}
	int numClockCyclesNeed(int clockcyclestake){
		if(bus.isBusBusy(c.count()) == false){
			bus.updateBusBusyTill(c.count()+clockcyclestake);
			return clockcyclestake;
		}else{
			int busyCount = (int)((bus.getBusBusyTill() - c.count())+clockcyclestake);
			//bus.updateBusBusyTill(c.count()+busyCount);
		    bus.updateBusBusyTill(bus.getBusBusyTill()+clockcyclestake);
			return busyCount;
		}
	}
	public int updateValue(long address, String data){
		long oldaddress = address;
		address = address >> 2;
		long blockOffsetMask = 3;
		int blockOffset = (int)(address & blockOffsetMask);
		long addr = address >> 2;
		int setNum = (int)(addr %2);
		this.requests++;
		//checking the hit or not
		int index = setNum * this.nwayass;
		int templrucounter[] = new int[4];
		templrucounter[index] = this.lrucounter[index];
		templrucounter[index+1] = this.lrucounter[index+1];
		this.lrucounter[index] = 0;
		this.lrucounter[index+1] = 0;
		//checking whether it is existed in the cache or not
		if(this.validbits[index] == 1 && this.tag[index] == addr){
			this.lrucounter[index] = 1;
			this.dirtybit[index] = 1;
			this.hits++;
			cache[index][blockOffset] = data;
			return 1;
		}
		if(this.validbits[index+1] == 1 && this.tag[index+1] == addr){
			this.lrucounter[index+1] = 1;
			this.dirtybit[index+1] = 1;
			cache[index+1][blockOffset] = data;
			this.hits++;
			return 1;
		}
		//----------------------------------------------------------------
		/* ------------------Checking whether free slots exists or not -------------------------------------*/
		String newdata[] = mem.updateGetData(oldaddress, blockOffset,data);
		if(this.validbits[index] == 0){
			this.validbits[index] = 1;
			this.lrucounter[index] = 1;
			this.dirtybit[index] = 0;
			this.tag[index] = (int)addr;
			for(int i = 0 ; i < newdata.length; i++){
				cache[index][i] = newdata[i];
			}
			return numClockCyclesNeed(12)+1;			
		}
		if(this.validbits[index+1] == 0){
			this.validbits[index+1] = 1;
			this.lrucounter[index+1] = 1;
			this.dirtybit[index+1] = 0;
			this.tag[index+1] = (int)addr;
			for(int i = 0 ; i < newdata.length; i++){
				cache[index+1][i] = newdata[i];
			}
			return numClockCyclesNeed(12)+1;	
		}
		//-------------------------------------------------------------------------
		/*-------------------------free slot not found ------------------------------------------*/
		if(templrucounter[index] == 0){
			this.validbits[index] = 1;
			this.lrucounter[index] = 1;
			this.tag[index] = (int)addr;
			int extraCycles = 0;
			if(this.dirtybit[index] == 1){
				extraCycles = 12;
				mem.updateData(oldaddress, cache[index]);
			}
			this.dirtybit[index] = 0;
			for(int i = 0 ; i < newdata.length; i++){
				cache[index][i] = newdata[i];
			}
			return numClockCyclesNeed(12+extraCycles)+1;
		}
		if(templrucounter[index+1] == 0){
			this.validbits[index+1] = 1;
			this.lrucounter[index+1] = 1;
			this.tag[index+1] = (int)addr;
			int extraCycles = 0;
			if(this.dirtybit[index+1] == 1){
				mem.updateData(oldaddress, cache[index+1]);
				extraCycles =12;
			}
			this.dirtybit[index+1] = 0;
			for(int i = 0 ; i < newdata.length; i++){
				cache[index+1][i] = newdata[i];
			}
			return numClockCyclesNeed(12+extraCycles)+1;
		}
		return -1;
	}
	public String getStats(){
		String requests = "Total number of access requests for data cache: "+this.requests;
		String hits = "Number of data cache hits: "+this.hits;
		String myResult = requests+"\n"+hits;
		return myResult;
	}
	public void writeToMem(){
		for(int i = 0 ; i < 4; i++){
			if(dirtybit[i] == 1){
				int tag_val = tag[i];
				int address = tag_val << 4;
				mem.updateData(address,cache[i]);
			}
		}
	}
	public void writeMemToFile(){
		mem.writeToFile();
	}
}
