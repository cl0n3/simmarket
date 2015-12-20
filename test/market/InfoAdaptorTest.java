package market;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class InfoAdaptorTest
{

	static class PriceLevel {
		double Price;
		int Volume;
		public PriceLevel(double price, int volume)
		{
			super();
			Price = price;
			Volume = volume;
		}
		
	}
	
	static class PriceDepthMessage {
		int InstrumentId;
		List<PriceLevel> Bid = new ArrayList<>(5);
		List<PriceLevel> Ask = new ArrayList<>(5);
	}
	
	MatchingEngine me = new MatchingEngine();
	MatchingEngineTest.StubMEListener l = new MatchingEngineTest.StubMEListener();
	InfoAdaptor adaptor = new InfoAdaptor(new InstrumentStore(), me);
	PriceDepthMessage m = new PriceDepthMessage();
	
	@Before
	public void before()
	{
		me.attach(l);
	}
	
	@Test
	public void shouldCreateOrdersFromInfo() 
	{
		m.Ask.add(new PriceLevel(101, 10));
		m.Ask.add(new PriceLevel(102, 10));
		m.Bid.add(new PriceLevel(100, 10));
		m.Bid.add(new PriceLevel(99, 10));
		adaptor.OnDepth(1, m);
		
		assertThat(l.getAskOrderCount(), is(2));
		assertTrue(l.hasOrder(1, false, 102, 10));
	}

	@Test
	public void shouldCreateClearFromInfo() 
	{
		m.Ask.add(new PriceLevel(101, 10));
		m.Ask.add(new PriceLevel(102, 10));
		m.Bid.add(new PriceLevel(100, 10));
		m.Bid.add(new PriceLevel(99, 10));
		adaptor.OnDepth(1, m);
		
		m.Ask.clear();
		m.Bid.clear();
		adaptor.OnDepth(1, m);
		
		assertThat(l.getAskOrderCount(), is(0));
		assertFalse(l.hasOrder(1, false, 102, 10));
		assertFalse(l.hasOrder(1, false, 101, 10));
		assertFalse(l.hasOrder(1, true, 100, 10));
		assertFalse(l.hasOrder(1, true, 99, 10));
	}
	

	@Test
	public void shouldAmendFromInfo() 
	{
		m.Ask.add(new PriceLevel(101, 10));
		m.Ask.add(new PriceLevel(102, 10));
		m.Bid.add(new PriceLevel(100, 10));
		m.Bid.add(new PriceLevel(99, 10));
		adaptor.OnDepth(1, m);
		m.Ask.clear();
		m.Bid.clear();
		m.Ask.add(new PriceLevel(100, 10));
		m.Ask.add(new PriceLevel(101, 10));
		m.Bid.add(new PriceLevel(99, 10));
		m.Bid.add(new PriceLevel(98, 10));
		adaptor.OnDepth(1, m);
		
		assertThat(l.getAskOrderCount(), is(2));
		assertThat(l.getBidOrderCount(), is(2));
		assertFalse(l.hasOrder(1, false, 102, 10));
		assertTrue(l.hasOrder(1, false, 101, 10));
		assertTrue(l.hasOrder(1, false, 100, 10));
		assertFalse(l.hasOrder(1, true, 100, 10));
		assertTrue(l.hasOrder(1, true, 99, 10));
		assertTrue(l.hasOrder(1, true, 98, 10));
	}
	
	@Test
	public void shouldTargetOnTrade()
	{
		m.Ask.add(new PriceLevel(101, 10));
		m.Ask.add(new PriceLevel(102, 10));
		m.Bid.add(new PriceLevel(100, 10));
		m.Bid.add(new PriceLevel(99, 10));
		adaptor.OnDepth(1, m);
		
		me.add(1, 100, 5, false);
		adaptor.OnDepth(1, m);
		
		assertTrue(l.hasOrder(1, false, 102, 10));
		assertTrue(l.hasOrder(1, false, 101, 10));
		assertTrue(l.hasOrder(1, true, 100, 5));
		assertTrue(l.hasOrder(1, true, 99, 10));

		adaptor.OnTimer();
		
		adaptor.OnDepth(1, m);
		assertTrue(l.hasOrder(1, false, 102, 10));
		assertTrue(l.hasOrder(1, false, 101, 10));
		assertTrue(l.hasOrder(1, true, 99, 10));
	}
}
