package market;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class MatchingEngine
{

	public interface Listener
	{
		void OnMatch(int instrId, double price, int volume, int bidId, int askId, boolean aggressorIsBuy);
		
		default void OnOrderAdd(int instrId, int orderId, double price, int volume, boolean isBuy) {}
		default void OnOrderRemove(int orderId) {}
		default void OnOrderAmend(int orderId, int volume) {}
	}

	private static class Order implements Comparable<Order> {
		int OrderId;
		int InstrumentId;
		double Price;
		int Volume;
		boolean Buy;
		
		public Order(int orderId, int instrumentId, double price, int volume, boolean buy)
		{
			OrderId = orderId;
			InstrumentId = instrumentId;
			Price = price;
			Volume = volume;
			Buy = buy;
		}
		
		
		
		@Override
		public String toString()
		{
			return "Order [OrderId=" + OrderId + ", InstrumentId=" + InstrumentId + ", Price=" + Price + ", Volume=" + Volume + ", Buy=" + Buy + "]";
		}



		boolean isFilled() { 
			return Volume == 0; 
		}
		
		@Override
		public int compareTo(Order rhs)
		{
			
			return (Buy ? -1 : 1) * Double.compare(this.Price, rhs.Price);
		}
	}
	
	static class BidAskPair<T> {
		T Bid;
		T Ask;
		@Override
		public String toString()
		{
			return "BidAskPair [Bid=" + Bid + ", Ask=" + Ask + "]";
		}
		
		
	}
	
	private final Map<Integer, BidAskPair<TreeSet<Order>>> instrumentOrders = new HashMap<Integer, BidAskPair<TreeSet<Order>>>();
	private final Map<Integer, Order> allOrders = new TreeMap<>();
	private final List<Listener> listeners = new LinkedList<Listener>();
	private int nextOrderId = 1;
	
	public int add(int instrId, double price, int volume, boolean isBuy)
	{
		BidAskPair<TreeSet<Order>> orders = instrumentOrders.get(instrId);
		if(orders == null)
		{
			orders = new BidAskPair<>();
			orders.Bid = new TreeSet<Order>();
			orders.Ask = new TreeSet<Order>();
			
			instrumentOrders.put(instrId, orders);
		}
		
		Order order = new Order(nextOrderId++, instrId, price, volume, isBuy);
		if(isBuy)
			orders.Bid.add(order);
		else
			orders.Ask.add(order);
		
		allOrders.put(order.OrderId, order);
		
		listeners.forEach(l -> { 
			l.OnOrderAdd(instrId, order.OrderId, order.Price, order.Volume, order.Buy);
		});

		
		match(instrId, isBuy, orders);
		
		return order.OrderId;
	}

	private void match(int instrId, final boolean aggressorIsBuy, BidAskPair<TreeSet<Order>> orders)
	{
		Order bid = orders.Bid.isEmpty() ? null : orders.Bid.first();
		Order ask = orders.Ask.isEmpty() ? null : orders.Ask.first();
		
		while(bid != null && ask != null &&
				bid.Price >= ask.Price) {
			
			int minVolume = Math.min(bid.Volume, ask.Volume);
			bid.Volume -= minVolume;
			ask.Volume -= minVolume;
			
			if(bid.isFilled()) {
				orders.Bid.remove(bid);
				allOrders.remove(bid.OrderId);	
			}
			if(ask.isFilled()) {
				orders.Ask.remove(ask);
				allOrders.remove(ask.OrderId);	
			}
			
			double price = aggressorIsBuy ? Math.min(bid.Price, ask.Price) : Math.max(bid.Price, ask.Price);
			final int bidId = bid.OrderId;
			final int askId = ask.OrderId;
			listeners.forEach(l -> { 
				l.OnMatch(instrId, price, minVolume, bidId, askId, aggressorIsBuy);
			});

			bid = orders.Bid.isEmpty() ? null : orders.Bid.first();
			ask = orders.Ask.isEmpty() ? null : orders.Ask.first();
		}
	}

	public void attach(Listener l)
	{
		this.listeners.add(l);
	}

	public boolean amend(int orderId, int volume)
	{
		Order o = allOrders.get(orderId);
		
		if (o == null)
			return false;
		
		if (volume <= 0)
			return false;
		
		o.Volume = volume;
		
		listeners.forEach(l -> { 
			l.OnOrderAmend(o.OrderId, o.Volume);
		});

		return true;
	}

	public boolean remove(int orderId)
	{
		Order o = allOrders.get(orderId);
		
		if (o == null)
			return false;

		BidAskPair<TreeSet<Order>> bidAskPair = instrumentOrders.get(o.InstrumentId);
		if (o.Buy)
			bidAskPair.Bid.remove(o);
		else
			bidAskPair.Ask.remove(o);

		allOrders.remove(o.OrderId);
		
		listeners.forEach(l -> { 
			l.OnOrderRemove(o.OrderId);
		});

		return true;
	}

}
