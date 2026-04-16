import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * DashboardFrame1 – BBL Database Dashboard
 *
 * Features implemented (matching the Key Features slide):
 *  1. CRUD Operations   – Add / Edit / Delete dialogs for every table (ADMIN only)
 *  2. Player Search     – LIKE search across all performance tables
 *  3. Leaderboards      – Top batters, bowlers, keepers, partnerships
 *  4. All-Rounder Query – HAVING filter for players good at bat AND ball
 *  5. Audit Log         – PLAYER_AUDIT table (populated via BEFORE DELETE trigger)
 *  6. Role-Based Login  – constructor receives "ADMIN" or "VIEWER";
 *                         CRUD buttons hidden for VIEWER role
 *
 * DB Objects expected (Oracle):
 *   Tables  : PLAYER, TEAM, MATCH, BATTING, BOWLING, FIELDING,
 *             WICKETKEEPING, PARTNERSHIP, PLAYER_AUDIT, USER_LOGIN
 *   Procedure: get_team_wins(IN Team_ID VARCHAR2, OUT wins INTEGER)
 *   Functions: total_runs(Player_ID VARCHAR2) RETURN INTEGER
 *              total_wickets(Player_ID VARCHAR2) RETURN INTEGER
 *   Package  : player_pkg.get_runs(Player_ID), player_pkg.get_wickets(Player_ID)
 *   Trigger  : player_audit_trigger – BEFORE DELETE ON PLAYER → inserts into PLAYER_AUDIT
 */
public class DashboardFrame2 extends JFrame {

    private final String role;           // "ADMIN" or "VIEWER"
    private final boolean isAdmin;

    private static final Color BG_DARK     = new Color(8, 18, 35);
    private static final Color BG_MID      = new Color(14, 29, 52);
    private static final Color BG_PANEL    = new Color(21, 38, 66);
    private static final Color PANEL_EDGE  = new Color(44, 79, 124);
    private static final Color ACCENT      = new Color(0, 198, 133);
    private static final Color ACCENT_SOFT = new Color(4, 150, 105);
    private static final Color ACCENT_GOLD = new Color(246, 194, 72);
    private static final Color TEXT_MAIN   = new Color(236, 244, 255);
    private static final Color TEXT_DIM    = new Color(173, 194, 220);

    public DashboardFrame2(String role) {
        this.role    = role;
        this.isAdmin = "ADMIN".equalsIgnoreCase(role);

        setTitle("BBL Cricket Command Center  [" + role + "]");
        setSize(1120, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);

        buildUI();
    }

    // ════════════════════════════════════════════════════════════════════
    //  UI CONSTRUCTION
    // ════════════════════════════════════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),      BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_MID);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_SOFT),
            BorderFactory.createEmptyBorder(16, 22, 12, 22)
        ));

        JLabel title = new JLabel("🏏  BBL CRICKET COMMAND CENTER");
        title.setFont(new Font("Georgia", Font.BOLD, 22));
        title.setForeground(ACCENT);

        JLabel subtitle = new JLabel("Live stats, leaderboards, and match intelligence");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_DIM);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.WEST);

        JLabel roleLabel = new JLabel("Role: " + role);
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        roleLabel.setForeground(TEXT_MAIN);
        roleLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PANEL_EDGE, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        header.add(roleLabel, BorderLayout.EAST);

        JPanel stripe = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        stripe.setOpaque(false);
        stripe.add(infoChip("🔥 In Form"));
        stripe.add(infoChip("🎯 Smart Queries"));
        stripe.add(infoChip("📊 Deep Stats"));
        header.add(stripe, BorderLayout.SOUTH);

        return header;
    }

    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(14, 14));
        center.setBackground(BG_DARK);
        center.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        // Left column: Table buttons + CRUD
        center.add(buildLeftPanel(),  BorderLayout.WEST);
        // Right column: Feature buttons
        center.add(buildRightPanel(), BorderLayout.CENTER);

        return center;
    }

    // ── LEFT PANEL  (table view + CRUD) ─────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(8, 10));
        left.setBackground(BG_PANEL);
        left.setBorder(cardBorder());
        left.setPreferredSize(new Dimension(365, 0));

        // Section label
        left.add(sectionLabel("🏟️  VIEW TABLES"), BorderLayout.NORTH);

        // 3×3 grid of table buttons
        JPanel grid = new JPanel(new GridLayout(3, 3, 8, 8));
        grid.setBackground(BG_PANEL);

        addTableButton(grid, "PLAYER",        "SELECT * FROM PLAYER");
        addTableButton(grid, "TEAM",          "SELECT * FROM TEAM");
        addTableButton(grid, "MATCH",         "SELECT * FROM MATCH");
        addTableButton(grid, "BATTING",       "SELECT * FROM BATTING");
        addTableButton(grid, "BOWLING",       "SELECT * FROM BOWLING");
        addTableButton(grid, "FIELDING",      "SELECT * FROM FIELDING");
        addTableButton(grid, "WK-KEEPING",    "SELECT * FROM WICKETKEEPING");
        addTableButton(grid, "PARTNERSHIP",   "SELECT * FROM PARTNERSHIP");
        addTableButton(grid, "AUDIT LOG",     "SELECT * FROM PLAYER_AUDIT");

        left.add(grid, BorderLayout.CENTER);

        // CRUD panel (ADMIN only)
        if (isAdmin) {
            left.add(buildCrudPanel(), BorderLayout.SOUTH);
        }

        return left;
    }

    private JPanel buildCrudPanel() {
        JPanel p = new JPanel(new GridLayout(4, 1, 6, 6));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        p.add(sectionLabel("🛠️  CRUD OPERATIONS (ADMIN)"));

        JButton addBtn  = accentButton("➕ Add Player");
        JButton editBtn = accentButton("✏️ Edit Player");
        JButton delBtn  = dangerButton("🗑 Delete Player");

        p.add(addBtn);
        p.add(editBtn);
        p.add(delBtn);

        addBtn .addActionListener(e -> showAddPlayerDialog());
        editBtn.addActionListener(e -> showEditPlayerDialog());
        delBtn .addActionListener(e -> showDeletePlayerDialog());

        return p;
    }

    // ── RIGHT PANEL  (features) ─────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 12));
        right.setBackground(BG_PANEL);
        right.setBorder(cardBorder());

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(sectionLabel("🏆  FEATURES & ANALYTICS"), BorderLayout.NORTH);
        top.add(cricketHeroCard(), BorderLayout.CENTER);
        right.add(top, BorderLayout.NORTH);

        JPanel features = new JPanel(new GridLayout(0, 1, 0, 10));
        features.setBackground(BG_PANEL);

        JPanel lbRow = featureCard("🏅 Leaderboards",
                "Top Batters | Top Bowlers | Top Keepers | Top Partnerships");
        JButton lbBtn = smallButton("Show All");
        lbBtn.addActionListener(e -> showLeaderboards());
        lbRow.add(lbBtn, BorderLayout.EAST);
        features.add(lbRow);

        JPanel srRow = featureCard("🔎 Player Search",
                "Search by name (LIKE) – full profile across all stat tables");
        JButton srBtn = smallButton("Search");
        srBtn.addActionListener(e -> showPlayerSearch());
        srRow.add(srBtn, BorderLayout.EAST);
        features.add(srRow);

        JPanel arRow = featureCard("⚡ All-Rounder Query",
                "HAVING filter: players excelling in both bat & ball");
        JButton arBtn = smallButton("Run");
        arBtn.addActionListener(e -> showAllRounders());
        arRow.add(arBtn, BorderLayout.EAST);
        features.add(arRow);

        JPanel prRow = featureCard("🧠 Stored Procedure",
                "get_team_wins – called with Team_ID ");
        JButton prBtn = smallButton("Execute");
        prBtn.addActionListener(e -> runProcedure());
        prRow.add(prBtn, BorderLayout.EAST);
        features.add(prRow);

        JPanel fnRow = featureCard("📐 Functions",
                "total_runs & total_wickets – called with Player_ID ");
        JButton fnBtn = smallButton("Execute");
        fnBtn.addActionListener(e -> runFunctions());
        fnRow.add(fnBtn, BorderLayout.EAST);
        features.add(fnRow);

        JPanel trRow = featureCard("🚨 Trigger Demo",
                "BEFORE DELETE trigger → auto-populates PLAYER_AUDIT");
        JButton trBtn = dangerButton("Execute");
        trBtn.setPreferredSize(new Dimension(92, 30));
        if (!isAdmin) trBtn.setEnabled(false);
        trBtn.addActionListener(e -> runTriggerDemo());
        trRow.add(trBtn, BorderLayout.EAST);
        features.add(trRow);

        JPanel pkRow = featureCard("📦 Package",
                "player_pkg.get_runs & get_wickets – called with Player_ID ");
        JButton pkBtn = smallButton("Execute");
        pkBtn.addActionListener(e -> runPackage());
        pkRow.add(pkBtn, BorderLayout.EAST);
        features.add(pkRow);

        JScrollPane scroll = new JScrollPane(features);
        scroll.setBackground(BG_PANEL);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        right.add(scroll, BorderLayout.CENTER);
        return right;
    }

    // ════════════════════════════════════════════════════════════════════
    //  FEATURE IMPLEMENTATIONS
    // ════════════════════════════════════════════════════════════════════

    // ── 1. LEADERBOARDS ─────────────────────────────────────────────────
    private void showLeaderboards() {
        runQuery(
            "SELECT p.Player_Name, SUM(b.Runs) AS Total_Runs " +
            "FROM PLAYER p JOIN BATTING b ON p.Player_ID = b.Player_ID " +
            "GROUP BY p.Player_Name ORDER BY Total_Runs DESC FETCH FIRST 5 ROWS ONLY",
            "Top Run-Scorers");

        runQuery(
            "SELECT p.Player_Name, SUM(bw.Wickets) AS Total_Wickets " +
            "FROM PLAYER p JOIN BOWLING bw ON p.Player_ID = bw.Player_ID " +
            "GROUP BY p.Player_Name ORDER BY Total_Wickets DESC FETCH FIRST 5 ROWS ONLY",
            "Top Wicket-Takers");

        runQuery(
            "SELECT p.Player_Name, SUM(NVL(wk.Catches,0) + NVL(wk.Stumpings,0)) AS Total_Dismissals " +
            "FROM PLAYER p JOIN WICKETKEEPING wk ON p.Player_ID = wk.Player_ID " +
            "GROUP BY p.Player_Name ORDER BY Total_Dismissals DESC FETCH FIRST 5 ROWS ONLY",
            "Top Wicket-Keepers");

        runQuery(
            "SELECT p1.Player_Name AS Partner1, p2.Player_Name AS Partner2, pt.Runs " +
            "FROM PARTNERSHIP pt " +
            "JOIN PLAYER p1 ON pt.Player1_ID = p1.Player_ID " +
            "JOIN PLAYER p2 ON pt.Player2_ID = p2.Player_ID " +
            "ORDER BY pt.Runs DESC FETCH FIRST 5 ROWS ONLY",
            "Top Partnerships");
    }

    // ── 2. PLAYER SEARCH ────────────────────────────────────────────────
    private void showPlayerSearch() {
        String name = JOptionPane.showInputDialog(this,
                "Enter player name to search (partial name OK):",
                "Player Search", JOptionPane.QUESTION_MESSAGE);

        if (name == null || name.trim().isEmpty()) return;

        // Basic profile
        runQuery(
            "SELECT * FROM PLAYER WHERE UPPER(Player_Name) LIKE UPPER('%" + name.trim() + "%')",
            "Player Profile – " + name);

        // Batting stats
        runQuery(
            "SELECT p.Player_Name, b.* FROM PLAYER p " +
            "JOIN BATTING b ON p.Player_ID = b.Player_ID " +
            "WHERE UPPER(p.Player_Name) LIKE UPPER('%" + name.trim() + "%')",
            "Batting Stats – " + name);

        // Bowling stats
        runQuery(
            "SELECT p.Player_Name, bw.* FROM PLAYER p " +
            "JOIN BOWLING bw ON p.Player_ID = bw.Player_ID " +
            "WHERE UPPER(p.Player_Name) LIKE UPPER('%" + name.trim() + "%')",
            "Bowling Stats – " + name);

        // Fielding stats
        runQuery(
            "SELECT p.Player_Name, f.* FROM PLAYER p " +
            "JOIN FIELDING f ON p.Player_ID = f.Player_ID " +
            "WHERE UPPER(p.Player_Name) LIKE UPPER('%" + name.trim() + "%')",
            "Fielding Stats – " + name);
    }

    // ── 3. ALL-ROUNDER QUERY ─────────────────────────────────────────────
    private void showAllRounders() {
        runQuery(
            "SELECT p.Player_Name, " +
            "       SUM(b.Runs)      AS Total_Runs, " +
            "       SUM(bw.Wickets)  AS Total_Wickets " +
            "FROM   PLAYER p " +
            "JOIN   BATTING  b  ON p.Player_ID = b.Player_ID " +
            "JOIN   BOWLING  bw ON p.Player_ID = bw.Player_ID " +
            "GROUP BY p.Player_Name " +
            "HAVING SUM(b.Runs) >= 100 AND SUM(bw.Wickets) >= 3 " +
            "ORDER BY Total_Runs DESC",
            "All-Rounders (≥100 Runs & ≥3 Wickets)");
    }

    // ── 4. STORED PROCEDURE ─────────────────────────────────────────────
    private void runProcedure() {
        String teamId = JOptionPane.showInputDialog(this,
                "Enter Team ID:", "T001");
        if (teamId == null || teamId.trim().isEmpty()) return;

        try {
            Connection c = DBConfig2.getConnection();
            CallableStatement cs = c.prepareCall("{call get_team_wins(?, ?)}");
            cs.setString(1, teamId.trim().toUpperCase());
            cs.registerOutParameter(2, Types.INTEGER);
            cs.execute();

            JOptionPane.showMessageDialog(this,
                    "Team " + teamId.trim().toUpperCase() + " → Wins: " + cs.getInt(2),
                    "Procedure: get_team_wins", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Procedure Error", ex);
        }
    }

    // ── 5. FUNCTIONS ────────────────────────────────────────────────────
    private void runFunctions() {
        String pid = JOptionPane.showInputDialog(this,
                "Enter Player ID:", "P001");
        if (pid == null || pid.trim().isEmpty()) return;

        try {
            Connection c = DBConfig2.getConnection();

            CallableStatement cs1 = c.prepareCall("{? = call total_runs(?)}");
            cs1.registerOutParameter(1, Types.INTEGER);
            cs1.setString(2, pid.trim().toUpperCase());
            cs1.execute();
            int runs = cs1.getInt(1);

            CallableStatement cs2 = c.prepareCall("{? = call total_wickets(?)}");
            cs2.registerOutParameter(1, Types.INTEGER);
            cs2.setString(2, pid.trim().toUpperCase());
            cs2.execute();
            int wickets = cs2.getInt(1);

            JOptionPane.showMessageDialog(this,
                    "Player " + pid.trim().toUpperCase() +
                    "\nTotal Runs   : " + runs +
                    "\nTotal Wickets: " + wickets,
                    "Function Output", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Function Error", ex);
        }
    }

    // ── 6. TRIGGER DEMO ─────────────────────────────────────────────────
    private void runTriggerDemo() {
        String pid = JOptionPane.showInputDialog(this,
                "Enter Player ID to DELETE (triggers audit log):", "P010");
        if (pid == null || pid.trim().isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "DELETE Player " + pid.trim().toUpperCase() + "? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Connection c = DBConfig2.getConnection();
            PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM PLAYER WHERE Player_ID = ?");
            ps.setString(1, pid.trim().toUpperCase());
            int rows = ps.executeUpdate();

            if (rows == 0) {
                JOptionPane.showMessageDialog(this,
                        "No player found with ID: " + pid.trim().toUpperCase(),
                        "Not Found", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Player " + pid.trim().toUpperCase() +
                        " deleted.\nThe BEFORE DELETE trigger has logged this action.\n" +
                        "Click  AUDIT LOG  to verify.",
                        "Trigger Executed", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            showError("Trigger/Delete Error", ex);
        }
    }

    // ── 7. PACKAGE ──────────────────────────────────────────────────────
    private void runPackage() {
        String pid = JOptionPane.showInputDialog(this,
                "Enter Player ID:", "P001");
        if (pid == null || pid.trim().isEmpty()) return;

        try {
            Connection c = DBConfig2.getConnection();

            CallableStatement cs1 = c.prepareCall("{? = call player_pkg.get_runs(?)}");
            cs1.registerOutParameter(1, Types.INTEGER);
            cs1.setString(2, pid.trim().toUpperCase());
            cs1.execute();

            CallableStatement cs2 = c.prepareCall("{? = call player_pkg.get_wickets(?)}");
            cs2.registerOutParameter(1, Types.INTEGER);
            cs2.setString(2, pid.trim().toUpperCase());
            cs2.execute();

            JOptionPane.showMessageDialog(this,
                    "Package: player_pkg\nPlayer: " + pid.trim().toUpperCase() +
                    "\nRuns   : " + cs1.getInt(1) +
                    "\nWickets: " + cs2.getInt(1),
                    "Package Output", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Package Error", ex);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  CRUD DIALOGS  (ADMIN only)
    // ════════════════════════════════════════════════════════════════════

    private void showAddPlayerDialog() {
        JTextField[] fields = new JTextField[2];
        String[]     labels = {"Player ID:", "Player Name:"};
        JPanel panel = labeledFields(labels, fields);

        int res = JOptionPane.showConfirmDialog(this, panel,
                "Add New Player", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            Connection c = DBConfig2.getConnection();
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO PLAYER (Player_ID, Player_Name) VALUES (?, ?)");
            ps.setString(1, fields[0].getText().trim().toUpperCase());
            ps.setString(2, fields[1].getText().trim());
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Player added successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Insert Error", ex);
        }
    }

    private void showEditPlayerDialog() {
        String pid = JOptionPane.showInputDialog(this,
                "Enter Player ID to edit:", "P001");
        if (pid == null || pid.trim().isEmpty()) return;

        try {
            Connection c = DBConfig2.getConnection();
            PreparedStatement sel = c.prepareStatement(
                "SELECT Player_Name FROM PLAYER WHERE Player_ID = ?");
            sel.setString(1, pid.trim().toUpperCase());
            ResultSet rs = sel.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Player not found.", "Not Found",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            JTextField[] fields = {
                new JTextField(rs.getString("Player_Name"))
            };
            String[] labels = {"Player Name:"};
            JPanel panel = labeledFields(labels, fields);

            int res = JOptionPane.showConfirmDialog(this, panel,
                    "Edit Player – " + pid.trim().toUpperCase(), JOptionPane.OK_CANCEL_OPTION);
            if (res != JOptionPane.OK_OPTION) return;

            PreparedStatement upd = c.prepareStatement(
                    "UPDATE PLAYER SET Player_Name=? WHERE Player_ID=?");
            upd.setString(1, fields[0].getText().trim());
                upd.setString(2, pid.trim().toUpperCase());
            upd.executeUpdate();

            JOptionPane.showMessageDialog(this, "Player updated successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Update Error", ex);
        }
    }

    private void showDeletePlayerDialog() {
        String pid = JOptionPane.showInputDialog(this,
                "Enter Player ID to delete:", "P010");
        if (pid == null || pid.trim().isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete player " + pid.trim().toUpperCase() + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Connection c = DBConfig2.getConnection();
            PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM PLAYER WHERE Player_ID = ?");
            ps.setString(1, pid.trim().toUpperCase());
            int rows = ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    rows > 0 ? "Player deleted. Audit log updated."
                             : "No player found with that ID.",
                    "Delete Result", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Delete Error", ex);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════════════════════════

    private void addTableButton(JPanel panel, String name, String query) {
        JButton btn = new JButton(name);
        btn.setBackground(new Color(16, 31, 54));
        btn.setForeground(TEXT_MAIN);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(PANEL_EDGE));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(27, 48, 80)); }
            public void mouseExited (MouseEvent e) { btn.setBackground(new Color(16, 31, 54)); }
        });
        panel.add(btn);
        btn.addActionListener(e -> runQuery(query, name));
    }

    private void runQuery(String sql, String title) {
        try {
            Connection c  = DBConfig2.getConnection();
            Statement  st = c.createStatement();
            ResultSet  rs = st.executeQuery(sql);
            showTable(rs, title);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Query error:\n" + ex.getMessage(),
                    title + " – Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTable(ResultSet rs, String title) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        String[] colNames = new String[cols];
        for (int i = 1; i <= cols; i++) colNames[i - 1] = meta.getColumnName(i);

        ArrayList<Object[]> data = new ArrayList<>();
        while (rs.next()) {
            Object[] row = new Object[cols];
            for (int i = 1; i <= cols; i++) row[i - 1] = rs.getObject(i);
            data.add(row);
        }

        JTable table = new JTable(data.toArray(new Object[0][]), colNames);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(24);
        table.setGridColor(new Color(66, 84, 107));
        table.setSelectionBackground(new Color(0, 145, 103));
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(20, 44, 77));
        table.getTableHeader().setForeground(new Color(236, 244, 255));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(700, 350));
        sp.getViewport().setBackground(new Color(237, 242, 248));

        JOptionPane.showMessageDialog(this, sp, title, JOptionPane.PLAIN_MESSAGE);
    }

    private void showError(String context, Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,
                context + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── UI helpers ──────────────────────────────────────────────────────

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(ACCENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return lbl;
    }

    private JButton accentButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(ACCENT);
        btn.setForeground(new Color(8, 26, 35));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(9, 218, 150)); }
            public void mouseExited (MouseEvent e) { btn.setBackground(ACCENT); }
        });
        return btn;
    }

    private JButton dangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(199, 66, 66));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(224, 83, 83)); }
            public void mouseExited (MouseEvent e) { btn.setBackground(new Color(199, 66, 66)); }
        });
        return btn;
    }

    private JLabel infoChip(String text) {
        JLabel chip = new JLabel(text);
        chip.setFont(new Font("Segoe UI", Font.BOLD, 11));
        chip.setForeground(new Color(13, 30, 50));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(208, 167, 53), 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        chip.setOpaque(true);
        chip.setBackground(ACCENT_GOLD);
        return chip;
    }

    private JPanel cricketHeroCard() {
        JPanel hero = new JPanel(new BorderLayout());
        hero.setOpaque(true);
        hero.setBackground(new Color(16, 33, 56));
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(58, 102, 157), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel line1 = new JLabel("🏏 Match Day Mode: ON");
        line1.setFont(new Font("Segoe UI", Font.BOLD, 13));
        line1.setForeground(new Color(236, 244, 255));

        JLabel line2 = new JLabel("Track leaders, form trends, and clutch partnerships in one place");
        line2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        line2.setForeground(TEXT_DIM);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(line1);
        text.add(line2);

        JLabel icon = new JLabel("🏟️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        icon.setForeground(ACCENT_GOLD);

        hero.add(text, BorderLayout.CENTER);
        hero.add(icon, BorderLayout.EAST);
        return hero;
    }

    private JButton smallButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(ACCENT);
        btn.setForeground(new Color(8, 26, 35));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(92, 30));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(9, 218, 150)); }
            public void mouseExited (MouseEvent e) { btn.setBackground(ACCENT); }
        });
        return btn;
    }

    /**
     * A card-like row panel for a feature with an icon-title on the left
     * and room for a button on the right.
     */
    private JPanel featureCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBackground(BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_EDGE, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 10)));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setBackground(BG_PANEL);

        JLabel tl = new JLabel(title);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tl.setForeground(TEXT_MAIN);

        JLabel sl = new JLabel(subtitle);
        sl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sl.setForeground(TEXT_DIM);

        text.add(tl);
        text.add(sl);
        card.add(text, BorderLayout.CENTER);

        return card;
    }

    private javax.swing.border.Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PANEL_EDGE, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );
    }

    private JPanel labeledFields(String[] labels, JTextField[] fields) {
        JPanel p = new JPanel(new GridLayout(labels.length, 2, 6, 6));
        for (int i = 0; i < labels.length; i++) {
            p.add(new JLabel(labels[i]));
            if (fields[i] == null) fields[i] = new JTextField(16);
            p.add(fields[i]);
        }
        return p;
    }
}
