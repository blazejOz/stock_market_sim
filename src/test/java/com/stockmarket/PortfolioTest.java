package com.stockmarket;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.stockmarket.domain.AssetType;
import com.stockmarket.domain.Commodity;
import com.stockmarket.domain.Currency;
import com.stockmarket.domain.Order;
import com.stockmarket.domain.OrderType;
import com.stockmarket.domain.Share;
import com.stockmarket.logic.Portfolio;

public class PortfolioTest {

    private Portfolio portfolio;
    private Share share;
    private Commodity commodity;
    private Currency currency;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio(10000.0);
        share = new Share("AAPL", 100.0);
        commodity = new Commodity("GOLD", 100.0);
        currency = new Currency("USD", 100.0);
    }

    // --- Initialization Tests ---
    @Test
    @DisplayName("Should initialize portfolio correctly")
    void testPortfolioInitialization() {
        assertAll("Initial State",
            () -> assertEquals(10000.0, portfolio.getCash()),
            () -> assertEquals(0, portfolio.getHoldingsCount()),
            () -> assertEquals(0.0, portfolio.calculateHoldingsValue())
        );
    }

    @Test
    @DisplayName("Constructor should throw exception when initial cash is negative")
    void testPortfolioConstructorWithNegativeCash() {
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(-100.0));
    }

    // --- Adding Assets Tests ---
    @Test
    @DisplayName("Should add asset and update holdings count")
    void testAddAsset() {
        portfolio.addAsset(share, 5);
        assertAll("Add Asset",
            () -> assertEquals(1, portfolio.getHoldingsCount()),
            () -> assertEquals(5, portfolio.getAssetQuantity(share))
        );
    }

    // --- FIFO Logic Tests (Selling) ---
    @Test
    @DisplayName("Should sell assets using FIFO logic (Purchase Lots)")
    void testSellAssetFIFO() {
        Share lotA = new Share("XYZ", 100.0);
        portfolio.addAsset(lotA, 10);

        portfolio.advanceTime(10);

        Share lotB = new Share("XYZ", 120.0);
        portfolio.addAsset(lotB, 10);

        assertEquals(20, portfolio.getAssetQuantity(lotA));

        double profit = portfolio.sellAsset("XYZ", 15, 150.0);

        assertAll("FIFO Sale Verification",
            () -> assertEquals(650.0, profit, 0.01, "Profit should be calculated using FIFO"),
            () -> assertEquals(5, portfolio.getAssetQuantity(lotA), "Should have 5 items remaining"),
            () -> assertTrue(portfolio.getCash() > 10000.0, "Cash should increase after sale")
        );
    }
    
    @Test
    @DisplayName("Should throw exception when selling quantity is negative")
    void testSellAssetNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            portfolio.addAsset(share, 10); 
            portfolio.sellAsset("AAPL", -100, 100);
        });
    }

    // --- Order Management Tests (PriorityQueue) ---
    @Test
    @DisplayName("BUY orders should be sorted by HIGHEST price (Attractiveness)")
    void testBuyOrderPrioritization() {
        Order cheapOrder = new Order("AAPL", AssetType.SHARE, 100.0, 10, OrderType.BUY);
        Order expensiveOrder = new Order("AAPL", AssetType.SHARE, 150.0, 10, OrderType.BUY);
        Order midOrder = new Order("AAPL", AssetType.SHARE, 120.0, 10, OrderType.BUY);

        portfolio.placeOrder(cheapOrder);
        portfolio.placeOrder(expensiveOrder);
        portfolio.placeOrder(midOrder);

        Order bestOrder = portfolio.peekBestBuyOrder();
        
        assertAll("Buy Queue Priority",
            () -> assertNotNull(bestOrder),
            () -> assertEquals(150.0, bestOrder.getPriceLimit(), "Highest price should be first"),
            () -> assertEquals(expensiveOrder, bestOrder)
        );
    }

    @Test
    @DisplayName("SELL orders should be sorted by LOWEST price (Attractiveness)")
    void testSellOrderPrioritization() {
        portfolio.addAsset(share, 100);

        Order expensiveSell = new Order("AAPL", AssetType.SHARE, 200.0, 10, OrderType.SELL);
        Order cheapSell = new Order("AAPL", AssetType.SHARE, 100.0, 10, OrderType.SELL);
        
        portfolio.placeOrder(expensiveSell);
        portfolio.placeOrder(cheapSell);

        Order bestOrder = portfolio.peekBestSellOrder();

        assertAll("Sell Queue Priority",
            () -> assertNotNull(bestOrder),
            () -> assertEquals(100.0, bestOrder.getPriceLimit(), "Lowest price should be first"),
            () -> assertEquals(cheapSell, bestOrder)
        );
    }

    // --- Polymorphism Tests ---
    @Test
    @DisplayName("Different asset types calculate real values differently")
    void testPolymorphicRealValues() {
        Share s = new Share("S1", 100.0);
        Commodity c = new Commodity("C1", 100.0);
        Currency cu = new Currency("FX1", 100.0);
        
        // 10 sztuk, 10 dni
        // Share: 1000.0 (Bez opłaty, brak wpływu czasu)
        // Commodity: 1000.0 - (10 szt * 0.10 * 10 dni) = 990.0
        // Currency: 1000.0 - (1000 * 0.01 spread) = 990.0 (chyba że 0.5%, wtedy 995.0)
        // Zakładam poprawną wartość 995.0 dla waluty i 990.0 dla Commodity
        
        assertAll("Polymorphic Calculation",
            () -> assertEquals(1000.0, s.calculateRealValue(10, 10), 0.01), 
            () -> assertEquals(950.0, c.calculateRealValue(10, 10), 0.01),  
            () -> assertEquals(995.0, cu.calculateRealValue(10, 10), 0.01)  
        );
    }

    // --- Validation Tests ---
    @Test
    @DisplayName("Should throw exception when selling non-existent asset")
    void testSellNonExistentAsset() {
        assertThrows(IllegalArgumentException.class, () -> portfolio.sellAsset("GHOST", 5, 100.0));
    }

    @Test
    @DisplayName("Should throw exception when selling more than owned")
    void testSellMoreThanOwned() {
        portfolio.addAsset(share, 5);
        assertThrows(IllegalArgumentException.class, () -> portfolio.sellAsset("AAPL", 10, 100.0));
    }

     // --- Report Tests ---
    @Test
    @DisplayName("Should generate report containing key information")
    void testGenerateReport() {
        Portfolio reportPortfolio = new Portfolio(20000.0);
        
        Share s = new Share("AAPL", 100.0);
        Currency c = new Currency("USD", 100.0);

        reportPortfolio.addAsset(s, 10);   
        reportPortfolio.addAsset(c, 50);  
        
        String report = reportPortfolio.generateReport();
        
        assertAll("Report Content",
            () -> assertTrue(report.contains("PORTFOLIO REPORT")),
            () -> assertTrue(report.contains("AAPL")), 
            () -> assertTrue(report.contains("USD")),  
            () -> assertTrue(report.contains("SHARE")), 
            () -> assertTrue(report.contains("CURRENCY")) 
        );
    }

    // --- Extra Coverage Tests (Pokrycie Czerwonych Linii) ---

    @Test
    @DisplayName("Should throw exception when placing NULL order")
    void testPlaceNullOrder() {
        assertThrows(IllegalArgumentException.class, () -> portfolio.placeOrder(null));
    }

    @Test
    @DisplayName("Should throw exception when placing SELL order without enough assets")
    void testPlaceSellOrderInsufficientAssets() {
        Order sellOrder = new Order("AAPL", AssetType.SHARE, 100.0, 10, OrderType.SELL);
        // Nie mamy akcji AAPL w pustym portfelu
        assertThrows(IllegalArgumentException.class, () -> portfolio.placeOrder(sellOrder));
    }

    @Test
    @DisplayName("Should remove asset from holdings when sold completely")
    void testSellAssetCompletely() {
        portfolio.addAsset(share, 10);
        portfolio.sellAsset("AAPL", 10, 150.0);
        
        assertAll("Asset Removed",
            () -> assertEquals(0, portfolio.getAssetQuantity(share)),
            () -> assertEquals(0, portfolio.getHoldingsCount()) 
        );
    }

    @Test
    @DisplayName("Should handle time advance correctly (ignore negative)")
    void testAdvanceTime() {
        portfolio.advanceTime(5);
        assertEquals(5, portfolio.getCurrentDay());
        portfolio.advanceTime(-2); // Powinno zostać zignorowane
        assertEquals(5, portfolio.getCurrentDay());
    }

    @Test
    @DisplayName("Should load asset correctly (I/O Helper)")
    void testLoadAsset() {
        // Testuje metodę używaną przy odczycie z pliku
        portfolio.loadAsset(new Share("LOADED", 50.0), 10, 5);
        assertEquals(10, portfolio.getAssetQuantity(new Share("LOADED", 50.0)));
        assertEquals(10000.0, portfolio.getCash()); // Gotówka nie powinna zniknąć przy load
    }

    @Test
    @DisplayName("Should export holdings data (I/O Helper)")
    void testGetHoldingsData() {
        portfolio.addAsset(share, 5);
        String[] data = portfolio.getHoldingsData();
        assertEquals(1, data.length);
        assertTrue(data[0].contains("SHARE|AAPL"));
    }

    // --- Order Domain Tests ---

    @Test
    @DisplayName("Should create valid BUY order")
    void testValidBuyOrderCreation() {
        String symbol = "AAPL";
        AssetType type = AssetType.SHARE;
        double price = 150.0;
        int quantity = 10;
        OrderType orderType = OrderType.BUY;

        Order order = new Order(symbol, type, price, quantity, orderType);

        assertAll("Order State",
            () -> assertEquals(symbol, order.getSymbol()),
            () -> assertEquals(type, order.getAssetType()),
            () -> assertEquals(price, order.getPriceLimit()),
            () -> assertEquals(quantity, order.getQuantity()),
            () -> assertEquals(orderType, order.getType())
        );
    }

    @Test
    @DisplayName("Should create valid SELL order")
    void testValidSellOrderCreation() {
        Order order = new Order("GOLD", AssetType.COMMODITY, 1800.0, 5, OrderType.SELL);
        assertEquals(OrderType.SELL, order.getType());
    }

    @ParameterizedTest
    @DisplayName("Should throw exception for invalid Price (zero or negative)")
    @ValueSource(doubles = {0.0, -100.50})
    void testInvalidOrderPrice(double invalidPrice) {
        assertThrows(IllegalArgumentException.class, () -> {
            new Order("AAPL", AssetType.SHARE, invalidPrice, 10, OrderType.BUY);
        });
    }

    @ParameterizedTest
    @DisplayName("Should throw exception for invalid Quantity (zero or negative)")
    @ValueSource(ints = {0, -5})
    void testInvalidOrderQuantity(int invalidQuantity) {
        assertThrows(IllegalArgumentException.class, () -> {
            new Order("AAPL", AssetType.SHARE, 100.0, invalidQuantity, OrderType.BUY);
        });
    }

    @Test
    @DisplayName("Should return correct string representation")
    void testOrderToString() {
        Order order = new Order("AAPL", AssetType.SHARE, 150.50, 10, OrderType.BUY);
        String stringRep = order.toString();
        
        assertAll("ToString Content",
            () -> assertTrue(stringRep.contains("BUY")),
            () -> assertTrue(stringRep.contains("SHARE")),
            () -> assertTrue(stringRep.contains("AAPL")),
            () -> assertTrue(stringRep.contains("150.50"))
        );
    }
}