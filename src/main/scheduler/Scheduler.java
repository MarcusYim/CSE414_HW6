package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Appointment;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Arrays;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    private static final String[] specialChars = new String[] {"!", "@", "#", "?"};
    private static final int passLength = 8;

    public static void main(String[] args) throws SQLException {

        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static boolean checkPassword(String pass)
    {
        int upper = 0, lower = 0, number = 0, special = 0;

        for(int i = 0; i < pass.length(); i++)
        {
            char ch = pass.charAt(i);
            if (ch >= 'A' && ch <= 'Z')
                upper++;
            else if (ch >= 'a' && ch <= 'z')
                lower++;
            else if (ch >= '0' && ch <= '9')
                number++;
            else
                special++;
        }

        if (pass.length() < passLength)
        {
            return false;
        }

        if (special < 1)
        {
            return false;
        }

        if (upper < 2)
        {
            return false;
        }

        if (lower < 2)
        {
            return false;
        }

        if (number < 2)
        {
            return false;
        }

        if (upper + lower < 4)
        {
            return false;
        }

        return true;
    }

    private static void createPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }

        String username = tokens[1];
        String password = tokens[2];

        if (!checkPassword(password))
        {
            System.out.println("Password is not strong enough.");
            return;
        }

        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        if (!checkPassword(password))
        {
            System.out.println("Password is not strong enough.");
            return;
        }

        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null)
        {
            System.out.println("Please login first!");
        }

        if (tokens.length != 2) {
            System.out.println("Login failed.");
            return;
        }

        String date = tokens[1];

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAvailabilities = "SELECT Username, Name, Doses FROM Availabilities LEFT OUTER JOIN Vaccines ON 1 = 1 WHERE Time = ? ORDER BY Username";
        try {
            Date d = Date.valueOf(date);
            PreparedStatement statement = con.prepareStatement(getAvailabilities);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();

            System.out.println("Available caregivers and vaccines:");
            while (resultSet.next()) {
                System.out.print(resultSet.getString("username") + " ");
                System.out.print(resultSet.getString("Name") + " ");
                System.out.println(resultSet.getInt("Doses"));
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("Please try again!");
        } finally {
            cm.closeConnection();
        }
    }

    private static String getCaregiver(Connection con, ConnectionManager cm, String date)
    {
        String getAvailabilities = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
        String caregiver = "";
        try {
            Date d = Date.valueOf(date);
            PreparedStatement statement = con.prepareStatement(getAvailabilities);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                caregiver = resultSet.getString("Username");
                return caregiver;
            }
        } catch (Exception e) {
            System.out.println("Please try again!");
            return null;
        } finally {
            cm.closeConnection();
        }

        System.out.println("No Caregiver is available!");
        return null;
    }

    public static void deleteCaregiver(Connection con, ConnectionManager cm, Date date, String caregiver)
    {
        String deleteAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";

        try {
            PreparedStatement statement = con.prepareStatement(deleteAvailability);
            statement.setDate(1, date);
            statement.setString(2, caregiver);
            statement.executeQuery();
        } catch (Exception e) {
            System.out.println("Please try again!");
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens)
    {
        if (currentPatient == null && currentCaregiver == null)
        {
            System.out.println("Please login first!");
            return;
        }

        if (currentPatient == null)
        {
            System.out.println("Please login as a patient!");
            return;
        }

        String date = tokens[1];
        String vaccine = tokens[2];

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAvailabilities = "SELECT Username, Doses FROM Availabilities, Vaccines WHERE Time = ? AND Name = ? ORDER BY Username DESC";
        String deleteAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
        String createAppointment = "INSERT INTO Appointments VALUES(?, ?, ?, ?)";
        String getID = "SELECT apID FROM Appointments WHERE Time = ? AND cUsername = ?";

        String caregiver = "";
        int doses = 0;
        try {
            Date d = Date.valueOf(date);
            PreparedStatement statement = con.prepareStatement(getAvailabilities);
            statement.setDate(1, d);
            statement.setString(2, vaccine);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                caregiver = resultSet.getString("Username");
                doses = resultSet.getInt("Doses");
            }

            if (caregiver.isEmpty())
            {
                System.out.println("No Caregiver is available!");
                return;
            }

            if (doses < 1)
            {
                System.out.println("Not enough available doses");
                return;
            }

            PreparedStatement dstatement = con.prepareStatement(deleteAvailability);
            dstatement.setDate(1, d);
            dstatement.setString(2, caregiver);
            dstatement.execute();

            PreparedStatement astatement = con.prepareStatement(createAppointment);
            astatement.setString(1, caregiver);
            astatement.setString(2, currentPatient.getUsername());
            astatement.setString(3, vaccine);
            astatement.setDate(4, d);
            astatement.execute();

            PreparedStatement istatement = con.prepareStatement(getID);
            istatement.setDate(1, d);
            istatement.setString(2, caregiver);
            ResultSet iresultSet = istatement.executeQuery();
            int apID = -1;

            while (iresultSet.next()) {
                apID = iresultSet.getInt("apID");
            }

            System.out.println("Appointment ID: " + apID + ", Caregiver username: " + caregiver);

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Please try again!");
        } finally {
            cm.closeConnection();
        }



        //query with vaccine and date to get caregivers
        //delete caregiver from availabilities
        //create appointment
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        String getAppointments = "";
        String currentUser = "";

        if (currentPatient != null)
        {
            getAppointments = "SELECT apID, vName, Time, cUsername FROM Appointments WHERE pUsername = '" + currentPatient.getUsername() + "' ORDER BY apID";
            currentUser = "cUsername";
        }
        else if (currentCaregiver != null)
        {
            getAppointments = "SELECT apID, vName, Time, pUsername FROM Appointments WHERE cUsername = '" + currentCaregiver.getUsername() + "' ORDER BY apID";
            currentUser = "pUsername";
        }
        else
        {
            System.out.println("Please login first!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            PreparedStatement statement = con.prepareStatement(getAppointments);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                System.out.println(resultSet.getInt("apID") + " " + resultSet.getString("vName") + " " + resultSet.getString("Time") + " " + resultSet.getString(currentUser));
            }
        } catch (Exception e) {
            System.out.println("Please try again!");
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens)
    {
        if (!(currentCaregiver == null || currentPatient == null))
        {
            System.out.println("Please try again!");
        }
        else if (currentCaregiver == null && currentPatient == null)
        {
            System.out.println("No user logged in!");
        }
        else
        {
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out!");
        }
    }
}
