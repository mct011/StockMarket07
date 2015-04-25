package pkg.trader;

import java.util.ArrayList;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.order.Order;
import pkg.order.OrderType;
import pkg.order.BuyOrder;
import pkg.order.SellOrder;

public class Trader {
	// Name of the trader
	String name;
	// Cash left in the trader's hand
	double cashInHand;
	// Stocks owned by the trader
	ArrayList<Order> position;
	// Orders placed by the trader
	ArrayList<Order> ordersPlaced;

	public Trader(String name, double cashInHand) {
		super();
		this.name = name;
		this.cashInHand = cashInHand;
		this.position = new ArrayList<Order>();
		this.ordersPlaced = new ArrayList<Order>();
	}

	public void buyFromBank(Market m, String symbol, int volume) throws StockMarketExpection {
		// Buy stock straight from the bank
		// Need not place the stock in the order list
		// Add it straight to the user's position
		// If the stock's price is larger than the cash possessed, then an
		// exception is thrown
		// Adjust cash possessed since the trader spent money to purchase a
		// stock.
		if(cashInHand < m.getStockForSymbol(symbol).getPrice() * volume)
			throw new StockMarketExpection("Cannot place order for stock: " + symbol + " since there is not enough money. Trader: \n" + name);
		BuyOrder buyOrder = new BuyOrder(symbol,volume, m.getStockForSymbol(symbol).getPrice(), this);
		position.add(buyOrder);
		cashInHand -= m.getStockForSymbol(symbol).getPrice() * volume;
	}

	public void placeNewOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Place a new order and add to the orderlist
		// Also enter the order into the orderbook of the market.
		// Note that no trade has been made yet. The order is in suspension
		// until a trade is triggered.
		//
		// If the stock's price is larger than the cash possessed, then an
		// exception is thrown
		// A trader cannot place two orders for the same stock, throw an
		// exception if there are multiple orders for the same stock.
		// Also a person cannot place a sell order for a stock that he does not
		// own. Or he cannot sell more stocks than he possesses. Throw an
		// exception in these cases.
		Order orderIn;
		if(orderType == OrderType.SELL)
		{
			 orderIn = new SellOrder(symbol,volume,price, this);
			 
			 if(duplicateOrder(orderIn))
					throw new StockMarketExpection("Cannot Place Order For Same Stock Twice");
			 
			 // if(!position.contains(orderIn)) | orderIn has different addr than actual order in position
			 // this would result in false every time
			 int index = ownsStock(symbol);
			 if(index < 0)
				 throw new StockMarketExpection("Don't Own This Stock");
			 Order currentOrder = position.get(index);
			 if(currentOrder.getSize() < volume)
				 throw new StockMarketExpection("You Do Not Currently Own That Many Shares");
			 ordersPlaced.add(orderIn);
			 m.addOrder(orderIn);
		}
		else // orderType == OrderType.BUY 
		{
			 orderIn = new BuyOrder(symbol,volume,price, this);
			 
			 if(duplicateOrder(orderIn))
					throw new StockMarketExpection("Cannot Place Order For Same Stock Twice");
			 
				if(m.getStockForSymbol(symbol).getPrice() * volume > cashInHand)
				{
					throw new StockMarketExpection("Cannot place order for stock: " + symbol + " since there is not enough money. Trader: \n" + name);
				}
				ordersPlaced.add(orderIn);
				m.addOrder(orderIn);
		}
	}

	public void placeNewMarketOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Similar to the other method, except the order is a market order
		
		Order orderIn;
		if(orderType == OrderType.SELL)
		{
			 orderIn = new SellOrder(symbol,volume, true, this);
			 
			 if(duplicateOrder(orderIn))
					throw new StockMarketExpection("Cannot Place Order For Same Stock Twice");
			 
			 // if(!position.contains(orderIn)) | orderIn has different addr than actual order in position
			 // this would result in false every time
			 int index = ownsStock(symbol);
			 if(index < 0)
				 throw new StockMarketExpection("Don't Own This Stock");
			 Order currentOrder = position.get(index);
			 if(currentOrder.getSize() < volume)
				 throw new StockMarketExpection("You Do Not Currently Own That Many Shares");
			 ordersPlaced.add(orderIn);
			 m.addOrder(orderIn);
		}
		else // orderType == OrderType.BUY 
		{
			 orderIn = new BuyOrder(symbol, volume, true, this);
			 
			 if(duplicateOrder(orderIn))
					throw new StockMarketExpection("Cannot Place Order For Same Stock Twice");
			 
				if(m.getStockForSymbol(symbol).getPrice() * volume > cashInHand)
				{
					throw new StockMarketExpection("Cannot place order for stock: " + symbol + " since there is not enough money. Trader: \n" + name);
				}
				ordersPlaced.add(orderIn);
				m.addOrder(orderIn);
		}
	}

	public void tradePerformed(Order o, double matchPrice)
			throws StockMarketExpection { //TODO: Where is SME thrown?
		
		// Notification received that a trade has been made, the parameters are
		// the order corresponding to the trade, and the match price calculated
		// in the order book. Note than an order can sell some of the stocks he
		// bought, etc. Or add more stocks of a kind to his position. Handle
		// these situations.

		// Update the trader's orderPlaced, position, and cashInHand members
		// based on the notification.
		if(o.getClass() == BuyOrder.class)	// if a buy order
		{
			cashInHand -= matchPrice * o.getSize();
			ordersPlaced.remove(findOrderBySymbol(o.getStockSymbol(), OrderType.BUY));
			
			int index = ownsStock(o.getStockSymbol());
			if(index >= 0)
			{
				Order currentStock = position.get(index);
				currentStock.setSize(currentStock.getSize() + o.getSize());
				position.set(index,currentStock);

			}
			else
				position.add(o);
		}
		else	// if a sell order
		{
			int index = ownsStock(o.getStockSymbol());
			cashInHand += matchPrice * o.getSize();
			if(o.getSize() < position.get(index).getSize())
			{
				Order currentStock = position.get(index);
				currentStock.setSize(currentStock.getSize() - o.getSize());
				position.set(index,currentStock);
			}
			else
				position.remove(index);
			
			index = findOrderBySymbol(o.getStockSymbol(), OrderType.SELL);
			ordersPlaced.remove(index);
		}
	}

	public void printTrader() {
		System.out.println("Trader Name: " + name);
		System.out.println("=====================");
		System.out.println("Cash: " + cashInHand);
		System.out.println("Stocks Owned: ");
		for (Order o : position) {
			o.printStockNameInOrder();
		}
		System.out.println("Stocks Desired: ");
		for (Order o : ordersPlaced) {
			o.printOrder();
		}
		System.out.println("+++++++++++++++++++++");
		System.out.println("+++++++++++++++++++++");
	}
	
	/** FOR TESTING PURPOSES ONLY **/
	public ArrayList<Order> getStocksOwned()
	{
		return position;
	}

	public ArrayList<Order> getOrdersPlaced()
	{
		return ordersPlaced;
	}
	
	public boolean duplicateOrder(Order orderIn)
	{
		boolean dup = false;
		Class searchType;
		if ((BuyOrder.class).isInstance(orderIn))
			searchType = BuyOrder.class;
		else
			searchType = SellOrder.class;
		
		for (Order o : ordersPlaced)
			if (o.getStockSymbol() == orderIn.getStockSymbol() && searchType.isInstance(o))
				dup = true;

		return dup;
	}
	
	public int ownsStock(String symbol)
	{
		boolean owns = false;
		int index = 0;
		for (Order o: position)
		{
			if (o.getStockSymbol() == symbol)
			{
				owns = true;
				break;
			}
			index++;
		}
		if (owns)
			return index;
		else
			return -1;
	}

	public int findOrderBySymbol(String symbol, OrderType type)
	{
		int index = -1;
		Class searchType;
		if (type == OrderType.BUY)
			searchType = BuyOrder.class;
		else	// type == OrderType.SELL
			searchType = SellOrder.class;
		
		int i = 0;
		while (i < ordersPlaced.size())
		{
			Order o = ordersPlaced.get(i);
			if (o.getStockSymbol() == symbol)
				index = i;
			i++;	
		}
		
		
		return index;
	}
	
	/** FOR TESTING PURPOSES ONLY **/
	public double getCashInHand() {
		return cashInHand;
	}
}
