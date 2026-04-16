import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.sql.*;
import javax.swing.*;

/**
 * LoginFrame1 – Role-Based Login
 *
 * Reads the USER_LOGIN table which must have columns:
 *   Username  VARCHAR2
 *   Password  VARCHAR2
 *   Role      VARCHAR2  ('ADMIN' | 'VIEWER')
 *
 * ADMIN  → full CRUD access in DashboardFrame1
 * VIEWER → read-only (no add / edit / delete buttons shown)
 */
public class LoginFrame2 extends JFrame {

    private static final Color BG_TOP      = new Color(8, 52, 32);
    private static final Color BG_BOTTOM   = new Color(7, 24, 46);
    private static final Color CARD_BG     = new Color(17, 38, 61);
    private static final Color INPUT_BG    = new Color(13, 26, 44);
    private static final Color BORDER      = new Color(48, 86, 128);
    private static final Color ACCENT      = new Color(0, 198, 133);
    private static final Color ACCENT_HOVER= new Color(7, 220, 151);
    private static final Color GOLD        = new Color(246, 194, 72);
    private static final Color TEXT_MAIN   = new Color(236, 244, 255);
    private static final Color TEXT_SUB    = new Color(166, 188, 214);

    private JTextField     txtUser;
    private JPasswordField txtPass;

    public LoginFrame2() {
        setTitle("BBL Match Center Login");
        setSize(460, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel outer = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(18, 24, 18, 24)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 0, 6, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx = 0;
        gc.gridy = 0;

        JLabel header = new JLabel("🏏  BBL MATCH CENTER", JLabel.CENTER);
        header.setFont(new Font("Georgia", Font.BOLD, 20));
        header.setForeground(ACCENT);
        card.add(header, gc);

        gc.gridy++;
        JLabel subHeader = new JLabel("Stadium Access | Secure Role Login", JLabel.CENTER);
        subHeader.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subHeader.setForeground(TEXT_SUB);
        subHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        card.add(subHeader, gc);

        gc.gridy++;
        JLabel imageBand = new JLabel("⚾  Live Score Deck  ⚡", JLabel.CENTER);
        imageBand.setFont(new Font("Segoe UI", Font.BOLD, 11));
        imageBand.setOpaque(true);
        imageBand.setBackground(new Color(13, 31, 51));
        imageBand.setForeground(GOLD);
        imageBand.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(79, 121, 170), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        card.add(imageBand, gc);

        gc.gridy++;
        JLabel lblUser = new JLabel("Username");
        lblUser.setForeground(TEXT_MAIN);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        card.add(lblUser, gc);

        gc.gridy++;
        txtUser = new JTextField(18);
        styleInput(txtUser);
        card.add(txtUser, gc);

        gc.gridy++;
        JLabel lblPass = new JLabel("Password");
        lblPass.setForeground(TEXT_MAIN);
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPass.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        card.add(lblPass, gc);

        gc.gridy++;
        txtPass = new JPasswordField(18);
        styleInput(txtPass);
        card.add(txtPass, gc);

        gc.gridy++;
        gc.insets = new Insets(14, 0, 0, 0);
        JButton btnLogin = createPrimaryButton("Sign In");
        card.add(btnLogin, gc);

        gc.gridy++;
        gc.insets = new Insets(10, 0, 0, 0);
        JLabel hint = new JLabel("Use credentials from USER_LOGIN table", JLabel.CENTER);
        hint.setForeground(TEXT_SUB);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        card.add(hint, gc);

        JLabel logo = new JLabel(createCricketBadge());
        logo.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(logo, new GridBagConstraints(
            0, 8, 1, 1,
            1.0, 0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(4, 0, 0, 0),
            0, 0
        ));

        GridBagConstraints shell = new GridBagConstraints();
        shell.insets = new Insets(18, 18, 18, 18);
        shell.fill = GridBagConstraints.HORIZONTAL;
        shell.weightx = 1;
        shell.weighty = 1;
        outer.add(card, shell);

        add(outer);

        txtPass.addActionListener(e -> login());
        btnLogin.addActionListener(e -> login());
    }

    private void styleInput(JTextField input) {
        input.setBackground(INPUT_BG);
        input.setForeground(TEXT_MAIN);
        input.setCaretColor(TEXT_MAIN);
        input.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT);
        button.setForeground(new Color(15, 28, 47));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ACCENT_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(ACCENT);
            }
        });
        return button;
    }

    private Icon createCricketBadge() {
        int w = 110;
        int h = 34;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(17, 50, 33));
        g2.fillRoundRect(0, 0, w, h, 12, 12);
        g2.setColor(new Color(78, 128, 82));
        g2.fillRect(40, 6, 66, 22);

        g2.setColor(new Color(206, 46, 54));
        g2.fill(new Ellipse2D.Double(8, 7, 20, 20));
        g2.setColor(Color.WHITE);
        g2.drawLine(18, 9, 18, 25);
        g2.drawLine(15, 9, 15, 25);

        g2.setColor(new Color(243, 206, 129));
        g2.fillRoundRect(52, 10, 34, 8, 4, 4);
        g2.setColor(new Color(198, 167, 90));
        g2.drawRoundRect(52, 10, 34, 8, 4, 4);

        g2.dispose();
        return new ImageIcon(img);
    }

    private void login() {
        String username = txtUser.getText().trim();
        String password = new String(txtPass.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Connection c = DBConfig2.getConnection();

            PreparedStatement ps = c.prepareStatement(
                    "SELECT Role FROM USER_LOGIN WHERE Username = ? AND Password = ?"
            );
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String role = rs.getString("Role");          // 'ADMIN' or 'VIEWER'
                JOptionPane.showMessageDialog(this,
                        "Login Successful!  Role: " + role,
                        "Welcome", JOptionPane.INFORMATION_MESSAGE);
                new DashboardFrame2(role).setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Database error:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame2().setVisible(true));
    }
}
