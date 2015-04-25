package pkg.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.market.api.PriceSetter;
import pkg.stock.Stock;

public class OrderBook
{
	Market m;
	HashMap<String, ArrayList<Order>> buyOrders;
	HashMap<String, ArrayList<Order>> sellOrders;

	public OrderBook(Market m)
	{
		this.m = m;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();
	}

	public void addToOrderBook(Order order)
	{
		Class buy = BuyOrder.class;
		Class sell = SellOrder.class;

		if (sell.isInstance(order))
		{
			SellOrder s = (SellOrder) order;
			// if symbol exists, pull out array and add to it, then put array in with symbol
			if (sellOrders.containsKey(s.getStockSymbol()))
			{
				ArrayList<Order> temp = sellOrders.get(s.getStockSymbol());
				temp.add(s);
				sellOrders.put(s.getStockSymbol(), temp);
			}
			// else put in order with key
			else
			{
				ArrayList<Order> temp = new ArrayList<Order>();
				temp.add(s);
				sellOrders.put(s.getStockSymbol(), temp);
			}
		}

		else if (buy.isInstance(order))
		{
			BuyOrder b = (BuyOrder) order;
			// if symbol exists, pull out array and add to it, then put array in with symbol
			if (buyOrders.containsKey(b.getStockSymbol()))
			{
				ArrayList<Order> temp = buyOrders.get(b.getStockSymbol());
				temp.add(b);
				buyOrders.put(b.getStockSymbol(), temp);
			}
			// else put in order with key
			else
			{
				ArrayList<Order> temp = new ArrayList<Order>();
				temp.add(b);
				buyOrders.put(b.getStockSymbol(), temp);
			}
		}
	}

	public void trade()
	{
		// 1. Follow and create the order book data representation

		// Retrieve all stocks within the market
		HashMap<String, Stock> stockList = m.getStockList();
		Set<String> stk = stockList.keySet();
		String[] stocks = stk.toArray(new String[stk.size()]);

		// String sym = stocks.iterator().next();
		// Was no iteration happening

		// Iterates through the OrderBook by stock
		for (String sym : stocks)
		{		
			// Separates orders for stock by order type
			ArrayList<Order> supply = sellOrders.get(sym);
			ArrayList<Order> demand = buyOrders.get(sym);

			// If no buy orders or sell orders for stock exist
			// then don't check for potential trades and move
			// to next stock
			if(supply != null && demand != null && supply.size() > 0 && demand.size() > 0)
			{
				// sorts orders by price from highest to lowest
				// String currentSym = supply.get(0).getStockSymbol();
				// Stock symbol is already gotten by loop invariant
				sortByPrice(supply);
				sortByPrice(demand);

				// reverses supply because potential sell order check
				// begins at the lowest price
				Collections.reverse(supply);

				// creates representation for cumulative size of
				// shares that compose potential trades
				int cumSupplySize = 0;
				int cumDemandSize = 0;

				// 2. Find the market price which maximizes buy/sell orders

				// get min share amount at each market price
				HashMap<Double, Integer> minSizeSupply = new HashMap<Double, Integer>();
				HashMap<Double, Integer> minSizeDemand = new HashMap<Double, Integer>();
				ArrayList<Double> prices = new ArrayList<Double>(); // Represents all prices which have at least one order
				int s = 0;
				int d = 0;
				while (s < supply.size())
				{
					Order temp = supply.get(s);

					if (minSizeSupply.containsKey(temp.getPrice()))
						minSizeSupply.put(temp.getPrice(), minSizeSupply.get(temp.getPrice()) + temp.getSize());
					else
						minSizeSupply.put(temp.getPrice(), temp.getSize() + cumSupplySize);

					cumSupplySize += temp.getSize();

					if (!prices.contains(temp.getPrice()))
						prices.add(temp.getPrice());

					s++;
				}

				while (d < demand.size())
				{
					Order temp = demand.get(d);

					if (minSizeDemand.containsKey(temp.getPrice()))
						minSizeDemand.put(temp.getPrice(), minSizeDemand.get(temp.getPrice()) + temp.getSize());
					else
						minSizeDemand.put(temp.getPrice(), temp.getSize() + cumDemandSize);

					cumDemandSize += temp.getSize();

					if (!prices.contains(temp.getPrice()))
						prices.add(temp.getPrice());

					d++;
				}

				// find out market price with highest min share amount
				int i = 0;
				double marketPrice = 0.0;
				int curMinShare = -1;
				while (i < prices.size())
				{
					if (minSizeSupply.containsKey(prices.get(i)) && minSizeDemand.containsKey(prices.get(i)))
					{
						int tempSupply = minSizeSupply.get(prices.get(i));
						int tempDemand = minSizeDemand.get(prices.get(i));

						if (Math.min(tempSupply, tempDemand) > curMinShare)
						{
							curMinShare = Math.min(tempSupply, tempDemand);
							marketPrice = prices.get(i);
						}
					}
					i++;
				}

				// 3. Update the stocks price in the market using the PriceSetter.

				PriceSetter ps = new PriceSetter();
				ps.registerObserver(m.getMarketHistory());
				m.getMarketHistory().setSubject(ps);
				ps.setNewPrice(m, sym, marketPrice);

				// 4. Delegate to trader that the trade has been made, so that the
				// trader's orders can be placed to his possession (a trader's position
				// is the stocks he owns)

				// If no matching price exists
				if (marketPrice == 0.0)
					marketPrice = calculateAverageMarketPrice(prices);
					
					// If a matching price exists
					i = 0;
				while (i < supply.size())
				{
					Order potentialOrder = supply.get(i);
					if (potentialOrder.getPrice() <= marketPrice)
					{
						try
						{
							(potentialOrder.getTrader()).tradePerformed(potentialOrder, marketPrice);
						}
						catch (StockMarketExpection e) {
							e.printStackTrace();
						}
						// 5. Remove the traded orders from the order book
						supply.remove(i);
					}			
					i++;
				}

				sellOrders.put(sym, supply);

				i = 0;
				while (i < demand.size())
				{
					Order potentialOrder = demand.get(i);
					if (potentialOrder.getPrice() >= marketPrice)
					{
						try
						{
							(potentialOrder.getTrader()).tradePerformed(potentialOrder, marketPrice);
						}
						catch (StockMarketExpection e) {
							e.printStackTrace();
						}
						// 5. Remove the traded orders from the order book
						demand.remove(i);
					}			
					i++;
				}

				buyOrders.put(sym, demand);
			}
		}
	}

	public HashMap<String, ArrayList<Order>> getSellOrders()
	{
		return sellOrders;
	}

	public HashMap<String, ArrayList<Order>> getBuyOrders()
	{
		return buyOrders;
	}

	// Sorts a collection of Orders by price in descending order
	private void sortByPrice(ArrayList<Order> orders)
	{
		int i = 0;

		while (i < orders.size())
		{
			int j = 0;
			while (j < orders.size() - 1)
			{
				Order tempTarg = orders.get(j);
				Order tempNext = orders.get(j+1);
				if (tempTarg.getPrice() < tempNext.getPrice())
				{
					Collections.swap(orders, j, j+1);
				}
				// ADDITION: added to remove infinite loop
				j++;
			}
			i++;
		}
	}

	// Finds the average of an ArrayList of double values
	private double calculateAverageMarketPrice(ArrayList<Double> list)
	{
		double sum = 0;
		for (double d : list)
			sum += d;
		double avg = sum / list.size();	
		return avg;
	}
}
