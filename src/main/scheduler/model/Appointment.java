package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class Appointment
{
    private final String vaccineName;
    private final String caregiverUsername;
    private final String patientUsername;
    private final String date;

    private Appointment(Appointment.AppointmentBuilder builder) {
        this.vaccineName = builder.vaccineName;
        this.caregiverUsername = builder.caregiverUsername;
        this.patientUsername = builder.patientUsername;
        this.date = builder.date;
    }

    private Appointment(Appointment.AppointmentGetter getter) {
        this.vaccineName = getter.vaccineName;
        this.caregiverUsername = getter.caregiverUsername;
        this.patientUsername = getter.patientUsername;
        this.date = getter.date;
    }

    public String getVaccineName(){
        return vaccineName;
    }

    public String getCaregiverUsernameUsername() {
        return caregiverUsername;
    }

    public String getPatientUsernameUsername() {
        return patientUsername;
    }

    public String getDate()
    {
        return date;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO Appointments VALUES (? , ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setString(1, this.vaccineName);
            statement.setString(2, this.caregiverUsername);
            statement.setString(3, this.patientUsername);
            statement.setString(4, this.date);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AppointmentBuilder {
        private final String vaccineName;
        private final String caregiverUsername;
        private final String patientUsername;
        private final String date;

        public AppointmentBuilder(String vaccineName, String caregiverUsername, String patientUsername, String date) {
            this.vaccineName = vaccineName;
            this.caregiverUsername = caregiverUsername;
            this.patientUsername = patientUsername;
            this.date = date;
        }

        public Appointment build() {
            return new Appointment(this);
        }
    }

    public static class AppointmentGetter {
        private final String vaccineName;
        private final String caregiverUsername;
        private final String patientUsername;
        private final String date;

        public AppointmentGetter(String vaccineName, String caregiverUsername, String patientUsername, String date) {
            this.vaccineName = vaccineName;
            this.caregiverUsername = caregiverUsername;
            this.patientUsername = patientUsername;
            this.date = date;
        }
    }
}
