import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

//abstract base class for any tradeable asset
abstract class Asset {
    protected String name;
    protected String symbol;

    public Asset(String name, String symbol) {
        this.name   = name;
        this.symbol = symbol;
    }

    public abstract double getPrice();

    public String getName()   { return name; }
    public String getSymbol() { return symbol; }
}

//Inheritance + Encapsulation — Crypto extends Asset
class Crypto extends Asset {

    private volatile double price;

    public Crypto(String name, String symbol, double price) {
        super(name, symbol);
        this.price = price;
    }

    // synchronized setter — thread-safe price update
    public synchronized void setPrice(double price) {
        if (price > 0) this.price = price;
    }

    @Override
    public double getPrice() { return price; }
}

//Encapsulation
//Serializable
class Portfolio implements Serializable {
    private static final long serialVersionUID = 1L;

    private double quantity      = 0;
    private double totalInvested = 0;
    private double bookedPL      = 0;

    public void buy(double amount, double price) {
        // Autoboxing/Unboxing — arithmetic uses unboxed doubles
        double qty = amount / price;
        quantity      += qty;
        totalInvested += amount;
    }

    public void buyByQty(double qty, double price) {
        double amount = qty * price;
        quantity      += qty;
        totalInvested += amount;
    }

    public void sell(double amount, double price) {
        double qty = amount / price;
        if (qty > quantity + 1e-9)
            throw new RuntimeException("Not enough coins to sell!");
        double avgCost   = (quantity > 0) ? totalInvested / quantity : 0;
        double costBasis = avgCost * qty;
        double saleValue = price  * qty;
        bookedPL         += saleValue - costBasis;
        totalInvested    -= costBasis;
        quantity         -= qty;
        if (quantity < 1e-9) { quantity = 0; totalInvested = 0; }
    }

    public void sellByQty(double qty, double price) {
        if (qty > quantity + 1e-9)
            throw new RuntimeException("Not enough coins to sell!");
        double avgCost   = (quantity > 0) ? totalInvested / quantity : 0;
        double costBasis = avgCost * qty;
        double saleValue = price  * qty;
        bookedPL         += saleValue - costBasis;
        totalInvested    -= costBasis;
        quantity         -= qty;
        if (quantity < 1e-9) { quantity = 0; totalInvested = 0; }
    }

    public double getQuantity()      { return quantity; }
    public double getTotalInvested() { return totalInvested; }
    public double getAvgCost()       { return quantity > 0 ? totalInvested / quantity : 0; }
    public double getBookedPL()      { return bookedPL; }
}

//Encapsulation — User class
//Serializable
//ArrayList and HashMap
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final HashMap<String, Portfolio> portfolios = new HashMap<>();
    private final HashMap<String, ArrayList<String>> transactions = new HashMap<>();

    public User(String name) { this.name = name; }

    public Portfolio getPortfolio(String crypto) {
        portfolios.putIfAbsent(crypto, new Portfolio());
        return portfolios.get(crypto);
    }

    public void addTransaction(String crypto, String tx) {
        transactions.putIfAbsent(crypto, new ArrayList<>());
        transactions.get(crypto).add(tx);
    }

    public ArrayList<String> getTransactions(String crypto) {
        return transactions.getOrDefault(crypto, new ArrayList<>());
    }

    public ArrayList<String> getAllTransactions() {
        ArrayList<String> all = new ArrayList<>();
        Iterator<Map.Entry<String, ArrayList<String>>> it =
                transactions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ArrayList<String>> entry = it.next();
            all.addAll(entry.getValue());
        }
        Collections.sort(all);
        Collections.reverse(all);
        return all;
    }

    public String getName() { return name; }

    public HashMap<String, Portfolio> getAllPortfolios() { return portfolios; }
}

//Encapsulation — TradingService
//synchronized methods
class TradingService {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    //synchronized — prevents race condition during buy/sell
    public synchronized void buy(User user, Crypto crypto, double amount) {
        user.getPortfolio(crypto.getName()).buy(amount, crypto.getPrice());
        user.addTransaction(crypto.getName(),
                String.format("[%s] %-8s BUY  Rs.%,.2f @ Rs.%,.2f  (+%.6f %s)",
                        LocalDateTime.now().format(FMT), crypto.getSymbol(),
                        amount, crypto.getPrice(),
                        amount / crypto.getPrice(), crypto.getSymbol()));
    }

    public synchronized void buyByQty(User user, Crypto crypto, double qty) {
        double amount = qty * crypto.getPrice();
        user.getPortfolio(crypto.getName()).buyByQty(qty, crypto.getPrice());
        user.addTransaction(crypto.getName(),
                String.format("[%s] %-8s BUY  %.6f %s @ Rs.%,.2f  (Rs.%,.2f)",
                        LocalDateTime.now().format(FMT), crypto.getSymbol(),
                        qty, crypto.getSymbol(), crypto.getPrice(), amount));
    }

    public synchronized void sell(User user, Crypto crypto, double amount) {
        user.getPortfolio(crypto.getName()).sell(amount, crypto.getPrice());
        user.addTransaction(crypto.getName(),
                String.format("[%s] %-8s SELL Rs.%,.2f @ Rs.%,.2f  (-%.6f %s)",
                        LocalDateTime.now().format(FMT), crypto.getSymbol(),
                        amount, crypto.getPrice(),
                        amount / crypto.getPrice(), crypto.getSymbol()));
    }

    public synchronized void sellByQty(User user, Crypto crypto, double qty) {
        double amount = qty * crypto.getPrice();
        user.getPortfolio(crypto.getName()).sellByQty(qty, crypto.getPrice());
        user.addTransaction(crypto.getName(),
                String.format("[%s] %-8s SELL %.6f %s @ Rs.%,.2f  (Rs.%,.2f)",
                        LocalDateTime.now().format(FMT), crypto.getSymbol(),
                        qty, crypto.getSymbol(), crypto.getPrice(), amount));
    }
}

//Thread — PriceUpdater extends Thread, uses sleep()
class PriceUpdater extends Thread {

    private final Crypto   crypto;
    private final Runnable onUpdate;

    // Double wrapper autoboxed in HashMap
    private static final HashMap<String, Double> VOLATILITY = new HashMap<>();
    static {
        VOLATILITY.put("Bitcoin",  500.0);
        VOLATILITY.put("Ethereum",  40.0);
        VOLATILITY.put("Solana",     3.0);
        VOLATILITY.put("Dogecoin",   0.003);
        VOLATILITY.put("Cardano",    0.008);
    }

    public PriceUpdater(Crypto crypto, Runnable onUpdate) {
        this.crypto   = crypto;
        this.onUpdate = onUpdate;
        setDaemon(true);
    }

    @Override
    public void run() {
        //Unboxing — Double unboxed to double
        double vol = VOLATILITY.getOrDefault(crypto.getName(), 1.0);
        while (true) {
            double change = (Math.random() - 0.5) * 2 * vol;
            crypto.setPrice(crypto.getPrice() + change);
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            if (onUpdate != null) onUpdate.run();
        }
    }
}

//Serialization / Deserialization
//FileHandler saves/loads a HashMap<String, User> (all users)
class FileHandler {
    private static final String FILE = "crypto_data.dat";

    // Serialize the entire users map to file
    public static void saveAll(HashMap<String, User> users) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Deserialize the entire users map from file
    @SuppressWarnings("unchecked")
    public static HashMap<String, User> loadAll() {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(FILE))) {
            return (HashMap<String, User>) ois.readObject();
        } catch (Exception e) {
            return new HashMap<>(); // return empty map if file not found
        }
    }
}

//Main GUI class
public class App7 extends Application {

    //Crypto data (parallel arrays)
    private static final String[] NAMES   = {"Bitcoin","Ethereum","Solana","Dogecoin","Cardano"};
    private static final String[] SYMBOLS = {"BTC","ETH","SOL","DOGE","ADA"};
    private static final double[] PRICES  = {50000, 3000, 120, 0.15, 0.45};

    private HashMap<String, User> allUsers = new HashMap<>();
    private final HashMap<String, Crypto> cryptos = new HashMap<>();

    private User           user;
    private TradingService service;
    private String         selectedCrypto = "Bitcoin";

    private Label    selectedCryptoLabel;
    private Label    priceLabel;
    private TextArea portfolioArea;
    private TextArea transactionArea;
    private VBox     cryptoButtonBox;
    private boolean  buyByQuantity = false;

    private Stage primaryStage;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        //Deserialize all users from file
        allUsers = FileHandler.loadAll();

        for (int i = 0; i < NAMES.length; i++)
            cryptos.put(NAMES[i], new Crypto(NAMES[i], SYMBOLS[i], PRICES[i]));

        service = new TradingService();

        for (Crypto c : cryptos.values())
            new PriceUpdater(c, () -> Platform.runLater(this::updateDisplay)).start();

        showLoginScene();
    }

    // LOGIN SCENE
    private void showLoginScene() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Multi-Crypto Trading Simulator");
        Label sub   = new Label("Select a user to login, or add a new user.");
        root.getChildren().addAll(title, new Separator(), sub);

        VBox userList = new VBox(8);
        userList.setAlignment(Pos.TOP_CENTER);
        rebuildUserList(userList);
        root.getChildren().add(userList);

        // "+ Add User" button
        Button addUserBtn = new Button("+ Add User");
        addUserBtn.setOnAction(e -> showAddUserDialog(userList));
        root.getChildren().add(addUserBtn);

        Scene loginScene = new Scene(root, 400, 450);
        primaryStage.setTitle("Login — Crypto Simulator");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private void rebuildUserList(VBox userList) {
        userList.getChildren().clear();
        if (allUsers.isEmpty()) {
            userList.getChildren().add(new Label("No users yet. Add a user to begin."));
            return;
        }
        int index = 1;
        Iterator<String> it = allUsers.keySet().iterator();
        while (it.hasNext()) {
            String username = it.next();
            Button btn = new Button("User " + index + " — " + username);
            btn.setPrefWidth(260);
            btn.setOnAction(e -> loginAs(username)); 
            userList.getChildren().add(btn);
            index++;
        }
    }

    // Dialog to type a new username
    private void showAddUserDialog(VBox userList) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add User");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter username:");
        dialog.showAndWait().ifPresent(name -> {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                showAlert("Username cannot be empty.");
                return;
            }
            if (allUsers.containsKey(trimmed)) {
                showAlert("User '" + trimmed + "' already exists.");
                return;
            }
            allUsers.put(trimmed, new User(trimmed));
            FileHandler.saveAll(allUsers);
            rebuildUserList(userList);
        });
    }

    private void loginAs(String username) {
        user           = allUsers.get(username);
        selectedCrypto = "Bitcoin";
        buyByQuantity  = false;
        showTradingScene();
    }

    // TRADING SCENE
    private void showTradingScene() {

        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(16));
        sidebar.setPrefWidth(170);

        Label userLabel = new Label("Logged in: " + user.getName());
        Label menuTitle = new Label("Select Crypto:");
        sidebar.getChildren().addAll(userLabel, new Separator(), menuTitle);

        cryptoButtonBox = new VBox(6);
        for (int i = 0; i < NAMES.length; i++) {
            String name = NAMES[i];
            String sym  = SYMBOLS[i];
            Button btn  = new Button(sym + "  " + name);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> selectCrypto(name));
            cryptoButtonBox.getChildren().add(btn);
        }
        sidebar.getChildren().add(cryptoButtonBox);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // "All Transactions" button
        Button allTxBtn = new Button("All Transactions");
        allTxBtn.setMaxWidth(Double.MAX_VALUE);
        allTxBtn.setOnAction(e -> showAllTransactionsWindow());

        // "Save Data" button
        Button saveBtn = new Button("Save Data");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            allUsers.put(user.getName(), user);
            FileHandler.saveAll(allUsers);
            showAlert("Data saved successfully!");
        });

        // "Logout" button — saves and returns to login screen
        Button logoutBtn = new Button("Logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            allUsers.put(user.getName(), user);
            FileHandler.saveAll(allUsers);
            showLoginScene();
        });

        sidebar.getChildren().addAll(allTxBtn, saveBtn, logoutBtn);

        //MAIN AREA
        VBox mainArea = new VBox(14);
        mainArea.setPadding(new Insets(20));
        VBox.setVgrow(mainArea, Priority.ALWAYS);

        // Header: selected crypto name and live price
        selectedCryptoLabel = new Label("Bitcoin (BTC)");
        priceLabel          = new Label("Rs. 50,000.00");
        HBox header = new HBox(20, selectedCryptoLabel, priceLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        // TRADE PANEL
        TitledPane tradePane = new TitledPane();
        tradePane.setText("Trade");
        tradePane.setCollapsible(false);

        // RadioButton + ToggleGroup — trade mode selection
        ToggleGroup modeGroup  = new ToggleGroup();
        RadioButton amountMode = new RadioButton("By Amount (Rs.)");
        RadioButton qtyMode    = new RadioButton("By Quantity (Coins)");
        amountMode.setToggleGroup(modeGroup);
        qtyMode.setToggleGroup(modeGroup);
        amountMode.setSelected(true);

        HBox modeBox = new HBox(20, amountMode, qtyMode);
        modeBox.setAlignment(Pos.CENTER_LEFT);

        TextField inputField = new TextField();
        inputField.setPromptText("Enter amount in Rs.");
        inputField.setPrefWidth(220);

        // RadioButton
        amountMode.setOnAction(e -> {
            buyByQuantity = false;
            inputField.setPromptText("Enter amount in Rs.");
            inputField.clear();
        });
        qtyMode.setOnAction(e -> {
            buyByQuantity = true;
            inputField.setPromptText("Enter quantity (coins)");
            inputField.clear();
        });

        Button buyBtn  = new Button("BUY");
        Button sellBtn = new Button("SELL");

        // BUY
        buyBtn.setOnAction(e -> {
            try {
                // Wrapper class — Double.parseDouble (unboxing)
                double val = Double.parseDouble(inputField.getText().trim());
                if (val <= 0) throw new NumberFormatException();
                Crypto c = cryptos.get(selectedCrypto);
                if (buyByQuantity)
                    service.buyByQty(user, c, val);
                else
                    service.buy(user, c, val);
                inputField.clear();
                updateDisplay();
            } catch (RuntimeException ex) {
                showAlert(ex.getMessage());
            } catch (Exception ex) {
                showAlert("Enter a valid positive value!");
            }
        });

        // SELL
        sellBtn.setOnAction(e -> {
            try {
                // Wrapper class — Double.parseDouble
                double val = Double.parseDouble(inputField.getText().trim());
                if (val <= 0) throw new NumberFormatException();
                Crypto c = cryptos.get(selectedCrypto);
                if (buyByQuantity)
                    service.sellByQty(user, c, val);
                else
                    service.sell(user, c, val);
                inputField.clear();
                updateDisplay();
            } catch (RuntimeException ex) {
                showAlert(ex.getMessage());
            } catch (Exception ex) {
                showAlert("Enter a valid positive value!");
            }
        });

        HBox tradeRow = new HBox(12, inputField, buyBtn, sellBtn);
        tradeRow.setAlignment(Pos.CENTER_LEFT);

        VBox tradeContent = new VBox(8, modeBox, tradeRow);
        tradeContent.setPadding(new Insets(10));
        tradePane.setContent(tradeContent);

        //TABS: Portfolio Summary + Per-Crypto Transactions
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab portfolioTab = new Tab("Portfolio Summary");
        portfolioArea = new TextArea();
        portfolioArea.setEditable(false);
        portfolioArea.setFont(javafx.scene.text.Font.font("Courier New", 12));
        portfolioTab.setContent(portfolioArea);

        Tab txTab = new Tab("Transactions (Selected Crypto)");
        transactionArea = new TextArea();
        transactionArea.setEditable(false);
        transactionArea.setFont(javafx.scene.text.Font.font("Courier New", 12));
        txTab.setContent(transactionArea);

        tabPane.getTabs().addAll(portfolioTab, txTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        mainArea.getChildren().addAll(header, tradePane, tabPane);

        HBox root = new HBox(sidebar, mainArea);
        HBox.setHgrow(mainArea, Priority.ALWAYS);

        updateDisplay();

        Scene tradingScene = new Scene(root, 950, 600);
        primaryStage.setTitle("Crypto Simulator — " + user.getName());
        primaryStage.setScene(tradingScene);
    }

    // ALL TRANSACTIONS WINDOW
    private void showAllTransactionsWindow() {
        ArrayList<String> all = user.getAllTransactions();

        TextArea area = new TextArea();
        area.setEditable(false);
        area.setFont(javafx.scene.text.Font.font("Courier New", 12));

        StringBuilder sb = new StringBuilder();
        sb.append("=== All Transactions for ").append(user.getName())
          .append(" ===\n\n");
        if (all.isEmpty()) {
            sb.append("  No transactions yet.\n");
        } else {
            for (String tx : all)
                sb.append(tx).append("\n");
        }
        area.setText(sb.toString());

        VBox root = new VBox(area);
        VBox.setVgrow(area, Priority.ALWAYS);

        Stage win = new Stage();
        win.setTitle("All Transactions — " + user.getName());
        win.setScene(new Scene(root, 700, 450));
        win.show();
    }

    private void selectCrypto(String name) {
        selectedCrypto = name;
        updateDisplay();
    }

    // Full display refresh — portfolio + transaction tabs
    private void updateDisplay() {
        if (user == null) return;
        Crypto crypto = cryptos.get(selectedCrypto);

        selectedCryptoLabel.setText(crypto.getName() + " (" + crypto.getSymbol() + ")");
        priceLabel.setText("Rs. " + String.format("%,.2f", crypto.getPrice()));

        //PORTFOLIO SUMMARY
        // Columns: Crypto | Sym | Qty | AvgCost | Invested | Cur.Value | Unreal P/L | Booked P/L
        StringBuilder pb = new StringBuilder();
        pb.append(String.format("%-10s %-4s %-12s %-12s %-12s %-12s %-12s %-12s%n",
                "Crypto", "Sym", "Qty", "AvgCost", "Invested",
                "Cur.Value", "Unreal P/L", "Booked P/L"));
        pb.append("-".repeat(94)).append("\n");

        // Iterator over HashMap entries
        double totalInvested = 0, totalValue = 0, totalBooked = 0;
        Iterator<Map.Entry<String, Crypto>> it = cryptos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Crypto> entry = it.next();
            Crypto    c   = entry.getValue();
            Portfolio p   = user.getPortfolio(c.getName());
            double curVal = p.getQuantity() * c.getPrice();
            double unreal = curVal - p.getTotalInvested();
            double booked = p.getBookedPL();
            totalInvested += p.getTotalInvested();
            totalValue    += curVal;
            totalBooked   += booked;
            pb.append(String.format("%-10s %-4s %-12.5f %-12.2f %-12.2f %-12.2f %s%-11.2f %s%.2f%n",
                    c.getName(), c.getSymbol(),
                    p.getQuantity(), p.getAvgCost(), p.getTotalInvested(),
                    curVal,
                    unreal >= 0 ? "+" : "", unreal,
                    booked >= 0 ? "+" : "", booked));
        }
        pb.append("-".repeat(94)).append("\n");
        double totalUnreal = totalValue - totalInvested;
        pb.append(String.format("%-10s %-4s %-12s %-12s %-12.2f %-12.2f %s%-11.2f %s%.2f%n",
                "TOTAL", "", "", "", totalInvested, totalValue,
                totalUnreal >= 0 ? "+" : "", totalUnreal,
                totalBooked >= 0 ? "+" : "", totalBooked));
        pb.append("\n  Unreal P/L = Current Value - Invested (open positions)\n");
        pb.append("  Booked P/L = Profit / Loss already realized from sells\n");
        portfolioArea.setText(pb.toString());

        // TRANSACTIONS for selected crypto only
        StringBuilder tb = new StringBuilder();
        tb.append("=== ").append(crypto.getName())
          .append(" (").append(crypto.getSymbol()).append(") Transactions ===\n\n");
        ArrayList<String> txList = user.getTransactions(crypto.getName());
        if (txList.isEmpty()) {
            tb.append("  No transactions yet.\n");
        } else {
            for (int i = txList.size() - 1; i >= 0; i--)
                tb.append(txList.get(i)).append("\n");
        }
        transactionArea.setText(tb.toString());
    }

    // Alert dialog
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Crypto Simulator");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}