package market;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

public class MatchingEngineTest
{

	public static class StubMEListener implements MatchingEngine.Listener {

		static class Match {
			int InstrId;
			double Price;
			double Volume; 
			int BidId;
			int AskId;

			public Match(int instrId, double price, double volume, int bidId, int askId)
			{
				InstrId = instrId;
				Price = price;
				Volume = volume;
				BidId = bidId;
				AskId = askId;
			}

			@Override
			public String toString()
			{
				return "Match [InstrId=" + InstrId + ", Price=" + Price + ", Volume=" + Volume + ", BidId=" + BidId + ", AskId=" + AskId + "]";
			}
			
			
		}
		
		static class Order {
			int instrId;
			boolean isBuy;
			double Price;
			int Volume;
			int OrderId;
			public Order(int instrId, boolean isBuy, double price, int volume, int orderId)
			{
				super();
				this.instrId = instrId;
				this.isBuy = isBuy;
				Price = price;
				Volume = volume;
				OrderId = orderId;
			}
			@Override
			public String toString()
			{
				return "Order [instrId=" + instrId + ", isBuy=" + isBuy + ", Price=" + Price + ", Volume=" + Volume + ", OrderId=" + OrderId + "]";
			}
			
			
		}
		
		private List<Match> matches = new LinkedList<MatchingEngineTest.StubMEListener.Match>();
		private List<Order> asks = new LinkedList<>();
		private List<Order> bids = new LinkedList<>();
		
		@Override
		public void OnMatch(int instrId, double price, int volume, int bidId, int askId, boolean aggressorIsBuy)
		{
			matches.add(new Match(instrId, price, volume, bidId, askId));
		}
		
		void forEach(Consumer<Match> c) {
			matches.forEach(c);
		}

		public int getMatchCount()
		{
			return matches.size();
		}

		public int getAskOrderCount()
		{
			return asks.size();
		}
		public int getBidOrderCount()
		{
			return bids.size();
		}

		public boolean hasOrder(int instrId, boolean isBuy, double price, int volume)
		{
			if (isBuy) {
				for (Order order : bids) {
					if (order.instrId == instrId && order.Price == price && order.Volume == volume)
						return true;
				}
			}
			else
			{
				for (Order order : asks) {
					if (order.instrId == instrId && order.Price == price && order.Volume == volume)
						return true;
				}
			}
			
			return false;
		}

		@Override
		public void OnOrderAdd(int instrId, int orderId, double price, int volume, boolean isBuy)
		{
			Order o = new Order(instrId, isBuy, price, volume, orderId);
			if (isBuy) {
				bids.add(o);
			} else {
				asks.add(o);
			}
		}

		@Override
		public void OnOrderRemove(int orderId)
		{
			System.out.println(bids);
			System.out.println(asks);
			bids.removeIf(o -> { return o.OrderId == orderId; });
			asks.removeIf(o -> { return o.OrderId == orderId; });
			System.out.println("remove " + orderId);
			System.out.println(bids);
			System.out.println(asks);
			System.out.println("");
		}
		

		@Override
		public void OnOrderAmend(int orderId, int volume)
		{
			bids.forEach(o -> {
				if (o.OrderId == orderId)
					o.Volume = volume;
			});
			asks.forEach(o -> {
				if (o.OrderId == orderId)
					o.Volume = volume;
			});
		}
		
	}
	
	private final MatchingEngine matchingEngine = new MatchingEngine();
	
	private final StubMEListener l = new StubMEListener();
	
	@Before
	public void before()
	{
		matchingEngine.attach(l);
	}
	
	@Test
	public void shouldAllowOrderInsert()
	{
		assertTrue(matchingEngine.add(1, 100, 10, true) > 0);
	}
	
	@Test
	public void shouldRaiseMatchOnOrderInCrossForFullMatchAskAggressor()
	{
		assertTrue(matchingEngine.add(1, 100, 10, true) > 0);
		assertTrue(matchingEngine.add(1, 100, 10, false) > 0);
		
		assertThat(l.getMatchCount(), is(1));
		l.forEach(m -> {
			assertThat(m.Price, is(100.0));
			assertThat(m.Volume, is(10.0));
		});
	}
	
	@Test
	public void shouldRaiseMatchOnOrderInCrossForPartialMatchAskAggressor()
	{
		assertTrue(matchingEngine.add(1, 100, 10, true) > 0);
		assertTrue(matchingEngine.add(1, 100, 5, false) > 0);
		assertTrue(matchingEngine.add(1, 100, 5, false) > 0);
		
		assertThat(l.getMatchCount(), is(2));
		l.forEach(m -> {
			assertThat(m.Price, is(100.0));
			assertThat(m.Volume, is(5.0));
		});
	}

	@Test
	public void shouldRaiseMatchOnOrderPaythroughForFullMatchAskAggressor()
	{
		assertTrue(matchingEngine.add(1, 100, 10, true) > 0);
		assertTrue(matchingEngine.add(1, 99, 5, false) > 0);
		assertTrue(matchingEngine.add(1, 99, 5, false) > 0);
		
		assertThat(l.getMatchCount(), is(2));
		l.forEach(m -> {
			assertThat(m.Price, is(100.0));
			assertThat(m.Volume, is(5.0));
		});
	}
	
	@Test
	public void shouldRaiseMatchOnOrderInCrossForFullMatchBidAggressor()
	{
		assertTrue(matchingEngine.add(1, 100, 10, false) > 0);
		assertTrue(matchingEngine.add(1, 100, 10, true) > 0);
		
		assertThat(l.getMatchCount(), is(1));
		l.forEach(m -> {
			assertThat(m.Price, is(100.0));
			assertThat(m.Volume, is(10.0));
		});
	}
	
	@Test
	public void shouldRaiseMatchOnOrderInCrossForPartialMatchBidAggressor()
	{
		assertTrue(matchingEngine.add(1, 100, 10, false) > 0);
		assertTrue(matchingEngine.add(1, 100, 5, true) > 0);
		assertTrue(matchingEngine.add(1, 100, 5, true) > 0);
		
		assertThat(l.getMatchCount(), is(2));
		l.forEach(m -> {
			assertThat(m.Price, is(100.0));
			assertThat(m.Volume, is(5.0));
		});
	}

	@Test
	public void shouldRaiseMatchOnOrderPaythroughForFullMatchBidAggressor()
	{
		assertTrue(matchingEngine.add(1, 100, 10, false) > 0);
		assertTrue(matchingEngine.add(1, 101, 5, true) > 0);
		assertTrue(matchingEngine.add(1, 101, 5, true) > 0);
		
		assertThat(l.getMatchCount(), is(2));
		l.forEach(m -> {
			assertThat(m.Price, is(100.0));
			assertThat(m.Volume, is(5.0));
		});
	}
}
