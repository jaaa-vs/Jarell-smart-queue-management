import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.JOptionPane;

/**
 * Singleton service for queue DB operations.
 * XAMPP MySQL: localhost:3306/root/(empty pass)
 */
public class QueueService {
    private static final Logger LOGGER = Logger.getLogger(QueueService.class.getName());
    private static QueueService instance;
    private Connection conn;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/queue_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    
    private static final int WAITING_CAPACITY = 5;

    private QueueService() {
        connect();
    }

    public static QueueService getInstance() {
        if (instance == null) {
            instance = new QueueService();
        }
        return instance;
    }

    private void connect() {
        try {
            conn = DriverManager.getConnection(DB_URL, "root", "");
            LOGGER.info("DB connected");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DB connection failed", e);
            JOptionPane.showMessageDialog(null, 
                "DB Error: " + e.getMessage() + "\nStart XAMPP MySQL & import setup.sql",
                "Connection Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConnected() {
        return conn != null;
    }

    // Generate next: A + MAX(seq)+1
    public String generateQueueNum() {
        if (!isConnected()) return null;
        try (PreparedStatement countPs = conn.prepareStatement("SELECT COUNT(*) FROM queues WHERE status = 'waiting'");
             PreparedStatement insertPs = conn.prepareStatement(
                 "INSERT INTO queues (queue_num, status) VALUES (?, ?)");
             PreparedStatement seqPs = conn.prepareStatement(
                 "SELECT COALESCE(MAX(CAST(SUBSTRING(queue_num,2) AS UNSIGNED)), 0) + 1 as next_seq FROM queues")) {

            // Get next seq
            ResultSet seqRs = seqPs.executeQuery();
            seqRs.next();
            int nextSeq = seqRs.getInt("next_seq");
            String newNum = String.format("A%03d", nextSeq);

            // Check capacity
            ResultSet countRs = countPs.executeQuery();
            countRs.next();
            int waitingCount = countRs.getInt(1);
            String status = (waitingCount < WAITING_CAPACITY) ? "waiting" : "next_batch";

            insertPs.setString(1, newNum);
            insertPs.setString(2, status);
            insertPs.executeUpdate();
            return newNum;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Generate failed", e);
            return null;
        }
    }

    // Get next waiting → set 'calling'
    public String callNext() {
        if (!isConnected()) return null;
        try (PreparedStatement ps1 = conn.prepareStatement(
            "UPDATE queues SET status = 'calling', called_time = CURRENT_TIMESTAMP WHERE status = 'waiting' ORDER BY gen_time ASC LIMIT 1");
             PreparedStatement ps2 = conn.prepareStatement(
                 "UPDATE queues SET status = 'calling', called_time = CURRENT_TIMESTAMP WHERE status = 'next_batch' ORDER BY gen_time ASC LIMIT 1");
             PreparedStatement getPs = conn.prepareStatement(
                 "SELECT queue_num FROM queues WHERE status = 'calling' ORDER BY called_time DESC LIMIT 1")) {

            // Try waiting first
            int updated = ps1.executeUpdate();

            if (updated == 0) {
                // Then next_batch
                updated = ps2.executeUpdate();
            }
            
            if (updated > 0) {
                ResultSet rs = getPs.executeQuery();
                if (rs.next()) {
                    String calledNum = rs.getString("queue_num");
                    return calledNum;
                }
            }
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Call next failed", e);
            return null;
        }
    }

    /**
     * Transfer next_batch to waiting when current waiting is empty.
     * Called automatically after serveCurrent or manually.
     */
    public boolean transferNextBatchToWaiting() {
        if (!isConnected()) return false;
        
        // Check if waiting is empty
        try (PreparedStatement checkPs = conn.prepareStatement("SELECT COUNT(*) FROM queues WHERE status = 'waiting'");
             PreparedStatement transferPs = conn.prepareStatement("UPDATE queues SET status = 'waiting' WHERE status = 'next_batch' ORDER BY gen_time ASC LIMIT ?")) {
            
            ResultSet checkRs = checkPs.executeQuery();
            checkRs.next();
            int waitingCount = checkRs.getInt(1);
            
            if (waitingCount == 0) {
                transferPs.setInt(1, WAITING_CAPACITY);
                int transferred = transferPs.executeUpdate();
                System.out.println("Transferred " + transferred + " from next_batch to waiting");
                return transferred > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Transfer next_batch failed", e);
        }
        return false;
    }

    // Serve current calling → 'served'
    public boolean serveCurrent(String queueNum) {
        if (!isConnected()) return false;
        try (PreparedStatement ps = conn.prepareStatement(
            "UPDATE queues SET status = 'served' WHERE queue_num = ? AND status = 'calling'")) {
            ps.setString(1, queueNum);
            int updated = ps.executeUpdate();
            boolean served = updated > 0;
            
            if (served) {
                // Auto-transfer next_batch to waiting if empty
                transferNextBatchToWaiting();
            }
            return served;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Serve failed", e);
            return false;
        }
    }

    // Live queue data
    public List<String> getWaitingQueue() {
        List<String> result = new ArrayList<>();
        if (!isConnected()) return result;
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT queue_num FROM queues WHERE status = 'waiting' ORDER BY gen_time ASC LIMIT ?")) {
            ps.setInt(1, WAITING_CAPACITY);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString("queue_num"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Load waiting failed", e);
        }
        return result;
    }

    public List<String> getCallingQueue() {
        List<String> result = new ArrayList<>();
        if (!isConnected()) return result;
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT queue_num FROM queues WHERE status = 'calling' ORDER BY called_time DESC LIMIT 1")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result.add(rs.getString("queue_num"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Load calling failed", e);
        }
        return result;
    }

    public List<String> getNextBatch(int count) {
        List<String> result = new ArrayList<>();
        if (!isConnected()) return result;
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT queue_num FROM queues WHERE status = 'next_batch' ORDER BY gen_time ASC LIMIT ?")) {
            ps.setInt(1, count);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString("queue_num"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Load next_batch failed", e);
        }
        return result;
    }

    private List<String> getQueuesByStatus(String status) {
        List<String> result = new ArrayList<>();
        if (!isConnected()) return result;
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT queue_num FROM queues WHERE status = ? ORDER BY gen_time ASC LIMIT 10")) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString("queue_num"));
            }
            System.out.println("DEBUG QueueService.getQueuesByStatus('" + status + "'): found " + result.size() + " items: " + result);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Load " + status + " failed", e);
            System.out.println("DEBUG QueueService.getQueuesByStatus('" + status + "'): SQL error: " + e.getMessage());
        }
        return result;
    }

    // History: last 20 served + recent
    public List<Object[]> getHistory() {
        List<Object[]> result = new ArrayList<>();
        if (!isConnected()) return result;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT queue_num, gen_time, called_time, status " +
                "FROM queues ORDER BY gen_time DESC LIMIT 20");
            while (rs.next()) {
                result.add(new Object[] {
                    rs.getString("queue_num"),
                    rs.getString("gen_time").substring(11, 16), // HH:MM
                    rs.getString("called_time") != null ? rs.getString("called_time").substring(11, 16) : "-",
                    rs.getString("status").toUpperCase()
                });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "History failed", e);
        }
        return result;
    }

    // Stats: waiting count, served today
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        if (!isConnected()) {
            stats.put("waiting", 0);
            stats.put("nextBatch", 0);
            stats.put("servedToday", 0);
            return stats;
        }
        try (PreparedStatement ps1 = conn.prepareStatement("SELECT COUNT(*) FROM queues WHERE status = 'waiting'");
             PreparedStatement ps2 = conn.prepareStatement("SELECT COUNT(*) FROM queues WHERE status = 'next_batch'");
             PreparedStatement ps3 = conn.prepareStatement(
                 "SELECT COUNT(*) FROM queues WHERE status = 'served' AND DATE(gen_time) = CURDATE()")) {

            ResultSet rs1 = ps1.executeQuery();
            rs1.next();
            stats.put("waiting", rs1.getInt(1));

            ResultSet rs2 = ps2.executeQuery();
            rs2.next();
            stats.put("nextBatch", rs2.getInt(1));
            
            ResultSet rs3 = ps3.executeQuery();
            rs3.next();
            stats.put("servedToday", rs3.getInt(1));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Stats failed", e);
        }
        return stats;
    }

    // Reset: clear waiting
    public void resetQueue() {
        if (!isConnected()) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM queues");  // Clear ALL queues, restart at A001
            System.out.println("Queue reset complete - next number will be A001");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Reset failed", e);
        }
    }

    public void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Close failed", e);
        }
    }
}

