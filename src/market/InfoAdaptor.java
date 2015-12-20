package market;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import market.InfoAdaptorTest.PriceDepthMessage;
import market.InfoAdaptorTest.PriceLevel;


public class InfoAdaptor implements MatchingEngine.Listener
{
	
	static class Order {
		int InstrumentId;
		double Price;
		int Volume;
		boolean IsBuy;
		int OrderId;
		public Order(int instrumentId, double price, int volume, boolean isBuy, int orderId)
		{
			InstrumentId = instrumentId;
			Price = price;
			Volume = volume;
			IsBuy = isBuy;
			OrderId = orderId;
		}
		@Override
		public String toString()
		{
			return "Order [InstrumentId=" + InstrumentId + ", Price=" + Price + ", Volume=" + Volume + ", IsBuy=" + IsBuy + ", OrderId=" + OrderId + "]";
		}
		
	}

	private final MatchingEngine me;
	private final InstrumentStore instrumentStore;

	private Map<Double, Order> price2Offers = new TreeMap<>();
	private Map<Double, Order> price2Bids= new TreeMap<>();
	
	private Map<Integer, Integer> instr2BidTarget = new HashMap<>();
	private Map<Integer, Integer> instr2AskTarget = new HashMap<>();
	
	public InfoAdaptor(final InstrumentStore instrumentStore, final MatchingEngine me)
	{
		this.instrumentStore = instrumentStore;
		this.me = me;
		
		me.attach(this);
	}

	public void OnDepth(int instrumentId, PriceDepthMessage m)
	{
		price2Offers = doDiff(instrumentId, m.Ask.iterator(), this.price2Offers.values().iterator(), false);
		price2Bids = doDiff(instrumentId, m.Bid.iterator(), this.price2Bids.values().iterator(), true);
	}

	private Map<Double, Order> doDiff(int instrumentId, Iterator<PriceLevel> current, Iterator<Order> previous, boolean isBuy)
	{
		
		Map<Double, Order> result = new TreeMap<>();
		Integer target = (isBuy ? instr2BidTarget.get(instrumentId) : instr2AskTarget.get(instrumentId));
		target = target == null ? 0 : target;
		
		
		while (current.hasNext() || previous.hasNext()) {
			PriceLevel newLevel = current.hasNext() ? current.next() : null;
			Order order = previous.hasNext() ? previous.next() : null;
			
			// apply targeting
			if (newLevel != null) {
				if (newLevel.Volume <= target) {
					target -= newLevel.Volume;
					continue;
				}
				
				if (newLevel.Volume > target && target > 0) {
					newLevel.Volume -= target;
					target = 0;
				}
			}
			
			// new price level
			if (order == null && newLevel != null) {
				int orderId = me.add(instrumentId, newLevel.Price, newLevel.Volume, isBuy);
				result.put(newLevel.Price, new Order(instrumentId, newLevel.Price, newLevel.Volume, isBuy, orderId));
			}
			
			// update, check if price matches, amend if same, delete / create if different
			if(order != null && newLevel != null) {
				if (order.Price == newLevel.Price) {
					me.amend(order.OrderId, newLevel.Volume);
				} 
				else 
				{
					me.remove(order.OrderId);
					int orderId = me.add(instrumentId, newLevel.Price, newLevel.Volume, isBuy);
					result.put(newLevel.Price, new Order(instrumentId, newLevel.Price, newLevel.Volume, isBuy, orderId));
				}
			}

			// clear price level
			if (order != null && newLevel == null) {
				me.remove(order.OrderId);
			}
			
		}
		
		return result;
	}

	@Override
	public void OnMatch(int instrId, double price, int volume, int bidId, int askId, boolean aggressorIsBuy)
	{
		System.out.println(String.format("match askId(%s) bidId(%s) price(%s) volume(%s) aggressor(%s)", new Object[] {
				askId, bidId, price, volume, aggressorIsBuy ? "Buy":"Sell"
		}));
		if (aggressorIsBuy) {
			Integer t = instr2AskTarget.get(instrId);
			t = (t == null ? 0 : t); 
			instr2AskTarget.put(instrId, t + volume);
		} else {
			Integer t = instr2BidTarget.get(instrId);
			t = t == null ? 0 : t; 
			instr2BidTarget.put(instrId, t + volume);
		}
		
	}
	
	public void OnTimer() {
		instr2AskTarget.clear();
		instr2BidTarget.clear();
	}

}
