package cache;

import java.util.HashMap;

public class InstructionCache {
	private int numBlocks;
	private int blockSize;
	HashMap<Integer,Integer> cache;
	ClockCycle c;
	MemoryBus bus;
	long blockOffsetMask;
	int offsetcount;
	int requests;
	int hits;
	public InstructionCache(int numBlocks, int blockSize, ClockCycle c, MemoryBus bus){
		this.numBlocks = numBlocks;
		this.blockSize = blockSize;
		this.c = c;
		this.bus = bus;
		cache = new HashMap<Integer, Integer>();
		updateoffsetMask();
		hits = 0;
		requests = 0;
	}
	public int fetchInstruction(long address){
		int blockOffset = (int)(address & this.blockOffsetMask);
		int blockNumber = (int) ((address >> offsetcount)%this.numBlocks);
		int tag = (int) (address >> offsetcount);
		this.requests++;
		if(cache.containsKey(blockNumber)){
			if(cache.get(blockNumber) == tag){
				this.hits++;
				return 1;
			}else{
				cache.put(blockNumber,tag);
				return numClockCyclesNeed(this.blockSize*3 )+1;	
			}
		}else{
			cache.put(blockNumber,tag);
			return numClockCyclesNeed(this.blockSize*3 )+1;
		}
	}
	private void updateoffsetMask(){
		int tempBlockSize = this.blockSize;
		this.blockOffsetMask = 0;
		this.offsetcount = 0;
		
		while(tempBlockSize != 0){
			tempBlockSize = tempBlockSize/2;
			this.blockOffsetMask = this.blockOffsetMask << 1;
			this.blockOffsetMask = this.blockOffsetMask | 1;
			this.offsetcount++;
		}
		this.blockOffsetMask = this.blockOffsetMask >> 1;
		this.offsetcount = this.offsetcount -1;
	}
	
	int numClockCyclesNeed(int clockcyclestake){
		if(bus.isBusBusy(c.count()) == false){
			bus.updateBusBusyTill(c.count()+clockcyclestake);
			return clockcyclestake;
		}else{
			int busyCount = (int)((bus.getBusBusyTill() - c.count())+clockcyclestake);
			bus.updateBusBusyTill(bus.getBusBusyTill()+clockcyclestake);
			return busyCount;
		}
	}
	
	public String getStats(){
		String result = "Total number of access requests for instruction cache: "+this.requests;
		String hits = "Number of instruction cache hits: "+this.hits;
		String finalResult = result+"\n"+hits;
		return finalResult;
	}
}
