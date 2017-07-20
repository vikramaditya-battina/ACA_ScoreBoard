package cache;
public class MemoryBus {
	private long busBusyTill;
	public MemoryBus(){
		busBusyTill = 0;
	}
	
	public boolean isBusBusy(long clockCount){
		if(busBusyTill > clockCount){
			return true;
		}
		return false;
	}
	
	public void updateBusBusyTill(long clockCount){
		busBusyTill = clockCount;
	}
	public long getBusBusyTill(){
		return busBusyTill;
	}
}
