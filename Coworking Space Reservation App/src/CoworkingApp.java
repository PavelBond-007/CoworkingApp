import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// --- Data Classes ---

class CoworkingSpace {
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    private final int id;
    private String type;
    private double pricePerHour;
    private boolean available;

    public CoworkingSpace(String type, double pricePerHour) {
        this.id = idCounter.incrementAndGet();
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.available = true;
    }

    // --- Getters ---
    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public double getPricePerHour() {
        return pricePerHour;
    }

    public boolean isAvailable() {
        return available;
    }

    // --- Setters ---
    public void setType(String type) {
        this.type = type;
    }

    public void setPricePerHour(double pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "ID: " + id + ", Type: " + type + ", Price/Hour: $" + String.format("%.2f", pricePerHour) +
                ", Status: " + (available ? "Available" : "Unavailable");
    }
}


class Reservation {
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    private final int id;
    private final int spaceId;
    private final String customerName;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final CoworkingSpace reservedSpace;

    public Reservation(int spaceId, String customerName, LocalDate date, LocalTime startTime, LocalTime endTime, CoworkingSpace reservedSpace) {
        this.id = idCounter.incrementAndGet();
        this.spaceId = spaceId;
        this.customerName = customerName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reservedSpace = reservedSpace;
    }

    public int getId() {
        return id;
    }

    public int getSpaceId() {
        return spaceId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }


    public boolean conflictsWith(LocalDate checkDate, LocalTime checkStartTime, LocalTime checkEndTime) {
        if (!this.date.equals(checkDate)) {
            return false;
        }

        return checkStartTime.isBefore(this.endTime) && checkEndTime.isAfter(this.startTime);
    }


    @Override
    public String toString() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return "Reservation ID: " + id + ", Customer: " + customerName +
                ", Space ID: " + spaceId + " (" + reservedSpace.getType() + ")" +
                ", Date: " + date.format(DateTimeFormatter.ISO_DATE) +
                ", Time: " + startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter);
    }
}

// --- Service Classes ---

class CoworkingSpaceService {
    private final List<CoworkingSpace> spaces = new ArrayList<>();

    public CoworkingSpaceService() {
        // Add some initial sample spaces
        spaces.add(new CoworkingSpace("Open Desk", 10.0));
        spaces.add(new CoworkingSpace("Private Office", 25.0));
        spaces.add(new CoworkingSpace("Meeting Room", 40.0));
        spaces.get(1).setAvailable(false);
    }

    public void addSpace(CoworkingSpace space) {
        spaces.add(space);
        System.out.println("Space added successfully: " + space);
    }

    public boolean removeSpace(int id) {
        return spaces.removeIf(space -> space.getId() == id);
    }

    public CoworkingSpace findSpaceById(int id) {
        for (CoworkingSpace space : spaces) {
            if (space.getId() == id) {
                return space;
            }
        }
        return null;
    }

    public List<CoworkingSpace> getAllSpaces() {
        return new ArrayList<>(spaces);
    }

    public List<CoworkingSpace> getAvailableSpaces(LocalDate date, LocalTime startTime, LocalTime endTime, List<Reservation> allReservations) {
        List<CoworkingSpace> potentialSpaces = spaces.stream()
                .filter(CoworkingSpace::isAvailable)
                .toList();

        List<CoworkingSpace> trulyAvailableSpaces = new ArrayList<>();
        for (CoworkingSpace space : potentialSpaces) {
            boolean conflict = false;
            for (Reservation res : allReservations) {
                if (res.getSpaceId() == space.getId() && res.conflictsWith(date, startTime, endTime)) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict) {
                trulyAvailableSpaces.add(space);
            }
        }
        return trulyAvailableSpaces;
    }
}

// Manages reservations.

class ReservationService {
    private final List<Reservation> reservations = new ArrayList<>();
    private final CoworkingSpaceService spaceService;

    public ReservationService(CoworkingSpaceService spaceService) {
        this.spaceService = spaceService;
    }

    public void makeReservation(String customerName, int spaceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        CoworkingSpace space = spaceService.findSpaceById(spaceId);
        if (space == null) {
            System.out.println("Error: Coworking space with ID " + spaceId + " not found.");
            return;
        }

        if (!space.isAvailable()) {
            System.out.println("Error: Space ID " + spaceId + " is currently marked as unavailable by the admin.");
            return;
        }


        for (Reservation existingRes : reservations) {
            if (existingRes.getSpaceId() == spaceId && existingRes.conflictsWith(date, startTime, endTime)) {
                System.out.println("Error: Space ID " + spaceId + " is already booked during the requested time.");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                System.out.println("   Existing booking: " + existingRes.getDate() + " from " +
                        existingRes.getStartTime().format(timeFormatter) + " to " +
                        existingRes.getEndTime().format(timeFormatter));
                return;
            }
        }

        // If no conflicts, create and add the reservation
        Reservation newReservation = new Reservation(spaceId, customerName, date, startTime, endTime, space);
        reservations.add(newReservation);
        System.out.println("Reservation successful!");
        System.out.println(newReservation);
    }

    public boolean cancelReservation(int reservationId) {
        return reservations.removeIf(reservation -> reservation.getId() == reservationId);
    }

    // Gets all reservations made by a specific customer.

    public List<Reservation> getReservationsByCustomer(String customerName) {
        List<Reservation> customerReservations = new ArrayList<>();
        for (Reservation reservation : reservations) {
            if (reservation.getCustomerName().equalsIgnoreCase(customerName)) {
                customerReservations.add(reservation);
            }
        }
        return customerReservations;
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations);
    }

    public List<Reservation> getReservationsBySpaceId(int spaceId) {
        return reservations.stream()
                .filter(res -> res.getSpaceId() == spaceId)
                .collect(Collectors.toList());
    }
}


// --- Main Application Class ---

public class CoworkingApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final CoworkingSpaceService spaceService = new CoworkingSpaceService();
    private static final ReservationService reservationService = new ReservationService(spaceService);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm"); // 24-hour format

    public static void main(String[] args) {
        System.out.println("\n=============================================");
        System.out.println(" Welcome to the Coworking Space Reservation App!");
        System.out.println("=============================================");

        while (true) {
            showMainMenu();
            int choice = getUserIntInput("Enter your choice: ");

            switch (choice) {
                case 1:
                    adminLogin();
                    break;
                case 2:
                    customerLogin();
                    break;
                case 3:
                    System.out.println("\nThank you for using the app. Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // --- Menus ---

    private static void showMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Admin Login");
        System.out.println("2. Customer Login");
        System.out.println("3. Exit");
    }

    private static void showAdminMenu() {
        System.out.println("\n--- Admin Menu ---");
        System.out.println("1. Add New Coworking Space");
        System.out.println("2. Remove Coworking Space");
        System.out.println("3. Update Coworking Space");
        System.out.println("4. View All Coworking Spaces");
        System.out.println("5. View All Reservations");
        System.out.println("6. Logout (Back to Main Menu)");
    }

    private static void showCustomerMenu(String customerName) {
        System.out.println("\n--- Customer Menu (Logged in as: " + customerName + ") ---");
        System.out.println("1. Browse Available Spaces for a Time Slot");
        System.out.println("2. Make a Reservation");
        System.out.println("3. View My Reservations");
        System.out.println("4. Cancel a Reservation");
        System.out.println("5. Logout (Back to Main Menu)");
    }

    // --- Login Flows ---

    private static void adminLogin() {
        System.out.println("\n--- Admin Access Granted ---");
        while (true) {
            showAdminMenu();
            int choice = getUserIntInput("Enter your choice: ");
            switch (choice) {
                case 1:
                    addCoworkingSpace();
                    break;
                case 2:
                    removeCoworkingSpace();
                    break;
                case 3:
                    updateCoworkingSpace();
                    break;
                case 4:
                    viewAllSpaces();
                    break;
                case 5:
                    viewAllReservations();
                    break;
                case 6:
                    System.out.println("Logging out from Admin Menu...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void customerLogin() {
        System.out.print("Enter your name to login/register: ");
        String customerName = scanner.nextLine();
        System.out.println("\nWelcome, " + customerName + "!");

        while (true) {
            showCustomerMenu(customerName);
            int choice = getUserIntInput("Enter your choice: ");
            switch (choice) {
                case 1:
                    browseAvailableSpaces();
                    break;
                case 2:
                    makeReservation(customerName);
                    break;
                case 3:
                    viewMyReservations(customerName);
                    break;
                case 4:
                    cancelReservation(customerName);
                    break;
                case 5:
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // --- Admin Actions ---

    private static void addCoworkingSpace() {
        System.out.println("\n--- Add New Coworking Space ---");
        System.out.print("Enter space type (e.g., Open Desk, Private Office): ");
        String type = scanner.nextLine();
        double price = getUserDoubleInput();

        CoworkingSpace newSpace = new CoworkingSpace(type, price);
        spaceService.addSpace(newSpace);
    }

    private static void removeCoworkingSpace() {
        System.out.println("\n--- Remove Coworking Space ---");
        viewAllSpaces();
        int idToRemove = getUserIntInput("Enter the ID of the space to remove: ");

        // Check if there are any reservations for this space
        List<Reservation> existingReservations = reservationService.getReservationsBySpaceId(idToRemove);
        if (!existingReservations.isEmpty()) {
            System.out.println("Warning: This space has existing reservations:");
            existingReservations.forEach(System.out::println);
            String confirm = getUserStringInput();
            if (!confirm.equalsIgnoreCase("yes")) {
                System.out.println("Removal cancelled.");
                return;
            }

            existingReservations.forEach(res -> reservationService.cancelReservation(res.getId()));
            System.out.println("Associated reservations cancelled.");
        }


        if (spaceService.removeSpace(idToRemove)) {
            System.out.println("Space with ID " + idToRemove + " removed successfully.");
        } else {
            System.out.println("Error: Space with ID " + idToRemove + " not found.");
        }
    }

    private static void updateCoworkingSpace() {
        System.out.println("\n--- Update Coworking Space ---");
        viewAllSpaces();
        int idToUpdate = getUserIntInput("Enter the ID of the space to update: ");
        CoworkingSpace space = spaceService.findSpaceById(idToUpdate);

        if (space == null) {
            System.out.println("Error: Space with ID " + idToUpdate + " not found.");
            return;
        }

        System.out.println("Current details: " + space);
        System.out.print("Enter new type (leave blank to keep '" + space.getType() + "'): ");
        String newType = scanner.nextLine();
        if (!newType.trim().isEmpty()) {
            space.setType(newType);
        }

        System.out.print("Enter new price per hour (leave blank or enter invalid number to keep $" + String.format("%.2f", space.getPricePerHour()) + "): $");
        String priceInput = scanner.nextLine();
        try {
            double newPrice = Double.parseDouble(priceInput);
            if (newPrice >= 0) {
                space.setPricePerHour(newPrice);
            } else {
                System.out.println("Price cannot be negative. Keeping original price.");
            }
        } catch (NumberFormatException e) {
            if (!priceInput.trim().isEmpty()) {
                System.out.println("Invalid price format. Keeping original price.");
            }
        }

        System.out.print("Set availability (true/false, leave blank to keep '" + space.isAvailable() + "'): ");
        String availableInput = scanner.nextLine();
        if (availableInput.trim().equalsIgnoreCase("true")) {
            space.setAvailable(true);
        } else if (availableInput.trim().equalsIgnoreCase("false")) {
            space.setAvailable(false);
        }

        System.out.println("Space updated successfully: " + space);
    }

    private static void viewAllSpaces() {
        System.out.println("\n--- All Coworking Spaces ---");
        List<CoworkingSpace> spaces = spaceService.getAllSpaces();
        if (spaces.isEmpty()) {
            System.out.println("No coworking spaces found.");
        } else {
            spaces.forEach(System.out::println);
        }
        System.out.println("-----------------------------");
    }

    private static void viewAllReservations() {
        System.out.println("\n--- All Reservations ---");
        List<Reservation> reservations = reservationService.getAllReservations();
        if (reservations.isEmpty()) {
            System.out.println("No reservations found.");
        } else {
            reservations.forEach(System.out::println);
        }
        System.out.println("-------------------------");
    }

    // --- Customer Actions ---

    private static void browseAvailableSpaces() {
        System.out.println("\n--- Browse Available Spaces ---");
        LocalDate date = getUserDateInput("Enter desired date (YYYY-MM-DD): ");
        if (date == null) return;

        LocalTime startTime = getUserTimeInput("Enter desired start time (HH:MM): ");
        if (startTime == null) return;

        LocalTime endTime = getUserTimeInput("Enter desired end time (HH:MM): ");
        if (endTime == null) return;

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            System.out.println("Error: End time must be after start time.");
            return;
        }


        System.out.println("\n--- Available Spaces for " + date + " from " + startTime.format(timeFormatter) + " to " + endTime.format(timeFormatter) + " ---");
        List<CoworkingSpace> availableSpaces = spaceService.getAvailableSpaces(date, startTime, endTime, reservationService.getAllReservations());

        if (availableSpaces.isEmpty()) {
            System.out.println("No spaces available for the selected time slot.");
        } else {
            availableSpaces.forEach(System.out::println);
        }
        System.out.println("-----------------------------------------------------");
    }

    private static void makeReservation(String customerName) {
        System.out.println("\n--- Make a Reservation ---");

        // Get date and time first to show appropriate availability
        LocalDate date = getUserDateInput("Enter desired reservation date (YYYY-MM-DD): ");
        if (date == null) return;

        LocalTime startTime = getUserTimeInput("Enter desired start time (HH:MM): ");
        if (startTime == null) return;

        LocalTime endTime = getUserTimeInput("Enter desired end time (HH:MM): ");
        if (endTime == null) return;

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            System.out.println("Error: End time must be after start time.");
            return;
        }

        System.out.println("\n--- Checking Availability for " + date + " from " + startTime.format(timeFormatter) + " to " + endTime.format(timeFormatter) + " ---");
        List<CoworkingSpace> availableSpaces = spaceService.getAvailableSpaces(date, startTime, endTime, reservationService.getAllReservations());

        if (availableSpaces.isEmpty()) {
            System.out.println("Sorry, no spaces are available for this time slot.");
            return;
        }

        System.out.println("Available Spaces:");
        availableSpaces.forEach(System.out::println);
        System.out.println("--------------------");


        int spaceId = getUserIntInput("Enter the ID of the space you want to reserve: ");

        // Double-check if the selected ID is actually in the *available* list
        boolean isValidChoice = availableSpaces.stream().anyMatch(s -> s.getId() == spaceId);
        if (!isValidChoice) {
            System.out.println("Error: The selected Space ID (" + spaceId + ") is not available for the chosen time slot or does not exist.");
            return;
        }


        // Attempt to make the reservation (includes final conflict check within the service)
        reservationService.makeReservation(customerName, spaceId, date, startTime, endTime);
    }

    private static void viewMyReservations(String customerName) {
        System.out.println("\n--- Your Reservations (" + customerName + ") ---");
        List<Reservation> myReservations = reservationService.getReservationsByCustomer(customerName);
        if (myReservations.isEmpty()) {
            System.out.println("You have no reservations.");
        } else {
            myReservations.forEach(System.out::println);
        }
        System.out.println("-------------------------------------");
    }

    private static void cancelReservation(String customerName) {
        System.out.println("\n--- Cancel a Reservation ---");
        List<Reservation> myReservations = reservationService.getReservationsByCustomer(customerName);

        if (myReservations.isEmpty()) {
            System.out.println("You have no reservations to cancel.");
            return;
        }

        System.out.println("Your current reservations:");
        myReservations.forEach(System.out::println);
        System.out.println("--------------------------");

        int reservationIdToCancel = getUserIntInput("Enter the Reservation ID you want to cancel: ");

        // Verify the ID belongs to the current customer before cancelling
        boolean found = false;
        for (Reservation res : myReservations) {
            if (res.getId() == reservationIdToCancel) {
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("Error: Reservation ID " + reservationIdToCancel + " not found in your bookings.");
            return;
        }


        if (reservationService.cancelReservation(reservationIdToCancel)) {
            System.out.println("Reservation ID " + reservationIdToCancel + " cancelled successfully.");
        } else {
            // This case should ideally not happen due to the check above
            System.out.println("Error: Could not cancel reservation ID " + reservationIdToCancel + ". It might have already been cancelled or does not exist.");
        }
    }


    // --- Input Helper Methods ---

    private static int getUserIntInput(String prompt) {
        int input;
        while (true) {
            System.out.print(prompt);
            try {
                input = Integer.parseInt(scanner.nextLine());
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
        return input;
    }

    // Gets double input from the user, handling potential errors.

    private static double getUserDoubleInput() {
        double input;
        while (true) {
            System.out.print("Enter price per hour: $");
            try {
                input = Double.parseDouble(scanner.nextLine());
                if (input < 0) {
                    System.out.println("Input cannot be negative. Please try again.");
                } else {
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number (e.g., 10.50).");
            }
        }
        return input;
    }

    // Gets non-empty string input from the user.

    private static String getUserStringInput() {
        String input;
        while (true) {
            System.out.print("Are you sure you want to remove this space and its reservations? (yes/no): ");
            input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                break;
            } else {
                System.out.println("Input cannot be empty. Please try again.");
            }
        }
        return input;
    }


    // Gets date input from the user in YYYY-MM-DD format.

    private static LocalDate getUserDateInput(String prompt) {
        LocalDate date = null;
        while (date == null) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                date = LocalDate.parse(input, dateFormatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD (e.g., 2025-04-03).");
            }
        }
        return date;
    }

    // Gets time input from the user in HH:MM format (24-hour).

    private static LocalTime getUserTimeInput(String prompt) {
        LocalTime time = null;
        while (time == null) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                time = LocalTime.parse(input, timeFormatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time format. Please use HH:MM (e.g., 09:00 or 14:30).");
            }
        }
        return time;
    }
}
