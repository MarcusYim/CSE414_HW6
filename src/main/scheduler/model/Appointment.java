package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.util.Arrays;

public class Appointment {
    private final int apID;
    private final String cUsername;
    private final String pUsername;
    private final String vName;

    private Appointment(AppointmentBuilder builder) {
        this.apID = builder.apID;
        this.cUsername = builder.cUsername;
        this.pUsername = builder.pUsername;
        this.vName = builder.vName;
    }

    private Appointment(AppointmentGetter getter) {
        this.apID = getter.apID;
        this.cUsername = getter.cUsername;
        this.pUsername = getter.pUsername;
        this.vName = getter.vName;
    }

    public int getApID() {
        return apID;
    }

    public String getcUsername() {
        return cUsername;
    }

    public String getpUsername() {
        return pUsername;
    }

    public String getvName() {
        return vName;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addCaregiver = "INSERT INTO Appointments VALUES (? , ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addCaregiver);
            statement.setInt(1, this.apID);
            statement.setString(2, this.cUsername);
            statement.setString(3, this.pUsername);
            statement.setString(4, this.vName);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AppointmentBuilder {
        private final int apID;
        private final String cUsername;
        private final String pUsername;
        private final String vName;

        public AppointmentBuilder(int apID, String cUsername, String pUsername, String vName) {
            this.apID = apID;
            this.cUsername = cUsername;
            this.pUsername = pUsername;
            this.vName = vName;
        }

        public Appointment build() {
            return new Appointment(this);
        }
    }

    public static class AppointmentGetter {
        private final int apID;
        private String cUsername;
        private String pUsername;
        private String vName;

        public AppointmentGetter(int apID, String cUsername, String pUsername, String vName) {
            this.apID = apID;
            this.cUsername = cUsername;
            this.pUsername = pUsername;
            this.vName = vName;
        }

        public Appointment get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getCaregiver = "SELECT apID, cUsername, pUsername, vName FROM Appointments WHERE apID = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getCaregiver);
                statement.setInt(1, this.apID);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next())
                {
                    this.cUsername = resultSet.getString("cUsername");
                    this.pUsername = resultSet.getString("pUsername");
                    this.vName = resultSet.getString("vName");
                    return new Appointment(this);
                }

                return null;

            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
